package com.smartagriculture.aiadvisorservice.controller;

import com.smartagriculture.aiadvisorservice.dto.FarmEventDto;
import com.smartagriculture.aiadvisorservice.service.ConversationalAdvisorService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/v1/advisor/farm-events")
@RequiredArgsConstructor
@Slf4j
public class FarmEventController {

    private final ConversationalAdvisorService conversationalAdvisorService;

    @GetMapping
    public ResponseEntity<Page<FarmEventDto.Response>> getFarmEvents(
            @RequestParam String farmerId,
            @RequestParam(required = false) String farmAssetId,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size,
            @RequestParam(defaultValue = "occurredAt") String sortBy,
            @RequestParam(defaultValue = "desc") String sortDir) {
        return ResponseEntity.ok(conversationalAdvisorService.getFarmEvents(
                farmerId, farmAssetId, PageableUtil.build(page, size, sortBy, sortDir)));
    }
}
