package com.smartagriculture.farmassetservice.dto;

import com.smartagriculture.farmassetservice.entity.FarmAsset;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.*;

import java.time.LocalDateTime;

/**
 * FarmAssetDto is split into nested Request/Response classes.
 *
 * - Request: used for create/update (no id, no audit fields)
 * - Response: returned to clients (includes id, audit, computed fields)
 */
public class FarmAssetDto {

    // ── Create / Update Request ───────────────────────────────────────────────

    @Getter
    @Setter
    @NoArgsConstructor
    @AllArgsConstructor
    @Builder
    public static class Request {

        @NotBlank(message = "Farmer id is required")
        private String farmerId;

        @NotNull(message = "Asset type is required")
        private FarmAsset.AssetType assetType;

        @NotBlank(message = "Label is required")
        @Size(max = 100, message = "Label must not exceed 100 characters")
        private String label;

        private Double areaOrVolume;
        private FarmAsset.Unit unit;
        private String currentCropOrStock;
        private String stage;
        private Double latitude;
        private Double longitude;
        private FarmAsset.AssetStatus status;
    }

    // ── API Response ──────────────────────────────────────────────────────────

    @Getter
    @Setter
    @NoArgsConstructor
    @AllArgsConstructor
    @Builder
    public static class Response {

        private String id;
        private String farmerId;
        private FarmAsset.AssetType assetType;
        private String label;
        private Double areaOrVolume;
        private FarmAsset.Unit unit;
        private String currentCropOrStock;
        private String stage;
        private Double latitude;
        private Double longitude;
        private FarmAsset.AssetStatus status;

        private LocalDateTime createdAt;
        private LocalDateTime updatedAt;
    }
}
