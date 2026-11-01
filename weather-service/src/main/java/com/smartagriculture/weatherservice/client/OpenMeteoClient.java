package com.smartagriculture.weatherservice.client;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.smartagriculture.weatherservice.entity.WeatherRecord;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.client.SimpleClientHttpRequestFactory;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;

import java.time.Duration;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.time.ZoneOffset;
import java.util.ArrayList;
import java.util.List;

/**
 * Fetches the hourly forecast for a coordinate from Open-Meteo (free, no API key).
 *
 * <p>Open-Meteo is asked for UTC times, which are converted to the JVM's default zone: the rest of
 * the platform stores and compares {@code LocalDateTime} in JVM-local time (see DAY10 §6).
 */
@Component
public class OpenMeteoClient {

    public static final String DATA_SOURCE = "OPEN_METEO";

    private static final String HOURLY_FIELDS =
            "temperature_2m,relative_humidity_2m,precipitation,wind_speed_10m,weather_code";

    private final RestClient restClient;

    // Built directly: Spring Boot 4 only auto-configures RestClient.Builder via the separate
    // spring-boot-starter-restclient, which this service doesn't otherwise need.
    public OpenMeteoClient(@Value("${weather.open-meteo.base-url}") String baseUrl) {
        SimpleClientHttpRequestFactory factory = new SimpleClientHttpRequestFactory();
        factory.setConnectTimeout(Duration.ofSeconds(5));
        factory.setReadTimeout(Duration.ofSeconds(10));
        this.restClient = RestClient.builder().baseUrl(baseUrl).requestFactory(factory).build();
    }

    /** Hourly forecast records for the next ~4 days; lat/lon should already be rounded. */
    public List<WeatherRecord> fetchHourly(String countryCode, String region, double latitude, double longitude) {
        OpenMeteoResponse response = restClient.get()
                .uri(uri -> uri.path("/v1/forecast")
                        .queryParam("latitude", latitude)
                        .queryParam("longitude", longitude)
                        .queryParam("hourly", HOURLY_FIELDS)
                        .queryParam("forecast_days", 4)
                        .queryParam("wind_speed_unit", "kmh")
                        .queryParam("timezone", "UTC")
                        .build())
                .retrieve()
                .body(OpenMeteoResponse.class);
        return toRecords(response, countryCode, region, latitude, longitude, ZoneId.systemDefault());
    }

    static List<WeatherRecord> toRecords(OpenMeteoResponse response, String countryCode, String region,
                                         double latitude, double longitude, ZoneId zone) {
        List<WeatherRecord> records = new ArrayList<>();
        if (response == null || response.hourly() == null || response.hourly().time() == null) {
            return records;
        }
        Hourly h = response.hourly();
        for (int i = 0; i < h.time().size(); i++) {
            LocalDateTime local = LocalDateTime.parse(h.time().get(i))
                    .atOffset(ZoneOffset.UTC)
                    .atZoneSameInstant(zone)
                    .toLocalDateTime();
            Double humidity = valueAt(h.relativeHumidity(), i);
            Double code = valueAt(h.weatherCode(), i);
            records.add(WeatherRecord.builder()
                    .countryCode(countryCode)
                    .region(region)
                    .latitude(latitude)
                    .longitude(longitude)
                    .recordedAt(local)
                    .recordType(WeatherRecord.RecordType.FORECAST)
                    .temperatureCelsius(valueAt(h.temperature(), i))
                    .humidityPercent(humidity != null ? (int) Math.round(humidity) : null)
                    .rainfallMm(valueAt(h.precipitation(), i))
                    .windSpeedKmh(valueAt(h.windSpeed(), i))
                    .weatherCondition(code != null ? mapWmoCode(code.intValue()) : null)
                    .dataSource(DATA_SOURCE)
                    .build());
        }
        return records;
    }

    /** WMO weather interpretation codes, as documented by Open-Meteo. */
    static WeatherRecord.WeatherCondition mapWmoCode(int code) {
        return switch (code) {
            case 0 -> WeatherRecord.WeatherCondition.SUNNY;
            case 1, 2 -> WeatherRecord.WeatherCondition.PARTLY_CLOUDY;
            case 3 -> WeatherRecord.WeatherCondition.CLOUDY;
            case 45, 48 -> WeatherRecord.WeatherCondition.FOGGY;
            case 51, 53, 55, 56, 57 -> WeatherRecord.WeatherCondition.DRIZZLE;
            case 61, 63, 65, 66, 67, 80, 81, 82 -> WeatherRecord.WeatherCondition.RAINY;
            case 71, 73, 75, 77, 85, 86 -> WeatherRecord.WeatherCondition.SNOWY;
            case 95 -> WeatherRecord.WeatherCondition.THUNDERSTORM;
            case 96, 99 -> WeatherRecord.WeatherCondition.HAIL;
            default -> null;
        };
    }

    private static Double valueAt(List<Double> values, int i) {
        return values != null && i < values.size() ? values.get(i) : null;
    }

    @JsonIgnoreProperties(ignoreUnknown = true)
    record OpenMeteoResponse(Hourly hourly) {}

    @JsonIgnoreProperties(ignoreUnknown = true)
    record Hourly(
            List<String> time,
            @JsonProperty("temperature_2m") List<Double> temperature,
            @JsonProperty("relative_humidity_2m") List<Double> relativeHumidity,
            List<Double> precipitation,
            @JsonProperty("wind_speed_10m") List<Double> windSpeed,
            @JsonProperty("weather_code") List<Double> weatherCode
    ) {}
}
