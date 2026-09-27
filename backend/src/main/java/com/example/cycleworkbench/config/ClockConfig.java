package com.example.cycleworkbench.config;

import com.example.cycleworkbench.clock.MutableClock;
import java.time.Clock;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class ClockConfig {

    @Bean
    public MutableClock mutableClock() {
        return new MutableClock();
    }

    @Bean
    public Clock applicationClock(MutableClock mutableClock) {
        return mutableClock;
    }
}
