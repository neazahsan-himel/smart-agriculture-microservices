package com.smartagriculture.aiadvisorservice.client;

import com.smartagriculture.aiadvisorservice.dto.external.PageResponse;
import com.smartagriculture.aiadvisorservice.dto.external.WeatherSummary;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestParam;

@FeignClient(name = "weather-service")
public interface WeatherServiceClient {

    @GetMapping("/api/v1/weather-records")
    PageResponse<WeatherSummary> getWeatherRecords(
            @RequestParam(defaultValue = "0")          int page,
            @RequestParam(defaultValue = "5")          int size,
            @RequestParam(defaultValue = "recordedAt") String sortBy,
            @RequestParam(defaultValue = "desc")       String sortDir
    );
}