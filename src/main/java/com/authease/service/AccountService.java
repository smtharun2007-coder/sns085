package com.authease.service;

import com.authease.dto.AccountSecurityResponse;
import com.authease.dto.LastDecisionResponse;
import com.authease.model.LoginEvent;
import com.authease.model.TrustedDevice;
import com.authease.model.User;
import com.authease.repository.LoginEventRepository;
import com.authease.repository.TrustedDeviceRepository;
import com.authease.repository.UserRepository;
import com.authease.util.CryptoUtil;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

@Service
public class AccountService {

    private final UserRepository userRepository;
    private final TrustedDeviceRepository trustedDeviceRepository;
    private final LoginEventRepository loginEventRepository;

    public AccountService(UserRepository userRepository,
                          TrustedDeviceRepository trustedDeviceRepository,
                          LoginEventRepository loginEventRepository) {
        this.userRepository = userRepository;
        this.trustedDeviceRepository = trustedDeviceRepository;
        this.loginEventRepository = loginEventRepository;
    }

    public AccountSecurityResponse getSecurityInfo(String userId, String currentDeviceCookie) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new UserService.CustomAuthException("UNAUTHORIZED", "Sign in required", "Please sign in.", "Sign in first."));

        List<TrustedDevice> devices = trustedDeviceRepository.findByUserId(userId);
        List<AccountSecurityResponse.DeviceInfo> deviceInfos = new ArrayList<>();

        String currentTokenHash = (currentDeviceCookie != null && !currentDeviceCookie.isBlank())
                ? CryptoUtil.sha256(currentDeviceCookie)
                : null;

        for (TrustedDevice dev : devices) {
            boolean isCurrent = currentTokenHash != null && currentTokenHash.equals(dev.getTokenHash());
            deviceInfos.add(new AccountSecurityResponse.DeviceInfo(
                    dev.getId(),
                    dev.getLabel(),
                    dev.getLastUsedAt(),
                    isCurrent
            ));
        }

        List<LoginEvent> events = loginEventRepository.findByUserIdOrderByTsDesc(userId);
        if (events.size() > 10) {
            events = events.subList(0, 10);
        }

        return new AccountSecurityResponse(user.isTotpEnabled(), deviceInfos, events);
    }

    public void deleteDevice(String userId, String deviceId) {
        Optional<TrustedDevice> opt = trustedDeviceRepository.findById(deviceId);
        if (opt.isPresent()) {
            TrustedDevice dev = opt.get();
            if (userId.equals(dev.getUserId())) {
                trustedDeviceRepository.delete(dev);
            }
        }
    }

    public LastDecisionResponse getLastDecision(String userId) {
        Optional<LoginEvent> opt = loginEventRepository.findFirstByUserIdOrderByTsDesc(userId);
        if (opt.isEmpty()) {
            return new LastDecisionResponse(10, "LOW", List.of(
                    new LastDecisionResponse.SignalDetail("BASELINE", "Default Baseline Score", 10)
            ));
        }

        LoginEvent event = opt.get();
        List<LastDecisionResponse.SignalDetail> signals = new ArrayList<>();
        if (event.getSignals() != null) {
            for (String sig : event.getSignals()) {
                int points = switch (sig) {
                    case "NEW_DEVICE" -> 25;
                    case "NEW_NETWORK" -> 20;
                    case "MANY_FAILURES" -> 25;
                    case "RECENT_FAILURES", "UNUSUAL_HOUR" -> 10;
                    case "TRUSTED_DEVICE" -> -20;
                    case "RECENT_SUCCESS" -> -10;
                    default -> 0;
                };
                signals.add(new LastDecisionResponse.SignalDetail(sig, sig.replace("_", " "), points));
            }
        }

        int score = event.getScore() != null ? event.getScore() : 10;
        String level = event.getLevel() != null ? event.getLevel().name() : "LOW";

        return new LastDecisionResponse(score, level, signals);
    }
}
