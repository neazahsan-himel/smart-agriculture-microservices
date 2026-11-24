package com.smartagriculture.knowledgeservice.controller;

import com.smartagriculture.knowledgeservice.dto.PesticideProductDto;
import com.smartagriculture.knowledgeservice.entity.KnowledgeEnums;
import com.smartagriculture.knowledgeservice.service.PesticideProductService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/v1/knowledge/pesticides")
@RequiredArgsConstructor
@Slf4j
public class PesticideProductController {

    private final PesticideProductService pesticideService;

    @PostMapping
    public ResponseEntity<PesticideProductDto.Response> createPesticide(@Valid @RequestBody PesticideProductDto.Request request) {
        log.info("POST /api/v1/knowledge/pesticides - tradeName={}, crop={}", request.getTradeName(), request.getCrop());
        return ResponseEntity.status(HttpStatus.CREATED).body(pesticideService.createPesticide(request));
    }

    // All filters optional: ?crop=RICE&target=blast&activeIngredient=...&registrationStatus=REGISTERED
    @GetMapping
    public ResponseEntity<Page<PesticideProductDto.Response>> searchPesticides(
            @RequestParam(required = false) String crop,
            @RequestParam(required = false) String target,
            @RequestParam(required = false) String activeIngredient,
            @RequestParam(required = false) KnowledgeEnums.RegistrationStatus registrationStatus,
            @RequestParam(defaultValue = "0")         int page,
            @RequestParam(defaultValue = "20")        int size,
            @RequestParam(defaultValue = "tradeName") String sortBy,
            @RequestParam(defaultValue = "asc")       String sortDir) {
        return ResponseEntity.ok(pesticideService.searchPesticides(crop, target, activeIngredient, registrationStatus,
                PageableUtil.build(page, size, sortBy, sortDir)));
    }

    // Is a product the farmer or the LLM mentioned on the list? Empty list = unknown product.
    @GetMapping("/check")
    public ResponseEntity<List<PesticideProductDto.Response>> checkByName(@RequestParam String name,
                                                                          @RequestParam(required = false) String crop) {
        return ResponseEntity.ok(pesticideService.findByName(name, crop));
    }

    @GetMapping("/{id}")
    public ResponseEntity<PesticideProductDto.Response> getPesticideById(@PathVariable String id) {
        return ResponseEntity.ok(pesticideService.getPesticideById(id));
    }

    @PutMapping("/{id}")
    public ResponseEntity<PesticideProductDto.Response> updatePesticide(@PathVariable String id,
                                                                        @Valid @RequestBody PesticideProductDto.Request request) {
        log.info("PUT /api/v1/knowledge/pesticides/{}", id);
        return ResponseEntity.ok(pesticideService.updatePesticide(id, request));
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<String> deletePesticide(@PathVariable String id) {
        log.info("DELETE /api/v1/knowledge/pesticides/{}", id);
        pesticideService.deletePesticide(id);
        return ResponseEntity.ok("Pesticide use deleted successfully");
    }
}
