package com.smartagriculture.farmassetservice.entity;

import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;

import java.time.LocalDateTime;

@Entity
@Table(
    name = "farm_assets",
    indexes = {
        @Index(name = "idx_farmasset_farmer",  columnList = "farmerId"),
        @Index(name = "idx_farmasset_type",    columnList = "assetType"),
        @Index(name = "idx_farmasset_status",  columnList = "status"),
        @Index(name = "idx_farmasset_deleted", columnList = "deleted")
    }
)
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class FarmAsset {

    // ── Identity ──────────────────────────────────────────────────────────────

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    @Column(updatable = false, nullable = false)
    private String id;

    // Cross-service reference — stored as plain String, no FK
    @Column(nullable = false)
    private String farmerId;

    // RICE_PLOT, POND, VEGETABLE_FIELD, ORCHARD, OTHER
    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private AssetType assetType;

    // Farmer-facing name, e.g. "Rice Plot 1", "Tilapia Pond"
    @Column(nullable = false)
    private String label;

    // ── Measurements ──────────────────────────────────────────────────────────

    private Double areaOrVolume;

    // HECTARE, DECIMAL, CUBIC_METER, OTHER
    @Enumerated(EnumType.STRING)
    private Unit unit;

    // Free-text link to crop-service catalog name / stocked species for now
    private String currentCropOrStock;

    private String stage;

    // ── Geography ─────────────────────────────────────────────────────────────

    private Double latitude;
    private Double longitude;

    // ── Lifecycle ─────────────────────────────────────────────────────────────

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    @Builder.Default
    private AssetStatus status = AssetStatus.ACTIVE;

    // Soft delete — never physically remove records
    @Column(nullable = false)
    @Builder.Default
    private Boolean deleted = false;

    // ── Audit ─────────────────────────────────────────────────────────────────

    @CreationTimestamp
    @Column(updatable = false)
    private LocalDateTime createdAt;

    @UpdateTimestamp
    private LocalDateTime updatedAt;

    // Optimistic locking — prevents race conditions when two requests update simultaneously
    @Version
    private Long version;


    // ── Enums ─────────────────────────────────────────────────────────────────

    public enum AssetType {
        RICE_PLOT, POND, VEGETABLE_FIELD, ORCHARD, OTHER
    }

    public enum Unit {
        HECTARE, DECIMAL, CUBIC_METER, OTHER
    }

    public enum AssetStatus {
        ACTIVE, FALLOW, HARVESTED, INACTIVE
    }
}
