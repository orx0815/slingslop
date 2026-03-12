'use strict';

const eslint       = require('@eslint/js');
const tseslint     = require('typescript-eslint');
const prettier     = require('eslint-plugin-prettier');
const prettierConf = require('eslint-config-prettier');
const globals      = require('globals');

module.exports = tseslint.config(
  {
    ignores: [
      'target/**',
      'scripts/**',
      '../src/main/content/**',
    ],
  },

  eslint.configs.recommended,
  ...tseslint.configs.recommended,

  prettierConf,

  {
    files: ['src/typescript/**/*.ts'],
    languageOptions: {
      globals: {
        ...globals.browser,
        ...globals.es2020,
      },
    },
    plugins: { prettier },
    rules: {
      'prettier/prettier': 'error',
      '@typescript-eslint/explicit-function-return-type': [
        'error',
        { allowExpressions: true, allowTypedFunctionExpressions: true },
      ],
      '@typescript-eslint/no-explicit-any': 'error',
      '@typescript-eslint/no-unused-vars': [
        'error',
        { argsIgnorePattern: '^_', varsIgnorePattern: '^_' },
      ],
      'no-console': ['warn', { allow: ['warn', 'error', 'log'] }],
      eqeqeq: ['error', 'always'],
      curly:  ['error', 'all'],
    },
  }
);
