package com.example.workbench.quartz;

import com.example.workbench.model.TaskDefinition;
import com.example.workbench.repo.TaskDefinitionRepository;
import com.example.workbench.repo.TriggerInstanceRepository;
import com.example.workbench.service.MutableClock;
import org.quartz.Job;
import org.quartz.JobExecutionContext;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import java.time.Instant;

/**
 * Quartz 触发入口：任务到点时把该任务下一条到期实例标记为已触发。
 * 触发时刻表（含夏令时跳过/重复语义）由 InstanceGenerator 预先生成并持久化，
 * Quartz 负责按 UTC 时刻准时唤起。
 */
@Component
public class TaskFireJob implements Job {

    private static final Logger log = LoggerFactory.getLogger(TaskFireJob.class);

    private final TaskDefinitionRepository taskRepo;
    private final TriggerInstanceRepository instanceRepo;
    private final MutableClock clock;

    public TaskFireJob(TaskDefinitionRepository taskRepo, TriggerInstanceRepository instanceRepo,
                       MutableClock clock) {
        this.taskRepo = taskRepo;
        this.instanceRepo = instanceRepo;
        this.clock = clock;
    }

    @Override
    public void execute(JobExecutionContext context) {
        var taskId = java.util.UUID.fromString(context.getJobDetail().getKey().getName());
        TaskDefinition task = taskRepo.findById(taskId).orElse(null);
        if (task == null || task.isPaused()) {
            return;
        }
        Instant now = clock.instant();
        instanceRepo.findByTaskIdOrderBySeqAsc(taskId).stream()
                .filter(i -> i.getFiredAt() == null && i.getFireTimeUtc() != null
                        && !i.getFireTimeUtc().isAfter(now))
                .findFirst()
                .ifPresent(instance -> {
                    instance.setFiredAt(now);
                    instanceRepo.save(instance);
                    log.info("[FIRE] task={} seq={} plannedUtc={} status={}",
                            task.getName(), instance.getSeq(), instance.getFireTimeUtc(),
                            instance.getStatus());
                });
    }
}
