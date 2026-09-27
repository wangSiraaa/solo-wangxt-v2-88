package com.example.workbench.config;

import com.example.workbench.quartz.QuartzSyncService;
import com.example.workbench.repo.TaskDefinitionRepository;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.stereotype.Component;

/** 重启后把数据库中的任务重新同步进 Quartz（内存 JobStore）。 */
@Component
public class QuartzBootstrap implements ApplicationRunner {

    private final TaskDefinitionRepository taskRepo;
    private final QuartzSyncService quartz;

    public QuartzBootstrap(TaskDefinitionRepository taskRepo, QuartzSyncService quartz) {
        this.taskRepo = taskRepo;
        this.quartz = quartz;
    }

    @Override
    public void run(ApplicationArguments args) {
        taskRepo.findAll().forEach(quartz::sync);
    }
}
