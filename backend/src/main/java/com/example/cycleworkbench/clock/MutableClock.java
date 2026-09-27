package com.example.cycleworkbench.clock;

import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.time.ZoneId;
import java.time.ZoneOffset;

/**
 * Injectable application clock.
 *
 * <p>In live mode it delegates to the system UTC clock. In demo mode it can be pinned to an
 * arbitrary instant (or advanced by a duration), so scheduling calculations and generated
 * trigger instances can be recorded against leap days and DST transitions without waiting.
 */
public class MutableClock extends Clock {

    private volatile boolean demo = false;
    private volatile Clock delegate = Clock.systemUTC();

    public boolean isDemo() {
        return demo;
    }

    public void pinTo(Instant instant) {
        this.delegate = Clock.fixed(instant, ZoneOffset.UTC);
        this.demo = true;
    }

    public void advance(Duration duration) {
        // Clock.offset of a fixed clock keeps the pinned anchor shifted by the sum of advances.
        this.delegate = Clock.offset(delegate, duration);
        this.demo = true;
    }

    public void returnToLive() {
        this.delegate = Clock.systemUTC();
        this.demo = false;
    }

    @Override
    public Instant instant() {
        return delegate.instant();
    }

    @Override
    public ZoneId getZone() {
        return delegate.getZone();
    }

    @Override
    public Clock withZone(ZoneId zone) {
        return delegate.withZone(zone);
    }
}
