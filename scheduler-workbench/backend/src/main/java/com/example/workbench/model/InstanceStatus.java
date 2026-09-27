package com.example.workbench.model;

public enum InstanceStatus {
    /** 正常触发 */
    NORMAL,
    /** 本地时间落入夏令时“空洞”（时钟拨快），该次不触发 */
    SKIPPED,
    /** 本地时间落入夏令时“重叠”（时钟回拨），该次会触发两次中的第一次 */
    REPEATED_FIRST,
    /** 重叠中的第二次触发 */
    REPEATED_SECOND
}
