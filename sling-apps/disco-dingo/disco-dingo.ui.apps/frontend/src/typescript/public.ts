/**
 * public.ts — public (non-editor) JavaScript entry point
 *
 * Kept intentionally lean: CSS handles as much of the disco vibe as
 * possible (gradients, glow, animation). This file only wires up the
 * handful of interactions that genuinely need JS:
 *
 *   - Top navigation bar mobile toggle
 *   - Smooth-scroll for on-page anchor links
 *   - Reveal-on-scroll animation trigger (adds a class; the actual
 *     animation is defined in 09-animations.css)
 */

(function (): void {
  'use strict';

  function initNavToggle(): void {
    const toggle = document.querySelector<HTMLButtonElement>('.nav-toggle');
    const menu = document.querySelector<HTMLElement>('.nav-menu');
    if (!toggle || !menu) {
      return;
    }

    toggle.addEventListener('click', () => {
      const isOpen = menu.classList.toggle('nav-menu--open');
      toggle.setAttribute('aria-expanded', String(isOpen));
    });

    menu.querySelectorAll('a').forEach((link) => {
      link.addEventListener('click', () => {
        menu.classList.remove('nav-menu--open');
        toggle.setAttribute('aria-expanded', 'false');
      });
    });
  }

  function initSmoothScroll(): void {
    document.querySelectorAll<HTMLAnchorElement>('a[href^="#"]').forEach((anchor) => {
      anchor.addEventListener('click', (event) => {
        const targetId = anchor.getAttribute('href');
        if (!targetId || targetId === '#') {
          return;
        }
        const target = document.querySelector(targetId);
        if (!target) {
          return;
        }
        event.preventDefault();
        target.scrollIntoView({ behavior: 'smooth', block: 'start' });
      });
    });
  }

  function initRevealOnScroll(): void {
    const targets = document.querySelectorAll<HTMLElement>('[data-reveal]');
    if (targets.length === 0) {
      return;
    }

    if (!('IntersectionObserver' in window)) {
      targets.forEach((el) => el.classList.add('is-revealed'));
      return;
    }

    const observer = new IntersectionObserver(
      (entries) => {
        entries.forEach((entry) => {
          if (entry.isIntersecting) {
            entry.target.classList.add('is-revealed');
            observer.unobserve(entry.target);
          }
        });
      },
      { threshold: 0.15 }
    );

    targets.forEach((el) => observer.observe(el));
  }

  document.addEventListener('DOMContentLoaded', () => {
    initNavToggle();
    initSmoothScroll();
    initRevealOnScroll();
  });
})();
