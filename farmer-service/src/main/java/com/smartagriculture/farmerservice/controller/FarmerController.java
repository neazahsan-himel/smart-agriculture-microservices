package com.smartagriculture.farmerservice.controller;

import com.smartagriculture.farmerservice.dto.FarmerDto;
import com.smartagriculture.farmerservice.service.FarmerService;
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
@RequestMapping("/api/v1/farmers")
@RequiredArgsConstructor
@Slf4j
public class FarmerController {

    private static final int MAX_PAGE_SIZE = 100;

    private final FarmerService farmerService;

    @PostMapping
    public ResponseEntity<FarmerDto.Response> createFarmer(@Valid @RequestBody FarmerDto.Request request) {
        log.info("POST /api/v1/farmers - name={}", request.getName());
        return ResponseEntity.status(HttpStatus.CREATED).body(farmerService.createFarmer(request));
    }

    /**
     * Paginated list with safe page-size cap.
     * Clients pass: ?page=0&size=20&sort=createdAt,desc
     */
    @GetMapping
    public ResponseEntity<Page<FarmerDto.Response>> getAllFarmers(
            @RequestParam(defaultValue = "0")  int page,
            @RequestParam(defaultValue = "20") int size,
            @RequestParam(defaultValue = "createdAt") String sortBy,
            @RequestParam(defaultValue = "desc") String sortDir
    ) {
        int safeSize = Math.min(size, MAX_PAGE_SIZE);
        Sort sort = sortDir.equalsIgnoreCase("asc")
                ? Sort.by(sortBy).ascending()
                : Sort.by(sortBy).descending();

        Pageable pageable = PageRequest.of(page, safeSize, sort);
        return ResponseEntity.ok(farmerService.getAllFarmers(pageable));
    }

    @GetMapping("/{id}")
    public ResponseEntity<FarmerDto.Response> getFarmerById(@PathVariable String id) {
        log.info("GET /api/v1/farmers/{}", id);
        return ResponseEntity.ok(farmerService.getFarmerById(id));
    }

    @PutMapping("/{id}")
    public ResponseEntity<FarmerDto.Response> updateFarmer(
            @PathVariable String id,
            @Valid @RequestBody FarmerDto.Request request
    ) {
        log.info("PUT /api/v1/farmers/{}", id);
        return ResponseEntity.ok(farmerService.updateFarmer(id, request));
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<String> deleteFarmer(@PathVariable String id) {
        log.info("DELETE /api/v1/farmers/{}", id);
        farmerService.deleteFarmer(id);
        return ResponseEntity.ok("Farmer deleted successfully");
    }
}