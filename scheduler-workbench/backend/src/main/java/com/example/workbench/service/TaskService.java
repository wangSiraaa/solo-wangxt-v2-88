package com.example.workbench.service;

import com.example.workbench.model.TaskDefinition;
import com.example.workbench.model.TriggerInstance;
import com.example.workbench.quartz.QuartzSyncService;
import com.example.workbench.repo.TaskDefinitionRepository;
import com.example.workbench.repo.TriggerInstanceRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

@Service
public class TaskService {

    private final TaskDefinitionRepository taskRepo;
    private final TriggerInstanceRepository instanceRepo;
    private final InstanceGenerator generator;
    private final QuartzSyncService quartz;
    private final MutableClock clock;

    public TaskService(TaskDefinitionRepository taskRepo, TriggerInstanceRepository instanceRepo,
                       InstanceGenerator generator, QuartzSyncService quartz, MutableClock clock) {
        this.taskRepo = taskRepo;
        this.instanceRepo = instanceRepo;
        this.generator = generator;
        this.quartz = quartz;
        this.clock = clock;
    }

    public List<TaskDefinition> list() {
        return taskRepo.findAll();
    }

    @Transactional
    public TaskDefinition create(TaskDefinition task) {
        TaskDefinition saved = taskRepo.save(task);
        quartz.sync(saved);
        return saved;
    }

    @Transactional
    public void delete(UUID id) {
        taskRepo.findById(id).ifPresent(t -> {
            quartz.remove(t);
            instanceRepo.deleteByTaskId(id);
            taskRepo.delete(t);
        });
    }

    @Transactional
    public TaskDefinition setPaused(UUID id, boolean paused) {
        TaskDefinition task = taskRepo.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("任务不存在: " + id));
        task.setPaused(paused);
        if (paused) {
            quartz.pause(task);
        } else {
            quartz.resume(task);
        }
        return taskRepo.save(task);
    }

    /** 预览：只计算不落库 */
    public List<TriggerInstance> preview(UUID id, int count) {
        TaskDefinition task = taskRepo.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("任务不存在: " + id));
        return generator.generate(task, clock.instant(), count);
    }

    /** 生成并持久化未来实例（先清除旧的） */
    @Transactional
    public List<TriggerInstance> generateAndStore(UUID id, int count) {
        TaskDefinition task = taskRepo.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("任务不存在: " + id));
        instanceRepo.deleteByTaskId(id);
        List<TriggerInstance> instances = generator.generate(task, clock.instant(), count);
        return instanceRepo.saveAll(instances);
    }

    public List<TriggerInstance> instances(UUID id) {
        return instanceRepo.findByTaskIdOrderBySeqAsc(id);
    }

    public Instant now() {
        return clock.instant();
    }
}
