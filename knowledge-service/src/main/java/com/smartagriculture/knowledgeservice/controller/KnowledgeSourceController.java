package com.smartagriculture.knowledgeservice.controller;

import com.smartagriculture.knowledgeservice.dto.KnowledgeSourceDto;
import com.smartagriculture.knowledgeservice.service.KnowledgeSourceService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/v1/knowledge/sources")
@RequiredArgsConstructor
@Slf4j
public class KnowledgeSourceController {

    private final KnowledgeSourceService sourceService;

    @PostMapping
    public ResponseEntity<KnowledgeSourceDto.Response> createSource(@Valid @RequestBody KnowledgeSourceDto.Request request) {
        log.info("POST /api/v1/knowledge/sources - title={}", request.getTitle());
        return ResponseEntity.status(HttpStatus.CREATED).body(sourceService.createSource(request));
    }

    @GetMapping
    public ResponseEntity<Page<KnowledgeSourceDto.Response>> getAllSources(
            @RequestParam(defaultValue = "0")         int page,
            @RequestParam(defaultValue = "20")        int size,
            @RequestParam(defaultValue = "createdAt") String sortBy,
            @RequestParam(defaultValue = "desc")      String sortDir) {
        return ResponseEntity.ok(sourceService.getAllSources(PageableUtil.build(page, size, sortBy, sortDir)));
    }

    @GetMapping("/{id}")
    public ResponseEntity<KnowledgeSourceDto.Response> getSourceById(@PathVariable String id) {
        return ResponseEntity.ok(sourceService.getSourceById(id));
    }

    @PutMapping("/{id}")
    public ResponseEntity<KnowledgeSourceDto.Response> updateSource(@PathVariable String id,
                                                                    @Valid @RequestBody KnowledgeSourceDto.Request request) {
        log.info("PUT /api/v1/knowledge/sources/{}", id);
        return ResponseEntity.ok(sourceService.updateSource(id, request));
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<String> deleteSource(@PathVariable String id) {
        log.info("DELETE /api/v1/knowledge/sources/{}", id);
        sourceService.deleteSource(id);
        return ResponseEntity.ok("Source deleted successfully");
    }
}
