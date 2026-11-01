package com.smartagriculture.aiadvisorservice.service.weather;

import com.smartagriculture.aiadvisorservice.dto.external.WeatherSummary;
import org.junit.jupiter.api.Test;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

class ForecastFormatterTest {

    @Test
    void groupsHoursInto3HourBlocksWithRainSumMaxWindAndRainyCondition() {
        List<WeatherSummary> hourly = new ArrayList<>();
        LocalDateTime start = LocalDateTime.of(2026, 10, 29, 6, 0); // Thu
        for (int i = 0; i < 6; i++) {
            WeatherSummary w = new WeatherSummary();
            w.setRecordedAt(start.plusHours(i));
            w.setTemperatureCelsius(26.0 + i);
            w.setWindSpeedKmh(5.0 + i);
            w.setRainfallMm(i == 4 ? 2.5 : 0.0);
            w.setWeatherCondition(i == 4 ? "RAINY" : "PARTLY_CLOUDY");
            hourly.add(w);
        }

        List<String> lines = ForecastFormatter.toThreeHourBlocks(hourly);

        assertThat(lines).containsExactly(
                "- Thu 29 Oct 06:00-09:00 (early morning): rain 0.0mm, wind up to 7km/h, up to 28°C, PARTLY_CLOUDY",
                "- Thu 29 Oct 09:00-12:00 (late morning): rain 2.5mm, wind up to 10km/h, up to 31°C, RAINY");
    }

    @Test
    void emptyForecastGivesNoLines() {
        assertThat(ForecastFormatter.toThreeHourBlocks(List.of())).isEmpty();
    }
}
