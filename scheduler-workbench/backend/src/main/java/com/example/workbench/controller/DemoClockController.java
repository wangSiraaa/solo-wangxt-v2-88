package com.example.workbench.controller;

import com.example.workbench.service.MutableClock;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.web.bind.annotation.*;

import java.time.Duration;
import java.time.Instant;
import java.time.ZoneId;
import java.time.ZonedDateTime;
import java.util.Map;

/**
 * 演示模式：可注入时钟。冻结/拨动“现在”，用于录制闰日、夏令时切换等场景。
 */
@RestController
@RequestMapping("/api/demo/clock")
@CrossOrigin(origins = "*")
@ConditionalOnProperty(name = "demo.clock-enabled", havingValue = "true", matchIfMissing = true)
public class DemoClockController {

    private final MutableClock clock;

    public DemoClockController(MutableClock clock) {
        this.clock = clock;
    }

    @GetMapping
    public Map<String, Object> now() {
        Instant now = clock.instant();
        return Map.of(
                "utc", now.toString(),
                "newYork", ZonedDateTime.ofInstant(now, ZoneId.of("America/New_York")).toString(),
                "berlin", ZonedDateTime.ofInstant(now, ZoneId.of("Europe/Berlin")).toString(),
                "shanghai", ZonedDateTime.ofInstant(now, ZoneId.of("Asia/Shanghai")).toString());
    }

    @PostMapping("/set")
    public Map<String, Object> set(@RequestBody Map<String, String> body) {
        clock.setInstant(Instant.parse(body.get("instant")));
        return now();
    }

    @PostMapping("/advance")
    public Map<String, Object> advance(@RequestBody Map<String, Long> body) {
        clock.advance(Duration.ofSeconds(body.get("seconds")));
        return now();
    }

    @PostMapping("/reset")
    public Map<String, Object> reset() {
        clock.reset();
        return now();
    }
}
