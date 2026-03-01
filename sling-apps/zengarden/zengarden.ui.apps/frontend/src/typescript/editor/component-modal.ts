// ─── Modal visibility ─────────────────────────────────────────────────────

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
    return;
  }
  modal.classList.remove('is-open');
  modal.setAttribute('aria-hidden', 'true');
  document.body.style.overflow = '';
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
  const modal = document.getElementById('editor-component-modal');
  modal?.remove();
}

// ─── Wiring ───────────────────────────────────────────────────────────────

export function wireComponentModal(): void {
  const modal = document.getElementById('editor-component-modal');
  const openButton = document.getElementById('edit-component-btn');
  if (!modal || !openButton) {
    return;
  }

  openButton.addEventListener('click', showComponentModal);

  // Click on backdrop closes the modal
  modal.addEventListener('click', (event: MouseEvent) => {
    if (event.target === modal) {
      hideComponentModal();
    }
  });

  mountComponentModal();
}
