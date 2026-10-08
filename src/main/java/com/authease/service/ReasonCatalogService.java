package com.authease.service;

import org.springframework.stereotype.Service;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

@Service
public class ReasonCatalogService {

    public record ReasonDetail(String reasonCode, String title, String message, String nextStep) {}

    private final Map<String, ReasonDetail> catalog = new ConcurrentHashMap<>();

    public ReasonCatalogService() {
        catalog.put("INVALID_CREDENTIALS", new ReasonDetail(
                "INVALID_CREDENTIALS",
                "Email or password does not match",
                "The email address or password you entered was incorrect. Please check for spelling mistakes and try again.",
                "Check your caps lock key or use the reset password link if you cannot remember your password."
        ));

        catalog.put("RATE_DELAY", new ReasonDetail(
                "RATE_DELAY",
                "Too many tries in a short time",
                "Several sign-in attempts were made recently. We have paused attempts for a short moment to prevent guessing.",
                "Wait for the countdown to reach zero, then try once more."
        ));

        catalog.put("NEW_DEVICE", new ReasonDetail(
                "NEW_DEVICE",
                "Sign in from a new device",
                "You are signing in from a device or browser we have not seen before.",
                "Enter the code from your phone or email to confirm this device is yours."
        ));

        catalog.put("NEW_NETWORK", new ReasonDetail(
                "NEW_NETWORK",
                "New network detected",
                "Your connection is coming from a new network or location.",
                "Confirm with your second factor to finish signing in."
        ));

        catalog.put("MANY_FAILURES", new ReasonDetail(
                "MANY_FAILURES",
                "Multiple incorrect attempts",
                "There were multiple failed attempts before this one.",
                "We require a second step to protect your account."
        ));

        catalog.put("UNUSUAL_HOUR", new ReasonDetail(
                "UNUSUAL_HOUR",
                "Unusual sign-in time",
                "This sign in is happening at an unusual hour compared to your normal habits.",
                "Confirm your second factor to continue safely."
        ));

        catalog.put("OTP_INVALID", new ReasonDetail(
                "OTP_INVALID",
                "Incorrect security code",
                "The 6-digit code entered does not match our records or has expired.",
                "Check the newest code in your app or email and try again."
        ));

        catalog.put("OTP_EXPIRED", new ReasonDetail(
                "OTP_EXPIRED",
                "Code expired",
                "This security code took too long to enter and is no longer valid.",
                "Request a new code using the button below."
        ));

        catalog.put("CHALLENGE_LOCKED", new ReasonDetail(
                "CHALLENGE_LOCKED",
                "Security check locked",
                "The code was entered incorrectly 5 times. To keep your account safe, this session has been cancelled.",
                "Please start over from your email and password."
        ));

        catalog.put("EMAIL_NOT_VERIFIED", new ReasonDetail(
                "EMAIL_NOT_VERIFIED",
                "Email address not yet confirmed",
                "Your account was created, but your email address has not been confirmed yet.",
                "Look in your email inbox for our verification link, or request a new link below."
        ));

        catalog.put("EMAIL_APPROVAL_PENDING", new ReasonDetail(
                "EMAIL_APPROVAL_PENDING",
                "Waiting for your approval",
                "We sent a verification link to your email because this login attempt was marked as high risk.",
                "Open your email, click \"Yes, it was me\", and this page will automatically let you in."
        ));

        catalog.put("EMAIL_APPROVAL_DENIED", new ReasonDetail(
                "EMAIL_APPROVAL_DENIED",
                "Sign in was denied",
                "This sign in was reported as unauthorized.",
                "If you did not do this, change your password immediately."
        ));

        catalog.put("VERIFICATION_SENT", new ReasonDetail(
                "VERIFICATION_SENT",
                "Verification link sent",
                "We sent a fresh confirmation link to your email.",
                "Check your inbox and spam folder."
        ));

        catalog.put("RECOVERY_SENT", new ReasonDetail(
                "RECOVERY_SENT",
                "Recovery email sent",
                "If an account matches that email, instructions have been sent.",
                "Check your email inbox to reset your password."
        ));

        catalog.put("TOKEN_INVALID_OR_EXPIRED", new ReasonDetail(
                "TOKEN_INVALID_OR_EXPIRED",
                "Link has expired or was already used",
                "This security link is no longer valid.",
                "Request a fresh link to continue."
        ));

        catalog.put("SESSION_EXPIRING", new ReasonDetail(
                "SESSION_EXPIRING",
                "Your session will end soon",
                "For your security, you will be signed out in 2 minutes due to inactivity.",
                "Select \"Stay signed in\" if you are still working."
        ));

        catalog.put("MFA_NOT_SET_UP", new ReasonDetail(
                "MFA_NOT_SET_UP",
                "Two-factor authentication recommended",
                "Your account does not have a second factor configured.",
                "Set up an authenticator app to protect your account."
        ));
    }

    public ReasonDetail get(String reasonCode) {
        if (reasonCode == null) {
            return catalog.get("INVALID_CREDENTIALS");
        }
        ReasonDetail detail = catalog.get(reasonCode);
        if (detail == null) {
            return new ReasonDetail(
                    reasonCode,
                    "Security Notice",
                    "We noticed something unusual about this request.",
                    "Please follow the on-screen steps to continue."
            );
        }
        return detail;
    }
}
