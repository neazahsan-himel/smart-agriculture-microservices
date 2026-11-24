package com.smartagriculture.knowledgeservice.repository;

import com.smartagriculture.knowledgeservice.entity.KnowledgeSource;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface KnowledgeSourceRepository extends JpaRepository<KnowledgeSource, String> {

    Optional<KnowledgeSource> findByIdAndDeletedFalse(String id);

    boolean existsByIdAndDeletedFalse(String id);

    Page<KnowledgeSource> findAllByDeletedFalse(Pageable pageable);

    // Duplicate rule: same title + edition (null edition matches IS NULL)
    boolean existsByTitleIgnoreCaseAndEditionAndDeletedFalse(String title, String edition);

    boolean existsByTitleIgnoreCaseAndEditionAndDeletedFalseAndIdNot(String title, String edition, String id);

    // CSV import lets fact rows name their source by title instead of UUID
    List<KnowledgeSource> findByTitleIgnoreCaseAndDeletedFalse(String title);
}
