import { state } from './state';

// htmx is loaded as a global script (dev) or inlined via banner (prod)
declare const htmx: {
  trigger: (element: HTMLElement, eventName: string) => void;
};

/**
 * Serialises editor content into the hidden form input and triggers an
 * htmx form submission to the Sling POST Servlet.
 */
export function saveEditorContent(): void {
  let content: string;
  if (state.isSourceView) {
    const sourceTextarea = document.getElementById('html-source') as HTMLTextAreaElement | null;
    content = sourceTextarea?.value ?? '';
  } else {
    content = state.editor?.getHTML() ?? '';
  }

  const form = document.getElementById('editor-form') as HTMLElement;
  const hiddenInput = document.getElementById('content-hidden') as HTMLInputElement;
  hiddenInput.value = content;
  htmx.trigger(form, 'submit');
  // No manual close needed — htmx outerHTML swap restores the view component
}
