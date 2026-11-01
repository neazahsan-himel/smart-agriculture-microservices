package com.smartagriculture.weatherservice.controller;

import com.smartagriculture.weatherservice.dto.WeatherDto;
import com.smartagriculture.weatherservice.service.WeatherService;
import jakarta.validation.Valid;
import jakarta.validation.constraints.DecimalMax;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/v1/weather-records")
@RequiredArgsConstructor
public class WeatherController {

    private final WeatherService weatherService;

    @PostMapping
    public ResponseEntity<WeatherDto.Response> createWeatherRecord(
            @Valid @RequestBody WeatherDto.Request request) {
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(weatherService.createWeatherRecord(request));
    }

    @GetMapping
    public ResponseEntity<Page<WeatherDto.Response>> getAllWeatherRecords(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") @Max(100) int size,
            @RequestParam(defaultValue = "recordedAt") String sortBy,
            @RequestParam(defaultValue = "desc") String sortDir) {

        Sort sort = sortDir.equalsIgnoreCase("asc")
                ? Sort.by(sortBy).ascending()
                : Sort.by(sortBy).descending();
        Pageable pageable = PageRequest.of(page, size, sort);
        return ResponseEntity.ok(weatherService.getAllWeatherRecords(pageable));
    }

    @GetMapping("/forecast")
    public ResponseEntity<List<WeatherDto.Response>> getHourlyForecast(
            @RequestParam @NotBlank String countryCode,
            @RequestParam(required = false) String region,
            @RequestParam @DecimalMin("-90.0") @DecimalMax("90.0") double latitude,
            @RequestParam @DecimalMin("-180.0") @DecimalMax("180.0") double longitude,
            @RequestParam(defaultValue = "48") @Min(1) @Max(72) int hours) {
        return ResponseEntity.ok(
                weatherService.getHourlyForecast(countryCode, region, latitude, longitude, hours));
    }

    @GetMapping("/{id}")
    public ResponseEntity<WeatherDto.Response> getWeatherRecordById(@PathVariable String id) {
        return ResponseEntity.ok(weatherService.getWeatherRecordById(id));
    }

    @PutMapping("/{id}")
    public ResponseEntity<WeatherDto.Response> updateWeatherRecord(
            @PathVariable String id,
            @Valid @RequestBody WeatherDto.Request request) {
        return ResponseEntity.ok(weatherService.updateWeatherRecord(id, request));
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<String> deleteWeatherRecord(@PathVariable String id) {
        weatherService.deleteWeatherRecord(id);
        return ResponseEntity.ok("Weather record deleted successfully");
    }
}