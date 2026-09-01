package com.smartagriculture.aiadvisorservice.dto;

import com.smartagriculture.aiadvisorservice.entity.FarmEvent;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

public class FarmEventDto {

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class Response {
        private String id;
        private String farmerId;
        private String farmAssetId;
        private FarmEvent.EventType eventType;
        private String description;
        private String diagnosticCaseId;
        private LocalDateTime occurredAt;
    }
}
