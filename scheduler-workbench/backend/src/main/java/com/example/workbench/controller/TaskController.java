package com.example.workbench.controller;

import com.example.workbench.model.TaskDefinition;
import com.example.workbench.model.TriggerInstance;
import com.example.workbench.service.TaskService;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;

import java.time.ZoneId;
import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/api/tasks")
@CrossOrigin(origins = "*")
public class TaskController {

    private final TaskService service;

    public TaskController(TaskService service) {
        this.service = service;
    }

    @GetMapping
    public List<TaskDefinition> list() {
        return service.list();
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public TaskDefinition create(@Valid @RequestBody TaskRequest req) {
        validate(req);
        var task = new TaskDefinition(UUID.randomUUID(), req.name(), req.scheduleType(),
                req.intervalSeconds(), req.dailyTime(), req.timezone(), service.now());
        return service.create(task);
    }

    @DeleteMapping("/{id}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void delete(@PathVariable UUID id) {
        service.delete(id);
    }

    @PostMapping("/{id}/pause")
    public TaskDefinition pause(@PathVariable UUID id) {
        return service.setPaused(id, true);
    }

    @PostMapping("/{id}/resume")
    public TaskDefinition resume(@PathVariable UUID id) {
        return service.setPaused(id, false);
    }

    /** 预览未来实例（不落库）。count：DAILY 为天数，INTERVAL 为条数。 */
    @GetMapping("/{id}/preview")
    public List<TriggerInstance> preview(@PathVariable UUID id,
                                         @RequestParam(defaultValue = "14") int count) {
        return service.preview(id, Math.min(count, 400));
    }

    /** 生成未来实例并持久化到 PostgreSQL */
    @PostMapping("/{id}/generate")
    public List<TriggerInstance> generate(@PathVariable UUID id,
                                          @RequestParam(defaultValue = "14") int count) {
        return service.generateAndStore(id, Math.min(count, 400));
    }

    @GetMapping("/{id}/instances")
    public List<TriggerInstance> instances(@PathVariable UUID id) {
        return service.instances(id);
    }

    private void validate(TaskRequest req) {
        try {
            ZoneId.of(req.timezone());
        } catch (Exception e) {
            throw new IllegalArgumentException("非法时区: " + req.timezone());
        }
        switch (req.scheduleType()) {
            case INTERVAL -> {
                if (req.intervalSeconds() == null) {
                    throw new IllegalArgumentException("INTERVAL 类型必须提供 intervalSeconds");
                }
            }
            case DAILY -> {
                if (req.dailyTime() == null) {
                    throw new IllegalArgumentException("DAILY 类型必须提供 dailyTime");
                }
            }
        }
    }
}
