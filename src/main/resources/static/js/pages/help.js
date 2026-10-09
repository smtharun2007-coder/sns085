/**
 * AuthEase - Help & Assist Page Controller
 * Interactive error explainer and guided mode triggers.
 */

import { mountShell } from '../ui.js';
import { api } from '../api.js';
import { setStepHeading, announce, isGuidedMode, setGuidedMode } from '../a11y.js';

document.addEventListener('DOMContentLoaded', () => {
  mountShell({ activeNav: 'help' });
  initHelpPage();
});

function initHelpPage() {
  setStepHeading('Help & Guidance', '#help-heading');

  const btn = document.getElementById('btn-ask-assist');
  const input = document.getElementById('assist-input');

  if (btn && input) {
    btn.addEventListener('click', () => {
      const code = input.value.trim().toUpperCase() || 'EMAIL_NOT_VERIFIED';
      explainCode(code);
    });

    input.addEventListener('keydown', (e) => {
      if (e.key === 'Enter') {
        e.preventDefault();
        const code = input.value.trim().toUpperCase() || 'EMAIL_NOT_VERIFIED';
        explainCode(code);
      }
    });
  }

  // Quick-click pills
  document.querySelectorAll('.btn-pill').forEach(pill => {
    pill.addEventListener('click', () => {
      const code = pill.getAttribute('data-code');
      if (input) input.value = code;
      explainCode(code);
    });
  });

  // Guided Mode Toggle Button on Help page
  const toggleGuidedBtn = document.getElementById('btn-toggle-guided-help');
  if (toggleGuidedBtn) {
    toggleGuidedBtn.addEventListener('click', () => {
      const current = isGuidedMode();
      setGuidedMode(!current, true);
      toggleGuidedBtn.textContent = !current ? 'Guided Mode is ON (Click to turn off)' : 'Toggle Guided Mode Now';
    });
  }

  // Pre-load default explanation
  explainCode('EMAIL_NOT_VERIFIED');
}

async function explainCode(code) {
  const result = document.getElementById('assist-result');
  const title = document.getElementById('assist-title');
  const explanation = document.getElementById('assist-explanation');
  const nextstep = document.getElementById('assist-nextstep');
  const btn = document.getElementById('btn-ask-assist');

  if (btn) {
    btn.disabled = true;
    btn.textContent = 'Explaining...';
  }

  try {
    const res = await api.explain(code, true);
    if (btn) {
      btn.disabled = false;
      btn.textContent = 'Explain Simply';
    }

    if (res.ok && res.data) {
      result.classList.remove('d-none');
      title.textContent = res.data.title || code;
      explanation.textContent = res.data.text || res.data.message || res.data.plainEnglish || 'No description found.';
      const sourceLabel = res.data.source === 'LLM' ? '🤖 Simplified by Google Gemini AI' : '📖 Plain-Language Guide Catalog';
      nextstep.innerHTML = `${escapeHtml(res.data.nextStep || 'Follow the on-screen instructions.')} <span class="badge bg-secondary ms-2">${sourceLabel}</span>`;
      announce(`Explanation for ${code} loaded.`);
    } else {
      result.classList.remove('d-none');
      title.textContent = code;
      explanation.textContent = 'This is a security check code. Please refer to the common errors section above.';
      nextstep.textContent = 'Check your connection or start over from the sign in page.';
    }
  } catch (err) {
    if (btn) {
      btn.disabled = false;
      btn.textContent = 'Explain Simply';
    }
    result.classList.remove('d-none');
    title.textContent = code;
    explanation.textContent = 'Could not contact the assistant service.';
    nextstep.textContent = 'Please check the common errors section above.';
  }
}

function escapeHtml(text) {
  if (!text) return '';
  return String(text)
    .replace(/&/g, '&amp;')
    .replace(/</g, '&lt;')
    .replace(/>/g, '&gt;')
    .replace(/"/g, '&quot;')
    .replace(/'/g, '&#039;');
}

