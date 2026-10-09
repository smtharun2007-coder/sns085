/**
 * AuthEase - Check Your Email Page Controller
 * 
 * WCAG 2.2 AA Compliant
 * Features:
 * - 60-second accessible cooldown timer on resend
 * - Screen reader announcements
 * - Simulated Dev Outbox integration
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

  // Check outbox right away to offer 1-click activation
  checkLatestOutboxLink(email);
}

async function checkLatestOutboxLink(email) {
  const slot = document.getElementById('instant-activation-slot');
  if (!slot) return;

  try {
    const res = await api.getDevOutbox();
    if (res.ok && Array.isArray(res.data) && res.data.length > 0) {
      const msg = res.data.find(m => (!email || email === 'your email address' || m.to?.toLowerCase() === email.toLowerCase()) && (m.subject?.includes('Verify') || m.type === 'VERIFICATION')) || res.data[0];

      if (msg) {
        const tokenMatch = (msg.body + ' ' + (msg.actionUrl || '') + ' ' + (msg.token || '')).match(/token=([a-zA-Z0-9_-]+)/i);
        const token = tokenMatch ? tokenMatch[1] : (msg.token || null);

        if (token) {
          slot.innerHTML = `
            <div class="alert alert-success p-3 text-start shadow-sm border-success">
              <div class="d-flex align-items-center gap-2 mb-2 text-success">
                <svg width="22" height="22" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2.5"><path d="M22 11.08V12a10 10 0 1 1-5.93-9.14"></path><polyline points="22 4 12 14.01 9 11.01"></polyline></svg>
                <strong class="fs-6">Instant Activation Available:</strong>
              </div>
              <p class="small text-dark mb-3">Your verification email was generated in the outbox. Click below to activate your account immediately:</p>
              <a href="verify-email.html?token=${encodeURIComponent(token)}" class="btn btn-success w-100 fw-bold fs-6 py-2 shadow-sm">
                👉 Click Here to Activate Account Instantly
              </a>
            </div>
          `;
          announce('Verification link detected. Click the button to activate your account.');
        }
      }
    }
  } catch (_) {}
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
