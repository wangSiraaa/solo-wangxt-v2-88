package com.example.workbench.model;

import jakarta.persistence.*;

import java.time.Instant;
import java.time.LocalTime;
import java.util.UUID;

@Entity
@Table(name = "task_definition")
public class TaskDefinition {

    @Id
    private UUID id;

    @Column(nullable = false)
    private String name;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private ScheduleType scheduleType;

    /** INTERVAL 类型：间隔秒数 */
    private Long intervalSeconds;

    /** DAILY 类型：本地时间，如 02:30 */
    private LocalTime dailyTime;

    /** DAILY 类型：IANA 时区，如 America/New_York；INTERVAL 类型仅用于展示 */
    @Column(nullable = false)
    private String timezone;

    @Column(nullable = false)
    private boolean paused = false;

    @Column(nullable = false)
    private Instant createdAt;

    protected TaskDefinition() {
    }

    public TaskDefinition(UUID id, String name, ScheduleType scheduleType, Long intervalSeconds,
                          LocalTime dailyTime, String timezone, Instant createdAt) {
        this.id = id;
        this.name = name;
        this.scheduleType = scheduleType;
        this.intervalSeconds = intervalSeconds;
        this.dailyTime = dailyTime;
        this.timezone = timezone;
        this.createdAt = createdAt;
    }

    public UUID getId() { return id; }
    public String getName() { return name; }
    public void setName(String name) { this.name = name; }
    public ScheduleType getScheduleType() { return scheduleType; }
    public Long getIntervalSeconds() { return intervalSeconds; }
    public void setIntervalSeconds(Long intervalSeconds) { this.intervalSeconds = intervalSeconds; }
    public LocalTime getDailyTime() { return dailyTime; }
    public void setDailyTime(LocalTime dailyTime) { this.dailyTime = dailyTime; }
    public String getTimezone() { return timezone; }
    public void setTimezone(String timezone) { this.timezone = timezone; }
    public boolean isPaused() { return paused; }
    public void setPaused(boolean paused) { this.paused = paused; }
    public Instant getCreatedAt() { return createdAt; }
}
