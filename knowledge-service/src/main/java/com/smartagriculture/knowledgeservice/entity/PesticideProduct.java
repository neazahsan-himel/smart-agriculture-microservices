package com.smartagriculture.knowledgeservice.entity;

import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;

import java.time.LocalDateTime;

/**
 * One registered use of a pesticide product: a product registered for several crops/targets
 * has one row per (crop, target), each with its own dose.
 */
@Entity
@Table(
    name = "pesticide_products",
    indexes = {
        @Index(name = "idx_pesticide_crop",       columnList = "crop"),
        @Index(name = "idx_pesticide_trade_name", columnList = "tradeName"),
        @Index(name = "idx_pesticide_active",     columnList = "activeIngredient"),
        @Index(name = "idx_pesticide_deleted",    columnList = "deleted")
    }
)
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class PesticideProduct {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    @Column(updatable = false, nullable = false)
    private String id;

    @Column(nullable = false)
    private String tradeName;

    @Column(nullable = false)
    private String activeIngredient;

    private String formulation;        // e.g. "75 WP", "5 G"

    private String registrationNo;

    @Column(nullable = false)
    private String crop;               // normalised upper case, e.g. "RICE"

    @Column(nullable = false)
    private String target;             // pest/disease it is registered against

    private Double doseValue;

    private String doseUnit;           // e.g. "g", "ml", "kg"

    private String doseBasis;          // e.g. "per litre of water", "per hectare"

    private Integer preHarvestIntervalDays;

    private String toxicityClass;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    @Builder.Default
    private KnowledgeEnums.RegistrationStatus registrationStatus = KnowledgeEnums.RegistrationStatus.UNKNOWN;

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
