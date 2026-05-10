package com.smartagriculture.aiadvisorservice.dto.external;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@JsonIgnoreProperties(ignoreUnknown = true)
public class CropSummary {
    private String id;
    private String name;
    private String variety;
    private String cropType;
    private String season;
    private String idealSoilType;
    private Double minTemperatureCelsius;
    private Double maxTemperatureCelsius;
    private Double minRainfallMm;
    private Double maxRainfallMm;
    private Integer growingDurationDays;
    private Boolean irrigationRequired;
    private Double typicalYieldPerHectare;
    private String countryCode;
    private String region;
    private String status;
}