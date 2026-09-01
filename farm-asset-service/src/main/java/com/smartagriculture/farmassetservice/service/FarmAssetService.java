package com.smartagriculture.farmassetservice.service;

import com.smartagriculture.farmassetservice.dto.FarmAssetDto;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

public interface FarmAssetService {

    FarmAssetDto.Response createFarmAsset(FarmAssetDto.Request request);

    Page<FarmAssetDto.Response> getAllFarmAssets(Pageable pageable);

    FarmAssetDto.Response getFarmAssetById(String id);

    Page<FarmAssetDto.Response> getFarmAssetsByFarmerId(String farmerId, Pageable pageable);

    FarmAssetDto.Response updateFarmAsset(String id, FarmAssetDto.Request request);

    void deleteFarmAsset(String id);
}
