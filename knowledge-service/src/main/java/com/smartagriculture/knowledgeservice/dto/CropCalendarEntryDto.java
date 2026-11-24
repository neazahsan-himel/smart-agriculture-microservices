package com.smartagriculture.knowledgeservice.dto;

import com.smartagriculture.knowledgeservice.entity.KnowledgeEnums;
import jakarta.validation.constraints.*;
import lombok.*;

import java.time.LocalDateTime;

public class CropCalendarEntryDto {

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

        @Size(max = 255, message = "Region must be at most 255 characters")
        private String region;

        @NotNull(message = "Activity is required")
        private KnowledgeEnums.CalendarActivity activity;

        @NotNull(message = "Start month is required")
        @Min(value = 1, message = "Start month must be 1-12")
        @Max(value = 12, message = "Start month must be 1-12")
        private Integer startMonth;

        @NotNull(message = "Start day is required")
        @Min(value = 1, message = "Start day must be 1-31")
        @Max(value = 31, message = "Start day must be 1-31")
        private Integer startDay;

        @NotNull(message = "End month is required")
        @Min(value = 1, message = "End month must be 1-12")
        @Max(value = 12, message = "End month must be 1-12")
        private Integer endMonth;

        @NotNull(message = "End day is required")
        @Min(value = 1, message = "End day must be 1-31")
        @Max(value = 31, message = "End day must be 1-31")
        private Integer endDay;

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
        private KnowledgeEnums.CalendarActivity activity;
        private Integer startMonth;
        private Integer startDay;
        private Integer endMonth;
        private Integer endDay;
        private String notes;
        private KnowledgeSourceDto.SourceRef source;
        private KnowledgeEnums.RecordStatus status;
        private LocalDateTime createdAt;
        private LocalDateTime updatedAt;
    }
}
