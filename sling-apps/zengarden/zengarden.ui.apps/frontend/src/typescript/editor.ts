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
        initializeTiptap();
        wireComponentModal();
        document.body.setAttribute('data-zen-editing', '');
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
