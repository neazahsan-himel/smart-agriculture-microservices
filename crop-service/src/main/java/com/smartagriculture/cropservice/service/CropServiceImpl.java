package com.smartagriculture.cropservice.service;

import com.smartagriculture.cropservice.dto.CropDto;
import com.smartagriculture.cropservice.entity.Crop;
import com.smartagriculture.cropservice.exception.DuplicateResourceException;
import com.smartagriculture.cropservice.exception.ResourceNotFoundException;
import com.smartagriculture.cropservice.repository.CropRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
@Slf4j
public class CropServiceImpl implements CropService {

    private final CropRepository cropRepository;

    @Override
    @Transactional
    public CropDto.Response createCrop(CropDto.Request request) {
        log.info("Creating crop: name={}, variety={}", request.getName(), request.getVariety());
        if (cropRepository.existsByNameAndVarietyAndDeletedFalse(request.getName(), request.getVariety())) {
            throw new DuplicateResourceException(duplicateMessage(request.getName(), request.getVariety()));
        }
        return mapToResponse(cropRepository.save(mapToEntity(request)));
    }

    @Override
    @Transactional(readOnly = true)
    public Page<CropDto.Response> getAllCrops(Pageable pageable) {
        log.info("Fetching crops - page={}, size={}", pageable.getPageNumber(), pageable.getPageSize());
        return cropRepository.findAllByDeletedFalse(pageable).map(this::mapToResponse);
    }

    @Override
    @Transactional(readOnly = true)
    public CropDto.Response getCropById(String id) {
        log.info("Fetching crop id={}", id);
        return mapToResponse(findActiveById(id));
    }

    @Override
    @Transactional
    public CropDto.Response updateCrop(String id, CropDto.Request request) {
        log.info("Updating crop id={}", id);
        Crop crop = findActiveById(id);

        crop.setName(request.getName()); // always required by validation

        // Compute the effective variety after this update so the duplicate check is accurate
        String effectiveVariety = request.getVariety() != null ? request.getVariety() : crop.getVariety();
        if (cropRepository.existsByNameAndVarietyAndDeletedFalseAndIdNot(request.getName(), effectiveVariety, id)) {
            throw new DuplicateResourceException(duplicateMessage(request.getName(), effectiveVariety));
        }

        if (request.getVariety() != null)              crop.setVariety(request.getVariety());
        if (request.getDescription() != null)          crop.setDescription(request.getDescription());
        if (request.getCropType() != null)             crop.setCropType(request.getCropType());
        if (request.getSeason() != null)               crop.setSeason(request.getSeason());
        if (request.getIdealSoilType() != null)        crop.setIdealSoilType(request.getIdealSoilType());
        if (request.getMinTemperatureCelsius() != null) crop.setMinTemperatureCelsius(request.getMinTemperatureCelsius());
        if (request.getMaxTemperatureCelsius() != null) crop.setMaxTemperatureCelsius(request.getMaxTemperatureCelsius());
        if (request.getMinRainfallMm() != null)        crop.setMinRainfallMm(request.getMinRainfallMm());
        if (request.getMaxRainfallMm() != null)        crop.setMaxRainfallMm(request.getMaxRainfallMm());
        if (request.getGrowingDurationDays() != null)  crop.setGrowingDurationDays(request.getGrowingDurationDays());
        if (request.getIrrigationRequired() != null)   crop.setIrrigationRequired(request.getIrrigationRequired());
        if (request.getTypicalYieldPerHectare() != null) crop.setTypicalYieldPerHectare(request.getTypicalYieldPerHectare());
        if (request.getCountryCode() != null)          crop.setCountryCode(request.getCountryCode());
        if (request.getRegion() != null)               crop.setRegion(request.getRegion());

        return mapToResponse(cropRepository.save(crop));
    }

    @Override
    @Transactional
    public void deleteCrop(String id) {
        log.info("Soft-deleting crop id={}", id);
        Crop crop = findActiveById(id);
        crop.setDeleted(true);
        crop.setStatus(Crop.CropStatus.INACTIVE);
        cropRepository.save(crop);
    }

    // ── Private Helpers ───────────────────────────────────────────────────────

    private Crop findActiveById(String id) {
        return cropRepository.findByIdAndDeletedFalse(id)
                .orElseThrow(() -> new ResourceNotFoundException("Crop not found with id: " + id));
    }

    private String duplicateMessage(String name, String variety) {
        return variety != null
                ? "Crop already exists: " + name + " (" + variety + ")"
                : "Crop already exists: " + name;
    }

    private Crop mapToEntity(CropDto.Request request) {
        return Crop.builder()
                .name(request.getName())
                .variety(request.getVariety())
                .description(request.getDescription())
                .cropType(request.getCropType())
                .season(request.getSeason())
                .idealSoilType(request.getIdealSoilType())
                .minTemperatureCelsius(request.getMinTemperatureCelsius())
                .maxTemperatureCelsius(request.getMaxTemperatureCelsius())
                .minRainfallMm(request.getMinRainfallMm())
                .maxRainfallMm(request.getMaxRainfallMm())
                .growingDurationDays(request.getGrowingDurationDays())
                .irrigationRequired(request.getIrrigationRequired())
                .typicalYieldPerHectare(request.getTypicalYieldPerHectare())
                .countryCode(request.getCountryCode())
                .region(request.getRegion())
                .build();
    }

    private CropDto.Response mapToResponse(Crop crop) {
        return CropDto.Response.builder()
                .id(crop.getId())
                .name(crop.getName())
                .variety(crop.getVariety())
                .description(crop.getDescription())
                .cropType(crop.getCropType())
                .season(crop.getSeason())
                .idealSoilType(crop.getIdealSoilType())
                .minTemperatureCelsius(crop.getMinTemperatureCelsius())
                .maxTemperatureCelsius(crop.getMaxTemperatureCelsius())
                .minRainfallMm(crop.getMinRainfallMm())
                .maxRainfallMm(crop.getMaxRainfallMm())
                .growingDurationDays(crop.getGrowingDurationDays())
                .irrigationRequired(crop.getIrrigationRequired())
                .typicalYieldPerHectare(crop.getTypicalYieldPerHectare())
                .countryCode(crop.getCountryCode())
                .region(crop.getRegion())
                .status(crop.getStatus())
                .createdAt(crop.getCreatedAt())
                .updatedAt(crop.getUpdatedAt())
                .build();
    }
}