package com.example.cycleworkbench.task;

public enum InstanceState {
    /** A scheduled fire persisted from the preview timeline. */
    SCHEDULED,
    /** The nominal local time falls in a DST spring-forward gap and never exists. */
    SUPPRESSED,
    /** Quartz has fired the instance. */
    FIRED
}
