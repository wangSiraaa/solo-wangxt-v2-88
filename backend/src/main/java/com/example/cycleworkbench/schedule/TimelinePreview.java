package com.example.cycleworkbench.schedule;

import java.time.Instant;
import java.util.List;

public record TimelinePreview(
        ScheduleSpec schedule,
        Instant generatedAt,
        Instant fromUtc,
        List<TimelineItem> items,
        String ruleSummary) {
}
