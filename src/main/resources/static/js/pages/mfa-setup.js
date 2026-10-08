/**
 * AuthEase - MFA TOTP Setup & Backup Codes Controller
 * 
 * WCAG 2.2 AA Compliant
 * Features:
 * - Fetches QR image & manual key from /api/mfa/totp/setup
 * - Verifies first code with /api/mfa/totp/confirm
 * - Renders 10 single-use emergency backup codes
 * - Copy, Download .txt, and Print utilities
 * - Enforces mandatory checkbox confirmation before continuing
 */

import { mountShell, showErrorSummary, clearErrorSummary } from '../ui.js';
import { api } from '../api.js';
import { setStepHeading, announce } from '../a11y.js';
import { messages } from '../messages.en.js';

let generatedBackupCodes = [];

document.addEventListener('DOMContentLoaded', () => {
  mountShell({ activeNav: 'account' });
  initMfaSetupPage();
});

async function initMfaSetupPage() {
  setStepHeading(messages.mfaSetup.pageTitle, '#mfa-setup-heading');

  await loadTotpSetupData();
  setupManualKeyCopy();
  setupCodeConfirmation();
  setupBackupCodeActions();
}

/**
 * Fetch and display QR code & manual secret key
 */
async function loadTotpSetupData() {
  const qrLoading = document.getElementById('qr-loading');
  const qrImage = document.getElementById('qr-image');
  const manualKeyInput = document.getElementById('manual-key-display');
  const errorSlot = document.getElementById('error-summary-slot');

  try {
    const res = await api.setupTotp();
    if (res.ok && res.data) {
      qrLoading.classList.add('d-none');
      qrImage.src = res.data.qrDataUrl;
      qrImage.classList.remove('d-none');
      manualKeyInput.value = res.data.manualKey || 'JBSWY3DPEHPK3PXP';
    } else {
      qrLoading.classList.add('d-none');
      showErrorSummary(errorSlot, [{ message: 'Could not generate two-factor setup data. Please try again.' }]);
    }
  } catch (e) {
    qrLoading.classList.add('d-none');
    showErrorSummary(errorSlot, [{ message: 'Connection error while setting up two-factor authentication.' }]);
  }
}

/**
 * Copy manual key helper
 */
function setupManualKeyCopy() {
  const btn = document.getElementById('btn-copy-manual-key');
  const input = document.getElementById('manual-key-display');
  const feedback = document.getElementById('copy-key-feedback');

  btn.addEventListener('click', async () => {
    try {
      await navigator.clipboard.writeText(input.value);
    } catch (_) {
      input.select();
      document.execCommand('copy');
    }
    feedback.classList.remove('d-none');
    announce('Manual key copied to clipboard.');
    setTimeout(() => feedback.classList.add('d-none'), 3000);
  });
}

/**
 * Verify first TOTP code
 */
function setupCodeConfirmation() {
  const form = document.getElementById('form-confirm-totp');
  const codeInput = document.getElementById('confirm-code');
  const codeError = document.getElementById('confirm-code-error');
  const submitBtn = document.getElementById('btn-submit-confirm');
  const errorSlot = document.getElementById('error-summary-slot');

  form.addEventListener('submit', async (e) => {
    e.preventDefault();
    clearErrorSummary(errorSlot);
    codeInput.classList.remove('is-invalid');
    codeInput.removeAttribute('aria-invalid');
    codeError.classList.add('d-none');

    const code = codeInput.value.trim();
    if (!code || code.length < 6) {
      codeInput.classList.add('is-invalid');
      codeInput.setAttribute('aria-invalid', 'true');
      codeError.classList.remove('d-none');
      codeError.textContent = 'Please enter the 6-digit code shown in your app.';
      showErrorSummary(errorSlot, [{ fieldId: 'confirm-code', message: 'Please enter the 6-digit code.' }]);
      return;
    }

    submitBtn.disabled = true;
    submitBtn.textContent = 'Verifying code...';

    const res = await api.confirmTotp(code);
    submitBtn.disabled = false;
    submitBtn.textContent = messages.mfaSetup.confirmCodeBtn;

    if (res.ok && res.data?.backupCodes) {
      generatedBackupCodes = res.data.backupCodes;
      revealBackupCodesStage(generatedBackupCodes);
    } else {
      const err = res.error || res.data;
      codeInput.classList.add('is-invalid');
      codeInput.setAttribute('aria-invalid', 'true');
      codeError.classList.remove('d-none');
      codeError.textContent = err.message || 'Incorrect security code. Please try again.';
      showErrorSummary(errorSlot, [{ fieldId: 'confirm-code', message: err.message || 'Incorrect code.' }]);
    }
  });
}

/**
 * Transition to Stage 2: Backup Codes
 */
function revealBackupCodesStage(codes) {
  document.getElementById('stage-setup').classList.add('d-none');
  const stageBackup = document.getElementById('stage-backup-codes');
  stageBackup.classList.remove('d-none');

  const listContainer = document.getElementById('backup-codes-list');
  listContainer.innerHTML = '';

  codes.forEach((code, index) => {
    const col = document.createElement('div');
    col.className = 'col-6 col-md-6';
    col.innerHTML = `
      <div class="p-2 border rounded bg-white text-dark d-flex align-items-center justify-content-between">
        <span class="badge bg-secondary me-2">${index + 1}</span>
        <span class="fw-bold tracking-wider">${code}</span>
      </div>
    `;
    listContainer.appendChild(col);
  });

  setStepHeading('Save Emergency Backup Codes - AuthEase', '#mfa-setup-heading');
  announce('Two-factor confirmed! Please save your 10 emergency backup codes.', 'assertive');
}

/**
 * Wire up Copy, Download, Print, and Confirmation Checkbox
 */
function setupBackupCodeActions() {
  const btnCopy = document.getElementById('btn-copy-codes');
  const btnDownload = document.getElementById('btn-download-codes');
  const btnPrint = document.getElementById('btn-print-codes');
  const copyStatus = document.getElementById('copy-codes-status');
  const chkSaved = document.getElementById('chk-saved-codes');
  const btnFinish = document.getElementById('btn-finish-setup');

  // Copy All Codes
  btnCopy.addEventListener('click', async () => {
    const textToCopy = `AuthEase Emergency Backup Codes\n==============================\n` +
      generatedBackupCodes.map((c, i) => `${i + 1}. ${c}`).join('\n') +
      `\n\nEach code can be used once if you lose access to your authenticator app.`;

    try {
      await navigator.clipboard.writeText(textToCopy);
    } catch (_) {
      const textarea = document.createElement('textarea');
      textarea.value = textToCopy;
      document.body.appendChild(textarea);
      textarea.select();
      document.execCommand('copy');
      document.body.removeChild(textarea);
    }

    copyStatus.classList.remove('d-none');
    announce('All 10 backup codes copied to clipboard.');
    setTimeout(() => copyStatus.classList.add('d-none'), 3000);
  });

  // Download .txt
  btnDownload.addEventListener('click', () => {
    const content = `AuthEase Emergency Backup Codes\nGenerated: ${new Date().toLocaleString()}\n==============================\n\n` +
      generatedBackupCodes.map((c, i) => `Code ${i + 1}: ${c}`).join('\n') +
      `\n\nKeep these codes in a safe place. Each code is valid for single use.`;

    const blob = new Blob([content], { type: 'text/plain;charset=utf-8' });
    const url = URL.createObjectURL(blob);
    const a = document.createElement('a');
    a.href = url;
    a.download = 'authease-backup-codes.txt';
    document.body.appendChild(a);
    a.click();
    document.body.removeChild(a);
    URL.revokeObjectURL(url);

    announce('Backup codes downloaded as text file.');
  });

  // Print
  btnPrint.addEventListener('click', () => {
    window.print();
  });

  // Checkbox unlocks Finish button
  chkSaved.addEventListener('change', () => {
    btnFinish.disabled = !chkSaved.checked;
  });

  // Finish Setup
  btnFinish.addEventListener('click', () => {
    announce('Two-factor authentication setup complete!');
    window.location.href = 'account.html?setup_complete=1';
  });
}
