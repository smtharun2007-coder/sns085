# AuthEase - Security & Threat Model

AuthEase was engineered for the hackathon challenge **"Secure & Accessible Digital Authentication"**. Its core security architecture is designed to prove that strong security and intuitive accessibility are not mutually exclusive.

This document details the complete threat model, threat vectors, mitigations implemented in code, and automated integration verifications.

---

## 1. Threat Matrix & Implemented Mitigations

| Threat Vector | Severity | Mitigated in Layer | Primary Mitigation Strategy |
| :--- | :---: | :--- | :--- |
| **Credential Stuffing & Breached Password Reuse** | Critical | `PasswordPolicyService`, `Argon2SecurityUtil` | NIST SP 800-63B policy + HIBP top breached password blacklist + Argon2id ($m=19456, t=2, p=1$). |
| **Brute Force & Dictionary Attacks** | High | `RateLimitService`, `ChallengeService` | Composite key rate limiting `(client_ip, email)` + exponential delay backoff ($1s \to 60s$) + 5-failure challenge destroy lock. |
| **Timing Analysis & User Enumeration** | High | `Argon2SecurityUtil`, `RecoveryService` | Dummy Argon2 hash computation for non-existent users ensures constant-time response ($<50\text{ms}$ variance); uniform recovery responses. |
| **Session Hijacking & Fixation** | High | `SecurityConfig`, `AuthService` | Session rotation on login (`migrateSession`), double-submit CSRF cookie token, strict session binding for challenges. |
| **Token & Code Replay Attacks** | High | `RecoveryService`, `MfaService` | Single-use recovery tokens, single-use email OTPs, TOTP time-step deduplication (`lastUsedTotpTimeStep`), single-use backup code consumption. |
| **Anti-Virus / Prefetch Link Execution** | Medium | `AuthController`, `SecurityConfig` | High-risk email approvals require state-modifying `POST /api/auth/email-approval`. `GET` requests return `405 Method Not Allowed`. |
| **Token Leakage & Exposure at Rest** | High | `CryptoUtil`, `Mongo Models` | Tokens and backup codes stored exclusively as SHA-256 hashes. TOTP secrets encrypted with AES-256-GCM. |
| **MFA Circumvention** | Critical | `RecoveryService`, `AdminController` | Recovery flow mandates TOTP/backup code for enrolled accounts; Admin portal mandates active TOTP (`ADMIN_MFA_REQUIRED`). |
| **Assistive Technology & AI Privacy Leakage** | Critical | `AssistService` | Zero PII policy: Only sanitized Reason Catalog static text is sent to LLM endpoints. 3-second hard timeout fallback. |
| **Resource Exhaustion & DoS** | Medium | `Mongo Indexes`, `Dockerfile` | TTL auto-purging Mongo indexes; JVM memory capping (`-Xmx350m`) for resource-constrained free tiers. |

---

## 2. In-Depth Mitigation Architecture

### 2.1 Credential Protection & Hashing
- **Argon2id**: Uses the memory-hard Argon2id variant implemented via BouncyCastle (`bcprov-jdk18on`). Parameters ($m=19456\text{ KiB}, t=2\text{ iterations}, p=1\text{ thread}$) prevent GPU and ASIC cracking.
- **NIST SP 800-63B Compliance**: 
  - Passwords between 8 and 128 characters permitted.
  - Allows spaces and any printable characters.
  - Rejects arbitrary complexity rules (e.g., forcing special characters) that weaken real-world human password choices.
  - Checks candidate passwords against an in-memory k-anonymity HaveIBeenPwned blacklist of common compromised passwords.

### 2.2 Constant-Time Defense & Timing Parity
- **Enumeration Defense**: When evaluating invalid passwords for an email that does not exist in the database, `Argon2SecurityUtil.verifyAgainstDummyHash()` executes Argon2 verification against a valid precomputed dummy hash.
- **Automated Verification**: Verified in [`AuthEaseIntegrationTest.testConstantTimeTimingParity`](file:///c:/xampp/htdocs/sns/sns085/src/test/java/com/authease/integration/AuthEaseIntegrationTest.java) to ensure response durations for existing vs. non-existing accounts remain within a tight 50ms tolerance band.

### 2.3 Progressive Rate Limiting & Challenge Locking
- **Key Hash**: Derived from `SHA256(ip + "|" + normalizedEmail)`.
- **Progressive Delay Curve**: Delay begins on failure and increases exponentially:
  $$\text{Delay} = \min(60, 2^{n - 1})\text{ seconds}$$
  - 1 failure: 1s
  - 2 failures: 2s
  - 3 failures: 4s
  - 4 failures: 8s
  - 5 failures: 16s
  - 6 failures: 32s
  - $\ge 7$ failures: capped at 60s
- **Challenge Locking**: Challenges allow at most 5 attempts. The 5th incorrect code permanently destroys the challenge and transitions the session to `CHALLENGE_LOCKED`, requiring the user to start over from credentials.

### 2.4 Multi-Factor Authentication & Device Trust
- **TOTP**: Standard RFC 6238 TOTP using SAM Stevens library with a $\pm 1$ time-step window. The server records `lastUsedTotpTimeStep` to reject replayed codes within the same 30-second window.
- **Emergency Backup Codes**: Generated as eight 8-character codes (`XXXX-XXXX`). Codes are SHA-256 hashed before storage; upon successful use, the specific hash is permanently deleted from the user document.
- **Device Trust**: Stored in a cryptographically random cookie (`ae_device`) mapped to a 30-day TTL `TrustedDevice` record. A device is recognized as trusted only if the hash matches and has not expired. Revocation is available from the Security Center (`DELETE /api/account/devices/{id}`).

### 2.5 Defense-in-Depth Admin Controls
- **Dual Guard**: Accessing administrator routes (`/api/admin/**`) requires:
  1. An authenticated session with `ROLE_ADMIN`.
  2. Active two-factor authentication (`user.isTotpEnabled() == true`).
- If an admin account has disabled TOTP, requests are halted with HTTP 403 and reason code `ADMIN_MFA_REQUIRED`.

### 2.6 Privacy-Preserving AI Explanations
- **Zero-PII Isolation**: The assistive explainer (`POST /api/assist/explain`) translates technical reason codes into simplified copy.
- To prevent prompt injection and data leakage, the backend **never** passes user inputs, emails, IP addresses, hashes, or passwords to LLM APIs. Only static template copy from the Reason Catalog is provided.
- If external AI connectivity fails or exceeds the 3-second timeout, the system falls back to Grade 6–8 Reason Catalog text with source `"TEMPLATE"`.

### 2.7 Security Headers & CSRF Protection
- **Content-Security-Policy (CSP)**: `default-src 'self'; script-src 'self'; style-src 'self' 'unsafe-inline'; img-src 'self' data:; connect-src 'self'; font-src 'self'; frame-ancestors 'none';`
- **X-Frame-Options**: `DENY` (clickjacking defense).
- **X-Content-Type-Options**: `nosniff` (MIME sniffing defense).
- **Referrer-Policy**: `strict-origin-when-cross-origin`.
- **HSTS**: `max-age=31536000; includeSubDomains`.
- **CSRF Token**: Stored in standard non-HttpOnly cookie `XSRF-TOKEN`, required in `X-XSRF-TOKEN` header for all state-changing requests.

---

## 3. Automated Security Test Verification

The above protections are continuously verified by our automated test suite:
- [`AuthEaseIntegrationTest`](file:///c:/xampp/htdocs/sns/sns085/src/test/java/com/authease/integration/AuthEaseIntegrationTest.java):
  - `testSingleUseRecoveryToken`: Single-use recovery token guarantees.
  - `testConstantTimeTimingParity`: Bounded variance timing parity test.
  - `testProgressiveRateLimitDelayIncrements`: Exponential backoff rate limiter.
  - `testBackupCodeSingleUseConsumption`: Single-use backup code consumption.
  - `testAdminEndpointRejectedIfAdminLacksTotp`: Admin TOTP defense-in-depth enforcement.
  - `testUnverifiedUserCannotLogin`: Verification status enforcement.
  - `testFiveWrongChallengeCodesLockChallenge`: 5-failure challenge auto-destruction.
  - `testApprovalConsumesViaPostOnly`: POST-only email approval verification.
- [`CsrfAndHttpSecurityIntegrationTest`](file:///c:/xampp/htdocs/sns/sns085/src/test/java/com/authease/integration/CsrfAndHttpSecurityIntegrationTest.java):
  - `testPostWithoutCsrfTokenIsRejected`: 403 Forbidden without CSRF token.
  - `testPostWithCsrfTokenPassesCsrfCheck`: 200 OK with valid CSRF token.
  - `testEmailApprovalRejectsGetRequest`: 405 Method Not Allowed for GET email approvals.
  - `testEmailApprovalAcceptsPostRequest`: 200 OK for POST email approvals.
