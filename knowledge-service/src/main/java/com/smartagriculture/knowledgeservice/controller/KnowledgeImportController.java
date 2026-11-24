package com.smartagriculture.knowledgeservice.controller;

import com.smartagriculture.knowledgeservice.dto.ImportReport;
import com.smartagriculture.knowledgeservice.importer.ImportType;
import com.smartagriculture.knowledgeservice.importer.KnowledgeImportService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

/**
 * Bulk CSV import, e.g.
 * {@code curl -X POST --data-binary @pesticides.csv -H "Content-Type: text/csv" .../api/v1/knowledge/import/pesticides}.
 * Import sources first; fact rows reference them by sourceId or sourceTitle.
 */
@RestController
@RequestMapping("/api/v1/knowledge/import")
@RequiredArgsConstructor
@Slf4j
public class KnowledgeImportController {

    private final KnowledgeImportService importService;

    // 200 with a per-row report rather than 201: a batch can partly fail, and the report says which rows
    @PostMapping(value = "/{type}", consumes = {"text/csv", "text/plain"})
    public ResponseEntity<ImportReport> importCsv(@PathVariable String type, @RequestBody String csv) {
        ImportType importType = ImportType.fromPath(type);
        log.info("POST /api/v1/knowledge/import/{} - {} bytes", type, csv.length());
        return ResponseEntity.ok(importService.importCsv(importType, csv));
    }
}
