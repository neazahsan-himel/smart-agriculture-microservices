package com.smartagriculture.aiadvisorservice.controller;

import com.smartagriculture.aiadvisorservice.dto.ConversationDto;
import com.smartagriculture.aiadvisorservice.service.ConversationalAdvisorService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/v1/advisor/conversations")
@RequiredArgsConstructor
@Slf4j
public class ConversationController {

    private final ConversationalAdvisorService conversationalAdvisorService;

    @PostMapping
    public ResponseEntity<ConversationDto.Response> startConversation(
            @Valid @RequestBody ConversationDto.StartRequest request) {
        log.info("POST /api/v1/advisor/conversations - farmerId={}", request.getFarmerId());
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(conversationalAdvisorService.startConversation(request));
    }

    @PostMapping("/{id}/messages")
    public ResponseEntity<ConversationDto.Response> continueConversation(
            @PathVariable String id,
            @Valid @RequestBody ConversationDto.MessageRequest request) {
        log.info("POST /api/v1/advisor/conversations/{}/messages", id);
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(conversationalAdvisorService.continueConversation(id, request));
    }

    @GetMapping("/{id}")
    public ResponseEntity<ConversationDto.Response> getConversation(@PathVariable String id) {
        return ResponseEntity.ok(conversationalAdvisorService.getConversation(id));
    }

    @GetMapping
    public ResponseEntity<Page<ConversationDto.Response>> getConversationsByFarmer(
            @RequestParam String farmerId,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size,
            @RequestParam(defaultValue = "createdAt") String sortBy,
            @RequestParam(defaultValue = "desc") String sortDir) {
        return ResponseEntity.ok(conversationalAdvisorService.getConversationsByFarmer(
                farmerId, PageableUtil.build(page, size, sortBy, sortDir)));
    }
}
