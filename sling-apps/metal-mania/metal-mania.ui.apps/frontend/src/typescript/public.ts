/**
 * public.ts — Metal Mania public JavaScript entry point.
 *
 * Handles:
 * - Top navigation: mobile hamburger toggle
 * - Scroll-based header shrink (CSS-first, JS triggers class)
 * - Smooth scroll for anchor links
 * - IntersectionObserver for entrance animations
 */
(function (): void {
  'use strict';

  // ── Mobile nav toggle ────────────────────────────────────────────────────

  function initNavToggle(): void {
    const toggle = document.getElementById('nav-toggle');
    const navMenu = document.getElementById('nav-menu');
    if (!toggle || !navMenu) {
      return;
    }
    toggle.addEventListener('click', () => {
      const isOpen = navMenu.classList.toggle('is-open');
      toggle.setAttribute('aria-expanded', String(isOpen));
      toggle.setAttribute('aria-label', isOpen ? 'Close menu' : 'Open menu');
    });

    // Close nav when a link is clicked (mobile UX)
    navMenu.querySelectorAll('a').forEach((link) => {
      link.addEventListener('click', () => {
        navMenu.classList.remove('is-open');
        toggle.setAttribute('aria-expanded', 'false');
        toggle.setAttribute('aria-label', 'Open menu');
      });
    });
  }

  // ── Scroll-based header shrink ────────────────────────────────────────────

  function initStickyHeader(): void {
    const header = document.querySelector<HTMLElement>('.site-header');
    if (!header) {
      return;
    }
    const threshold = 60;
    const onScroll = (): void => {
      header.classList.toggle('is-scrolled', window.scrollY > threshold);
    };
    window.addEventListener('scroll', onScroll, { passive: true });
    onScroll();
  }

  // ── Entrance animations via IntersectionObserver ──────────────────────────

  function initEntranceAnimations(): void {
    const elements = document.querySelectorAll<HTMLElement>('[data-animate]');
    if (!elements.length || !('IntersectionObserver' in window)) {
      return;
    }
    const observer = new IntersectionObserver(
      (entries) => {
        entries.forEach((entry) => {
          if (entry.isIntersecting) {
            (entry.target as HTMLElement).classList.add('is-visible');
            observer.unobserve(entry.target);
          }
        });
      },
      { threshold: 0.1 }
    );
    elements.forEach((el) => observer.observe(el));
  }

  // ── Init ──────────────────────────────────────────────────────────────────

  function init(): void {
    initNavToggle();
    initStickyHeader();
    initEntranceAnimations();
  }

  if (document.readyState === 'loading') {
    document.addEventListener('DOMContentLoaded', init);
  } else {
    init();
  }
})();
