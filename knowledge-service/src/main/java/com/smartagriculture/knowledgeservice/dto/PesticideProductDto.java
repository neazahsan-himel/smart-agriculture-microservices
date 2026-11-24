package com.smartagriculture.knowledgeservice.dto;

import com.smartagriculture.knowledgeservice.entity.KnowledgeEnums;
import jakarta.validation.constraints.*;
import lombok.*;

import java.time.LocalDateTime;

public class PesticideProductDto {

    @Getter
    @Setter
    @NoArgsConstructor
    @AllArgsConstructor
    @Builder
    public static class Request {

        @NotBlank(message = "Trade name is required")
        @Size(max = 255, message = "Trade name must be at most 255 characters")
        private String tradeName;

        @NotBlank(message = "Active ingredient is required")
        @Size(max = 255, message = "Active ingredient must be at most 255 characters")
        private String activeIngredient;

        @Size(max = 255, message = "Formulation must be at most 255 characters")
        private String formulation;

        @Size(max = 255, message = "Registration number must be at most 255 characters")
        private String registrationNo;

        @NotBlank(message = "Crop is required")
        @Size(max = 100, message = "Crop must be at most 100 characters")
        private String crop;

        @NotBlank(message = "Target pest/disease is required")
        @Size(max = 255, message = "Target must be at most 255 characters")
        private String target;

        @Positive(message = "Dose must be a positive number")
        private Double doseValue;

        @Size(max = 50, message = "Dose unit must be at most 50 characters")
        private String doseUnit;

        @Size(max = 255, message = "Dose basis must be at most 255 characters")
        private String doseBasis;

        @PositiveOrZero(message = "Pre-harvest interval must be zero or positive")
        private Integer preHarvestIntervalDays;

        @Size(max = 255, message = "Toxicity class must be at most 255 characters")
        private String toxicityClass;

        private KnowledgeEnums.RegistrationStatus registrationStatus;

        @Size(max = 1000, message = "Notes must be at most 1000 characters")
        private String notes;

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
        private String tradeName;
        private String activeIngredient;
        private String formulation;
        private String registrationNo;
        private String crop;
        private String target;
        private Double doseValue;
        private String doseUnit;
        private String doseBasis;
        private Integer preHarvestIntervalDays;
        private String toxicityClass;
        private KnowledgeEnums.RegistrationStatus registrationStatus;
        private String notes;
        private KnowledgeSourceDto.SourceRef source;
        private KnowledgeEnums.RecordStatus status;
        private LocalDateTime createdAt;
        private LocalDateTime updatedAt;
    }
}
