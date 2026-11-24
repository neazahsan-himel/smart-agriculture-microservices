package com.smartagriculture.knowledgeservice.repository;

import com.smartagriculture.knowledgeservice.entity.FertilizerRecommendation;
import com.smartagriculture.knowledgeservice.entity.KnowledgeEnums;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.Optional;

public interface FertilizerRecommendationRepository extends JpaRepository<FertilizerRecommendation, String> {

    Optional<FertilizerRecommendation> findByIdAndDeletedFalse(String id);

    // Duplicate rule: crop + season + region/zone + soil fertility class (nulls match IS NULL)
    boolean existsByCropAndSeasonAndRegionAndSoilFertilityClassAndDeletedFalse(
            String crop, KnowledgeEnums.CropSeason season, String region, String soilFertilityClass);

    boolean existsByCropAndSeasonAndRegionAndSoilFertilityClassAndDeletedFalseAndIdNot(
            String crop, KnowledgeEnums.CropSeason season, String region, String soilFertilityClass, String id);

    @Query("""
            SELECT f FROM FertilizerRecommendation f
            WHERE f.deleted = false
              AND (:crop IS NULL OR f.crop = :crop)
              AND (:season IS NULL OR f.season = :season)
              AND (:region IS NULL OR LOWER(f.region) LIKE LOWER(CONCAT('%', :region, '%')))
            """)
    Page<FertilizerRecommendation> search(@Param("crop") String crop,
                                          @Param("season") KnowledgeEnums.CropSeason season,
                                          @Param("region") String region,
                                          Pageable pageable);
}
