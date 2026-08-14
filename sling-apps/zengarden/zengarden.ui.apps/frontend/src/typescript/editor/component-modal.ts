// ─── Modal visibility ─────────────────────────────────────────────────────

function unlockBodyScroll(): void {
  document.body.style.overflow = '';
}

export function showComponentModal(): void {
  mountEditorOverlay();
  const modal = document.getElementById('editor-component-modal');
  if (!modal) {
    return;
  }
  modal.classList.add('is-open');
  modal.setAttribute('aria-hidden', 'false');
  document.body.style.overflow = 'hidden';
  modal.querySelector<HTMLInputElement>('input, textarea, select')?.focus();
}

export function hideComponentModal(): void {
  const modal = document.getElementById('editor-component-modal');
  if (!modal) {
    unlockBodyScroll();
    return;
  }

  let isFinalized = false;
  const finalize = (): void => {
    if (isFinalized) {
      return;
    }
    isFinalized = true;
    modal.classList.remove('is-open', 'is-closing');
    modal.setAttribute('aria-hidden', 'true');
    unlockBodyScroll();
  };

  modal.classList.add('is-closing');
  modal.addEventListener(
    'animationend',
    () => {
      finalize();
    },
    { once: true }
  );

  // HTMX swaps may remove the modal before animationend fires.
  window.setTimeout(finalize, 300);
}

// ─── Editor overlay portalling ─────────────────────────────────────────────
// Every piece of fixed-position editor chrome (toolbar, bottom action bar,
// component modal, save-error dialog) is moved into #editor-modal-container
// -- a plain div directly under <body> -- the moment editing starts.
//
// This is NOT optional polish: `position: fixed` only anchors to the true
// viewport as long as NO ancestor establishes a new containing block (any
// ancestor with a transform/translate/scale/rotate/filter/perspective value
// other than none, or `will-change`/`contain` naming one of those). A host
// page has countless ways to do this -- deliberately (a hero parallax
// effect) or accidentally (a lingering `animation-fill-mode: both` that
// freezes a non-none `translate` after the animation ends). When that
// happens, "fixed" toolbars/footers collapse to the size of whatever
// ancestor box they ended up confined to instead of spanning the viewport.
//
// Since this editor bundle is meant to be dropped onto ANY future host page
// (that is the whole point of the zen-garden reference), it cannot assume
// the host page's CSS is well-behaved. Portalling every fixed-position piece
// out to a body-level container sidesteps the problem entirely: nothing
// under <body> directly should ever acquire a stray containing-block
// property, and even if it did, the portal container itself is unaffected
// by whatever the EDITED component's own ancestors are doing.
const OVERLAY_IDS = [
  'tiptap-toolbar',
  'inline-editor-footer',
  'editor-component-modal',
  'editor-save-error',
];

export function mountEditorOverlay(): void {
  const globalContainer = document.getElementById('editor-modal-container');
  if (!globalContainer) {
    return;
  }
  for (const id of OVERLAY_IDS) {
    const el = document.getElementById(id);
    if (el && el.parentElement !== globalContainer) {
      globalContainer.appendChild(el);
    }
  }
}

export function unmountEditorOverlay(): void {
  unlockBodyScroll();
  for (const id of OVERLAY_IDS) {
    document.getElementById(id)?.remove();
  }
}

// ─── Wiring ───────────────────────────────────────────────────────────────

export function wireComponentModal(): void {
  const modal = document.getElementById('editor-component-modal');
  const openButton = document.getElementById('edit-component-btn');
  if (!modal) {
    return;
  }

  if (openButton) {
    openButton.addEventListener('click', showComponentModal);
  }

  // Click on backdrop closes the modal
  modal.addEventListener('click', (event: MouseEvent) => {
    if (event.target === modal) {
      hideComponentModal();
    }
  });

  mountEditorOverlay();
}
