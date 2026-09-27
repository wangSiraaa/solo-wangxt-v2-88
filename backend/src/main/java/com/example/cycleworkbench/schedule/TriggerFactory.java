package com.example.cycleworkbench.schedule;

import static org.quartz.CronScheduleBuilder.dailyAtHourAndMinute;
import static org.quartz.SimpleScheduleBuilder.simpleSchedule;

import java.util.Date;
import java.util.TimeZone;
import org.quartz.Trigger;
import org.quartz.TriggerBuilder;
import org.springframework.stereotype.Component;

/** Builds the Quartz {@link Trigger} that represents a {@link ScheduleSpec}. */
@Component
public class TriggerFactory {

    public Trigger build(ScheduleSpec spec, String identity) {
        Date startAt = Date.from(spec.anchorUtc());
        return switch (spec.kind()) {
            case FIXED_INTERVAL -> TriggerBuilder.newTrigger()
                    .withIdentity(identity, "cycle-workbench")
                    .startAt(startAt)
                    .withSchedule(simpleSchedule()
                            .withIntervalInSeconds(spec.intervalSeconds().intValue())
                            .repeatForever()
                            .withMisfireHandlingInstructionNextWithExistingCount())
                    .build();
            case DAILY_LOCAL -> TriggerBuilder.newTrigger()
                    .withIdentity(identity, "cycle-workbench")
                    .startAt(startAt)
                    .withSchedule(dailyAtHourAndMinute(spec.hour(), spec.minute())
                            .inTimeZone(TimeZone.getTimeZone(spec.zoneId()))
                            .withMisfireHandlingInstructionDoNothing())
                    .build();
        };
    }
}
