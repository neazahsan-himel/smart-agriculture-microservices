package com.smartagriculture.aiadvisorservice.dto;

import com.smartagriculture.aiadvisorservice.entity.FarmTask;
import com.smartagriculture.aiadvisorservice.entity.WeatherTaskType;
import jakarta.validation.constraints.NotBlank;
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
    public static class Request {
        @NotBlank(message = "Farmer ID is required")
        private String farmerId;

        private String farmAssetId;

        @NotNull(message = "Task type is required")
        private WeatherTaskType taskType;

        // When the farmer plans to do it; the best-time search covers 24h before to 48h after
        @NotNull(message = "Due date is required")
        private LocalDateTime dueDate;

        private FarmTask.TaskPriority priority;
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
        private LocalDateTime recommendedStartAt;
        private String weatherAdvice;
        private LocalDateTime weatherCheckedAt;
    }
}
