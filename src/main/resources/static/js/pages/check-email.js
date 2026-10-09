/**
 * AuthEase - Check Your Email Page Controller
 * 
 * WCAG 2.2 AA Compliant
 * Features:
 * - 60-second accessible cooldown timer on resend
 * - Screen reader announcements
 * - Simulated Dev Outbox integration & automatic polling
 */

import { mountShell } from '../ui.js';
import { api } from '../api.js';
import { setStepHeading, announce } from '../a11y.js';
import { messages } from '../messages.en.js';

let resendInterval = null;

document.addEventListener('DOMContentLoaded', () => {
  mountShell({ activeNav: 'register' });
  initCheckEmailPage();
});

function initCheckEmailPage() {
  setStepHeading(messages.checkEmail.pageTitle, '#check-email-heading');

  const params = new URLSearchParams(window.location.search);
  const email = params.get('email') || sessionStorage.getItem('authease.pending_email') || 'your email address';

  const emailDisplay = document.getElementById('target-email-display');
  if (emailDisplay) {
    emailDisplay.textContent = email;
  }

  const devOutboxLink = document.getElementById('link-dev-outbox');
  if (devOutboxLink && email && email !== 'your email address') {
    devOutboxLink.href = `dev-outbox.html?email=${encodeURIComponent(email)}`;
  }

  const resendBtn = document.getElementById('btn-resend');
  const resendStatus = document.getElementById('resend-status');

  if (resendBtn) {
    resendBtn.addEventListener('click', async () => {
      resendBtn.disabled = true;
      resendBtn.textContent = 'Sending...';

      const res = await api.resendVerification(email);
      if (res.ok) {
        resendStatus.textContent = messages.checkEmail.sentNotice;
        announce(messages.checkEmail.sentNotice, 'assertive');
        startResendCooldown(resendBtn, resendStatus);
        checkLatestOutboxLink(email);
      } else {
        resendStatus.textContent = 'Could not send verification email. Please try again.';
        resendBtn.disabled = false;
        resendBtn.textContent = messages.checkEmail.resendBtn;
      }
    });
  }

  // Check outbox right away and poll up to 6 times (every 1.5s)
  checkLatestOutboxLink(email);
  let pollAttempts = 0;
  const pollTimer = setInterval(async () => {
    pollAttempts++;
    const found = await checkLatestOutboxLink(email);
    if (found || pollAttempts >= 6) {
      clearInterval(pollTimer);
    }
  }, 1500);
}

async function checkLatestOutboxLink(email) {
  const slot = document.getElementById('instant-activation-slot');
  if (!slot) return false;

  try {
    const res = await api.getDevOutbox();
    if (res.ok && Array.isArray(res.data) && res.data.length > 0) {
      const msg = res.data.find(m => 
        (email && email !== 'your email address' && m.to?.toLowerCase() === email.toLowerCase())
      ) || res.data.find(m => (m.subject?.includes('Verify') || m.type === 'VERIFICATION')) || res.data[0];

      if (msg) {
        let token = msg.token || null;
        if (!token) {
          const tokenMatch = (msg.body + ' ' + (msg.actionUrl || '')).match(/token=([a-zA-Z0-9_-]+)/i);
          if (tokenMatch) token = tokenMatch[1];
        }

        if (token) {
          slot.innerHTML = `
            <div class="alert alert-success p-3 text-start shadow-sm border-success">
              <div class="d-flex align-items-center gap-2 mb-2 text-success">
                <svg width="22" height="22" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2.5" aria-hidden="true"><path d="M22 11.08V12a10 10 0 1 1-5.93-9.14"></path><polyline points="22 4 12 14.01 9 11.01"></polyline></svg>
                <strong class="fs-6">Instant Activation Link Detected:</strong>
              </div>
              <p class="small text-dark mb-2">Simulated verification email for <strong>${escapeHtml(msg.to || email)}</strong> was received:</p>
              <a href="verify-email.html?token=${encodeURIComponent(token)}" class="btn btn-success w-100 fw-bold fs-6 py-2 shadow-sm mb-2">
                👉 Click Here to Activate Account Instantly
              </a>
              <div class="small text-muted text-center">
                Or inspect message details in the <a href="dev-outbox.html?email=${encodeURIComponent(email)}" class="text-decoration-underline">Simulated Dev Outbox</a>.
              </div>
            </div>
          `;
          announce('Verification link detected. Click the button to activate your account.');
          return true;
        }
      }
    }
  } catch (_) {}
  return false;
}

function startResendCooldown(btn, statusEl) {
  let remaining = 60;
  btn.disabled = true;

  clearInterval(resendInterval);
  resendInterval = setInterval(() => {
    remaining--;
    if (remaining <= 0) {
      clearInterval(resendInterval);
      btn.disabled = false;
      btn.textContent = messages.checkEmail.resendBtn;
      statusEl.textContent = '';
      return;
    }
    btn.textContent = messages.checkEmail.resendCooldown(remaining);
  }, 1000);
}

function escapeHtml(text) {
  if (!text) return '';
  return String(text).replace(/&/g, '&amp;').replace(/</g, '&lt;').replace(/>/g, '&gt;').replace(/"/g, '&quot;');
}
