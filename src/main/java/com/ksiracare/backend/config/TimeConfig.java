package com.ksiracare.backend.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.time.Clock;

@Configuration
public class TimeConfig {

    /** Injected wherever "now" matters, so time-based rules can be tested with a fixed clock. */
    @Bean
    public Clock clock() {
        return Clock.systemUTC();
    }
}
