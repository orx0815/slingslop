/**
 * editor.ts — Metal Mania editor bundle entry point.
 *
 * Wires htmx lifecycle events, exposes the window API used by HTL buttons,
 * and bootstraps everything once the DOM is ready.
 */

import { initializeTiptap, destroyEditor } from './editor/tiptap';
import {
  wireComponentModal,
  showComponentModal,
  hideComponentModal,
  unmountComponentModal,
} from './editor/component-modal';
import { saveEditorContent } from './editor/save';

// htmx is loaded as a global script (dev) or inlined via banner (prod)
declare const htmx: {
  process: (element: HTMLElement) => void;
  trigger: (element: HTMLElement, eventName: string) => void;
};

// Window API called from HTL onclick attributes
declare global {
  interface Window {
    saveEditorContent: () => void;
    openEditorComponentModal: () => void;
    closeEditorComponentModal: () => void;
    dismissSaveError: () => void;
  }
}

(function (): void {
  'use strict';

  function initializeEventListeners(): void {
    // Destroy editor before any swap that removes the active editing region
    document.body.addEventListener('htmx:beforeSwap', function (event: Event): void {
      const htmxEvent = event as CustomEvent<{ target: HTMLElement }>;
      if (htmxEvent.detail.target.hasAttribute('data-zen-editable-editing')) {
        hideComponentModal();
        unmountComponentModal();
        destroyEditor();
        document.body.removeAttribute('data-zen-editing');
      }
    });

    // Init editing UI after htmx swaps in an edit form.
    // Richtext supertype renders #tiptap-editor.
    // Modal-only supertype omits it and opens the modal directly.
    document.body.addEventListener('htmx:afterSwap', function (): void {
      const form = document.getElementById('editor-form') as HTMLElement | null;
      if (!form) {
        return;
      }

      htmx.process(form);
      wireComponentModal();

      const tiptapEl = document.getElementById('tiptap-editor');
      if (tiptapEl) {
        initializeTiptap();
      } else {
        destroyEditor();
        showComponentModal();
      }

      document.body.setAttribute('data-zen-editing', '');
    });

    // Show a user-friendly message for save errors (401/422 = not logged in, 404, 500)
    document.body.addEventListener('htmx:responseError', function (event: Event): void {
      const htmxEvent = event as CustomEvent<{ xhr: XMLHttpRequest }>;
      const status = htmxEvent.detail.xhr.status;

      let message: string;
      if (status === 422) {
        message = "I'm sorry, Dave. I'm afraid I can't let you do that. (w/o login)";
      } else if (status === 401) {
        message = 'Save failed: you are not logged in. Please log in and try again.';
      } else if (status === 404) {
        message = 'Save failed: the content could not be found (404).';
      } else if (status === 500) {
        message = 'Save failed: a server error occurred (500). Please try again later.';
      } else {
        message = `Save failed: unexpected server response (${status}).`;
      }

      const errorEl = document.getElementById('editor-save-error');
      if (errorEl) {
        const msgEl = errorEl.querySelector('.editor-save-error__message');
        if (msgEl) {
          msgEl.textContent = message;
        }
        errorEl.setAttribute('aria-hidden', 'false');
        errorEl.classList.add('is-visible');
      }
    });

    // Escape key behaviour:
    // - Modal open, modal-only component  → Cancel (restores original HTML)
    // - Modal open, richtext component    → close modal, stay in inline edit
    // - Modal closed, inline edit active  → Cancel (restores original HTML)
    document.addEventListener('keydown', function (event: KeyboardEvent): void {
      if (event.key === 'Escape') {
        const modal = document.getElementById('editor-component-modal');
        const tiptapEl = document.getElementById('tiptap-editor');

        if (modal?.classList.contains('is-open')) {
          if (!tiptapEl) {
            // Modal-only mode: trigger Cancel to restore original component HTML
            const cancelBtn = modal.querySelector<HTMLButtonElement>('.btn-secondary[hx-get]');
            cancelBtn?.click();
          } else {
            // Richtext: just close the modal, keep inline editing
            hideComponentModal();
          }
        } else if (tiptapEl) {
          // Inline edit active, modal not open: trigger the footer Cancel button
          const cancelBtn = document.querySelector<HTMLButtonElement>(
            '.inline-editor-footer .btn-secondary[hx-get]'
          );
          cancelBtn?.click();
        }
      }
    });

    // Animated dismiss for save-error overlay
    function dismissSaveError(): void {
      const errorEl = document.getElementById('editor-save-error');
      if (!errorEl || !errorEl.classList.contains('is-visible')) {
        return;
      }
      errorEl.classList.add('is-closing');
      window.setTimeout(() => {
        errorEl.classList.remove('is-visible', 'is-closing');
        errorEl.setAttribute('aria-hidden', 'true');
      }, 420);
    }

    // Expose API to HTL onclick attributes
    window.saveEditorContent = saveEditorContent;
    window.openEditorComponentModal = showComponentModal;
    window.closeEditorComponentModal = hideComponentModal;
    window.dismissSaveError = dismissSaveError;
  }

  if (document.readyState === 'loading') {
    document.addEventListener('DOMContentLoaded', initializeEventListeners);
  } else {
    initializeEventListeners();
  }
})();
