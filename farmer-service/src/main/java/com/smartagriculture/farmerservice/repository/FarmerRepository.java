package com.smartagriculture.farmerservice.repository;

import com.smartagriculture.farmerservice.entity.Farmer;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface FarmerRepository extends JpaRepository<Farmer, String> {

    // Excludes soft-deleted records from all standard queries
    Page<Farmer> findAllByDeletedFalse(Pageable pageable);

    Optional<Farmer> findByIdAndDeletedFalse(String id);

    boolean existsByPhoneNumberAndDeletedFalse(String phoneNumber);

    boolean existsByPhoneNumberAndDeletedFalseAndIdNot(String phoneNumber, String id);
}