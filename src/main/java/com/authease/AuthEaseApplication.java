package com.authease;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.data.mongodb.config.EnableMongoAuditing;

@SpringBootApplication
public class AuthEaseApplication {

    public static void main(String[] args) {
        SpringApplication.run(AuthEaseApplication.class, args);
    }
}
