package com.smartagriculture.knowledgeservice.controller;

import com.smartagriculture.knowledgeservice.dto.PestDiseaseEntryDto;
import com.smartagriculture.knowledgeservice.entity.KnowledgeEnums;
import com.smartagriculture.knowledgeservice.service.PestDiseaseEntryService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/v1/knowledge/pests-diseases")
@RequiredArgsConstructor
@Slf4j
public class PestDiseaseEntryController {

    private final PestDiseaseEntryService pestDiseaseService;

    @PostMapping
    public ResponseEntity<PestDiseaseEntryDto.Response> createEntry(@Valid @RequestBody PestDiseaseEntryDto.Request request) {
        log.info("POST /api/v1/knowledge/pests-diseases - crop={}, name={}", request.getCrop(), request.getNameEn());
        return ResponseEntity.status(HttpStatus.CREATED).body(pestDiseaseService.createEntry(request));
    }

    // All filters optional: ?crop=RICE&kind=DISEASE&q=spot (q searches names and symptoms)
    @GetMapping
    public ResponseEntity<Page<PestDiseaseEntryDto.Response>> searchEntries(
            @RequestParam(required = false) String crop,
            @RequestParam(required = false) KnowledgeEnums.PestDiseaseKind kind,
            @RequestParam(required = false) String q,
            @RequestParam(defaultValue = "0")      int page,
            @RequestParam(defaultValue = "20")     int size,
            @RequestParam(defaultValue = "nameEn") String sortBy,
            @RequestParam(defaultValue = "asc")    String sortDir) {
        return ResponseEntity.ok(pestDiseaseService.searchEntries(crop, kind, q,
                PageableUtil.build(page, size, sortBy, sortDir)));
    }

    @GetMapping("/{id}")
    public ResponseEntity<PestDiseaseEntryDto.Response> getEntryById(@PathVariable String id) {
        return ResponseEntity.ok(pestDiseaseService.getEntryById(id));
    }

    @PutMapping("/{id}")
    public ResponseEntity<PestDiseaseEntryDto.Response> updateEntry(@PathVariable String id,
                                                                    @Valid @RequestBody PestDiseaseEntryDto.Request request) {
        log.info("PUT /api/v1/knowledge/pests-diseases/{}", id);
        return ResponseEntity.ok(pestDiseaseService.updateEntry(id, request));
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<String> deleteEntry(@PathVariable String id) {
        log.info("DELETE /api/v1/knowledge/pests-diseases/{}", id);
        pestDiseaseService.deleteEntry(id);
        return ResponseEntity.ok("Pest/disease entry deleted successfully");
    }
}
