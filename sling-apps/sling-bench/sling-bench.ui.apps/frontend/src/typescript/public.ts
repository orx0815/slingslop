/**
 * public.ts — Sling Bench dashboard public JavaScript entry point.
 *
 * Two independent, self-contained pieces of behaviour:
 *  - initRunForm(): the script-generator panel (pure client-side, no fetch)
 *  - initCopyButton(): wires the "Copy to clipboard" button via the Clipboard API
 */
(function (): void {
  'use strict';

  function initRunForm(): void {
    const form = document.getElementById('run-form');
    if (!form) {
      return;
    }

    const baseUrlInput = document.getElementById('rf-base-url') as HTMLInputElement | null;
    const delayInput = document.getElementById('rf-delay-ms') as HTMLInputElement | null;
    const durationInput = document.getElementById('rf-duration') as HTMLInputElement | null;
    const concurrenciesInput = document.getElementById(
      'rf-concurrencies'
    ) as HTMLInputElement | null;
    const output = document.getElementById('generated-script');

    if (!baseUrlInput || !delayInput || !durationInput || !concurrenciesInput || !output) {
      return;
    }

    function render(): void {
      const baseUrl = (baseUrlInput!.value || 'http://localhost:8080').trim();
      const delay = (delayInput!.value || '100').trim();
      const duration = (durationInput!.value || '10').trim();
      const concurrencies = (concurrenciesInput!.value || '10,50,200,500').trim();
      output!.textContent = `./vt-bench.sh ${baseUrl} ${delay} ${duration} ${concurrencies}`;
    }

    [baseUrlInput, delayInput, durationInput, concurrenciesInput].forEach((input) => {
      input.addEventListener('input', render);
    });
    render();
  }

  function initCopyButton(): void {
    const button = document.getElementById('rf-copy-btn');
    const output = document.getElementById('generated-script');
    const feedback = document.getElementById('rf-copy-feedback');
    if (!button || !output) {
      return;
    }

    button.addEventListener('click', () => {
      const text = output.textContent || '';
      if (navigator.clipboard && navigator.clipboard.writeText) {
        navigator.clipboard
          .writeText(text)
          .then(() => showFeedback('Copied!'))
          .catch(() => showFeedback('Could not copy — select and copy manually.'));
      } else {
        showFeedback('Clipboard API unavailable — select and copy manually.');
      }
    });

    function showFeedback(message: string): void {
      if (!feedback) {
        return;
      }
      feedback.textContent = message;
      window.setTimeout(() => {
        feedback.textContent = '';
      }, 2000);
    }
  }

  document.addEventListener('DOMContentLoaded', () => {
    initRunForm();
    initCopyButton();
  });
})();
