/**
 * "Click to edit" hover badge, positioned via JS (position: fixed +
 * getBoundingClientRect()) instead of a position:absolute pseudo-element,
 * since the hovered element may have its own position:absolute in host CSS.
 */

let badgeEl: HTMLDivElement | null = null;

function ensureBadge(): HTMLDivElement | null {
  if (badgeEl) {
    return badgeEl;
  }
  const container = document.getElementById('editor-modal-container');
  if (!container) {
    return null;
  }
  badgeEl = document.createElement('div');
  badgeEl.className = 'zen-editable-hover-badge';
  badgeEl.textContent = '\u270E  Click to edit';
  badgeEl.setAttribute('aria-hidden', 'true');
  container.appendChild(badgeEl);
  return badgeEl;
}

function hideBadge(): void {
  badgeEl?.classList.remove('is-visible');
}

export function wireHoverBadge(): void {
  document.body.addEventListener('mouseover', (event: MouseEvent) => {
    const target = (event.target as HTMLElement).closest<HTMLElement>('[data-zen-editable]');
    if (!target) {
      return;
    }
    const badge = ensureBadge();
    if (!badge) {
      return;
    }
    const rect = target.getBoundingClientRect();
    badge.classList.add('is-visible');
    const badgeWidth = badge.getBoundingClientRect().width;
    badge.style.top = `${rect.top + 4}px`;
    badge.style.left = `${rect.right - badgeWidth - 6}px`;
  });

  document.body.addEventListener('mouseout', (event: MouseEvent) => {
    const target = (event.target as HTMLElement).closest<HTMLElement>('[data-zen-editable]');
    if (!target) {
      return;
    }
    const related = event.relatedTarget as Node | null;
    if (related && target.contains(related)) {
      return;
    }
    hideBadge();
  });
}
