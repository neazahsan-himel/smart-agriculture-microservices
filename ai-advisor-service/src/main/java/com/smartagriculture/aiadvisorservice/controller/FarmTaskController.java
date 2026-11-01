package com.smartagriculture.aiadvisorservice.controller;

import com.smartagriculture.aiadvisorservice.dto.FarmTaskDto;
import com.smartagriculture.aiadvisorservice.service.ConversationalAdvisorService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/v1/advisor/farm-tasks")
@RequiredArgsConstructor
@Slf4j
public class FarmTaskController {

    private final ConversationalAdvisorService conversationalAdvisorService;

    // Plan a weather-dependent task (spray/fertilise/irrigate/harvest/sow); best time is evaluated immediately
    @PostMapping
    public ResponseEntity<FarmTaskDto.Response> createWeatherTask(@Valid @RequestBody FarmTaskDto.Request request) {
        log.info("POST /api/v1/advisor/farm-tasks - farmerId={}, taskType={}, dueDate={}",
                request.getFarmerId(), request.getTaskType(), request.getDueDate());
        return ResponseEntity.status(HttpStatus.CREATED).body(conversationalAdvisorService.createWeatherTask(request));
    }

    // Re-evaluates against the latest forecast
    @GetMapping("/{id}/best-time")
    public ResponseEntity<FarmTaskDto.Response> getBestTime(@PathVariable String id) {
        return ResponseEntity.ok(conversationalAdvisorService.refreshBestTime(id));
    }

    @GetMapping
    public ResponseEntity<Page<FarmTaskDto.Response>> getFarmTasks(
            @RequestParam String farmerId,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size,
            @RequestParam(defaultValue = "dueDate") String sortBy,
            @RequestParam(defaultValue = "asc") String sortDir) {
        return ResponseEntity.ok(conversationalAdvisorService.getFarmTasks(
                farmerId, PageableUtil.build(page, size, sortBy, sortDir)));
    }

    @PatchMapping("/{id}/status")
    public ResponseEntity<FarmTaskDto.Response> updateStatus(
            @PathVariable String id,
            @Valid @RequestBody FarmTaskDto.StatusUpdateRequest request) {
        log.info("PATCH /api/v1/advisor/farm-tasks/{}/status - status={}", id, request.getStatus());
        return ResponseEntity.ok(conversationalAdvisorService.updateFarmTaskStatus(id, request.getStatus()));
    }
}
