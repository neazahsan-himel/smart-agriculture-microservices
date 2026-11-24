package com.smartagriculture.knowledgeservice.dto;

import com.smartagriculture.knowledgeservice.entity.KnowledgeEnums;
import jakarta.validation.constraints.*;
import lombok.*;

import java.time.LocalDate;
import java.time.LocalDateTime;

public class KnowledgeSourceDto {

    @Getter
    @Setter
    @NoArgsConstructor
    @AllArgsConstructor
    @Builder
    public static class Request {

        @NotBlank(message = "Title is required")
        @Size(max = 300, message = "Title must be at most 300 characters")
        private String title;

        @NotBlank(message = "Publisher is required")
        @Size(max = 255, message = "Publisher must be at most 255 characters")
        private String publisher;

        @Size(max = 255, message = "Edition must be at most 255 characters")
        private String edition;

        @Min(value = 1900, message = "Publication year must be 1900 or later")
        @Max(value = 2100, message = "Publication year must be 2100 or earlier")
        private Integer publicationYear;

        @Size(max = 1000, message = "URL must be at most 1000 characters")
        private String url;

        @Size(max = 5, message = "Language must be a short code such as en or bn")
        private String language;

        @Size(max = 500, message = "Licence note must be at most 500 characters")
        private String licenceNote;

        private LocalDate retrievedAt;
    }

    @Getter
    @Setter
    @NoArgsConstructor
    @AllArgsConstructor
    @Builder
    public static class Response {
        private String id;
        private String title;
        private String publisher;
        private String edition;
        private Integer publicationYear;
        private String url;
        private String language;
        private String licenceNote;
        private LocalDate retrievedAt;
        private KnowledgeEnums.RecordStatus status;
        private LocalDateTime createdAt;
        private LocalDateTime updatedAt;
    }

    /** Short citation embedded in every fact response, so callers can show "Source: …" without a second call. */
    @Getter
    @Setter
    @NoArgsConstructor
    @AllArgsConstructor
    @Builder
    public static class SourceRef {
        private String id;
        private String title;
        private String publisher;
        private String edition;
        private Integer publicationYear;
        private String page;
    }
}
