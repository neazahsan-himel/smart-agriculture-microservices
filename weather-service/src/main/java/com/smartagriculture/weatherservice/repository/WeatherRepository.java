package com.smartagriculture.weatherservice.repository;

import com.smartagriculture.weatherservice.entity.WeatherRecord;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.Optional;

@Repository
public interface WeatherRepository extends JpaRepository<WeatherRecord, String> {

    Page<WeatherRecord> findAllByDeletedFalse(Pageable pageable);

    Optional<WeatherRecord> findByIdAndDeletedFalse(String id);

    // Duplicate check: same location + time + type (region may be null — service layer enforces this)
    boolean existsByCountryCodeAndRegionAndRecordedAtAndRecordTypeAndDeletedFalse(
            String countryCode, String region, LocalDateTime recordedAt, WeatherRecord.RecordType recordType);

    boolean existsByCountryCodeAndRegionAndRecordedAtAndRecordTypeAndDeletedFalseAndIdNot(
            String countryCode, String region, LocalDateTime recordedAt, WeatherRecord.RecordType recordType, String id);
}