/**
 * AuthEase - Admin Dashboard Controller
 * Displays security telemetry, risk breakdown, and audit event logs.
 */

import { mountShell } from '../ui.js';
import { api } from '../api.js';
import { setStepHeading, announce } from '../a11y.js';

let currentPage = 1;
const pageSize = 10;
let currentRange = '24h';
let currentTypeFilter = '';
let currentLevelFilter = '';

document.addEventListener('DOMContentLoaded', () => {
  mountShell({ activeNav: 'admin' });
  initAdminDashboard();
});

async function initAdminDashboard() {
  setStepHeading('Admin Security Dashboard', '#admin-heading');

  const unauthView = document.getElementById('admin-unauth-view');
  const authView = document.getElementById('admin-auth-view');

  try {
    const meRes = await api.getMe();
    const isAdmin = meRes.ok && meRes.data?.authenticated && meRes.data?.user?.roles?.includes('ADMIN');

    if (!isAdmin) {
      if (unauthView) unauthView.classList.remove('d-none');
      if (authView) authView.classList.add('d-none');
      return;
    }

    if (unauthView) unauthView.classList.add('d-none');
    if (authView) authView.classList.remove('d-none');

    setupEventHandlers();
    loadDashboardData();
  } catch (e) {
    if (unauthView) unauthView.classList.remove('d-none');
    if (authView) authView.classList.add('d-none');
  }
}

function setupEventHandlers() {
  const rangeSelect = document.getElementById('select-metrics-range');
  if (rangeSelect) {
    rangeSelect.addEventListener('change', (e) => {
      currentRange = e.target.value;
      loadMetrics();
    });
  }

  const refreshBtn = document.getElementById('btn-refresh-admin');
  if (refreshBtn) {
    refreshBtn.addEventListener('click', () => {
      loadDashboardData();
      announce('Admin telemetry refreshed.');
    });
  }

  const typeFilter = document.getElementById('filter-event-type');
  if (typeFilter) {
    typeFilter.addEventListener('change', (e) => {
      currentTypeFilter = e.target.value;
      currentPage = 1;
      loadEvents();
    });
  }

  const levelFilter = document.getElementById('filter-risk-level');
  if (levelFilter) {
    levelFilter.addEventListener('change', (e) => {
      currentLevelFilter = e.target.value;
      currentPage = 1;
      loadEvents();
    });
  }

  const prevBtn = document.getElementById('btn-page-prev');
  if (prevBtn) {
    prevBtn.addEventListener('click', () => {
      if (currentPage > 1) {
        currentPage--;
        loadEvents();
      }
    });
  }

  const nextBtn = document.getElementById('btn-page-next');
  if (nextBtn) {
    nextBtn.addEventListener('click', () => {
      currentPage++;
      loadEvents();
    });
  }
}

async function loadDashboardData() {
  await Promise.all([loadMetrics(), loadEvents()]);
}

async function loadMetrics() {
  try {
    const res = await api.getAdminMetrics(currentRange);
    if (!res.ok || !res.data) return;

    const data = res.data;

    // KPI Cards
    const kpiFirstTry = document.getElementById('kpi-first-try-rate');
    if (kpiFirstTry) kpiFirstTry.textContent = `${(data.firstTrySuccessRate ?? 0).toFixed(1)}%`;

    const kpiMfaDrop = document.getElementById('kpi-mfa-drop-rate');
    if (kpiMfaDrop) kpiMfaDrop.textContent = `${(data.mfaDropOffRate ?? 0).toFixed(1)}%`;

    const kpiRecovery = document.getElementById('kpi-recovery-rate');
    if (kpiRecovery) kpiRecovery.textContent = `${(data.recoveryCompletionRate ?? 0).toFixed(1)}%`;

    const kpiDuration = document.getElementById('kpi-recovery-duration');
    if (kpiDuration) kpiDuration.textContent = `${data.avgRecoverySeconds ?? 0}s`;

    // Risk Tiers Distribution
    const levels = data.loginsByLevel || { LOW: 0, MEDIUM: 0, HIGH: 0 };
    const lowCount = levels.LOW || 0;
    const medCount = levels.MEDIUM || 0;
    const highCount = levels.HIGH || 0;
    const totalLogins = Math.max(1, lowCount + medCount + highCount);

    const elLow = document.getElementById('risk-count-low');
    const barLow = document.getElementById('risk-bar-low');
    if (elLow) elLow.textContent = `${lowCount.toLocaleString()} (${Math.round((lowCount / totalLogins) * 100)}%)`;
    if (barLow) barLow.style.width = `${(lowCount / totalLogins) * 100}%`;

    const elMed = document.getElementById('risk-count-med');
    const barMed = document.getElementById('risk-bar-med');
    if (elMed) elMed.textContent = `${medCount.toLocaleString()} (${Math.round((medCount / totalLogins) * 100)}%)`;
    if (barMed) barMed.style.width = `${(medCount / totalLogins) * 100}%`;

    const elHigh = document.getElementById('risk-count-high');
    const barHigh = document.getElementById('risk-bar-high');
    if (elHigh) elHigh.textContent = `${highCount.toLocaleString()} (${Math.round((highCount / totalLogins) * 100)}%)`;
    if (barHigh) barHigh.style.width = `${(highCount / totalLogins) * 100}%`;

    // Hourly Failures Chart
    renderFailuresChart(data.failuresPerHour || []);
  } catch (e) {
    console.error('Failed to load admin metrics:', e);
  }
}

function renderFailuresChart(failures) {
  const container = document.getElementById('failures-chart-container');
  if (!container) return;

  if (failures.length === 0) {
    container.innerHTML = '<div class="text-muted small py-3 text-center w-100">No failures recorded in this period.</div>';
    return;
  }

  const maxCount = Math.max(1, ...failures.map(f => f.count || 0));

  container.innerHTML = failures.map(f => {
    const count = f.count || 0;
    const heightPercent = Math.max(8, Math.round((count / maxCount) * 100));
    const label = f.hour || '';

    return `
      <div class="d-flex flex-column align-items-center flex-grow-1" style="height: 100%;">
        <span class="small fw-bold text-muted mb-1" style="font-size: 0.7rem;">${count}</span>
        <div class="w-100 bg-light rounded-top d-flex align-items-end" style="height: 80px;">
          <div class="w-100 bg-danger rounded-top" style="height: ${heightPercent}%; transition: height 0.3s ease;" title="${count} failures at ${escapeHtml(label)}"></div>
        </div>
        <span class="text-muted mt-1" style="font-size: 0.65rem;">${escapeHtml(label)}</span>
      </div>
    `;
  }).join('');
}

async function loadEvents() {
  const tbody = document.getElementById('events-tbody');
  const prevBtn = document.getElementById('btn-page-prev');
  const nextBtn = document.getElementById('btn-page-next');
  const pageInfo = document.getElementById('pagination-info');

  if (!tbody) return;

  try {
    const res = await api.getAdminEvents(currentTypeFilter, currentLevelFilter, currentPage);
    if (!res.ok || !res.data) {
      tbody.innerHTML = '<tr><td colspan="6" class="text-center py-4 text-muted">No audit events available.</td></tr>';
      return;
    }

    const events = res.data.items || [];
    const total = res.data.total || 0;
    const totalPages = Math.max(1, Math.ceil(total / pageSize));

    if (pageInfo) {
      pageInfo.textContent = `Showing page ${currentPage} of ${totalPages} (${total.toLocaleString()} total events)`;
    }

    if (prevBtn) prevBtn.disabled = currentPage <= 1;
    if (nextBtn) nextBtn.disabled = currentPage >= totalPages;

    if (events.length === 0) {
      tbody.innerHTML = '<tr><td colspan="6" class="text-center py-4 text-muted">No matching audit events found.</td></tr>';
      return;
    }

    tbody.innerHTML = events.map(ev => {
      const timeStr = ev.ts ? new Date(ev.ts).toLocaleTimeString([], { hour: '2-digit', minute: '2-digit', second: '2-digit' }) : (ev.timestamp || 'Just now');
      const level = ev.level || 'LOW';
      let badgeClass = 'bg-success';
      if (level === 'MEDIUM') badgeClass = 'bg-warning text-dark';
      if (level === 'HIGH') badgeClass = 'bg-danger text-white';

      const type = ev.type || 'UNKNOWN';
      const user = ev.userId || ev.identifierHash || 'Anonymous';
      const signals = (ev.signals && ev.signals.length > 0) ? ev.signals.join(', ') : (ev.reasonCode || 'Normal');
      const ip = ev.ipPrefixHash || ev.ip || 'Local';

      return `
        <tr>
          <td class="font-monospace text-muted">${escapeHtml(timeStr)}</td>
          <td><code class="fw-semibold">${escapeHtml(type)}</code></td>
          <td><span class="badge ${badgeClass}">${escapeHtml(level)}</span></td>
          <td class="text-truncate" style="max-width: 140px;">${escapeHtml(user)}</td>
          <td class="small text-muted">${escapeHtml(signals)}</td>
          <td class="font-monospace small text-muted">${escapeHtml(ip)}</td>
        </tr>
      `;
    }).join('');
  } catch (e) {
    tbody.innerHTML = '<tr><td colspan="6" class="text-center py-4 text-danger">Failed to load events from server.</td></tr>';
  }
}

function escapeHtml(text) {
  if (!text) return '';
  return String(text)
    .replace(/&/g, '&amp;')
    .replace(/</g, '&lt;')
    .replace(/>/g, '&gt;')
    .replace(/"/g, '&quot;');
}
