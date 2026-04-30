/**
 * public.ts — Sling Matrix public JavaScript entry point
 *
 * Handles client-side interactions for the Sling Matrix documentation site:
 * - Circular navigation menu (top-left, opens on mouse-over)
 * - Code syntax highlighting with highlight.js
 * - Matrix digital rain effect (optional)
 */

import hljs from 'highlight.js';

(function (): void {
  'use strict';

  // ── Code Highlighting ──────────────────────────────────────────────────────
  /**
   * Initialize syntax highlighting for all code blocks.
   * Runs on DOMContentLoaded and after HTMX content swaps.
   */
  function highlightCode(): void {
    const codeBlocks = document.querySelectorAll('pre code:not(.hljs)');
    codeBlocks.forEach((block) => {
      hljs.highlightElement(block as HTMLElement);
    });
  }

  // ── Circular Navigation ────────────────────────────────────────────────────
  /**
   * Handles the circular navigation menu interaction.
   * Top-left toggle opens first-level items in a circular layout.
   * Hovering over first-level items opens second-level items.
   */
  function initCircularNav(): void {
    const navToggle = document.querySelector('.nav-toggle');
    const navLevel1 = document.querySelector('.nav-level-1');
    const navItems = document.querySelectorAll('.nav-item');

    if (!navToggle || !navLevel1) {
      return;
    }

    // Toggle first-level navigation on mouse-over
    navToggle.addEventListener('mouseenter', () => {
      navLevel1.classList.add('nav-open');
    });

    // Close navigation when mouse leaves the entire nav area
    const navContainer = document.querySelector('.matrix-nav');
    if (navContainer) {
      navContainer.addEventListener('mouseleave', () => {
        navLevel1.classList.remove('nav-open');
        // Close all second-level menus
        navItems.forEach((item) => {
          const level2 = item.querySelector('.nav-level-2');
          if (level2) {
            level2.classList.remove('nav-level-2-open');
          }
        });
      });
    }

    // Open second-level navigation on mouse-over of first-level items
    navItems.forEach((item) => {
      const level2 = item.querySelector('.nav-level-2');
      if (level2) {
        item.addEventListener('mouseenter', () => {
          // Close other second-level menus
          navItems.forEach((otherItem) => {
            if (otherItem !== item) {
              const otherLevel2 = otherItem.querySelector('.nav-level-2');
              if (otherLevel2) {
                otherLevel2.classList.remove('nav-level-2-open');
              }
            }
          });
          level2.classList.add('nav-level-2-open');
        });
      }
    });
  }

  // ── Digital Rain Effect (Optional) ─────────────────────────────────────────
  /**
   * Creates a Matrix-style digital rain effect on the hero section.
   * Can be disabled by removing the .matrix-rain-container element.
   */
  function initMatrixRain(): void {
    const container = document.querySelector('.matrix-rain-container');
    if (!container) {
      return;
    }

    const canvas = document.createElement('canvas');
    canvas.className = 'matrix-rain-canvas';
    container.appendChild(canvas);

    const ctx = canvas.getContext('2d');
    if (!ctx) {
      return;
    }

    // Set canvas size
    function resizeCanvas(): void {
      canvas.width = window.innerWidth;
      canvas.height = window.innerHeight;
    }
    resizeCanvas();
    window.addEventListener('resize', resizeCanvas);

    // Matrix rain configuration
    const fontSize = 14;
    const columns = Math.floor(canvas.width / fontSize);
    const drops: number[] = Array(columns).fill(1);
    const chars =
      '01アイウエオカキクケコサシスセソタチツテトナニヌネノハヒフヘホマミムメモヤユヨラリルレロワヲン';

    function draw(): void {
      if (!ctx) {
        return;
      }
      // Fade effect
      ctx.fillStyle = 'rgba(0, 0, 0, 0.05)';
      ctx.fillRect(0, 0, canvas.width, canvas.height);

      // Draw characters
      ctx.fillStyle = '#00ff41'; // Matrix green
      ctx.font = `${fontSize}px monospace`;

      for (let i = 0; i < drops.length; i++) {
        const text = chars.charAt(Math.floor(Math.random() * chars.length));
        ctx.fillText(text, i * fontSize, drops[i] * fontSize);

        // Reset drop to top after it reaches bottom
        if (drops[i] * fontSize > canvas.height && Math.random() > 0.975) {
          drops[i] = 0;
        }
        drops[i]++;
      }
    }

    // Animate at 30fps
    setInterval(draw, 33);
  }

  // ── Initialization ─────────────────────────────────────────────────────────

  // Run on page load
  document.addEventListener('DOMContentLoaded', () => {
    highlightCode();
    initCircularNav();
    initMatrixRain();

    // Re-run code highlighting after HTMX content swaps
    document.body.addEventListener('htmx:afterSwap', () => {
      highlightCode();
    });
  });
})();
