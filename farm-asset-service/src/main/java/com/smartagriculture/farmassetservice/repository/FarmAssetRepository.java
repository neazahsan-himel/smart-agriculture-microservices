package com.smartagriculture.farmassetservice.repository;

import com.smartagriculture.farmassetservice.entity.FarmAsset;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface FarmAssetRepository extends JpaRepository<FarmAsset, String> {

    // Excludes soft-deleted records from all standard queries
    Page<FarmAsset> findAllByDeletedFalse(Pageable pageable);

    Optional<FarmAsset> findByIdAndDeletedFalse(String id);

    Page<FarmAsset> findByFarmerIdAndDeletedFalse(String farmerId, Pageable pageable);

    Optional<FarmAsset> findByFarmerIdAndLabelAndDeletedFalse(String farmerId, String label);
}
