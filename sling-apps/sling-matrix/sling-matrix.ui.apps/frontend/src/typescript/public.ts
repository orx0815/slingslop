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

    let closeTimeout: number | undefined;

    // Toggle first-level navigation on mouse-over
    navToggle.addEventListener('mouseenter', () => {
      if (closeTimeout) {
        clearTimeout(closeTimeout);
      }
      navLevel1.classList.add('nav-open');
    });

    // Delayed close to allow moving to second-level menus
    const scheduleClose = (): void => {
      closeTimeout = window.setTimeout(() => {
        navLevel1.classList.remove('nav-open');
        // Close all second-level menus
        navItems.forEach((item) => {
          const level2 = item.querySelector('.nav-level-2');
          if (level2) {
            level2.classList.remove('nav-level-2-open');
          }
        });
      }, 300); // 300ms delay to move mouse to submenu
    };

    // Close navigation when mouse leaves toggle
    navToggle.addEventListener('mouseleave', scheduleClose);

    // Keep open when hovering over nav items or their submenus
    navItems.forEach((item) => {
      const level2 = item.querySelector('.nav-level-2');

      item.addEventListener('mouseenter', () => {
        if (closeTimeout) {
          clearTimeout(closeTimeout);
        }

        // Close other second-level menus
        navItems.forEach((otherItem) => {
          if (otherItem !== item) {
            const otherLevel2 = otherItem.querySelector('.nav-level-2');
            if (otherLevel2) {
              otherLevel2.classList.remove('nav-level-2-open');
            }
          }
        });

        // Open this item's submenu if it has one
        if (level2) {
          level2.classList.add('nav-level-2-open');
        }
      });

      item.addEventListener('mouseleave', (e) => {
        // Don't close if moving to the submenu
        const relatedTarget = e.relatedTarget as HTMLElement;
        if (level2 && level2.contains(relatedTarget)) {
          return;
        }
        scheduleClose();
      });

      // Keep open when hovering over second-level menu
      if (level2) {
        level2.addEventListener('mouseenter', () => {
          if (closeTimeout) {
            clearTimeout(closeTimeout);
          }
        });

        level2.addEventListener('mouseleave', scheduleClose);
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
    // Random speed for each column (between 0.3 and 1.0)
    const speeds: number[] = Array(columns)
      .fill(0)
      .map(() => 0.3 + Math.random() * 0.7);
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
        // Each column falls at its own speed
        drops[i] += speeds[i];
      }
    }

    // Animate at 20fps (slower than before which was 30fps)
    setInterval(draw, 50);
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
