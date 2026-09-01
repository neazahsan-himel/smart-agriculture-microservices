package com.smartagriculture.aiadvisorservice.repository;

import com.smartagriculture.aiadvisorservice.entity.FarmTask;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

public interface FarmTaskRepository extends JpaRepository<FarmTask, String> {

    Optional<FarmTask> findByIdAndDeletedFalse(String id);

    Optional<FarmTask> findBySourceDiagnosticCaseIdAndDeletedFalse(String sourceDiagnosticCaseId);

    Page<FarmTask> findByFarmerIdAndDeletedFalse(String farmerId, Pageable pageable);

    List<FarmTask> findByStatusAndWeatherDependentFalseAndDueDateLessThanEqual(
            FarmTask.TaskStatus status, LocalDateTime now);
}
