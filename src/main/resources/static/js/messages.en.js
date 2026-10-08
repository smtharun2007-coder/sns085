/**
 * AuthEase - User-Facing Plain Language Messages (English)
 * Reading level: Grade 6 to 8. Short, clear sentences. No technical jargon.
 */
export const messages = {
  app: {
    name: 'AuthEase',
    tagline: 'Security that understands you.',
    description: 'Sign in simply and safely. We adapt to how you use your device and protect what matters.',
    skipToContent: 'Skip to main content',
    help: 'Help and guides',
    account: 'Security center',
    logIn: 'Log in',
    createAccount: 'Create account',
    logOut: 'Log out',
    footerText: 'AuthEase is built for everyone. No puzzles. No passwords you cannot paste. Clear security.',
  },

  controls: {
    themeLabel: 'Theme',
    themeLight: 'Light theme',
    themeDark: 'Dark theme',
    themeHighContrast: 'High contrast theme',
    textSizeLabel: 'Text size',
    textSmaller: 'Smaller text',
    textNormal: 'Normal text',
    textLarger: 'Larger text',
    guidedMode: 'Guided mode',
    guidedModeHelp: 'Show one step at a time with extra tips',
    readAloud: 'Read this page aloud',
    stopReadAloud: 'Stop reading aloud',
    speakingNow: 'Reading text aloud now...',
  },

  steps: {
    stepOf: (current, total) => `Step ${current} of ${total}`,
    credentialsTitle: 'Enter your account details',
    mfaTitle: 'Confirm your identity',
    approvalTitle: 'Check your email for approval',
    successTitle: 'You are signed in',
  },

  login: {
    pageTitle: 'Log in - AuthEase',
    heading: 'Sign in to your account',
    emailLabel: 'Email address',
    emailPlaceholder: 'name@example.com',
    passwordLabel: 'Password',
    passwordPlaceholder: 'Enter your password',
    showPassword: 'Show password text',
    hidePassword: 'Hide password text',
    forgotPassword: 'Forgot your password?',
    submitCredentials: 'Continue',
    mfaHeading: 'Additional security check',
    chooseMethod: 'Choose how to receive or enter your code:',
    methodTotp: 'Authenticator app (code on your phone)',
    methodEmail: 'Email code (sent to your inbox)',
    methodBackup: 'Backup code (one of your saved emergency codes)',
    codeLabel: 'Enter the 6-digit security code',
    codePlaceholder: 'e.g. 123456',
    codeHint: 'Paste is allowed. Type numbers without spaces.',
    sendEmailCodeBtn: 'Send me a new code',
    codeSentSuccess: 'A fresh code was sent to your email.',
    resendCooldown: (seconds) => `You can request another code in ${seconds}s`,
    rememberDeviceLabel: 'Remember this device for 30 days',
    rememberDeviceHelp: 'Only check this if this is your private computer or phone.',
    verifyCodeBtn: 'Verify and sign in',
    cancelLogin: 'Back to email and password',
    whyDisclosureTitle: 'Why am I being asked this?',
    whyDefaultExplain: 'We noticed something new about this sign-in attempt, such as a different browser or network. We want to be sure it is really you.',
    waitingApprovalHeading: 'Approval needed from your email',
    waitingApprovalNotice: 'We emailed you a security link. Open it, select "Yes, it was me", then return to this tab.',
    waitingApprovalPolling: 'Checking for your confirmation...',
    approvalReceived: 'Approval received! Signing you in now...',
    delayWarning: (seconds) => `Too many attempts. For your safety, please wait ${seconds} seconds before trying again.`,
    delayCountdownLive: (seconds) => `${seconds} seconds remaining before you can try again.`,
    delayEnded: 'You can now try signing in again.',
  },

  reasons: {
    INVALID_CREDENTIALS: {
      title: 'Email or password does not match',
      message: 'The email address or password you entered was incorrect. Please check for spelling mistakes and try again.',
      nextStep: 'Check your caps lock key or use the reset password link if you cannot remember your password.'
    },
    RATE_DELAY: {
      title: 'Too many tries in a short time',
      message: 'Several sign-in attempts were made recently. We have paused attempts for a short moment to prevent guessing.',
      nextStep: 'Wait for the countdown to reach zero, then try once more.'
    },
    NEW_DEVICE: {
      title: 'Sign in from a new device',
      message: 'You are signing in from a device or browser we have not seen before.',
      nextStep: 'Enter the code from your phone or email to confirm this device is yours.'
    },
    NEW_NETWORK: {
      title: 'New network detected',
      message: 'Your connection is coming from a new network or location.',
      nextStep: 'Confirm with your second factor to finish signing in.'
    },
    MANY_FAILURES: {
      title: 'Multiple incorrect attempts',
      message: 'There were multiple failed attempts before this one.',
      nextStep: 'We require a second step to protect your account.'
    },
    UNUSUAL_HOUR: {
      title: 'Unusual sign-in time',
      message: 'This sign in is happening at an unusual hour compared to your normal habits.',
      nextStep: 'Confirm your second factor to continue safely.'
    },
    OTP_INVALID: {
      title: 'Incorrect security code',
      message: 'The 6-digit code entered does not match our records or has expired.',
      nextStep: 'Check the newest code in your app or email and try again.'
    },
    OTP_EXPIRED: {
      title: 'Code expired',
      message: 'This security code took too long to enter and is no longer valid.',
      nextStep: 'Request a new code using the button below.'
    },
    CHALLENGE_LOCKED: {
      title: 'Security check locked',
      message: 'The code was entered incorrectly 5 times. To keep your account safe, this session has been cancelled.',
      nextStep: 'Please start over from your email and password.'
    },
    EMAIL_NOT_VERIFIED: {
      title: 'Email address not yet confirmed',
      message: 'Your account was created, but your email address has not been confirmed yet.',
      nextStep: 'Look in your email inbox for our verification link, or request a new link below.'
    },
    EMAIL_APPROVAL_PENDING: {
      title: 'Waiting for your approval',
      message: 'We sent a verification link to your email because this login attempt was marked as high risk.',
      nextStep: 'Open your email, click "Yes, it was me", and this page will automatically let you in.'
    },
    EMAIL_APPROVAL_DENIED: {
      title: 'Sign in was denied',
      message: 'This sign in was reported as unauthorized.',
      nextStep: 'If you did not do this, change your password immediately.'
    },
    VERIFICATION_SENT: {
      title: 'Verification link sent',
      message: 'We sent a fresh confirmation link to your email.',
      nextStep: 'Check your inbox and spam folder.'
    },
    RECOVERY_SENT: {
      title: 'Recovery email sent',
      message: 'If an account matches that email, instructions have been sent.',
      nextStep: 'Check your email inbox to reset your password.'
    },
    TOKEN_INVALID_OR_EXPIRED: {
      title: 'Link has expired or was already used',
      message: 'This security link is no longer valid.',
      nextStep: 'Request a fresh link to continue.'
    },
    SESSION_EXPIRING: {
      title: 'Your session will end soon',
      message: 'For your security, you will be signed out in 2 minutes due to inactivity.',
      nextStep: 'Select "Stay signed in" if you are still working.'
    },
    MFA_NOT_SET_UP: {
      title: 'Two-factor authentication recommended',
      message: 'Your account does not have a second factor configured.',
      nextStep: 'Set up an authenticator app to protect your account.'
    }
  },

  demo: {
    panelTitle: 'Demo Simulation Controls',
    disclaimer: 'Simulation for the demo. Not part of the real product.',
    newDevice: 'Simulate new device (triggers Medium MFA)',
    newNetwork: 'Simulate new network',
    unusualHour: 'Simulate unusual hour',
    forceFailures: 'Force 3 failed attempts (triggers Rate Delay)',
    presetHeader: 'Quick Test Scenarios',
    presetTrusted: 'Scenario 1: Trusted device (Instant sign-in)',
    presetNewDevice: 'Scenario 2: New device (MFA code 123456)',
    presetSuspicious: 'Scenario 3: High risk (Email approval needed)',
    presetWrongPassword: 'Scenario 4: Wrong password (Clear error)',
    presetDelay: 'Scenario 5: Too many attempts (Live countdown)',
    presetLock: 'Scenario 6: 5 wrong OTPs (Challenge locked)',
    openOutbox: 'View Simulated Dev Outbox (Emails & Codes)',
  },

  a11y: {
    errorSummaryTitle: 'There is a problem',
    fieldRequired: (field) => `Please enter your ${field}.`,
    invalidEmail: 'Please enter a valid email address with an @ symbol.',
    passwordTooShort: 'Password must have at least 8 characters.',
  },

  register: {
    pageTitle: 'Create account - AuthEase',
    heading: 'Create your AuthEase account',
    subheading: 'No complicated rules or puzzles. Choose a phrase that is easy for you to remember.',
    displayNameLabel: 'Display name',
    displayNamePlaceholder: 'e.g. Alex Taylor',
    emailLabel: 'Email address',
    emailPlaceholder: 'name@example.com',
    passwordLabel: 'Create a password',
    passwordPlaceholder: 'Enter a strong phrase or password',
    passwordHint: 'Tip: A phrase of 3 or 4 random words is easy to remember and very hard to guess.',
    submitBtn: 'Create account',
    alreadyHaveAccount: 'Already have an account?',
    logInLink: 'Sign in here'
  },

  checkEmail: {
    pageTitle: 'Check your email - AuthEase',
    heading: 'Check your email',
    message: 'We sent an activation link to your email inbox. Click the link in that email to confirm your account.',
    resendBtn: 'Resend confirmation email',
    resendCooldown: (seconds) => `You can request another email in ${seconds}s`,
    sentNotice: 'A fresh confirmation link has been sent to your inbox.',
    outboxHint: 'Testing with demo simulation? Check the simulated emails in the Dev Outbox below.'
  },

  verifyEmail: {
    pageTitle: 'Email Verification - AuthEase',
    verifyingHeading: 'Confirming your email address...',
    successHeading: 'Email verified successfully!',
    successMessage: 'Your email address is now confirmed. You can sign in to your new account.',
    logInBtn: 'Continue to Sign In',
    errorHeading: 'Unable to verify email',
    errorMessage: 'This verification link is invalid, has expired, or was already used.',
    resendPrompt: 'Enter your email address to receive a fresh verification link:'
  },

  approve: {
    pageTitle: 'Sign-in Approval - AuthEase',
    heading: 'Is this sign-in attempt yours?',
    explanation: 'We detected a sign-in attempt with your email from an unusual network or time. Please confirm whether it was you.',
    approveBtn: 'Yes, it was me',
    denyBtn: 'No, this was not me',
    approvedNotice: 'Thank you for confirming. Your sign-in was approved. You can return to your original browser tab now.',
    deniedNotice: 'Sign-in blocked. Because you did not make this attempt, someone else may have entered your password.',
    deniedAdvice: 'We strongly recommend resetting your password immediately to protect your account.',
    resetPasswordBtn: 'Reset your password now',
    returnHomeBtn: 'Return to home'
  },

  mfaSetup: {
    pageTitle: 'Set up Two-Factor Authentication - AuthEase',
    heading: 'Two-Factor Authentication Setup',
    subheading: 'Add an extra shield of security to your account with an authenticator app.',
    step1Title: 'Step 1: Scan the QR code',
    step1Instruction: 'Open Google Authenticator, Microsoft Authenticator, 1Password, or any TOTP app and scan this image.',
    manualKeyLabel: 'Manual secret key (if you cannot scan QR):',
    copyManualKey: 'Copy key',
    step2Title: 'Step 2: Enter the 6-digit code from your app',
    step2Instruction: 'Enter the code currently displayed in your authenticator app to make sure it is linked correctly.',
    confirmCodeBtn: 'Verify and reveal backup codes',
    step3Title: 'Step 3: Save your emergency backup codes',
    step3Instruction: 'Save these 10 one-time emergency codes. If you ever lose your phone or authenticator app, these codes are the only way to sign in.',
    copyAllCodes: 'Copy all backup codes',
    downloadCodes: 'Download codes as .txt file',
    printCodes: 'Print backup codes',
    savedConfirmation: 'I have safely saved these 10 backup codes',
    finishBtn: 'Complete setup',
    skipBtn: 'Skip for now'
  }
};
