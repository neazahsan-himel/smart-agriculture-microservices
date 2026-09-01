package com.smartagriculture.aiadvisorservice.dto;

import com.smartagriculture.aiadvisorservice.entity.Conversation;
import com.smartagriculture.aiadvisorservice.entity.DiagnosticCase;
import com.smartagriculture.aiadvisorservice.entity.Message;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;
import java.util.List;

public class ConversationDto {

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class StartRequest {

        @NotBlank(message = "Farmer ID is required")
        private String farmerId;

        private String farmAssetId;

        @NotNull(message = "Query type is required")
        private AdvisorDto.QueryType queryType;

        @NotBlank(message = "Message is required")
        @Size(max = 1000, message = "Message must not exceed 1000 characters")
        private String message;
    }

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class MessageRequest {

        @NotBlank(message = "Message is required")
        @Size(max = 1000, message = "Message must not exceed 1000 characters")
        private String message;
    }

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class Response {
        private String id;
        private String farmerId;
        private String farmAssetId;
        private AdvisorDto.QueryType topic;
        private Conversation.ConversationStatus status;
        private List<MessageView> messages;
        private DiagnosticCaseView latestCase;
        private LocalDateTime createdAt;
        private LocalDateTime updatedAt;
    }

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class MessageView {
        private String id;
        private Message.Sender sender;
        private String content;
        private LocalDateTime createdAt;
    }

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class DiagnosticCaseView {
        private String id;
        private DiagnosticCase.ConfidenceLevel confidenceLevel;
        private DiagnosticCase.RiskLevel riskLevel;
        private DiagnosticCase.CaseStatus status;
        private String recommendedAction;
        private LocalDateTime followUpDueAt;
        private DiagnosticCase.Outcome outcome;
    }
}
