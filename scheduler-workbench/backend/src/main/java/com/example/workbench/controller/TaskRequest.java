package com.example.workbench.controller;

import com.example.workbench.model.ScheduleType;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;

import java.time.LocalTime;

public record TaskRequest(
        @NotBlank String name,
        @NotNull ScheduleType scheduleType,
        @Positive Long intervalSeconds,
        LocalTime dailyTime,
        @NotBlank String timezone
) {
}
