/**
 * hero3d.ts — Raymarched fractal tetrahedron for the hero section
 *
 * A full-screen shader quad using GLSL raymarching to render a fractal
 * tetrahedron with volumetric glow.
 * Inspired by: https://codepen.io/sabosugi/pen/jEVPrOx
 *
 * Drag on the hero canvas to rotate the fractal.
 */

import * as THREE from 'three';

export function initHero3D(): void {
  const maybeContainer = document.querySelector<HTMLElement>('.hero-3d-container');
  if (!maybeContainer) {
    return;
  }
  // Re-assign to an explicitly typed const so the non-null type carries into closures.
  const container: HTMLElement = maybeContainer;

  // ── Scene & Camera ────────────────────────────────────────────────────────
  // Orthographic camera: the shader handles its own perspective internally.
  const scene = new THREE.Scene();
  const camera = new THREE.OrthographicCamera(-1, 1, 1, -1, 0, 1);

  // ── Renderer ──────────────────────────────────────────────────────────────
  const renderer = new THREE.WebGLRenderer({ antialias: true, alpha: true });
  renderer.setPixelRatio(1.0); // keep at 1× for hero-section performance
  renderer.setClearColor(0x000000, 0); // fully transparent clear
  renderer.setSize(container.clientWidth, container.clientHeight);
  container.appendChild(renderer.domElement);

  // ── Uniforms ───────────────────────────────────────────────────────────────
  const uniforms = {
    u_time: { value: 0.0 },
    u_resolution: {
      value: new THREE.Vector2(container.clientWidth, container.clientHeight),
    },
    u_mouse_rot: { value: new THREE.Vector2(0.0, 0.0) },
    u_zoom: { value: 5.0 },
    u_perspective: { value: 2.2 },
    u_anim_speed: { value: 0.15 },
    u_shape_size: { value: 1.6 },
    u_iterations: { value: 5 },
    u_fold: { value: new THREE.Vector3(1.7, 0.5, 0.7) },
    u_glow_intensity: { value: 0.028 },
    u_glow_base: { value: 0.282 },
  };

  // ── Shaders ────────────────────────────────────────────────────────────────
  const vertexShader = /* glsl */ `
    void main() {
      gl_Position = vec4(position, 1.0);
    }
  `;

  const fragmentShader = /* glsl */ `
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
      return finalColor * finalColor; // contrast boost
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
      gl_FragColor = vec4(color, alpha);
    }
  `;

  // ── Full-screen quad ───────────────────────────────────────────────────────
  const geometry = new THREE.PlaneGeometry(2, 2);
  const material = new THREE.ShaderMaterial({ vertexShader, fragmentShader, uniforms });
  scene.add(new THREE.Mesh(geometry, material));

  // ── Drag-to-rotate ────────────────────────────────────────────────────────
  let isDragging = false;
  const prevPos = { x: 0, y: 0 };
  const targetRot = new THREE.Vector2(0, 0);
  const currentRot = new THREE.Vector2(0, 0);

  // ── Scroll-to-zoom ────────────────────────────────────────────────────────
  let targetZoom = uniforms.u_zoom.value;
  const ZOOM_MIN = 3.0;
  const ZOOM_MAX = 14.0;

  const canvas = renderer.domElement;

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

  // ── Resize ────────────────────────────────────────────────────────────────
  function onResize(): void {
    const w = container.clientWidth;
    const h = container.clientHeight;
    renderer.setSize(w, h);
    uniforms.u_resolution.value.set(w, h);
  }

  window.addEventListener('resize', onResize);

  // ── Animation loop ────────────────────────────────────────────────────────
  const startTime = performance.now();

  function animate(): void {
    requestAnimationFrame(animate);
    uniforms.u_time.value = (performance.now() - startTime) / 1000;
    currentRot.lerp(targetRot, 0.08);
    uniforms.u_mouse_rot.value.copy(currentRot);
    uniforms.u_zoom.value += (targetZoom - uniforms.u_zoom.value) * 0.08;
    renderer.render(scene, camera);
  }

  animate();
}
