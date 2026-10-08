/**
 * AuthEase - Sign-In Approval Page Controller
 * 
 * WCAG 2.2 AA Compliant
 * 
 * CRITICAL RULE:
 * This page MUST NEVER change state or submit an approval on load.
 * Only explicit user interaction with the buttons triggers an API call.
 */

import { mountShell } from '../ui.js';
import { api } from '../api.js';
import { setStepHeading, announce } from '../a11y.js';
import { messages } from '../messages.en.js';

document.addEventListener('DOMContentLoaded', () => {
  mountShell({ activeNav: 'approve' });
  initApprovePage();
});

function initApprovePage() {
  const params = new URLSearchParams(window.location.search);
  const token = params.get('token') || 'mock-approval-token';

  const promptState = document.getElementById('approve-state-prompt');
  const approvedState = document.getElementById('approve-state-approved');
  const deniedState = document.getElementById('approve-state-denied');

  const btnApprove = document.getElementById('btn-approve');
  const btnDeny = document.getElementById('btn-deny');

  setStepHeading(messages.approve.pageTitle, '#approve-heading');

  btnApprove.addEventListener('click', async () => {
    btnApprove.disabled = true;
    btnDeny.disabled = true;
    btnApprove.textContent = 'Approving...';

    const res = await api.emailApproval(token, 'APPROVE');
    promptState.classList.add('d-none');
    approvedState.classList.remove('d-none');

    setStepHeading('Sign-in Approved - AuthEase', '#approved-heading');
    announce(messages.approve.approvedNotice, 'assertive');
  });

  btnDeny.addEventListener('click', async () => {
    btnApprove.disabled = true;
    btnDeny.disabled = true;
    btnDeny.textContent = 'Blocking...';

    const res = await api.emailApproval(token, 'DENY');
    promptState.classList.add('d-none');
    deniedState.classList.remove('d-none');

    setStepHeading('Sign-in Blocked - AuthEase', '#denied-heading');
    announce(messages.approve.deniedNotice + ' ' + messages.approve.deniedAdvice, 'assertive');
  });
}
