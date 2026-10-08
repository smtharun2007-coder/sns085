# AuthEase - Hackathon Judge Demo Walkthrough Script

This script provides step-by-step instructions with both **Web UI clicks** and **raw `curl` commands** to demonstrate all hackathon rubric requirements for **AuthEase** ("Secure & Accessible Digital Authentication").

---

## Prerequisites

1. Start AuthEase using Docker Compose or bare-metal:
   ```bash
   # Via Docker Compose (starts MongoDB and AuthEase together)
   docker compose up --build

   # OR via Bare-Metal (with MongoDB on localhost:27017)
   mvnw.cmd spring-boot:run
   ```
2. Open your browser at: `http://localhost:8080`

---

## Demonstration Scenarios

### Scenario 1: Accessible Registration & Email Verification

**Goal:** Demonstrate NIST SP 800-63B password guidance, live strength checking, outbox inspection, and token verification.

1. **Browser Steps:**
   - Navigate to `http://localhost:8080/register.html`.
   - Enter Email: `judge@example.com`, Name: `Judge Evaluator`.
   - Start typing a common weak password (e.g. `password123`).
   - Observe live feedback: The password checker announces: *"This password has appeared in previous data breaches."*
   - Change password to a strong passphrase: `Correct-Horse-Battery-2026!#`.
   - Observe the live NIST criteria checklist checking off length, passphrase suitability, and breach freedom.
   - Click **Create Account**.
   - The UI redirects to `check-email.html`.
   - Click the floating **Open Dev Outbox** button or navigate to `http://localhost:8080/dev-outbox.html`.
   - Locate the verification email for `judge@example.com` and click the verification link.
   - The UI confirms the account is verified and directs to login.

2. **Curl Equivalent:**
   ```bash
   # Check password strength
   curl -s -X POST http://localhost:8080/api/password/check \
     -H "Content-Type: application/json" \
     -d '{"password":"Correct-Horse-Battery-2026!#"}'

   # Fetch CSRF token
   curl -s -c cookies.txt http://localhost:8080/api/auth/me > /dev/null
   CSRF=$(grep XSRF-TOKEN cookies.txt | awk '{print $7}')

   # Register user
   curl -s -b cookies.txt -c cookies.txt -X POST http://localhost:8080/api/auth/register \
     -H "Content-Type: application/json" \
     -H "X-XSRF-TOKEN: $CSRF" \
     -d '{"email":"judge@example.com","displayName":"Judge Evaluator","password":"Correct-Horse-Battery-2026!#"}'

   # Inspect Dev Outbox
   curl -s http://localhost:8080/api/dev/outbox
   ```

---

### Scenario 2: Baseline Sign-In & Device Trust (Low Risk)

**Goal:** Demonstrate seamless single-factor authentication for recognized low-risk sign-ins.

1. **Browser Steps:**
   - Go to `http://localhost:8080/login.html`.
   - Sign in with `judge@example.com` and `Correct-Horse-Battery-2026!#`.
   - Check **Remember this device for 30 days**.
   - Notice immediate authentication (`status: "AUTHENTICATED"`). No friction, no puzzle, no delay.
   - View your Security Dashboard (`index.html` or `/api/account/security`). The browser is recorded as a trusted device.

---

### Scenario 3: Adaptive Medium-Risk Challenge & "Why" Disclosure

**Goal:** Demonstrate risk elevation on unfamiliar context, the Grade 6–8 Reason Catalog, accessible "Why" disclosures, and MFA options.

1. **Browser Steps:**
   - On `http://localhost:8080/login.html`, click the floating **Demo Simulation** button (gear icon) on the right edge.
   - Toggle **New device (Triggers Medium MFA)** to **ON**.
   - Enter your credentials and click **Sign in**.
   - **Observe:**
     - The page transitions smoothly to Step 2 with risk badge: **Medium Risk Check**.
     - An accessible disclosure titled **"Why am I being asked this?"** appears.
     - Click the disclosure or use your screen reader: It reads in clear Grade 6–8 language:
       > *"Sign in from a new device: You are signing in from a device or browser we have not seen before. Enter the code from your phone or email to confirm this device is yours."*
     - Click the **Read Aloud** button in the top navigation: The screen instructions are read via Web Speech synthesis.
     - Click **Send code via email**.
     - Check `dev-outbox.html`, copy the 6-digit OTP, enter it, and complete sign in.

---

### Scenario 4: High-Risk Detection & Email Approval

**Goal:** Demonstrate elevated high-risk policy requiring email authorization and preventing automated scanner exploitation.

1. **Browser Steps:**
   - Open the **Demo Simulation** panel on the login page.
   - Toggle **Unusual hour (Triggers High MFA)** to **ON**.
   - Submit login credentials.
   - **Observe:**
     - Risk badge updates to **High Risk Security Hold**.
     - The UI indicates that an approval request has been sent to your registered email.
     - A 30-second security countdown displays with `aria-live` updates.
     - Check `dev-outbox.html`: Open the email and click **Yes, approve this sign-in**.
     - The sign-in page polling recognizes the approval and instantly signs you in!

---

### Scenario 5: Progressive Rate Limiting & Timing Parity

**Goal:** Demonstrate brute-force rate delay curves and constant-time execution against enumeration.

1. **Browser Steps:**
   - Attempt to log in with an incorrect password 3 times in a row.
   - **Observe:**
     - On the 3rd attempt, the submit button is disabled.
     - An accessible countdown timer starts (e.g. 5 seconds) announcing remaining seconds via `aria-live`.
     - Attempting again doubles the delay progressively.
   - Notice that entering a non-existent email versus an existing email with an incorrect password exhibits identical response latencies due to Argon2 dummy hash computation ($<50\text{ms}$ variance).

2. **Curl Equivalent (Progressive Delay):**
   ```bash
   # Rapidly fail 3 attempts:
   for i in {1..3}; do
     curl -s -b cookies.txt -c cookies.txt -X POST http://localhost:8080/api/auth/login \
       -H "Content-Type: application/json" \
       -H "X-XSRF-TOKEN: $CSRF" \
       -d '{"email":"judge@example.com","password":"wrong"}' | grep -o '"status":"[^"]*"'
   done
   # 3rd attempt returns: "status":"DELAYED"
   ```

---

### Scenario 6: Challenge Locking (5 Failed MFA Attempts)

**Goal:** Prevent brute-forcing 6-digit OTPs by permanently destroying the challenge session after 5 mistakes.

1. **Browser Steps:**
   - Trigger an MFA challenge (using the Demo drawer toggle).
   - Enter an incorrect 6-digit code `000000` 4 times.
   - Notice the countdown: *"4 attempts remaining"*, *"3 remaining"*, *"2 remaining"*, *"1 remaining"*.
   - On the 5th attempt, the challenge is destroyed:
     > *"Security check locked: The code was entered incorrectly 5 times. To keep your account safe, this session has been cancelled. Please start over from your email and password."*

---

### Scenario 7: Two-Factor Account Recovery (MFA Non-Bypass)

**Goal:** Demonstrate password recovery that respects MFA enrollment and destroys the recovery token after a single use.

1. **Browser Steps:**
   - Set up an Authenticator App on `http://localhost:8080/mfa-setup.html`.
   - Log out and click **Forgot password?** on the login page.
   - Enter your email and click **Send Recovery Link**.
   - In `dev-outbox.html`, click the password reset link.
   - **Observe:** Because TOTP was enabled on your account, Step 2 requires your 6-digit authenticator code or emergency backup code before allowing password reset!
   - Reset the password.
   - Refresh the reset link in your browser: The token is rejected as `TOKEN_INVALID_OR_EXPIRED`.

---

### Scenario 8: Admin Dashboard & Defense-in-Depth

**Goal:** Demonstrate metrics calculation and the rule that administrators MUST have TOTP enabled to access the admin portal.

1. **Curl Verification:**
   ```bash
   # Admin account without TOTP enabled receives 403:
   curl -s -b admin-cookie.txt http://localhost:8080/api/admin/metrics
   # Response:
   # {"reasonCode":"ADMIN_MFA_REQUIRED","title":"Two-factor authentication required for administrators",...}

   # Admin account with TOTP enabled receives aggregated metrics:
   curl -s -b mfa-admin-cookie.txt http://localhost:8080/api/admin/metrics?range=24h
   # Response includes:
   # firstTrySuccessRate, mfaDropOffRate, recoveryCompletionRate, avgRecoverySeconds, loginsByLevel, failuresPerHour
   ```

---

### Scenario 9: Assistive AI Explainer & Privacy Rule

**Goal:** Demonstrate on-demand AI copy simplification with zero-PII exposure.

1. **Curl Test:**
   ```bash
   curl -s -X POST http://localhost:8080/api/assist/explain \
     -H "Content-Type: application/json" \
     -d '{"reasonCode":"RATE_DELAY","simplify":true}'
   ```
   **Response:**
   ```json
   {
     "text": "Too many tries in a short time. Several sign-in attempts were made recently. We have paused attempts for a short moment to prevent guessing. Wait for the countdown to reach zero, then try once more.",
     "source": "TEMPLATE"
   }
   ```
   *Verified: Only the template text is processed; never emails, passwords, IPs, or tokens.*

---

### Scenario 10: Accessibility Features

1. **Keyboard-Only Navigation:** Press `Tab` on any page to see the high-visibility focus ring and the **Skip to main content** link.
2. **Text Resizing:** Click `A-`, `A`, or `A+` in the header toolbar to scale typography dynamically.
3. **Theme Modes:** Toggle between **Light**, **Dark**, and **High Contrast (HC)** modes with WCAG AAA compliant contrast ratios.
4. **Screen Reader Live Regions:** All countdowns, error summaries, and status updates are wired to `aria-live="polite"` and `role="alert"`.
