package com.smartagriculture.farmerservice.service;

import com.smartagriculture.farmerservice.dto.FarmerDto;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

public interface FarmerService {

    FarmerDto.Response createFarmer(FarmerDto.Request request);

    Page<FarmerDto.Response> getAllFarmers(Pageable pageable);

    FarmerDto.Response getFarmerById(String id);

    FarmerDto.Response updateFarmer(String id, FarmerDto.Request request);

    void deleteFarmer(String id);
}