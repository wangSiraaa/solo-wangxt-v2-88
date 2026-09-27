package com.example.cycleworkbench;

import static org.assertj.core.api.Assertions.assertThat;

import com.example.cycleworkbench.task.InstanceState;
import com.example.cycleworkbench.task.TaskDefinition;
import com.example.cycleworkbench.task.TaskRepository;
import com.example.cycleworkbench.task.TriggerInstanceRepository;
import com.example.cycleworkbench.task.TriggerInstanceRow;
import com.example.cycleworkbench.schedule.DstNote;
import com.example.cycleworkbench.schedule.ScheduleSpec;
import com.example.cycleworkbench.schedule.TimelineGenerator;
import com.example.cycleworkbench.schedule.TimelineItem;
import com.example.cycleworkbench.schedule.TimelinePreview;
import com.example.cycleworkbench.schedule.TriggerFactory;
import com.example.cycleworkbench.clock.MutableClock;
import java.time.Instant;
import java.util.List;
import javax.sql.DataSource;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.context.TestPropertySource;

/**
 * Persistence "recording": the generated instances (including a suppressed DST-gap row with a
 * null actual_utc) round-trip through the database. Runs on H2 in PostgreSQL mode; production
 * uses PostgreSQL with db/schema.sql.
 */
@SpringBootTest
@TestPropertySource(properties = {
        "spring.datasource.url=jdbc:h2:mem:wb;MODE=PostgreSQL;DATABASE_TO_LOWER=TRUE;NON_KEYWORDS=HOUR,MINUTE;DB_CLOSE_DELAY=-1",
        "spring.datasource.driver-class-name=org.h2.Driver",
        "spring.datasource.username=sa",
        "spring.datasource.password=",
        "spring.sql.init.schema-locations=classpath:schema-h2.sql",
        "spring.quartz.job-store-type=memory",
        "spring.quartz.auto-startup=false"
})
class PersistenceRecordingTest {

    @Autowired
    TaskRepository taskRepository;
    @Autowired
    TriggerInstanceRepository instanceRepository;
    @Autowired
    DataSource dataSource;

    @BeforeEach
    void clean() {
        new JdbcTemplate(dataSource).execute("DELETE FROM trigger_instances");
        new JdbcTemplate(dataSource).execute("DELETE FROM task_definitions");
    }

    @Test
    void dstGapTimelineIsPersistedAndReloaded() {
        MutableClock clock = new MutableClock();
        clock.pinTo(Instant.parse("2026-03-07T00:00:00Z"));
        TimelineGenerator generator = new TimelineGenerator(new TriggerFactory(), clock);
        ScheduleSpec spec = ScheduleSpec.dailyLocal(2, 30, "America/New_York",
                Instant.parse("2026-03-07T00:00:00Z"));
        TimelinePreview preview = generator.generate(spec, 5, null);

        TaskDefinition task = new TaskDefinition();
        task.setName("NY 02:30 DST 录制");
        task.setSchedule(spec);
        task.setCreatedAt(clock.instant());
        task.setUpdatedAt(clock.instant());
        taskRepository.save(task);
        instanceRepository.replaceForTask(task.getId(), preview.items(), clock.instant());

        List<TriggerInstanceRow> rows = instanceRepository.findByTask(task.getId());
        assertThat(rows).hasSize((int) preview.items().size());

        TriggerInstanceRow gap = rows.stream()
                .filter(r -> r.getDstNote().equals(DstNote.GAP_SKIPPED.name()))
                .findFirst().orElseThrow();
        assertThat(gap.getState()).isEqualTo(InstanceState.SUPPRESSED);
        assertThat(gap.getActualUtc()).isNull();
        assertThat(gap.getActualLocal()).isNull();
        assertThat(gap.getUtcOffset()).isNull();
        assertThat(gap.getNominalLocal()).hasToString("2026-03-08T02:30");
        assertThat(gap.getExplanation()).contains("跳过");

        // Round-trip the produced row too.
        TriggerInstanceRow fired = rows.stream()
                .filter(r -> r.getState() == InstanceState.SCHEDULED
                        && r.getNominalLocal().toString().equals("2026-03-07T02:30"))
                .findFirst().orElseThrow();
        assertThat(fired.getActualUtc()).isEqualTo("2026-03-07T07:30:00Z");
        assertThat(fired.getUtcOffset()).isEqualTo("-05:00");
    }

    @Test
    void regeneratingReplacesOldInstances() {
        MutableClock clock = new MutableClock();
        clock.pinTo(Instant.parse("2026-09-27T00:00:00Z"));
        TimelineGenerator generator = new TimelineGenerator(new TriggerFactory(), clock);
        ScheduleSpec spec = ScheduleSpec.dailyLocal(9, 0, "UTC",
                Instant.parse("2026-09-27T00:00:00Z"));
        TaskDefinition task = new TaskDefinition();
        task.setName("两次录制");
        task.setSchedule(spec);
        task.setCreatedAt(clock.instant());
        task.setUpdatedAt(clock.instant());
        taskRepository.save(task);

        instanceRepository.replaceForTask(task.getId(),
                generator.generate(spec, 3, null).items(), clock.instant());
        instanceRepository.replaceForTask(task.getId(),
                generator.generate(spec, 7, null).items(), clock.instant());

        List<TriggerInstanceRow> rows = instanceRepository.findByTask(task.getId());
        assertThat(rows).hasSize(7);
        assertThat(rows).extracting(TriggerInstanceRow::getOrdinal)
                .containsExactly(1, 2, 3, 4, 5, 6, 7);
    }

    @Test
    void markFiredPicksLatestPastScheduledInstance() {
        MutableClock clock = new MutableClock();
        clock.pinTo(Instant.parse("2026-09-27T00:00:00Z"));
        TimelineGenerator generator = new TimelineGenerator(new TriggerFactory(), clock);
        ScheduleSpec spec = ScheduleSpec.fixedInterval(86_400,
                Instant.parse("2026-09-27T00:00:00Z"));
        TaskDefinition task = new TaskDefinition();
        task.setName("触发标记");
        task.setSchedule(spec);
        task.setCreatedAt(clock.instant());
        task.setUpdatedAt(clock.instant());
        taskRepository.save(task);
        List<TimelineItem> items = generator.generate(spec, 4, null).items();
        instanceRepository.replaceForTask(task.getId(), items, clock.instant());

        instanceRepository.markFired(task.getId(), Instant.parse("2026-09-29T00:00:00Z"));
        List<TriggerInstanceRow> rows = instanceRepository.findByTask(task.getId());
        assertThat(rows.stream().filter(r -> r.getState() == InstanceState.FIRED).count())
                .isEqualTo(1);
        assertThat(rows.stream()
                .filter(r -> r.getState() == InstanceState.FIRED)
                .findFirst().orElseThrow().getActualUtc())
                .isEqualTo("2026-09-29T00:00:00Z");
    }
}
