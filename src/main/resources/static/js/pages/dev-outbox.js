/**
 * AuthEase - Simulated Dev Outbox Controller
 * Displays all outbound emails from MongoDB / mock state in real time.
 */

import { mountShell } from '../ui.js';
import { api } from '../api.js';
import { setStepHeading, announce } from '../a11y.js';

let refreshTimer = null;

document.addEventListener('DOMContentLoaded', () => {
  mountShell({ activeNav: 'demo' });
  initOutboxPage();
});

function initOutboxPage() {
  setStepHeading('Simulated Dev Outbox', '#outbox-heading');

  const refreshBtn = document.getElementById('btn-refresh-outbox');
  if (refreshBtn) {
    refreshBtn.addEventListener('click', () => {
      loadMessages();
      announce('Outbox refreshed.');
    });
  }

  loadMessages();

  // Auto-refresh every 5 seconds so new emails pop up automatically
  refreshTimer = setInterval(loadMessages, 5000);
}

window.addEventListener('beforeunload', () => {
  if (refreshTimer) clearInterval(refreshTimer);
});

async function loadMessages() {
  const loading = document.getElementById('outbox-loading');
  const empty = document.getElementById('outbox-empty');
  const list = document.getElementById('outbox-list');

  try {
    const res = await api.getDevOutbox();
    loading.classList.add('d-none');

    const messages = res.ok && Array.isArray(res.data) ? res.data : [];

    if (messages.length === 0) {
      empty.classList.remove('d-none');
      list.innerHTML = '';
      return;
    }

    empty.classList.add('d-none');
    renderMessages(messages, list);
  } catch (e) {
    loading.classList.add('d-none');
  }
}

function renderMessages(messages, container) {
  container.innerHTML = '';

  messages.forEach((msg, idx) => {
    const card = document.createElement('div');
    card.className = 'card shadow-sm border p-3';

    const dateStr = msg.ts ? new Date(msg.ts).toLocaleTimeString([], { hour: '2-digit', minute: '2-digit', second: '2-digit' }) : 'Just now';
    const to = msg.to || 'Unknown recipient';
    const subject = msg.subject || 'No subject';
    const body = msg.body || '';

    // Extract links in the email body
    const linkMatch = body.match(/https?:\/\/[^\s]+/i) || body.match(/\/([a-zA-Z0-9_-]+\.html\?[^\s]+)/i);
    let linkUrl = linkMatch ? linkMatch[0] : null;
    if (linkUrl) {
      try {
        const parsed = new URL(linkUrl, window.location.origin);
        linkUrl = window.location.origin + parsed.pathname + parsed.search;
      } catch (_) {}
    }

    // Extract 6-digit numeric codes
    const codeMatch = body.match(/\b\d{6}\b/);
    const code = codeMatch ? codeMatch[0] : null;

    card.innerHTML = `
      <div class="d-flex flex-wrap justify-content-between align-items-start gap-2 border-bottom pb-2 mb-2">
        <div>
          <span class="badge bg-primary me-2">New</span>
          <strong class="text-body">${escapeHtml(subject)}</strong>
          <div class="small text-muted mt-1">To: <code>${escapeHtml(to)}</code></div>
        </div>
        <div class="small text-muted">${dateStr}</div>
      </div>

      <pre class="bg-light p-3 rounded border small mb-3 text-secondary" style="white-space: pre-wrap; font-family: monospace;">${escapeHtml(body)}</pre>

      <div class="d-flex flex-wrap align-items-center gap-2">
        ${linkUrl ? `
          <a href="${escapeHtml(linkUrl)}" target="_blank" class="btn btn-sm btn-success fw-bold d-inline-flex align-items-center gap-1">
            <svg width="16" height="16" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2"><path d="M18 13v6a2 2 0 0 1-2 2H5a2 2 0 0 1-2-2V8a2 2 0 0 1 2-2h6"></path><polyline points="15 3 21 3 21 9"></polyline><line x1="10" y1="14" x2="21" y2="3"></line></svg>
            <span>Open Link in Email</span>
          </a>
        ` : ''}

        ${code ? `
          <button type="button" class="btn btn-sm btn-outline-primary fw-semibold btn-copy-code" data-code="${escapeHtml(code)}">
            Copy Code (${escapeHtml(code)})
          </button>
        ` : ''}
      </div>
    `;

    const copyBtn = card.querySelector('.btn-copy-code');
    if (copyBtn) {
      copyBtn.addEventListener('click', () => {
        navigator.clipboard.writeText(code).then(() => {
          copyBtn.textContent = 'Copied!';
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
