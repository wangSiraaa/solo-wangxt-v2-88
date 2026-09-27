package com.example.cycleworkbench.schedule;

/**
 * Why a generated timeline entry looks the way it does, especially around daylight saving.
 */
public enum DstNote {
    /** Nothing special: exactly one unambiguous wall time. */
    NORMAL,
    /** Spring-forward gap: the nominal local wall time does not exist; Quartz skips the day. */
    GAP_SKIPPED,
    /** Fall-back overlap: the wall time exists twice; Quartz fires once at the earlier offset. */
    OVERLAP_FIRED_ONCE,
    /** The local UTC offset changed between this and the previous entry (23h/25h day). */
    OFFSET_SHIFT,
    /** A fixed-interval entry whose local wall time drifts because the offset changed. */
    INTERVAL_OFFSET_DRIFT
}
