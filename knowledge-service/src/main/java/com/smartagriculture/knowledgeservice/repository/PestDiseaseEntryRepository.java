package com.smartagriculture.knowledgeservice.repository;

import com.smartagriculture.knowledgeservice.entity.KnowledgeEnums;
import com.smartagriculture.knowledgeservice.entity.PestDiseaseEntry;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.Optional;

public interface PestDiseaseEntryRepository extends JpaRepository<PestDiseaseEntry, String> {

    Optional<PestDiseaseEntry> findByIdAndDeletedFalse(String id);

    // Duplicate rule: crop + English name
    boolean existsByCropAndNameEnIgnoreCaseAndDeletedFalse(String crop, String nameEn);

    boolean existsByCropAndNameEnIgnoreCaseAndDeletedFalseAndIdNot(String crop, String nameEn, String id);

    // q matches English/Bangla name or symptoms (simple keyword search until pilot Phase 2 retrieval)
    @Query("""
            SELECT e FROM PestDiseaseEntry e
            WHERE e.deleted = false
              AND (:crop IS NULL OR e.crop = :crop)
              AND (:kind IS NULL OR e.kind = :kind)
              AND (:q IS NULL
                   OR LOWER(e.nameEn) LIKE LOWER(CONCAT('%', :q, '%'))
                   OR e.nameBn LIKE CONCAT('%', :q, '%')
                   OR LOWER(e.symptoms) LIKE LOWER(CONCAT('%', :q, '%')))
            """)
    Page<PestDiseaseEntry> search(@Param("crop") String crop,
                                  @Param("kind") KnowledgeEnums.PestDiseaseKind kind,
                                  @Param("q") String q,
                                  Pageable pageable);
}
