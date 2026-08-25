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
import { wireConfirmModal } from './editor/confirm-modal';
import { wireHoverBadge } from './editor/hover-badge';

// htmx is loaded as a global script (dev) or inlined via banner (prod)
declare const htmx: {
  process: (element: HTMLElement) => void;
  trigger: (element: HTMLElement, eventName: string) => void;
  config: { noSwap: (number | string)[] };
};

// htmx 4 event detail: swap/response events carry a `ctx` object.
// target is the resolved swap target; response holds the fetch status.
interface HtmxSwapDetail {
  ctx: { target: HTMLElement; response?: { status: number }; swap?: string };
}
interface HtmxResponseErrorDetail {
  ctx: { response: { status: number } };
}

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
    // Themed confirmation dialog in place of the native window.confirm()
    wireConfirmModal();
    wireHoverBadge();

    // htmx 4 swaps non-2xx responses by default (v2 did not). The save-error
    // overlay relies on error responses NOT being swapped into the page, so
    // every non-2xx wildcard ('1xx'/'3xx'/'4xx'/'5xx' -- i.e. anything except
    // '2xx') is added to noSwap to restore the v2 behaviour uniformly, not
    // just for the status codes with a bespoke message below.
    for (const pattern of ['1xx', '3xx', '4xx', '5xx']) {
      if (!htmx.config.noSwap.includes(pattern)) {
        htmx.config.noSwap.push(pattern);
      }
    }

    // Destroy editor before any swap that removes the active editing region.
    // With outerMorph htmx diffs the response into the live DOM, so the
    // Tiptap-managed DOM must be torn down here to avoid a dangling instance.
    //
    // Listen on `document`, NOT `document.body`: htmx's outerMorph can only
    // patch a node in place when the swapped-in markup's root has the SAME
    // tag name as the element being replaced (e.g. a richtext component's
    // <div> view swapped for the <div> edit-form wrapper). When the tag
    // differs -- e.g. a modal-only component whose view root is a semantic
    // <header>/<footer> (kept because host-page CSS commonly styles those
    // tags directly) swapped for editable-component-modal's <div> wrapper --
    // htmx cannot morph in place; it removes the old node and dispatches its
    // lifecycle events directly on `document` instead of on a (now detached)
    // element. Such events never reach a `document.body` listener, silently
    // breaking the edit flow for every modal-only component. `document`
    // catches both cases uniformly.
    document.addEventListener('htmx:before:swap', function (event: Event): void {
      const htmxEvent = event as CustomEvent<HtmxSwapDetail>;
      // htmx already resolves noSwap (see above) into ctx.swap === 'none'
      // before this event fires, so check that directly instead of
      // re-deriving it from the status code ourselves.
      // htmx still fires before:swap/after:swap for noSwap'd responses even
      // though no content is swapped in -- it just re-settles the existing
      // DOM. Tearing the overlay down here for those responses would delete
      // #editor-save-error (portalled, found by ID) right as
      // htmx:response:error tries to show it, and would destroy the live
      // Tiptap instance while editing is still active, leaving Save looking
      // like it silently did nothing.
      if (htmxEvent.detail.ctx.swap === 'none') {
        return;
      }
      if (htmxEvent.detail.ctx.target.hasAttribute('data-zen-editable-editing')) {
        hideComponentModal();
        unmountComponentModal();
        destroyEditor();
        document.body.removeAttribute('data-zen-editing');
      }
    });

    // Init editing UI after htmx swaps in an edit form.
    // Richtext supertype renders #tiptap-editor.
    // Modal-only supertype omits it and opens the modal directly.
    document.addEventListener('htmx:after:swap', function (): void {
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

    // Show a user-friendly message for save errors (401/403/422 = not logged in / no permission, 404, 500)
    document.addEventListener('htmx:response:error', function (event: Event): void {
      const htmxEvent = event as CustomEvent<HtmxResponseErrorDetail>;
      const status = htmxEvent.detail.ctx.response.status;

      let message: string;
      if (status === 422) {
        message = "I'm sorry, Dave. I'm afraid I can't let you do that. (w/o login)";
      } else if (status === 401) {
        message = 'Save failed: you are not logged in. Please log in and try again.';
      } else if (status === 403) {
        message = "I'm sorry, Dave. I'm afraid I can't let you do that. (no permission)";
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
