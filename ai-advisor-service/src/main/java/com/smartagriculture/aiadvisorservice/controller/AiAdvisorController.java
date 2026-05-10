package com.smartagriculture.aiadvisorservice.controller;

import com.smartagriculture.aiadvisorservice.dto.AdvisorDto;
import com.smartagriculture.aiadvisorservice.service.AiAdvisorService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/advisor")
@RequiredArgsConstructor
@Slf4j
public class AiAdvisorController {

    private final AiAdvisorService aiAdvisorService;

    @PostMapping("/advice")
    public ResponseEntity<AdvisorDto.Response> getAdvice(@Valid @RequestBody AdvisorDto.Request request) {
        log.info("POST /api/v1/advisor/advice - queryType={}, farmerId={}",
                request.getQueryType(), request.getFarmerId());
        return ResponseEntity.status(HttpStatus.CREATED).body(aiAdvisorService.getAdvice(request));
    }
}