package com.smartagriculture.cropservice.controller;

import com.smartagriculture.cropservice.dto.CropDto;
import com.smartagriculture.cropservice.service.CropService;
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
@RequestMapping("/api/v1/crops")
@RequiredArgsConstructor
@Slf4j
public class CropController {

    private static final int MAX_PAGE_SIZE = 100;

    private final CropService cropService;

    @PostMapping
    public ResponseEntity<CropDto.Response> createCrop(@Valid @RequestBody CropDto.Request request) {
        log.info("POST /api/v1/crops - name={}, variety={}", request.getName(), request.getVariety());
        return ResponseEntity.status(HttpStatus.CREATED).body(cropService.createCrop(request));
    }

    @GetMapping
    public ResponseEntity<Page<CropDto.Response>> getAllCrops(
            @RequestParam(defaultValue = "0")          int page,
            @RequestParam(defaultValue = "20")         int size,
            @RequestParam(defaultValue = "createdAt")  String sortBy,
            @RequestParam(defaultValue = "desc")       String sortDir
    ) {
        int safeSize = Math.min(size, MAX_PAGE_SIZE);
        Sort sort = sortDir.equalsIgnoreCase("asc")
                ? Sort.by(sortBy).ascending()
                : Sort.by(sortBy).descending();

        Pageable pageable = PageRequest.of(page, safeSize, sort);
        return ResponseEntity.ok(cropService.getAllCrops(pageable));
    }

    @GetMapping("/{id}")
    public ResponseEntity<CropDto.Response> getCropById(@PathVariable String id) {
        log.info("GET /api/v1/crops/{}", id);
        return ResponseEntity.ok(cropService.getCropById(id));
    }

    @PutMapping("/{id}")
    public ResponseEntity<CropDto.Response> updateCrop(
            @PathVariable String id,
            @Valid @RequestBody CropDto.Request request
    ) {
        log.info("PUT /api/v1/crops/{}", id);
        return ResponseEntity.ok(cropService.updateCrop(id, request));
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<String> deleteCrop(@PathVariable String id) {
        log.info("DELETE /api/v1/crops/{}", id);
        cropService.deleteCrop(id);
        return ResponseEntity.ok("Crop deleted successfully");
    }
}