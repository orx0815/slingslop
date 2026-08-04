/**
 * hero3d.ts — Raymarched fractal tetrahedron for the hero section
 *
 * A full-screen shader quad using GLSL raymarching to render a fractal
 * tetrahedron with volumetric glow.
 * Inspired by: https://codepen.io/sabosugi/pen/jEVPrOx
 *
 * Drag on the hero canvas to rotate the fractal.
 *
 * Uses raw WebGL2 instead of Three.js to keep the bundle tiny (~3 KB vs 500 KB).
 */

/** iOS 13+ gates DeviceOrientation/DeviceMotion behind a permission request. */
interface MotionPermission {
  requestPermission?: () => Promise<'granted' | 'denied' | 'default'>;
}

export function initHero3D(): void {
  const maybeContainer = document.querySelector<HTMLElement>('.hero-3d-container');
  if (!maybeContainer) {
    return;
  }
  const container: HTMLElement = maybeContainer;

  // ── Canvas & WebGL2 Context ────────────────────────────────────────────────
  const canvas = document.createElement('canvas');
  canvas.width = container.clientWidth;
  canvas.height = container.clientHeight;
  canvas.style.width = '100%';
  canvas.style.height = '100%';
  container.appendChild(canvas);

  const gl = canvas.getContext('webgl2', { alpha: true, antialias: true });
  if (!gl) {
    return;
  }

  // ── Shaders ────────────────────────────────────────────────────────────────
  const vertexSrc = /* glsl */ `#version 300 es
    in vec2 a_position;
    void main() {
      gl_Position = vec4(a_position, 0.0, 1.0);
    }
  `;

  const fragmentSrc = /* glsl */ `#version 300 es
    precision highp float;
    #define MAX_ITERATIONS 10

    uniform float u_time;
    uniform vec2  u_resolution;
    uniform vec2  u_mouse_rot;
    uniform float u_zoom;
    uniform float u_perspective;
    uniform float u_anim_speed;
    uniform float u_shape_size;
    uniform int   u_iterations;
    uniform vec3  u_fold;
    uniform float u_glow_intensity;
    uniform float u_glow_base;
    uniform sampler2D u_textTex;
    uniform float u_hasText;

    out vec4 fragColor;

    // Global fractal tint accumulated during SDF evaluation
    vec3 g_fractalTint;

    // Precomputed rotation matrices (computed once per fragment in main)
    mat2 g_rotAnim;
    mat2 g_rotAnim07;
    mat2 g_rotFractalXY;
    mat2 g_rotFractalXZ;
    mat2 g_camRotX;
    mat2 g_camRotY;

    mat2 rotate2D(float angle) {
      float c = cos(angle);
      float s = sin(angle);
      return mat2(c, -s, s, c);
    }

    // Tetrahedron signed distance field
    float sdTetrahedron(vec3 p, float r) {
      float d = max(
        max(-p.x - p.y - p.z, p.x + p.y - p.z),
        max(-p.x + p.y + p.z, p.x - p.y + p.z)
      );
      return (d - r) / sqrt(3.0);
    }

    // Hash for per-ray jitter (removes volumetric banding)
    float hash(vec2 p) {
      return fract(sin(dot(p, vec2(12.9098, 78.013))) * 43758.5453);
    }

    float evaluateSceneSDF(vec3 samplePoint) {
      vec3 localPos = samplePoint;

      // Apply animation rotations (precomputed — no sin/cos in the loop)
      localPos.xz *= g_rotAnim;
      localPos.xy *= g_rotAnim07;

      float boundShape = sdTetrahedron(localPos, u_shape_size);

      vec4 q = vec4(localPos, 1.0);

      // Spherical inversion hybrid fractal
      for (int k = 0; k < MAX_ITERATIONS; k++) {
        if (k >= u_iterations) break;

        float r2 = dot(q.xyz, q.xyz);
        float sphereFold = max(1.4 / max(r2, -0.8), 1.1);
        q *= sphereFold;

        q.xyz = abs(q.xyz) - u_fold;

        q.xy *= g_rotFractalXY;
        q.xz *= g_rotFractalXZ;

        q *= 1.4;
      }

      g_fractalTint = q.xyz * log(q.w + 1.0);

      float fractalSurface = (length(q.xyz) - 1.2) / q.w;

      // Intersect infinite fractal with tetrahedron boundary
      return max(boundShape, fractalSurface);
    }

    // Normal estimation via tetrahedral finite differences (4 SDF calls)
    vec3 estimateNormal(vec3 p) {
      vec2 e = vec2(1.0, -1.0) * 0.501;
      return normalize(
        e.xyy * evaluateSceneSDF(p + e.xyy) +
        e.yyx * evaluateSceneSDF(p + e.yyx) +
        e.yxy * evaluateSceneSDF(p + e.yxy) +
        e.xxx * evaluateSceneSDF(p + e.xxx)
      );
    }

    vec3 renderRay(vec3 origin, vec3 direction, float jitter) {
      float totalDistance = jitter;
      int bounceCount = 1;
      vec3 accumulatedLight = vec3(0.0);

      for (int step = 0; step < 120; step++) {
        vec3 currentPos = origin + direction * totalDistance;
        float sceneDist = evaluateSceneSDF(currentPos);

        g_fractalTint = cos(g_fractalTint * u_glow_base);
        g_fractalTint *= g_fractalTint;

        if (step > 1) {
          float distSq = dot(currentPos, currentPos);
          float attenuation = exp(-distSq * 0.25);
          accumulatedLight += u_glow_intensity * g_fractalTint * attenuation;
        }

        if (sceneDist < 0.0002) {
          if (bounceCount > 1) break;
          vec3 surfaceNormal = estimateNormal(currentPos);
          direction = reflect(direction, surfaceNormal);
          accumulatedLight *= (1.1 + 0.1 * exp(float(bounceCount)));
          totalDistance += 0.02;
          bounceCount++;
        }

        totalDistance += sceneDist * 0.8;

        if (totalDistance > u_zoom + 4.0) break;
      }

      return accumulatedLight;
    }

    // Sample the text texture projected onto the fractal's front face
    float sampleText(vec3 p) {
      if (u_hasText < 0.5) return 0.0;
      // Project onto XY plane, map to [0,1] UV range
      vec2 textUV = p.xy / u_shape_size * 0.5 + 0.5;
      // Flip Y for canvas coordinate system
      textUV.y = 1.0 - textUV.y;
      if (textUV.x < 0.0 || textUV.x > 1.0 || textUV.y < 0.0 || textUV.y > 1.0) return 0.0;
      return texture(u_textTex, textUV).r;
    }

    vec3 getPixelColor(vec2 fragCoord) {
      vec2 uv = (2.0 * fragCoord.xy - u_resolution.xy) / u_resolution.y;
      float jitter = hash(uv) * 0.1;

      vec3 cameraOrigin = vec3(0.0, 0.0, -u_zoom);
      vec3 rayDirection = normalize(vec3(uv, u_perspective));

      cameraOrigin.yz *= g_camRotY;
      cameraOrigin.xz *= g_camRotX;
      rayDirection.yz *= g_camRotY;
      rayDirection.xz *= g_camRotX;

      vec3 finalColor = renderRay(cameraOrigin, rayDirection, jitter);
      finalColor = finalColor * finalColor; // contrast boost

      // Etch text: cast a secondary ray to find the surface point and sample text there
      float t = jitter;
      for (int i = 0; i < 80; i++) {
        vec3 pos = cameraOrigin + rayDirection * t;
        float d = evaluateSceneSDF(pos);
        if (d < 0.001) {
          float textVal = sampleText(pos);
          // Brighten where text is — etch a glowing imprint
          finalColor += textVal * vec3(0.4, 0.8, 1.0) * 1.5;
          break;
        }
        t += d * 0.8;
        if (t > u_zoom + 4.0) break;
      }

      return finalColor;
    }

    void main() {
      float t = u_time * u_anim_speed;
      g_rotAnim      = rotate2D(t);
      g_rotAnim07    = rotate2D(t * 0.7);
      g_rotFractalXY = rotate2D(0.7 + sin(u_time * 0.05) * 0.1);
      g_rotFractalXZ = rotate2D(0.5);
      g_camRotY      = rotate2D(-u_mouse_rot.y);
      g_camRotX      = rotate2D(-u_mouse_rot.x);

      vec3 color = getPixelColor(gl_FragCoord.xy);
      // Use luminance as alpha so the black background is transparent
      float alpha = clamp(dot(color, vec3(0.299, 0.587, 0.114)) * 4.0, 0.0, 1.0);
      fragColor = vec4(color, alpha);
    }
  `;

  // ── Compile & Link ─────────────────────────────────────────────────────────
  function compileShader(type: number, src: string): WebGLShader | null {
    const s = gl!.createShader(type);
    if (!s) {
      return null;
    }
    gl!.shaderSource(s, src);
    gl!.compileShader(s);
    if (!gl!.getShaderParameter(s, gl!.COMPILE_STATUS)) {
      console.error(gl!.getShaderInfoLog(s));
      gl!.deleteShader(s);
      return null;
    }
    return s;
  }

  const vs = compileShader(gl.VERTEX_SHADER, vertexSrc);
  const fs = compileShader(gl.FRAGMENT_SHADER, fragmentSrc);
  if (!vs || !fs) {
    return;
  }

  const program = gl.createProgram()!;
  gl.attachShader(program, vs);
  gl.attachShader(program, fs);
  gl.linkProgram(program);
  if (!gl.getProgramParameter(program, gl.LINK_STATUS)) {
    console.error(gl.getProgramInfoLog(program));
    return;
  }
  gl.useProgram(program);

  // ── Fullscreen quad (two triangles) ────────────────────────────────────────
  const vao = gl.createVertexArray();
  gl.bindVertexArray(vao);
  const buf = gl.createBuffer();
  gl.bindBuffer(gl.ARRAY_BUFFER, buf);
  // prettier-ignore
  gl.bufferData(gl.ARRAY_BUFFER, new Float32Array([
    -1, -1,  1, -1,  -1, 1,
    -1,  1,  1, -1,   1, 1,
  ]), gl.STATIC_DRAW);
  const aPos = gl.getAttribLocation(program, 'a_position');
  gl.enableVertexAttribArray(aPos);
  gl.vertexAttribPointer(aPos, 2, gl.FLOAT, false, 0, 0);

  // ── Text Texture ───────────────────────────────────────────────────────────
  const heroText = container.dataset.heroText || '';

  function createTextTexture(text: string): WebGLTexture | null {
    const tex = gl!.createTexture();
    gl!.activeTexture(gl!.TEXTURE0);
    gl!.bindTexture(gl!.TEXTURE_2D, tex);

    const size = 512;
    const offscreen = document.createElement('canvas');
    offscreen.width = size;
    offscreen.height = size;
    const ctx2d = offscreen.getContext('2d')!;

    // Black background, white text
    ctx2d.fillStyle = '#000';
    ctx2d.fillRect(0, 0, size, size);

    if (text) {
      ctx2d.fillStyle = '#fff';
      ctx2d.textAlign = 'center';
      ctx2d.textBaseline = 'middle';
      // Scale font to fit width
      const fontSize = Math.min((size * 0.8) / (text.length * 0.55), size * 0.4);
      ctx2d.font = `bold ${fontSize}px monospace`;
      ctx2d.fillText(text, size / 2, size / 2);
    }

    gl!.texImage2D(gl!.TEXTURE_2D, 0, gl!.RGBA, gl!.RGBA, gl!.UNSIGNED_BYTE, offscreen);
    gl!.texParameteri(gl!.TEXTURE_2D, gl!.TEXTURE_MIN_FILTER, gl!.LINEAR);
    gl!.texParameteri(gl!.TEXTURE_2D, gl!.TEXTURE_MAG_FILTER, gl!.LINEAR);
    gl!.texParameteri(gl!.TEXTURE_2D, gl!.TEXTURE_WRAP_S, gl!.CLAMP_TO_EDGE);
    gl!.texParameteri(gl!.TEXTURE_2D, gl!.TEXTURE_WRAP_T, gl!.CLAMP_TO_EDGE);

    return tex;
  }

  createTextTexture(heroText);

  // ── Uniform locations ──────────────────────────────────────────────────────
  const loc = {
    u_time: gl.getUniformLocation(program, 'u_time'),
    u_resolution: gl.getUniformLocation(program, 'u_resolution'),
    u_mouse_rot: gl.getUniformLocation(program, 'u_mouse_rot'),
    u_zoom: gl.getUniformLocation(program, 'u_zoom'),
    u_perspective: gl.getUniformLocation(program, 'u_perspective'),
    u_anim_speed: gl.getUniformLocation(program, 'u_anim_speed'),
    u_shape_size: gl.getUniformLocation(program, 'u_shape_size'),
    u_iterations: gl.getUniformLocation(program, 'u_iterations'),
    u_fold: gl.getUniformLocation(program, 'u_fold'),
    u_glow_intensity: gl.getUniformLocation(program, 'u_glow_intensity'),
    u_glow_base: gl.getUniformLocation(program, 'u_glow_base'),
    u_textTex: gl.getUniformLocation(program, 'u_textTex'),
    u_hasText: gl.getUniformLocation(program, 'u_hasText'),
  };

  // Set static uniforms once
  gl.uniform1f(loc.u_perspective, 2.2);
  gl.uniform1f(loc.u_anim_speed, 0.15);
  gl.uniform1f(loc.u_shape_size, 1.6);
  gl.uniform1i(loc.u_iterations, 5);
  gl.uniform3f(loc.u_fold, 1.7, 0.5, 0.7);
  gl.uniform1f(loc.u_glow_intensity, 0.028);
  gl.uniform1f(loc.u_glow_base, 0.282);
  gl.uniform1i(loc.u_textTex, 0);
  gl.uniform1f(loc.u_hasText, heroText ? 1.0 : 0.0);

  // Enable alpha blending
  gl.enable(gl.BLEND);
  gl.blendFunc(gl.SRC_ALPHA, gl.ONE_MINUS_SRC_ALPHA);
  gl.clearColor(0, 0, 0, 0);

  // ── Drag-to-rotate ────────────────────────────────────────────────────────
  let isDragging = false;
  const prevPos = { x: 0, y: 0 };
  const targetRot = { x: 0, y: 0 };
  const currentRot = { x: 0, y: 0 };

  // ── Device-tilt sway + shake wiggle (mobile) ──────────────────────────────
  const orientTilt = { x: 0, y: 0 };
  let wiggleStart = -1;
  let wiggleAmp = 0;

  function triggerWiggle(strength: number): void {
    wiggleStart = performance.now();
    wiggleAmp = Math.min(0.6, strength);
  }

  // ── Scroll-to-zoom ────────────────────────────────────────────────────────
  let zoom = 5.0;
  let targetZoom = 5.0;
  const ZOOM_MIN = 3.0;
  const ZOOM_MAX = 14.0;

  canvas.addEventListener('mousedown', (e: MouseEvent) => {
    isDragging = true;
    prevPos.x = e.offsetX;
    prevPos.y = e.offsetY;
  });

  window.addEventListener('mouseup', () => {
    isDragging = false;
  });

  window.addEventListener('mousemove', (e: MouseEvent) => {
    if (!isDragging) {
      return;
    }
    const rect = canvas.getBoundingClientRect();
    const x = e.clientX - rect.left;
    const y = e.clientY - rect.top;
    targetRot.x += (x - prevPos.x) * 0.01;
    targetRot.y -= (y - prevPos.y) * 0.01;
    prevPos.x = x;
    prevPos.y = y;
  });

  canvas.addEventListener(
    'wheel',
    (e: WheelEvent) => {
      e.preventDefault();
      targetZoom = Math.min(ZOOM_MAX, Math.max(ZOOM_MIN, targetZoom + e.deltaY * 0.01));
    },
    { passive: false }
  );

  // ── Desktop fallback: double-click to wiggle (no motion sensor needed) ──────
  canvas.addEventListener('dblclick', (e: MouseEvent) => {
    e.preventDefault();
    triggerWiggle(0.5);
  });

  // ── Touch: single-finger drag to rotate, two-finger pinch to zoom ──────────
  let pinchDist = 0;

  function touchDistance(touches: TouchList): number {
    const dx = touches[0].clientX - touches[1].clientX;
    const dy = touches[0].clientY - touches[1].clientY;
    return Math.hypot(dx, dy);
  }

  function beginDragFromTouch(touch: Touch): void {
    const rect = canvas.getBoundingClientRect();
    isDragging = true;
    prevPos.x = touch.clientX - rect.left;
    prevPos.y = touch.clientY - rect.top;
  }

  canvas.addEventListener(
    'touchstart',
    (e: TouchEvent) => {
      // First touch is a user gesture — request motion-sensor permission (iOS).
      void enableMotion();
      if (e.touches.length === 1) {
        beginDragFromTouch(e.touches[0]);
      } else if (e.touches.length === 2) {
        isDragging = false;
        pinchDist = touchDistance(e.touches);
      }
    },
    { passive: true }
  );

  canvas.addEventListener(
    'touchmove',
    (e: TouchEvent) => {
      if (e.touches.length === 1 && isDragging) {
        const rect = canvas.getBoundingClientRect();
        const x = e.touches[0].clientX - rect.left;
        const y = e.touches[0].clientY - rect.top;
        targetRot.x += (x - prevPos.x) * 0.01;
        targetRot.y -= (y - prevPos.y) * 0.01;
        prevPos.x = x;
        prevPos.y = y;
        e.preventDefault();
      } else if (e.touches.length === 2) {
        const d = touchDistance(e.touches);
        if (pinchDist > 0) {
          targetZoom = Math.min(ZOOM_MAX, Math.max(ZOOM_MIN, targetZoom + (pinchDist - d) * 0.02));
        }
        pinchDist = d;
        e.preventDefault();
      }
    },
    { passive: false }
  );

  canvas.addEventListener(
    'touchend',
    (e: TouchEvent) => {
      if (e.touches.length === 0) {
        isDragging = false;
        pinchDist = 0;
      } else if (e.touches.length === 1) {
        // Lifted from a pinch back to a single finger — resume dragging.
        pinchDist = 0;
        beginDragFromTouch(e.touches[0]);
      }
    },
    { passive: true }
  );

  // ── Device motion: tilt-to-sway + shake-to-wiggle (mobile only) ────────────
  // Sensor events only fire in a secure context (HTTPS or localhost on-device),
  // and iOS 13+ requires an explicit permission grant from a user gesture.
  let motionEnabled = false;
  let orientBaseline: { beta: number; gamma: number } | null = null;
  let lastShake = 0;

  function handleOrientation(e: DeviceOrientationEvent): void {
    if (e.beta === null || e.gamma === null) {
      return;
    }
    // Anchor to the pose the phone was in when sensing started.
    if (orientBaseline === null) {
      orientBaseline = { beta: e.beta, gamma: e.gamma };
    }
    const dGamma = e.gamma - orientBaseline.gamma; // left-right tilt
    const dBeta = e.beta - orientBaseline.beta; //   front-back tilt
    // Gentle sway, clamped so a big tilt can't fling the fractal off-screen.
    orientTilt.x = Math.max(-0.6, Math.min(0.6, dGamma * 0.012));
    orientTilt.y = Math.max(-0.6, Math.min(0.6, dBeta * 0.012));
  }

  function handleMotion(e: DeviceMotionEvent): void {
    // Prefer gravity-free acceleration; fall back to the with-gravity variant.
    const pure = e.acceleration;
    const acc = pure && pure.x !== null ? pure : e.accelerationIncludingGravity;
    if (!acc) {
      return;
    }
    const mag = Math.hypot(acc.x ?? 0, acc.y ?? 0, acc.z ?? 0);
    // ~9.8 baseline when gravity is included, ~0 otherwise.
    const threshold = pure && pure.x !== null ? 16 : 26;
    const now = performance.now();
    if (mag > threshold && now - lastShake > 600) {
      lastShake = now;
      triggerWiggle(0.45);
    }
  }

  function attachMotionListeners(): void {
    window.addEventListener('deviceorientation', handleOrientation);
    window.addEventListener('devicemotion', handleMotion);
  }

  /** True when the platform gates the sensors behind an explicit permission grant (iOS 13+). */
  function motionNeedsPermission(): boolean {
    const orientationCtor = window.DeviceOrientationEvent as unknown as
      | MotionPermission
      | undefined;
    const motionCtor = window.DeviceMotionEvent as unknown as MotionPermission | undefined;
    return (
      (!!orientationCtor && typeof orientationCtor.requestPermission === 'function') ||
      (!!motionCtor && typeof motionCtor.requestPermission === 'function')
    );
  }

  async function enableMotion(): Promise<void> {
    if (motionEnabled) {
      return;
    }
    motionEnabled = true;
    const orientationCtor = window.DeviceOrientationEvent as unknown as
      | MotionPermission
      | undefined;
    const motionCtor = window.DeviceMotionEvent as unknown as MotionPermission | undefined;
    try {
      if (orientationCtor && typeof orientationCtor.requestPermission === 'function') {
        const granted = await orientationCtor.requestPermission();
        if (granted !== 'granted') {
          motionEnabled = false;
          return;
        }
      }
      if (motionCtor && typeof motionCtor.requestPermission === 'function') {
        await motionCtor.requestPermission().catch(() => 'denied');
      }
    } catch {
      // Permission API rejected (e.g. not a user gesture) — leave motion off.
      motionEnabled = false;
      return;
    }
    attachMotionListeners();
  }

  // Platforms without a permission gate (most Android) can sense immediately —
  // tilt-to-sway and shake-to-wiggle work from the start, no touch required.
  // iOS defers until the first touch gesture (see the canvas 'touchstart' handler).
  if (!motionNeedsPermission()) {
    motionEnabled = true;
    attachMotionListeners();
  }

  // ── Resize ────────────────────────────────────────────────────────────────
  function onResize(): void {
    const w = container.clientWidth;
    const h = container.clientHeight;
    canvas.width = w;
    canvas.height = h;
    gl!.viewport(0, 0, w, h);
  }

  window.addEventListener('resize', onResize);

  // ── Animation loop ────────────────────────────────────────────────────────
  const startTime = performance.now();

  function animate(): void {
    requestAnimationFrame(animate);

    const time = (performance.now() - startTime) / 1000;

    // Lerp rotation & zoom
    currentRot.x += (targetRot.x - currentRot.x) * 0.08;
    currentRot.y += (targetRot.y - currentRot.y) * 0.08;
    zoom += (targetZoom - zoom) * 0.08;

    // Shake-triggered wiggle: a decaying oscillation on top of the base rotation.
    let wiggleX = 0;
    let wiggleY = 0;
    if (wiggleStart >= 0) {
      const wt = (performance.now() - wiggleStart) / 1000;
      const envelope = Math.exp(-6 * wt);
      if (envelope < 0.01) {
        wiggleStart = -1;
      } else {
        const w = wiggleAmp * envelope * Math.sin(wt * 42);
        wiggleX = w;
        wiggleY = w * 0.5;
      }
    }

    // Set dynamic uniforms
    gl!.uniform1f(loc.u_time, time);
    gl!.uniform2f(loc.u_resolution, canvas.width, canvas.height);
    gl!.uniform2f(
      loc.u_mouse_rot,
      currentRot.x + orientTilt.x + wiggleX,
      currentRot.y + orientTilt.y + wiggleY
    );
    gl!.uniform1f(loc.u_zoom, zoom);

    // Draw
    gl!.clear(gl!.COLOR_BUFFER_BIT);
    gl!.drawArrays(gl!.TRIANGLES, 0, 6);
  }

  gl.viewport(0, 0, canvas.width, canvas.height);
  animate();
}
