/**
 * Editor Modal Handler for Slimpogrine
 * Manages TinyMCE rich text editor in a modal popup
 */

// Type declarations for external libraries
declare const htmx: {
  process: (element: HTMLElement) => void;
  trigger: (element: HTMLElement, eventName: string) => void;
};

declare const tinymce: {
  init: (config: TinyMCEConfig) => void;
  get: (selector: string) => TinyMCEEditor;
  remove: (selector: string) => void;
};

interface TinyMCEConfig {
  selector: string;
  license_key: string;
  base_url?: string;
  suffix?: string;
  theme: string;
  height: number;
  menubar: boolean;
  plugins: string[];
  toolbar: string;
  content_style: string;
  setup?: (editor: TinyMCEEditor) => void;
}

interface TinyMCEEditor {
  on: (event: string, callback: () => void) => void;
  getContent: () => string;
}

interface HTMXAfterSwapDetail {
  target: HTMLElement;
}

interface HTMXAfterSwapEvent extends Event {
  detail: HTMXAfterSwapDetail;
}

// Extend Window interface using interface merging (no export needed)
// eslint-disable-next-line @typescript-eslint/no-unused-vars
interface Window {
  closeEditorModal: () => void;
  saveEditorContent: () => void;
}

(function (): void {
  'use strict';

  // Wait for DOM to be ready before accessing document.body
  function initializeEventListeners(): void {
    // Initialize TinyMCE after htmx loads content into the modal
    document.body.addEventListener('htmx:afterSwap', function (event: Event): void {
      const htmxEvent = event as HTMXAfterSwapEvent;
      if (htmxEvent.detail.target.id === 'editor-modal-container') {
        // Get the form element that contains the passed parameters
        const form = document.getElementById('editor-form') as HTMLElement;
        // Re-process htmx attributes on the form
        htmx.process(form);

        initializeTinyMCE();
        showModal();
      }
    });

    function initializeTinyMCE(): void {
      // Detect if we're using the minified bundle by checking script sources
      const scripts = Array.from(document.getElementsByTagName('script'));
      const isUsingBundle = scripts.some((script) => script.src.includes('bundle.min.js'));

      tinymce.init({
        selector: '#content-editor',
        license_key: 'gpl',
        base_url: '/apps/slingslop/zengarden/js/tinymce',
        suffix: isUsingBundle ? '.min' : '',
        theme: 'silver',
        height: 400,
        menubar: false,
        plugins: [
          'advlist',
          'autolink',
          'lists',
          'link',
          'image',
          'charmap',
          'preview',
          'anchor',
          'searchreplace',
          'visualblocks',
          'code',
          'fullscreen',
          'insertdatetime',
          'media',
          'table',
          'help',
          'wordcount',
        ],
        toolbar:
          'undo redo | formatselect | bold italic backcolor | ' +
          'alignleft aligncenter alignright alignjustify | ' +
          'bullist numlist outdent indent | removeformat | help',
        content_style:
          'body { font-family: -apple-system, BlinkMacSystemFont, "Segoe UI", Roboto, "Helvetica Neue", Arial, sans-serif; font-size: 14px }',
        setup: function (editor: TinyMCEEditor): void {
          editor.on('init', function (): void {
            console.log('TinyMCE initialized');
          });
        },
      });
    }

    function showModal(): void {
      initializeEventListeners();
      const modal = document.getElementById('editor-modal');
      if (modal) {
        modal.style.display = 'block';
        document.body.style.overflow = 'hidden'; // Prevent background scrolling
      }
    }

    // Close modal function
    window.closeEditorModal = function (): void {
      const modal = document.getElementById('editor-modal');
      if (modal) {
        // Destroy TinyMCE instance before closing
        tinymce.remove('#content-editor');

        modal.style.display = 'none';
        document.body.style.overflow = ''; // Restore scrolling

        // Clear the modal container
        const container = document.getElementById('editor-modal-container');
        if (container) {
          container.innerHTML = '';
        }
      }
    };

    // Save content function
    window.saveEditorContent = function (): void {
      const content = tinymce.get('content-editor').getContent();

      // Get the form and submit via htmx
      const form = document.getElementById('editor-form') as HTMLElement;

      // Update the hidden input with editor content
      const hiddenInput = document.getElementById('content-hidden') as HTMLInputElement;
      hiddenInput.value = content;

      // Trigger htmx submit
      htmx.trigger(form, 'submit');
      window.closeEditorModal();
    };

    // Close modal when clicking outside
    window.onclick = function (event: MouseEvent): void {
      const modal = document.getElementById('editor-modal');
      if (event.target === modal) {
        window.closeEditorModal();
      }
    };

    // Close modal on ESC key
    document.addEventListener('keydown', function (event: KeyboardEvent): void {
      if (event.key === 'Escape') {
        const modal = document.getElementById('editor-modal');
        if (modal && modal.style.display === 'block') {
          window.closeEditorModal();
        }
      }
    });
  }

  // Initialize when DOM is ready
  if (document.readyState === 'loading') {
    document.addEventListener('DOMContentLoaded', initializeEventListeners);
  } else {
    initializeEventListeners();
  }
})();
