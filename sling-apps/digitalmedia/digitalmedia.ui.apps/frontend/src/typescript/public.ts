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

  console.log('Digital Media Library initialized');

  // Initialize HTMX event listeners for custom behaviors
  document.body.addEventListener('htmx:afterSwap', (event: Event) => {
    const htmxEvent = event as CustomEvent;
    console.log('HTMX swap completed', htmxEvent.detail);
  });

  // Asset selection handler
  document.body.addEventListener('click', (event: Event) => {
    const target = event.target as HTMLElement;

    // Handle asset item clicks
    if (target.closest('[data-asset-id]')) {
      const assetElement = target.closest('[data-asset-id]') as HTMLElement;
      const assetId = assetElement.dataset.assetId;

      // Remove previous selection
      document.querySelectorAll('[data-asset-id].selected').forEach((el) => {
        el.classList.remove('selected');
      });

      // Add selection to clicked asset
      assetElement.classList.add('selected');

      console.log('Asset selected:', assetId);
    }

    // Handle folder clicks
    if (target.closest('[data-folder-path]')) {
      const folderElement = target.closest('[data-folder-path]') as HTMLElement;
      const folderPath = folderElement.dataset.folderPath;

      console.log('Folder clicked:', folderPath);
    }
  });

  // File upload preview
  const uploadInput = document.querySelector<HTMLInputElement>('input[type="file"][name="asset"]');
  if (uploadInput) {
    uploadInput.addEventListener('change', (event: Event) => {
      const input = event.target as HTMLInputElement;
      const files = input.files;

      if (files && files.length > 0) {
        const file = files[0];
        console.log('File selected for upload:', file.name, file.size, file.type);

        // Show file info in UI
        const fileInfo = document.querySelector('.upload-file-info');
        if (fileInfo) {
          fileInfo.textContent = `${file.name} (${formatFileSize(file.size)})`;
        }
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
    return Math.round(bytes / Math.pow(k, i) * 100) / 100 + ' ' + sizes[i];
  }
})();
