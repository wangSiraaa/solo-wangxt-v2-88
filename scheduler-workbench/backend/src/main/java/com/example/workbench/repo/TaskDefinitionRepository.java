package com.example.workbench.repo;

import com.example.workbench.model.TaskDefinition;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.UUID;

public interface TaskDefinitionRepository extends JpaRepository<TaskDefinition, UUID> {
}
