/**
 * AuthEase - Account Recovery Page Controller
 */

import { mountShell, showErrorSummary, clearErrorSummary } from '../ui.js';
import { api } from '../api.js';
import { setStepHeading, announce } from '../a11y.js';

let recoveryToken = null;

document.addEventListener('DOMContentLoaded', () => {
  mountShell({ activeNav: 'login' });
  initRecoverPage();
});

async function initRecoverPage() {
  const params = new URLSearchParams(window.location.search);
  recoveryToken = params.get('token');

  if (recoveryToken) {
    // We have a token in the URL! Validate and show step 2
    await handleTokenValidation(recoveryToken);
  } else {
    // Normal request flow
    setupRequestFlow();
  }
}

function setupRequestFlow() {
  setStepHeading('Reset your password', '#recover-heading');
  const form = document.getElementById('form-request');
  const emailInput = document.getElementById('recovery-email');
  const errorSlot = document.getElementById('error-summary-slot');
  const stepRequest = document.getElementById('step-request');
  const stepNotice = document.getElementById('step-requested-notice');
  const noticeEmail = document.getElementById('notice-email');

  form.addEventListener('submit', async (e) => {
    e.preventDefault();
    clearErrorSummary(errorSlot);

    const email = emailInput.value.trim();
    if (!email) {
      showErrorSummary(errorSlot, [{ message: 'Please enter your email address.' }]);
      return;
    }

    const btn = document.getElementById('btn-submit-request');
    btn.disabled = true;
    btn.textContent = 'Sending...';

    const res = await api.recoveryRequest(email);
    btn.disabled = false;
    btn.textContent = 'Send recovery link';

    if (res.ok) {
      stepRequest.classList.add('d-none');
      stepNotice.classList.remove('d-none');
      if (noticeEmail) noticeEmail.textContent = email;
      announce('Recovery email sent. Check your inbox or dev outbox.');
    } else {
      const err = res.error || {};
      showErrorSummary(errorSlot, [{ message: err.message || 'Could not send recovery link.' }]);
    }
  });
}

async function handleTokenValidation(token) {
  const stepRequest = document.getElementById('step-request');
  const stepReset = document.getElementById('step-reset');
  const errorSlot = document.getElementById('error-summary-slot');
  const secondFactorGroup = document.getElementById('second-factor-group');

  stepRequest.classList.add('d-none');

  const valRes = await api.recoveryValidate(token);
  if (!valRes.ok) {
    stepRequest.classList.remove('d-none');
    const err = valRes.error || {};
    showErrorSummary(errorSlot, [{ message: err.message || 'Recovery link has expired or is invalid.' }]);
    return;
  }

  // Valid token!
  stepReset.classList.remove('d-none');
  setStepHeading('Choose new password', '#step-reset h1');

  if (valRes.data && valRes.data.requiresSecondFactor) {
    secondFactorGroup.classList.remove('d-none');
  }

  setupResetSubmit(token);
}

function setupResetSubmit(token) {
  const form = document.getElementById('form-reset');
  const passInput = document.getElementById('new-password');
  const mfaInput = document.getElementById('second-factor-code');
  const errorSlot = document.getElementById('error-summary-slot');
  const stepReset = document.getElementById('step-reset');
  const stepSuccess = document.getElementById('step-reset-success');

  form.addEventListener('submit', async (e) => {
    e.preventDefault();
    clearErrorSummary(errorSlot);

    const newPass = passInput.value;
    const mfaCode = mfaInput.value.trim();

    if (!newPass || newPass.length < 8) {
      showErrorSummary(errorSlot, [{ message: 'Password must be at least 8 characters long.' }]);
      return;
    }

    const btn = document.getElementById('btn-submit-reset');
    btn.disabled = true;
    btn.textContent = 'Saving password...';

    const res = await api.recoveryReset(token, newPass, mfaCode || null);
    btn.disabled = false;
    btn.textContent = 'Save new password';

    if (res.ok) {
      stepReset.classList.add('d-none');
      stepSuccess.classList.remove('d-none');
      announce('Password changed successfully.');
    } else {
      const err = res.error || {};
      showErrorSummary(errorSlot, [{ message: err.message || 'Could not reset password.' }]);
    }
  });
}
