package com.example.cycleworkbench.schedule;

import com.example.cycleworkbench.task.ScheduleKind;
import java.time.Instant;

/**
 * Immutable description of a recurrence rule.
 *
 * <ul>
 *   <li>{@link ScheduleKind#FIXED_INTERVAL}: {@code intervalSeconds} counts UTC seconds starting
 *       at {@code anchorUtc}. Time zone is irrelevant by construction.</li>
 *   <li>{@link ScheduleKind#DAILY_LOCAL}: fires every day at {@code hour}:{@code minute}
 *       wall-clock time inside {@code zoneId}, starting at {@code anchorUtc}.</li>
 * </ul>
 */
public record ScheduleSpec(
        ScheduleKind kind,
        Long intervalSeconds,
        Integer hour,
        Integer minute,
        String zoneId,
        Instant anchorUtc) {

    public static ScheduleSpec fixedInterval(long intervalSeconds, Instant anchorUtc) {
        if (intervalSeconds <= 0) {
            throw new IllegalArgumentException("intervalSeconds must be positive");
        }
        return new ScheduleSpec(ScheduleKind.FIXED_INTERVAL, intervalSeconds, null, null,
                "UTC", anchorUtc);
    }

    public static ScheduleSpec dailyLocal(int hour, int minute, String zoneId, Instant anchorUtc) {
        if (hour < 0 || hour > 23 || minute < 0 || minute > 59) {
            throw new IllegalArgumentException("hour must be 0-23 and minute 0-59");
        }
        java.time.ZoneId.of(zoneId); // fail fast on bad zone id
        return new ScheduleSpec(ScheduleKind.DAILY_LOCAL, null, hour, minute, zoneId, anchorUtc);
    }
}
