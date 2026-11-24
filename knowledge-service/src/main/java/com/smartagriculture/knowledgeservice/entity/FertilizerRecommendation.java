package com.smartagriculture.knowledgeservice.entity;

import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;

import java.time.LocalDateTime;

/** Fertiliser doses for one crop/season/region/soil-fertility combination, per hectare. */
@Entity
@Table(
    name = "fertilizer_recommendations",
    indexes = {
        @Index(name = "idx_fert_crop_season", columnList = "crop, season"),
        @Index(name = "idx_fert_deleted",     columnList = "deleted")
    }
)
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class FertilizerRecommendation {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    @Column(updatable = false, nullable = false)
    private String id;

    @Column(nullable = false)
    private String crop;

    @Enumerated(EnumType.STRING)
    private KnowledgeEnums.CropSeason season;

    private String region;        // region, district or agro-ecological zone, as the source uses

    private String soilFertilityClass;  // as written in the source, e.g. "medium"

    private Double ureaKgPerHa;
    private Double tspKgPerHa;
    private Double mopKgPerHa;
    private Double gypsumKgPerHa;
    private Double zincSulphateKgPerHa;

    @Column(length = 1000)
    private String splitSchedule;       // when each part is applied

    @Column(length = 1000)
    private String notes;

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
