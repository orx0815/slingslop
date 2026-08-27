/* DML picker widget — vanilla JS, no framework dependency.
 *
 * Wires up:
 *   - "Pick image…" button → opens a <dialog>, fetches the asset grid via
 *     the existing DML `.assets.html` selector, listens for clicks on
 *     `.dml-asset-item`, writes the chosen path into the hidden input,
 *     refreshes the thumbnail.
 *   - "Clear" button → clears the hidden input + thumbnail.
 *   - Focus-point dot → draggable inside the preview image. On release
 *     POSTs to `{assetPath}.focus-point.json` so any other component using
 *     the same asset picks up the new focus.
 *
 * Self-contained: idempotent init guarded by a global flag; safe to ship via
 * HTL on every HTMX swap.
 */
(function () {
  'use strict';

  if (window.__dmaPickerInit) {
    rescan();
    return;
  }
  window.__dmaPickerInit = true;

  document.addEventListener('click', onDocClick);
  document.addEventListener('htmx:afterSwap', rescan);
  document.addEventListener('DOMContentLoaded', rescan);
  rescan();

  function rescan() {
    document.querySelectorAll('.dma-picker').forEach(initPicker);
  }

  function initPicker(root) {
    if (root.__dmaInit) return;
    root.__dmaInit = true;

    const ratioStr = root.dataset.ratio || '4:3';
    const ratio = parseRatio(ratioStr);
    setupFocusStage(root, ratio);
  }

  function parseRatio(s) {
    const [w, h] = s.split(':').map(Number);
    if (!w || !h) return 4 / 3;
    return w / h;
  }

  function onDocClick(ev) {
    const open = ev.target.closest('.dma-picker-open');
    const close = ev.target.closest('.dma-picker-close');
    const clear = ev.target.closest('.dma-picker-clear');
    if (open) {
      ev.preventDefault();
      openDialog(open.closest('.dma-picker'));
    } else if (close) {
      ev.preventDefault();
      const dlg = close.closest('dialog');
      if (dlg && dlg.open) dlg.close();
    } else if (clear) {
      ev.preventDefault();
      const picker = clear.closest('.dma-picker');
      setSelection(picker, '');
    }

    const card = ev.target.closest('.dma-picker-grid .dml-asset-item');
    if (card) {
      ev.preventDefault();
      const picker = card.closest('.dma-picker');
      const path = card.dataset.assetPath || card.dataset.assetId;
      setSelection(picker, path);
      const dlg = picker.querySelector('.dma-picker-dialog');
      if (dlg && dlg.open) dlg.close();
      return;
    }

    // Folder navigation: folder cards, "up" card and breadcrumb segments all
    // carry data-folder-path and re-render the grid without closing the dialog.
    const nav = ev.target.closest(
      '.dma-picker-grid .dml-folder-item, .dma-picker-grid .dml-picker-crumb'
    );
    if (nav && nav.dataset.folderPath) {
      ev.preventDefault();
      const picker = nav.closest('.dma-picker');
      loadFolder(picker, nav.dataset.folderPath);
    }
  }

  function listEndpointFor(picker) {
    return (
      picker.dataset.assetsListUrl ||
      '/content/motorbrot/dml/home/_jcr_content.assets.html'
    );
  }

  function loadFolder(picker, folder) {
    const dlg = picker.querySelector('.dma-picker-dialog');
    const grid = dlg.querySelector('.dma-picker-grid');
    grid.innerHTML = '<p class="dma-picker-loading">Loading…</p>';

    const root = picker.dataset.assetsRoot || '/content/motorbrot/dml/assets';
    const current = folder || root;
    picker.dataset.currentFolder = current;

    // The DML dashboard's "assets" selector renders the picker grid. Its script
    // is registered against the dashboard resource type, so we hit the
    // dashboard's jcr:content directly. includeFolders=true adds folder cards +
    // breadcrumbs; root bounds the "up"/breadcrumb navigation.
    const url =
      listEndpointFor(picker) +
      '?includeFolders=true' +
      '&folder=' + encodeURIComponent(current) +
      '&root=' + encodeURIComponent(root);
    fetch(url, { credentials: 'same-origin' })
      .then((r) => (r.ok ? r.text() : Promise.reject(r.status)))
      .then((html) => {
        grid.innerHTML = html;
      })
      .catch((err) => {
        grid.innerHTML =
          '<p class="dma-picker-loading">Could not load asset list (' + err + ')</p>';
      });
  }

  function openDialog(picker) {
    const dlg = picker.querySelector('.dma-picker-dialog');
    dlg.showModal();
    const root = picker.dataset.assetsRoot || '/content/motorbrot/dml/assets';
    loadFolder(picker, picker.dataset.currentFolder || root);
  }

  function setSelection(picker, path) {
    const input = picker.querySelector('.dma-picker-value');
    const thumb = picker.querySelector('.dma-picker-thumb');
    const img = picker.querySelector('.dma-picker-thumb-img');
    const placeholder = picker.querySelector('.dma-picker-thumb-placeholder');

    if (input) input.value = path || '';
    // Focus is per-placement: a freshly picked image starts centred so the
    // crop boxes and the saved dmaFocusX/Y match the new source.
    const fx = picker.querySelector('.dma-picker-focus-x');
    const fy = picker.querySelector('.dma-picker-focus-y');
    if (fx) fx.value = '50';
    if (fy) fy.value = '50';
    if (thumb) thumb.dataset.empty = path ? 'false' : 'true';
    if (img) {
      if (path) {
        img.src = path + '/renditions/preview';
        img.style.display = '';
      } else {
        img.removeAttribute('src');
        img.style.display = 'none';
      }
    }
    if (placeholder) {
      placeholder.style.display = path ? 'none' : '';
    }

    // Replace the focus section so the dragger snaps to the new image.
    const oldFocus = picker.querySelector('.dma-focus-section');
    if (oldFocus) oldFocus.remove();
    if (path) {
      const sect = renderFocusSection(picker, path);
      picker.appendChild(sect);
      const ratio = parseRatio(picker.dataset.ratio || '4:3');
      setupFocusStage(picker, ratio);
    }
  }

  function renderFocusSection(picker, assetPath) {
    const ratio = picker.dataset.ratio || '4:3';
    const sect = document.createElement('div');
    sect.className = 'dma-focus-section';
    sect.innerHTML =
      '<div class="edit-field-row">' +
      '  <span class="edit-field-meta">' +
      '    <span class="edit-field-label">Focus point</span>' +
      '    <small class="edit-field-description">' +
      '      Drag the dot to mark the most important point.' +
      '    </small>' +
      '  </span>' +
      '  <div class="dma-focus-stage" data-ratio="' + ratio + '">' +
      '    <img class="dma-focus-image" src="' + assetPath + '/renditions/preview" alt=""/>' +
      '    <div class="dma-focus-crop"></div>' +
      '    <div class="dma-focus-dot"></div>' +
      '  </div>' +
      '</div>' +
      '<div class="dma-picker-crop" hidden>' +
      '  <p class="dma-picker-crop-hint">The picked image is too small for the format(s) below. ' +
      'A focus-cropped rendition will be saved on this component for each:</p>' +
      '  <ul class="dma-picker-crop-list"></ul>' +
      '  <div class="dma-picker-crop-actions">' +
      '    <button type="button" class="dma-picker-crop-generate">Generate cropped rendition(s)</button>' +
      '    <span class="dma-picker-crop-result" aria-live="polite"></span>' +
      '  </div>' +
      '</div>';
    return sect;
  }

  function setupFocusStage(picker, ratio) {
    const stage = picker.querySelector('.dma-focus-stage');
    if (!stage || stage.__dmaInit) return;
    stage.__dmaInit = true;

    const img = stage.querySelector('.dma-focus-image');
    const dot = stage.querySelector('.dma-focus-dot');
    const defaultBox = stage.querySelector('.dma-focus-crop');
    if (!img || !dot) return;

    const cropFormats = readCropFormats(picker);
    const componentPath = picker.dataset.componentPath || '';
    const focusXInput = picker.querySelector('.dma-picker-focus-x');
    const focusYInput = picker.querySelector('.dma-picker-focus-y');
    const cropBlock = picker.querySelector('.dma-picker-crop');
    const cropList = picker.querySelector('.dma-picker-crop-list');
    const cropResult = picker.querySelector('.dma-picker-crop-result');
    const cropBtn = picker.querySelector('.dma-picker-crop-generate');

    const initial = readFocusFromPicker(picker);
    let boxes = defaultBox ? [defaultBox] : [];
    let needs = [];
    // The focus preview is a small rendition; crop decisions must use the
    // REAL source dimensions (from asset metadata) so the modal matches the
    // server-side postprocessor. Falls back to the preview size until loaded.
    let sourceDims = { w: 0, h: 0 };

    function loadSourceDims() {
      const p = pickerAssetPath(picker);
      if (!p) {
        sourceDims = { w: 0, h: 0 };
        return Promise.resolve();
      }
      return fetch(p + '/metadata.json', { credentials: 'same-origin' })
        .then((r) => (r.ok ? r.json() : null))
        .then((j) => {
          const w = j ? parseInt(j.width, 10) : 0;
          const h = j ? parseInt(j.height, 10) : 0;
          if (w && h) sourceDims = { w: w, h: h };
        })
        .catch(() => {});
    }

    function syncInputs(xPct, yPct) {
      const rx = Math.round(xPct * 100) / 100;
      const ry = Math.round(yPct * 100) / 100;
      if (focusXInput) focusXInput.value = String(rx);
      if (focusYInput) focusYInput.value = String(ry);
    }

    // Mirror of RenditionValidator: can the source satisfy this format?
    function fits(f, w, h) {
      if (f.maintain) return !(w < f.w && h < f.h);
      const targetRatio = f.w / f.h;
      const srcRatio = w / h;
      let cw, ch;
      if (srcRatio > targetRatio) { ch = h; cw = ch * targetRatio; }
      else { cw = w; ch = cw / targetRatio; }
      return cw >= f.w && ch >= f.h;
    }

    function rebuildBoxes() {
      // Remove previously generated per-format boxes (keep the default one).
      boxes.forEach((b) => { if (b !== defaultBox) b.remove(); });
      if (!cropFormats.length) {
        boxes = defaultBox ? [defaultBox] : [];
        needs = [];
        updateCropUi();
        return;
      }
      if (defaultBox) defaultBox.style.display = 'none';
      const sw = sourceDims.w || img.naturalWidth;
      const sh = sourceDims.h || img.naturalHeight;
      needs = (sw && sh)
        ? cropFormats.filter((f) => !fits(f, sw, sh))
        : [];
      boxes = needs.map((f, i) => {
        const b = document.createElement('div');
        b.className = 'dma-focus-crop';
        b.dataset.format = f.name;
        b.dataset.w = f.w;
        b.dataset.h = f.h;
        b.style.setProperty('--crop-idx', String(i));
        stage.appendChild(b);
        return b;
      });
      updateCropUi();
    }

    function updateCropUi() {
      if (!cropBlock) return;
      if (needs.length) {
        cropBlock.hidden = false;
        if (cropList) {
          cropList.innerHTML = needs
            .map((f) => '<li>' + escapeHtml(f.label) + ' (' + f.w + '\u00d7' + f.h + ')</li>')
            .join('');
        }
      } else {
        cropBlock.hidden = true;
        if (cropList) cropList.innerHTML = '';
      }
      if (cropResult) cropResult.textContent = '';
    }

    function boxRatio(box) {
      const w = parseFloat(box.dataset.w);
      const h = parseFloat(box.dataset.h);
      if (w && h) return w / h;
      return ratio;
    }

    function placeBox(box, xPct, yPct) {
      const rect = stage.getBoundingClientRect();
      if (!rect.width || !rect.height) return;
      const r = boxRatio(box);
      const srcRatio = rect.width / rect.height;
      let cw, ch;
      if (srcRatio > r) { ch = rect.height; cw = ch * r; }
      else { cw = rect.width; ch = cw / r; }
      const fx = (xPct / 100) * rect.width;
      const fy = (yPct / 100) * rect.height;
      box.style.left = clamp(fx - cw / 2, 0, rect.width - cw) + 'px';
      box.style.top = clamp(fy - ch / 2, 0, rect.height - ch) + 'px';
      box.style.width = cw + 'px';
      box.style.height = ch + 'px';
    }

    function placeAt(xPct, yPct) {
      const x = clamp(xPct, 0, 100);
      const y = clamp(yPct, 0, 100);
      dot.style.left = x + '%';
      dot.style.top = y + '%';
      boxes.forEach((b) => placeBox(b, x, y));
      syncInputs(x, y);
    }

    function applyStageAspect() {
      // The stage must carry the SELECTED ASSET's aspect ratio so the preview
      // is never letterboxed or distorted, and the crop boxes map 1:1 onto the
      // source pixels the server will crop.
      if (img.naturalWidth && img.naturalHeight) {
        stage.style.aspectRatio = img.naturalWidth + ' / ' + img.naturalHeight;
      }
    }

    function applyInitial() {
      applyStageAspect();
      rebuildBoxes();
      placeAt(initial.x, initial.y);
      // Refine crop decisions once the real source dimensions are known.
      loadSourceDims().then(() => {
        rebuildBoxes();
        placeAt(currentPct().x, currentPct().y);
      });
    }
    if (img.complete && img.naturalWidth) applyInitial();
    else img.addEventListener('load', applyInitial);
    window.addEventListener('resize', () => placeAt(currentPct().x, currentPct().y));
    // The stage may have zero size while the edit modal is still hidden, which
    // makes the initial pixel-based box placement a no-op. Reposition the boxes
    // whenever the stage gains (or changes) its rendered size.
    if (typeof ResizeObserver !== 'undefined') {
      const ro = new ResizeObserver(() => {
        const p = currentPct();
        placeAt(p.x, p.y);
      });
      ro.observe(stage);
    }

    function currentPct() {
      return {
        x: parseFloat(dot.style.left) || 50,
        y: parseFloat(dot.style.top) || 50,
      };
    }

    let dragging = false;
    dot.addEventListener('pointerdown', (ev) => {
      dragging = true;
      dot.setPointerCapture(ev.pointerId);
      dot.classList.add('is-dragging');
    });
    dot.addEventListener('pointermove', (ev) => {
      if (!dragging) return;
      const rect = stage.getBoundingClientRect();
      const xPct = clamp(((ev.clientX - rect.left) / rect.width) * 100, 0, 100);
      const yPct = clamp(((ev.clientY - rect.top) / rect.height) * 100, 0, 100);
      placeAt(xPct, yPct);
    });
    dot.addEventListener('pointerup', (ev) => {
      if (!dragging) return;
      dragging = false;
      dot.classList.remove('is-dragging');
      dot.releasePointerCapture(ev.pointerId);
    });

    if (cropBtn) {
      cropBtn.addEventListener('click', () => {
        if (!componentPath || !needs.length) return;
        const fileReference = pickerAssetPath(picker);
        if (!fileReference) return;
        const p = currentPct();
        const fd = new FormData();
        fd.set('fileReference', fileReference);
        fd.set('focusX', String(p.x));
        fd.set('focusY', String(p.y));
        needs.forEach((f) => fd.append('format', f.name));

        cropBtn.disabled = true;
        const label = cropBtn.textContent;
        cropBtn.textContent = 'Generating…';
        if (cropResult) cropResult.textContent = '';

        fetch(componentPath + '.crop-rendition.html', {
          method: 'POST',
          credentials: 'same-origin',
          body: fd,
        })
          .then((r) => (r.ok ? r.text() : Promise.reject(r.status)))
          .then(() => {
            cropBtn.disabled = false;
            cropBtn.textContent = label;
            if (cropResult) {
              cropResult.textContent =
                needs.length + ' cropped rendition(s) saved for this component.';
            }
          })
          .catch((err) => {
            cropBtn.disabled = false;
            cropBtn.textContent = label;
            if (cropResult) cropResult.textContent = 'Could not generate: ' + err;
          });
      });
    }
  }

  function readCropFormats(picker) {
    return Array.prototype.slice
      .call(picker.querySelectorAll('.dma-picker-cropfmt'))
      .map((el) => ({
        name: el.dataset.format,
        label: el.dataset.label || el.dataset.format,
        w: parseFloat(el.dataset.w) || 0,
        h: parseFloat(el.dataset.h) || 0,
        maintain: el.dataset.maintain === 'true',
      }))
      .filter((f) => f.name && f.w && f.h);
  }

  function escapeHtml(s) {
    return String(s).replace(/[&<>"]/g, (c) => {
      return { '&': '&amp;', '<': '&lt;', '>': '&gt;', '"': '&quot;' }[c];
    });
  }

  function pickerAssetPath(picker) {
    const input = picker.querySelector('.dma-picker-value');
    return input ? input.value : '';
  }

  function readFocusFromPicker(picker) {
    const fx = picker.querySelector('.dma-picker-focus-x');
    const fy = picker.querySelector('.dma-picker-focus-y');
    const x = fx ? parseFloat(fx.value) : NaN;
    const y = fy ? parseFloat(fy.value) : NaN;
    return { x: isNaN(x) ? 50 : x, y: isNaN(y) ? 50 : y };
  }

  function clamp(v, lo, hi) {
    return Math.max(lo, Math.min(hi, v));
  }
})();
