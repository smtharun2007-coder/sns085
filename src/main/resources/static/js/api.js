/**
 * AuthEase - Core API Client
 * 
 * Rules:
 * - Credentials: "same-origin"
 * - Automatically sends X-XSRF-TOKEN cookie value in X-XSRF-TOKEN header on POST/DELETE
 * - Auto-retries once on 403 CSRF after refreshing with GET /api/auth/me
 * - Seamlessly delegates to api.mock.js if ?mock=1 or authease.mock=1
 * - Zero secrets stored in storage
 */

import { mockFetch } from './api.mock.js';
import { messages } from './messages.en.js';

const MOCK_STORAGE_KEY = 'authease.mock';

export function isMockMode() {
  const urlParams = new URLSearchParams(window.location.search);
  if (urlParams.get('mock') === '1') {
    localStorage.setItem(MOCK_STORAGE_KEY, '1');
    return true;
  }
  if (urlParams.get('mock') === '0') {
    localStorage.removeItem(MOCK_STORAGE_KEY);
    return false;
  }
  return localStorage.getItem(MOCK_STORAGE_KEY) === '1';
}

export function setMockMode(enabled) {
  if (enabled) {
    localStorage.setItem(MOCK_STORAGE_KEY, '1');
  } else {
    localStorage.removeItem(MOCK_STORAGE_KEY);
  }
}

function getXsrfToken() {
  const match = document.cookie.match(/(?:^|; )XSRF-TOKEN=([^;]*)/);
  return match ? decodeURIComponent(match[1]) : '';
}

/**
 * Universal Request Wrapper
 */
async function request(endpoint, options = {}, isRetry = false) {
  const method = (options.method || 'GET').toUpperCase();
  const headers = { ...(options.headers || {}) };

  if (['POST', 'DELETE', 'PUT', 'PATCH'].includes(method)) {
    headers['Content-Type'] = 'application/json';
    const xsrfToken = getXsrfToken();
    if (xsrfToken) {
      headers['X-XSRF-TOKEN'] = xsrfToken;
    }
  }

  let response;
  try {
    if (isMockMode()) {
      response = await mockFetch(endpoint, { ...options, method, headers });
    } else {
      response = await fetch(endpoint, {
        ...options,
        method,
        headers,
        credentials: 'same-origin'
      });
    }
  } catch (netErr) {
    return {
      ok: false,
      status: 0,
      data: null,
      error: {
        reasonCode: 'NETWORK_ERROR',
        title: 'Connection error',
        message: 'Could not connect to the authentication server. Please check your network or enable Mock Mode.',
        nextStep: 'Check your connection or test using ?mock=1.'
      }
    };
  }

  // Handle 403 CSRF token expiration with automatic one-time retry
  if (response.status === 403 && !isRetry) {
    try {
      const peekData = await response.clone().json();
      if (peekData && (peekData.reasonCode === 'CSRF' || peekData.message?.toLowerCase().includes('csrf'))) {
        await api.me(); // Refresh token
        return request(endpoint, options, true); // Retry once
      }
    } catch (_) {}
  }

  let data = null;
  try {
    data = await response.json();
  } catch (_) {
    data = null;
  }

  if (response.ok) {
    return {
      ok: true,
      status: response.status,
      data
    };
  } else {
    // Normalise error shape: { reasonCode, title, message, nextStep }
    const reason = data?.reasonCode;
    const defaultInfo = messages.reasons[reason] || {};

    const error = {
      status: response.status,
      reasonCode: reason || 'UNKNOWN_ERROR',
      title: data?.title || defaultInfo.title || 'Action could not be completed',
      message: data?.message || defaultInfo.message || 'An unexpected error occurred. Please try again.',
      nextStep: data?.nextStep || defaultInfo.nextStep || 'Try again in a few moments.',
      raw: data
    };

    return {
      ok: false,
      status: response.status,
      data,
      error
    };
  }
}

/**
 * Public API client interface
 */
export const api = {
  isMockMode,
  setMockMode,

  /**
   * Called on every page load to initialize session & fetch XSRF cookie
   */
  async init() {
    return this.me();
  },

  async me() {
    return request('/api/auth/me');
  },

  async checkPassword(password) {
    return request('/api/password/check', {
      method: 'POST',
      body: JSON.stringify({ password })
    });
  },

  async login(email, password) {
    return request('/api/auth/login', {
      method: 'POST',
      body: JSON.stringify({ email, password })
    });
  },

  async sendEmailOtp(challengeId) {
    return request('/api/auth/mfa/send-email-otp', {
      method: 'POST',
      body: JSON.stringify({ challengeId })
    });
  },

  async verifyMfa(challengeId, method, code, rememberDevice = false) {
    return request('/api/auth/mfa/verify', {
      method: 'POST',
      body: JSON.stringify({ challengeId, method, code, rememberDevice })
    });
  },

  async getChallengeStatus(challengeId) {
    return request(`/api/auth/challenge/${encodeURIComponent(challengeId)}/status`);
  },

  async emailApproval(token, decision) {
    return request('/api/auth/email-approval', {
      method: 'POST',
      body: JSON.stringify({ token, decision })
    });
  },

  async register(email, displayName, password) {
    return request('/api/auth/register', {
      method: 'POST',
      body: JSON.stringify({ email, displayName, password })
    });
  },

  async verifyEmail(token) {
    return request('/api/auth/verify-email', {
      method: 'POST',
      body: JSON.stringify({ token })
    });
  },

  async resendVerification(email) {
    return request('/api/auth/resend-verification', {
      method: 'POST',
      body: JSON.stringify({ email })
    });
  },

  async logout() {
    return request('/api/auth/logout', { method: 'POST' });
  },

  async extendSession() {
    return request('/api/auth/session/extend', { method: 'POST' });
  },

  async setupTotp() {
    return request('/api/mfa/totp/setup', { method: 'POST' });
  },

  async confirmTotp(code) {
    return request('/api/mfa/totp/confirm', {
      method: 'POST',
      body: JSON.stringify({ code })
    });
  },

  async regenerateBackupCodes() {
    return request('/api/mfa/backup-codes/regenerate', { method: 'POST' });
  },

  async recoveryRequest(email) {
    return request('/api/recovery/request', {
      method: 'POST',
      body: JSON.stringify({ email })
    });
  },

  async recoveryValidate(token) {
    return request('/api/recovery/validate', {
      method: 'POST',
      body: JSON.stringify({ token })
    });
  },

  async recoveryReset(token, newPassword, secondFactorCode) {
    return request('/api/recovery/reset', {
      method: 'POST',
      body: JSON.stringify({ token, newPassword, secondFactorCode })
    });
  },

  async getSecurityCenter() {
    return request('/api/account/security');
  },

  async revokeDevice(id) {
    return request(`/api/account/devices/${encodeURIComponent(id)}`, { method: 'DELETE' });
  },

  async getLastDecision() {
    return request('/api/account/last-decision');
  },

  async explain(reasonCode, simplify = true) {
    return request('/api/assist/explain', {
      method: 'POST',
      body: JSON.stringify({ reasonCode, simplify })
    });
  },

  async getAdminMetrics(range = '24h') {
    return request(`/api/admin/metrics?range=${encodeURIComponent(range)}`);
  },

  async getAdminEvents(type = '', level = '', page = 1) {
    return request(`/api/admin/events?type=${encodeURIComponent(type)}&level=${encodeURIComponent(level)}&page=${page}`);
  },

  async getDemoContext() {
    return request('/api/demo/context');
  },

  async setDemoContext(context) {
    return request('/api/demo/context', {
      method: 'POST',
      body: JSON.stringify(context)
    });
  },

  async getDevOutbox(to = '') {
    const q = to ? `?to=${encodeURIComponent(to)}` : '';
    return request(`/api/dev/outbox${q}`);
  }
};
