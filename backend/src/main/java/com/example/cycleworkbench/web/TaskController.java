package com.example.cycleworkbench.web;

import com.example.cycleworkbench.task.TaskDefinition;
import com.example.cycleworkbench.task.TaskService;
import com.example.cycleworkbench.task.TriggerInstanceRow;
import com.example.cycleworkbench.schedule.TimelinePreview;
import java.util.List;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/tasks")
public class TaskController {

    private final TaskService service;

    public TaskController(TaskService service) {
        this.service = service;
    }

    @GetMapping
    public List<TaskDefinition> list() {
        return service.list();
    }

    @GetMapping("/{id}")
    public TaskDefinition get(@PathVariable long id) {
        return service.get(id);
    }

    @PostMapping
    public TaskDefinition create(@RequestBody TaskCreateRequest request) {
        validateName(request.name());
        int limit = request.instances() == null ? 14 : request.instances();
        return service.create(request.name(), request.schedule().toSpec(), limit);
    }

    @PutMapping("/{id}")
    public TaskDefinition update(@PathVariable long id, @RequestBody TaskUpdateRequest request) {
        validateName(request.name());
        return service.update(id, request.name(), request.schedule().toSpec(), request.instances());
    }

    @DeleteMapping("/{id}")
    public void delete(@PathVariable long id) {
        service.delete(id);
    }

    @PostMapping("/{id}/pause")
    public TaskDefinition pause(@PathVariable long id) {
        service.pause(id);
        return service.get(id);
    }

    @PostMapping("/{id}/resume")
    public TaskDefinition resume(@PathVariable long id) {
        service.resume(id);
        return service.get(id);
    }

    @PostMapping("/preview")
    public TimelinePreview preview(@RequestBody PreviewRequest request) {
        int limit = request.limit() == null ? 14 : request.limit();
        return service.preview(request.schedule().toSpec(), limit, request.viewZoneId());
    }

    @GetMapping("/{id}/instances")
    public List<TriggerInstanceRow> instances(@PathVariable long id) {
        return service.instances(id);
    }

    @PostMapping("/{id}/instances/generate")
    public List<TriggerInstanceRow> generate(@PathVariable long id,
                                             @RequestParam(defaultValue = "14") int limit) {
        return service.regenerate(id, Math.max(1, Math.min(limit, 366)));
    }

    private static void validateName(String name) {
        if (name == null || name.isBlank()) {
            throw new IllegalArgumentException("任务名称不能为空");
        }
    }
}
