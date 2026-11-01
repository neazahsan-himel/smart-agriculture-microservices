package com.smartagriculture.weatherservice.service;

import com.smartagriculture.weatherservice.client.OpenMeteoClient;
import com.smartagriculture.weatherservice.dto.WeatherDto;
import com.smartagriculture.weatherservice.entity.WeatherRecord;
import com.smartagriculture.weatherservice.exception.DuplicateResourceException;
import com.smartagriculture.weatherservice.exception.ResourceNotFoundException;
import com.smartagriculture.weatherservice.repository.WeatherRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;

import java.time.Duration;
import java.time.Instant;
import java.time.LocalDateTime;
import java.time.temporal.ChronoUnit;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.function.Function;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Slf4j
public class WeatherServiceImpl implements WeatherService {

    private final WeatherRepository weatherRepository;
    private final OpenMeteoClient openMeteoClient;

    @Value("${weather.forecast.refresh-minutes:180}")
    private long forecastRefreshMinutes;

    // Last successful Open-Meteo sync per rounded coordinate. In-memory on purpose: updatedAt can't
    // be used, since Hibernate skips the UPDATE (and the timestamp) when a re-fetched hour is unchanged.
    // Lost on restart, which only costs one extra fetch.
    private final Map<String, Instant> lastForecastSync = new ConcurrentHashMap<>();

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

        return toResponse(weatherRepository.saveAndFlush(record));
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

        return toResponse(weatherRepository.saveAndFlush(record));
    }

    @Override
    public void deleteWeatherRecord(String id) {
        WeatherRecord record = weatherRepository.findByIdAndDeletedFalse(id)
                .orElseThrow(() -> new ResourceNotFoundException("Weather record not found with id: " + id));
        record.setDeleted(true);
        record.setStatus(WeatherRecord.WeatherStatus.INACTIVE);
        weatherRepository.save(record);
    }

    @Override
    public List<WeatherDto.Response> getHourlyForecast(String countryCode, String region,
                                                       double latitude, double longitude, int hours) {
        // ~1 km grid, so nearby farmers share one cached forecast
        double lat = roundCoordinate(latitude);
        double lon = roundCoordinate(longitude);

        String key = lat + "," + lon;
        Instant lastSync = lastForecastSync.get(key);
        if (lastSync == null || lastSync.isBefore(Instant.now().minus(Duration.ofMinutes(forecastRefreshMinutes)))) {
            try {
                syncForecast(countryCode, region, lat, lon);
                lastForecastSync.put(key, Instant.now());
            } catch (Exception e) {
                // Serve whatever is cached (possibly stale or empty) rather than failing the caller
                log.warn("Open-Meteo sync failed for {}: {}", key, e.getMessage());
            }
        }

        LocalDateTime from = LocalDateTime.now().truncatedTo(ChronoUnit.HOURS);
        return weatherRepository
                .findByLatitudeAndLongitudeAndRecordTypeAndDeletedFalseAndRecordedAtBetweenOrderByRecordedAtAsc(
                        lat, lon, WeatherRecord.RecordType.FORECAST, from, from.plusHours(hours - 1L))
                .stream()
                .map(this::toResponse)
                .toList();
    }

    // Upsert by (lat, lon, recordedAt, FORECAST) — a re-sync refreshes existing hours instead of
    // duplicating them, so the user-facing 409 duplicate check doesn't apply here.
    private void syncForecast(String countryCode, String region, double lat, double lon) {
        List<WeatherRecord> fetched = openMeteoClient.fetchHourly(countryCode, region, lat, lon);
        if (fetched.isEmpty()) return;

        Map<LocalDateTime, WeatherRecord> existing = weatherRepository
                .findByLatitudeAndLongitudeAndRecordTypeAndDeletedFalseAndRecordedAtBetweenOrderByRecordedAtAsc(
                        lat, lon, WeatherRecord.RecordType.FORECAST,
                        fetched.getFirst().getRecordedAt(), fetched.getLast().getRecordedAt())
                .stream()
                .collect(Collectors.toMap(WeatherRecord::getRecordedAt, Function.identity(), (a, b) -> a));

        List<WeatherRecord> toSave = fetched.stream().map(f -> {
            WeatherRecord record = existing.get(f.getRecordedAt());
            if (record == null) return f;
            record.setTemperatureCelsius(f.getTemperatureCelsius());
            record.setHumidityPercent(f.getHumidityPercent());
            record.setRainfallMm(f.getRainfallMm());
            record.setWindSpeedKmh(f.getWindSpeedKmh());
            record.setWeatherCondition(f.getWeatherCondition());
            record.setDataSource(f.getDataSource());
            return record;
        }).toList();

        weatherRepository.saveAll(toSave);
        log.info("Synced {} Open-Meteo forecast hours for {},{}", toSave.size(), lat, lon);
    }

    static double roundCoordinate(double value) {
        return Math.round(value * 100.0) / 100.0;
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