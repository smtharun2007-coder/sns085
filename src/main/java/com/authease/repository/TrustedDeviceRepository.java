package com.authease.repository;

import com.authease.model.TrustedDevice;
import org.springframework.data.mongodb.repository.MongoRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface TrustedDeviceRepository extends MongoRepository<TrustedDevice, String> {
    List<TrustedDevice> findByUserId(String userId);
    Optional<TrustedDevice> findByTokenHash(String tokenHash);
    Optional<TrustedDevice> findByUserIdAndTokenHash(String userId, String tokenHash);
    void deleteByUserId(String userId);
}
