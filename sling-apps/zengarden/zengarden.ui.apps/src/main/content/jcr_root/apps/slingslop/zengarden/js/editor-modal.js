"use strict";
/**
 * Editor Modal Handler for Slimpogrine
 * Manages TinyMCE rich text editor in a modal popup
 */
(function () {
    'use strict';
    // Wait for DOM to be ready before accessing document.body
    function initializeEventListeners() {
        // Initialize TinyMCE after htmx loads content into the modal
        document.body.addEventListener('htmx:afterSwap', function (event) {
            const htmxEvent = event;
            if (htmxEvent.detail.target.id === 'editor-modal-container') {
                // Get the form element that contains the passed parameters
                const form = document.getElementById('editor-form');
                // Re-process htmx attributes on the form
                htmx.process(form);
                initializeTinyMCE();
                showModal();
            }
        });
        function initializeTinyMCE() {
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
                toolbar: 'undo redo | formatselect | bold italic backcolor | ' +
                    'alignleft aligncenter alignright alignjustify | ' +
                    'bullist numlist outdent indent | removeformat | help',
                content_style: 'body { font-family: -apple-system, BlinkMacSystemFont, "Segoe UI", Roboto, "Helvetica Neue", Arial, sans-serif; font-size: 14px }',
                setup: function (editor) {
                    editor.on('init', function () {
                        console.log('TinyMCE initialized');
                    });
                },
            });
        }
        function showModal() {
            initializeEventListeners();
            const modal = document.getElementById('editor-modal');
            if (modal) {
                modal.style.display = 'block';
                document.body.style.overflow = 'hidden'; // Prevent background scrolling
            }
        }
        // Close modal function
        window.closeEditorModal = function () {
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
        window.saveEditorContent = function () {
            const content = tinymce.get('content-editor').getContent();
            // Get the form and submit via htmx
            const form = document.getElementById('editor-form');
            // Update the hidden input with editor content
            const hiddenInput = document.getElementById('content-hidden');
            hiddenInput.value = content;
            // Trigger htmx submit
            htmx.trigger(form, 'submit');
            window.closeEditorModal();
        };
        // Close modal when clicking outside
        window.onclick = function (event) {
            const modal = document.getElementById('editor-modal');
            if (event.target === modal) {
                window.closeEditorModal();
            }
        };
        // Close modal on ESC key
        document.addEventListener('keydown', function (event) {
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
    }
    else {
        initializeEventListeners();
    }
})();
