package com.example.workbench.quartz;

import com.example.workbench.model.ScheduleType;
import com.example.workbench.model.TaskDefinition;
import org.quartz.CronScheduleBuilder;
import org.quartz.JobBuilder;
import org.quartz.JobDetail;
import org.quartz.JobKey;
import org.quartz.Scheduler;
import org.quartz.SchedulerException;
import org.quartz.SimpleScheduleBuilder;
import org.quartz.Trigger;
import org.quartz.TriggerBuilder;
import org.springframework.stereotype.Service;

import java.util.Date;
import java.util.TimeZone;

/**
 * 把任务定义同步进 Quartz 调度器（内存 JobStore，重启后由数据库重建）。
 */
@Service
public class QuartzSyncService {

    private final Scheduler scheduler;

    public QuartzSyncService(Scheduler scheduler) {
        this.scheduler = scheduler;
    }

    public void sync(TaskDefinition task) {
        try {
            JobKey key = JobKey.jobKey(task.getId().toString());
            scheduler.deleteJob(key);

            JobDetail job = JobBuilder.newJob(TaskFireJob.class)
                    .withIdentity(key)
                    .storeDurably(false)
                    .build();

            Trigger trigger;
            if (task.getScheduleType() == ScheduleType.INTERVAL) {
                trigger = TriggerBuilder.newTrigger()
                        .forJob(job)
                        .startAt(Date.from(task.getCreatedAt().plusSeconds(task.getIntervalSeconds())))
                        .withSchedule(SimpleScheduleBuilder.simpleSchedule()
                                .withIntervalInSeconds(task.getIntervalSeconds().intValue())
                                .repeatForever()
                                .withMisfireHandlingInstructionNextWithRemainingCount())
                        .build();
            } else {
                trigger = TriggerBuilder.newTrigger()
                        .forJob(job)
                        .withSchedule(CronScheduleBuilder
                                .dailyAtHourAndMinute(task.getDailyTime().getHour(),
                                        task.getDailyTime().getMinute())
                                .inTimeZone(TimeZone.getTimeZone(task.getTimezone()))
                                .withMisfireHandlingInstructionDoNothing())
                        .build();
            }
            scheduler.scheduleJob(job, trigger);
            if (task.isPaused()) {
                scheduler.pauseJob(key);
            }
        } catch (SchedulerException e) {
            throw new IllegalStateException("同步 Quartz 调度失败", e);
        }
    }

    public void pause(TaskDefinition task) {
        try {
            scheduler.pauseJob(JobKey.jobKey(task.getId().toString()));
        } catch (SchedulerException e) {
            throw new IllegalStateException("暂停失败", e);
        }
    }

    public void resume(TaskDefinition task) {
        try {
            scheduler.resumeJob(JobKey.jobKey(task.getId().toString()));
        } catch (SchedulerException e) {
            throw new IllegalStateException("恢复失败", e);
        }
    }

    public void remove(TaskDefinition task) {
        try {
            scheduler.deleteJob(JobKey.jobKey(task.getId().toString()));
        } catch (SchedulerException e) {
            throw new IllegalStateException("移除调度失败", e);
        }
    }
}
