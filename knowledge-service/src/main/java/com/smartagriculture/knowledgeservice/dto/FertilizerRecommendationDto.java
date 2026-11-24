package com.smartagriculture.knowledgeservice.dto;

import com.smartagriculture.knowledgeservice.entity.KnowledgeEnums;
import jakarta.validation.constraints.*;
import lombok.*;

import java.time.LocalDateTime;

public class FertilizerRecommendationDto {

    @Getter
    @Setter
    @NoArgsConstructor
    @AllArgsConstructor
    @Builder
    public static class Request {

        @NotBlank(message = "Crop is required")
        @Size(max = 100, message = "Crop must be at most 100 characters")
        private String crop;

        private KnowledgeEnums.CropSeason season;

        @Size(max = 255, message = "Region/zone must be at most 255 characters")
        private String region;

        @Size(max = 100, message = "Soil fertility class must be at most 100 characters")
        private String soilFertilityClass;

        @PositiveOrZero(message = "Urea dose must be zero or positive")
        private Double ureaKgPerHa;

        @PositiveOrZero(message = "TSP dose must be zero or positive")
        private Double tspKgPerHa;

        @PositiveOrZero(message = "MoP dose must be zero or positive")
        private Double mopKgPerHa;

        @PositiveOrZero(message = "Gypsum dose must be zero or positive")
        private Double gypsumKgPerHa;

        @PositiveOrZero(message = "Zinc sulphate dose must be zero or positive")
        private Double zincSulphateKgPerHa;

        @Size(max = 1000, message = "Split schedule must be at most 1000 characters")
        private String splitSchedule;

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
        private String crop;
        private KnowledgeEnums.CropSeason season;
        private String region;
        private String soilFertilityClass;
        private Double ureaKgPerHa;
        private Double tspKgPerHa;
        private Double mopKgPerHa;
        private Double gypsumKgPerHa;
        private Double zincSulphateKgPerHa;
        private String splitSchedule;
        private String notes;
        private KnowledgeSourceDto.SourceRef source;
        private KnowledgeEnums.RecordStatus status;
        private LocalDateTime createdAt;
        private LocalDateTime updatedAt;
    }
}
