package com.smartagriculture.farmassetservice.controller;

import com.smartagriculture.farmassetservice.dto.FarmAssetDto;
import com.smartagriculture.farmassetservice.service.FarmAssetService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/v1/farm-assets")
@RequiredArgsConstructor
@Slf4j
public class FarmAssetController {

    private static final int MAX_PAGE_SIZE = 100;

    private final FarmAssetService farmAssetService;

    @PostMapping
    public ResponseEntity<FarmAssetDto.Response> createFarmAsset(@Valid @RequestBody FarmAssetDto.Request request) {
        log.info("POST /api/v1/farm-assets - farmerId={}, label={}", request.getFarmerId(), request.getLabel());
        return ResponseEntity.status(HttpStatus.CREATED).body(farmAssetService.createFarmAsset(request));
    }

    /**
     * Paginated list with safe page-size cap.
     * Clients pass: ?page=0&size=20&sortBy=createdAt&sortDir=desc
     */
    @GetMapping
    public ResponseEntity<Page<FarmAssetDto.Response>> getAllFarmAssets(
            @RequestParam(defaultValue = "0")  int page,
            @RequestParam(defaultValue = "20") int size,
            @RequestParam(defaultValue = "createdAt") String sortBy,
            @RequestParam(defaultValue = "desc") String sortDir
    ) {
        return ResponseEntity.ok(farmAssetService.getAllFarmAssets(buildPageable(page, size, sortBy, sortDir)));
    }

    @GetMapping("/{id}")
    public ResponseEntity<FarmAssetDto.Response> getFarmAssetById(@PathVariable String id) {
        log.info("GET /api/v1/farm-assets/{}", id);
        return ResponseEntity.ok(farmAssetService.getFarmAssetById(id));
    }

    @GetMapping("/farmer/{farmerId}")
    public ResponseEntity<Page<FarmAssetDto.Response>> getFarmAssetsByFarmerId(
            @PathVariable String farmerId,
            @RequestParam(defaultValue = "0")  int page,
            @RequestParam(defaultValue = "20") int size,
            @RequestParam(defaultValue = "createdAt") String sortBy,
            @RequestParam(defaultValue = "desc") String sortDir
    ) {
        log.info("GET /api/v1/farm-assets/farmer/{}", farmerId);
        return ResponseEntity.ok(
                farmAssetService.getFarmAssetsByFarmerId(farmerId, buildPageable(page, size, sortBy, sortDir)));
    }

    @PutMapping("/{id}")
    public ResponseEntity<FarmAssetDto.Response> updateFarmAsset(
            @PathVariable String id,
            @Valid @RequestBody FarmAssetDto.Request request
    ) {
        log.info("PUT /api/v1/farm-assets/{}", id);
        return ResponseEntity.ok(farmAssetService.updateFarmAsset(id, request));
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<String> deleteFarmAsset(@PathVariable String id) {
        log.info("DELETE /api/v1/farm-assets/{}", id);
        farmAssetService.deleteFarmAsset(id);
        return ResponseEntity.ok("Farm asset deleted successfully");
    }

    // ── Private Helpers ───────────────────────────────────────────────────────

    private Pageable buildPageable(int page, int size, String sortBy, String sortDir) {
        int safeSize = Math.min(size, MAX_PAGE_SIZE);
        Sort sort = sortDir.equalsIgnoreCase("asc")
                ? Sort.by(sortBy).ascending()
                : Sort.by(sortBy).descending();
        return PageRequest.of(page, safeSize, sort);
    }
}
