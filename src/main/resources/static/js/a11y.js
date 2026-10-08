/**
 * AuthEase - Accessibility Utilities (WCAG 2.2 AA Compliance Engine)
 */

const THEME_KEY = 'authease.theme';
const TEXT_SIZE_KEY = 'authease.text-size';
const GUIDED_MODE_KEY = 'authease.guided-mode';

let liveRegionPolite = null;
let liveRegionAssertive = null;

/**
 * Initialize persistent accessibility preferences
 */
export function initA11y() {
  ensureLiveRegions();

  // 1. Theme
  const savedTheme = localStorage.getItem(THEME_KEY);
  if (savedTheme) {
    setTheme(savedTheme, false);
  } else {
    // Check system preference
    if (window.matchMedia && window.matchMedia('(prefers-color-scheme: dark)').matches) {
      setTheme('dark', false);
    } else {
      setTheme('light', false);
    }
  }

  // 2. Text Size
  const savedTextSize = localStorage.getItem(TEXT_SIZE_KEY) || 'md';
  setTextSize(savedTextSize, false);

  // 3. Guided Mode
  const guidedModeActive = localStorage.getItem(GUIDED_MODE_KEY) === '1';
  setGuidedMode(guidedModeActive, false);
}

/**
 * Ensure aria-live announcement regions exist in DOM
 */
function ensureLiveRegions() {
  if (!liveRegionPolite) {
    liveRegionPolite = document.createElement('div');
    liveRegionPolite.id = 'ae-live-polite';
    liveRegionPolite.setAttribute('aria-live', 'polite');
    liveRegionPolite.setAttribute('aria-atomic', 'true');
    liveRegionPolite.className = 'visually-hidden';
    document.body.appendChild(liveRegionPolite);
  }

  if (!liveRegionAssertive) {
    liveRegionAssertive = document.createElement('div');
    liveRegionAssertive.id = 'ae-live-assertive';
    liveRegionAssertive.setAttribute('role', 'alert');
    liveRegionAssertive.setAttribute('aria-live', 'assertive');
    liveRegionAssertive.setAttribute('aria-atomic', 'true');
    liveRegionAssertive.className = 'visually-hidden';
    document.body.appendChild(liveRegionAssertive);
  }
}

/**
 * Announce message to assistive technology
 * @param {string} message 
 * @param {'polite'|'assertive'} priority 
 */
export function announce(message, priority = 'polite') {
  ensureLiveRegions();
  const region = priority === 'assertive' ? liveRegionAssertive : liveRegionPolite;
  // Clear and update to force screen readers to read repetitive text
  region.textContent = '';
  setTimeout(() => {
    region.textContent = message;
  }, 50);
}

/**
 * Set and persist theme
 * @param {'light'|'dark'|'high-contrast'} theme 
 * @param {boolean} notify 
 */
export function setTheme(theme, notify = true) {
  document.documentElement.setAttribute('data-theme', theme);
  localStorage.setItem(THEME_KEY, theme);

  // Update theme toggle buttons aria-pressed states if present
  document.querySelectorAll('[data-action-theme]').forEach(btn => {
    const isCurrent = btn.getAttribute('data-action-theme') === theme;
    btn.setAttribute('aria-pressed', isCurrent ? 'true' : 'false');
    btn.classList.toggle('active', isCurrent);
  });

  if (notify) {
    announce(`Theme switched to ${theme.replace('-', ' ')}`);
  }
}

export function getTheme() {
  return document.documentElement.getAttribute('data-theme') || 'light';
}

/**
 * Set and persist text size scale
 * @param {'sm'|'md'|'lg'} size 
 * @param {boolean} notify 
 */
export function setTextSize(size, notify = true) {
  document.documentElement.setAttribute('data-text-size', size);
  localStorage.setItem(TEXT_SIZE_KEY, size);

  document.querySelectorAll('[data-action-text-size]').forEach(btn => {
    const isCurrent = btn.getAttribute('data-action-text-size') === size;
    btn.setAttribute('aria-pressed', isCurrent ? 'true' : 'false');
    btn.classList.toggle('active', isCurrent);
  });

  if (notify) {
    const sizeName = size === 'sm' ? 'smaller' : size === 'lg' ? 'larger' : 'normal';
    announce(`Text size set to ${sizeName}`);
  }
}

export function getTextSize() {
  return document.documentElement.getAttribute('data-text-size') || 'md';
}

/**
 * Set and persist Guided Mode
 * @param {boolean} enabled 
 * @param {boolean} notify 
 */
export function setGuidedMode(enabled, notify = true) {
  if (enabled) {
    document.body.classList.add('guided-mode');
    localStorage.setItem(GUIDED_MODE_KEY, '1');
  } else {
    document.body.classList.remove('guided-mode');
    localStorage.removeItem(GUIDED_MODE_KEY);
  }

  const toggleBtn = document.getElementById('ae-guided-toggle');
  if (toggleBtn) {
    toggleBtn.setAttribute('aria-checked', enabled ? 'true' : 'false');
    toggleBtn.classList.toggle('active', enabled);
  }

  if (notify) {
    announce(enabled ? 'Guided mode enabled. Showing extra hints.' : 'Guided mode turned off.');
  }
}

export function isGuidedMode() {
  return document.body.classList.contains('guided-mode');
}

/**
 * Focus helper with safety checks
 * @param {HTMLElement|string} target 
 */
export function focusTarget(target) {
  const el = typeof target === 'string' ? document.querySelector(target) : target;
  if (!el) return;

  // Ensure element is focusable if it's a heading or container
  if (!el.hasAttribute('tabindex') && !/^(BUTTON|INPUT|SELECT|TEXTAREA|A)$/.test(el.tagName)) {
    el.setAttribute('tabindex', '-1');
  }
  el.focus();
}

/**
 * Update page title and announce new step heading
 * @param {string} pageTitle 
 * @param {HTMLElement|string} headingTarget 
 */
export function setStepHeading(pageTitle, headingTarget) {
  document.title = pageTitle;
  if (headingTarget) {
    focusTarget(headingTarget);
    const text = typeof headingTarget === 'string' 
      ? document.querySelector(headingTarget)?.textContent 
      : headingTarget?.textContent;
    if (text) {
      announce(text);
    }
  }
}
