package com.example.cycleworkbench.web;

public record TaskCreateRequest(
        String name,
        ScheduleRequest schedule,
        Integer instances) {
}
