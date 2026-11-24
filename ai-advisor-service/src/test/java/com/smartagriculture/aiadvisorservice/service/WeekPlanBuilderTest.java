package com.smartagriculture.aiadvisorservice.service;

import com.smartagriculture.aiadvisorservice.dto.CommandCenterDto;
import com.smartagriculture.aiadvisorservice.dto.external.WeatherSummary;
import com.smartagriculture.aiadvisorservice.entity.DiagnosticCase;
import com.smartagriculture.aiadvisorservice.entity.FarmEvent;
import com.smartagriculture.aiadvisorservice.entity.FarmTask;
import org.junit.jupiter.api.Test;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;

class WeekPlanBuilderTest {

    private static final LocalDateTime NOW = LocalDateTime.of(2026, 11, 2, 9, 0); // Mon
    private static final LocalDate TODAY = NOW.toLocalDate();

    @Test
    void returnsEveryDayOfTheWindow_evenWhenEmpty() {
        CommandCenterDto.Response week = build(7, List.of(), Map.of(), List.of());

        assertThat(week.getDays()).extracting(CommandCenterDto.Day::getDate)
                .containsExactly(TODAY, TODAY.plusDays(1), TODAY.plusDays(2), TODAY.plusDays(3),
                        TODAY.plusDays(4), TODAY.plusDays(5), TODAY.plusDays(6));
        assertThat(week.getFrom()).isEqualTo(TODAY);
        assertThat(week.getTo()).isEqualTo(TODAY.plusDays(6));
        assertThat(week.isForecastAvailable()).isFalse();
    }

    @Test
    void weatherTask_sitsOnItsRecommendedDay_notItsDueDay() {
        FarmTask spray = task("t1", "SPRAY", TODAY.plusDays(2).atTime(8, 0));
        spray.setWeatherDependent(true);
        spray.setRecommendedStartAt(TODAY.plusDays(1).atTime(16, 0)); // engine moved it a day earlier

        CommandCenterDto.Response week = build(7, List.of(spray), Map.of(), List.of());

        assertThat(week.getDays().get(1).getItems()).singleElement().satisfies(item -> {
            assertThat(item.getTitle()).isEqualTo("Spray");
            assertThat(item.getTime()).isEqualTo(TODAY.plusDays(1).atTime(16, 0));
        });
        assertThat(week.getDays().get(2).getItems()).isEmpty();
    }

    @Test
    void pastWork_isOverdue_andWorkBeyondTheWindow_isLeftOut() {
        FarmTask late = task("late", "SOW", TODAY.minusDays(1).atTime(7, 0));
        FarmTask later = task("later", "HARVEST", TODAY.plusDays(9).atTime(7, 0));
        FarmTask earlierToday = task("today", "IRRIGATE", TODAY.atTime(6, 0)); // earlier today still counts as today

        CommandCenterDto.Response week = build(7, List.of(late, later, earlierToday), Map.of(), List.of());

        assertThat(week.getOverdue()).extracting(CommandCenterDto.PlanItem::getTaskId).containsExactly("late");
        assertThat(week.getDays().get(0).getItems()).extracting(CommandCenterDto.PlanItem::getTaskId).containsExactly("today");
        assertThat(week.getDays()).flatExtracting(CommandCenterDto.Day::getItems)
                .extracting(CommandCenterDto.PlanItem::getTaskId).doesNotContain("later");
    }

    @Test
    void itemsWithinADay_areSortedByTime() {
        FarmTask afternoon = task("pm", "IRRIGATE", TODAY.plusDays(1).atTime(16, 0));
        FarmTask morning = task("am", "SOW", TODAY.plusDays(1).atTime(7, 0));

        CommandCenterDto.Response week = build(7, List.of(afternoon, morning), Map.of(), List.of());

        assertThat(week.getDays().get(1).getItems()).extracting(CommandCenterDto.PlanItem::getTaskId).containsExactly("am", "pm");
    }

    @Test
    void followUpTask_isTitledWithTheCaseSymptom_andCarriesItsRisk() {
        FarmTask followUp = task("f1", "FOLLOW_UP", TODAY.plusDays(3).atTime(9, 0));
        followUp.setSourceDiagnosticCaseId("c1");
        DiagnosticCase c = DiagnosticCase.builder().id("c1").symptom("  yellow leaves on the north plot  ")
                .riskLevel(DiagnosticCase.RiskLevel.HIGH).build();

        CommandCenterDto.PlanItem item = build(7, List.of(followUp), Map.of("c1", c), List.of())
                .getDays().get(3).getItems().getFirst();

        assertThat(item.getTitle()).isEqualTo("Check on: yellow leaves on the north plot");
        assertThat(item.getCaseRiskLevel()).isEqualTo(DiagnosticCase.RiskLevel.HIGH);
    }

    @Test
    void titles_forLongSymptoms_missingCases_andOtherTypes() {
        DiagnosticCase longCase = DiagnosticCase.builder().symptom("x".repeat(100)).build();

        assertThat(WeekPlanBuilder.title(task("a", "FOLLOW_UP", NOW), longCase))
                .hasSize("Check on: ".length() + WeekPlanBuilder.MAX_SYMPTOM_LENGTH).endsWith("…");
        assertThat(WeekPlanBuilder.title(task("b", "FOLLOW_UP", NOW), null)).isEqualTo("Follow-up check");
        assertThat(WeekPlanBuilder.title(task("c", "FERTILIZE", NOW), null)).isEqualTo("Apply fertiliser");
        assertThat(WeekPlanBuilder.title(task("d", "SOIL_TEST", NOW), null)).isEqualTo("Soil test");
    }

    @Test
    void forecast_isSummarisedPerDay_withRainyConditionPreferred() {
        List<WeatherSummary> hourly = List.of(
                hour(TODAY.atTime(6, 0), 24.0, 0.0, 5.0, "CLEAR"),
                hour(TODAY.atTime(12, 0), 31.0, 0.0, 9.0, "CLOUDY"),
                hour(TODAY.atTime(15, 0), 29.0, 4.2, 18.0, "RAINY"),
                hour(TODAY.plusDays(1).atTime(12, 0), 30.0, 0.0, 7.0, "CLEAR"));

        CommandCenterDto.Response week = build(7, List.of(), Map.of(), hourly);

        CommandCenterDto.DayWeather today = week.getDays().get(0).getWeather();
        assertThat(today.getMinTemperatureCelsius()).isEqualTo(24.0);
        assertThat(today.getMaxTemperatureCelsius()).isEqualTo(31.0);
        assertThat(today.getRainfallMm()).isEqualTo(4.2);
        assertThat(today.getMaxWindSpeedKmh()).isEqualTo(18.0);
        assertThat(today.getCondition()).isEqualTo("RAINY");
        assertThat(week.getDays().get(1).getWeather().getCondition()).isEqualTo("CLEAR");
        assertThat(week.getDays().get(2).getWeather()).isNull(); // beyond the forecast
        assertThat(week.isForecastAvailable()).isTrue();
    }

    @Test
    void recentEvents_areMappedInOrder() {
        FarmEvent e = FarmEvent.builder().id("e1").eventType(FarmEvent.EventType.ADVICE)
                .description("Advised drainage").occurredAt(NOW.minusDays(1)).build();

        CommandCenterDto.Response week = WeekPlanBuilder.build("f", NOW, 7, List.of(), Map.of(), List.of(), List.of(e));

        assertThat(week.getRecentEvents()).singleElement().satisfies(item -> {
            assertThat(item.getEventType()).isEqualTo(FarmEvent.EventType.ADVICE);
            assertThat(item.getDescription()).isEqualTo("Advised drainage");
        });
    }

    private static CommandCenterDto.Response build(int days, List<FarmTask> tasks, Map<String, DiagnosticCase> cases,
                                                   List<WeatherSummary> forecast) {
        return WeekPlanBuilder.build("farmer-1", NOW, days, tasks, cases, forecast, List.of());
    }

    private static FarmTask task(String id, String type, LocalDateTime due) {
        return FarmTask.builder().id(id).farmerId("farmer-1").taskType(type).dueDate(due).build();
    }

    private static WeatherSummary hour(LocalDateTime at, double temp, double rain, double wind, String condition) {
        WeatherSummary w = new WeatherSummary();
        w.setRecordedAt(at);
        w.setTemperatureCelsius(temp);
        w.setRainfallMm(rain);
        w.setWindSpeedKmh(wind);
        w.setWeatherCondition(condition);
        return w;
    }
}
