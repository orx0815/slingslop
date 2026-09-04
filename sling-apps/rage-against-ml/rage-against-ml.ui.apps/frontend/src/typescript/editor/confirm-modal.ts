/**
 * confirm-modal.ts — replaces the browser's native window.confirm() (triggered
 * by htmx's `hx-confirm`) with a themed modal dialog.
 *
 * htmx fires `htmx:confirm` for every request; when the triggering element
 * carries `hx-confirm`, `event.detail.question` holds the prompt text and the
 * request is deferred until we call `event.detail.issueRequest(true)`.
 */

interface HtmxConfirmDetail {
  question: string | null;
  issueRequest: (skipConfirmation?: boolean) => void;
}

let dialogEl: HTMLElement | null = null;
let messageEl: HTMLElement | null = null;
let confirmBtn: HTMLButtonElement | null = null;
let cancelBtn: HTMLButtonElement | null = null;
let onConfirm: (() => void) | null = null;

function buildModal(): void {
  if (dialogEl) {
    return;
  }

  const overlay = document.createElement('div');
  overlay.className = 'parsys-confirm';
  overlay.setAttribute('role', 'dialog');
  overlay.setAttribute('aria-modal', 'true');
  overlay.setAttribute('aria-labelledby', 'parsys-confirm-title');
  overlay.hidden = true;
  overlay.innerHTML = `
    <div class="parsys-confirm__dialog">
      <div class="parsys-confirm__body">
        <h2 class="parsys-confirm__title" id="parsys-confirm-title">Delete component</h2>
        <p class="parsys-confirm__message"></p>
      </div>
      <div class="parsys-confirm__actions">
        <button type="button" class="parsys-confirm__btn parsys-confirm__btn--cancel">Cancel</button>
        <button type="button" class="parsys-confirm__btn parsys-confirm__btn--danger">Delete</button>
      </div>
    </div>`;

  document.body.appendChild(overlay);

  dialogEl = overlay;
  messageEl = overlay.querySelector('.parsys-confirm__message');
  cancelBtn = overlay.querySelector('.parsys-confirm__btn--cancel');
  confirmBtn = overlay.querySelector('.parsys-confirm__btn--danger');

  cancelBtn?.addEventListener('click', hideConfirm);
  confirmBtn?.addEventListener('click', () => {
    const action = onConfirm;
    hideConfirm();
    action?.();
  });
  overlay.addEventListener('click', (event: MouseEvent) => {
    if (event.target === overlay) {
      hideConfirm();
    }
  });
}

function hideConfirm(): void {
  onConfirm = null;
  if (!dialogEl) {
    return;
  }
  dialogEl.classList.add('is-closing');
  const el = dialogEl;
  window.setTimeout(() => {
    el.classList.remove('is-closing');
    el.hidden = true;
  }, 180);
}

function showConfirm(message: string, confirmAction: () => void): void {
  buildModal();
  if (!dialogEl || !messageEl) {
    // Fallback: never block the action if the modal can't be built.
    confirmAction();
    return;
  }
  onConfirm = confirmAction;
  messageEl.textContent = message;
  dialogEl.classList.remove('is-closing');
  dialogEl.hidden = false;
  confirmBtn?.focus();
}

/**
 * Intercepts htmx's confirm step and routes it through the themed modal.
 */
export function wireConfirmModal(): void {
  document.body.addEventListener('htmx:confirm', function (event: Event): void {
    const detail = (event as CustomEvent<HtmxConfirmDetail>).detail;
    if (!detail || !detail.question) {
      return; // no hx-confirm on this request → let htmx proceed normally
    }
    event.preventDefault();
    showConfirm(detail.question, () => detail.issueRequest(true));
  });

  document.addEventListener('keydown', function (event: KeyboardEvent): void {
    if (event.key === 'Escape' && dialogEl && !dialogEl.hidden) {
      hideConfirm();
    }
  });
}
