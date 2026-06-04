/**
 * public.ts — Digital Media Library public JavaScript entry point
 *
 * Handles dashboard interactions for the media library:
 * - Folder navigation
 * - Asset upload via HTMX
 * - Asset selection and metadata display
 * - Rendition generation
 */
(function (): void {
  'use strict';

  function init(): void {
    console.log('Digital Media Library initialized');

    // Initialize HTMX event listeners for custom behaviors
    document.body.addEventListener('htmx:afterSwap', (event: Event) => {
      const htmxEvent = event as CustomEvent;
      console.log('HTMX swap completed', htmxEvent.detail);

      // After successful upload, trigger asset list reload
      if (htmxEvent.detail.target && htmxEvent.detail.target.classList.contains('dml-asset-grid')) {
        console.log('Assets updated');
      }
    });

    // After successful upload form submission, trigger reload
    document.body.addEventListener('htmx:afterRequest', (event: Event) => {
      const htmxEvent = event as CustomEvent;

      if (
        htmxEvent.detail.successful &&
        htmxEvent.detail.elt.classList.contains('dml-upload-form')
      ) {
        console.log('Upload successful, triggering asset reload');
        // Dispatch custom event to reload assets
        document.body.dispatchEvent(new CustomEvent('assetUploaded'));

        // Reset the form
        const form = htmxEvent.detail.elt as HTMLFormElement;
        form.reset();

        // Hide file info
        const fileInfo = document.querySelector<HTMLElement>('.upload-file-info');
        if (fileInfo) {
          fileInfo.textContent = '';
          fileInfo.style.display = 'none';
        }
      }
    });

    // Asset selection handler
    document.body.addEventListener('click', (event: Event) => {
      const target = event.target as HTMLElement;

      // Handle asset item clicks
      if (target.closest('[data-asset-id]')) {
        const assetElement = target.closest('[data-asset-id]') as HTMLElement;
        const assetId = assetElement.dataset.assetId;
        const assetPath = assetElement.dataset.assetPath;

        // Remove previous selection
        document.querySelectorAll('[data-asset-id].selected').forEach((el) => {
          el.classList.remove('selected');
        });

        // Add selection to clicked asset
        assetElement.classList.add('selected');

        console.log('Asset selected:', assetId, assetPath);

        // Load metadata in the metadata panel
        if (assetPath) {
          const metadataPanel = document.querySelector('.dml-metadata-panel');
          if (metadataPanel) {
            // Use HTMX to load the metadata panel for this asset
            (
              window as unknown as {
                htmx: {
                  ajax: (
                    method: string,
                    url: string,
                    opts: { target: string; swap: string }
                  ) => void;
                };
              }
            ).htmx.ajax('GET', `${assetPath}.metadata-panel.html`, {
              target: '.dml-metadata-panel',
              swap: 'innerHTML',
            });
          }
        }
      }

      // Handle folder clicks
      if (target.closest('[data-folder-path]')) {
        const folderElement = target.closest('[data-folder-path]') as HTMLElement;
        const folderPath = folderElement.dataset.folderPath;

        // Update active state
        document.querySelectorAll('[data-folder-path]').forEach((el) => {
          el.classList.remove('active');
        });
        folderElement.classList.add('active');

        // Reload asset grid for the selected folder
        const assetGrid = document.querySelector<HTMLElement>('.dml-asset-grid');
        if (assetGrid) {
          const baseUrl = assetGrid.getAttribute('hx-get') ?? '';
          const url =
            folderPath && folderPath !== '/'
              ? `${baseUrl}?folder=${encodeURIComponent(folderPath)}`
              : baseUrl;
          (
            window as unknown as {
              htmx: {
                ajax: (method: string, url: string, opts: { target: string; swap: string }) => void;
              };
            }
          ).htmx.ajax('GET', url, { target: '.dml-asset-grid', swap: 'innerHTML' });
        }
      }
    });

    // File upload preview
    const uploadInput = document.querySelector<HTMLInputElement>(
      'input[type="file"][name="asset"]'
    );
    const uploadArea = document.querySelector<HTMLElement>('.dml-upload-area');
    const fileInfoEl = document.querySelector<HTMLElement>('.upload-file-info');

    function showFileInfo(file: File): void {
      if (fileInfoEl) {
        fileInfoEl.textContent = `${file.name} (${formatFileSize(file.size)})`;
        fileInfoEl.style.display = 'block';
      }
    }

    if (uploadInput) {
      uploadInput.addEventListener('change', (event: Event) => {
        const input = event.target as HTMLInputElement;
        const files = input.files;
        if (files && files.length > 0) {
          const file = files[0];
          console.log('File selected for upload:', file.name, file.size, file.type);
          showFileInfo(file);
        }
      });
    }

    // Drag and drop support
    if (uploadArea && uploadInput) {
      uploadArea.addEventListener('dragover', (e: DragEvent) => {
        e.preventDefault();
        uploadArea.classList.add('drag-over');
      });

      uploadArea.addEventListener('dragleave', () => {
        uploadArea.classList.remove('drag-over');
      });

      uploadArea.addEventListener('drop', (e: DragEvent) => {
        e.preventDefault();
        uploadArea.classList.remove('drag-over');
        const files = e.dataTransfer?.files;
        if (files && files.length > 0) {
          const dt = new DataTransfer();
          dt.items.add(files[0]);
          uploadInput.files = dt.files;
          showFileInfo(files[0]);
          uploadInput.dispatchEvent(new Event('change'));
        }
      });
    }

    // Helper: Format file size for display
    function formatFileSize(bytes: number): string {
      if (bytes === 0) {
        return '0 Bytes';
      }
      const k = 1024;
      const sizes = ['Bytes', 'KB', 'MB', 'GB'];
      const i = Math.floor(Math.log(bytes) / Math.log(k));
      return Math.round((bytes / Math.pow(k, i)) * 100) / 100 + ' ' + sizes[i];
    }
  } // end init()

  if (document.readyState === 'loading') {
    document.addEventListener('DOMContentLoaded', init);
  } else {
    init();
  }
})();
