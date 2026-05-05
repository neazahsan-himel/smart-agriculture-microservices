package com.smartagriculture.weatherservice.service;

import com.smartagriculture.weatherservice.dto.WeatherDto;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

public interface WeatherService {

    WeatherDto.Response createWeatherRecord(WeatherDto.Request request);

    Page<WeatherDto.Response> getAllWeatherRecords(Pageable pageable);

    WeatherDto.Response getWeatherRecordById(String id);

    WeatherDto.Response updateWeatherRecord(String id, WeatherDto.Request request);

    void deleteWeatherRecord(String id);
}