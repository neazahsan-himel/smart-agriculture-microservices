package com.smartagriculture.knowledgeservice.repository;

import com.smartagriculture.knowledgeservice.entity.KnowledgeEnums;
import com.smartagriculture.knowledgeservice.entity.PesticideProduct;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Optional;

public interface PesticideProductRepository extends JpaRepository<PesticideProduct, String> {

    Optional<PesticideProduct> findByIdAndDeletedFalse(String id);

    // Duplicate rule: one row per registered use = trade name + crop + target
    boolean existsByTradeNameIgnoreCaseAndCropAndTargetIgnoreCaseAndDeletedFalse(String tradeName, String crop, String target);

    boolean existsByTradeNameIgnoreCaseAndCropAndTargetIgnoreCaseAndDeletedFalseAndIdNot(
            String tradeName, String crop, String target, String id);

    // All filters optional; text filters are case-insensitive "contains"
    @Query("""
            SELECT p FROM PesticideProduct p
            WHERE p.deleted = false
              AND (:crop IS NULL OR p.crop = :crop)
              AND (:target IS NULL OR LOWER(p.target) LIKE LOWER(CONCAT('%', :target, '%')))
              AND (:activeIngredient IS NULL OR LOWER(p.activeIngredient) LIKE LOWER(CONCAT('%', :activeIngredient, '%')))
              AND (:registrationStatus IS NULL OR p.registrationStatus = :registrationStatus)
            """)
    Page<PesticideProduct> search(@Param("crop") String crop,
                                  @Param("target") String target,
                                  @Param("activeIngredient") String activeIngredient,
                                  @Param("registrationStatus") KnowledgeEnums.RegistrationStatus registrationStatus,
                                  Pageable pageable);

    // Exact name lookup (trade name or active ingredient) for the advisor's registered-product guard
    @Query("""
            SELECT p FROM PesticideProduct p
            WHERE p.deleted = false
              AND (LOWER(p.tradeName) = LOWER(:name) OR LOWER(p.activeIngredient) = LOWER(:name))
              AND (:crop IS NULL OR p.crop = :crop)
            """)
    List<PesticideProduct> findByName(@Param("name") String name, @Param("crop") String crop);
}
