#!/usr/bin/env node

const fs = require('fs');
const path = require('path');
const { minify } = require('terser');

// Define paths
const sourceFiles = [
  'node_modules/htmx.org/dist/htmx.js',
  'node_modules/tinymce/tinymce.js',
  '../src/main/content/jcr_root/apps/slingslop/zengarden/js/editor-modal.js'
];

const outputDir = '../src/main/content/jcr_root/apps/slingslop/zengarden/js';
const outputFile = path.join(outputDir, 'bundle.min.js');
const tempFile = path.join(outputDir, 'bundle-temp.js');

// Ensure output directory exists
if (!fs.existsSync(outputDir)) {
  fs.mkdirSync(outputDir, { recursive: true });
}

// Concatenate files
console.log('Concatenating files...');
let concatenated = '';

for (const file of sourceFiles) {
  if (!fs.existsSync(file)) {
    console.error(`Error: File not found: ${file}`);
    process.exit(1);
  }
  console.log(`  Adding: ${file}`);
  concatenated += fs.readFileSync(file, 'utf8') + '\n';
}

// Write temporary file
fs.writeFileSync(tempFile, concatenated);
console.log(`Temporary file created: ${tempFile}`);

// Minify
console.log('Minifying...');
minify(concatenated, {
  compress: true,
  mangle: true
})
  .then(result => {
    if (result.code) {
      fs.writeFileSync(outputFile, result.code);
      console.log(`Bundle created: ${outputFile}`);
      
      // Clean up temp file
      if (fs.existsSync(tempFile)) {
        fs.unlinkSync(tempFile);
        console.log('Temporary file removed.');
      }
      
      console.log('✓ Bundling complete!');
    } else {
      console.error('Error: Minification produced no output');
      process.exit(1);
    }
  })
  .catch(err => {
    console.error('Error during minification:', err);
    // Clean up temp file on error
    if (fs.existsSync(tempFile)) {
      fs.unlinkSync(tempFile);
    }
    process.exit(1);
  });
