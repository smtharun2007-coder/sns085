package com.authease.service;

import com.authease.config.AppProperties;
import com.authease.model.*;
import com.authease.repository.UserRepository;
import com.authease.security.Argon2SecurityUtil;
import com.authease.util.CryptoUtil;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.core.env.Environment;
import org.springframework.data.domain.Sort;
import org.springframework.data.mongodb.core.MongoTemplate;
import org.springframework.data.mongodb.core.index.Index;
import org.springframework.stereotype.Component;

import java.time.Duration;
import java.time.Instant;
import java.util.Arrays;
import java.util.Set;

@Component
public class StartupInitializer implements ApplicationRunner {

    private static final Logger log = LoggerFactory.getLogger(StartupInitializer.class);

    private final AppProperties appProperties;
    private final Environment environment;
    private final MongoTemplate mongoTemplate;
    private final UserRepository userRepository;
    private final Argon2SecurityUtil passwordEncoder;

    public StartupInitializer(AppProperties appProperties,
                              Environment environment,
                              MongoTemplate mongoTemplate,
                              UserRepository userRepository,
                              Argon2SecurityUtil passwordEncoder) {
        this.appProperties = appProperties;
        this.environment = environment;
        this.mongoTemplate = mongoTemplate;
        this.userRepository = userRepository;
        this.passwordEncoder = passwordEncoder;
    }

    @Override
    public void run(ApplicationArguments args) {
        validateEnvironment();
        ensureIndexes();
        if (appProperties.isDemoMode()) {
            seedDemoUsers();
        }
    }

    private void validateEnvironment() {
        boolean isProduction = Arrays.stream(environment.getActiveProfiles())
                .anyMatch(p -> p.equalsIgnoreCase("prod") || p.equalsIgnoreCase("production"));

        if (appProperties.isDemoMode() && isProduction) {
            throw new IllegalStateException("FATAL: AuthEase demo mode cannot be active in a production profile!");
        }

        if (appProperties.isDemoMode()) {
            log.warn("********************************************************************************");
            log.warn("   WARNING: AUTHEASE DEMO MODE IS ENABLED! NOT FOR PRODUCTION USE!             ");
            log.warn("********************************************************************************");
        }
    }

    private void ensureIndexes() {
        try {
            // users: unique email
            mongoTemplate.indexOps(User.class).ensureIndex(
                    new Index().on("email", Sort.Direction.ASC).unique()
            );

            // trusted_devices: TTL on expiresAt
            mongoTemplate.indexOps(TrustedDevice.class).ensureIndex(
                    new Index().on("expiresAt", Sort.Direction.ASC).expire(Duration.ZERO)
            );

            // challenges: TTL on expiresAt
            mongoTemplate.indexOps(Challenge.class).ensureIndex(
                    new Index().on("expiresAt", Sort.Direction.ASC).expire(Duration.ZERO)
            );

            // email_tokens: TTL on expiresAt
            mongoTemplate.indexOps(EmailToken.class).ensureIndex(
                    new Index().on("expiresAt", Sort.Direction.ASC).expire(Duration.ZERO)
            );

            // rate_counters: TTL on expiresAt
            mongoTemplate.indexOps(RateCounter.class).ensureIndex(
                    new Index().on("expiresAt", Sort.Direction.ASC).expire(Duration.ZERO)
            );

            log.info("MongoDB collections and TTL indexes verified successfully.");
        } catch (Exception e) {
            log.warn("Index creation skipped or failed (MongoDB may not be connected yet): {}", e.getMessage());
        }
    }

    private void seedDemoUsers() {
        try {
            // Seed Demo User
            String userEmail = "user@authease.demo";
            String userPass = "DemoUser@Pass123";
            if (!userRepository.existsByEmail(userEmail)) {
                User demoUser = new User(userEmail, "Demo User", passwordEncoder.encode(userPass));
                demoUser.setEmailVerified(true);
                demoUser.setRoles(Set.of("USER"));
                userRepository.save(demoUser);
                log.info("--------------------------------------------------");
                log.info("SEED DEMO USER CREATED:");
                log.info("Email:    {}", userEmail);
                log.info("Password: {}", userPass);
                log.info("--------------------------------------------------");
            }

            // Seed Demo Admin
            String adminEmail = "admin@authease.demo";
            String adminPass = "DemoAdmin@Pass123";
            if (!userRepository.existsByEmail(adminEmail)) {
                User demoAdmin = new User(adminEmail, "Demo Admin", passwordEncoder.encode(adminPass));
                demoAdmin.setEmailVerified(true);
                demoAdmin.setRoles(Set.of("USER", "ADMIN"));
                demoAdmin.setTotpEnabled(true);
                demoAdmin.setTotpSecretEncrypted(CryptoUtil.encryptAesGcm("JBSWY3DPEHPK3PXP", appProperties.getEncKey()));
                userRepository.save(demoAdmin);
                log.info("--------------------------------------------------");
                log.info("SEED DEMO ADMIN CREATED (TOTP enabled):");
                log.info("Email:    {}", adminEmail);
                log.info("Password: {}", adminPass);
                log.info("TOTP Secret (Base32): JBSWY3DPEHPK3PXP");
                log.info("--------------------------------------------------");
            }
        } catch (Exception e) {
            log.warn("Demo user seeding skipped (database may not be connected yet): {}", e.getMessage());
        }
    }
}
