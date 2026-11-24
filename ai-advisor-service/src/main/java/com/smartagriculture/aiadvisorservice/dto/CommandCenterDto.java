package com.smartagriculture.aiadvisorservice.dto;

import com.smartagriculture.aiadvisorservice.entity.DiagnosticCase;
import com.smartagriculture.aiadvisorservice.entity.FarmEvent;
import com.smartagriculture.aiadvisorservice.entity.FarmTask;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;

/** 7-Day Farm Command Center: the farmer's week at a glance (read-only view over tasks, cases, forecast, events). */
public class CommandCenterDto {

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class Response {
        private String farmerId;
        private LocalDate from;
        private LocalDate to;
        private boolean forecastAvailable;
        private List<PlanItem> overdue;      // pending work whose time has already passed
        private List<Day> days;              // one entry per day, from..to, even when empty
        private List<EventItem> recentEvents;
    }

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class Day {
        private LocalDate date;
        private DayWeather weather;          // null beyond the forecast horizon (~3 days)
        private List<PlanItem> items;
    }

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class DayWeather {
        private Double minTemperatureCelsius;
        private Double maxTemperatureCelsius;
        private Double rainfallMm;
        private Double maxWindSpeedKmh;
        private String condition;
    }

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class PlanItem {
        private String taskId;
        private String taskType;
        private String title;                // farmer-facing, e.g. "Spray" or "Check on: yellow leaves"
        private LocalDateTime time;          // best time if the engine found one, else the due date
        private LocalDateTime dueDate;
        private LocalDateTime recommendedStartAt;
        private String weatherAdvice;
        private FarmTask.TaskPriority priority;
        private Boolean weatherDependent;
        private String diagnosticCaseId;
        private DiagnosticCase.RiskLevel caseRiskLevel;
    }

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class EventItem {
        private String id;
        private FarmEvent.EventType eventType;
        private String description;
        private LocalDateTime occurredAt;
    }
}
