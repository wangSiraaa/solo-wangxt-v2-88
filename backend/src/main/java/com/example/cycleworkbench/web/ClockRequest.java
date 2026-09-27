package com.example.cycleworkbench.web;

import java.time.Instant;

public record ClockRequest(
        /** Pin the demo clock to this instant. */
        Instant instant,
        /** Alternatively, advance the current clock by this many seconds. */
        Long advanceSeconds) {
}
