/**
 * AuthEase - Mock API Client Engine (Offline / Standalone Demonstration Mode)
 * Simulates the complete shared API contract with all security scenarios and reasons.
 */

import { messages } from './messages.en.js';

// In-session state
const STORAGE_PREFIX = 'authease.mock.';

function getMockState() {
  try {
    const raw = localStorage.getItem(STORAGE_PREFIX + 'state');
    if (raw) {
      const parsed = JSON.parse(raw);
      if (!parsed.outbox || !Array.isArray(parsed.outbox) || parsed.outbox.length === 0) {
        parsed.outbox = [
          {
            id: 'msg-starter-01',
            to: 'demo@authease.local',
            subject: 'Verify your AuthEase account (Starter Sample)',
            type: 'VERIFICATION',
            token: 'mock-verify-token-welcome',
            actionUrl: 'verify-email.html?token=mock-verify-token-welcome',
            body: 'Welcome to AuthEase!\n\nThis is a starter sample email in your Simulated Dev Outbox.\nYou can register with ANY email address you want (e.g. test@example.com or your personal email), and your verification emails will appear right here!\n\nPlease verify your email address by opening the following link:\nverify-email.html?token=mock-verify-token-welcome\n\nThis link will expire in 24 hours.',
            ts: new Date().toISOString()
          }
        ];
      }
      return parsed;
    }
  } catch (e) {}

  return {
    authenticated: false,
    user: null,
    sessionExpiresInSeconds: 900,
    demoContext: {
      newDevice: false,
      newNetwork: false,
      unusualHour: false,
      forceFailures: false,
    },
    consecutiveFailures: 0,
    rateDelayUntil: 0,
    challenges: {},
    outbox: [
      {
        id: 'msg-starter-01',
        to: 'demo@authease.local',
        subject: 'Verify your AuthEase account (Starter Sample)',
        type: 'VERIFICATION',
        token: 'mock-verify-token-welcome',
        actionUrl: 'verify-email.html?token=mock-verify-token-welcome',
        body: 'Welcome to AuthEase!\n\nThis is a starter sample email in your Simulated Dev Outbox.\nYou can register with ANY email address you want (e.g. test@example.com or your personal email), and your verification emails will appear right here!\n\nPlease verify your email address by opening the following link:\nverify-email.html?token=mock-verify-token-welcome\n\nThis link will expire in 24 hours.',
        ts: new Date().toISOString()
      }
    ],
    events: [
      { id: 'ev-1', type: 'LOGIN_SUCCESS', level: 'LOW', timestamp: new Date(Date.now() - 3600000).toISOString(), ip: '192.168.1.10', userAgent: 'Chrome on Windows' },
      { id: 'ev-2', type: 'MFA_CHALLENGE', level: 'MEDIUM', timestamp: new Date(Date.now() - 7200000).toISOString(), ip: '198.51.100.24', userAgent: 'Safari on iPhone' },
      { id: 'ev-3', type: 'NEW_DEVICE', level: 'MEDIUM', timestamp: new Date(Date.now() - 86400000).toISOString(), ip: '198.51.100.24', userAgent: 'Safari on iPhone' }
    ],
    devices: [
      { id: 'dev-1', label: 'Windows PC - Chrome (This device)', lastUsed: 'Just now', current: true },
      { id: 'dev-2', label: 'iPhone 15 - Safari', lastUsed: 'Yesterday at 4:15 PM', current: false }
    ],
    lastDecision: {
      score: 15,
      level: 'LOW',
      signals: [
        { code: 'KNOWN_DEVICE', label: 'Recognized browser fingerprint', points: -10 },
        { code: 'TYPICAL_TIME', label: 'Within normal working hours', points: 0 },
        { code: 'TRUSTED_IP', label: 'Known domestic home network', points: -5 }
      ]
    }
  };
}

function saveMockState(state) {
  try {
    localStorage.setItem(STORAGE_PREFIX + 'state', JSON.stringify(state));
  } catch (e) {}
}

/**
 * Record an email sent to the mock dev outbox
 */
function sendToDevOutbox(state, emailData) {
  state.outbox.unshift({
    id: 'msg-' + Math.random().toString(36).substring(2, 9),
    ts: new Date().toISOString(),
    timestamp: new Date().toLocaleTimeString(),
    ...emailData
  });
  saveMockState(state);
}

/**
 * Main Mock Fetch Handler
 */
export async function mockFetch(url, options = {}) {
  const method = (options.method || 'GET').toUpperCase();
  const body = options.body ? JSON.parse(options.body) : {};
  const state = getMockState();

  // Artificial short delay to feel real (150ms)
  await new Promise(r => setTimeout(r, 150));

  // --- GET /api/health ---
  if (url === '/api/health') {
    return { ok: true, status: 200, json: async () => ({ status: 'UP' }) };
  }

  // --- GET /api/auth/me ---
  if (url === '/api/auth/me') {
    document.cookie = 'XSRF-TOKEN=mock-xsrf-token-' + Date.now() + '; Path=/; SameSite=Strict';
    return {
      ok: true,
      status: 200,
      json: async () => ({
        authenticated: state.authenticated,
        user: state.user,
        sessionExpiresInSeconds: state.authenticated ? state.sessionExpiresInSeconds : undefined
      })
    };
  }

  // --- POST /api/password/check ---
  if (url === '/api/password/check') {
    const pwd = body.password || '';
    let score = 0;
    const hints = [];
    if (pwd.length >= 8) score++;
    if (pwd.length >= 14) score++;
    if (/[A-Z]/.test(pwd) && /[a-z]/.test(pwd)) score++;
    if (/[0-9]/.test(pwd) || /[^A-Za-z0-9]/.test(pwd)) score++;
    if (score < 3) {
      hints.push('Try a memorable phrase of 3 or 4 random words (e.g. river blue lamp dance).');
    } else {
      hints.push('Strong passphrase! Easy to remember and hard to guess.');
    }
    const breached = pwd.toLowerCase() === 'password123' || pwd.toLowerCase() === '12345678';
    if (breached) {
      score = 0;
      hints.unshift('This password has been exposed in common security leaks. Please choose another.');
    }
    return {
      ok: true,
      status: 200,
      json: async () => ({ score, hints, breached })
    };
  }

  // --- POST /api/auth/register ---
  if (url === '/api/auth/register') {
    const rawToken = 'mock-verify-' + Math.random().toString(36).substring(2, 10);
    sendToDevOutbox(state, {
      to: body.email,
      subject: 'Verify your AuthEase account',
      type: 'VERIFICATION',
      token: rawToken,
      actionUrl: `verify-email.html?token=${rawToken}`,
      body: `Welcome to AuthEase!\n\nPlease verify your email address by opening the following link:\nverify-email.html?token=${rawToken}\n\nThis link will expire in 24 hours.`
    });
    return { ok: true, status: 200, json: async () => ({ status: 'OK' }) };
  }

  // --- POST /api/auth/verify-email ---
  if (url === '/api/auth/verify-email') {
    if (body.token) {
      return { ok: true, status: 200, json: async () => ({ status: 'OK' }) };
    }
    return {
      ok: false,
      status: 400,
      json: async () => ({
        reasonCode: 'TOKEN_INVALID_OR_EXPIRED',
        ...messages.reasons.TOKEN_INVALID_OR_EXPIRED
      })
    };
  }

  // --- POST /api/auth/resend-verification ---
  if (url === '/api/auth/resend-verification') {
    const rawToken = 'mock-verify-' + Math.random().toString(36).substring(2, 10);
    sendToDevOutbox(state, {
      to: body.email || 'user@example.com',
      subject: 'New Verification Link - AuthEase',
      type: 'VERIFICATION',
      token: rawToken,
      actionUrl: `verify-email.html?token=${rawToken}`,
      body: `Welcome to AuthEase!\n\nHere is your new account verification link:\nverify-email.html?token=${rawToken}\n\nThis link will expire in 24 hours.`
    });
    return { ok: true, status: 200, json: async () => ({ status: 'OK' }) };
  }

  // --- POST /api/auth/login ---
  if (url === '/api/auth/login') {
    const { email = '', password = '' } = body;

    // Check if in active RATE_DELAY
    const now = Date.now();
    if (state.rateDelayUntil && now < state.rateDelayUntil) {
      const remaining = Math.ceil((state.rateDelayUntil - now) / 1000);
      return {
        ok: false,
        status: 429,
        json: async () => ({
          status: 'DELAYED',
          retryAfterSeconds: remaining,
          reasonCode: 'RATE_DELAY',
          ...messages.reasons.RATE_DELAY
        })
      };
    }

    // Check forced failures or > 3 failures
    if (state.demoContext.forceFailures || state.consecutiveFailures >= 3) {
      state.rateDelayUntil = Date.now() + 30000; // 30 sec delay
      state.consecutiveFailures = 0;
      state.demoContext.forceFailures = false;
      saveMockState(state);
      return {
        ok: false,
        status: 429,
        json: async () => ({
          status: 'DELAYED',
          retryAfterSeconds: 30,
          reasonCode: 'RATE_DELAY',
          ...messages.reasons.RATE_DELAY
        })
      };
    }

    // Unverified email scenario
    if (email.toLowerCase().includes('unverified')) {
      return {
        ok: false,
        status: 400,
        json: async () => ({
          status: 'FAILED',
          reasonCode: 'EMAIL_NOT_VERIFIED',
          ...messages.reasons.EMAIL_NOT_VERIFIED
        })
      };
    }

    // Wrong password scenario
    if (password === 'wrong' || password === 'error') {
      state.consecutiveFailures++;
      saveMockState(state);
      return {
        ok: false,
        status: 401,
        json: async () => ({
          status: 'FAILED',
          reasonCode: 'INVALID_CREDENTIALS',
          ...messages.reasons.INVALID_CREDENTIALS
        })
      };
    }

    // SCENARIO 3: Suspicious / High Risk (Email Approval required)
    if (email.toLowerCase().includes('suspicious') || state.demoContext.unusualHour || state.demoContext.newNetwork) {
      const challengeId = 'ch-high-' + Math.random().toString(36).substring(2, 8);
      state.challenges[challengeId] = {
        level: 'HIGH',
        needsEmailApproval: true,
        emailApproved: false,
        failedOtpAttempts: 0,
        email,
        expiresAt: Date.now() + 300000
      };
      
      const approvalToken = 'appr-token-' + challengeId;
      sendToDevOutbox(state, {
        to: email,
        subject: 'Security Alert: Sign-in approval required',
        type: 'EMAIL_APPROVAL',
        token: approvalToken,
        actionUrl: `approve.html?token=${approvalToken}`,
        body: 'A sign-in attempt from an unusual location was detected. Click to approve or deny.'
      });

      saveMockState(state);
      return {
        ok: true,
        status: 200,
        json: async () => ({
          status: 'MFA_REQUIRED',
          challengeId,
          level: 'HIGH',
          methods: ['TOTP', 'EMAIL_OTP', 'BACKUP_CODE'],
          needsEmailApproval: true,
          step: { current: 2, total: 3 },
          reasonCode: 'UNUSUAL_HOUR',
          ...messages.reasons.UNUSUAL_HOUR
        })
      };
    }

    // SCENARIO 2: New Device (Medium MFA, code 123456)
    if (email.toLowerCase().includes('mfa') || state.demoContext.newDevice) {
      const challengeId = 'ch-med-' + Math.random().toString(36).substring(2, 8);
      state.challenges[challengeId] = {
        level: 'MEDIUM',
        needsEmailApproval: false,
        failedOtpAttempts: 0,
        email,
        expiresAt: Date.now() + 300000
      };
      saveMockState(state);
      return {
        ok: true,
        status: 200,
        json: async () => ({
          status: 'MFA_REQUIRED',
          challengeId,
          level: 'MEDIUM',
          methods: ['TOTP', 'EMAIL_OTP', 'BACKUP_CODE'],
          needsEmailApproval: false,
          step: { current: 2, total: 2 },
          reasonCode: 'NEW_DEVICE',
          ...messages.reasons.NEW_DEVICE
        })
      };
    }

    // SCENARIO 1: Trusted Device -> AUTHENTICATED
    state.authenticated = true;
    state.consecutiveFailures = 0;
    state.user = {
      email,
      displayName: email.split('@')[0] || 'Demo User',
      roles: email.startsWith('admin') ? ['ADMIN', 'USER'] : ['USER'],
      mfaEnabled: true
    };
    saveMockState(state);
    return {
      ok: true,
      status: 200,
      json: async () => ({
        status: 'AUTHENTICATED',
        user: state.user
      })
    };
  }

  // --- POST /api/auth/mfa/send-email-otp ---
  if (url === '/api/auth/mfa/send-email-otp') {
    const ch = state.challenges[body.challengeId];
    const emailTarget = ch ? ch.email : 'user@example.com';
    sendToDevOutbox(state, {
      to: emailTarget,
      subject: 'Your 6-Digit AuthEase Security Code',
      type: 'OTP_CODE',
      code: '654321',
      body: 'Your single-use sign-in security code is 654321. Valid for 5 minutes.'
    });
    return { ok: true, status: 200, json: async () => ({ status: 'OK' }) };
  }

  // --- POST /api/auth/mfa/verify ---
  if (url === '/api/auth/mfa/verify') {
    const { challengeId, code, method: factorMethod } = body;
    const ch = state.challenges[challengeId] || { failedOtpAttempts: 0, level: 'MEDIUM' };

    // SCENARIO 6: 5 wrong attempts -> CHALLENGE_LOCKED
    if (code !== '123456' && code !== '654321' && code !== 'BACKUP01') {
      ch.failedOtpAttempts = (ch.failedOtpAttempts || 0) + 1;
      saveMockState(state);

      if (ch.failedOtpAttempts >= 5) {
        delete state.challenges[challengeId];
        saveMockState(state);
        return {
          ok: false,
          status: 403,
          json: async () => ({
            status: 'FAILED',
            reasonCode: 'CHALLENGE_LOCKED',
            ...messages.reasons.CHALLENGE_LOCKED
          })
        };
      }

      return {
        ok: false,
        status: 400,
        json: async () => ({
          status: 'FAILED',
          reasonCode: 'OTP_INVALID',
          ...messages.reasons.OTP_INVALID
        })
      };
    }

    // Code is valid!
    if (ch.needsEmailApproval) {
      // Moves to Step 3 (Waiting for email approval)
      return {
        ok: true,
        status: 200,
        json: async () => ({
          status: 'MFA_REQUIRED',
          challengeId,
          level: 'HIGH',
          needsEmailApproval: true,
          step: { current: 3, total: 3 },
          reasonCode: 'EMAIL_APPROVAL_PENDING',
          ...messages.reasons.EMAIL_APPROVAL_PENDING
        })
      };
    }

    // Fully authenticated
    state.authenticated = true;
    state.user = {
      email: ch.email || 'user@example.com',
      displayName: (ch.email || 'user').split('@')[0],
      roles: ['USER'],
      mfaEnabled: true
    };
    delete state.challenges[challengeId];
    saveMockState(state);

    return {
      ok: true,
      status: 200,
      json: async () => ({
        status: 'AUTHENTICATED',
        user: state.user
      })
    };
  }

  // --- GET /api/auth/challenge/{id}/status ---
  if (url.startsWith('/api/auth/challenge/')) {
    const parts = url.split('/');
    const challengeId = parts[parts.length - 2];
    const ch = state.challenges[challengeId];

    return {
      ok: true,
      status: 200,
      json: async () => ({
        emailApproved: ch ? ch.emailApproved : false,
        expiresInSeconds: 240,
        minimumDelayRemainingSeconds: 0
      })
    };
  }

  // --- POST /api/auth/email-approval ---
  if (url === '/api/auth/email-approval') {
    const { token, decision } = body;
    // Look up matching challenge
    for (const id in state.challenges) {
      if (token && token.includes(id)) {
        if (decision === 'APPROVE') {
          state.challenges[id].emailApproved = true;
        } else {
          delete state.challenges[id];
        }
      }
    }
    saveMockState(state);
    return {
      ok: true,
      status: 200,
      json: async () => ({
        status: 'OK',
        decision
      })
    };
  }

  // --- POST /api/auth/logout ---
  if (url === '/api/auth/logout') {
    state.authenticated = false;
    state.user = null;
    saveMockState(state);
    return { ok: true, status: 200, json: async () => ({ status: 'OK' }) };
  }

  // --- POST /api/auth/session/extend ---
  if (url === '/api/auth/session/extend') {
    state.sessionExpiresInSeconds = 900;
    saveMockState(state);
    return { ok: true, status: 200, json: async () => ({ status: 'OK', sessionExpiresInSeconds: 900 }) };
  }

  // --- POST /api/mfa/totp/setup ---
  if (url === '/api/mfa/totp/setup') {
    return {
      ok: true,
      status: 200,
      json: async () => ({
        // Inline SVG Data URL for offline QR display
        qrDataUrl: 'data:image/svg+xml;utf8,<svg xmlns="http://www.w3.org/2000/svg" width="200" height="200" viewBox="0 0 200 200"><rect width="200" height="200" fill="%23ffffff"/><rect x="20" y="20" width="60" height="60" fill="%230284c7"/><rect x="120" y="20" width="60" height="60" fill="%230284c7"/><rect x="20" y="120" width="60" height="60" fill="%230284c7"/><rect x="35" y="35" width="30" height="30" fill="%23ffffff"/><rect x="135" y="35" width="30" height="30" fill="%23ffffff"/><rect x="35" y="135" width="30" height="30" fill="%23ffffff"/><rect x="95" y="95" width="20" height="20" fill="%230284c7"/><text x="100" y="190" font-size="12" font-family="sans-serif" text-anchor="middle" fill="%23475569">Sample QR Code</text></svg>',
        manualKey: 'JBSWY3DPEHPK3PXP'
      })
    };
  }

  // --- POST /api/mfa/totp/confirm ---
  if (url === '/api/mfa/totp/confirm') {
    return {
      ok: true,
      status: 200,
      json: async () => ({
        backupCodes: [
          'A8F2-9X1B', 'K3P9-7M2Q', 'R4T1-8L5W', 'E6Y8-2C9V', 'M1N5-4B8X',
          'T7H2-6J9K', 'D3F8-1S4P', 'W9L2-5Z8Q', 'C4V7-3N1M', 'X8B2-9T4R'
        ]
      })
    };
  }

  // --- POST /api/mfa/backup-codes/regenerate ---
  if (url === '/api/mfa/backup-codes/regenerate') {
    return {
      ok: true,
      status: 200,
      json: async () => ({
        backupCodes: [
          'B9G3-1Y2C', 'L4Q1-8N3R', 'S5U2-9M6X', 'F7Z9-3D1W', 'N2P6-5C9Y',
          'U8J3-7K1L', 'E4G9-2T5Q', 'X1M3-6A9R', 'D5W8-4P2N', 'Y9C3-1U5S'
        ]
      })
    };
  }

  // --- POST /api/recovery/request ---
  if (url === '/api/recovery/request') {
    sendToDevOutbox(state, {
      to: body.email,
      subject: 'AuthEase Account Recovery Instructions',
      type: 'RECOVERY',
      token: 'mock-recovery-token-456',
      actionUrl: `reset.html?token=mock-recovery-token-456`,
      body: 'Use this secure link to set a new password and review your security settings.'
    });
    return {
      ok: true,
      status: 200,
      json: async () => ({
        status: 'OK',
        message: messages.reasons.RECOVERY_SENT.message
      })
    };
  }

  // --- POST /api/recovery/validate ---
  if (url === '/api/recovery/validate') {
    return {
      ok: true,
      status: 200,
      json: async () => ({
        valid: true,
        requiresSecondFactor: true
      })
    };
  }

  // --- POST /api/recovery/reset ---
  if (url === '/api/recovery/reset') {
    return {
      ok: true,
      status: 200,
      json: async () => ({
        status: 'OK',
        message: 'Your password has been changed. All other devices were signed out.'
      })
    };
  }

  // --- GET /api/account/security ---
  if (url === '/api/account/security') {
    return {
      ok: true,
      status: 200,
      json: async () => ({
        mfaEnabled: true,
        devices: state.devices,
        recentEvents: state.events
      })
    };
  }

  // --- DELETE /api/account/devices/{id} ---
  if (url.startsWith('/api/account/devices/')) {
    const id = url.split('/').pop();
    state.devices = state.devices.filter(d => d.id !== id);
    saveMockState(state);
    return { ok: true, status: 200, json: async () => ({ status: 'OK' }) };
  }

  // --- GET /api/account/last-decision ---
  if (url === '/api/account/last-decision') {
    return {
      ok: true,
      status: 200,
      json: async () => state.lastDecision
    };
  }

  // --- POST /api/assist/explain ---
  if (url === '/api/assist/explain') {
    const code = body.reasonCode || 'DEFAULT';
    const reasonInfo = messages.reasons[code] || messages.reasons.NEW_DEVICE;
    return {
      ok: true,
      status: 200,
      json: async () => ({
        text: `${reasonInfo.title}: ${reasonInfo.message} ${reasonInfo.nextStep}`,
        source: 'TEMPLATE'
      })
    };
  }

  // --- GET /api/admin/metrics ---
  if (url.startsWith('/api/admin/metrics')) {
    return {
      ok: true,
      status: 200,
      json: async () => ({
        firstTrySuccessRate: 92.4,
        mfaDropOffRate: 2.1,
        recoveryCompletionRate: 96.8,
        avgRecoverySeconds: 114,
        loginsByLevel: { LOW: 1420, MEDIUM: 310, HIGH: 48 },
        failuresPerHour: [
          { hour: '00:00', count: 4 }, { hour: '04:00', count: 1 },
          { hour: '08:00', count: 12 }, { hour: '12:00', count: 18 },
          { hour: '16:00', count: 15 }, { hour: '20:00', count: 7 }
        ]
      })
    };
  }

  // --- GET /api/admin/events ---
  if (url.startsWith('/api/admin/events')) {
    return {
      ok: true,
      status: 200,
      json: async () => ({
        items: state.events,
        total: state.events.length,
        page: 1,
        pageSize: 10
      })
    };
  }

  // --- DEMO / OUTBOX ENDPOINTS ---
  if (url === '/api/demo/context') {
    if (method === 'POST') {
      state.demoContext = { ...state.demoContext, ...body };
      saveMockState(state);
      return { ok: true, status: 200, json: async () => state.demoContext };
    }
    return { ok: true, status: 200, json: async () => state.demoContext };
  }

  if (url.startsWith('/api/dev/outbox')) {
    const queryPart = url.includes('?') ? url.split('?')[1] : '';
    const params = new URLSearchParams(queryPart);
    const toFilter = params.get('to');
    if (toFilter) {
      const filtered = state.outbox.filter(m => m.to && m.to.toLowerCase() === toFilter.toLowerCase());
      return { ok: true, status: 200, json: async () => filtered };
    }
    return {
      ok: true,
      status: 200,
      json: async () => state.outbox
    };
  }

  // Fallback 404
  return {
    ok: false,
    status: 404,
    json: async () => ({
      reasonCode: 'NOT_FOUND',
      title: 'Resource not found',
      message: 'The requested mock endpoint was not recognized.',
      nextStep: 'Check API contract URL.'
    })
  };
}
