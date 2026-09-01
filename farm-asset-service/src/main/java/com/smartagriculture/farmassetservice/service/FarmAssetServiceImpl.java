package com.smartagriculture.farmassetservice.service;

import com.smartagriculture.farmassetservice.dto.FarmAssetDto;
import com.smartagriculture.farmassetservice.entity.FarmAsset;
import com.smartagriculture.farmassetservice.exception.DuplicateResourceException;
import com.smartagriculture.farmassetservice.exception.ResourceNotFoundException;
import com.smartagriculture.farmassetservice.repository.FarmAssetRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
@Slf4j
public class FarmAssetServiceImpl implements FarmAssetService {

    private final FarmAssetRepository farmAssetRepository;

    @Override
    @Transactional
    public FarmAssetDto.Response createFarmAsset(FarmAssetDto.Request request) {
        log.info("Creating farm asset: farmerId={}, label={}", request.getFarmerId(), request.getLabel());
        if (farmAssetRepository.findByFarmerIdAndLabelAndDeletedFalse(request.getFarmerId(), request.getLabel()).isPresent()) {
            throw new DuplicateResourceException(
                    "Farm asset already exists for farmer " + request.getFarmerId() + " with label: " + request.getLabel());
        }
        return mapToResponse(farmAssetRepository.save(mapToEntity(request)));
    }

    @Override
    @Transactional(readOnly = true)
    public Page<FarmAssetDto.Response> getAllFarmAssets(Pageable pageable) {
        log.info("Fetching farm assets - page={}, size={}", pageable.getPageNumber(), pageable.getPageSize());
        return farmAssetRepository.findAllByDeletedFalse(pageable)
                .map(this::mapToResponse);
    }

    @Override
    @Transactional(readOnly = true)
    public FarmAssetDto.Response getFarmAssetById(String id) {
        log.info("Fetching farm asset id={}", id);
        return mapToResponse(findActiveById(id));
    }

    @Override
    @Transactional(readOnly = true)
    public Page<FarmAssetDto.Response> getFarmAssetsByFarmerId(String farmerId, Pageable pageable) {
        log.info("Fetching farm assets for farmerId={}", farmerId);
        return farmAssetRepository.findByFarmerIdAndDeletedFalse(farmerId, pageable)
                .map(this::mapToResponse);
    }

    @Override
    @Transactional
    public FarmAssetDto.Response updateFarmAsset(String id, FarmAssetDto.Request request) {
        log.info("Updating farm asset id={}", id);
        FarmAsset asset = findActiveById(id);

        asset.setLabel(request.getLabel()); // always required by validation

        if (request.getFarmerId() != null)          asset.setFarmerId(request.getFarmerId());
        if (request.getAssetType() != null)         asset.setAssetType(request.getAssetType());
        if (request.getAreaOrVolume() != null)       asset.setAreaOrVolume(request.getAreaOrVolume());
        if (request.getUnit() != null)               asset.setUnit(request.getUnit());
        if (request.getCurrentCropOrStock() != null) asset.setCurrentCropOrStock(request.getCurrentCropOrStock());
        if (request.getStage() != null)              asset.setStage(request.getStage());
        if (request.getLatitude() != null)           asset.setLatitude(request.getLatitude());
        if (request.getLongitude() != null)          asset.setLongitude(request.getLongitude());
        if (request.getStatus() != null)             asset.setStatus(request.getStatus());

        return mapToResponse(farmAssetRepository.save(asset));
    }

    @Override
    @Transactional
    public void deleteFarmAsset(String id) {
        log.info("Soft-deleting farm asset id={}", id);
        FarmAsset asset = findActiveById(id);
        asset.setDeleted(true);
        asset.setStatus(FarmAsset.AssetStatus.INACTIVE);
        farmAssetRepository.save(asset);
    }

    // ── Private Helpers ───────────────────────────────────────────────────────

    private FarmAsset findActiveById(String id) {
        return farmAssetRepository.findByIdAndDeletedFalse(id)
                .orElseThrow(() -> new ResourceNotFoundException("Farm asset not found with id: " + id));
    }

    private FarmAsset mapToEntity(FarmAssetDto.Request request) {
        return FarmAsset.builder()
                .farmerId(request.getFarmerId())
                .assetType(request.getAssetType())
                .label(request.getLabel())
                .areaOrVolume(request.getAreaOrVolume())
                .unit(request.getUnit())
                .currentCropOrStock(request.getCurrentCropOrStock())
                .stage(request.getStage())
                .latitude(request.getLatitude())
                .longitude(request.getLongitude())
                .status(request.getStatus() != null ? request.getStatus() : FarmAsset.AssetStatus.ACTIVE)
                .build();
    }

    private FarmAssetDto.Response mapToResponse(FarmAsset asset) {
        return FarmAssetDto.Response.builder()
                .id(asset.getId())
                .farmerId(asset.getFarmerId())
                .assetType(asset.getAssetType())
                .label(asset.getLabel())
                .areaOrVolume(asset.getAreaOrVolume())
                .unit(asset.getUnit())
                .currentCropOrStock(asset.getCurrentCropOrStock())
                .stage(asset.getStage())
                .latitude(asset.getLatitude())
                .longitude(asset.getLongitude())
                .status(asset.getStatus())
                .createdAt(asset.getCreatedAt())
                .updatedAt(asset.getUpdatedAt())
                .build();
    }
}
