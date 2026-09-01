package com.smartagriculture.aiadvisorservice.repository;

import com.smartagriculture.aiadvisorservice.entity.FarmEvent;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface FarmEventRepository extends JpaRepository<FarmEvent, String> {

    List<FarmEvent> findTop5ByFarmerIdAndFarmAssetIdOrderByOccurredAtDesc(String farmerId, String farmAssetId);

    List<FarmEvent> findTop5ByFarmerIdOrderByOccurredAtDesc(String farmerId);

    Page<FarmEvent> findByFarmerIdAndFarmAssetId(String farmerId, String farmAssetId, Pageable pageable);

    Page<FarmEvent> findByFarmerId(String farmerId, Pageable pageable);
}
