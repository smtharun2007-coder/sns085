package com.authease.repository;

import com.authease.model.RateCounter;
import org.springframework.data.mongodb.repository.MongoRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface RateCounterRepository extends MongoRepository<RateCounter, String> {
}
