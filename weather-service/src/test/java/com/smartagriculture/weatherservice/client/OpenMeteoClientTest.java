package com.smartagriculture.weatherservice.client;

import com.smartagriculture.weatherservice.entity.WeatherRecord;
import org.junit.jupiter.api.Test;

import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.Arrays;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

/** Plain unit test of the Open-Meteo response mapping (no HTTP, no Spring context). */
class OpenMeteoClientTest {

    private static final ZoneId DHAKA = ZoneId.of("Asia/Dhaka"); // UTC+6, no DST

    @Test
    void mapsHourlyArraysToForecastRecords_convertingUtcToTargetZone() {
        OpenMeteoClient.OpenMeteoResponse response = new OpenMeteoClient.OpenMeteoResponse(
                new OpenMeteoClient.Hourly(
                        List.of("2026-10-29T00:00", "2026-10-29T01:00"),
                        List.of(26.4, 25.9),
                        List.of(81.6, 84.0),
                        List.of(0.0, 3.2),
                        List.of(7.5, 18.0),
                        List.of(2.0, 63.0)));

        List<WeatherRecord> records =
                OpenMeteoClient.toRecords(response, "BD", "Dhaka", 23.81, 90.41, DHAKA);

        assertThat(records).hasSize(2);
        WeatherRecord first = records.getFirst();
        assertThat(first.getRecordedAt()).isEqualTo(LocalDateTime.of(2026, 10, 29, 6, 0));
        assertThat(first.getRecordType()).isEqualTo(WeatherRecord.RecordType.FORECAST);
        assertThat(first.getDataSource()).isEqualTo(OpenMeteoClient.DATA_SOURCE);
        assertThat(first.getCountryCode()).isEqualTo("BD");
        assertThat(first.getLatitude()).isEqualTo(23.81);
        assertThat(first.getTemperatureCelsius()).isEqualTo(26.4);
        assertThat(first.getHumidityPercent()).isEqualTo(82);
        assertThat(first.getWeatherCondition()).isEqualTo(WeatherRecord.WeatherCondition.PARTLY_CLOUDY);

        WeatherRecord second = records.get(1);
        assertThat(second.getRainfallMm()).isEqualTo(3.2);
        assertThat(second.getWindSpeedKmh()).isEqualTo(18.0);
        assertThat(second.getWeatherCondition()).isEqualTo(WeatherRecord.WeatherCondition.RAINY);
    }

    @Test
    void missingOrShortArraysYieldNullFieldsInsteadOfFailing() {
        OpenMeteoClient.OpenMeteoResponse response = new OpenMeteoClient.OpenMeteoResponse(
                new OpenMeteoClient.Hourly(
                        List.of("2026-10-29T00:00", "2026-10-29T01:00"),
                        List.of(26.4), null, Arrays.asList(null, 1.0), null, null));

        List<WeatherRecord> records =
                OpenMeteoClient.toRecords(response, "BD", null, 23.81, 90.41, DHAKA);

        assertThat(records).hasSize(2);
        assertThat(records.get(1).getTemperatureCelsius()).isNull();
        assertThat(records.getFirst().getRainfallMm()).isNull();
        assertThat(records.getFirst().getHumidityPercent()).isNull();
        assertThat(records.getFirst().getWeatherCondition()).isNull();
    }

    @Test
    void emptyResponseYieldsNoRecords() {
        assertThat(OpenMeteoClient.toRecords(null, "BD", null, 0, 0, DHAKA)).isEmpty();
        assertThat(OpenMeteoClient.toRecords(new OpenMeteoClient.OpenMeteoResponse(null), "BD", null, 0, 0, DHAKA))
                .isEmpty();
    }

    @Test
    void mapsWmoCodes() {
        assertThat(OpenMeteoClient.mapWmoCode(0)).isEqualTo(WeatherRecord.WeatherCondition.SUNNY);
        assertThat(OpenMeteoClient.mapWmoCode(3)).isEqualTo(WeatherRecord.WeatherCondition.CLOUDY);
        assertThat(OpenMeteoClient.mapWmoCode(45)).isEqualTo(WeatherRecord.WeatherCondition.FOGGY);
        assertThat(OpenMeteoClient.mapWmoCode(53)).isEqualTo(WeatherRecord.WeatherCondition.DRIZZLE);
        assertThat(OpenMeteoClient.mapWmoCode(81)).isEqualTo(WeatherRecord.WeatherCondition.RAINY);
        assertThat(OpenMeteoClient.mapWmoCode(75)).isEqualTo(WeatherRecord.WeatherCondition.SNOWY);
        assertThat(OpenMeteoClient.mapWmoCode(95)).isEqualTo(WeatherRecord.WeatherCondition.THUNDERSTORM);
        assertThat(OpenMeteoClient.mapWmoCode(99)).isEqualTo(WeatherRecord.WeatherCondition.HAIL);
        assertThat(OpenMeteoClient.mapWmoCode(42)).isNull();
    }
}
