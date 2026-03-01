#!/usr/bin/env node
/**
 * Build script for Zen Garden UI assets.
 *
 * Produces two independent bundles — editor and public — each with:
 *   • a plain (unminified) version for ?minJs=false development use
 *   • a minified production version
 *
 * Output layout under  ../src/main/content/jcr_root/apps/slingslop/zengarden/
 *
 *   js/
 *     editor/
 *       editor-bundle.js        ← dev (unminified), htmx loaded separately
 *       editor-bundle.min.js    ← prod (minified, htmx inlined)
 *     public/
 *       public-bundle.js
 *       public-bundle.min.js
 *   css/
 *     editor/
 *       editor.css
 *       editor.min.css
 *     public/
 *       public.css
 *       public.min.css
 */

'use strict';

const esbuild = require('esbuild');
const fs      = require('fs');
const path    = require('path');

// ── Paths ──────────────────────────────────────────────────────────────────

const ROOT     = path.resolve(__dirname, '..');
const JCR_BASE = path.resolve(ROOT, '../src/main/content/jcr_root/apps/slingslop/zengarden');
const JS_ROOT  = path.join(JCR_BASE, 'js');
const CSS_ROOT = path.join(JCR_BASE, 'css');
const HTMX_SRC = path.join(ROOT, 'node_modules/htmx.org/dist/htmx.js');

function mkdirp(dir) {
  fs.mkdirSync(dir, { recursive: true });
}

// ── CSS processor ──────────────────────────────────────────────────────────

/** Inline @import rules (one level deep). */
function resolveImports(entryFile) {
  const base = path.dirname(entryFile);
  let src = fs.readFileSync(entryFile, 'utf8');
  src = src.replace(/@import\s+["']([^"']+)["'];?\n?/g, (_match, rel) => {
    const abs = path.resolve(base, rel);
    return fs.readFileSync(abs, 'utf8') + '\n';
  });
  return src;
}

/** Strip comments and collapse whitespace. */
function minifyCss(src) {
  return src
    .replace(/\/\*[\s\S]*?\*\//g, '')
    .replace(/\s*([{}:;,>~+])\s*/g, '$1')
    .replace(/\s+/g, ' ')
    .trim();
}

async function buildCss(bundle) {
  const { name, entry, outDir } = bundle;
  mkdirp(outDir);

  const plain = resolveImports(entry);
  const min   = minifyCss(plain);

  fs.writeFileSync(path.join(outDir, `${name}.css`),     plain, 'utf8');
  fs.writeFileSync(path.join(outDir, `${name}.min.css`), min,   'utf8');

  console.log(`  ✓  css/${name}/  ${name}.css  |  ${name}.min.css`);
}

// ── JS build ───────────────────────────────────────────────────────────────

async function buildJs(bundle) {
  const { name, entry, outDir, inlineHtmx } = bundle;
  mkdirp(outDir);

  // plain (unminified)
  await esbuild.build({
    entryPoints: [entry],
    bundle:      true,
    format:      'iife',
    outfile:     path.join(outDir, `${name}-bundle.js`),
    minify:      false,
    sourcemap:   false,
    target:      'es2020',
  });

  // minified (optionally with htmx prepended)
  const htmxBanner = inlineHtmx ? fs.readFileSync(HTMX_SRC, 'utf8') : '';
  await esbuild.build({
    entryPoints: [entry],
    bundle:      true,
    format:      'iife',
    outfile:     path.join(outDir, `${name}-bundle.min.js`),
    minify:      true,
    sourcemap:   false,
    target:      'es2020',
    ...(htmxBanner ? { banner: { js: htmxBanner } } : {}),
  });

  console.log(`  ✓  js/${name}/  ${name}-bundle.js  |  ${name}-bundle.min.js`);
}

// ── Entry point ────────────────────────────────────────────────────────────

async function build() {
  console.log('Building Zen Garden UI assets…\n');

  await Promise.all([
    buildCss({
      name:   'editor',
      entry:  path.join(ROOT, 'src/css/editor/editor.css'),
      outDir: path.join(CSS_ROOT, 'editor'),
    }),
    buildCss({
      name:   'public',
      entry:  path.join(ROOT, 'src/css/public/public.css'),
      outDir: path.join(CSS_ROOT, 'public'),
    }),
    buildJs({
      name:       'editor',
      entry:      path.join(ROOT, 'src/typescript/editor.ts'),
      outDir:     path.join(JS_ROOT, 'editor'),
      inlineHtmx: true,
    }),
    buildJs({
      name:       'public',
      entry:      path.join(ROOT, 'src/typescript/public.ts'),
      outDir:     path.join(JS_ROOT, 'public'),
      inlineHtmx: false,
    }),
  ]);

  console.log('\n✓ Build complete!');
}

build().catch((err) => {
  console.error(err);
  process.exit(1);
});
