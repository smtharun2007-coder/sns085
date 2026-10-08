package com.authease.repository;

import com.authease.model.Challenge;
import org.springframework.data.mongodb.repository.MongoRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface ChallengeRepository extends MongoRepository<Challenge, String> {
    Optional<Challenge> findByIdAndBoundSessionId(String id, String boundSessionId);
    Optional<Challenge> findByApprovalTokenHash(String approvalTokenHash);
}
