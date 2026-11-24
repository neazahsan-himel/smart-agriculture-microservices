package com.smartagriculture.aiadvisorservice.service;

import com.smartagriculture.aiadvisorservice.dto.CommandCenterDto;
import com.smartagriculture.aiadvisorservice.dto.external.WeatherSummary;
import com.smartagriculture.aiadvisorservice.entity.DiagnosticCase;
import com.smartagriculture.aiadvisorservice.entity.FarmEvent;
import com.smartagriculture.aiadvisorservice.entity.FarmTask;
import com.smartagriculture.aiadvisorservice.entity.WeatherTaskType;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.*;

/**
 * Pure assembly of the Command Center week (no I/O, so it is unit-tested directly):
 * places each pending task on a day, summarises the forecast per day, and lists recent events.
 */
public final class WeekPlanBuilder {

    static final int MAX_SYMPTOM_LENGTH = 60;

    private WeekPlanBuilder() {}

    public static CommandCenterDto.Response build(String farmerId, LocalDateTime now, int days,
                                                  List<FarmTask> pendingTasks,
                                                  Map<String, DiagnosticCase> casesById,
                                                  List<WeatherSummary> hourlyForecast,
                                                  List<FarmEvent> recentEvents) {
        LocalDate today = now.toLocalDate();
        LocalDate lastDay = today.plusDays(days - 1L);

        Map<LocalDate, List<CommandCenterDto.PlanItem>> itemsByDay = new TreeMap<>();
        List<CommandCenterDto.PlanItem> overdue = new ArrayList<>();
        for (FarmTask task : pendingTasks) {
            if (task.getDueDate() == null) continue;
            // Guarded: immutable maps (Map.of) throw on get(null), and most tasks have no source case
            String caseId = task.getSourceDiagnosticCaseId();
            CommandCenterDto.PlanItem item = toItem(task, caseId != null ? casesById.get(caseId) : null);
            LocalDate day = item.getTime().toLocalDate();
            if (day.isBefore(today)) {
                overdue.add(item);
            } else if (!day.isAfter(lastDay)) {
                itemsByDay.computeIfAbsent(day, d -> new ArrayList<>()).add(item);
            }
        }
        overdue.sort(Comparator.comparing(CommandCenterDto.PlanItem::getTime));

        Map<LocalDate, CommandCenterDto.DayWeather> weatherByDay = summariseByDay(hourlyForecast);

        List<CommandCenterDto.Day> dayList = new ArrayList<>();
        for (LocalDate d = today; !d.isAfter(lastDay); d = d.plusDays(1)) {
            List<CommandCenterDto.PlanItem> items = itemsByDay.getOrDefault(d, new ArrayList<>());
            items.sort(Comparator.comparing(CommandCenterDto.PlanItem::getTime));
            dayList.add(CommandCenterDto.Day.builder().date(d).weather(weatherByDay.get(d)).items(items).build());
        }

        return CommandCenterDto.Response.builder()
                .farmerId(farmerId)
                .from(today)
                .to(lastDay)
                .forecastAvailable(!weatherByDay.isEmpty())
                .overdue(overdue)
                .days(dayList)
                .recentEvents(recentEvents.stream().map(WeekPlanBuilder::toEventItem).toList())
                .build();
    }

    // Weather tasks sit on the day the engine recommends; everything else on its due date
    static CommandCenterDto.PlanItem toItem(FarmTask task, DiagnosticCase sourceCase) {
        LocalDateTime time = task.getRecommendedStartAt() != null ? task.getRecommendedStartAt() : task.getDueDate();
        return CommandCenterDto.PlanItem.builder()
                .taskId(task.getId())
                .taskType(task.getTaskType())
                .title(title(task, sourceCase))
                .time(time)
                .dueDate(task.getDueDate())
                .recommendedStartAt(task.getRecommendedStartAt())
                .weatherAdvice(task.getWeatherAdvice())
                .priority(task.getPriority())
                .weatherDependent(task.getWeatherDependent())
                .diagnosticCaseId(task.getSourceDiagnosticCaseId())
                .caseRiskLevel(sourceCase != null ? sourceCase.getRiskLevel() : null)
                .build();
    }

    static String title(FarmTask task, DiagnosticCase sourceCase) {
        WeatherTaskType weatherType = WeatherTaskType.fromTaskType(task.getTaskType());
        if (weatherType != null) {
            return capitalise(weatherType.verb());
        }
        if ("FOLLOW_UP".equals(task.getTaskType())) {
            String symptom = sourceCase != null ? sourceCase.getSymptom() : null;
            if (symptom == null || symptom.isBlank()) return "Follow-up check";
            String trimmed = symptom.strip();
            return "Check on: " + (trimmed.length() > MAX_SYMPTOM_LENGTH
                    ? trimmed.substring(0, MAX_SYMPTOM_LENGTH - 1) + "…" : trimmed);
        }
        return capitalise(task.getTaskType().toLowerCase(Locale.ROOT).replace('_', ' '));
    }

    static Map<LocalDate, CommandCenterDto.DayWeather> summariseByDay(List<WeatherSummary> hourly) {
        Map<LocalDate, List<WeatherSummary>> byDay = new TreeMap<>();
        for (WeatherSummary w : hourly) {
            if (w.getRecordedAt() != null) {
                byDay.computeIfAbsent(w.getRecordedAt().toLocalDate(), d -> new ArrayList<>()).add(w);
            }
        }
        Map<LocalDate, CommandCenterDto.DayWeather> result = new TreeMap<>();
        byDay.forEach((day, hours) -> result.put(day, CommandCenterDto.DayWeather.builder()
                .minTemperatureCelsius(stat(hours, WeatherSummary::getTemperatureCelsius, false))
                .maxTemperatureCelsius(stat(hours, WeatherSummary::getTemperatureCelsius, true))
                .rainfallMm(round1(hours.stream().mapToDouble(w -> w.getRainfallMm() != null ? w.getRainfallMm() : 0.0).sum()))
                .maxWindSpeedKmh(stat(hours, WeatherSummary::getWindSpeedKmh, true))
                .condition(condition(hours))
                .build()));
        return result;
    }

    // Same rule as ForecastFormatter: a rainy hour's condition wins, so a shower isn't hidden; else midday's
    private static String condition(List<WeatherSummary> hours) {
        return hours.stream()
                .filter(w -> w.getRainfallMm() != null && w.getRainfallMm() > 0 && w.getWeatherCondition() != null)
                .map(WeatherSummary::getWeatherCondition)
                .findFirst()
                .orElseGet(() -> hours.stream()
                        .min(Comparator.comparingInt(w -> Math.abs(w.getRecordedAt().getHour() - 12)))
                        .map(WeatherSummary::getWeatherCondition)
                        .orElse(null));
    }

    private static Double stat(List<WeatherSummary> hours, java.util.function.Function<WeatherSummary, Double> field, boolean max) {
        DoubleSummaryStatistics s = hours.stream().map(field).filter(Objects::nonNull)
                .mapToDouble(Double::doubleValue).summaryStatistics();
        if (s.getCount() == 0) return null;
        return round1(max ? s.getMax() : s.getMin());
    }

    private static double round1(double v) {
        return Math.round(v * 10) / 10.0;
    }

    private static String capitalise(String s) {
        return s.isEmpty() ? s : Character.toUpperCase(s.charAt(0)) + s.substring(1);
    }

    private static CommandCenterDto.EventItem toEventItem(FarmEvent e) {
        return CommandCenterDto.EventItem.builder()
                .id(e.getId())
                .eventType(e.getEventType())
                .description(e.getDescription())
                .occurredAt(e.getOccurredAt())
                .build();
    }
}
