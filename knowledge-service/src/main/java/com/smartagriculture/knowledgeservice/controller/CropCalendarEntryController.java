package com.smartagriculture.knowledgeservice.controller;

import com.smartagriculture.knowledgeservice.dto.CropCalendarEntryDto;
import com.smartagriculture.knowledgeservice.entity.KnowledgeEnums;
import com.smartagriculture.knowledgeservice.service.CropCalendarEntryService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/v1/knowledge/crop-calendar")
@RequiredArgsConstructor
@Slf4j
public class CropCalendarEntryController {

    private final CropCalendarEntryService calendarService;

    @PostMapping
    public ResponseEntity<CropCalendarEntryDto.Response> createEntry(@Valid @RequestBody CropCalendarEntryDto.Request request) {
        log.info("POST /api/v1/knowledge/crop-calendar - crop={}, season={}, activity={}",
                request.getCrop(), request.getSeason(), request.getActivity());
        return ResponseEntity.status(HttpStatus.CREATED).body(calendarService.createEntry(request));
    }

    // All filters optional: ?crop=RICE&season=BORO&activity=TRANSPLANTING&region=Gazipur
    @GetMapping
    public ResponseEntity<Page<CropCalendarEntryDto.Response>> searchEntries(
            @RequestParam(required = false) String crop,
            @RequestParam(required = false) KnowledgeEnums.CropSeason season,
            @RequestParam(required = false) KnowledgeEnums.CalendarActivity activity,
            @RequestParam(required = false) String region,
            @RequestParam(defaultValue = "0")          int page,
            @RequestParam(defaultValue = "20")         int size,
            @RequestParam(defaultValue = "startMonth") String sortBy,
            @RequestParam(defaultValue = "asc")        String sortDir) {
        return ResponseEntity.ok(calendarService.searchEntries(crop, season, activity, region,
                PageableUtil.build(page, size, sortBy, sortDir)));
    }

    @GetMapping("/{id}")
    public ResponseEntity<CropCalendarEntryDto.Response> getEntryById(@PathVariable String id) {
        return ResponseEntity.ok(calendarService.getEntryById(id));
    }

    @PutMapping("/{id}")
    public ResponseEntity<CropCalendarEntryDto.Response> updateEntry(@PathVariable String id,
                                                                     @Valid @RequestBody CropCalendarEntryDto.Request request) {
        log.info("PUT /api/v1/knowledge/crop-calendar/{}", id);
        return ResponseEntity.ok(calendarService.updateEntry(id, request));
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<String> deleteEntry(@PathVariable String id) {
        log.info("DELETE /api/v1/knowledge/crop-calendar/{}", id);
        calendarService.deleteEntry(id);
        return ResponseEntity.ok("Calendar entry deleted successfully");
    }
}
