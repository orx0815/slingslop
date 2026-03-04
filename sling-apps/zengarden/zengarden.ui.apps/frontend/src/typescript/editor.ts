/**
 * editor.ts — Zen Garden editor bundle entry point.
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
  }
}

(function (): void {
  'use strict';

  /**
   * Edit form contract:
   * - Components that use richtext should provide #content-editor and #content-hidden.
   * - Components can set data-inline-richtext="true|false" on their edit-form-fields root.
   *   `false` skips Tiptap and opens the component modal directly.
   */
  function isRichtextEditMode(): boolean {
    const marker = document.querySelector('[data-inline-richtext]') as HTMLElement | null;
    if (marker) {
      return marker.dataset.inlineRichtext === 'true';
    }

    return !!document.getElementById('content-editor');
  }

  function enableNoRichtextMode(): void {
    const editingRoot = document.querySelector('[data-zen-editable-editing]') as HTMLElement | null;
    if (editingRoot) {
      editingRoot.classList.add('no-richtext-edit');
    }

    ['tiptap-toolbar', 'tiptap-editor', 'html-source', 'char-count'].forEach((id: string): void => {
      const el = document.getElementById(id) as HTMLElement | null;
      if (el) {
        el.style.display = 'none';
      }
    });

    const editButton = document.getElementById('edit-component-btn') as HTMLElement | null;
    if (editButton) {
      editButton.style.display = 'none';
    }

    const footerBar = document.querySelector('.inline-editor-footer') as HTMLElement | null;
    if (footerBar) {
      footerBar.style.display = 'none';
    }

    showComponentModal();
  }

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

    // Init Tiptap after htmx swaps in the edit form (detected by #tiptap-editor)
    document.body.addEventListener('htmx:afterSwap', function (): void {
      const tiptapEl = document.getElementById('tiptap-editor');
      if (tiptapEl) {
        const form = document.getElementById('editor-form') as HTMLElement | null;
        if (form) {
          htmx.process(form);
        }
        wireComponentModal();

        if (isRichtextEditMode()) {
          initializeTiptap();
        } else {
          destroyEditor();
          enableNoRichtextMode();
        }

        document.body.setAttribute('data-zen-editing', '');
      }
    });

    // Show a user-friendly message for save errors (401/422 = not logged in, 404, 500)
    document.body.addEventListener('htmx:responseError', function (event: Event): void {
      const htmxEvent = event as CustomEvent<{ xhr: XMLHttpRequest }>;
      const status = htmxEvent.detail.xhr.status;

      let message: string;
      if (status === 401 || status === 422) {
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

    // Escape key closes the component modal
    document.addEventListener('keydown', function (event: KeyboardEvent): void {
      if (event.key === 'Escape') {
        const modal = document.getElementById('editor-component-modal');
        if (modal?.classList.contains('is-open')) {
          hideComponentModal();
        }
      }
    });

    // Expose API to HTL onclick attributes
    window.saveEditorContent = saveEditorContent;
    window.openEditorComponentModal = showComponentModal;
    window.closeEditorComponentModal = hideComponentModal;
  }

  if (document.readyState === 'loading') {
    document.addEventListener('DOMContentLoaded', initializeEventListeners);
  } else {
    initializeEventListeners();
  }
})();
