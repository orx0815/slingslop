/**
 * public.ts — Cyberpunk Alpaca public JavaScript entry point.
 *
 * Kept deliberately lean: the visual work is done in CSS. This only handles the
 * couple of interactions CSS cannot do on its own — the mobile nav toggle and a
 * scroll-reveal fallback for browsers without scroll-driven animation support.
 */
(function (): void {
  'use strict';

  function initNavToggle(): void {
    const toggle = document.querySelector<HTMLButtonElement>('[data-nav-toggle]');
    const menu = document.querySelector<HTMLElement>('[data-nav-menu]');
    if (!toggle || !menu) {
      return;
    }
    toggle.addEventListener('click', () => {
      const open = menu.classList.toggle('is-open');
      toggle.setAttribute('aria-expanded', String(open));
    });
  }

  function initScrollReveal(): void {
    const targets = document.querySelectorAll<HTMLElement>('[data-reveal]');
    if (targets.length === 0) {
      return;
    }
    // If scroll-driven animations are supported, CSS handles the reveal.
    if (CSS.supports('animation-timeline: view()')) {
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
      { threshold: 0.15 },
    );
    targets.forEach((el) => observer.observe(el));
  }

  function init(): void {
    initNavToggle();
    initScrollReveal();
  }

  if (document.readyState === 'loading') {
    document.addEventListener('DOMContentLoaded', init);
  } else {
    init();
  }
})();
