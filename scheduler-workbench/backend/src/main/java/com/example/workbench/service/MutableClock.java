package com.example.workbench.service;

import org.springframework.stereotype.Component;

import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.time.ZoneId;

/**
 * 可注入、可变的时钟。生产代码一律通过它获取“现在”，
 * 演示模式下可通过 /api/demo/clock 冻结或拨动时间。
 */
@Component
public class MutableClock extends Clock {

    private Clock delegate = Clock.systemUTC();

    @Override
    public ZoneId getZone() {
        return delegate.getZone();
    }

    @Override
    public Clock withZone(ZoneId zone) {
        return delegate.withZone(zone);
    }

    @Override
    public Instant instant() {
        return delegate.instant();
    }

    public synchronized void setInstant(Instant instant) {
        this.delegate = Clock.fixed(instant, ZoneId.of("UTC"));
    }

    public synchronized void advance(Duration duration) {
        setInstant(instant().plus(duration));
    }

    public synchronized void reset() {
        this.delegate = Clock.systemUTC();
    }
}
