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
      } else {
        resendStatus.textContent = 'Could not send verification email. Please try again.';
        resendBtn.disabled = false;
        resendBtn.textContent = messages.checkEmail.resendBtn;
      }
    });
  }
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
