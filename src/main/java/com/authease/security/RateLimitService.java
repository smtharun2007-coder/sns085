package com.authease.security;

import com.authease.model.RateCounter;
import com.authease.repository.RateCounterRepository;
import com.authease.util.CryptoUtil;
import org.springframework.stereotype.Service;

import java.time.Duration;
import java.time.Instant;
import java.util.Optional;

@Service
public class RateLimitService {

    private final RateCounterRepository rateCounterRepository;

    public RateLimitService(RateCounterRepository rateCounterRepository) {
        this.rateCounterRepository = rateCounterRepository;
    }

    public static class RateLimitStatus {
        private final boolean delayed;
        private final long retryAfterSeconds;
        private final int failures;

        public RateLimitStatus(boolean delayed, long retryAfterSeconds, int failures) {
            this.delayed = delayed;
            this.retryAfterSeconds = retryAfterSeconds;
            this.failures = failures;
        }

        public boolean isDelayed() {
            return delayed;
        }

        public long getRetryAfterSeconds() {
            return retryAfterSeconds;
        }

        public int getFailures() {
            return failures;
        }
    }

    public RateLimitStatus checkRateLimit(String ip, String identifier) {
        String keyHash = generateKeyHash(ip, identifier);
        Instant now = Instant.now();

        try {
            Optional<RateCounter> optional = rateCounterRepository.findById(keyHash);
            if (optional.isPresent()) {
                RateCounter counter = optional.get();
                if (counter.getNextAllowedAt() != null && counter.getNextAllowedAt().isAfter(now)) {
                    long seconds = Math.max(1, Duration.between(now, counter.getNextAllowedAt()).toSeconds());
                    return new RateLimitStatus(true, seconds, counter.getFailures());
                }
                return new RateLimitStatus(false, 0, counter.getFailures());
            }
        } catch (Exception ignored) {}

        return new RateLimitStatus(false, 0, 0);
    }

    public void recordFailure(String ip, String identifier) {
        String keyHash = generateKeyHash(ip, identifier);
        Instant now = Instant.now();

        try {
            RateCounter counter = rateCounterRepository.findById(keyHash)
                    .orElse(new RateCounter(keyHash, 0, null, now.plus(Duration.ofMinutes(15))));

            int newFailures = counter.getFailures() + 1;
            counter.setFailures(newFailures);

            // Progressive delay: 1, 2, 4, 8, 16, 32, capped at 60 s
            long delaySeconds = Math.min(60, (long) Math.pow(2, Math.max(0, newFailures - 1)));
            counter.setNextAllowedAt(now.plusSeconds(delaySeconds));
            counter.setExpiresAt(now.plus(Duration.ofMinutes(15)));

            rateCounterRepository.save(counter);
        } catch (Exception ignored) {}
    }

    public void clear(String ip, String identifier) {
        String keyHash = generateKeyHash(ip, identifier);
        try {
            rateCounterRepository.deleteById(keyHash);
        } catch (Exception ignored) {}
    }

    public int getRecentFailures(String ip, String identifier) {
        String keyHash = generateKeyHash(ip, identifier);
        try {
            return rateCounterRepository.findById(keyHash)
                    .map(RateCounter::getFailures)
                    .orElse(0);
        } catch (Exception ignored) {
            return 0;
        }
    }

    private String generateKeyHash(String ip, String identifier) {
        String normalizedId = identifier != null ? identifier.trim().toLowerCase() : "unknown";
        String normalizedIp = ip != null ? ip.trim() : "127.0.0.1";
        return CryptoUtil.sha256(normalizedIp + ":" + normalizedId);
    }
}
