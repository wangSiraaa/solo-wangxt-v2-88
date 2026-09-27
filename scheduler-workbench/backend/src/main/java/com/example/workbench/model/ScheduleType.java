package com.example.workbench.model;

public enum ScheduleType {
    /** 固定间隔：每 N 秒触发一次，基于 UTC 锚点，不受时区/夏令时影响 */
    INTERVAL,
    /** 每日定点：在指定时区的某个本地时间触发，受夏令时影响 */
    DAILY
}
