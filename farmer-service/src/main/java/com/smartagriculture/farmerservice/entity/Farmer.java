package com.smartagriculture.farmerservice.entity;

import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;

import java.time.LocalDateTime;

@Entity
@Table(
    name = "farmers",
    indexes = {
        @Index(name = "idx_farmer_phone",   columnList = "phoneNumber"),
        @Index(name = "idx_farmer_country", columnList = "countryCode"),
        @Index(name = "idx_farmer_region",  columnList = "region"),
        @Index(name = "idx_farmer_status",  columnList = "status"),
        @Index(name = "idx_farmer_deleted", columnList = "deleted")
    }
)
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Farmer {

    // ── Identity ──────────────────────────────────────────────────────────────

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    @Column(updatable = false, nullable = false)
    private String id;

    @Column(nullable = false)
    private String name;

    // Primary identifier for farmers worldwide (E.164 format: +8801XXXXXXXXX)
    @Column(unique = true)
    private String phoneNumber;

    private String email;

    // ── Geography ─────────────────────────────────────────────────────────────

    // ISO 3166-1 alpha-2: BD, IN, US, KE, NG
    @Column(length = 2)
    private String countryCode;

    private String region;      // state / province / division
    private String district;    // sub-region for local routing

    // GPS coordinates for weather correlation and AI crop zone analysis
    private Double latitude;
    private Double longitude;

    // ── Farm Details ──────────────────────────────────────────────────────────

    private Double farmSizeHectares;

    // RICE, WHEAT, VEGETABLE, FRUIT, MIXED
    @Enumerated(EnumType.STRING)
    private FarmType farmType;

    // CLAY, LOAMY, SANDY, SILT — used by AI for fertilizer recommendations
    @Enumerated(EnumType.STRING)
    private SoilType soilType;

    // RAIN_FED, DRIP, FLOOD, SPRINKLER — used by AI for water management
    @Enumerated(EnumType.STRING)
    private IrrigationMethod irrigationMethod;

    // ── AI & Localization ─────────────────────────────────────────────────────

    // ISO 639-1: en, bn, hi, sw — AI responds in this language
    @Column(length = 5)
    private String preferredLanguage;

    // IANA timezone: Asia/Dhaka, Africa/Nairobi
    private String timezone;

    // SMARTPHONE, FEATURE_PHONE, DESKTOP — determines voice vs text experience
    @Enumerated(EnumType.STRING)
    private DeviceType deviceType;

    // Farmer has consented to AI data processing (GDPR/legal requirement)
    @Column(nullable = false)
    @Builder.Default
    private Boolean aiConsentGiven = false;

    // ── Lifecycle ─────────────────────────────────────────────────────────────

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    @Builder.Default
    private FarmerStatus status = FarmerStatus.ACTIVE;

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

    public enum FarmerStatus {
        ACTIVE, INACTIVE, SUSPENDED
    }

    public enum FarmType {
        RICE, WHEAT, VEGETABLE, FRUIT, MIXED, OTHER
    }

    public enum SoilType {
        CLAY, LOAMY, SANDY, SILT, UNKNOWN
    }

    public enum IrrigationMethod {
        RAIN_FED, DRIP, FLOOD, SPRINKLER, UNKNOWN
    }

    public enum DeviceType {
        SMARTPHONE, FEATURE_PHONE, DESKTOP, UNKNOWN
    }
}