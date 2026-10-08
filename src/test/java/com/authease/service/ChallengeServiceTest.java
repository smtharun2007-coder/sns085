package com.authease.service;

import com.authease.dto.ChallengeStatusResponse;
import com.authease.model.Challenge;
import com.authease.model.RiskLevel;
import com.authease.repository.ChallengeRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;

import java.time.Instant;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

class ChallengeServiceTest {

    private ChallengeRepository challengeRepository;
    private ReasonCatalogService reasonCatalogService;
    private ChallengeService challengeService;

    @BeforeEach
    void setUp() {
        challengeRepository = mock(ChallengeRepository.class);
        reasonCatalogService = new ReasonCatalogService();
        challengeService = new ChallengeService(challengeRepository, reasonCatalogService);
    }

    @Test
    void testCreateChallenge_Medium_NoApprovalToken() {
        ChallengeService.ChallengeCreation creation = challengeService.createChallenge("u1", "sess1", RiskLevel.MEDIUM, 2);
        assertNotNull(creation.challenge());
        assertEquals("sess1", creation.challenge().getBoundSessionId());
        assertEquals(RiskLevel.MEDIUM, creation.challenge().getLevel());
        assertEquals(2, creation.challenge().getRequiredSteps());
        assertNull(creation.rawApprovalToken());
        assertNull(creation.challenge().getMinReleaseAt());
        verify(challengeRepository).save(any(Challenge.class));
    }

    @Test
    void testCreateChallenge_High_HasApprovalTokenAndMinReleaseAt() {
        ChallengeService.ChallengeCreation creation = challengeService.createChallenge("u1", "sess1", RiskLevel.HIGH, 3);
        assertNotNull(creation.challenge());
        assertEquals(RiskLevel.HIGH, creation.challenge().getLevel());
        assertEquals(3, creation.challenge().getRequiredSteps());
        assertNotNull(creation.rawApprovalToken());
        assertNotNull(creation.challenge().getApprovalTokenHash());
        assertNotNull(creation.challenge().getMinReleaseAt());
        verify(challengeRepository).save(any(Challenge.class));
    }

    @Test
    void testFindValidChallenge_WrongSessionReturnsEmpty() {
        when(challengeRepository.findByIdAndBoundSessionId("c1", "other-sess")).thenReturn(Optional.empty());
        Optional<Challenge> opt = challengeService.findValidChallenge("c1", "other-sess");
        assertTrue(opt.isEmpty());
    }

    @Test
    void testRecordWrongCode_FiveTimes_LocksAndDeletesChallenge() {
        Challenge challenge = new Challenge("c1", "u1", "sess1", RiskLevel.MEDIUM, 2, null, Instant.now().plusSeconds(600));
        challenge.setWrongCodeCount(4);

        UserService.CustomAuthException ex = assertThrows(UserService.CustomAuthException.class,
                () -> challengeService.recordWrongCode(challenge));

        assertEquals("CHALLENGE_LOCKED", ex.getReasonCode());
        verify(challengeRepository).delete(challenge);
    }

    @Test
    void testGetStatus_ValidChallenge() {
        Challenge challenge = new Challenge("c1", "u1", "sess1", RiskLevel.HIGH, 3,
                Instant.now().plusSeconds(25), Instant.now().plusSeconds(550));
        challenge.setEmailApproved(true);

        when(challengeRepository.findByIdAndBoundSessionId("c1", "sess1")).thenReturn(Optional.of(challenge));

        ChallengeStatusResponse status = challengeService.getStatus("c1", "sess1");
        assertTrue(status.isEmailApproved());
        assertTrue(status.getExpiresInSeconds() > 500);
        assertTrue(status.getMinimumDelayRemainingSeconds() > 0 && status.getMinimumDelayRemainingSeconds() <= 25);
    }
}
