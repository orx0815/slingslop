/**
 * public.ts — Alf vs Agent Smith public JavaScript entry point
 */
(function (): void {
  'use strict';

  // Animate elements on scroll into view
  const observer = new IntersectionObserver(
    (entries) => {
      for (const entry of entries) {
        if (entry.isIntersecting) {
          entry.target.classList.add('in-view');
          observer.unobserve(entry.target);
        }
      }
    },
    { threshold: 0.15 }
  );

  document.querySelectorAll('[data-animate]').forEach((el) => observer.observe(el));
})();
