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
    default:
      console.warn(`Unknown toolbar action: ${action}`);
  }
}

// ─── Toolbar wiring ───────────────────────────────────────────────────────

export function setupToolbar(): void {
  const toolbar = document.getElementById('tiptap-toolbar');
  if (!toolbar) {
    return;
  }

  toolbar.addEventListener('click', (event: MouseEvent) => {
    const target = (event.target as HTMLElement).closest<HTMLButtonElement>('[data-action]');
    if (target?.dataset['action']) {
      handleToolbarAction(target.dataset['action']);
    }
  });

  toolbar.addEventListener('change', (event: Event) => {
    const target = event.target as HTMLSelectElement;
    if (target.id === 'heading-select') {
      const level = parseInt(target.value, 10);
      const { editor } = state;
      if (!editor) {
        return;
      }
      if (level === 0) {
        editor.chain().focus().setParagraph().run();
      } else {
        editor
          .chain()
          .focus()
          .setHeading({ level: level as 1 | 2 | 3 | 4 | 5 | 6 })
          .run();
      }
    }
  });

  const colorPicker = toolbar.querySelector<HTMLInputElement>('#text-color-picker');
  if (colorPicker) {
    colorPicker.addEventListener('input', () => {
      state.editor?.chain().focus().setColor(colorPicker.value).run();
    });
  }

  const sourceBtn = toolbar.querySelector<HTMLButtonElement>('[data-action="sourceView"]');
  if (sourceBtn) {
    sourceBtn.addEventListener('click', () => {
      const tiptapEl = document.getElementById('tiptap-editor');
      const sourceEl = document.getElementById('html-source') as HTMLTextAreaElement | null;
      if (!tiptapEl || !sourceEl) {
        return;
      }
      state.isSourceView = !state.isSourceView;
      if (state.isSourceView) {
        sourceEl.value = state.editor?.getHTML() ?? '';
        tiptapEl.style.display = 'none';
        sourceEl.style.display = 'block';
        setToolbarDisabled(toolbar, true);
        sourceBtn.classList.add('is-active');
      } else {
        state.editor?.commands.setContent(sourceEl.value, false);
        tiptapEl.style.display = '';
        sourceEl.style.display = 'none';
        setToolbarDisabled(toolbar, false);
        sourceBtn.classList.remove('is-active');
      }
    });
  }
}
