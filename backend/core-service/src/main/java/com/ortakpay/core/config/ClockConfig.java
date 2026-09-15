package com.ortakpay.core.config;

import java.time.Clock;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * A single injectable Clock, so time-dependent logic (BalanceReminderService's
 * day-boundary check) can be unit-tested against a fixed instant instead of
 * relying on two independent Instant.now() calls a few microseconds apart
 * ever landing on the exact same value.
 */
@Configuration
public class ClockConfig {

    @Bean
    public Clock clock() {
        return Clock.systemUTC();
    }
}
