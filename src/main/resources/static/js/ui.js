/**
 * AuthEase - Shared UI Components & Accessibility Shell
 * 
 * WCAG 2.2 AA Compliant
 * Strict Security: Never injects raw server strings with innerHTML; uses safe textContent.
 */

import { initA11y, setTheme, setTextSize, setGuidedMode, getTheme, getTextSize, isGuidedMode, focusTarget } from './a11y.js';
import { initSpeech, toggleSpeech } from './speech.js';
import { messages } from './messages.en.js';
import { api } from './api.js';

/**
 * Mount the full application layout shell (Header, Controls, Footer, Demo Panel)
 * @param {Object} options 
 */
export function mountShell(options = {}) {
  initA11y();
  initSpeech();

  mountHeader(options);
  mountFooter(options);
  mountDemoPanel();

  // Initialize API on page load (checks /api/auth/me for XSRF and session status)
  api.init().catch(err => console.warn('API init notice:', err));
}

/**
 * Top Navigation Header with Accessibility Controls
 */
function mountHeader(options = {}) {
  let header = document.querySelector('header.app-header');
  if (!header) {
    header = document.createElement('header');
    header.className = 'app-header';
    header.setAttribute('role', 'banner');
    document.body.prepend(header);
  }

  // Insert Skip Link as the very first interactive element in <body>
  let skipLink = document.querySelector('.skip-link');
  if (!skipLink) {
    skipLink = document.createElement('a');
    skipLink.href = '#main-content';
    skipLink.className = 'skip-link';
    skipLink.textContent = messages.app.skipToContent;
    document.body.prepend(skipLink);
  }

  const currentTheme = getTheme();
  const currentSize = getTextSize();
  const isGuided = isGuidedMode();
  const inMock = api.isMockMode();

  header.innerHTML = `
    <div class="container-fluid px-3 px-md-4 py-2">
      <div class="d-flex flex-wrap align-items-center justify-content-between gap-2">
        <!-- Brand -->
        <div class="d-flex align-items-center gap-3">
          <a href="index.html" class="brand-logo" aria-label="AuthEase Home">
            <svg width="28" height="28" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2.2" stroke-linecap="round" stroke-linejoin="round" aria-hidden="true">
              <path d="M12 22s8-4 8-10V5l-8-3-8 3v7c0 6 8 10 8 10z"/>
              <path d="m9 12 2 2 4-4"/>
            </svg>
            <span>AuthEase</span>
          </a>
          ${inMock ? '<span class="badge bg-primary text-white" style="font-size:0.75rem;">MOCK MODE</span>' : ''}
        </div>

        <!-- Accessibility & Theme Controls Toolbar -->
        <nav aria-label="Accessibility options" class="d-flex flex-wrap align-items-center gap-2">
          
          <!-- Read Aloud Button -->
          <button type="button" class="read-aloud-btn" id="ae-read-aloud-btn" aria-pressed="false" aria-label="Read instructions on this screen aloud">
            <svg width="18" height="18" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2" aria-hidden="true">
              <polygon points="11 5 6 9 2 9 2 15 6 15 11 19 11 5"></polygon>
              <path d="M15.54 8.46a5 5 0 0 1 0 7.07"></path>
              <path d="M19.07 4.93a10 10 0 0 1 0 14.14"></path>
            </svg>
            <span class="read-aloud-text">${messages.controls.readAloud}</span>
          </button>

          <!-- Text Size Controls -->
          <div class="btn-group" role="group" aria-label="Text sizing">
            <button type="button" class="btn btn-sm btn-ae-outline text-size-btn ${currentSize === 'sm' ? 'active' : ''}" data-action-text-size="sm" aria-pressed="${currentSize === 'sm'}" aria-label="Smaller text">A-</button>
            <button type="button" class="btn btn-sm btn-ae-outline text-size-btn ${currentSize === 'md' ? 'active' : ''}" data-action-text-size="md" aria-pressed="${currentSize === 'md'}" aria-label="Normal text">A</button>
            <button type="button" class="btn btn-sm btn-ae-outline text-size-btn ${currentSize === 'lg' ? 'active' : ''}" data-action-text-size="lg" aria-pressed="${currentSize === 'lg'}" aria-label="Larger text">A+</button>
          </div>

          <!-- Theme Selector -->
          <div class="btn-group" role="group" aria-label="Theme mode">
            <button type="button" class="btn btn-sm btn-ae-outline theme-btn ${currentTheme === 'light' ? 'active' : ''}" data-action-theme="light" aria-pressed="${currentTheme === 'light'}" aria-label="Light mode">
              <svg width="16" height="16" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2" aria-hidden="true"><circle cx="12" cy="12" r="5"/><line x1="12" y1="1" x2="12" y2="3"/><line x1="12" y1="21" x2="12" y2="23"/><line x1="4.22" y1="4.22" x2="5.64" y2="5.64"/><line x1="18.36" y1="18.36" x2="19.78" y2="19.78"/><line x1="1" y1="12" x2="3" y2="12"/><line x1="21" y1="12" x2="23" y2="12"/><line x1="4.22" y1="19.78" x2="5.64" y2="18.36"/><line x1="18.36" y1="5.64" x2="19.78" y2="4.22"/></svg>
            </button>
            <button type="button" class="btn btn-sm btn-ae-outline theme-btn ${currentTheme === 'dark' ? 'active' : ''}" data-action-theme="dark" aria-pressed="${currentTheme === 'dark'}" aria-label="Dark mode">
              <svg width="16" height="16" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2" aria-hidden="true"><path d="M21 12.79A9 9 0 1 1 11.21 3 7 7 0 0 0 21 12.79z"/></svg>
            </button>
            <button type="button" class="btn btn-sm btn-ae-outline theme-btn ${currentTheme === 'high-contrast' ? 'active' : ''}" data-action-theme="high-contrast" aria-pressed="${currentTheme === 'high-contrast'}" aria-label="High contrast mode">
              <span style="font-weight:900; font-size:0.75rem;">HC</span>
            </button>
          </div>

          <!-- Guided Mode Toggle -->
          <div class="form-check form-switch m-0 d-inline-flex align-items-center gap-1">
            <input class="form-check-input" type="checkbox" role="switch" id="ae-guided-toggle" ${isGuided ? 'checked' : ''} aria-label="Toggle guided simple mode">
            <label class="form-check-label small d-none d-sm-inline" for="ae-guided-toggle">Guided</label>
          </div>

          <!-- Navigation Links -->
          <a href="help.html" class="btn btn-sm btn-ae-outline" aria-label="Help and plain language guides">Help</a>
        </nav>
      </div>
    </div>
  `;

  // Attach event listeners safely
  header.querySelectorAll('[data-action-theme]').forEach(btn => {
    btn.addEventListener('click', () => {
      setTheme(btn.getAttribute('data-action-theme'));
    });
  });

  header.querySelectorAll('[data-action-text-size]').forEach(btn => {
    btn.addEventListener('click', () => {
      setTextSize(btn.getAttribute('data-action-text-size'));
    });
  });

  const guidedToggle = header.querySelector('#ae-guided-toggle');
  if (guidedToggle) {
    guidedToggle.addEventListener('change', (e) => {
      setGuidedMode(e.target.checked);
    });
  }

  const readAloudBtn = header.querySelector('#ae-read-aloud-btn');
  if (readAloudBtn) {
    readAloudBtn.addEventListener('click', () => {
      toggleSpeech('#main-content', readAloudBtn);
    });
  }
}

/**
 * Footer Landmark
 */
function mountFooter() {
  let footer = document.querySelector('footer.app-footer');
  if (!footer) {
    footer = document.createElement('footer');
    footer.className = 'app-footer mt-auto';
    footer.setAttribute('role', 'contentinfo');
    document.body.appendChild(footer);
  }

  footer.innerHTML = `
    <div class="container text-center">
      <p class="mb-1 fw-semibold">${messages.app.tagline}</p>
      <p class="text-muted small mb-2">${messages.app.footerText}</p>
      <div class="d-flex justify-content-center gap-3 small">
        <a href="help.html" class="text-decoration-underline">Help & Guidance</a>
        <span>•</span>
        <a href="dev-outbox.html" class="text-decoration-underline">Simulated Dev Outbox</a>
        <span>•</span>
        <a href="admin.html" class="text-decoration-underline">Admin Dashboard</a>
        <span>•</span>
        <a href="index.html" class="text-decoration-underline">Home</a>
      </div>
    </div>
  `;
}

/**
 * Accessible Error Summary Box (WCAG 3.3.1 & 3.3.3)
 * @param {HTMLElement} container 
 * @param {Array<{fieldId?: string, message: string}>} errors 
 * @param {string} [customTitle]
 */
export function showErrorSummary(container, errors, customTitle) {
  let box = container.querySelector('.error-summary');
  if (!box) {
    box = document.createElement('div');
    box.className = 'error-summary';
    box.setAttribute('role', 'alert');
    box.setAttribute('tabindex', '-1');
    container.prepend(box);
  }

  box.innerHTML = '';

  const h2 = document.createElement('h2');
  h2.innerHTML = `
    <svg width="22" height="22" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2.5" aria-hidden="true">
      <circle cx="12" cy="12" r="10"></circle>
      <line x1="12" y1="8" x2="12" y2="12"></line>
      <line x1="12" y1="16" x2="12.01" y2="16"></line>
    </svg>
    <span>${customTitle || messages.a11y.errorSummaryTitle}</span>
  `;
  box.appendChild(h2);

  const ul = document.createElement('ul');
  errors.forEach(err => {
    const li = document.createElement('li');
    if (err.fieldId) {
      const a = document.createElement('a');
      a.href = `#${err.fieldId}`;
      a.textContent = err.message;
      a.addEventListener('click', (e) => {
        e.preventDefault();
        const targetField = document.getElementById(err.fieldId);
        if (targetField) targetField.focus();
      });
      li.appendChild(a);
    } else {
      li.textContent = err.message;
    }
    ul.appendChild(li);
  });
  box.appendChild(ul);

  box.focus();
}

export function clearErrorSummary(container) {
  const box = container.querySelector('.error-summary');
  if (box) box.remove();
}

/**
 * Accessible Risk Badge (Icon + Text, WCAG 1.4.1)
 * @param {'LOW'|'MEDIUM'|'HIGH'} level 
 * @returns {string} HTML string
 */
export function renderRiskBadge(level) {
  switch (level) {
    case 'LOW':
      return `
        <span class="badge-risk badge-risk-low" role="status">
          <svg width="14" height="14" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="3" aria-hidden="true">
            <polyline points="20 6 9 17 4 12"></polyline>
          </svg>
          <span>Low Risk (Verified)</span>
        </span>
      `;
    case 'MEDIUM':
      return `
        <span class="badge-risk badge-risk-medium" role="status">
          <svg width="14" height="14" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2.5" aria-hidden="true">
            <path d="M12 22s8-4 8-10V5l-8-3-8 3v7c0 6 8 10 8 10z"/>
            <line x1="12" y1="8" x2="12" y2="12"/>
            <line x1="12" y1="16" x2="12.01" y2="16"/>
          </svg>
          <span>Medium Risk (Extra Check)</span>
        </span>
      `;
    case 'HIGH':
    default:
      return `
        <span class="badge-risk badge-risk-high" role="status">
          <svg width="14" height="14" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2.5" aria-hidden="true">
            <polygon points="7.86 2 16.14 2 22 7.86 22 16.14 16.14 22 7.86 22 2 16.14 2 7.86 7.86 2"></polygon>
            <line x1="12" y1="8" x2="12" y2="12"></line>
            <line x1="12" y1="16" x2="12.01" y2="16"></line>
          </svg>
          <span>High Risk (Approval Required)</span>
        </span>
      `;
  }
}

/**
 * Step Progress Indicator
 * @param {number} current 
 * @param {number} total 
 * @param {string} title 
 */
export function renderStepIndicator(current, total, title) {
  const percent = Math.round((current / total) * 100);
  return `
    <div class="step-indicator" role="region" aria-label="Sign in progress">
      <div class="d-flex justify-content-between align-items-center mb-1">
        <span class="step-text">${messages.steps.stepOf(current, total)}: ${title}</span>
        <span class="small text-muted fw-bold">${percent}%</span>
      </div>
      <div class="step-progress-bar" role="progressbar" aria-valuenow="${current}" aria-valuemin="1" aria-valuemax="${total}" aria-label="Step progress">
        <div class="step-progress-fill" style="width: ${percent}%;"></div>
      </div>
    </div>
  `;
}

/**
 * "Why am I being asked this?" Accessible Disclosure Component
 * @param {Object} info { title, message, nextStep }
 */
export function renderWhyDisclosure(info = {}) {
  const title = info.title || messages.login.whyDisclosureTitle;
  const message = info.message || messages.login.whyDefaultExplain;
  const nextStep = info.nextStep || 'Follow the step shown on screen to continue.';
  const disclosureId = 'disclosure-' + Math.random().toString(36).substring(2, 7);

  return `
    <div class="disclosure-container">
      <button type="button" class="disclosure-btn" id="btn-${disclosureId}" aria-expanded="false" aria-controls="${disclosureId}">
        <span>
          <svg width="16" height="16" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2" class="me-1" aria-hidden="true">
            <circle cx="12" cy="12" r="10"></circle>
            <path d="M9.09 9a3 3 0 0 1 5.83 1c0 2-3 3-3 3"></path>
            <line x1="12" y1="17" x2="12.01" y2="17"></line>
          </svg>
          <strong>${messages.login.whyDisclosureTitle}</strong>
        </span>
        <svg width="16" height="16" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2.5" class="disclosure-icon" aria-hidden="true">
          <polyline points="6 9 12 15 18 9"></polyline>
        </svg>
      </button>
      <div id="${disclosureId}" class="disclosure-content d-none" role="region" aria-labelledby="btn-${disclosureId}">
        <p class="fw-bold mb-1">${escapeHtml(title)}</p>
        <p class="mb-2 text-muted">${escapeHtml(message)}</p>
        <div class="small p-2 bg-muted rounded border">
          <strong>Next step:</strong> <span>${escapeHtml(nextStep)}</span>
        </div>
      </div>
    </div>
  `;
}

export function initDisclosures(container = document) {
  container.querySelectorAll('.disclosure-btn').forEach(btn => {
    btn.onclick = () => {
      const isExpanded = btn.getAttribute('aria-expanded') === 'true';
      const targetId = btn.getAttribute('aria-controls');
      const target = document.getElementById(targetId);
      if (!target) return;

      btn.setAttribute('aria-expanded', String(!isExpanded));
      target.classList.toggle('d-none', isExpanded);
    };
  });
}

/**
 * Demo Simulation Offcanvas Drawer
 */
function mountDemoPanel() {
  if (document.getElementById('ae-demo-drawer')) return;

  const floatingBtn = document.createElement('button');
  floatingBtn.type = 'button';
  floatingBtn.id = 'ae-demo-btn';
  floatingBtn.className = 'demo-floating-btn';
  floatingBtn.setAttribute('aria-haspopup', 'dialog');
  floatingBtn.setAttribute('aria-expanded', 'false');
  floatingBtn.setAttribute('aria-controls', 'ae-demo-drawer');
  floatingBtn.innerHTML = `
    <svg width="18" height="18" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2.5" aria-hidden="true">
      <path d="M12 2v4M12 18v4M4.93 4.93l2.83 2.83M16.24 16.24l2.83 2.83M2 12h4M18 12h4M4.93 19.07l2.83-2.83M16.24 7.76l2.83-2.83"/>
    </svg>
    <span>Demo Simulation</span>
  `;
  document.body.appendChild(floatingBtn);

  const drawer = document.createElement('div');
  drawer.id = 'ae-demo-drawer';
  drawer.className = 'offcanvas offcanvas-end';
  drawer.setAttribute('tabindex', '-1');
  drawer.setAttribute('aria-labelledby', 'ae-demo-drawer-label');
  drawer.style.width = '380px';
  drawer.innerHTML = `
    <div class="offcanvas-header border-bottom">
      <h2 class="offcanvas-title h5 fw-bold" id="ae-demo-drawer-label">${messages.demo.panelTitle}</h2>
      <button type="button" class="btn-close text-reset" id="ae-demo-close" aria-label="Close demo panel"></button>
    </div>
    <div class="offcanvas-body">
      <div class="alert alert-warning py-2 small mb-3">
        <strong>Notice:</strong> ${messages.demo.disclaimer}
      </div>

      <h3 class="h6 fw-bold mb-2">Simulate Security Signals</h3>
      <div class="form-check form-switch mb-2">
        <input class="form-check-input" type="checkbox" id="sim-new-device">
        <label class="form-check-label" for="sim-new-device">New device (Triggers Medium MFA)</label>
      </div>
      <div class="form-check form-switch mb-2">
        <input class="form-check-input" type="checkbox" id="sim-new-network">
        <label class="form-check-label" for="sim-new-network">New network / IP</label>
      </div>
      <div class="form-check form-switch mb-3">
        <input class="form-check-input" type="checkbox" id="sim-unusual-hour">
        <label class="form-check-label" for="sim-unusual-hour">Unusual hour (Triggers High MFA)</label>
      </div>

      <button type="button" class="btn btn-outline-danger w-100 mb-3" id="sim-force-failures">
        Force 3 failed attempts (Triggers Rate Delay)
      </button>

      <hr class="my-3">

      <h3 class="h6 fw-bold mb-2">${messages.demo.presetHeader}</h3>
      <div class="d-grid gap-2 mb-3">
        <button type="button" class="btn btn-sm btn-outline-primary text-start" id="preset-trusted">
          ${messages.demo.presetTrusted}
        </button>
        <button type="button" class="btn btn-sm btn-outline-primary text-start" id="preset-new-device">
          ${messages.demo.presetNewDevice}
        </button>
        <button type="button" class="btn btn-sm btn-outline-primary text-start" id="preset-suspicious">
          ${messages.demo.presetSuspicious}
        </button>
        <button type="button" class="btn btn-sm btn-outline-primary text-start" id="preset-wrong">
          ${messages.demo.presetWrongPassword}
        </button>
      </div>

      <hr class="my-3">

      <a href="dev-outbox.html" class="btn btn-primary w-100 d-flex align-items-center justify-content-center gap-2">
        <svg width="18" height="18" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2" aria-hidden="true">
          <path d="M4 4h16c1.1 0 2 .9 2 2v12c0 1.1-.9 2-2 2H4c-1.1 0-2-.9-2-2V6c0-1.1.9-2 2-2z"></path>
          <polyline points="22,6 12,13 2,6"></polyline>
        </svg>
        <span>${messages.demo.openOutbox}</span>
      </a>
    </div>
  `;
  document.body.appendChild(drawer);

  // Wire up toggles and open/close
  floatingBtn.onclick = () => {
    drawer.classList.add('show');
    drawer.style.visibility = 'visible';
    floatingBtn.setAttribute('aria-expanded', 'true');
    drawer.focus();
    loadDemoContextState();
  };

  const closeBtn = drawer.querySelector('#ae-demo-close');
  closeBtn.onclick = () => {
    drawer.classList.remove('show');
    drawer.style.visibility = 'hidden';
    floatingBtn.setAttribute('aria-expanded', 'false');
    floatingBtn.focus();
  };

  async function loadDemoContextState() {
    try {
      const res = await api.getDemoContext();
      if (res.ok && res.data) {
        drawer.querySelector('#sim-new-device').checked = !!res.data.newDevice;
        drawer.querySelector('#sim-new-network').checked = !!res.data.newNetwork;
        drawer.querySelector('#sim-unusual-hour').checked = !!res.data.unusualHour;
      }
    } catch (_) {}
  }

  async function updateDemoContext() {
    await api.setDemoContext({
      newDevice: drawer.querySelector('#sim-new-device').checked,
      newNetwork: drawer.querySelector('#sim-new-network').checked,
      unusualHour: drawer.querySelector('#sim-unusual-hour').checked,
    });
  }

  drawer.querySelectorAll('.form-check-input').forEach(chk => {
    chk.onchange = updateDemoContext;
  });

  drawer.querySelector('#sim-force-failures').onclick = async () => {
    await api.setDemoContext({ forceFailures: true });
    alert('Forced 3 failed attempts simulated. The next sign-in attempt will trigger a 30-second rate-limiting delay countdown.');
  };

  drawer.querySelector('#preset-trusted').onclick = () => {
    sessionStorage.removeItem('authease.mock.state');
    window.location.href = 'login.html?scenario=trusted&mock=1';
  };
  drawer.querySelector('#preset-new-device').onclick = () => {
    window.location.href = 'login.html?scenario=new_device&mock=1';
  };
  drawer.querySelector('#preset-suspicious').onclick = () => {
    window.location.href = 'login.html?scenario=suspicious&mock=1';
  };
  drawer.querySelector('#preset-wrong').onclick = () => {
    window.location.href = 'login.html?scenario=wrong&mock=1';
  };
}

function escapeHtml(str) {
  if (!str) return '';
  return String(str)
    .replace(/&/g, '&amp;')
    .replace(/</g, '&lt;')
    .replace(/>/g, '&gt;')
    .replace(/"/g, '&quot;')
    .replace(/'/g, '&#39;');
}
