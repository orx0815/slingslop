/**
 * hero3d-entry.ts — Standalone entry point for the Three.js hero 3D effect.
 *
 * This bundle is loaded only on the homepage template to avoid bloating
 * the shared public-bundle with Three.js (~600 KB).
 */

import { initHero3D } from './hero3d';

document.addEventListener('DOMContentLoaded', () => {
  initHero3D();
});
