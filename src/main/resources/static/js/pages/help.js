/**
 * AuthEase - Help & Assist Page Controller
 */

import { mountShell } from '../ui.js';
import { api } from '../api.js';
import { setStepHeading, announce } from '../a11y.js';

document.addEventListener('DOMContentLoaded', () => {
  mountShell({ activeNav: 'help' });
  initHelpPage();
});

function initHelpPage() {
  setStepHeading('Help & Guidance', '#help-heading');

  const btn = document.getElementById('btn-ask-assist');
  const input = document.getElementById('assist-input');
  const result = document.getElementById('assist-result');
  const title = document.getElementById('assist-title');
  const explanation = document.getElementById('assist-explanation');
  const nextstep = document.getElementById('assist-nextstep');

  if (btn && input) {
    btn.addEventListener('click', async () => {
      const code = input.value.trim().toUpperCase() || 'MFA_REQUIRED';
      btn.disabled = true;
      btn.textContent = 'Explaining...';

      const res = await api.explain(code, true);
      btn.disabled = false;
      btn.textContent = 'Explain Simply';

      if (res.ok && res.data) {
        result.classList.remove('d-none');
        title.textContent = res.data.title || code;
        explanation.textContent = res.data.message || res.data.plainEnglish || 'No explanation found.';
        nextstep.textContent = res.data.nextStep || 'Follow the on-screen instructions.';
        announce('Explanation loaded.');
      } else {
        result.classList.remove('d-none');
        title.textContent = code;
        explanation.textContent = 'Could not find a specific guide for this code, but our team is ready to help.';
        nextstep.textContent = 'Check your connection or try again.';
      }
    });
  }
}
