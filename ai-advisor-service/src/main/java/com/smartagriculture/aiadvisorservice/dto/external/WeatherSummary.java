package com.smartagriculture.aiadvisorservice.dto.external;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Data
@NoArgsConstructor
@JsonIgnoreProperties(ignoreUnknown = true)
public class WeatherSummary {
    private String id;
    private String countryCode;
    private String region;
    private LocalDateTime recordedAt;
    private String recordType;
    private Double temperatureCelsius;
    private Integer humidityPercent;
    private Double rainfallMm;
    private String weatherCondition;
    private Double windSpeedKmh;
    private String status;
}