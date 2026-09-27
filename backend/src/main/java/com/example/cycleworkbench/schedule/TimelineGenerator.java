package com.example.cycleworkbench.schedule;

import com.example.cycleworkbench.task.InstanceState;
import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.time.ZoneOffset;
import java.time.zone.ZoneOffsetTransition;
import java.time.zone.ZoneRules;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import org.quartz.Trigger;
import org.springframework.stereotype.Service;

/**
 * Produces a list of future fire instances from a {@link ScheduleSpec}, with per-instance DST
 * explanations. The instants themselves come from real Quartz triggers; the annotations are
 * derived independently from {@link java.time} zone rules, so skipped/duplicated wall times are
 * surfaced rather than hidden.
 */
@Service
public class TimelineGenerator {

    private final TriggerFactory triggerFactory;
    private final Clock clock;

    public TimelineGenerator(TriggerFactory triggerFactory, Clock applicationClock) {
        this.triggerFactory = triggerFactory;
        this.clock = applicationClock;
    }

    public TimelinePreview generate(ScheduleSpec spec, Integer requestedLimit, String viewZoneId) {
        int limit = requestedLimit == null ? 14 : Math.max(1, Math.min(requestedLimit, 366));
        Instant from = clock.instant();
        List<TimelineItem> items = switch (spec.kind()) {
            case DAILY_LOCAL -> dailyLocal(spec, from, limit);
            case FIXED_INTERVAL -> fixedInterval(spec, from, limit,
                    viewZoneId == null ? ZoneOffset.UTC : ZoneId.of(viewZoneId));
        };
        return new TimelinePreview(spec, clock.instant(), from, items, summarize(spec));
    }

    private List<TimelineItem> dailyLocal(ScheduleSpec spec, Instant from, int limit) {
        ZoneId zone = ZoneId.of(spec.zoneId());
        ZoneRules rules = zone.getRules();

        // Ground truth from Quartz: strictly after `from` (minus a second so an anchor equal to
        // `from` is included).
        Trigger trigger = triggerFactory.build(spec, "preview-" + System.nanoTime());
        List<Instant> fires = new ArrayList<>();
        Date cursor = Date.from(from.minusSeconds(1));
        while (fires.size() < limit) {
            Date next = trigger.getFireTimeAfter(cursor);
            if (next == null) {
                break;
            }
            fires.add(next.toInstant());
            cursor = next;
        }

        List<TimelineItem> items = new ArrayList<>();
        int ordinal = 1;
        int produced = 0;
        Instant previousProduced = null;
        ZoneOffset previousOffset = null;

        LocalDate day = LocalDate.ofInstant(from, zone);
        // At most one extra nominal slot per produced day is needed; +3 is a safety bound.
        int maxDays = limit + limit + 3;
        for (int scanned = 0; scanned < maxDays && produced < limit; scanned++, day = day.plusDays(1)) {
            LocalDateTime nominal = day.atTime(spec.hour(), spec.minute());
            List<ZoneOffset> offsets = rules.getValidOffsets(nominal);
            ZoneOffsetTransition transition = rules.getTransition(nominal);

            if (offsets.isEmpty()) {
                // Spring-forward gap. Quartz skips this calendar day for the daily rule.
                // Suppress it only when the gap slot is still in the future relative to `from`.
                if (nominal.isAfter(LocalDateTime.ofInstant(from, zone))) {
                    items.add(new TimelineItem(
                            ordinal++, InstanceState.SUPPRESSED, DstNote.GAP_SKIPPED,
                            nominal, null, null, null,
                            gapText(zone, nominal, transition), null));
                }
                continue;
            }

            Instant actual = takeFittingFire(fires, zone, nominal);
            if (actual == null) {
                // Nominal day before the first produced fire (can happen around the anchor).
                continue;
            }
            produced++;
            ZonedParts parts = new ZonedParts(actual, zone);
            DstNote note;
            if (offsets.size() > 1) {
                note = DstNote.OVERLAP_FIRED_ONCE;
            } else if (previousOffset != null && !previousOffset.equals(parts.offset)) {
                note = DstNote.OFFSET_SHIFT;
            } else {
                note = DstNote.NORMAL;
            }
            Long gap = previousProduced == null ? null
                    : Duration.between(previousProduced, actual).getSeconds();
            String explanation = switch (note) {
                case OVERLAP_FIRED_ONCE -> overlapText(zone, nominal, parts.offset, transition);
                case OFFSET_SHIFT -> shiftText(zone, nominal, previousOffset, parts.offset, gap);
                default -> gap == null
                        ? "本地时间每天 %02d:%02d 触发；首个实例 UTC 偏移 %s。"
                                .formatted(spec.hour(), spec.minute(), parts.offset)
                        : "本地时间每天 %02d:%02d 触发；UTC 偏移 %s，与上一实例相隔 24 小时。"
                                .formatted(spec.hour(), spec.minute(), parts.offset);
            };
            items.add(new TimelineItem(ordinal++, InstanceState.SCHEDULED, note,
                    nominal, actual, parts.local, parts.offset, explanation, gap));
            previousProduced = actual;
            previousOffset = parts.offset;
        }
        return items;
    }

    private List<TimelineItem> fixedInterval(ScheduleSpec spec, Instant from, int limit,
                                             ZoneId viewZone) {
        Trigger trigger = triggerFactory.build(spec, "preview-" + System.nanoTime());
        List<TimelineItem> items = new ArrayList<>();
        Date cursor = Date.from(from.minusSeconds(1));
        Instant previous = null;
        ZoneOffset previousViewOffset = null;
        for (int ordinal = 1; ordinal <= limit; ordinal++) {
            Date next = trigger.getFireTimeAfter(cursor);
            if (next == null) {
                break;
            }
            Instant actual = next.toInstant();
            ZonedParts utc = new ZonedParts(actual, ZoneOffset.UTC);
            ZonedParts view = new ZonedParts(actual, viewZone);
            Duration expected = Duration.ofSeconds(spec.intervalSeconds());
            boolean drifts = previous != null && !previousViewOffset.equals(view.offset);
            DstNote note = drifts ? DstNote.INTERVAL_OFFSET_DRIFT : DstNote.NORMAL;
            Long gap = previous == null ? null
                    : Duration.between(previous, actual).getSeconds();
            String explanation;
            if (note == DstNote.INTERVAL_OFFSET_DRIFT) {
                explanation = "规则按 UTC 固定间隔 %s 锚定，本身不跳过也不重复；但在时区 %s 中，"
                        .formatted(humanDuration(expected), viewZone)
                        + "墙上时刻偏移由 " + previousViewOffset + " 变为 " + view.offset
                        + "，本地显示时刻发生漂移。这与“每天当地固定钟点”不是同一条规则。";
            } else {
                explanation = "按 UTC 固定间隔 %s 触发；本实例与上一实例实际相隔 %s。"
                        .formatted(humanDuration(expected),
                                gap == null ? humanDuration(Duration.ZERO)
                                        : humanDuration(Duration.ofSeconds(gap)));
            }
            items.add(new TimelineItem(ordinal, InstanceState.SCHEDULED, note,
                    utc.local, actual, view.local, view.offset, explanation, gap));
            previous = actual;
            previousViewOffset = view.offset;
            cursor = next;
        }
        return items;
    }

    private static Instant takeFittingFire(List<Instant> fires, ZoneId zone, LocalDateTime nominal) {
        // Quartz resolves overlap days to the earlier offset, and only emits one instant.
        for (Instant fire : fires) {
            if (LocalDateTime.ofInstant(fire, zone).equals(nominal)) {
                return fire;
            }
        }
        return null;
    }

    private static String gapText(ZoneId zone, LocalDateTime nominal, ZoneOffsetTransition t) {
        String jump = t == null ? ""
                : "时钟从 %s 直接跳到 %s，".formatted(
                        t.getDateTimeBefore().toLocalTime(),
                        t.getDateTimeAfter().toLocalTime());
        return "%s %s 的 %s 不存在（%s该本地时刻落在夏令时“跳时”空档）。"
                .formatted(nominal.toLocalDate(), zone, nominal.toLocalTime(), jump)
                + "Quartz 的每日 Cron 规则当天没有任何触发，跳过这一日后继续。";
    }

    private static String overlapText(ZoneId zone, LocalDateTime nominal, ZoneOffset chosen,
                                      ZoneOffsetTransition t) {
        String jump = t == null ? ""
                : "时钟从 %s 拨回 %s，".formatted(
                        t.getDateTimeBefore().toLocalTime(),
                        t.getDateTimeAfter().toLocalTime());
        return "%s %s 的 %s 在本地出现两次（%s夏令时结束）。"
                .formatted(nominal.toLocalDate(), zone, nominal.toLocalTime(), jump)
                + "Quartz 的 Cron 规则只在较晚的一次（偏移 %s，对应 UTC %s）触发一次，不重复执行。"
                        .formatted(chosen, nominal.atOffset(chosen).toInstant());
    }

    private static String shiftText(ZoneId zone, LocalDateTime nominal, ZoneOffset before,
                                    ZoneOffset after, Long gapSeconds) {
        long hours = gapSeconds == null ? 24 : gapSeconds / 3600;
        return "夏令时切换：%s 的 UTC 偏移由 %s 变为 %s。本地墙上时刻仍是 %s，"
                .formatted(zone, before, after, nominal.toLocalTime())
                + "但相邻两次 UTC 触发实际相隔 %d 小时（不是固定 24 小时）。".formatted(hours);
    }

    private static String humanDuration(Duration d) {
        long hours = d.toHours();
        long minutes = d.toMinutesPart();
        if (hours > 0 && minutes == 0) {
            return hours + " 小时";
        }
        return d.toSeconds() + " 秒";
    }

    private String summarize(ScheduleSpec spec) {
        return switch (spec.kind()) {
            case FIXED_INTERVAL -> "固定间隔：从 %s 起每 %s 一次（锚定 UTC，与“每天当地钟点”不同）。"
                    .formatted(spec.anchorUtc(), humanDuration(Duration.ofSeconds(spec.intervalSeconds())));
            case DAILY_LOCAL -> "每日本地时间：%s 时区每天 %02d:%02d 触发（锚定当地墙上时间，DST 当天可能为 23/25 小时，空档时刻会被跳过）。"
                    .formatted(spec.zoneId(), spec.hour(), spec.minute());
        };
    }

    private record ZonedParts(LocalDateTime local, ZoneOffset offset) {
        ZonedParts(Instant instant, ZoneId zone) {
            this(LocalDateTime.ofInstant(instant, zone),
                    zone.getRules().getOffset(instant));
        }
    }
}
