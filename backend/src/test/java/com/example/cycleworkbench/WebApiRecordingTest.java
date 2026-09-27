package com.example.cycleworkbench;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;

/**
 * End-to-end API recordings: injectable clock -> multi-instance preview -> persist with a real
 * Quartz scheduler -> pause/resume toggle. DST gap rows appear in the persisted instances.
 */
@SpringBootTest
@AutoConfigureMockMvc
@TestPropertySource(properties = {
        "spring.datasource.url=jdbc:h2:mem:api;MODE=PostgreSQL;DATABASE_TO_LOWER=TRUE;NON_KEYWORDS=HOUR,MINUTE;DB_CLOSE_DELAY=-1",
        "spring.datasource.driver-class-name=org.h2.Driver",
        "spring.datasource.username=sa",
        "spring.datasource.password=",
        "spring.sql.init.schema-locations=classpath:schema-h2.sql",
        "spring.quartz.job-store-type=memory"
})
class WebApiRecordingTest {

    @Autowired
    MockMvc mvc;

    @Test
    void pinClockPreviewCreatePauseResume() throws Exception {
        // 1. Pin the demo clock just before the 2026 New York spring-forward.
        mvc.perform(post("/api/clock/demo")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"instant":"2026-03-07T00:00:00Z"}"""))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.demo").value(true));

        // 2. Preview the 02:30 daily rule: expect a suppressed gap and a 47h produced gap.
        String schedule = """
                {"kind":"DAILY_LOCAL","hour":2,"minute":30,
                 "zoneId":"America/New_York","anchorUtc":"2026-03-07T00:00:00Z"}""";
        mvc.perform(post("/api/tasks/preview")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"schedule\":" + schedule + ",\"limit\":6}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.items[?(@.note=='GAP_SKIPPED')].nominalLocal")
                        .value(org.hamcrest.Matchers.hasItem("2026-03-08T02:30:00")))
                .andExpect(jsonPath("$.items[?(@.actualUtc=='2026-03-09T06:30:00Z')]"
                        + ".gapSecondsFromPrevious").value(47 * 3600));

        // 3. Persist the task; Quartz registers a real trigger.
        MvcResult created = mvc.perform(post("/api/tasks")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"name\":\"NY 02:30\",\"schedule\":" + schedule
                                + ",\"instances\":6}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.state").value("ACTIVE"))
                .andReturn();
        long taskId = com.jayway.jsonpath.JsonPath
                .parse(created.getResponse().getContentAsString()).read("$.id", Long.class);

        // 4. Persisted instances contain the NULL-UTC suppressed row.
        mvc.perform(get("/api/tasks/{id}/instances", taskId))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[?(@.dstNote=='GAP_SKIPPED')].actualUtc")
                        .value(org.hamcrest.Matchers.hasItem(org.hamcrest.Matchers.nullValue())))
                .andExpect(jsonPath("$[?(@.dstNote=='GAP_SKIPPED')].state")
                        .value(org.hamcrest.Matchers.hasItem("SUPPRESSED")));

        // 5. Pause then resume through the Quartz scheduler.
        mvc.perform(post("/api/tasks/{id}/pause", taskId))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.state").value("PAUSED"));
        mvc.perform(post("/api/tasks/{id}/resume", taskId))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.state").value("ACTIVE"));

        // 6. Back to live clock.
        mvc.perform(post("/api/clock/live"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.demo").value(false));
    }

    @Test
    void invalidScheduleReturns400() throws Exception {
        mvc.perform(post("/api/tasks/preview")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"schedule":{"kind":"DAILY_LOCAL","hour":25,"minute":0,
                                  "zoneId":"UTC","anchorUtc":"2026-01-01T00:00:00Z"}}"""))
                .andExpect(status().isBadRequest());
    }
}
