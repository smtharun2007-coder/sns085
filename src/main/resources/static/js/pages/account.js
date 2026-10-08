/**
 * AuthEase - Account Security Center Page Controller
 * Gracefully handles unauthenticated visits and renders security settings.
 */

import { mountShell } from '../ui.js';
import { api } from '../api.js';
import { setStepHeading, announce } from '../a11y.js';

document.addEventListener('DOMContentLoaded', () => {
  mountShell({ activeNav: 'account' });
  initAccountPage();
});

async function initAccountPage() {
  const unauthView = document.getElementById('unauth-view');
  const authView = document.getElementById('auth-view');

  try {
    const meRes = await api.me();
    if (!meRes.ok || !meRes.data || !meRes.data.authenticated) {
      // User is not signed in: show accessible "Sign In Required" view cleanly
      unauthView.classList.remove('d-none');
      authView.classList.add('d-none');
      setStepHeading('Sign in required', '#unauth-view h1');
      return;
    }

    // User is signed in!
    unauthView.classList.add('d-none');
    authView.classList.remove('d-none');
    setStepHeading('Account Security Center', '#account-heading');

    const user = meRes.data;
    const identityDisplay = document.getElementById('user-identity-display');
    if (identityDisplay) {
      identityDisplay.textContent = `Signed in as ${user.email || 'User'} (${user.role || 'USER'})`;
    }

    setupLogout();
    await loadSecurityDetails();
  } catch (err) {
    unauthView.classList.remove('d-none');
    authView.classList.add('d-none');
  }
}

function setupLogout() {
  const logoutBtn = document.getElementById('btn-logout');
  if (logoutBtn) {
    logoutBtn.addEventListener('click', async () => {
      logoutBtn.disabled = true;
      logoutBtn.textContent = 'Signing out...';
      await api.logout();
      window.location.href = 'login.html';
    });
  }
}

async function loadSecurityDetails() {
  const secRes = await api.getSecurityCenter();
  if (!secRes.ok || !secRes.data) return;

  const data = secRes.data;

  // 1. MFA Status
  const mfaDesc = document.getElementById('mfa-status-desc');
  const mfaSlot = document.getElementById('mfa-action-slot');
  if (data.mfaEnabled) {
    if (mfaDesc) {
      mfaDesc.innerHTML = `<span class="badge bg-success me-1">Active</span> Authenticator App 2FA is active on your account.`;
    }
    if (mfaSlot) {
      mfaSlot.innerHTML = `<a href="mfa-setup.html" class="btn btn-sm btn-ae-outline">Manage 2FA</a>`;
    }
  } else {
    if (mfaDesc) {
      mfaDesc.innerHTML = `<span class="badge bg-secondary me-1">Off</span> Two-factor authentication is not yet enabled.`;
    }
    if (mfaSlot) {
      mfaSlot.innerHTML = `<a href="mfa-setup.html" class="btn btn-sm btn-ae-primary">Enable 2FA</a>`;
    }
  }

  // 2. Devices
  const devicesList = document.getElementById('devices-list');
  if (devicesList) {
    devicesList.innerHTML = '';
    const devices = data.devices || [];
    if (devices.length === 0) {
      devicesList.innerHTML = '<div class="text-muted small">No remembered devices recorded.</div>';
    } else {
      devices.forEach(dev => {
        const item = document.createElement('div');
        item.className = 'd-flex justify-content-between align-items-center p-2 rounded border bg-light small';
        item.innerHTML = `
          <div>
            <strong>${escapeHtml(dev.label || 'Web Browser')}</strong>
            ${dev.isCurrent ? '<span class="badge bg-info text-dark ms-2">Current session</span>' : ''}
            <div class="text-muted small">Added: ${dev.createdAt ? new Date(dev.createdAt).toLocaleDateString() : 'Recent'}</div>
          </div>
          <button type="button" class="btn btn-sm btn-outline-danger btn-revoke" data-id="${escapeHtml(dev.id)}">
            Revoke
          </button>
        `;

        const revokeBtn = item.querySelector('.btn-revoke');
        revokeBtn.addEventListener('click', async () => {
          revokeBtn.disabled = true;
          revokeBtn.textContent = 'Revoking...';
          await api.revokeDevice(dev.id);
          item.remove();
          announce('Device removed.');
        });

        devicesList.appendChild(item);
      });
    }
  }

  // 3. Events Log
  const eventsList = document.getElementById('events-list');
  if (eventsList) {
    eventsList.innerHTML = '';
    const events = data.recentEvents || [];
    if (events.length === 0) {
      eventsList.innerHTML = '<div class="text-muted small">No recent security events recorded.</div>';
    } else {
      events.slice(0, 5).forEach(ev => {
        const item = document.createElement('div');
        item.className = 'p-2 rounded border small d-flex justify-content-between align-items-center';
        const levelBadge = ev.level === 'HIGH' ? 'bg-danger' : ev.level === 'MEDIUM' ? 'bg-warning text-dark' : 'bg-success';
        item.innerHTML = `
          <div>
            <span class="badge ${levelBadge} me-2">${escapeHtml(ev.level || 'INFO')}</span>
            <strong>${escapeHtml(ev.type || 'EVENT')}</strong>
            <span class="text-muted ms-2">${escapeHtml(ev.reasonCode || '')}</span>
          </div>
          <div class="text-muted small">${ev.ts ? new Date(ev.ts).toLocaleTimeString() : ''}</div>
        `;
        eventsList.appendChild(item);
      });
    }
  }
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
