/**
 * public.ts — Rage Against the Machine Learning public JavaScript entry point
 *
 * Kept deliberately lean: the riot is CSS-driven. JS only handles
 *   • the mobile hamburger toggle for the top-bar navigation
 *   • closing open dropdowns on outside click / Escape
 *   • an IntersectionObserver that reveals rant sections as they scroll in
 */
(function (): void {
  'use strict';

  /* ── Top-bar navigation ─────────────────────────────────────────────── */

  function initNav(): void {
    const nav = document.querySelector('.rage-nav');
    if (!nav) {
      return;
    }

    const burger = nav.querySelector<HTMLButtonElement>('.rage-nav__burger');
    const list = nav.querySelector<HTMLUListElement>('.rage-nav__list');
    if (burger && list) {
      burger.addEventListener('click', () => {
        const open = nav.classList.toggle('rage-nav--open');
        burger.setAttribute('aria-expanded', open ? 'true' : 'false');
      });
    }

    // Section dropdowns: click toggles (works for touch), outside click closes.
    nav.querySelectorAll<HTMLElement>('.rage-nav__item--parent').forEach((item) => {
      const link = item.querySelector<HTMLAnchorElement>(':scope > a');
      if (!link) {
        return;
      }
      link.addEventListener('click', (e) => {
        // First tap/click opens the dropdown; second follows the link.
        if (!item.classList.contains('rage-nav__item--dropdown-open')) {
          e.preventDefault();
          nav
            .querySelectorAll('.rage-nav__item--dropdown-open')
            .forEach((other) => other.classList.remove('rage-nav__item--dropdown-open'));
          item.classList.add('rage-nav__item--dropdown-open');
        }
      });
    });

    document.addEventListener('click', (e) => {
      if (!(e.target instanceof Node) || !nav.contains(e.target)) {
        nav
          .querySelectorAll('.rage-nav__item--dropdown-open')
          .forEach((item) => item.classList.remove('rage-nav__item--dropdown-open'));
      }
    });

    document.addEventListener('keydown', (e) => {
      if (e.key === 'Escape') {
        nav.classList.remove('rage-nav--open');
        nav
          .querySelectorAll('.rage-nav__item--dropdown-open')
          .forEach((item) => item.classList.remove('rage-nav__item--dropdown-open'));
      }
    });
  }

  /* ── Scroll-in reveal for rant content ──────────────────────────────── */

  function initReveal(): void {
    const targets = document.querySelectorAll('.text-block, .pull-quote');
    if (targets.length === 0 || !('IntersectionObserver' in window)) {
      targets.forEach((t) => t.classList.add('is-visible'));
      return;
    }
    const observer = new IntersectionObserver(
      (entries) => {
        entries.forEach((entry) => {
          if (entry.isIntersecting) {
            entry.target.classList.add('is-visible');
            observer.unobserve(entry.target);
          }
        });
      },
      { rootMargin: '0px 0px -10% 0px' }
    );
    targets.forEach((t) => observer.observe(t));
  }

  if (document.readyState === 'loading') {
    document.addEventListener('DOMContentLoaded', () => {
      initNav();
      initReveal();
    });
  } else {
    initNav();
    initReveal();
  }
})();
