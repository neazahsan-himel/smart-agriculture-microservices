package com.smartagriculture.knowledgeservice.repository;

import com.smartagriculture.knowledgeservice.entity.CropCalendarEntry;
import com.smartagriculture.knowledgeservice.entity.KnowledgeEnums;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.Optional;

public interface CropCalendarEntryRepository extends JpaRepository<CropCalendarEntry, String> {

    Optional<CropCalendarEntry> findByIdAndDeletedFalse(String id);

    // Duplicate rule: crop + season + region + activity (nulls match IS NULL)
    boolean existsByCropAndSeasonAndRegionAndActivityAndDeletedFalse(
            String crop, KnowledgeEnums.CropSeason season, String region, KnowledgeEnums.CalendarActivity activity);

    boolean existsByCropAndSeasonAndRegionAndActivityAndDeletedFalseAndIdNot(
            String crop, KnowledgeEnums.CropSeason season, String region, KnowledgeEnums.CalendarActivity activity, String id);

    // A null region on the entry means country-wide, so it matches any requested region
    @Query("""
            SELECT c FROM CropCalendarEntry c
            WHERE c.deleted = false
              AND (:crop IS NULL OR c.crop = :crop)
              AND (:season IS NULL OR c.season = :season)
              AND (:activity IS NULL OR c.activity = :activity)
              AND (:region IS NULL OR c.region IS NULL OR LOWER(c.region) LIKE LOWER(CONCAT('%', :region, '%')))
            """)
    Page<CropCalendarEntry> search(@Param("crop") String crop,
                                   @Param("season") KnowledgeEnums.CropSeason season,
                                   @Param("activity") KnowledgeEnums.CalendarActivity activity,
                                   @Param("region") String region,
                                   Pageable pageable);
}
