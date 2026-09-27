package com.example.cycleworkbench.web;

public record TaskUpdateRequest(
        String name,
        ScheduleRequest schedule,
        Integer instances) {
}
