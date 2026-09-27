package com.example.cycleworkbench.task;

import com.example.cycleworkbench.clock.MutableClock;
import com.example.cycleworkbench.schedule.ScheduleSpec;
import com.example.cycleworkbench.schedule.TaskExecutorJob;
import com.example.cycleworkbench.schedule.TimelineGenerator;
import com.example.cycleworkbench.schedule.TimelineItem;
import com.example.cycleworkbench.schedule.TimelinePreview;
import com.example.cycleworkbench.schedule.TriggerFactory;
import java.time.Instant;
import java.util.List;
import org.quartz.JobBuilder;
import org.quartz.JobKey;
import org.quartz.Scheduler;
import org.quartz.SchedulerException;
import org.quartz.Trigger;
import org.quartz.TriggerKey;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class TaskService {

    private final TaskRepository tasks;
    private final TriggerInstanceRepository instances;
    private final TriggerFactory triggerFactory;
    private final TimelineGenerator timelineGenerator;
    private final Scheduler scheduler;
    private final MutableClock clock;

    public TaskService(TaskRepository tasks, TriggerInstanceRepository instances,
                       TriggerFactory triggerFactory, TimelineGenerator timelineGenerator,
                       Scheduler scheduler, MutableClock clock) {
        this.tasks = tasks;
        this.instances = instances;
        this.triggerFactory = triggerFactory;
        this.timelineGenerator = timelineGenerator;
        this.scheduler = scheduler;
        this.clock = clock;
    }

    @Transactional
    public TaskDefinition create(String name, ScheduleSpec spec, int instanceLimit) {
        Instant now = clock.instant();
        TaskDefinition task = new TaskDefinition();
        task.setName(name);
        task.setSchedule(spec);
        task.setState(TaskState.ACTIVE);
        task.setCreatedAt(now);
        task.setUpdatedAt(now);
        tasks.save(task);
        registerQuartzJob(task);
        regenerateInstances(task, instanceLimit);
        return task;
    }

    @Transactional
    public TaskDefinition update(long id, String name, ScheduleSpec spec, Integer limit) {
        TaskDefinition task = get(id);
        task.setName(name);
        task.setSchedule(spec);
        task.setUpdatedAt(clock.instant());
        tasks.update(task);
        reschedule(task);
        if (limit != null) {
            regenerateInstances(task, limit);
        }
        return task;
    }

    public void pause(long id) {
        TaskDefinition task = get(id);
        task.setState(TaskState.PAUSED);
        task.setUpdatedAt(clock.instant());
        tasks.update(task);
        try {
            scheduler.pauseTrigger(triggerKey(id));
        } catch (SchedulerException e) {
            throw new IllegalStateException("Could not pause trigger for task " + id, e);
        }
    }

    public void resume(long id) {
        TaskDefinition task = get(id);
        task.setState(TaskState.ACTIVE);
        task.setUpdatedAt(clock.instant());
        tasks.update(task);
        try {
            // Resume lets Quartz apply its misfire policy for anything missed while paused.
            scheduler.resumeTrigger(triggerKey(id));
        } catch (SchedulerException e) {
            throw new IllegalStateException("Could not resume trigger for task " + id, e);
        }
    }

    @Transactional
    public void delete(long id) {
        try {
            scheduler.deleteJob(jobKey(id));
        } catch (SchedulerException e) {
            throw new IllegalStateException("Could not delete Quartz job for task " + id, e);
        }
        tasks.delete(id);
    }

    public List<TaskDefinition> list() {
        return tasks.findAll();
    }

    public TaskDefinition get(long id) {
        return tasks.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("任务不存在: " + id));
    }

    public List<TriggerInstanceRow> instances(long id) {
        get(id);
        return instances.findByTask(id);
    }

    public TimelinePreview preview(ScheduleSpec spec, int limit, String viewZoneId) {
        return timelineGenerator.generate(spec, limit, viewZoneId);
    }

    @Transactional
    public List<TriggerInstanceRow> regenerate(long id, int limit) {
        TaskDefinition task = get(id);
        return regenerateInstances(task, limit);
    }

    private List<TriggerInstanceRow> regenerateInstances(TaskDefinition task, int limit) {
        TimelinePreview preview = timelineGenerator.generate(task.getSchedule(), limit, null);
        instances.replaceForTask(task.getId(), preview.items(), clock.instant());
        return instances.findByTask(task.getId());
    }

    private void registerQuartzJob(TaskDefinition task) {
        try {
            var job = JobBuilder.newJob(TaskExecutorJob.class)
                    .withIdentity(jobKey(task.getId()))
                    .usingJobData(TaskExecutorJob.data(task.getId()))
                    .storeDurably(false)
                    .build();
            Trigger trigger = triggerFactory.build(task.getSchedule(), "trigger-" + task.getId());
            scheduler.scheduleJob(job, trigger);
            if (task.getState() == TaskState.PAUSED) {
                scheduler.pauseTrigger(triggerKey(task.getId()));
            }
        } catch (SchedulerException e) {
            throw new IllegalStateException("Could not register Quartz job for task " + task.getId(), e);
        }
    }

    private void reschedule(TaskDefinition task) {
        try {
            Trigger trigger = triggerFactory.build(task.getSchedule(), "trigger-" + task.getId());
            scheduler.rescheduleJob(triggerKey(task.getId()), trigger);
            if (task.getState() == TaskState.PAUSED) {
                scheduler.pauseTrigger(triggerKey(task.getId()));
            }
        } catch (SchedulerException e) {
            throw new IllegalStateException("Could not reschedule task " + task.getId(), e);
        }
    }

    private JobKey jobKey(long id) {
        return new JobKey("task-" + id, "cycle-workbench");
    }

    private TriggerKey triggerKey(long id) {
        return new TriggerKey("trigger-" + id, "cycle-workbench");
    }
}
