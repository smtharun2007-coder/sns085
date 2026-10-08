package com.authease.repository;

import com.authease.model.LoginEvent;
import com.authease.model.LoginEventType;
import com.authease.model.RiskLevel;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.mongodb.repository.MongoRepository;
import org.springframework.stereotype.Repository;

import java.time.Instant;
import java.util.List;
import java.util.Optional;

@Repository
public interface LoginEventRepository extends MongoRepository<LoginEvent, String> {
    List<LoginEvent> findByUserIdOrNullOrderByTsDesc(String userId);
    List<LoginEvent> findByIdentifierHashAndTsAfter(String identifierHash, Instant after);
    long countByIdentifierHashAndTypeAndTsAfter(String identifierHash, LoginEventType type, Instant after);
    long countByTypeAndTsAfter(LoginEventType type, Instant after);
    List<LoginEvent> findByTsAfter(Instant after);
    Optional<LoginEvent> findFirstByUserIdOrNullOrderByTsDesc(String userId);
    Page<LoginEvent> findByTypeAndLevel(LoginEventType type, RiskLevel level, Pageable pageable);
    Page<LoginEvent> findByType(LoginEventType type, Pageable pageable);
    Page<LoginEvent> findByLevel(RiskLevel level, Pageable pageable);
}
