#!/usr/bin/env node

const esbuild = require('esbuild');
const fs = require('fs');
const path = require('path');

const jsOutDir = '../src/main/content/jcr_root/apps/slingslop/zengarden/js';
const entryPoint = 'src/typescript/editor-modal.ts';
const htmxSrc = 'node_modules/htmx.org/dist/htmx.js';

// Ensure output directory exists
if (!fs.existsSync(jsOutDir)) {
  fs.mkdirSync(jsOutDir, { recursive: true });
}

async function build() {
  // --- Dev bundle (unminified): Tiptap + editor code, htmx loaded separately ---
  console.log('Building dev bundle (editor-bundle.js)...');
  await esbuild.build({
    entryPoints: [entryPoint],
    bundle: true,
    format: 'iife',
    outfile: path.join(jsOutDir, 'editor-bundle.js'),
    minify: false,
    sourcemap: false,
    target: 'es2020',
  });
  console.log('  ✓ editor-bundle.js');

  // --- Prod bundle (minified): htmx prepended + Tiptap + editor code ---
  console.log('Building prod bundle (bundle.min.js)...');
  const htmxContent = fs.readFileSync(htmxSrc, 'utf8');
  await esbuild.build({
    entryPoints: [entryPoint],
    bundle: true,
    format: 'iife',
    outfile: path.join(jsOutDir, 'bundle.min.js'),
    minify: true,
    sourcemap: false,
    target: 'es2020',
    // Prepend htmx so it is available as window.htmx before the IIFE runs
    banner: { js: htmxContent },
  });
  console.log('  ✓ bundle.min.js');

  console.log('✓ Bundling complete!');
}

build().catch((err) => {
  console.error(err);
  process.exit(1);
});
