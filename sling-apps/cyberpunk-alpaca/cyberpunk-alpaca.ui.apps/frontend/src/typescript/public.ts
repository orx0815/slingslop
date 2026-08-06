/**
 * public.ts — Cyberpunk Alpaca public JavaScript entry point
 *
 * Handles nav toggling, scroll-driven effects, and entrance animations
 * for the Cyberpunk Alpaca public site.
 */
(function (): void {
  'use strict';

  // ── Navigation hamburger toggle ──────────────────────────────────────────
  const navToggle = document.querySelector<HTMLButtonElement>('.nav-toggle');
  const navMenu = document.querySelector<HTMLElement>('.nav-menu');

  if (navToggle && navMenu) {
    navToggle.addEventListener('click', () => {
      const isOpen = navMenu.classList.toggle('nav-menu--open');
      navToggle.setAttribute('aria-expanded', String(isOpen));
      navToggle.classList.toggle('nav-toggle--open', isOpen);
    });

    // Close on outside click
    document.addEventListener('click', (e) => {
      if (!navToggle.contains(e.target as Node) && !navMenu.contains(e.target as Node)) {
        navMenu.classList.remove('nav-menu--open');
        navToggle.setAttribute('aria-expanded', 'false');
        navToggle.classList.remove('nav-toggle--open');
      }
    });
  }

  // ── Scroll-triggered entrance animations ────────────────────────────────
  const animTargets = document.querySelectorAll<HTMLElement>('[data-animate]');

  if (animTargets.length > 0 && 'IntersectionObserver' in window) {
    const observer = new IntersectionObserver(
      (entries) => {
        entries.forEach((entry) => {
          if (entry.isIntersecting) {
            entry.target.classList.add('animate--visible');
            observer.unobserve(entry.target);
          }
        });
      },
      { threshold: 0.15 }
    );

    animTargets.forEach((el) => observer.observe(el));
  }

  // ── Glitch text effect on hover ─────────────────────────────────────────
  const glitchEls = document.querySelectorAll<HTMLElement>('[data-glitch]');
  glitchEls.forEach((el) => {
    el.addEventListener('mouseenter', () => {
      el.classList.add('glitch--active');
    });
    el.addEventListener('mouseleave', () => {
      el.classList.remove('glitch--active');
    });
  });
})();
