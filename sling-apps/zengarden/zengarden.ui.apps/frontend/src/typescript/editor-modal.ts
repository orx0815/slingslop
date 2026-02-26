/**
 * Editor Modal Handler for Zen Garden
 * Manages Tiptap rich text editor in a modal popup.
 * Saves edited HTML directly to Sling via htmx form submission.
 */

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

// htmx is loaded as a global script (dev) or bundled via banner (prod)
declare const htmx: {
  process: (element: HTMLElement) => void;
  trigger: (element: HTMLElement, eventName: string) => void;
};

// Extend Window interface for inline editor API
declare global {
  interface Window {
    saveEditorContent: () => void;
  }
}

(function (): void {
  'use strict';

  let editor: Editor | null = null;
  let isSourceView = false;

  // ─── Editor initialisation ────────────────────────────────────────────────

  function initializeTiptap(): void {
    const editorEl = document.getElementById('tiptap-editor');
    if (!editorEl) {
      return;
    }

    // Read initial HTML from the hidden textarea populated by Sling HTL
    const initialContentEl = document.getElementById(
      'content-editor'
    ) as HTMLTextAreaElement | null;
    const initialContent = initialContentEl?.value ?? '';

    editor = new Editor({
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
    updateToolbarState(editor);
    updateCharCount(editor);
  }

  // ─── Toolbar state ────────────────────────────────────────────────────────

  function updateCharCount(ed: Editor): void {
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

  function updateToolbarState(ed: Editor): void {
    const toolbar = document.getElementById('tiptap-toolbar');
    if (!toolbar) {
      return;
    }

    // Toggle-style marks & nodes
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
      const headingLevels: Array<1 | 2 | 3 | 4 | 5 | 6> = [1, 2, 3, 4, 5, 6];
      let found = false;
      for (const level of headingLevels) {
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

  // ─── Toolbar actions ──────────────────────────────────────────────────────

  function handleToolbarAction(action: string): void {
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
        } // cancelled
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

  function setToolbarDisabled(toolbar: HTMLElement, disabled: boolean): void {
    toolbar
      .querySelectorAll<
        HTMLButtonElement | HTMLSelectElement | HTMLInputElement
      >('button[data-action], select, input[type="color"]')
      .forEach((el) => {
        el.disabled = disabled;
      });
  }

  function setupToolbar(): void {
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
          editor?.chain().focus().setParagraph().run();
        } else {
          editor
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
        editor?.chain().focus().setColor(colorPicker.value).run();
      });
    }

    // HTML source toggle
    const toggleSourceBtnAsButton = document.getElementById(
      'toggle-source'
    ) as HTMLButtonElement | null;
    const editorContainer = document.getElementById('tiptap-editor');
    const sourceTextarea = document.getElementById('html-source') as HTMLTextAreaElement | null;

    if (toggleSourceBtnAsButton && editorContainer && sourceTextarea) {
      toggleSourceBtnAsButton.addEventListener('click', () => {
        isSourceView = !isSourceView;
        if (isSourceView) {
          sourceTextarea.value = editor?.getHTML() ?? '';
          editorContainer.style.display = 'none';
          sourceTextarea.style.display = 'block';
          setToolbarDisabled(toolbar, true);
          toggleSourceBtnAsButton.disabled = false; // keep toggle itself active
          toggleSourceBtnAsButton.classList.add('is-active');
          toggleSourceBtnAsButton.title = 'Exit HTML Source Mode';
        } else {
          if (editor) {
            editor.commands.setContent(sourceTextarea.value, false);
          }
          sourceTextarea.style.display = 'none';
          editorContainer.style.display = '';
          setToolbarDisabled(toolbar, false);
          toggleSourceBtnAsButton.classList.remove('is-active');
          toggleSourceBtnAsButton.title = 'View HTML Source';
        }
      });
    }
  }

  // ─── Modal lifecycle ──────────────────────────────────────────────────────

  /** Destroy the Tiptap editor instance if active. */
  function destroyEditor(): void {
    if (editor) {
      editor.destroy();
      editor = null;
      isSourceView = false;
    }
  }

  function initializeEventListeners(): void {
    // Destroy editor before any swap that removes the active editing div
    document.body.addEventListener('htmx:beforeSwap', function (event: Event): void {
      const htmxEvent = event as CustomEvent<{ target: HTMLElement }>;
      if (htmxEvent.detail.target.hasAttribute('data-zen-editable-editing')) {
        destroyEditor();
      }
    });

    // Init Tiptap after htmx swaps in the edit form (detected by #tiptap-editor presence)
    document.body.addEventListener('htmx:afterSwap', function (): void {
      const tiptapEl = document.getElementById('tiptap-editor');
      if (tiptapEl) {
        const form = document.getElementById('editor-form') as HTMLElement | null;
        if (form) {
          htmx.process(form);
        }
        initializeTiptap();
      }
    });

    // Save content via htmx (populates hidden input and triggers form submit)
    window.saveEditorContent = function (): void {
      let content: string;
      if (isSourceView) {
        const sourceTextarea = document.getElementById('html-source') as HTMLTextAreaElement | null;
        content = sourceTextarea?.value ?? '';
      } else {
        content = editor?.getHTML() ?? '';
      }
      const form = document.getElementById('editor-form') as HTMLElement;
      const hiddenInput = document.getElementById('content-hidden') as HTMLInputElement;
      hiddenInput.value = content;
      htmx.trigger(form, 'submit');
      // No manual close needed — htmx outerHTML swap restores the view
    };
  }

  // Kick everything off once the DOM is ready
  if (document.readyState === 'loading') {
    document.addEventListener('DOMContentLoaded', initializeEventListeners);
  } else {
    initializeEventListeners();
  }
})();
