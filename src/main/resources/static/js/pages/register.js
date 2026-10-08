/**
 * AuthEase - Register Page Controller
 * 
 * WCAG 2.2 AA Compliant
 * Features:
 * - 400ms debounced live password strength check (/api/password/check)
 * - Plain language strength tips ("Try a phrase of 4 random words")
 * - No forced symbol/case rules; allows paste and password managers
 * - Full accessible error summary and inline field errors
 */

import { mountShell, showErrorSummary, clearErrorSummary } from '../ui.js';
import { api } from '../api.js';
import { setStepHeading, announce } from '../a11y.js';
import { messages } from '../messages.en.js';

let debounceTimer = null;

document.addEventListener('DOMContentLoaded', () => {
  mountShell({ activeNav: 'register' });
  initRegisterPage();
});

function initRegisterPage() {
  setStepHeading(messages.register.pageTitle, '#register-heading');

  setupPasswordToggle();
  setupLiveStrengthChecker();
  setupRegistrationForm();
}

function setupPasswordToggle() {
  const toggleBtn = document.getElementById('toggle-password-btn');
  const passwordInput = document.getElementById('password');
  if (!toggleBtn || !passwordInput) return;

  toggleBtn.addEventListener('click', () => {
    const isShowing = passwordInput.type === 'text';
    passwordInput.type = isShowing ? 'password' : 'text';
    toggleBtn.setAttribute('aria-pressed', String(!isShowing));
    toggleBtn.setAttribute('aria-label', isShowing ? messages.login.showPassword : messages.login.hidePassword);

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
 * Debounced (400ms) Live Password Strength Checker
 */
function setupLiveStrengthChecker() {
  const passwordInput = document.getElementById('password');
  const fillBar = document.getElementById('password-strength-fill');
  const strengthBar = document.getElementById('password-strength-bar');
  const hintEl = document.getElementById('password-strength-hint');

  if (!passwordInput || !fillBar || !hintEl) return;

  passwordInput.addEventListener('input', () => {
    clearTimeout(debounceTimer);
    const pwd = passwordInput.value;

    if (!pwd) {
      fillBar.className = 'strength-fill strength-0';
      strengthBar.setAttribute('aria-valuenow', '0');
      hintEl.textContent = messages.register.passwordHint;
      return;
    }

    debounceTimer = setTimeout(async () => {
      try {
        const res = await api.checkPassword(pwd);
        if (res.ok && res.data) {
          const { score = 0, hints = [], breached = false } = res.data;
          
          fillBar.className = `strength-fill strength-${score}`;
          strengthBar.setAttribute('aria-valuenow', String(score));

          if (breached) {
            hintEl.textContent = 'Warning: This password was found in known security leaks. Please choose a different passphrase.';
            hintEl.className = 'small mt-1 text-danger fw-semibold';
            announce('Warning: This password was found in known leaks.', 'polite');
          } else if (hints.length > 0) {
            hintEl.textContent = hints[0];
            hintEl.className = 'small mt-1 text-muted';
          } else {
            hintEl.textContent = 'Strong passphrase!';
            hintEl.className = 'small mt-1 text-success fw-semibold';
          }
        }
      } catch (_) {}
    }, 400); // Debounced 400ms per requirements
  });
}

function setupRegistrationForm() {
  const form = document.getElementById('form-register');
  const nameInput = document.getElementById('display-name');
  const emailInput = document.getElementById('email');
  const passwordInput = document.getElementById('password');
  const nameError = document.getElementById('display-name-error');
  const emailError = document.getElementById('email-error');
  const passwordError = document.getElementById('password-error');
  const errorSlot = document.getElementById('error-summary-slot');
  const submitBtn = document.getElementById('btn-submit-register');

  form.addEventListener('submit', async (e) => {
    e.preventDefault();
    clearErrors();

    const displayName = nameInput.value.trim();
    const email = emailInput.value.trim();
    const password = passwordInput.value;

    const errors = [];
    if (!displayName) {
      errors.push({ fieldId: 'display-name', message: messages.a11y.fieldRequired('display name') });
      showFieldError(nameInput, nameError, messages.a11y.fieldRequired('display name'));
    }

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
    } else if (password.length < 8) {
      errors.push({ fieldId: 'password', message: messages.a11y.passwordTooShort });
      showFieldError(passwordInput, passwordError, messages.a11y.passwordTooShort);
    }

    if (errors.length > 0) {
      showErrorSummary(errorSlot, errors);
      return;
    }

    submitBtn.disabled = true;
    submitBtn.textContent = 'Creating account...';

    const res = await api.register(email, displayName, password);
    submitBtn.disabled = false;
    submitBtn.textContent = messages.register.submitBtn;

    if (res.ok) {
      // Save email for check-email.html display & resend
      sessionStorage.setItem('authease.pending_email', email);
      announce('Account created! Please check your email.', 'assertive');
      window.location.href = `check-email.html?email=${encodeURIComponent(email)}`;
    } else {
      const err = res.error || res.data;
      showErrorSummary(errorSlot, [{ message: err.message || 'Could not create account.' }], err.title);
    }
  });

  function clearErrors() {
    clearErrorSummary(errorSlot);
    nameInput.classList.remove('is-invalid');
    nameInput.removeAttribute('aria-invalid');
    nameError.classList.add('d-none');

    emailInput.classList.remove('is-invalid');
    emailInput.removeAttribute('aria-invalid');
    emailError.classList.add('d-none');

    passwordInput.classList.remove('is-invalid');
    passwordInput.removeAttribute('aria-invalid');
    passwordError.classList.add('d-none');
  }

  function showFieldError(input, errorContainer, message) {
    input.classList.add('is-invalid');
    input.setAttribute('aria-invalid', 'true');
    errorContainer.classList.remove('d-none');
    errorContainer.textContent = message;
  }
}
