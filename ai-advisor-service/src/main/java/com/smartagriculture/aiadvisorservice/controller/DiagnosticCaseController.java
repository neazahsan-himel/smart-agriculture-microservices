package com.smartagriculture.aiadvisorservice.controller;

import com.smartagriculture.aiadvisorservice.dto.DiagnosticCaseDto;
import com.smartagriculture.aiadvisorservice.service.ConversationalAdvisorService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/v1/advisor/diagnostic-cases")
@RequiredArgsConstructor
@Slf4j
public class DiagnosticCaseController {

    private final ConversationalAdvisorService conversationalAdvisorService;

    @PatchMapping("/{id}/outcome")
    public ResponseEntity<DiagnosticCaseDto.Response> recordOutcome(
            @PathVariable String id,
            @Valid @RequestBody DiagnosticCaseDto.OutcomeRequest request) {
        log.info("PATCH /api/v1/advisor/diagnostic-cases/{}/outcome - outcome={}", id, request.getOutcome());
        return ResponseEntity.ok(conversationalAdvisorService.recordOutcome(id, request.getOutcome()));
    }
}
