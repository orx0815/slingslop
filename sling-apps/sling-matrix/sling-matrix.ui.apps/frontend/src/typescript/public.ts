/**
 * public.ts — Sling Matrix public JavaScript entry point
 *
 * Handles client-side interactions for the Sling Matrix documentation site:
 * - Circular navigation menu (top-left, opens on mouse-over)
 * - Code syntax highlighting with highlight.js
 * - Matrix digital rain effect (optional)
 */

import hljs from 'highlight.js/lib/core';
import bash from 'highlight.js/lib/languages/bash';
import css from 'highlight.js/lib/languages/css';
import xml from 'highlight.js/lib/languages/xml';
import ini from 'highlight.js/lib/languages/ini';
import javascript from 'highlight.js/lib/languages/javascript';
import json from 'highlight.js/lib/languages/json';
import plaintext from 'highlight.js/lib/languages/plaintext';
import shell from 'highlight.js/lib/languages/shell';
import stylus from 'highlight.js/lib/languages/stylus';
import java from 'highlight.js/lib/languages/java';
import typescript from 'highlight.js/lib/languages/typescript';

hljs.registerLanguage('bash', bash);
hljs.registerLanguage('sh', bash);
hljs.registerLanguage('css', css);
hljs.registerLanguage('html', xml);
hljs.registerLanguage('xml', xml);
hljs.registerLanguage('ini', ini);
hljs.registerLanguage('javascript', javascript);
hljs.registerLanguage('js', javascript);
hljs.registerLanguage('json', json);
hljs.registerLanguage('plaintext', plaintext);
hljs.registerLanguage('text', plaintext);
hljs.registerLanguage('shell', shell);
hljs.registerLanguage('stylus', stylus);
hljs.registerLanguage('java', java);
hljs.registerLanguage('typescript', typescript);
hljs.registerLanguage('ts', typescript);

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
   *
   * Fine-pointer (mouse) devices use hover: the grip opens the ring and each
   * bubble/list item reveals its own nested submenu (".nav-submenu", any
   * depth) on hover.
   *
   * Coarse-pointer (touch) devices have no hover, so everything is tap-driven:
   * tap the grip to open/close the ring, tap an item to reveal its submenu
   * (a second tap on the same item follows the link), and tap outside to close.
   */
  function initCircularNav(): void {
    const navToggle = document.querySelector('.nav-toggle');
    const navLevel1 = document.querySelector('.nav-level-1') as HTMLElement | null;
    const nav = document.querySelector('.matrix-nav');

    if (!navToggle || !navLevel1) {
      return;
    }

    // Every item that can own a submenu, at any depth.
    const navItems = navLevel1.querySelectorAll('.nav-item');

    // The submenu directly owned by an item (not a deeper-nested one).
    const submenuOf = (item: Element): HTMLElement | null =>
      item.querySelector(':scope > .nav-submenu');

    // Clamp a submenu into the viewport on both axes.
    const clampIntoViewport = (el: HTMLElement): void => {
      // Reset first so getBoundingClientRect() measures the true final position
      // (the base CSS transform is translateY(-50%), no transition to fight).
      el.style.transform = '';
      const rect = el.getBoundingClientRect();
      const margin = 8;
      let dx = 0;
      let dy = 0;
      if (rect.top < margin) {
        dy = margin - rect.top;
      } else if (rect.bottom > window.innerHeight - margin) {
        dy = -(rect.bottom - (window.innerHeight - margin));
      }
      if (rect.right > window.innerWidth - margin) {
        dx = -(rect.right - (window.innerWidth - margin));
      } else if (rect.left < margin) {
        dx = margin - rect.left;
      }
      if (dx !== 0 || dy !== 0) {
        el.style.transform = `translate(${dx}px, calc(-50% + ${dy}px))`;
      }
    };

    // Close a submenu and everything nested inside it, so reopening a branch
    // never resurfaces a stale deeper submenu.
    const closeSubmenu = (submenu: Element): void => {
      submenu.classList.remove('nav-submenu-open');
      submenu.querySelectorAll('.nav-submenu').forEach((nested) => {
        nested.classList.remove('nav-submenu-open');
      });
    };

    // Close every OTHER submenu in the same list as `item`, so only one
    // branch of the tree is expanded at a time — ancestors are left alone.
    const closeSiblingSubmenus = (item: Element): void => {
      const parentList = item.parentElement;
      if (!parentList) {
        return;
      }
      Array.from(parentList.children).forEach((sibling) => {
        if (sibling === item) {
          return;
        }
        const siblingSubmenu = submenuOf(sibling);
        if (siblingSubmenu) {
          closeSubmenu(siblingSubmenu);
        }
      });
    };

    const closeAll = (): void => {
      navLevel1.classList.remove('nav-open');
      navLevel1.querySelectorAll('.nav-submenu').forEach((submenu) => closeSubmenu(submenu));
    };

    const canHover = window.matchMedia('(hover: hover) and (pointer: fine)').matches;

    // ── Touch / coarse-pointer: tap-driven ─────────────────────────────────
    if (!canHover) {
      navToggle.addEventListener('click', (e) => {
        e.stopPropagation();
        if (navLevel1.classList.contains('nav-open')) {
          closeAll();
        } else {
          navLevel1.classList.add('nav-open');
        }
      });

      navItems.forEach((item) => {
        const submenu = submenuOf(item);
        const link = item.querySelector(':scope > a') as HTMLElement | null;
        if (!submenu || !link) {
          return; // Childless item — its link navigates on the first tap.
        }
        link.addEventListener('click', (e) => {
          if (!submenu.classList.contains('nav-submenu-open')) {
            // First tap reveals the submenu instead of navigating.
            e.preventDefault();
            e.stopPropagation();
            closeSiblingSubmenus(item);
            submenu.classList.add('nav-submenu-open');
            clampIntoViewport(submenu);
          }
          // Second tap on the same item falls through and follows the link.
        });
      });

      // Tap anywhere outside the nav closes it.
      document.addEventListener('click', (e) => {
        const target = e.target as Node | null;
        if (nav && target && !nav.contains(target)) {
          closeAll();
        }
      });

      return;
    }

    // ── Fine-pointer (mouse): hover-driven ─────────────────────────────────
    let closeTimeout: number | undefined;

    navToggle.addEventListener('mouseenter', () => {
      if (closeTimeout) {
        clearTimeout(closeTimeout);
      }
      navLevel1.classList.add('nav-open');
    });

    // Delayed close so the pointer can travel to a submenu.
    const scheduleClose = (): void => {
      closeTimeout = window.setTimeout(closeAll, 300);
    };

    navToggle.addEventListener('mouseleave', scheduleClose);

    navItems.forEach((item) => {
      const submenu = submenuOf(item);

      // mouseenter/mouseleave don't bubble, so each item's own hitbox is
      // independent regardless of nesting depth.
      item.addEventListener('mouseenter', () => {
        if (closeTimeout) {
          clearTimeout(closeTimeout);
        }
        closeSiblingSubmenus(item);
        if (submenu) {
          submenu.classList.add('nav-submenu-open');
          clampIntoViewport(submenu);
        }
      });

      item.addEventListener('mouseleave', (e) => {
        // Don't close if the pointer is moving into this item's own submenu.
        const relatedTarget = (e as MouseEvent).relatedTarget as HTMLElement | null;
        if (submenu && relatedTarget && submenu.contains(relatedTarget)) {
          return;
        }
        scheduleClose();
      });

      if (submenu) {
        submenu.addEventListener('mouseenter', () => {
          if (closeTimeout) {
            clearTimeout(closeTimeout);
          }
        });
        submenu.addEventListener('mouseleave', scheduleClose);
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
    document.body.addEventListener('htmx:after:swap', () => {
      highlightCode();
    });
  });
})();
