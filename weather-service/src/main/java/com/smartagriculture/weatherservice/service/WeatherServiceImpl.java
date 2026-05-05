package com.smartagriculture.weatherservice.service;

import com.smartagriculture.weatherservice.dto.WeatherDto;
import com.smartagriculture.weatherservice.entity.WeatherRecord;
import com.smartagriculture.weatherservice.exception.DuplicateResourceException;
import com.smartagriculture.weatherservice.exception.ResourceNotFoundException;
import com.smartagriculture.weatherservice.repository.WeatherRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class WeatherServiceImpl implements WeatherService {

    private final WeatherRepository weatherRepository;

    @Override
    public WeatherDto.Response createWeatherRecord(WeatherDto.Request request) {
        if (weatherRepository.existsByCountryCodeAndRegionAndRecordedAtAndRecordTypeAndDeletedFalse(
                request.getCountryCode(), request.getRegion(),
                request.getRecordedAt(), request.getRecordType())) {
            throw new DuplicateResourceException(
                    "Weather record already exists for: " + request.getCountryCode() +
                    " / " + request.getRegion() + " at " + request.getRecordedAt());
        }

        WeatherRecord record = WeatherRecord.builder()
                .countryCode(request.getCountryCode())
                .region(request.getRegion())
                .latitude(request.getLatitude())
                .longitude(request.getLongitude())
                .recordedAt(request.getRecordedAt())
                .recordType(request.getRecordType())
                .temperatureCelsius(request.getTemperatureCelsius())
                .feelsLikeCelsius(request.getFeelsLikeCelsius())
                .humidityPercent(request.getHumidityPercent())
                .rainfallMm(request.getRainfallMm())
                .windSpeedKmh(request.getWindSpeedKmh())
                .windDirection(request.getWindDirection())
                .weatherCondition(request.getWeatherCondition())
                .visibilityKm(request.getVisibilityKm())
                .uvIndex(request.getUvIndex())
                .dataSource(request.getDataSource())
                .build();

        return toResponse(weatherRepository.save(record));
    }

    @Override
    public Page<WeatherDto.Response> getAllWeatherRecords(Pageable pageable) {
        return weatherRepository.findAllByDeletedFalse(pageable).map(this::toResponse);
    }

    @Override
    public WeatherDto.Response getWeatherRecordById(String id) {
        return weatherRepository.findByIdAndDeletedFalse(id)
                .map(this::toResponse)
                .orElseThrow(() -> new ResourceNotFoundException("Weather record not found with id: " + id));
    }

    @Override
    public WeatherDto.Response updateWeatherRecord(String id, WeatherDto.Request request) {
        WeatherRecord record = weatherRepository.findByIdAndDeletedFalse(id)
                .orElseThrow(() -> new ResourceNotFoundException("Weather record not found with id: " + id));

        if (weatherRepository.existsByCountryCodeAndRegionAndRecordedAtAndRecordTypeAndDeletedFalseAndIdNot(
                request.getCountryCode(), request.getRegion(),
                request.getRecordedAt(), request.getRecordType(), id)) {
            throw new DuplicateResourceException(
                    "Another weather record already exists for: " + request.getCountryCode() +
                    " / " + request.getRegion() + " at " + request.getRecordedAt());
        }

        // countryCode, recordedAt, recordType are @NotNull/@NotBlank — always set
        record.setCountryCode(request.getCountryCode());
        record.setRecordedAt(request.getRecordedAt());
        record.setRecordType(request.getRecordType());

        // null-safe: only overwrite when the request provides a value
        if (request.getRegion() != null)             record.setRegion(request.getRegion());
        if (request.getLatitude() != null)           record.setLatitude(request.getLatitude());
        if (request.getLongitude() != null)          record.setLongitude(request.getLongitude());
        if (request.getTemperatureCelsius() != null) record.setTemperatureCelsius(request.getTemperatureCelsius());
        if (request.getFeelsLikeCelsius() != null)   record.setFeelsLikeCelsius(request.getFeelsLikeCelsius());
        if (request.getHumidityPercent() != null)    record.setHumidityPercent(request.getHumidityPercent());
        if (request.getRainfallMm() != null)         record.setRainfallMm(request.getRainfallMm());
        if (request.getWindSpeedKmh() != null)       record.setWindSpeedKmh(request.getWindSpeedKmh());
        if (request.getWindDirection() != null)      record.setWindDirection(request.getWindDirection());
        if (request.getWeatherCondition() != null)   record.setWeatherCondition(request.getWeatherCondition());
        if (request.getVisibilityKm() != null)       record.setVisibilityKm(request.getVisibilityKm());
        if (request.getUvIndex() != null)            record.setUvIndex(request.getUvIndex());
        if (request.getDataSource() != null)         record.setDataSource(request.getDataSource());

        return toResponse(weatherRepository.save(record));
    }

    @Override
    public void deleteWeatherRecord(String id) {
        WeatherRecord record = weatherRepository.findByIdAndDeletedFalse(id)
                .orElseThrow(() -> new ResourceNotFoundException("Weather record not found with id: " + id));
        record.setDeleted(true);
        record.setStatus(WeatherRecord.WeatherStatus.INACTIVE);
        weatherRepository.save(record);
    }

    private WeatherDto.Response toResponse(WeatherRecord record) {
        return WeatherDto.Response.builder()
                .id(record.getId())
                .countryCode(record.getCountryCode())
                .region(record.getRegion())
                .latitude(record.getLatitude())
                .longitude(record.getLongitude())
                .recordedAt(record.getRecordedAt())
                .recordType(record.getRecordType())
                .temperatureCelsius(record.getTemperatureCelsius())
                .feelsLikeCelsius(record.getFeelsLikeCelsius())
                .humidityPercent(record.getHumidityPercent())
                .rainfallMm(record.getRainfallMm())
                .windSpeedKmh(record.getWindSpeedKmh())
                .windDirection(record.getWindDirection())
                .weatherCondition(record.getWeatherCondition())
                .visibilityKm(record.getVisibilityKm())
                .uvIndex(record.getUvIndex())
                .dataSource(record.getDataSource())
                .status(record.getStatus())
                .createdAt(record.getCreatedAt())
                .updatedAt(record.getUpdatedAt())
                .build();
    }
}