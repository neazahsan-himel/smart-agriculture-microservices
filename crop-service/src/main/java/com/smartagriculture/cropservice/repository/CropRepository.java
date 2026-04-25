package com.smartagriculture.cropservice.repository;

import com.smartagriculture.cropservice.entity.Crop;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface CropRepository extends JpaRepository<Crop, String> {

    Page<Crop> findAllByDeletedFalse(Pageable pageable);

    Optional<Crop> findByIdAndDeletedFalse(String id);

    // Spring Data generates IS NULL when variety is null — handles generic crops correctly
    boolean existsByNameAndVarietyAndDeletedFalse(String name, String variety);

    boolean existsByNameAndVarietyAndDeletedFalseAndIdNot(String name, String variety, String id);
}