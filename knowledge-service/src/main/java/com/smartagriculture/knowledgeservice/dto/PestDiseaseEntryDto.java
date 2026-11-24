package com.smartagriculture.knowledgeservice.dto;

import com.smartagriculture.knowledgeservice.entity.KnowledgeEnums;
import jakarta.validation.constraints.*;
import lombok.*;

import java.time.LocalDateTime;

public class PestDiseaseEntryDto {

    @Getter
    @Setter
    @NoArgsConstructor
    @AllArgsConstructor
    @Builder
    public static class Request {

        @NotBlank(message = "Crop is required")
        @Size(max = 100, message = "Crop must be at most 100 characters")
        private String crop;

        @NotNull(message = "Kind is required")
        private KnowledgeEnums.PestDiseaseKind kind;

        @NotBlank(message = "English name is required")
        @Size(max = 255, message = "English name must be at most 255 characters")
        private String nameEn;

        @Size(max = 255, message = "Bangla name must be at most 255 characters")
        private String nameBn;

        @Size(max = 2000, message = "Symptoms must be at most 2000 characters")
        private String symptoms;

        @Size(max = 1000, message = "Favourable conditions must be at most 1000 characters")
        private String favourableConditions;

        @Size(max = 500, message = "Action threshold must be at most 500 characters")
        private String actionThreshold;

        @Size(max = 2000, message = "Cultural control must be at most 2000 characters")
        private String culturalControl;

        @Size(max = 1000, message = "Chemical control must be at most 1000 characters")
        private String chemicalControlActiveIngredients;

        @NotBlank(message = "Source ID is required: every fact must cite its source")
        private String sourceId;

        @Size(max = 50, message = "Source page must be at most 50 characters")
        private String sourcePage;
    }

    @Getter
    @Setter
    @NoArgsConstructor
    @AllArgsConstructor
    @Builder
    public static class Response {
        private String id;
        private String crop;
        private KnowledgeEnums.PestDiseaseKind kind;
        private String nameEn;
        private String nameBn;
        private String symptoms;
        private String favourableConditions;
        private String actionThreshold;
        private String culturalControl;
        private String chemicalControlActiveIngredients;
        private KnowledgeSourceDto.SourceRef source;
        private KnowledgeEnums.RecordStatus status;
        private LocalDateTime createdAt;
        private LocalDateTime updatedAt;
    }
}
