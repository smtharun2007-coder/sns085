package com.authease.repository;

import com.authease.model.EmailToken;
import com.authease.model.TokenType;
import org.springframework.data.mongodb.repository.MongoRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface EmailTokenRepository extends MongoRepository<EmailToken, String> {
    Optional<EmailToken> findByTokenHashAndType(String tokenHash, TokenType type);
}
