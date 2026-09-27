package com.example.workbench;

import com.example.workbench.model.InstanceStatus;
import com.example.workbench.model.ScheduleType;
import com.example.workbench.model.TaskDefinition;
import com.example.workbench.model.TriggerInstance;
import com.example.workbench.service.InstanceGenerator;
import org.junit.jupiter.api.Test;

import java.time.Instant;
import java.time.LocalTime;
import java.util.List;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * 录制并核对多个未来实例：普通日期、闰日、夏令时春季拨快（跳过）、
 * 秋季回拨（重复），以及“每天当地九点”与“每隔二十四小时”的差异。
 */
class InstanceGeneratorTest {

    private final InstanceGenerator generator = new InstanceGenerator();

    private TaskDefinition daily(String time, String zone, Instant createdAt) {
        return new TaskDefinition(UUID.randomUUID(), "daily-" + time, ScheduleType.DAILY,
                null, LocalTime.parse(time), zone, createdAt);
    }

    private TaskDefinition interval(long seconds, String zone, Instant createdAt) {
        return new TaskDefinition(UUID.randomUUID(), "interval-" + seconds, ScheduleType.INTERVAL,
                seconds, null, zone, createdAt);
    }

    @Test
    void 普通日期_连续五天全部正常() {
        var task = daily("09:00", "Asia/Shanghai", Instant.parse("2026-09-20T00:00:00Z"));
        List<TriggerInstance> list = generator.generate(task, Instant.parse("2026-09-25T00:00:00Z"), 5);

        assertThat(list).hasSize(5).allMatch(i -> i.getStatus() == InstanceStatus.NORMAL);
        assertThat(list.get(0).getLocalDateTime().toString()).isEqualTo("2026-09-25T09:00");
        assertThat(list.get(4).getLocalDateTime().toString()).isEqualTo("2026-09-29T09:00");
        // 上海无夏令时，UTC 时刻恒为 01:00
        assertThat(list).allMatch(i -> i.getFireTimeUtc().toString().endsWith("T01:00:00Z"));
    }

    @Test
    void 闰日_2024年2月29日被正确生成() {
        var task = daily("08:00", "UTC", Instant.parse("2024-02-20T00:00:00Z"));
        List<TriggerInstance> list = generator.generate(task, Instant.parse("2024-02-27T00:00:00Z"), 5);

        assertThat(list).hasSize(5);
        assertThat(list.stream().map(i -> i.getLocalDateTime().toLocalDate().toString()))
                .containsExactly("2024-02-27", "2024-02-28", "2024-02-29", "2024-03-01", "2024-03-02");
        assertThat(list.get(2).getFireTimeUtc()).isEqualTo(Instant.parse("2024-02-29T08:00:00Z"));
    }

    @Test
    void 夏令时春季拨快_纽约0230不存在_该次跳过() {
        // 2026-03-08 美国夏令时开始，02:00-02:59 不存在
        var task = daily("02:30", "America/New_York", Instant.parse("2026-03-01T00:00:00Z"));
        List<TriggerInstance> list = generator.generate(task, Instant.parse("2026-03-06T12:00:00Z"), 5);

        assertThat(list).hasSize(5);
        // 3/6、3/7 正常（EST, UTC-5）
        assertThat(list.get(0).getStatus()).isEqualTo(InstanceStatus.NORMAL);
        assertThat(list.get(0).getFireTimeUtc()).isEqualTo(Instant.parse("2026-03-06T07:30:00Z"));
        assertThat(list.get(1).getFireTimeUtc()).isEqualTo(Instant.parse("2026-03-07T07:30:00Z"));
        // 3/8 跳过：无 UTC 时刻，带原因
        TriggerInstance skipped = list.get(2);
        assertThat(skipped.getStatus()).isEqualTo(InstanceStatus.SKIPPED);
        assertThat(skipped.getFireTimeUtc()).isNull();
        assertThat(skipped.getLocalDateTime().toString()).isEqualTo("2026-03-08T02:30");
        assertThat(skipped.getReason()).contains("夏令时开始").contains("跳过");
        // 3/9 起进入 EDT（UTC-4），UTC 触发时刻提前一小时
        assertThat(list.get(3).getStatus()).isEqualTo(InstanceStatus.NORMAL);
        assertThat(list.get(3).getFireTimeUtc()).isEqualTo(Instant.parse("2026-03-09T06:30:00Z"));
        assertThat(list.get(3).getZoneOffset()).isEqualTo("-04:00");
    }

    @Test
    void 夏令时秋季回拨_纽约0130出现两次_生成两条实例() {
        // 2026-11-01 美国夏令时结束，01:00-01:59 出现两次
        var task = daily("01:30", "America/New_York", Instant.parse("2026-10-20T00:00:00Z"));
        List<TriggerInstance> list = generator.generate(task, Instant.parse("2026-10-30T12:00:00Z"), 4);

        // 4 天产出 5 条：11/1 当天两条
        assertThat(list).hasSize(5);
        TriggerInstance first = list.get(2);
        TriggerInstance second = list.get(3);
        assertThat(first.getStatus()).isEqualTo(InstanceStatus.REPEATED_FIRST);
        assertThat(second.getStatus()).isEqualTo(InstanceStatus.REPEATED_SECOND);
        // 同一本地挂钟时间，两个不同 UTC 时刻，相差一小时
        assertThat(first.getLocalDateTime()).isEqualTo(second.getLocalDateTime());
        assertThat(first.getFireTimeUtc()).isEqualTo(Instant.parse("2026-11-01T05:30:00Z"));
        assertThat(second.getFireTimeUtc()).isEqualTo(Instant.parse("2026-11-01T06:30:00Z"));
        assertThat(first.getZoneOffset()).isEqualTo("-04:00");
        assertThat(second.getZoneOffset()).isEqualTo("-05:00");
        assertThat(first.getReason()).contains("回拨").contains("第一次");
        assertThat(second.getReason()).contains("回拨").contains("第二次");
    }

    @Test
    void 南半球夏令时方向相反_悉尼在十月拨快() {
        // 2026-10-04 悉尼夏令时开始
        var task = daily("02:30", "Australia/Sydney", Instant.parse("2026-09-25T00:00:00Z"));
        List<TriggerInstance> list = generator.generate(task, Instant.parse("2026-10-03T12:00:00Z"), 3);

        assertThat(list.get(0).getStatus()).isEqualTo(InstanceStatus.NORMAL);
        assertThat(list.get(1).getStatus()).isEqualTo(InstanceStatus.SKIPPED);
        assertThat(list.get(1).getLocalDateTime().toString()).isEqualTo("2026-10-04T02:30");
        assertThat(list.get(2).getStatus()).isEqualTo(InstanceStatus.NORMAL);
    }

    @Test
    void 固定间隔_每24小时在UTC上严格等距_本地时间因夏令时漂移() {
        // 锚点 2026-03-07 14:00 UTC = 纽约当地 09:00 EST；from 早于锚点使首条即锚点
        var task = interval(86400, "America/New_York", Instant.parse("2026-03-07T14:00:00Z"));
        List<TriggerInstance> list = generator.generate(task, Instant.parse("2026-03-07T13:00:00Z"), 4);

        assertThat(list).hasSize(4).allMatch(i -> i.getStatus() == InstanceStatus.NORMAL);
        // UTC 严格等差 86400 秒
        for (int i = 1; i < 4; i++) {
            long gap = list.get(i).getFireTimeUtc().getEpochSecond()
                    - list.get(i - 1).getFireTimeUtc().getEpochSecond();
            assertThat(gap).isEqualTo(86400);
        }
        // 3/8 夏令时开始后，本地挂钟时间从 09:00 漂移到 10:00
        assertThat(list.get(0).getLocalDateTime().toLocalTime()).isEqualTo(LocalTime.of(9, 0));
        assertThat(list.get(1).getLocalDateTime().toLocalTime()).isEqualTo(LocalTime.of(10, 0));
    }

    @Test
    void 对照_每天当地九点与每隔二十四小时不是同一条规则() {
        Instant anchor = Instant.parse("2026-03-07T14:00:00Z"); // 纽约 09:00 EST
        var dailyTask = daily("09:00", "America/New_York", anchor);
        var intervalTask = interval(86400, "America/New_York", anchor);

        List<TriggerInstance> daily = generator.generate(dailyTask, anchor, 3);
        List<TriggerInstance> every24h = generator.generate(intervalTask, anchor, 3);

        // 夏令时切换当天（3/8），两条规则的 UTC 触发时刻相差一小时
        assertThat(daily.get(1).getFireTimeUtc()).isEqualTo(Instant.parse("2026-03-08T13:00:00Z"));
        assertThat(every24h.get(0).getFireTimeUtc()).isEqualTo(Instant.parse("2026-03-08T14:00:00Z"));
        // 每日规则保持本地 09:00；间隔规则本地时间漂移
        assertThat(daily.get(1).getLocalDateTime().toLocalTime()).isEqualTo(LocalTime.of(9, 0));
        assertThat(every24h.get(0).getLocalDateTime().toLocalTime()).isEqualTo(LocalTime.of(10, 0));
    }
}
