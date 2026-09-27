package com.example.workbench.repo;

import com.example.workbench.model.TriggerInstance;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.UUID;

public interface TriggerInstanceRepository extends JpaRepository<TriggerInstance, UUID> {

    List<TriggerInstance> findByTaskIdOrderBySeqAsc(UUID taskId);

    void deleteByTaskId(UUID taskId);
}
