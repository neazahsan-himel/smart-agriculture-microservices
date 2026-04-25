package com.smartagriculture.cropservice.entity;

import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;

import java.time.LocalDateTime;

@Entity
@Table(
    name = "crops",
    indexes = {
        @Index(name = "idx_crop_type",    columnList = "cropType"),
        @Index(name = "idx_crop_season",  columnList = "season"),
        @Index(name = "idx_crop_country", columnList = "countryCode"),
        @Index(name = "idx_crop_status",  columnList = "status"),
        @Index(name = "idx_crop_deleted", columnList = "deleted")
    }
)
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Crop {

    // ── Identity ──────────────────────────────────────────────────────────────

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    @Column(updatable = false, nullable = false)
    private String id;

    @Column(nullable = false)
    private String name;       // e.g. "Rice", "Wheat", "Tomato"

    private String variety;    // e.g. "Basmati", "IR-64" — null means generic

    @Column(length = 500)
    private String description;

    // ── Classification ────────────────────────────────────────────────────────

    // CEREAL, LEGUME, VEGETABLE, FRUIT, OILSEED, FIBER, SPICE, OTHER
    @Enumerated(EnumType.STRING)
    private CropType cropType;

    // KHARIF, RABI, ZAID, YEAR_ROUND
    @Enumerated(EnumType.STRING)
    private GrowingSeason season;

    // ── Growing Conditions (used by AI for recommendations) ───────────────────

    // CLAY, LOAMY, SANDY, SILT, UNKNOWN
    @Enumerated(EnumType.STRING)
    private SoilType idealSoilType;

    private Double minTemperatureCelsius;
    private Double maxTemperatureCelsius;
    private Double minRainfallMm;
    private Double maxRainfallMm;
    private Integer growingDurationDays;  // seed-to-harvest in days

    // ── Farming ───────────────────────────────────────────────────────────────

    private Boolean irrigationRequired;
    private Double typicalYieldPerHectare;  // tonnes/hectare

    // ── Geography ─────────────────────────────────────────────────────────────

    // ISO 3166-1 alpha-2: BD, IN, KE — primary country of cultivation
    @Column(length = 2)
    private String countryCode;

    private String region;

    // ── Lifecycle ─────────────────────────────────────────────────────────────

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    @Builder.Default
    private CropStatus status = CropStatus.ACTIVE;

    @Column(nullable = false)
    @Builder.Default
    private Boolean deleted = false;

    // ── Audit ─────────────────────────────────────────────────────────────────

    @CreationTimestamp
    @Column(updatable = false)
    private LocalDateTime createdAt;

    @UpdateTimestamp
    private LocalDateTime updatedAt;

    @Version
    private Long version;


    // ── Enums ─────────────────────────────────────────────────────────────────

    public enum CropType {
        CEREAL, LEGUME, VEGETABLE, FRUIT, OILSEED, FIBER, SPICE, OTHER
    }

    public enum GrowingSeason {
        KHARIF, RABI, ZAID, YEAR_ROUND
    }

    public enum SoilType {
        CLAY, LOAMY, SANDY, SILT, UNKNOWN
    }

    public enum CropStatus {
        ACTIVE, INACTIVE
    }
}