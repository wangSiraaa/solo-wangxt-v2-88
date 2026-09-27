package com.example.cycleworkbench.schedule;

import com.example.cycleworkbench.task.InstanceState;
import java.time.Instant;
import java.time.LocalDateTime;
import java.time.ZoneOffset;

/**
 * One row of the future-fire timeline.
 *
 * <p>{@code localTime}/{@code utcOffset} describe where the (nominal or real) instant falls on
 * the wall clock of the rule's zone. {@code actualUtc} is null for a suppressed gap entry
 * because that instant never exists.
 */
public record TimelineItem(
        int ordinal,
        InstanceState state,
        DstNote note,
        /** Nominal wall-clock time requested by the rule (e.g. 02:30 on the gap day). */
        LocalDateTime nominalLocal,
        /** What Quartz actually produced; null for suppressed gaps. */
        Instant actualUtc,
        LocalDateTime actualLocal,
        ZoneOffset utcOffset,
        String explanation,
        /** UTC gap from the previous produced (non-suppressed) entry, in seconds; null for first. */
        Long gapSecondsFromPrevious) {
}
