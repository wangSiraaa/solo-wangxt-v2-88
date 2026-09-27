package com.example.cycleworkbench.web;

import com.example.cycleworkbench.task.ScheduleKind;
import java.time.Instant;

public record ScheduleRequest(
        ScheduleKind kind,
        Long intervalSeconds,
        Integer hour,
        Integer minute,
        String zoneId,
        Instant anchorUtc) {

    com.example.cycleworkbench.schedule.ScheduleSpec toSpec() {
        if (anchorUtc == null) {
            throw new IllegalArgumentException("anchorUtc 必填（演示模式下可为任意历史/未来时刻）");
        }
        return switch (kind) {
            case FIXED_INTERVAL -> {
                if (intervalSeconds == null) {
                    throw new IllegalArgumentException("固定间隔模式必须提供 intervalSeconds");
                }
                yield com.example.cycleworkbench.schedule.ScheduleSpec
                        .fixedInterval(intervalSeconds, anchorUtc);
            }
            case DAILY_LOCAL -> {
                if (hour == null || minute == null || zoneId == null) {
                    throw new IllegalArgumentException(
                            "每日本地时间模式必须提供 hour、minute、zoneId");
                }
                yield com.example.cycleworkbench.schedule.ScheduleSpec
                        .dailyLocal(hour, minute, zoneId, anchorUtc);
            }
        };
    }
}
