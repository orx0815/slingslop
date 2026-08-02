/* Focus-point crop editor for the DML-client responsive-image component.
 *
 * Shown to logged-in editors when one or more configured MediaFormats are
 * invalid for the shared DML asset (source too small). It lets the editor:
 *   - drag a focus dot over a preview of the ORIGINAL image (the stage keeps
 *     the original image's aspect ratio),
 *   - see one dashed crop box per invalid MediaFormat, each with that format's
 *     aspect ratio, all moving with the focus dot,
 *   - POST {componentPath}.crop-rendition.html to generate one component-local
 *     cropped rendition per invalid format (saved under the component, not the
 *     shared DML asset), together with the focus point.
 *
 * Vanilla JS, idempotent, HTMX-swap safe.
 */
(function () {
  'use strict';

  if (window.__dmaCropEditorInit) {
    rescan();
    return;
  }
  window.__dmaCropEditorInit = true;

  document.addEventListener('htmx:afterSwap', rescan);
  document.addEventListener('DOMContentLoaded', rescan);
  rescan();

  function rescan() {
    document.querySelectorAll('.dma-crop-editor').forEach(initEditor);
  }

  function clamp(v, lo, hi) {
    return Math.max(lo, Math.min(hi, v));
  }

  function initEditor(root) {
    if (root.__dmaInit) return;
    root.__dmaInit = true;

    const stage = root.querySelector('.dma-focus-stage');
    const img = root.querySelector('.dma-focus-image');
    const dot = root.querySelector('.dma-focus-dot');
    const boxes = Array.prototype.slice.call(root.querySelectorAll('.dma-focus-crop'));
    if (!stage || !img || !dot) return;

    const sourceW = parseFloat(root.dataset.sourceWidth) || 0;
    const sourceH = parseFloat(root.dataset.sourceHeight) || 0;
    const componentPath = root.dataset.componentPath;

    // The stage must mirror the ORIGINAL image's aspect ratio so the crop boxes
    // map 1:1 onto the source pixels the server will crop.
    function applyStageAspect() {
      let w = sourceW;
      let h = sourceH;
      if ((!w || !h) && img.naturalWidth && img.naturalHeight) {
        w = img.naturalWidth;
        h = img.naturalHeight;
      }
      if (w && h) {
        stage.style.aspectRatio = w + ' / ' + h;
      }
    }

    let focus = {
      x: clampPct(parseFloat(root.dataset.focusX), 50),
      y: clampPct(parseFloat(root.dataset.focusY), 50),
    };

    function clampPct(v, dflt) {
      if (isNaN(v)) return dflt;
      return clamp(v, 0, 100);
    }

    function boxRatio(box) {
      const w = parseFloat(box.dataset.w);
      const h = parseFloat(box.dataset.h);
      if (!w || !h) return 1;
      return w / h;
    }

    function placeBox(box, xPct, yPct) {
      const rect = stage.getBoundingClientRect();
      if (!rect.width || !rect.height) return;
      const ratio = boxRatio(box);
      const srcRatio = rect.width / rect.height;
      let cw, ch;
      if (srcRatio > ratio) {
        ch = rect.height;
        cw = ch * ratio;
      } else {
        cw = rect.width;
        ch = cw / ratio;
      }
      const fx = (xPct / 100) * rect.width;
      const fy = (yPct / 100) * rect.height;
      const left = clamp(fx - cw / 2, 0, rect.width - cw);
      const top = clamp(fy - ch / 2, 0, rect.height - ch);
      box.style.left = left + 'px';
      box.style.top = top + 'px';
      box.style.width = cw + 'px';
      box.style.height = ch + 'px';
    }

    function place(xPct, yPct) {
      focus.x = clamp(xPct, 0, 100);
      focus.y = clamp(yPct, 0, 100);
      dot.style.left = focus.x + '%';
      dot.style.top = focus.y + '%';
      boxes.forEach((b) => placeBox(b, focus.x, focus.y));
    }

    function refresh() {
      applyStageAspect();
      place(focus.x, focus.y);
    }

    if (img.complete) refresh();
    img.addEventListener('load', refresh);
    window.addEventListener('resize', () => place(focus.x, focus.y));

    let dragging = false;
    dot.addEventListener('pointerdown', (ev) => {
      dragging = true;
      dot.setPointerCapture(ev.pointerId);
      dot.classList.add('is-dragging');
    });
    dot.addEventListener('pointermove', (ev) => {
      if (!dragging) return;
      const rect = stage.getBoundingClientRect();
      place(
        ((ev.clientX - rect.left) / rect.width) * 100,
        ((ev.clientY - rect.top) / rect.height) * 100
      );
    });
    dot.addEventListener('pointerup', (ev) => {
      if (!dragging) return;
      dragging = false;
      dot.classList.remove('is-dragging');
      dot.releasePointerCapture(ev.pointerId);
    });

    const btn = root.querySelector('.dma-crop-generate');
    if (btn) {
      btn.addEventListener('click', () => generate(root, componentPath, focus, btn));
    }
  }

  function generate(root, componentPath, focus, btn) {
    if (!componentPath) return;
    const formats = Array.prototype.slice
      .call(root.querySelectorAll('.dma-crop-format'))
      .map((el) => el.value)
      .filter(Boolean);
    if (!formats.length) return;

    const fd = new FormData();
    fd.set('focusX', String(focus.x));
    fd.set('focusY', String(focus.y));
    formats.forEach((f) => fd.append('format', f));

    btn.disabled = true;
    const original = btn.textContent;
    btn.textContent = 'Generating…';

    fetch(componentPath + '.crop-rendition.html', {
      method: 'POST',
      credentials: 'same-origin',
      body: fd,
    })
      .then((r) => (r.ok ? r.text() : Promise.reject(r.status)))
      .then(() => {
        // Reload so the freshly cropped renditions are picked up server-side.
        window.location.reload();
      })
      .catch((err) => {
        btn.disabled = false;
        btn.textContent = original;
        // eslint-disable-next-line no-alert
        alert('Could not generate cropped rendition(s): ' + err);
      });
  }
})();
