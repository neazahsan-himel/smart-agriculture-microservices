package com.smartagriculture.cropservice.service;

import com.smartagriculture.cropservice.dto.CropDto;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

public interface CropService {

    CropDto.Response createCrop(CropDto.Request request);

    Page<CropDto.Response> getAllCrops(Pageable pageable);

    CropDto.Response getCropById(String id);

    CropDto.Response updateCrop(String id, CropDto.Request request);

    void deleteCrop(String id);
}