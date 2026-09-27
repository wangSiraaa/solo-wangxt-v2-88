package com.example.cycleworkbench.task;

/**
 * The two fundamentally different recurrence rules.
 *
 * <p>{@code FIXED_INTERVAL} anchors every fire to the previous UTC instant ("every 24 hours"),
 * while {@code DAILY_LOCAL} anchors every fire to a wall-clock time in a zone ("every day at
 * 09:00 local"). They are not the same rule across a DST boundary.
 */
public enum ScheduleKind {
    FIXED_INTERVAL,
    DAILY_LOCAL
}
