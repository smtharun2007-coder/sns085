/**
 * AuthEase - Speech Synthesis Controller
 * 
 * CRITICAL SECURITY & PRIVACY RULE:
 * Reads ONLY the instructions and public guidance of the current step.
 * NEVER reads values of password, security codes, OTPs, or backup codes.
 */

import { announce } from './a11y.js';
import { messages } from './messages.en.js';

let isSpeaking = false;
let currentUtterance = null;

export function initSpeech() {
  if (!('speechSynthesis' in window)) {
    // Hide read aloud controls if browser doesn't support Web Speech API
    document.querySelectorAll('.read-aloud-btn').forEach(btn => {
      btn.style.display = 'none';
    });
    return;
  }

  // Cancel any speech on page unload
  window.addEventListener('beforeunload', () => {
    stopSpeech();
  });
}

/**
 * Toggle speech synthesis for the current active container
 * @param {HTMLElement|string} containerRef 
 * @param {HTMLButtonElement} triggerBtn 
 */
export function toggleSpeech(containerRef, triggerBtn) {
  if (isSpeaking) {
    stopSpeech(triggerBtn);
  } else {
    speakCurrentStep(containerRef, triggerBtn);
  }
}

/**
 * Stop any current speech synthesis
 * @param {HTMLButtonElement} [triggerBtn] 
 */
export function stopSpeech(triggerBtn) {
  if ('speechSynthesis' in window) {
    window.speechSynthesis.cancel();
  }
  isSpeaking = false;
  currentUtterance = null;
  updateButtonUI(triggerBtn, false);
}

/**
 * Extract safe instruction text and speak it
 * @param {HTMLElement|string} containerRef 
 * @param {HTMLButtonElement} [triggerBtn] 
 */
export function speakCurrentStep(containerRef, triggerBtn) {
  stopSpeech();

  const container = typeof containerRef === 'string' 
    ? document.querySelector(containerRef) 
    : (containerRef || document.querySelector('main'));

  if (!container) return;

  const safeText = extractSafeInstructionText(container);
  if (!safeText) {
    announce('No instructions available to read aloud on this step.');
    return;
  }

  if (!('speechSynthesis' in window)) {
    announce('Text-to-speech is not supported in this browser.');
    return;
  }

  const utterance = new SpeechSynthesisUtterance(safeText);
  currentUtterance = utterance;
  utterance.rate = 0.95; // Slightly slower, very clear pronunciation
  utterance.lang = 'en-US';

  utterance.onstart = () => {
    isSpeaking = true;
    updateButtonUI(triggerBtn, true);
    announce(messages.controls.speakingNow);
  };

  utterance.onend = () => {
    isSpeaking = false;
    updateButtonUI(triggerBtn, false);
  };

  utterance.onerror = (e) => {
    console.warn('SpeechSynthesis error:', e);
    isSpeaking = false;
    updateButtonUI(triggerBtn, false);
  };

  window.speechSynthesis.speak(utterance);
}

/**
 * Collect safe instructions, rigorously filtering out credentials, codes, and inputs
 * @param {HTMLElement} container 
 * @returns {string}
 */
function extractSafeInstructionText(container) {
  const parts = [];

  // Look for headings and instruction paragraphs
  const candidates = container.querySelectorAll(
    'h1, h2, h3, .step-text, .form-label, .form-text, .disclosure-content, .guided-hint, .lead, p:not(.secret-data)'
  );

  candidates.forEach(el => {
    // Exclude any element marked secret, within an input, or inside dev tools
    if (
      el.closest('.secret-data') || 
      el.closest('.no-read-aloud') || 
      el.closest('input') || 
      el.closest('.demo-panel')
    ) {
      return;
    }

    const text = el.textContent.trim();
    // Extra safety: do not read strings that look like 6-digit OTPs or backup codes
    if (/^\d{6}$/.test(text) || /^[A-Z0-9]{4}-[A-Z0-9]{4}/.test(text)) {
      return;
    }

    if (text && !parts.includes(text)) {
      parts.push(text);
    }
  });

  return parts.join('. ');
}

function updateButtonUI(btn, active) {
  if (!btn) {
    btn = document.querySelector('.read-aloud-btn');
  }
  if (!btn) return;

  btn.classList.toggle('speaking', active);
  btn.setAttribute('aria-pressed', active ? 'true' : 'false');
  
  const textSpan = btn.querySelector('.read-aloud-text');
  if (textSpan) {
    textSpan.textContent = active ? messages.controls.stopReadAloud : messages.controls.readAloud;
  }
}
