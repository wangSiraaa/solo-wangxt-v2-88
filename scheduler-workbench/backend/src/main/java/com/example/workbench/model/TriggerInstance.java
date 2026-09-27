package com.example.workbench.model;

import jakarta.persistence.*;

import java.time.Instant;
import java.time.LocalDateTime;
import java.util.UUID;

/**
 * 已生成的触发实例。SKIPPED 实例的 fireTimeUtc 为 null（不会真正触发）。
 */
@Entity
@Table(name = "trigger_instance")
public class TriggerInstance {

    @Id
    private UUID id;

    @Column(nullable = false)
    private UUID taskId;

    @Column(nullable = false)
    private int seq;

    /** 实际触发时刻（UTC）。SKIPPED 时为 null。 */
    private Instant fireTimeUtc;

    /** 计划触发的本地时间（任务时区下的挂钟时间） */
    @Column(nullable = false)
    private LocalDateTime localDateTime;

    /** 实际触发时本地偏移，如 -04:00；SKIPPED 为 null */
    private String zoneOffset;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private InstanceStatus status;

    /** 人类可读的处理原因（夏令时跳过/重复说明） */
    private String reason;

    /** 实际被 Quartz 触发执行的时刻；未触发为 null */
    private Instant firedAt;

    protected TriggerInstance() {
    }

    public TriggerInstance(UUID id, UUID taskId, int seq, Instant fireTimeUtc,
                           LocalDateTime localDateTime, String zoneOffset,
                           InstanceStatus status, String reason) {
        this.id = id;
        this.taskId = taskId;
        this.seq = seq;
        this.fireTimeUtc = fireTimeUtc;
        this.localDateTime = localDateTime;
        this.zoneOffset = zoneOffset;
        this.status = status;
        this.reason = reason;
    }

    public UUID getId() { return id; }
    public UUID getTaskId() { return taskId; }
    public int getSeq() { return seq; }
    public Instant getFireTimeUtc() { return fireTimeUtc; }
    public LocalDateTime getLocalDateTime() { return localDateTime; }
    public String getZoneOffset() { return zoneOffset; }
    public InstanceStatus getStatus() { return status; }
    public String getReason() { return reason; }
    public Instant getFiredAt() { return firedAt; }
    public void setFiredAt(Instant firedAt) { this.firedAt = firedAt; }
}
