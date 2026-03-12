// ─── Modal visibility ─────────────────────────────────────────────────────

function unlockBodyScroll(): void {
  document.body.style.overflow = '';
}

export function showComponentModal(): void {
  mountComponentModal();
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
  window.setTimeout(finalize, 300);
}

// ─── Modal portalling ─────────────────────────────────────────────────────
// The modal is moved into #editor-modal-container (on <body>) so it escapes
// any overflow:hidden ancestor while editing.

export function mountComponentModal(): void {
  const modal = document.getElementById('editor-component-modal');
  const globalContainer = document.getElementById('editor-modal-container');
  if (!modal || !globalContainer || modal.parentElement === globalContainer) {
    return;
  }
  globalContainer.appendChild(modal);
}

export function unmountComponentModal(): void {
  unlockBodyScroll();
  const modal = document.getElementById('editor-component-modal');
  modal?.remove();
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
}
