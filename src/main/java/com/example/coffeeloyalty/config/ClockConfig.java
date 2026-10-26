package com.example.coffeeloyalty.config;

import java.time.Clock;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class ClockConfig {

    // Every date in the application comes from this bean, never from LocalDate.now() (SPECS §21).
    @Bean
    public Clock clock() {
        return Clock.systemUTC();
    }
}
