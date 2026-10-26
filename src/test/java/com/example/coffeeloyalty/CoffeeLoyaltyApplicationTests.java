package com.example.coffeeloyalty;

import static org.assertj.core.api.Assertions.assertThat;

import java.time.Clock;
import java.time.ZoneOffset;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;

@SpringBootTest
@ActiveProfiles("test")
class CoffeeLoyaltyApplicationTests {

    private final Clock clock;

    @Autowired
    CoffeeLoyaltyApplicationTests(Clock clock) {
        this.clock = clock;
    }

    @Test
    void providesAClockInUtc() {
        assertThat(clock.getZone()).isEqualTo(ZoneOffset.UTC);
    }
}
