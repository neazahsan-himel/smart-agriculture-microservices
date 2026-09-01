package com.smartagriculture.aiadvisorservice.dto;

import com.smartagriculture.aiadvisorservice.entity.FarmTask;
import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

public class FarmTaskDto {

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class StatusUpdateRequest {

        @NotNull(message = "Status is required")
        private FarmTask.TaskStatus status;
    }

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class Response {
        private String id;
        private String farmerId;
        private String farmAssetId;
        private String taskType;
        private LocalDateTime dueDate;
        private FarmTask.TaskPriority priority;
        private Boolean weatherDependent;
        private FarmTask.TaskStatus status;
        private String sourceDiagnosticCaseId;
    }
}
