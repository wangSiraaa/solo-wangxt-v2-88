package com.example.cycleworkbench.web;

public record PreviewRequest(
        ScheduleRequest schedule,
        Integer limit,
        String viewZoneId) {
}
