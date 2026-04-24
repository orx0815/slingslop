/**
 * public.ts — Flower Power public JavaScript entry point
 *
 * Adds playful interactions: smooth scroll, navigation toggle, fade-in animations
 */

(function (): void {
  'use strict';

  // ── Smooth scroll for anchor links ──────────────────────────────────────
  function initSmoothScroll(): void {
    document.querySelectorAll('a[href^="#"]').forEach((anchor) => {
      anchor.addEventListener('click', function (e: Event): void {
        const href = (e.currentTarget as HTMLAnchorElement).getAttribute('href');
        if (href && href !== '#') {
          e.preventDefault();
          const target = document.querySelector(href);
          if (target) {
            target.scrollIntoView({
              behavior: 'smooth',
              block: 'start',
            });
          }
        }
      });
    });
  }

  // ── Mobile navigation toggle ────────────────────────────────────────────
  function initNavToggle(): void {
    const navToggle = document.querySelector('[data-nav-toggle]');
    const navMenu = document.querySelector('[data-nav-menu]');

    if (navToggle && navMenu) {
      navToggle.addEventListener('click', (): void => {
        const isExpanded = navToggle.getAttribute('aria-expanded') === 'true';
        navToggle.setAttribute('aria-expanded', String(!isExpanded));
        navMenu.classList.toggle('is-open');
      });

      // Close menu on Escape
      document.addEventListener('keydown', (e: KeyboardEvent): void => {
        if (e.key === 'Escape' && navMenu.classList.contains('is-open')) {
          navToggle.setAttribute('aria-expanded', 'false');
          navMenu.classList.remove('is-open');
        }
      });
    }
  }

  // ── Fade-in animation on scroll (IntersectionObserver) ─────────────────
  function initScrollAnimations(): void {
    const fadeElements = document.querySelectorAll<HTMLElement>('[data-fade-in]');

    if (fadeElements.length === 0) {
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
      {
        threshold: 0.1,
        rootMargin: '0px 0px -50px 0px',
      }
    );

    fadeElements.forEach((el) => {
      observer.observe(el);
    });
  }

  // ── Initialize everything ───────────────────────────────────────────────
  function init(): void {
    initSmoothScroll();
    initNavToggle();
    initScrollAnimations();
  }

  if (document.readyState === 'loading') {
    document.addEventListener('DOMContentLoaded', init);
  } else {
    init();
  }
})();
