package com.smartagriculture.aiadvisorservice.dto;

import com.smartagriculture.aiadvisorservice.entity.DiagnosticCase;
import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

public class DiagnosticCaseDto {

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class OutcomeRequest {

        @NotNull(message = "Outcome is required")
        private DiagnosticCase.Outcome outcome;
    }

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class Response {
        private String id;
        private String conversationId;
        private String farmerId;
        private String farmAssetId;
        private String symptom;
        private String hypothesis;
        private DiagnosticCase.ConfidenceLevel confidenceLevel;
        private DiagnosticCase.RiskLevel riskLevel;
        private DiagnosticCase.CaseStatus status;
        private String recommendedAction;
        private LocalDateTime followUpDueAt;
        private DiagnosticCase.Outcome outcome;
        private LocalDateTime createdAt;
        private LocalDateTime updatedAt;
    }
}
