import { Editor } from '@tiptap/core';
import { state } from './state';

// ─── Character count ──────────────────────────────────────────────────────

export function updateCharCount(ed: Editor): void {
  const charCountEl = document.getElementById('char-count');
  if (!charCountEl) {
    return;
  }
  const storage = ed.storage['characterCount'] as
    | { characters: () => number; words: () => number }
    | undefined;
  if (storage) {
    charCountEl.textContent = `${storage.words()} words · ${storage.characters()} characters`;
  }
}

// ─── Toolbar active-state sync ────────────────────────────────────────────

export function updateToolbarState(ed: Editor): void {
  const toolbar = document.getElementById('tiptap-toolbar');
  if (!toolbar) {
    return;
  }

  const toggleActions: string[] = [
    'bold',
    'italic',
    'underline',
    'strike',
    'subscript',
    'superscript',
    'bulletList',
    'orderedList',
    'blockquote',
    'code',
    'highlight',
  ];
  toggleActions.forEach((action) => {
    const btn = toolbar.querySelector<HTMLButtonElement>(`[data-action="${action}"]`);
    if (btn) {
      btn.classList.toggle('is-active', ed.isActive(action));
    }
  });

  // Heading level select
  const headingSelect = document.getElementById('heading-select') as HTMLSelectElement | null;
  if (headingSelect) {
    const levels: Array<1 | 2 | 3 | 4 | 5 | 6> = [1, 2, 3, 4, 5, 6];
    let found = false;
    for (const level of levels) {
      if (ed.isActive('heading', { level })) {
        headingSelect.value = String(level);
        found = true;
        break;
      }
    }
    if (!found) {
      headingSelect.value = '0';
    }
  }

  // Text-align buttons
  const alignments = ['Left', 'Center', 'Right', 'Justify'] as const;
  for (const cap of alignments) {
    const btn = toolbar.querySelector<HTMLButtonElement>(`[data-action="align${cap}"]`);
    if (btn) {
      btn.classList.toggle('is-active', ed.isActive({ textAlign: cap.toLowerCase() }));
    }
  }
}

// ─── Toolbar enable / disable ─────────────────────────────────────────────

export function setToolbarDisabled(toolbar: HTMLElement, disabled: boolean): void {
  toolbar
    .querySelectorAll<
      HTMLButtonElement | HTMLSelectElement | HTMLInputElement
    >('button[data-action], select, input[type="color"]')
    .forEach((el) => {
      el.disabled = disabled;
    });
}

// ─── Toolbar actions ──────────────────────────────────────────────────────

export function handleToolbarAction(action: string): void {
  const { editor } = state;
  if (!editor) {
    return;
  }

  switch (action) {
    case 'bold':
      editor.chain().focus().toggleBold().run();
      break;
    case 'italic':
      editor.chain().focus().toggleItalic().run();
      break;
    case 'underline':
      editor.chain().focus().toggleUnderline().run();
      break;
    case 'strike':
      editor.chain().focus().toggleStrike().run();
      break;
    case 'subscript':
      editor.chain().focus().toggleSubscript().run();
      break;
    case 'superscript':
      editor.chain().focus().toggleSuperscript().run();
      break;
    case 'bulletList':
      editor.chain().focus().toggleBulletList().run();
      break;
    case 'orderedList':
      editor.chain().focus().toggleOrderedList().run();
      break;
    case 'blockquote':
      editor.chain().focus().toggleBlockquote().run();
      break;
    case 'codeBlock':
      editor.chain().focus().toggleCodeBlock().run();
      break;
    case 'code':
      editor.chain().focus().toggleCode().run();
      break;
    case 'horizontalRule':
      editor.chain().focus().setHorizontalRule().run();
      break;
    case 'highlight':
      editor.chain().focus().toggleHighlight().run();
      break;
    case 'alignLeft':
      editor.chain().focus().setTextAlign('left').run();
      break;
    case 'alignCenter':
      editor.chain().focus().setTextAlign('center').run();
      break;
    case 'alignRight':
      editor.chain().focus().setTextAlign('right').run();
      break;
    case 'alignJustify':
      editor.chain().focus().setTextAlign('justify').run();
      break;
    case 'undo':
      editor.chain().focus().undo().run();
      break;
    case 'redo':
      editor.chain().focus().redo().run();
      break;
    case 'clearFormatting':
      editor.chain().focus().clearNodes().unsetAllMarks().run();
      break;
    case 'link': {
      const existing = editor.getAttributes('link').href as string | undefined;
      const url = window.prompt('Enter URL:', existing ?? 'https://');
      if (url === null) {
        break;
      }
      if (url === '') {
        editor.chain().focus().unsetLink().run();
      } else {
        editor.chain().focus().setLink({ href: url }).run();
      }
      break;
    }
    case 'image': {
      const src = window.prompt('Enter image URL:');
      if (!src) {
        break;
      }
      const alt = window.prompt('Alt text (optional):') ?? '';
      editor.chain().focus().setImage({ src, alt }).run();
      break;
    }
    case 'insertTable':
      editor.chain().focus().insertTable({ rows: 3, cols: 3, withHeaderRow: true }).run();
      break;
    case 'addColumnBefore':
      editor.chain().focus().addColumnBefore().run();
      break;
    case 'addColumnAfter':
      editor.chain().focus().addColumnAfter().run();
      break;
    case 'deleteColumn':
      editor.chain().focus().deleteColumn().run();
      break;
    case 'addRowBefore':
      editor.chain().focus().addRowBefore().run();
      break;
    case 'addRowAfter':
      editor.chain().focus().addRowAfter().run();
      break;
    case 'deleteRow':
      editor.chain().focus().deleteRow().run();
      break;
    case 'deleteTable':
      editor.chain().focus().deleteTable().run();
      break;
    case 'mergeCells':
      editor.chain().focus().mergeCells().run();
      break;
    case 'splitCell':
      editor.chain().focus().splitCell().run();
      break;
    default:
      break;
  }
}

// ─── Toolbar wiring ───────────────────────────────────────────────────────

export function setupToolbar(): void {
  const toolbar = document.getElementById('tiptap-toolbar');
  if (!toolbar) {
    return;
  }

  // Bind all [data-action] buttons
  toolbar.querySelectorAll<HTMLButtonElement>('[data-action]').forEach((btn) => {
    btn.addEventListener('click', () => {
      const action = btn.getAttribute('data-action');
      if (action) {
        handleToolbarAction(action);
      }
    });
  });

  // Heading level select
  const headingSelect = document.getElementById('heading-select') as HTMLSelectElement | null;
  if (headingSelect) {
    headingSelect.addEventListener('change', () => {
      const level = parseInt(headingSelect.value, 10);
      if (level === 0) {
        state.editor?.chain().focus().setParagraph().run();
      } else {
        state.editor
          ?.chain()
          .focus()
          .setHeading({ level: level as 1 | 2 | 3 | 4 | 5 | 6 })
          .run();
      }
    });
  }

  // Text colour picker
  const colorPicker = document.getElementById('text-color-picker') as HTMLInputElement | null;
  if (colorPicker) {
    colorPicker.addEventListener('input', () => {
      state.editor?.chain().focus().setColor(colorPicker.value).run();
    });
  }

  // HTML source toggle
  const toggleBtn = document.getElementById('toggle-source') as HTMLButtonElement | null;
  const editorContainer = document.getElementById('tiptap-editor');
  const sourceTextarea = document.getElementById('html-source') as HTMLTextAreaElement | null;

  if (toggleBtn && editorContainer && sourceTextarea) {
    toggleBtn.addEventListener('click', () => {
      state.isSourceView = !state.isSourceView;
      if (state.isSourceView) {
        sourceTextarea.value = state.editor?.getHTML() ?? '';
        editorContainer.style.display = 'none';
        sourceTextarea.style.display = 'block';
        setToolbarDisabled(toolbar, true);
        toggleBtn.disabled = false; // keep toggle itself active
        toggleBtn.classList.add('is-active');
        toggleBtn.title = 'Exit HTML Source Mode';
      } else {
        if (state.editor) {
          state.editor.commands.setContent(sourceTextarea.value, false);
        }
        sourceTextarea.style.display = 'none';
        editorContainer.style.display = '';
        setToolbarDisabled(toolbar, false);
        toggleBtn.classList.remove('is-active');
        toggleBtn.title = 'View HTML Source';
      }
    });
  }
}
