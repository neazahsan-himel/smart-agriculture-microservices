package com.smartagriculture.apigateway.controller;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import reactor.core.publisher.Mono;

import java.time.LocalDateTime;
import java.util.LinkedHashMap;
import java.util.Map;

@RestController
@RequestMapping("/fallback")
public class FallbackController {

    @RequestMapping("/farmer-service")
    public Mono<ResponseEntity<Map<String, Object>>> farmerServiceFallback() {
        return Mono.just(buildFallback("farmer-service"));
    }

    @RequestMapping("/crop-service")
    public Mono<ResponseEntity<Map<String, Object>>> cropServiceFallback() {
        return Mono.just(buildFallback("crop-service"));
    }

    @RequestMapping("/weather-service")
    public Mono<ResponseEntity<Map<String, Object>>> weatherServiceFallback() {
        return Mono.just(buildFallback("weather-service"));
    }

    @RequestMapping("/ai-advisor-service")
    public Mono<ResponseEntity<Map<String, Object>>> aiAdvisorServiceFallback() {
        return Mono.just(buildFallback("ai-advisor-service"));
    }

    @RequestMapping("/notification-service")
    public Mono<ResponseEntity<Map<String, Object>>> notificationServiceFallback() {
        return Mono.just(buildFallback("notification-service"));
    }

    @RequestMapping("/farm-asset-service")
    public Mono<ResponseEntity<Map<String, Object>>> farmAssetServiceFallback() {
        return Mono.just(buildFallback("farm-asset-service"));
    }

    private ResponseEntity<Map<String, Object>> buildFallback(String serviceName) {
        Map<String, Object> body = new LinkedHashMap<>();
        body.put("timestamp", LocalDateTime.now().toString());
        body.put("status", 503);
        body.put("error", "Service Unavailable");
        body.put("message", serviceName + " is currently unavailable. Please try again later.");
        return ResponseEntity.status(HttpStatus.SERVICE_UNAVAILABLE).body(body);
    }
}