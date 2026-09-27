package com.example.workbench.service;

import com.example.workbench.model.InstanceStatus;
import com.example.workbench.model.ScheduleType;
import com.example.workbench.model.TaskDefinition;
import com.example.workbench.model.TriggerInstance;
import org.springframework.stereotype.Service;

import java.time.Duration;
import java.time.Instant;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.time.ZoneOffset;
import java.time.ZonedDateTime;
import java.time.zone.ZoneRules;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

/**
 * 未来触发实例的计算核心。
 *
 * 关键区别：
 *  - INTERVAL：锚定 UTC 时刻做等差数列，夏令时只影响“展示用的本地时间”，不影响触发时刻。
 *  - DAILY：每天在任务时区内解析“本地挂钟时间”。解析结果有三种：
 *      1. 恰好一个偏移  -> NORMAL
 *      2. 零个偏移（春季拨快产生的空洞） -> SKIPPED，不触发
 *      3. 两个偏移（秋季回拨产生的重叠） -> REPEATED_FIRST / REPEATED_SECOND，触发两次
 */
@Service
public class InstanceGenerator {

    /**
     * 生成从 from（含）之后、count 个“本地日”内的全部实例。
     * 对 DAILY 而言 count 是天数（重叠日会产出 2 条，跳过日产出 1 条 SKIPPED 记录）；
     * 对 INTERVAL 而言 count 就是实例条数。
     */
    public List<TriggerInstance> generate(TaskDefinition task, Instant from, int count) {
        if (task.getScheduleType() == ScheduleType.INTERVAL) {
            return generateInterval(task, from, count);
        }
        return generateDaily(task, from, count);
    }

    private List<TriggerInstance> generateInterval(TaskDefinition task, Instant from, int count) {
        ZoneId zone = ZoneId.of(task.getTimezone());
        long step = task.getIntervalSeconds();
        // 以任务创建时刻为锚点，找到 from 之后的第一次触发
        Instant anchor = task.getCreatedAt();
        long elapsed = Duration.between(anchor, from).getSeconds();
        long steps = elapsed < 0 ? 0 : elapsed / step + 1;

        List<TriggerInstance> out = new ArrayList<>(count);
        for (int i = 0; i < count; i++) {
            Instant fire = anchor.plusSeconds((steps + i) * step);
            ZonedDateTime local = fire.atZone(zone);
            out.add(new TriggerInstance(UUID.randomUUID(), task.getId(), i, fire,
                    local.toLocalDateTime(), local.getOffset().toString(),
                    InstanceStatus.NORMAL, null));
        }
        return out;
    }

    private List<TriggerInstance> generateDaily(TaskDefinition task, Instant from, int days) {
        ZoneId zone = ZoneId.of(task.getTimezone());
        ZoneRules rules = zone.getRules();
        LocalDate startDate = LocalDateTime.ofInstant(from, zone).toLocalDate();

        List<TriggerInstance> out = new ArrayList<>();
        int seq = 0;
        for (int d = 0; d < days; d++) {
            LocalDate date = startDate.plusDays(d);
            LocalDateTime localPlan = LocalDateTime.of(date, task.getDailyTime());
            List<ZoneOffset> offsets = rules.getValidOffsets(localPlan);

            if (offsets.isEmpty()) {
                // 春季拨快：该本地时间不存在
                out.add(new TriggerInstance(UUID.randomUUID(), task.getId(), seq++, null,
                        localPlan, null, InstanceStatus.SKIPPED,
                        "本地时间 %s 在 %s 不存在：夏令时开始，时钟拨快，该时刻被跳过，本次不触发"
                                .formatted(task.getDailyTime(), date)));
            } else if (offsets.size() == 2) {
                // 秋季回拨：该本地时间出现两次
                ZoneOffset first = offsets.get(0);  // 夏令时偏移（较早的 UTC 时刻）
                ZoneOffset second = offsets.get(1); // 标准时偏移
                out.add(new TriggerInstance(UUID.randomUUID(), task.getId(), seq++,
                        localPlan.atOffset(first).toInstant(), localPlan, first.toString(),
                        InstanceStatus.REPEATED_FIRST,
                        "夏令时结束，时钟回拨，本地时间 %s 在 %s 出现两次；这是第一次（仍处夏令时 %s）"
                                .formatted(task.getDailyTime(), date, first)));
                out.add(new TriggerInstance(UUID.randomUUID(), task.getId(), seq++,
                        localPlan.atOffset(second).toInstant(), localPlan, second.toString(),
                        InstanceStatus.REPEATED_SECOND,
                        "夏令时结束，时钟回拨，本地时间 %s 在 %s 出现两次；这是第二次（已进入标准时 %s）"
                                .formatted(task.getDailyTime(), date, second)));
            } else {
                ZoneOffset offset = offsets.get(0);
                out.add(new TriggerInstance(UUID.randomUUID(), task.getId(), seq++,
                        localPlan.atOffset(offset).toInstant(), localPlan, offset.toString(),
                        InstanceStatus.NORMAL, null));
            }
        }
        return out;
    }
}
