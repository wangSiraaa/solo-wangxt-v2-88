package com.example.cycleworkbench;

import static org.assertj.core.api.Assertions.assertThat;

import com.example.cycleworkbench.clock.MutableClock;
import com.example.cycleworkbench.schedule.DstNote;
import com.example.cycleworkbench.schedule.ScheduleSpec;
import com.example.cycleworkbench.schedule.TimelineGenerator;
import com.example.cycleworkbench.schedule.TimelineItem;
import com.example.cycleworkbench.schedule.TimelinePreview;
import com.example.cycleworkbench.schedule.TriggerFactory;
import com.example.cycleworkbench.task.InstanceState;
import java.time.Clock;
import java.time.Instant;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.time.ZoneOffset;
import java.util.List;
import java.util.stream.Collectors;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

/**
 * "Recordings" of multiple future instances against ordinary dates, the Feb 29 leap day and
 * DST transitions — verified against independently computed java.time expectations, not just
 * a single next-run time.
 */
class ScheduleRecordingTest {

    private TimelineGenerator generatorAt(String pinnedIso) {
        MutableClock clock = new MutableClock();
        clock.pinTo(Instant.parse(pinnedIso));
        return new TimelineGenerator(new TriggerFactory(), clock);
    }

    private List<LocalDateTime> localsOf(TimelinePreview preview) {
        return preview.items().stream()
                .map(TimelineItem::nominalLocal)
                .toList();
    }

    @Test
    @DisplayName("普通日期：每天当地 09:00 在无 DST 的上海时区严格相隔 24 小时")
    void ordinaryDailyShanghaiStaysAtNine() {
        TimelineGenerator generator = generatorAt("2026-09-27T00:00:00Z");
        ScheduleSpec spec = ScheduleSpec.dailyLocal(9, 0, "Asia/Shanghai",
                Instant.parse("2026-09-27T00:00:00Z"));

        TimelinePreview preview = generator.generate(spec, 10, null);

        ZoneId shanghai = ZoneId.of("Asia/Shanghai");
        List<TimelineItem> fired = preview.items();
        assertThat(fired).hasSize(10);
        assertThat(fired).allSatisfy(item -> {
            assertThat(item.note()).isEqualTo(DstNote.NORMAL);
            assertThat(item.actualLocal().toLocalTime()).hasToString("09:00");
            assertThat(item.utcOffset()).isEqualTo(ZoneOffset.ofHours(8));
        });
        assertThat(fired.get(0).actualUtc()).isEqualTo("2026-09-27T01:00:00Z");
        // Every UTC gap is exactly 24h.
        assertThat(fired).allSatisfy(item -> {
            if (item.gapSecondsFromPrevious() != null) {
                assertThat(item.gapSecondsFromPrevious()).isEqualTo(86_400L);
            }
        });
        assertThat(fired.get(0).actualUtc()).isNotNull();
        // local mapping sanity: 09:00 Shanghai is 01:00 UTC.
        assertThat(LocalDateTime.ofInstant(fired.get(0).actualUtc(), shanghai))
                .isEqualTo(fired.get(0).actualLocal());
    }

    @Test
    @DisplayName("普通日期对比：固定间隔 24 小时 与 每日本地 09:00 在 UTC 时区实例完全一致")
    void fixed24hEqualsDailyLocalInUtc() {
        TimelineGenerator generator = generatorAt("2026-03-01T09:00:00Z");
        ScheduleSpec interval = ScheduleSpec.fixedInterval(86_400,
                Instant.parse("2026-03-01T09:00:00Z"));
        ScheduleSpec daily = ScheduleSpec.dailyLocal(9, 0, "UTC",
                Instant.parse("2026-03-01T09:00:00Z"));

        List<Instant> a = generator.generate(interval, 12, null).items().stream()
                .map(TimelineItem::actualUtc).toList();
        List<Instant> b = generator.generate(daily, 12, null).items().stream()
                .map(TimelineItem::actualUtc).toList();

        assertThat(a).isEqualTo(b);
    }

    @Test
    @DisplayName("闰日 2024：每日本地时间录制包含 2 月 29 日；2025 平年同日则直接跳到 3 月 1 日")
    void leapDayIncludedIn2024AndAbsentIn2025() {
        ScheduleSpec spec2024 = ScheduleSpec.dailyLocal(9, 0, "Asia/Shanghai",
                Instant.parse("2024-02-27T00:00:00Z"));
        List<LocalDateTime> dates2024 = localsOf(
                generatorAt("2024-02-27T00:00:00Z").generate(spec2024, 6, null));

        assertThat(dates2024).contains(
                LocalDateTime.parse("2024-02-27T09:00"),
                LocalDateTime.parse("2024-02-28T09:00"),
                LocalDateTime.parse("2024-02-29T09:00"),
                LocalDateTime.parse("2024-03-01T09:00"));

        ScheduleSpec spec2025 = ScheduleSpec.dailyLocal(9, 0, "Asia/Shanghai",
                Instant.parse("2025-02-27T00:00:00Z"));
        List<LocalDateTime> dates2025 = localsOf(
                generatorAt("2025-02-27T00:00:00Z").generate(spec2025, 4, null));

        assertThat(dates2025).containsExactly(
                LocalDateTime.parse("2025-02-27T09:00"),
                LocalDateTime.parse("2025-02-28T09:00"),
                LocalDateTime.parse("2025-03-01T09:00"),
                LocalDateTime.parse("2025-03-02T09:00"));
    }

    @Test
    @DisplayName("闰日固定间隔：从 2024-02-29 00:00 UTC 起每 365 天，第二年落在 2025-02-28")
    void yearlyIntervalAcrossLeapDay() {
        TimelineGenerator generator = generatorAt("2024-02-29T00:00:00Z");
        ScheduleSpec spec = ScheduleSpec.fixedInterval(365 * 86_400L,
                Instant.parse("2024-02-29T00:00:00Z"));

        List<TimelineItem> items = generator.generate(spec, 3, "UTC").items();

        assertThat(items.stream().map(i -> i.actualLocal().toLocalDate()).toList())
                .containsExactly(
                        java.time.LocalDate.parse("2024-02-29"),
                        java.time.LocalDate.parse("2025-02-28"),
                        java.time.LocalDate.parse("2026-02-28"));
    }

    @Test
    @DisplayName("DST 春令时空档：纽约 02:30 在 2026-03-08 不存在，录制为 SUPPRESSED/GAP_SKIPPED，"
            + "实际触发跳过当天")
    void springForwardGapIsSuppressed() {
        TimelineGenerator generator = generatorAt("2026-03-07T00:00:00Z");
        ScheduleSpec spec = ScheduleSpec.dailyLocal(2, 30, "America/New_York",
                Instant.parse("2026-03-07T00:00:00Z"));

        List<TimelineItem> items = generator.generate(spec, 6, null).items();

        TimelineItem gap = items.stream()
                .filter(i -> i.note() == DstNote.GAP_SKIPPED)
                .findFirst().orElseThrow();
        assertThat(gap.state()).isEqualTo(InstanceState.SUPPRESSED);
        assertThat(gap.nominalLocal()).isEqualTo(LocalDateTime.parse("2026-03-08T02:30"));
        assertThat(gap.actualUtc()).isNull();
        assertThat(gap.explanation()).contains("02:00").contains("03:00").contains("跳过");

        // The produced fires jump Mar 7 07:30Z -> Mar 9 06:30Z (47 hours), proving the day skip.
        List<TimelineItem> produced = items.stream()
                .filter(i -> i.state() == InstanceState.SCHEDULED).toList();
        assertThat(produced.get(0).actualUtc()).isEqualTo("2026-03-07T07:30:00Z");
        TimelineItem afterGap = produced.get(1);
        assertThat(afterGap.actualUtc()).isEqualTo("2026-03-09T06:30:00Z");
        assertThat(afterGap.gapSecondsFromPrevious()).isEqualTo(47 * 3600L);
    }

    @Test
    @DisplayName("DST 秋令时重叠：纽约 01:30 在 2026-11-01 出现两次，只触发一次（较晚的 EST）")
    void fallBackOverlapFiresOnceAtEarlierOffset() {
        TimelineGenerator generator = generatorAt("2026-10-31T00:00:00Z");
        ScheduleSpec spec = ScheduleSpec.dailyLocal(1, 30, "America/New_York",
                Instant.parse("2026-10-31T00:00:00Z"));

        List<TimelineItem> items = generator.generate(spec, 4, null).items();

        assertThat(items).hasSize(4);
        TimelineItem overlap = items.get(1);
        assertThat(overlap.nominalLocal()).isEqualTo(LocalDateTime.parse("2026-11-01T01:30"));
        assertThat(overlap.note()).isEqualTo(DstNote.OVERLAP_FIRED_ONCE);
        // Quartz resolves the ambiguous wall time to the later offset: EST (-05:00) -> 06:30 UTC.
        assertThat(overlap.actualUtc()).isEqualTo("2026-11-01T06:30:00Z");
        assertThat(overlap.utcOffset()).isEqualTo(ZoneOffset.ofHours(-5));
        assertThat(overlap.explanation()).contains("两次").contains("较晚");
        // Adjacent produced instants are 25 hours apart, but there is still only one ordinal.
        assertThat(overlap.gapSecondsFromPrevious()).isEqualTo(25 * 3600L);
        long countAtWall = items.stream()
                .filter(i -> i.nominalLocal().equals(LocalDateTime.parse("2026-11-01T01:30")))
                .count();
        assertThat(countAtWall).isEqualTo(1);
    }

    @Test
    @DisplayName("纽约每天 09:00：跨 DST 时本地时刻不变，UTC 从 14:00Z 变为 13:00Z（23/25 小时间隔）")
    void dailyAtNineShiftsUtcAcrossDst() {
        // Spring transition 2026-03-08.
        TimelineGenerator spring = generatorAt("2026-03-06T12:00:00Z");
        ScheduleSpec spec = ScheduleSpec.dailyLocal(9, 0, "America/New_York",
                Instant.parse("2026-03-06T12:00:00Z"));
        List<TimelineItem> springItems = spring.generate(spec, 4, null).items();

        assertThat(springItems).extracting(TimelineItem::actualUtc).containsExactly(
                Instant.parse("2026-03-06T14:00:00Z"),
                Instant.parse("2026-03-07T14:00:00Z"),
                Instant.parse("2026-03-08T13:00:00Z"),
                Instant.parse("2026-03-09T13:00:00Z"));
        TimelineItem shift = springItems.get(2);
        assertThat(shift.note()).isEqualTo(DstNote.OFFSET_SHIFT);
        assertThat(shift.gapSecondsFromPrevious()).isEqualTo(23 * 3600L);
        assertThat(shift.explanation()).contains("23");

        // Fall transition 2026-11-01: 13:00Z -> 14:00Z, 25 hours.
        TimelineGenerator fall = generatorAt("2026-10-31T12:00:00Z");
        ScheduleSpec fallSpec = ScheduleSpec.dailyLocal(9, 0, "America/New_York",
                Instant.parse("2026-10-31T12:00:00Z"));
        List<TimelineItem> fallItems = fall.generate(fallSpec, 3, null).items();
        assertThat(fallItems.get(1).actualUtc()).isEqualTo("2026-11-01T14:00:00Z");
        assertThat(fallItems.get(1).gapSecondsFromPrevious()).isEqualTo(25 * 3600L);
    }

    @Test
    @DisplayName("固定间隔 24 小时：锚定 UTC 绝不跳过/重复，但在纽约墙上时间显示漂移一小时")
    void fixedIntervalDriftsOnWallClockAcrossDst() {
        TimelineGenerator generator = generatorAt("2026-03-07T12:00:00Z");
        ScheduleSpec spec = ScheduleSpec.fixedInterval(86_400,
                Instant.parse("2026-03-07T12:00:00Z"));

        TimelinePreview preview = generator.generate(spec, 4, "America/New_York");
        List<TimelineItem> items = preview.items();

        // UTC instants are rigidly every 24h ...
        assertThat(items).allSatisfy(i -> {
            if (i.gapSecondsFromPrevious() != null) {
                assertThat(i.gapSecondsFromPrevious()).isEqualTo(86_400L);
            }
        });
        // ... while New York wall times go 07:00 (EST) -> 08:00 (EDT) after spring-forward.
        assertThat(items).extracting(TimelineItem::actualLocal).containsExactly(
                LocalDateTime.parse("2026-03-07T07:00"),
                LocalDateTime.parse("2026-03-08T08:00"),
                LocalDateTime.parse("2026-03-09T08:00"),
                LocalDateTime.parse("2026-03-10T08:00"));
        TimelineItem drifted = items.get(1);
        assertThat(drifted.note()).isEqualTo(DstNote.INTERVAL_OFFSET_DRIFT);
        assertThat(drifted.explanation()).contains("UTC").contains("漂移");
    }

    @Test
    @DisplayName("南半球夏令时：悉尼每天 02:30 会在 2026-10-04 空档日被跳过")
    void southernHemisphereGap() {
        TimelineGenerator generator = generatorAt("2026-10-03T12:00:00Z");
        ScheduleSpec spec = ScheduleSpec.dailyLocal(2, 30, "Australia/Sydney",
                Instant.parse("2026-10-03T12:00:00Z"));

        List<TimelineItem> items = generator.generate(spec, 4, null).items();

        assertThat(items).anySatisfy(item -> {
            assertThat(item.note()).isEqualTo(DstNote.GAP_SKIPPED);
            assertThat(item.nominalLocal()).isEqualTo(LocalDateTime.parse("2026-10-04T02:30"));
            assertThat(item.actualUtc()).isNull();
        });
    }

    @Test
    @DisplayName("演示时钟可注入：把时钟拨到未来日期后，时间线从该时刻开始录制")
    void injectableClockPinsGenerationWindow() {
        MutableClock clock = new MutableClock();
        TimelineGenerator generator = new TimelineGenerator(new TriggerFactory(), clock);
        ScheduleSpec spec = ScheduleSpec.dailyLocal(9, 0, "UTC",
                Instant.parse("2000-01-01T09:00:00Z"));

        clock.pinTo(Instant.parse("2030-06-15T09:00:00Z"));
        assertThat(generator.generate(spec, 1, null).items().get(0).actualUtc())
                .isEqualTo("2030-06-15T09:00:00Z");

        clock.advance(java.time.Duration.ofDays(1));
        assertThat(generator.generate(spec, 1, null).items().get(0).actualUtc())
                .isEqualTo("2030-06-16T09:00:00Z");

        clock.returnToLive();
        // Back in live mode the "now" used is the real system clock; anchor is still 2000.
        Instant nowish = Instant.now();
        Instant first = generator.generate(spec, 1, null).items().get(0).actualUtc();
        assertThat(first).isAfterOrEqualTo(nowish.minusSeconds(60));
        assertThat(first).isBefore(nowish.plusSeconds(60 * 60 * 24));
    }
}
