/**
 * AuthEase - Verify Email Page Controller
 * 
 * WCAG 2.2 AA Compliant
 * Features:
 * - Reads ?token= and calls POST /api/auth/verify-email
 * - Manages accessible states: Loading -> Success OR Error
 * - Provides immediate fallback resend form
 */

import { mountShell } from '../ui.js';
import { api } from '../api.js';
import { setStepHeading, announce } from '../a11y.js';
import { messages } from '../messages.en.js';

document.addEventListener('DOMContentLoaded', () => {
  mountShell({ activeNav: 'register' });
  initVerifyEmailPage();
});

async function initVerifyEmailPage() {
  const loadingState = document.getElementById('verify-state-loading');
  const successState = document.getElementById('verify-state-success');
  const errorState = document.getElementById('verify-state-error');

  const params = new URLSearchParams(window.location.search);
  const token = params.get('token');

  if (!token) {
    showError('No verification token was provided in the link.');
    return;
  }

  try {
    const res = await api.verifyEmail(token);
    loadingState.classList.add('d-none');

    if (res.ok) {
      successState.classList.remove('d-none');
      setStepHeading('Email Verified - AuthEase', '#verify-success-heading');
      announce(messages.verifyEmail.successHeading, 'assertive');
    } else {
      const err = res.error || res.data;
      showError(err.message || messages.reasons.TOKEN_INVALID_OR_EXPIRED.message);
    }
  } catch (e) {
    loadingState.classList.add('d-none');
    showError('Could not connect to the verification server. Please try again.');
  }

  function showError(msg) {
    loadingState.classList.add('d-none');
    errorState.classList.remove('d-none');
    const msgEl = document.getElementById('verify-error-message');
    if (msgEl) msgEl.textContent = msg;

    setStepHeading('Verification Failed - AuthEase', '#verify-error-heading');
    announce(msg, 'assertive');

    setupResendForm();
  }

  function setupResendForm() {
    const form = document.getElementById('form-resend-link');
    const emailInput = document.getElementById('resend-email');
    const feedback = document.getElementById('resend-feedback');
    const submitBtn = document.getElementById('btn-submit-resend');

    if (form) {
      form.onsubmit = async (e) => {
        e.preventDefault();
        const email = emailInput.value.trim();
        if (!email || !email.includes('@')) {
          alert('Please enter a valid email address.');
          return;
        }

        submitBtn.disabled = true;
        submitBtn.textContent = 'Sending...';

        await api.resendVerification(email);
        submitBtn.textContent = 'Link sent!';
        feedback.classList.remove('d-none');
        feedback.textContent = messages.reasons.VERIFICATION_SENT.message;
        announce(messages.reasons.VERIFICATION_SENT.message);
      };
    }
  }
}
