/**
 * AuthEase - Simulated Dev Outbox Controller
 * Displays all outbound emails from MongoDB / mock state in real time.
 */

import { mountShell } from '../ui.js';
import { api } from '../api.js';
import { setStepHeading, announce } from '../a11y.js';

let refreshTimer = null;
let allMessages = [];
let currentFilter = '';

document.addEventListener('DOMContentLoaded', () => {
  mountShell({ activeNav: 'demo' });
  initOutboxPage();
});

function initOutboxPage() {
  setStepHeading('Simulated Dev Outbox', '#outbox-heading');

  const urlParams = new URLSearchParams(window.location.search);
  const initialEmail = urlParams.get('email') || urlParams.get('to') || '';

  const filterInput = document.getElementById('outbox-filter-input');
  if (filterInput && initialEmail) {
    filterInput.value = initialEmail;
    currentFilter = initialEmail.toLowerCase().trim();
  }

  if (filterInput) {
    filterInput.addEventListener('input', () => {
      currentFilter = filterInput.value.toLowerCase().trim();
      applyFilterAndRender();
    });
  }

  const clearBtn = document.getElementById('btn-clear-filter');
  if (clearBtn) {
    clearBtn.addEventListener('click', () => {
      if (filterInput) filterInput.value = '';
      currentFilter = '';
      applyFilterAndRender();
    });
  }

  const resetEmptyBtn = document.getElementById('btn-reset-empty-filter');
  if (resetEmptyBtn) {
    resetEmptyBtn.addEventListener('click', () => {
      if (filterInput) filterInput.value = '';
      currentFilter = '';
      applyFilterAndRender();
    });
  }

  const refreshBtn = document.getElementById('btn-refresh-outbox');
  if (refreshBtn) {
    refreshBtn.addEventListener('click', () => {
      loadMessages(true);
      announce('Outbox refreshed.');
    });
  }

  loadMessages(false);

  // Auto-refresh every 5 seconds so new emails pop up automatically
  refreshTimer = setInterval(() => loadMessages(false), 5000);
}

window.addEventListener('beforeunload', () => {
  if (refreshTimer) clearInterval(refreshTimer);
});

async function loadMessages(isManual = false) {
  const loading = document.getElementById('outbox-loading');
  const errorSlot = document.getElementById('outbox-error-slot');

  try {
    const res = await api.getDevOutbox();
    if (loading) loading.classList.add('d-none');
    if (errorSlot) errorSlot.classList.add('d-none');

    if (res.ok && Array.isArray(res.data)) {
      allMessages = res.data;
      applyFilterAndRender();
    } else {
      if (isManual && errorSlot) {
        errorSlot.textContent = 'Could not load messages from outbox. Please check server status.';
        errorSlot.classList.remove('d-none');
      }
    }
  } catch (e) {
    if (loading) loading.classList.add('d-none');
    if (errorSlot && isManual) {
      errorSlot.textContent = 'Failed to fetch outbox emails: ' + (e.message || 'Network error');
      errorSlot.classList.remove('d-none');
    }
  }
}

function applyFilterAndRender() {
  const empty = document.getElementById('outbox-empty');
  const list = document.getElementById('outbox-list');
  const badge = document.getElementById('outbox-count-badge');

  if (!list) return;

  let filtered = allMessages;
  if (currentFilter) {
    filtered = allMessages.filter(msg => {
      const to = (msg.to || '').toLowerCase();
      const subject = (msg.subject || '').toLowerCase();
      const body = (msg.body || '').toLowerCase();
      return to.includes(currentFilter) || subject.includes(currentFilter) || body.includes(currentFilter);
    });
  }

  if (badge) {
    if (currentFilter) {
      badge.textContent = `Showing ${filtered.length} of ${allMessages.length} emails (Filtered)`;
      badge.className = 'badge bg-primary py-2 px-3 fw-semibold';
    } else {
      badge.textContent = `${allMessages.length} total email${allMessages.length === 1 ? '' : 's'} recorded`;
      badge.className = 'badge bg-secondary py-2 px-3 fw-normal';
    }
  }

  if (filtered.length === 0) {
    if (empty) empty.classList.remove('d-none');
    list.innerHTML = '';
    return;
  }

  if (empty) empty.classList.add('d-none');
  renderMessages(filtered, list);
}

function renderMessages(messages, container) {
  container.innerHTML = '';

  messages.forEach((msg) => {
    const card = document.createElement('div');
    card.className = 'card shadow-sm border p-3';

    let dateStr = 'Just now';
    if (msg.ts) {
      try {
        const d = new Date(msg.ts);
        dateStr = d.toLocaleTimeString([], { hour: '2-digit', minute: '2-digit', second: '2-digit' }) + ' (' + d.toLocaleDateString() + ')';
      } catch (_) {}
    } else if (msg.timestamp) {
      dateStr = msg.timestamp;
    }

    const to = msg.to || 'Unknown recipient';
    const subject = msg.subject || 'No subject';
    const body = msg.body || '';

    // Extract links in the email body or actionUrl
    let linkUrl = msg.actionUrl || null;
    if (!linkUrl) {
      const linkMatch = body.match(/https?:\/\/[^\s]+/i) || body.match(/\/([a-zA-Z0-9_-]+\.html\?[^\s]+)/i) || body.match(/([a-zA-Z0-9_-]+\.html\?[^\s]+)/i);
      if (linkMatch) {
        linkUrl = linkMatch[0];
      }
    }

    if (linkUrl) {
      try {
        const parsed = new URL(linkUrl, window.location.origin);
        linkUrl = window.location.origin + (parsed.pathname.startsWith('/') ? parsed.pathname : '/' + parsed.pathname) + parsed.search;
      } catch (_) {
        if (!linkUrl.startsWith('http')) {
          linkUrl = window.location.origin + (linkUrl.startsWith('/') ? '' : '/') + linkUrl;
        }
      }
    }

    // Extract 6-digit numeric codes
    const codeMatch = body.match(/\b\d{6}\b/);
    const code = codeMatch ? codeMatch[0] : null;

    let actionLabel = 'Open Link in Email';
    let btnClass = 'btn-primary';
    if (subject.toLowerCase().includes('verify') || (linkUrl && linkUrl.includes('verify-email'))) {
      actionLabel = '👉 Verify & Activate Account Now';
      btnClass = 'btn-success';
    } else if (subject.toLowerCase().includes('authorization') || subject.toLowerCase().includes('approval') || (linkUrl && linkUrl.includes('approve'))) {
      actionLabel = '🛡️ Review & Approve Sign-in';
      btnClass = 'btn-warning text-dark';
    } else if (subject.toLowerCase().includes('recovery') || (linkUrl && linkUrl.includes('recover'))) {
      actionLabel = '🔑 Reset Account Password';
      btnClass = 'btn-danger';
    }

    card.innerHTML = `
      <div class="d-flex flex-wrap justify-content-between align-items-start gap-2 border-bottom pb-2 mb-2">
        <div>
          <span class="badge bg-primary me-2">Outbox Message</span>
          <strong class="text-body">${escapeHtml(subject)}</strong>
          <div class="small text-muted mt-1">To: <code class="text-primary fw-bold">${escapeHtml(to)}</code></div>
        </div>
        <div class="small text-muted font-monospace">${escapeHtml(dateStr)}</div>
      </div>

      <pre class="bg-light p-3 rounded border small mb-3 text-secondary" style="white-space: pre-wrap; font-family: monospace;">${escapeHtml(body)}</pre>

      <div class="d-flex flex-wrap align-items-center gap-2">
        ${linkUrl ? `
          <a href="${escapeHtml(linkUrl)}" target="_blank" rel="noopener noreferrer" class="btn btn-sm ${btnClass} fw-bold d-inline-flex align-items-center gap-1 shadow-sm px-3 py-2">
            <svg width="18" height="18" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2" aria-hidden="true"><path d="M18 13v6a2 2 0 0 1-2 2H5a2 2 0 0 1-2-2V8a2 2 0 0 1 2-2h6"></path><polyline points="15 3 21 3 21 9"></polyline><line x1="10" y1="14" x2="21" y2="3"></line></svg>
            <span>${actionLabel}</span>
          </a>
        ` : ''}

        ${code ? `
          <button type="button" class="btn btn-sm btn-outline-primary fw-semibold btn-copy-code px-3 py-2" data-code="${escapeHtml(code)}">
            Copy Code (${escapeHtml(code)})
          </button>
        ` : ''}
      </div>
    `;

    const copyBtn = card.querySelector('.btn-copy-code');
    if (copyBtn && code) {
      copyBtn.addEventListener('click', () => {
        navigator.clipboard.writeText(code).then(() => {
          copyBtn.textContent = 'Copied!';
          announce('Code copied to clipboard: ' + code);
          setTimeout(() => {
            copyBtn.textContent = `Copy Code (${code})`;
          }, 2000);
        });
      });
    }

    container.appendChild(card);
  });
}

function escapeHtml(text) {
  if (!text) return '';
  return String(text)
    .replace(/&/g, '&amp;')
    .replace(/</g, '&lt;')
    .replace(/>/g, '&gt;')
    .replace(/"/g, '&quot;')
    .replace(/'/g, '&#039;');
}
