package com.authease.service;

import com.authease.dto.ChallengeStatusResponse;
import com.authease.model.Challenge;
import com.authease.model.RiskLevel;
import com.authease.repository.ChallengeRepository;
import com.authease.util.CryptoUtil;
import org.springframework.stereotype.Service;

import java.time.Duration;
import java.time.Instant;
import java.util.Optional;

@Service
public class ChallengeService {

    public record ChallengeCreation(Challenge challenge, String rawApprovalToken) {}

    private final ChallengeRepository challengeRepository;
    private final ReasonCatalogService reasonCatalogService;

    public ChallengeService(ChallengeRepository challengeRepository,
                            ReasonCatalogService reasonCatalogService) {
        this.challengeRepository = challengeRepository;
        this.reasonCatalogService = reasonCatalogService;
    }

    public ChallengeCreation createChallenge(String userId, String boundSessionId, RiskLevel level, int requiredSteps) {
        String id = CryptoUtil.generateRandom256BitHex();
        Instant now = Instant.now();
        Instant expiresAt = now.plus(Duration.ofMinutes(10)); // 10 min TTL
        Instant minReleaseAt = (level == RiskLevel.HIGH) ? now.plusSeconds(30) : null;

        Challenge challenge = new Challenge(id, userId, boundSessionId, level, requiredSteps, minReleaseAt, expiresAt);

        String rawApprovalToken = null;
        if (level == RiskLevel.HIGH) {
            rawApprovalToken = CryptoUtil.generateRandomToken(32);
            challenge.setApprovalTokenHash(CryptoUtil.sha256(rawApprovalToken));
        }

        challengeRepository.save(challenge);
        return new ChallengeCreation(challenge, rawApprovalToken);
    }

    public Optional<Challenge> findValidChallenge(String challengeId, String boundSessionId) {
        if (challengeId == null || boundSessionId == null) {
            return Optional.empty();
        }
        Optional<Challenge> optional = challengeRepository.findByIdAndBoundSessionId(challengeId, boundSessionId);
        if (optional.isEmpty()) {
            return Optional.empty();
        }
        Challenge challenge = optional.get();
        if (challenge.getExpiresAt().isBefore(Instant.now())) {
            challengeRepository.delete(challenge);
            return Optional.empty();
        }
        return Optional.of(challenge);
    }

    public void recordWrongCode(Challenge challenge) {
        int wrong = challenge.getWrongCodeCount() + 1;
        challenge.setWrongCodeCount(wrong);

        if (wrong >= 5) {
            challengeRepository.delete(challenge);
            ReasonCatalogService.ReasonDetail detail = reasonCatalogService.get("CHALLENGE_LOCKED");
            throw new UserService.CustomAuthException(detail.reasonCode(), detail.title(), detail.message(), detail.nextStep());
        }

        challengeRepository.save(challenge);
    }

    public ChallengeStatusResponse getStatus(String challengeId, String boundSessionId) {
        Challenge challenge = findValidChallenge(challengeId, boundSessionId)
                .orElseThrow(() -> {
                    ReasonCatalogService.ReasonDetail detail = reasonCatalogService.get("TOKEN_INVALID_OR_EXPIRED");
                    return new UserService.CustomAuthException(detail.reasonCode(), detail.title(), detail.message(), detail.nextStep());
                });

        Instant now = Instant.now();
        long expiresInSeconds = Math.max(0, Duration.between(now, challenge.getExpiresAt()).toSeconds());
        long minDelayRemaining = 0;
        if (challenge.getMinReleaseAt() != null && challenge.getMinReleaseAt().isAfter(now)) {
            minDelayRemaining = Duration.between(now, challenge.getMinReleaseAt()).toSeconds();
        }

        boolean approved = Boolean.TRUE.equals(challenge.getEmailApproved());
        return new ChallengeStatusResponse(approved, expiresInSeconds, minDelayRemaining);
    }
}
