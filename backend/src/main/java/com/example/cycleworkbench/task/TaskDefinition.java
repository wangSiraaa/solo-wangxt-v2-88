package com.example.cycleworkbench.task;

import com.example.cycleworkbench.schedule.ScheduleSpec;
import java.time.Instant;

public class TaskDefinition {

    private Long id;
    private String name;
    private ScheduleSpec schedule;
    private TaskState state = TaskState.ACTIVE;
    private Instant createdAt;
    private Instant updatedAt;

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public ScheduleSpec getSchedule() {
        return schedule;
    }

    public void setSchedule(ScheduleSpec schedule) {
        this.schedule = schedule;
    }

    public TaskState getState() {
        return state;
    }

    public void setState(TaskState state) {
        this.state = state;
    }

    public Instant getCreatedAt() {
        return createdAt;
    }

    public void setCreatedAt(Instant createdAt) {
        this.createdAt = createdAt;
    }

    public Instant getUpdatedAt() {
        return updatedAt;
    }

    public void setUpdatedAt(Instant updatedAt) {
        this.updatedAt = updatedAt;
    }
}
