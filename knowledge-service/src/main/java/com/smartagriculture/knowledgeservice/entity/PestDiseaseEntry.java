package com.smartagriculture.knowledgeservice.entity;

import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;

import java.time.LocalDateTime;

/**
 * A pest, disease or nutrient deficiency of a crop: how to recognise it and how to manage it.
 * Chemical control lists active ingredients only; which products and doses are legal comes
 * from {@link PesticideProduct}.
 */
@Entity
@Table(
    name = "pest_disease_entries",
    indexes = {
        @Index(name = "idx_pest_crop",    columnList = "crop"),
        @Index(name = "idx_pest_deleted", columnList = "deleted")
    }
)
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class PestDiseaseEntry {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    @Column(updatable = false, nullable = false)
    private String id;

    @Column(nullable = false)
    private String crop;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private KnowledgeEnums.PestDiseaseKind kind;

    @Column(nullable = false)
    private String nameEn;

    private String nameBn;

    @Column(length = 2000)
    private String symptoms;

    @Column(length = 1000)
    private String favourableConditions;

    @Column(length = 500)
    private String actionThreshold;

    @Column(length = 2000)
    private String culturalControl;

    @Column(length = 1000)
    private String chemicalControlActiveIngredients;

    @Column(nullable = false)
    private String sourceId;

    private String sourcePage;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    @Builder.Default
    private KnowledgeEnums.RecordStatus status = KnowledgeEnums.RecordStatus.ACTIVE;

    @Column(nullable = false)
    @Builder.Default
    private Boolean deleted = false;

    @CreationTimestamp
    @Column(updatable = false)
    private LocalDateTime createdAt;

    @UpdateTimestamp
    private LocalDateTime updatedAt;

    @Version
    private Long version;
}
