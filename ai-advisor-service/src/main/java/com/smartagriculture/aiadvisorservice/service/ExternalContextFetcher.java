package com.smartagriculture.aiadvisorservice.service;

import com.smartagriculture.aiadvisorservice.client.CropServiceClient;
import com.smartagriculture.aiadvisorservice.client.FarmerServiceClient;
import com.smartagriculture.aiadvisorservice.client.WeatherServiceClient;
import com.smartagriculture.aiadvisorservice.dto.external.CropSummary;
import com.smartagriculture.aiadvisorservice.dto.external.FarmerResponse;
import com.smartagriculture.aiadvisorservice.dto.external.PageResponse;
import com.smartagriculture.aiadvisorservice.dto.external.WeatherSummary;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.util.Collections;
import java.util.List;

/**
 * Graceful-fallback fetchers for the external context (farmer/crop/weather) that every
 * advisor prompt is built from. Shared between the one-shot {@code /advice} flow and the
 * multi-turn conversation flow so both stay consistent.
 */
@Component
@RequiredArgsConstructor
@Slf4j
public class ExternalContextFetcher {

    private final FarmerServiceClient farmerServiceClient;
    private final CropServiceClient cropServiceClient;
    private final WeatherServiceClient weatherServiceClient;

    public FarmerResponse fetchFarmer(String farmerId) {
        if (farmerId == null || farmerId.isBlank()) return null;
        try {
            return farmerServiceClient.getFarmerById(farmerId);
        } catch (Exception e) {
            log.warn("Could not fetch farmer {}: {}", farmerId, e.getMessage());
            return null;
        }
    }

    public List<CropSummary> fetchCrops() {
        try {
            PageResponse<CropSummary> page = cropServiceClient.getCrops(0, 20, "name", "asc");
            return page.getContent() != null ? page.getContent() : Collections.emptyList();
        } catch (Exception e) {
            log.warn("Could not fetch crops: {}", e.getMessage());
            return Collections.emptyList();
        }
    }

    public List<WeatherSummary> fetchWeather() {
        try {
            PageResponse<WeatherSummary> page = weatherServiceClient.getWeatherRecords(0, 5, "recordedAt", "desc");
            return page.getContent() != null ? page.getContent() : Collections.emptyList();
        } catch (Exception e) {
            log.warn("Could not fetch weather records: {}", e.getMessage());
            return Collections.emptyList();
        }
    }
}
