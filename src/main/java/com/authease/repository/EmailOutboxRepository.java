package com.authease.repository;

import com.authease.model.EmailOutbox;
import org.springframework.data.mongodb.repository.MongoRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface EmailOutboxRepository extends MongoRepository<EmailOutbox, String> {
    List<EmailOutbox> findByDemoSessionIdOrderByTsDesc(String demoSessionId);
    List<EmailOutbox> findByToIgnoreCaseOrderByTsDesc(String to);
}
