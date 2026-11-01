package com.smartagriculture.aiadvisorservice.service.weather;

import com.smartagriculture.aiadvisorservice.dto.external.WeatherSummary;
import com.smartagriculture.aiadvisorservice.entity.WeatherTaskType;
import com.smartagriculture.aiadvisorservice.service.weather.BestTimeEvaluator.BestTimeResult;
import com.smartagriculture.aiadvisorservice.service.weather.BestTimeEvaluator.Status;
import org.junit.jupiter.api.Test;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.function.Consumer;

import static org.assertj.core.api.Assertions.assertThat;

/** Pure unit test: synthetic hourly forecasts, fixed clock (Thu 29 Oct 2026 08:30). */
class BestTimeEvaluatorTest {

    private static final LocalDateTime THU_MIDNIGHT = LocalDateTime.of(2026, 10, 29, 0, 0);
    private static final LocalDateTime NOW = THU_MIDNIGHT.withHour(8).withMinute(30);
    private static final LocalDateTime DUE_FRI_8AM = LocalDateTime.of(2026, 10, 30, 8, 0);

    private final BestTimeEvaluator evaluator = new BestTimeEvaluator();

    @Test
    void spray_calmDryWeather_recommendsNextAllowedHour() {
        BestTimeResult result = evaluator.evaluate(WeatherTaskType.SPRAY, forecast(w -> {}), NOW, DUE_FRI_8AM);

        assertThat(result.status()).isEqualTo(Status.RECOMMENDED);
        // search starts 24h before due (Thu 08:00) but never before the next full hour (09:00)
        assertThat(result.recommendedStart()).isEqualTo(at(0, 9));
        assertThat(result.advice()).isEqualTo("Best time to spray: Thu 29 Oct 09:00.");
    }

    @Test
    void spray_rainDueThisAfternoon_skipsToAfterTheRainAndExplainsWhy() {
        List<WeatherSummary> f = forecast(w -> {});
        hour(f, at(0, 12)).setRainfallMm(2.0);

        BestTimeResult result = evaluator.evaluate(WeatherTaskType.SPRAY, f, NOW, DUE_FRI_8AM);

        // 09:00 and 10:00 both have the 12:00 rain within slot+6h; 16:00 is the next allowed, dry hour
        assertThat(result.status()).isEqualTo(Status.RECOMMENDED);
        assertThat(result.recommendedStart()).isEqualTo(at(0, 16));
        assertThat(result.advice())
                .startsWith("Best time to spray: Thu 29 Oct 16:00.")
                .contains("2.0 mm rain expected within 6 h of Thu 29 Oct 09:00");
    }

    @Test
    void spray_windyAllDay_movesToNextMorning() {
        List<WeatherSummary> f = forecast(w -> {
            if (w.getRecordedAt().getDayOfMonth() == 29) w.setWindSpeedKmh(20.0);
        });

        BestTimeResult result = evaluator.evaluate(WeatherTaskType.SPRAY, f, NOW, DUE_FRI_8AM);

        assertThat(result.recommendedStart()).isEqualTo(at(1, 6));
        assertThat(result.advice()).contains("wind 20 km/h at Thu 29 Oct 09:00 (max 15)");
    }

    @Test
    void spray_tooHot_isRejected() {
        List<WeatherSummary> f = forecast(w -> {
            if (w.getRecordedAt().getDayOfMonth() == 29) w.setTemperatureCelsius(34.0);
        });

        BestTimeResult result = evaluator.evaluate(WeatherTaskType.SPRAY, f, NOW, DUE_FRI_8AM);

        assertThat(result.recommendedStart()).isEqualTo(at(1, 6));
        assertThat(result.advice()).contains("34°C at Thu 29 Oct 09:00 (max 32°C)");
    }

    @Test
    void fertilize_heavyRainWithin24h_waitsUntilItIsOutOfTheWindow() {
        List<WeatherSummary> f = forecast(w -> {});
        hour(f, at(0, 20)).setRainfallMm(12.0);

        BestTimeResult result = evaluator.evaluate(WeatherTaskType.FERTILIZE, f, NOW, DUE_FRI_8AM);

        // Every Thu slot's 24h window contains Thu 20:00; Fri 06:00 is the first one that doesn't
        assertThat(result.recommendedStart()).isEqualTo(at(1, 6));
        assertThat(result.advice()).contains("12.0 mm rain expected in the 24 h after (fertiliser would wash off)");
    }

    @Test
    void fertilize_lightRainInTheSlotItselfIsAllowed() {
        List<WeatherSummary> f = forecast(w -> {});
        hour(f, at(0, 9)).setRainfallMm(1.0); // ≤ 1 mm in the slot is fine

        BestTimeResult result = evaluator.evaluate(WeatherTaskType.FERTILIZE, f, NOW, DUE_FRI_8AM);

        assertThat(result.recommendedStart()).isEqualTo(at(0, 9));
    }

    @Test
    void irrigate_rainyThroughout_saysNoNeedToIrrigate() {
        List<WeatherSummary> f = forecast(w -> w.setRainfallMm(1.0));

        BestTimeResult result = evaluator.evaluate(WeatherTaskType.IRRIGATE, f, NOW, DUE_FRI_8AM);

        assertThat(result.status()).isEqualTo(Status.NO_WINDOW);
        assertThat(result.recommendedStart()).isNull();
        assertThat(result.advice())
                .startsWith("No need to irrigate before")
                .contains("the rain will water the field");
    }

    @Test
    void harvest_rainyThroughout_reportsNoSafeTime() {
        List<WeatherSummary> f = forecast(w -> w.setRainfallMm(0.2));

        BestTimeResult result = evaluator.evaluate(WeatherTaskType.HARVEST, f, NOW, DUE_FRI_8AM);

        assertThat(result.status()).isEqualTo(Status.NO_WINDOW);
        assertThat(result.advice()).startsWith("No safe time to harvest before");
    }

    @Test
    void sow_onlyHeavyRainBlocks() {
        List<WeatherSummary> f = forecast(w -> w.setRainfallMm(0.5)); // 12 mm / 24h — below the 20 mm limit

        BestTimeResult result = evaluator.evaluate(WeatherTaskType.SOW, f, NOW, DUE_FRI_8AM);

        assertThat(result.recommendedStart()).isEqualTo(at(0, 9));
    }

    @Test
    void emptyForecast_isNoForecast() {
        BestTimeResult result = evaluator.evaluate(WeatherTaskType.SPRAY, List.of(), NOW, DUE_FRI_8AM);

        assertThat(result.status()).isEqualTo(Status.NO_FORECAST);
        assertThat(result.advice()).contains("Check the weather yourself before you spray");
    }

    @Test
    void dueDateBeyondForecast_isNoForecastYet() {
        LocalDateTime dueInTenDays = NOW.plusDays(10);

        BestTimeResult result = evaluator.evaluate(WeatherTaskType.SPRAY, forecast(w -> {}), NOW, dueInTenDays);

        assertThat(result.status()).isEqualTo(Status.NO_FORECAST);
        assertThat(result.advice()).contains("doesn't reach");
    }

    @Test
    void dueDateLongPast_isNoWindowAskingForANewDate() {
        BestTimeResult result = evaluator.evaluate(WeatherTaskType.SPRAY, forecast(w -> {}), NOW, NOW.minusDays(3));

        assertThat(result.status()).isEqualTo(Status.NO_WINDOW);
        assertThat(result.advice()).contains("has passed");
    }

    @Test
    void nullDueDate_isTreatedAsNow() {
        BestTimeResult result = evaluator.evaluate(WeatherTaskType.SPRAY, forecast(w -> {}), NOW, null);

        assertThat(result.recommendedStart()).isEqualTo(at(0, 9));
    }

    @Test
    void ceilToHour() {
        assertThat(BestTimeEvaluator.ceilToHour(NOW)).isEqualTo(at(0, 9));
        assertThat(BestTimeEvaluator.ceilToHour(at(0, 9))).isEqualTo(at(0, 9));
    }

    // ── helpers ──────────────────────────────────────────────────────────────

    /** 96 hourly rows from Thu 00:00: 28°C, 5 km/h wind, no rain — then the customiser. */
    private static List<WeatherSummary> forecast(Consumer<WeatherSummary> customise) {
        List<WeatherSummary> hours = new ArrayList<>();
        for (int i = 0; i < 96; i++) {
            WeatherSummary w = new WeatherSummary();
            w.setRecordedAt(THU_MIDNIGHT.plusHours(i));
            w.setTemperatureCelsius(28.0);
            w.setWindSpeedKmh(5.0);
            w.setRainfallMm(0.0);
            w.setWeatherCondition("PARTLY_CLOUDY");
            customise.accept(w);
            hours.add(w);
        }
        return hours;
    }

    private static WeatherSummary hour(List<WeatherSummary> forecast, LocalDateTime t) {
        return forecast.stream().filter(w -> w.getRecordedAt().equals(t)).findFirst().orElseThrow();
    }

    /** dayOffset 0 = Thu 29 Oct. */
    private static LocalDateTime at(int dayOffset, int hour) {
        return THU_MIDNIGHT.plusDays(dayOffset).withHour(hour);
    }
}
