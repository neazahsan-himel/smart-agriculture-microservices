package com.smartagriculture.farmerservice.service;

import com.smartagriculture.farmerservice.dto.FarmerDto;
import com.smartagriculture.farmerservice.entity.Farmer;
import com.smartagriculture.farmerservice.exception.DuplicateResourceException;
import com.smartagriculture.farmerservice.exception.ResourceNotFoundException;
import com.smartagriculture.farmerservice.repository.FarmerRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
@Slf4j
public class FarmerServiceImpl implements FarmerService {

    private final FarmerRepository farmerRepository;

    @Override
    @Transactional
    public FarmerDto.Response createFarmer(FarmerDto.Request request) {
        log.info("Creating farmer: name={}, country={}", request.getName(), request.getCountryCode());
        if (request.getPhoneNumber() != null
                && farmerRepository.existsByPhoneNumberAndDeletedFalse(request.getPhoneNumber())) {
            throw new DuplicateResourceException("Phone number already registered: " + request.getPhoneNumber());
        }
        return mapToResponse(farmerRepository.save(mapToEntity(request)));
    }

    @Override
    @Transactional(readOnly = true)
    public Page<FarmerDto.Response> getAllFarmers(Pageable pageable) {
        log.info("Fetching farmers - page={}, size={}", pageable.getPageNumber(), pageable.getPageSize());
        return farmerRepository.findAllByDeletedFalse(pageable)
                .map(this::mapToResponse);
    }

    @Override
    @Transactional(readOnly = true)
    public FarmerDto.Response getFarmerById(String id) {
        log.info("Fetching farmer id={}", id);
        return mapToResponse(findActiveById(id));
    }

    @Override
    @Transactional
    public FarmerDto.Response updateFarmer(String id, FarmerDto.Request request) {
        log.info("Updating farmer id={}", id);
        Farmer farmer = findActiveById(id);

        farmer.setName(request.getName()); // always required by validation

        if (request.getPhoneNumber() != null) {
            if (!request.getPhoneNumber().equals(farmer.getPhoneNumber())
                    && farmerRepository.existsByPhoneNumberAndDeletedFalseAndIdNot(request.getPhoneNumber(), id)) {
                throw new DuplicateResourceException("Phone number already registered: " + request.getPhoneNumber());
            }
            farmer.setPhoneNumber(request.getPhoneNumber());
        }
        if (request.getEmail() != null)             farmer.setEmail(request.getEmail());
        if (request.getCountryCode() != null)       farmer.setCountryCode(request.getCountryCode());
        if (request.getRegion() != null)            farmer.setRegion(request.getRegion());
        if (request.getDistrict() != null)          farmer.setDistrict(request.getDistrict());
        if (request.getLatitude() != null)          farmer.setLatitude(request.getLatitude());
        if (request.getLongitude() != null)         farmer.setLongitude(request.getLongitude());
        if (request.getFarmSizeHectares() != null)  farmer.setFarmSizeHectares(request.getFarmSizeHectares());
        if (request.getFarmType() != null)          farmer.setFarmType(request.getFarmType());
        if (request.getSoilType() != null)          farmer.setSoilType(request.getSoilType());
        if (request.getIrrigationMethod() != null)  farmer.setIrrigationMethod(request.getIrrigationMethod());
        if (request.getPreferredLanguage() != null) farmer.setPreferredLanguage(request.getPreferredLanguage());
        if (request.getTimezone() != null)          farmer.setTimezone(request.getTimezone());
        if (request.getDeviceType() != null)        farmer.setDeviceType(request.getDeviceType());
        if (request.getAiConsentGiven() != null)    farmer.setAiConsentGiven(request.getAiConsentGiven());

        return mapToResponse(farmerRepository.save(farmer));
    }

    @Override
    @Transactional
    public void deleteFarmer(String id) {
        log.info("Soft-deleting farmer id={}", id);
        Farmer farmer = findActiveById(id);
        farmer.setDeleted(true);
        farmer.setStatus(Farmer.FarmerStatus.INACTIVE);
        farmerRepository.save(farmer);
    }

    // ── Private Helpers ───────────────────────────────────────────────────────

    private Farmer findActiveById(String id) {
        return farmerRepository.findByIdAndDeletedFalse(id)
                .orElseThrow(() -> new ResourceNotFoundException("Farmer not found with id: " + id));
    }

    private Farmer mapToEntity(FarmerDto.Request request) {
        return Farmer.builder()
                .name(request.getName())
                .phoneNumber(request.getPhoneNumber())
                .email(request.getEmail())
                .countryCode(request.getCountryCode())
                .region(request.getRegion())
                .district(request.getDistrict())
                .latitude(request.getLatitude())
                .longitude(request.getLongitude())
                .farmSizeHectares(request.getFarmSizeHectares())
                .farmType(request.getFarmType())
                .soilType(request.getSoilType())
                .irrigationMethod(request.getIrrigationMethod())
                .preferredLanguage(request.getPreferredLanguage())
                .timezone(request.getTimezone())
                .deviceType(request.getDeviceType())
                .aiConsentGiven(request.getAiConsentGiven() != null ? request.getAiConsentGiven() : false)
                .build();
    }

    private FarmerDto.Response mapToResponse(Farmer farmer) {
        return FarmerDto.Response.builder()
                .id(farmer.getId())
                .name(farmer.getName())
                .phoneNumber(farmer.getPhoneNumber())
                .email(farmer.getEmail())
                .countryCode(farmer.getCountryCode())
                .region(farmer.getRegion())
                .district(farmer.getDistrict())
                .latitude(farmer.getLatitude())
                .longitude(farmer.getLongitude())
                .farmSizeHectares(farmer.getFarmSizeHectares())
                .farmType(farmer.getFarmType())
                .soilType(farmer.getSoilType())
                .irrigationMethod(farmer.getIrrigationMethod())
                .preferredLanguage(farmer.getPreferredLanguage())
                .timezone(farmer.getTimezone())
                .deviceType(farmer.getDeviceType())
                .aiConsentGiven(farmer.getAiConsentGiven())
                .status(farmer.getStatus())
                .createdAt(farmer.getCreatedAt())
                .updatedAt(farmer.getUpdatedAt())
                .build();
    }
}