import { Editor } from '@tiptap/core';
import StarterKit from '@tiptap/starter-kit';
import Link from '@tiptap/extension-link';
import Image from '@tiptap/extension-image';
import Underline from '@tiptap/extension-underline';
import TextAlign from '@tiptap/extension-text-align';
import TextStyle from '@tiptap/extension-text-style';
import Color from '@tiptap/extension-color';
import Highlight from '@tiptap/extension-highlight';
import Subscript from '@tiptap/extension-subscript';
import Superscript from '@tiptap/extension-superscript';
import Table from '@tiptap/extension-table';
import TableRow from '@tiptap/extension-table-row';
import TableCell from '@tiptap/extension-table-cell';
import TableHeader from '@tiptap/extension-table-header';
import Placeholder from '@tiptap/extension-placeholder';
import CharacterCount from '@tiptap/extension-character-count';
import Typography from '@tiptap/extension-typography';

import { state } from './state';
import { setupToolbar, updateToolbarState, updateCharCount } from './toolbar';

// ─── Initialisation ───────────────────────────────────────────────────────

export function initializeTiptap(): void {
  const editorEl = document.getElementById('tiptap-editor');
  if (!editorEl) {
    return;
  }

  // Initial HTML is seeded by Sling HTL into a hidden textarea
  const initialContentEl = document.getElementById('content-editor') as HTMLTextAreaElement | null;
  const initialContent = initialContentEl?.value ?? '';

  state.editor = new Editor({
    element: editorEl,
    extensions: [
      StarterKit.configure({
        heading: { levels: [1, 2, 3, 4, 5, 6] },
      }),
      Underline,
      Link.configure({
        openOnClick: false,
        HTMLAttributes: { rel: 'noopener noreferrer' },
      }),
      Image.configure({ inline: false }),
      TextAlign.configure({ types: ['heading', 'paragraph'] }),
      TextStyle,
      Color,
      Highlight.configure({ multicolor: true }),
      Subscript,
      Superscript,
      Table.configure({ resizable: true }),
      TableRow,
      TableHeader,
      TableCell,
      Placeholder.configure({
        placeholder: 'Start typing your content here…',
      }),
      CharacterCount,
      Typography,
    ],
    content: initialContent,
    onUpdate({ editor: ed }): void {
      updateToolbarState(ed);
      updateCharCount(ed);
    },
    onSelectionUpdate({ editor: ed }): void {
      updateToolbarState(ed);
    },
  });

  setupToolbar();
  updateToolbarState(state.editor);
  updateCharCount(state.editor);
}

// ─── Teardown ─────────────────────────────────────────────────────────────

export function destroyEditor(): void {
  if (state.editor) {
    state.editor.destroy();
    state.editor = null;
    state.isSourceView = false;
  }
}
