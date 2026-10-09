/**
 * AuthEase - Login Page State Machine & Controller
 * 
 * WCAG 2.2 AA Compliant
 * Handles States: CREDENTIALS -> SECOND_FACTOR -> WAITING_EMAIL_APPROVAL -> SUCCESS
 * Statuses: AUTHENTICATED, MFA_REQUIRED, DELAYED, FAILED, CHALLENGE_LOCKED
 */

import { mountShell, showErrorSummary, clearErrorSummary, renderRiskBadge, renderStepIndicator, renderWhyDisclosure, initDisclosures } from '../ui.js';
import { api } from '../api.js';
import { announce, setStepHeading, focusTarget } from '../a11y.js';
import { messages } from '../messages.en.js';

// State Machine Storage
let currentState = 'CREDENTIALS';
let currentChallenge = null;
let approvalPollInterval = null;
let delayCountdownInterval = null;
let emailOtpCooldownInterval = null;

document.addEventListener('DOMContentLoaded', () => {
  mountShell({ activeNav: 'login' });
  initLoginPage();
});

function initLoginPage() {
  setupPasswordToggle();
  setupCredentialsForm();
  setupMfaForm();
  setupApprovalScreen();
  handleUrlPresets();

  // Initial Step 1 Setup
  transitionTo('CREDENTIALS');
}

/**
 * Handle URL Presets for Hackathon Demo & Testing
 */
function handleUrlPresets() {
  const params = new URLSearchParams(window.location.search);
  const scenario = params.get('scenario');
  const emailInput = document.getElementById('email');
  const passwordInput = document.getElementById('password');

  if (!emailInput || !passwordInput) return;

  if (scenario === 'trusted') {
    emailInput.value = 'user@example.com';
    passwordInput.value = 'Password123!';
  } else if (scenario === 'new_device') {
    emailInput.value = 'mfa@example.com';
    passwordInput.value = 'Password123!';
  } else if (scenario === 'suspicious') {
    emailInput.value = 'suspicious@example.com';
    passwordInput.value = 'Password123!';
  } else if (scenario === 'wrong') {
    emailInput.value = 'user@example.com';
    passwordInput.value = 'wrong';
  }
}

/**
 * Password Visibility Toggle
 */
function setupPasswordToggle() {
  const toggleBtn = document.getElementById('toggle-password-btn');
  const passwordInput = document.getElementById('password');
  if (!toggleBtn || !passwordInput) return;

  toggleBtn.addEventListener('click', () => {
    const isShowing = passwordInput.type === 'text';
    passwordInput.type = isShowing ? 'password' : 'text';
    toggleBtn.setAttribute('aria-pressed', String(!isShowing));
    toggleBtn.setAttribute('aria-label', isShowing ? messages.login.showPassword : messages.login.hidePassword);
    
    // Toggle SVG icon
    toggleBtn.innerHTML = isShowing ? `
      <svg width="18" height="18" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2" class="eye-icon" aria-hidden="true">
        <path d="M1 12s4-8 11-8 11 8 11 8-4 8-11 8-11-8-11-8z"></path>
        <circle cx="12" cy="12" r="3"></circle>
      </svg>
    ` : `
      <svg width="18" height="18" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2" class="eye-off-icon" aria-hidden="true">
        <path d="M17.94 17.94A10.07 10.07 0 0 1 12 20c-7 0-11-8-11-8a18.45 18.45 0 0 1 5.06-5.94M9.9 4.24A9.12 9.12 0 0 1 12 4c7 0 11 8 11 8a18.5 18.5 0 0 1-2.16 3.19m-6.72-1.07a3 3 0 1 1-4.24-4.24"></path>
        <line x1="1" y1="1" x2="23" y2="23"></line>
      </svg>
    `;
  });
}

/**
 * Master State Machine Transition
 * @param {'CREDENTIALS'|'SECOND_FACTOR'|'WAITING_EMAIL_APPROVAL'|'SUCCESS'} nextState 
 * @param {Object} [payload] 
 */
function transitionTo(nextState, payload = {}) {
  currentState = nextState;
  clearIntervals();

  // Hide all step sections
  document.querySelectorAll('.auth-state-step').forEach(el => el.classList.add('d-none'));

  const stepSlot = document.getElementById('step-indicator-slot');
  const errorSlot = document.getElementById('error-summary-slot');
  clearErrorSummary(errorSlot);

  switch (nextState) {
    case 'CREDENTIALS': {
      document.getElementById('state-credentials').classList.remove('d-none');
      stepSlot.innerHTML = ''; // Single step for credentials
      setStepHeading('Sign in - AuthEase', '#login-heading');
      break;
    }

    case 'SECOND_FACTOR': {
      document.getElementById('state-second-factor').classList.remove('d-none');
      const step = payload.step || { current: 2, total: 2 };
      stepSlot.innerHTML = renderStepIndicator(step.current, step.total, messages.steps.mfaTitle);
      
      // Mount risk badge
      const riskSlot = document.getElementById('mfa-risk-badge-slot');
      if (riskSlot) riskSlot.innerHTML = renderRiskBadge(payload.level || 'MEDIUM');

      // Mount disclosure
      const discSlot = document.getElementById('mfa-why-disclosure-slot');
      if (discSlot) {
        discSlot.innerHTML = renderWhyDisclosure({
          title: payload.title || messages.reasons[payload.reasonCode]?.title,
          message: payload.message || messages.reasons[payload.reasonCode]?.message,
          nextStep: payload.nextStep || messages.reasons[payload.reasonCode]?.nextStep
        });
        initDisclosures(discSlot);
      }

      // Render Methods Radio Group
      renderMfaMethods(payload.methods || ['TOTP']);

      // Focus heading
      setStepHeading('Confirm your identity - AuthEase', '#mfa-heading');
      break;
    }

    case 'WAITING_EMAIL_APPROVAL': {
      document.getElementById('state-email-approval').classList.remove('d-none');
      stepSlot.innerHTML = renderStepIndicator(3, 3, messages.steps.approvalTitle);

      const riskSlot = document.getElementById('approval-risk-badge-slot');
      if (riskSlot) riskSlot.innerHTML = renderRiskBadge('HIGH');

      const discSlot = document.getElementById('approval-why-disclosure-slot');
      if (discSlot) {
        discSlot.innerHTML = renderWhyDisclosure({
          title: messages.reasons.EMAIL_APPROVAL_PENDING.title,
          message: messages.reasons.EMAIL_APPROVAL_PENDING.message,
          nextStep: messages.reasons.EMAIL_APPROVAL_PENDING.nextStep
        });
        initDisclosures(discSlot);
      }

      // Configure direct testing shortcut link
      const demoDirectLink = document.getElementById('demo-direct-approval-link');
      if (demoDirectLink && payload.challengeId) {
        demoDirectLink.href = `approve.html?token=appr-token-${payload.challengeId}`;
      }

      setStepHeading('Check your email - AuthEase', '#approval-heading');
      startApprovalPolling(payload.challengeId);
      break;
    }

    case 'SUCCESS': {
      document.getElementById('state-success').classList.remove('d-none');
      stepSlot.innerHTML = '';
      const userDisplay = document.getElementById('success-user-display');
      if (userDisplay && payload.user?.displayName) {
        userDisplay.textContent = `Welcome back, ${payload.user.displayName}!`;
      }
      const adminLink = document.getElementById('link-admin-panel');
      if (adminLink && payload.user?.roles?.includes('ADMIN')) {
        adminLink.classList.remove('d-none');
      }
      setStepHeading('Sign-in Successful - AuthEase', '#success-heading');
      announce('You are signed in successfully.');
      break;
    }
  }
}

/**
 * Handle Credentials Form Submission
 */
function setupCredentialsForm() {
  const form = document.getElementById('form-credentials');
  const emailInput = document.getElementById('email');
  const passwordInput = document.getElementById('password');
  const emailError = document.getElementById('email-error');
  const passwordError = document.getElementById('password-error');
  const submitBtn = document.getElementById('btn-submit-credentials');
  const errorSlot = document.getElementById('error-summary-slot');
  const resendBtn = document.getElementById('btn-resend-verification');

  form.addEventListener('submit', async (e) => {
    e.preventDefault();
    clearErrors();

    const email = emailInput.value.trim();
    const password = passwordInput.value;

    const errors = [];
    if (!email) {
      errors.push({ fieldId: 'email', message: messages.a11y.fieldRequired('email address') });
      showFieldError(emailInput, emailError, messages.a11y.fieldRequired('email address'));
    } else if (!email.includes('@')) {
      errors.push({ fieldId: 'email', message: messages.a11y.invalidEmail });
      showFieldError(emailInput, emailError, messages.a11y.invalidEmail);
    }

    if (!password) {
      errors.push({ fieldId: 'password', message: messages.a11y.fieldRequired('password') });
      showFieldError(passwordInput, passwordError, messages.a11y.fieldRequired('password'));
    }

    if (errors.length > 0) {
      showErrorSummary(errorSlot, errors);
      return;
    }

    // Call API
    submitBtn.disabled = true;
    submitBtn.textContent = 'Checking details...';

    const res = await api.login(email, password);
    submitBtn.disabled = false;
    submitBtn.textContent = messages.login.submitCredentials;

    if (res.ok) {
      const data = res.data;
      if (data.status === 'AUTHENTICATED') {
        transitionTo('SUCCESS', data);
      } else if (data.status === 'MFA_REQUIRED') {
        currentChallenge = data;
        transitionTo('SECOND_FACTOR', data);
      }
    } else {
      handleLoginFailure(res.error || res.data);
    }
  });

  if (resendBtn) {
    resendBtn.addEventListener('click', async () => {
      resendBtn.disabled = true;
      resendBtn.textContent = 'Sending...';
      const email = emailInput.value.trim();
      await api.resendVerification(email);
      resendBtn.textContent = 'Verification email sent! Check your inbox.';
      announce(messages.reasons.VERIFICATION_SENT.message);
    });
  }

  function clearErrors() {
    clearErrorSummary(errorSlot);
    emailInput.classList.remove('is-invalid');
    emailInput.removeAttribute('aria-invalid');
    emailError.classList.add('d-none');
    emailError.textContent = '';

    passwordInput.classList.remove('is-invalid');
    passwordInput.removeAttribute('aria-invalid');
    passwordError.classList.add('d-none');
    passwordError.textContent = '';
  }

  function showFieldError(input, errorContainer, message) {
    input.classList.add('is-invalid');
    input.setAttribute('aria-invalid', 'true');
    errorContainer.classList.remove('d-none');
    errorContainer.textContent = message;
  }
}

/**
 * Handle Failure Statuses (DELAYED, INVALID_CREDENTIALS, EMAIL_NOT_VERIFIED)
 */
function handleLoginFailure(err) {
  const errorSlot = document.getElementById('error-summary-slot');
  const delayedBanner = document.getElementById('delayed-banner');
  const unverifiedBanner = document.getElementById('unverified-banner');
  const submitBtn = document.getElementById('btn-submit-credentials');

  delayedBanner.classList.add('d-none');
  unverifiedBanner.classList.add('d-none');

  if (err.status === 'DELAYED' || err.reasonCode === 'RATE_DELAY') {
    const seconds = err.retryAfterSeconds || 30;
    startDelayCountdown(seconds);
    showErrorSummary(errorSlot, [{ message: err.message || messages.reasons.RATE_DELAY.message }], err.title);
    return;
  }

  if (err.reasonCode === 'EMAIL_NOT_VERIFIED') {
    unverifiedBanner.classList.remove('d-none');
    showErrorSummary(errorSlot, [
      { fieldId: 'email', message: err.message || messages.reasons.EMAIL_NOT_VERIFIED.message }
    ], err.title);
    return;
  }

  if (err.reasonCode === 'INVALID_CREDENTIALS') {
    const passwordInput = document.getElementById('password');
    passwordInput.classList.add('is-invalid');
    passwordInput.setAttribute('aria-invalid', 'true');
    showErrorSummary(errorSlot, [
      { fieldId: 'password', message: err.message || messages.reasons.INVALID_CREDENTIALS.message }
    ], err.title);
    return;
  }

  // Generic failure fallback
  showErrorSummary(errorSlot, [{ message: err.message || 'Could not sign in.' }], err.title);
}

/**
 * Rate Limiting Delay Countdown with Screen Reader Throttling
 * WCAG rule: Announce every 10 seconds, not every second!
 */
function startDelayCountdown(secondsTotal) {
  const banner = document.getElementById('delayed-banner');
  const msgEl = banner.querySelector('.delayed-message');
  const countEl = banner.querySelector('.delayed-countdown');
  const submitBtn = document.getElementById('btn-submit-credentials');

  banner.classList.remove('d-none');
  msgEl.textContent = messages.reasons.RATE_DELAY.message;
  submitBtn.disabled = true;

  let remaining = secondsTotal;
  countEl.textContent = messages.login.delayCountdownLive(remaining);
  announce(messages.login.delayWarning(remaining), 'assertive');

  clearInterval(delayCountdownInterval);
  delayCountdownInterval = setInterval(() => {
    remaining--;

    if (remaining <= 0) {
      clearInterval(delayCountdownInterval);
      banner.classList.add('d-none');
      submitBtn.disabled = false;
      announce(messages.login.delayEnded, 'assertive');
      focusTarget(submitBtn);
      return;
    }

    countEl.textContent = messages.login.delayCountdownLive(remaining);

    // Announce to screen reader every 10 seconds only!
    if (remaining % 10 === 0) {
      announce(messages.login.delayCountdownLive(remaining), 'polite');
    }
  }, 1000);
}

/**
 * Render Dynamic MFA Methods Radio Group
 */
function renderMfaMethods(methods = []) {
  const container = document.getElementById('mfa-methods-group');
  container.innerHTML = '';

  const methodConfigs = {
    TOTP: {
      id: 'mfa-totp',
      label: messages.login.methodTotp,
      hint: 'Open Google Authenticator, 1Password, or your auth app.'
    },
    EMAIL_OTP: {
      id: 'mfa-email',
      label: messages.login.methodEmail,
      hint: 'We will send a 6-digit code to your registered email address.'
    },
    BACKUP_CODE: {
      id: 'mfa-backup',
      label: messages.login.methodBackup,
      hint: 'Enter one of the 8-character codes you saved when setting up 2FA.'
    }
  };

  methods.forEach((methodKey, index) => {
    const config = methodConfigs[methodKey];
    if (!config) return;

    const div = document.createElement('div');
    div.className = 'form-check p-3 border rounded';
    div.innerHTML = `
      <input class="form-check-input" type="radio" name="mfaMethod" id="${config.id}" value="${methodKey}" ${index === 0 ? 'checked' : ''}>
      <label class="form-check-label fw-semibold" for="${config.id}">
        ${config.label}
      </label>
      <div class="small text-muted ps-1 mt-1">${config.hint}</div>
    `;
    container.appendChild(div);
  });

  // Watch radio changes to toggle "Send email OTP" button
  const emailBox = document.getElementById('email-otp-action-box');
  function updateMethodUI() {
    const selected = container.querySelector('input[name="mfaMethod"]:checked')?.value;
    if (selected === 'EMAIL_OTP') {
      emailBox.classList.remove('d-none');
    } else {
      emailBox.classList.add('d-none');
    }

    const codeLabel = document.getElementById('mfa-code-label');
    if (selected === 'BACKUP_CODE') {
      codeLabel.textContent = 'Enter 8-character backup code';
    } else {
      codeLabel.textContent = messages.login.codeLabel;
    }
  }

  container.querySelectorAll('input[name="mfaMethod"]').forEach(r => {
    r.addEventListener('change', updateMethodUI);
  });
  updateMethodUI();
}

/**
 * Setup MFA Code Verification Form
 */
function setupMfaForm() {
  const form = document.getElementById('form-mfa');
  const codeInput = document.getElementById('mfa-code');
  const codeError = document.getElementById('mfa-code-error');
  const rememberChk = document.getElementById('remember-device');
  const submitBtn = document.getElementById('btn-submit-mfa');
  const cancelBtn = document.getElementById('btn-cancel-mfa');
  const sendEmailOtpBtn = document.getElementById('btn-send-email-otp');
  const emailOtpStatus = document.getElementById('otp-resend-status');
  const errorSlot = document.getElementById('error-summary-slot');

  // Send Email OTP Button
  if (sendEmailOtpBtn) {
    sendEmailOtpBtn.addEventListener('click', async () => {
      sendEmailOtpBtn.disabled = true;
      sendEmailOtpBtn.textContent = 'Sending code...';
      const res = await api.sendEmailOtp(currentChallenge?.challengeId);
      
      if (res.ok) {
        emailOtpStatus.textContent = messages.login.codeSentSuccess;
        announce(messages.login.codeSentSuccess);
        startOtpCooldown(sendEmailOtpBtn, emailOtpStatus);
      } else {
        emailOtpStatus.textContent = 'Could not send code. Please try again.';
        sendEmailOtpBtn.disabled = false;
        sendEmailOtpBtn.textContent = messages.login.sendEmailCodeBtn;
      }
    });
  }

  // MFA Submit
  form.addEventListener('submit', async (e) => {
    e.preventDefault();
    clearErrorSummary(errorSlot);
    codeInput.classList.remove('is-invalid');
    codeInput.removeAttribute('aria-invalid');
    codeError.classList.add('d-none');

    const code = codeInput.value.trim().replace(/\s+/g, '');
    if (!code) {
      codeInput.classList.add('is-invalid');
      codeInput.setAttribute('aria-invalid', 'true');
      codeError.classList.remove('d-none');
      codeError.textContent = 'Please enter your verification code.';
      showErrorSummary(errorSlot, [{ fieldId: 'mfa-code', message: 'Please enter your verification code.' }]);
      return;
    }

    const method = document.querySelector('input[name="mfaMethod"]:checked')?.value || 'TOTP';
    const rememberDevice = rememberChk.checked;

    submitBtn.disabled = true;
    submitBtn.textContent = 'Verifying code...';

    const res = await api.verifyMfa(currentChallenge?.challengeId, method, code, rememberDevice);
    submitBtn.disabled = false;
    submitBtn.textContent = messages.login.verifyCodeBtn;

    if (res.ok) {
      const data = res.data;
      if (data.status === 'AUTHENTICATED') {
        transitionTo('SUCCESS', data);
      } else if (data.status === 'MFA_REQUIRED' && data.needsEmailApproval) {
        transitionTo('WAITING_EMAIL_APPROVAL', data);
      }
    } else {
      const err = res.error || res.data;

      // Handle SCENARIO 6: CHALLENGE_LOCKED -> Return to Password Step!
      if (err.reasonCode === 'CHALLENGE_LOCKED') {
        transitionTo('CREDENTIALS');
        showErrorSummary(errorSlot, [{
          message: messages.reasons.CHALLENGE_LOCKED.message
        }], messages.reasons.CHALLENGE_LOCKED.title);
        announce(messages.reasons.CHALLENGE_LOCKED.message, 'assertive');
        return;
      }

      // Inline error on code input
      codeInput.classList.add('is-invalid');
      codeInput.setAttribute('aria-invalid', 'true');
      codeError.classList.remove('d-none');
      codeError.textContent = err.message || messages.reasons.OTP_INVALID.message;
      showErrorSummary(errorSlot, [{
        fieldId: 'mfa-code',
        message: err.message || messages.reasons.OTP_INVALID.message
      }], err.title);
      codeInput.focus();
    }
  });

  // Cancel MFA button -> Return to Credentials
  cancelBtn.addEventListener('click', () => {
    transitionTo('CREDENTIALS');
  });
}

/**
 * Cooldown timer for sending Email OTP
 */
function startOtpCooldown(btn, statusEl) {
  let remaining = 60;
  btn.disabled = true;

  clearInterval(emailOtpCooldownInterval);
  emailOtpCooldownInterval = setInterval(() => {
    remaining--;
    if (remaining <= 0) {
      clearInterval(emailOtpCooldownInterval);
      btn.disabled = false;
      btn.textContent = messages.login.sendEmailCodeBtn;
      statusEl.textContent = '';
      return;
    }
    btn.textContent = messages.login.resendCooldown(remaining);
  }, 1000);
}

/**
 * Polling for Email Approval (High Risk Scenario 3)
 */
function startApprovalPolling(challengeId) {
  const statusEl = document.getElementById('approval-polling-status');
  const delayEl = document.getElementById('approval-delay-remaining');

  clearInterval(approvalPollInterval);
  approvalPollInterval = setInterval(async () => {
    if (!challengeId) return;

    try {
      const res = await api.getChallengeStatus(challengeId);
      if (res.ok && res.data) {
        if (res.data.minimumDelayRemainingSeconds > 0) {
          delayEl.classList.remove('d-none');
          delayEl.textContent = `Security delay remaining: ${res.data.minimumDelayRemainingSeconds}s`;
        } else {
          delayEl.classList.add('d-none');
        }

        if (res.data.emailApproved) {
          clearInterval(approvalPollInterval);
          statusEl.textContent = messages.login.approvalReceived;
          announce(messages.login.approvalReceived, 'assertive');
          setTimeout(() => {
            transitionTo('SUCCESS', { user: { displayName: 'Verified User' } });
          }, 1000);
        }
      }
    } catch (_) {}
  }, 3000); // 3 second polling per specification
}

function setupApprovalScreen() {
  const cancelBtn = document.getElementById('btn-cancel-approval');
  if (cancelBtn) {
    cancelBtn.addEventListener('click', () => {
      transitionTo('CREDENTIALS');
    });
  }
}

function clearIntervals() {
  clearInterval(approvalPollInterval);
  clearInterval(delayCountdownInterval);
  clearInterval(emailOtpCooldownInterval);
}
