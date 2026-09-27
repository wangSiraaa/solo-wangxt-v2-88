package com.example.cycleworkbench.web;

import com.example.cycleworkbench.clock.MutableClock;
import java.time.Duration;
import java.time.Instant;
import java.time.ZoneId;
import java.util.List;
import java.util.Set;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/clock")
public class ClockController {

    private final MutableClock clock;

    public ClockController(MutableClock clock) {
        this.clock = clock;
    }

    @GetMapping
    public ClockView current() {
        Instant now = clock.instant();
        return new ClockView(clock.isDemo(), now, now.atZone(ZoneId.of("UTC")).toLocalDateTime());
    }

    @PostMapping("/demo")
    public ClockView pin(@RequestBody ClockRequest request) {
        if (request.instant() != null) {
            clock.pinTo(request.instant());
        } else if (request.advanceSeconds() != null) {
            clock.advance(Duration.ofSeconds(request.advanceSeconds()));
        } else {
            throw new IllegalArgumentException("需要 instant 或 advanceSeconds");
        }
        return current();
    }

    @PostMapping("/live")
    public ClockView live() {
        clock.returnToLive();
        return current();
    }

    @GetMapping("/zones")
    public List<String> zones() {
        // A compact but representative set covering all the DST cases demonstrated.
        return List.of(
                "UTC",
                "Asia/Shanghai",
                "America/New_York",
                "America/Sao_Paulo",
                "Europe/London",
                "Europe/Berlin",
                "Australia/Sydney",
                "Pacific/Chatham");
    }

    public record ClockView(boolean demo, Instant instant, java.time.LocalDateTime utcTime) {
    }
}
