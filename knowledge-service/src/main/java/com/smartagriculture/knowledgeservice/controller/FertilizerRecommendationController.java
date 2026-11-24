package com.smartagriculture.knowledgeservice.controller;

import com.smartagriculture.knowledgeservice.dto.FertilizerRecommendationDto;
import com.smartagriculture.knowledgeservice.entity.KnowledgeEnums;
import com.smartagriculture.knowledgeservice.service.FertilizerRecommendationService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/v1/knowledge/fertilizer-recommendations")
@RequiredArgsConstructor
@Slf4j
public class FertilizerRecommendationController {

    private final FertilizerRecommendationService fertilizerService;

    @PostMapping
    public ResponseEntity<FertilizerRecommendationDto.Response> createRecommendation(
            @Valid @RequestBody FertilizerRecommendationDto.Request request) {
        log.info("POST /api/v1/knowledge/fertilizer-recommendations - crop={}, season={}", request.getCrop(), request.getSeason());
        return ResponseEntity.status(HttpStatus.CREATED).body(fertilizerService.createRecommendation(request));
    }

    // All filters optional: ?crop=RICE&season=BORO&region=Gazipur
    @GetMapping
    public ResponseEntity<Page<FertilizerRecommendationDto.Response>> searchRecommendations(
            @RequestParam(required = false) String crop,
            @RequestParam(required = false) KnowledgeEnums.CropSeason season,
            @RequestParam(required = false) String region,
            @RequestParam(defaultValue = "0")         int page,
            @RequestParam(defaultValue = "20")        int size,
            @RequestParam(defaultValue = "createdAt") String sortBy,
            @RequestParam(defaultValue = "desc")      String sortDir) {
        return ResponseEntity.ok(fertilizerService.searchRecommendations(crop, season, region,
                PageableUtil.build(page, size, sortBy, sortDir)));
    }

    @GetMapping("/{id}")
    public ResponseEntity<FertilizerRecommendationDto.Response> getRecommendationById(@PathVariable String id) {
        return ResponseEntity.ok(fertilizerService.getRecommendationById(id));
    }

    @PutMapping("/{id}")
    public ResponseEntity<FertilizerRecommendationDto.Response> updateRecommendation(
            @PathVariable String id, @Valid @RequestBody FertilizerRecommendationDto.Request request) {
        log.info("PUT /api/v1/knowledge/fertilizer-recommendations/{}", id);
        return ResponseEntity.ok(fertilizerService.updateRecommendation(id, request));
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<String> deleteRecommendation(@PathVariable String id) {
        log.info("DELETE /api/v1/knowledge/fertilizer-recommendations/{}", id);
        fertilizerService.deleteRecommendation(id);
        return ResponseEntity.ok("Fertiliser recommendation deleted successfully");
    }
}
