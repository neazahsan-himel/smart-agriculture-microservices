package com.smartagriculture.aiadvisorservice.client;

import com.smartagriculture.aiadvisorservice.dto.external.PageResponse;
import com.smartagriculture.aiadvisorservice.dto.external.WeatherSummary;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestParam;

import java.util.List;

@FeignClient(name = "weather-service")
public interface WeatherServiceClient {

    @GetMapping("/api/v1/weather-records")
    PageResponse<WeatherSummary> getWeatherRecords(
            @RequestParam(defaultValue = "0")          int page,
            @RequestParam(defaultValue = "5")          int size,
            @RequestParam(defaultValue = "recordedAt") String sortBy,
            @RequestParam(defaultValue = "desc")       String sortDir
    );

    // Hourly forecast for a location from now; weather-service syncs it from Open-Meteo
    @GetMapping("/api/v1/weather-records/forecast")
    List<WeatherSummary> getForecast(
            @RequestParam("countryCode")                    String countryCode,
            @RequestParam(value = "region", required = false) String region,
            @RequestParam("latitude")                       double latitude,
            @RequestParam("longitude")                      double longitude,
            @RequestParam("hours")                          int hours
    );
}