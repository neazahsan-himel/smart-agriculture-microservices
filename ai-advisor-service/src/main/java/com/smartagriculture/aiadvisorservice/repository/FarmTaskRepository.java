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

    // A diagnostic case can now source both its FOLLOW_UP task and advisor-suggested weather tasks,
    // so the lookup must be scoped by task type to stay single-result.
    Optional<FarmTask> findBySourceDiagnosticCaseIdAndTaskTypeAndDeletedFalse(String sourceDiagnosticCaseId,
                                                                             String taskType);

    // Duplicate check / upsert key for weather tasks (null farmAssetId matches IS NULL)
    Optional<FarmTask> findFirstByFarmerIdAndFarmAssetIdAndTaskTypeAndStatusAndDeletedFalse(
            String farmerId, String farmAssetId, String taskType, FarmTask.TaskStatus status);

    List<FarmTask> findByStatusAndWeatherDependentTrueAndDeletedFalseAndDueDateLessThanEqual(
            FarmTask.TaskStatus status, LocalDateTime horizon);

    Page<FarmTask> findByFarmerIdAndDeletedFalse(String farmerId, Pageable pageable);

        List<FarmTask> findByStatusAndWeatherDependentFalseAndDeletedFalseAndDueDateLessThanEqual(
            FarmTask.TaskStatus status, LocalDateTime now);
}
