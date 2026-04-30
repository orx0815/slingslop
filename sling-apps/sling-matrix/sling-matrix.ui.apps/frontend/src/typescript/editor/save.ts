import { state } from './state';

/**
 * Serialises editor content into the hidden form input and triggers an
 * htmx form submission to the Sling POST Servlet.
 *
 * For richtext components: serializes Tiptap content to content-hidden input.
 * For modal-only components: directly submits the form (no serialization needed).
 */
export function saveEditorContent(): void {
  // Clear any previous save error before attempting again
  const errorEl = document.getElementById('editor-save-error');
  if (errorEl) {
    errorEl.classList.remove('is-visible', 'is-closing');
    errorEl.setAttribute('aria-hidden', 'true');
  }

  const form = document.getElementById('editor-form') as HTMLFormElement | null;
  if (!form) {
    console.error('saveEditorContent: form#editor-form not found');
    return;
  }

  const hiddenInput = document.getElementById('content-hidden') as HTMLInputElement | null;

  // Richtext component: serialize Tiptap content
  if (hiddenInput) {
    let content: string;
    if (state.isSourceView) {
      const sourceTextarea = document.getElementById('html-source') as HTMLTextAreaElement | null;
      content = sourceTextarea?.value ?? '';
    } else {
      content = state.editor?.getHTML() ?? '';
    }

    hiddenInput.value = content;
    console.log('saveEditorContent: serialized richtext content');
  } else {
    // Modal-only component: no serialization needed
    console.log('saveEditorContent: modal-only component, submitting form directly');
  }

  // Trigger form submit - use requestSubmit() instead of htmx.trigger()
  // because the modal is portalled outside the form, breaking htmx.trigger()
  console.log('saveEditorContent: triggering form submit via requestSubmit()');
  form.requestSubmit();
  // No manual close needed — htmx outerHTML swap restores the view component
}
