import { Editor } from '@tiptap/core';

/**
 * Shared mutable state for the active editor session.
 * Passed by reference so all modules always see the current values.
 */
export const state: {
  editor: Editor | null;
  isSourceView: boolean;
} = {
  editor: null,
  isSourceView: false,
};
