package com.authease.security;

import com.authease.model.RateCounter;
import com.authease.repository.RateCounterRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;

import java.time.Instant;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.*;

class RateLimitServiceTest {

    private RateCounterRepository repository;
    private RateLimitService rateLimitService;

    @BeforeEach
    void setUp() {
        repository = mock(RateCounterRepository.class);
        rateLimitService = new RateLimitService(repository);
    }

    @Test
    void testNoFailures_NotDelayed() {
        when(repository.findById(anyString())).thenReturn(Optional.empty());

        RateLimitService.RateLimitStatus status = rateLimitService.checkRateLimit("192.168.1.5", "test@example.com");
        assertFalse(status.isDelayed());
        assertEquals(0, status.getRetryAfterSeconds());
    }

    @Test
    void testActiveDelay_ReturnsDelayedStatus() {
        RateCounter counter = new RateCounter("hash", 3, Instant.now().plusSeconds(10), Instant.now().plusSeconds(900));
        when(repository.findById(anyString())).thenReturn(Optional.of(counter));

        RateLimitService.RateLimitStatus status = rateLimitService.checkRateLimit("192.168.1.5", "test@example.com");
        assertTrue(status.isDelayed());
        assertTrue(status.getRetryAfterSeconds() > 0 && status.getRetryAfterSeconds() <= 10);
    }

    @Test
    void testRecordFailure_ProgressiveDelaysCappedAt60() {
        ArgumentCaptor<RateCounter> captor = ArgumentCaptor.forClass(RateCounter.class);

        // 1st failure: 2^0 = 1s
        when(repository.findById(anyString())).thenReturn(Optional.empty());
        rateLimitService.recordFailure("10.0.0.1", "user@example.com");
        verify(repository, times(1)).save(captor.capture());
        assertEquals(1, captor.getValue().getFailures());

        // 6th failure: min(60, 2^5 = 32s)
        RateCounter counter5 = new RateCounter("hash", 5, Instant.now(), Instant.now().plusSeconds(900));
        when(repository.findById(anyString())).thenReturn(Optional.of(counter5));
        rateLimitService.recordFailure("10.0.0.1", "user@example.com");
        verify(repository, times(2)).save(captor.capture());
        assertEquals(6, captor.getValue().getFailures());

        // 10th failure: min(60, 2^9 = 512) -> capped at 60s
        RateCounter counter9 = new RateCounter("hash", 9, Instant.now(), Instant.now().plusSeconds(900));
        when(repository.findById(anyString())).thenReturn(Optional.of(counter9));
        rateLimitService.recordFailure("10.0.0.1", "user@example.com");
        verify(repository, times(3)).save(captor.capture());
        assertEquals(10, captor.getValue().getFailures());
    }

    @Test
    void testClear_DeletesRateCounter() {
        rateLimitService.clear("127.0.0.1", "test@example.com");
        verify(repository).deleteById(anyString());
    }
}
