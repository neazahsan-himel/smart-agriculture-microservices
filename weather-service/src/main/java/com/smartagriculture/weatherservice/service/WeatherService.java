package com.smartagriculture.weatherservice.service;

import com.smartagriculture.weatherservice.dto.WeatherDto;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

import java.util.List;

public interface WeatherService {

    WeatherDto.Response createWeatherRecord(WeatherDto.Request request);

    Page<WeatherDto.Response> getAllWeatherRecords(Pageable pageable);

    WeatherDto.Response getWeatherRecordById(String id);

    WeatherDto.Response updateWeatherRecord(String id, WeatherDto.Request request);

    void deleteWeatherRecord(String id);

    // Hourly FORECAST rows for a location from now, synced from Open-Meteo when the cache is stale
    List<WeatherDto.Response> getHourlyForecast(String countryCode, String region,
                                                double latitude, double longitude, int hours);
}