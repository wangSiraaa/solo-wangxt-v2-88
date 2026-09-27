package com.example.cycleworkbench.schedule;

import com.example.cycleworkbench.task.TaskState;
import com.example.cycleworkbench.task.TriggerInstanceRepository;
import java.time.Instant;
import org.quartz.JobExecutionContext;
import org.quartz.JobExecutionException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.scheduling.quartz.QuartzJobBean;

/**
 * Lightweight Quartz job: records that a generated instance fired. The trigger (not this job)
 * carries all the recurrence semantics, including time-zone daily rules and DST handling.
 */
public class TaskExecutorJob extends QuartzJobBean {

    private static final Logger log = LoggerFactory.getLogger(TaskExecutorJob.class);

    @Override
    protected void executeInternal(JobExecutionContext context) throws JobExecutionException {
        long taskId = context.getMergedJobDataMap().getLong("taskId");
        Instant firedAt = context.getScheduledFireTime().toInstant();
        TriggerInstanceRepository repository = SpringLookup.get(TriggerInstanceRepository.class);
        repository.markFired(taskId, firedAt);
        log.info("task {} fired at {} (state={})", taskId, firedAt, TaskState.ACTIVE);
        // A real worker would dispatch the payload here; the workbench focuses on timing.
    }

    public static org.quartz.JobDataMap data(long taskId) {
        org.quartz.JobDataMap map = new org.quartz.JobDataMap();
        map.put("taskId", taskId);
        return map;
    }
}
