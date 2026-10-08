package com.authease.config;

import org.springframework.context.annotation.Configuration;
import org.springframework.web.servlet.config.annotation.ViewControllerRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

@Configuration
public class WebConfig implements WebMvcConfigurer {

    @Override
    public void addViewControllers(ViewControllerRegistry registry) {
        registry.addViewController("/help").setViewName("forward:/help.html");
        registry.addViewController("/dev-outbox").setViewName("forward:/dev-outbox.html");
        registry.addViewController("/account").setViewName("forward:/account.html");
        registry.addViewController("/recover").setViewName("forward:/recover.html");
        registry.addViewController("/login").setViewName("forward:/login.html");
        registry.addViewController("/register").setViewName("forward:/register.html");
        registry.addViewController("/mfa-setup").setViewName("forward:/mfa-setup.html");
        registry.addViewController("/check-email").setViewName("forward:/check-email.html");
    }
}
