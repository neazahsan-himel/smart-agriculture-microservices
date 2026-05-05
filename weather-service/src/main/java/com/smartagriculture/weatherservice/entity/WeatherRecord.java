package com.smartagriculture.weatherservice.entity;

import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;

import java.time.LocalDateTime;

@Entity
@Table(
    name = "weather_records",
    indexes = {
        @Index(name = "idx_weather_country",   columnList = "countryCode"),
        @Index(name = "idx_weather_region",    columnList = "region"),
        @Index(name = "idx_weather_recordedAt", columnList = "recordedAt"),
        @Index(name = "idx_weather_type",      columnList = "recordType"),
        @Index(name = "idx_weather_condition", columnList = "weatherCondition"),
        @Index(name = "idx_weather_status",    columnList = "status"),
        @Index(name = "idx_weather_deleted",   columnList = "deleted")
    }
)
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class WeatherRecord {

    // ── Identity ──────────────────────────────────────────────────────────────

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    @Column(updatable = false, nullable = false)
    private String id;

    // ── Location ──────────────────────────────────────────────────────────────

    // ISO 3166-1 alpha-2: BD, IN, KE
    @Column(nullable = false, length = 2)
    private String countryCode;

    private String region;      // state / province — null means country-wide

    private Double latitude;
    private Double longitude;

    // ── Observation Time ──────────────────────────────────────────────────────

    @Column(nullable = false)
    private LocalDateTime recordedAt;   // the time this weather data applies to

    // OBSERVATION = actual measured data; FORECAST = predicted future data
    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private RecordType recordType;

    // ── Weather Measurements ──────────────────────────────────────────────────

    private Double  temperatureCelsius;
    private Double  feelsLikeCelsius;

    // 0–100 %
    private Integer humidityPercent;

    // mm during the observation/forecast period
    private Double  rainfallMm;

    private Double  windSpeedKmh;

    @Enumerated(EnumType.STRING)
    private WindDirection windDirection;

    @Enumerated(EnumType.STRING)
    private WeatherCondition weatherCondition;

    private Double  visibilityKm;
    private Integer uvIndex;

    // Where this data came from: "MANUAL", "OPENWEATHER", "SENSOR", etc.
    private String dataSource;

    // ── Lifecycle ─────────────────────────────────────────────────────────────

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    @Builder.Default
    private WeatherStatus status = WeatherStatus.ACTIVE;

    @Column(nullable = false)
    @Builder.Default
    private Boolean deleted = false;

    // ── Audit ─────────────────────────────────────────────────────────────────

    @CreationTimestamp
    @Column(updatable = false)
    private LocalDateTime createdAt;

    @UpdateTimestamp
    private LocalDateTime updatedAt;

    @Version
    private Long version;


    // ── Enums ─────────────────────────────────────────────────────────────────

    public enum RecordType {
        OBSERVATION, FORECAST
    }

    public enum WeatherCondition {
        SUNNY, PARTLY_CLOUDY, CLOUDY, RAINY, DRIZZLE, THUNDERSTORM, STORMY, FOGGY, SNOWY, HAIL
    }

    public enum WindDirection {
        N, NE, E, SE, S, SW, W, NW
    }

    public enum WeatherStatus {
        ACTIVE, INACTIVE
    }
}