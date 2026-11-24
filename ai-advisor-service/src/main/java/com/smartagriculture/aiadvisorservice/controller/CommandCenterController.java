package com.smartagriculture.aiadvisorservice.controller;

import com.smartagriculture.aiadvisorservice.dto.CommandCenterDto;
import com.smartagriculture.aiadvisorservice.service.CommandCenterService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/v1/advisor/command-center")
@RequiredArgsConstructor
@Slf4j
public class CommandCenterController {

    private final CommandCenterService commandCenterService;

    // 7-Day Farm Command Center: planned work per day, overdue work, daily forecast, recent events
    @GetMapping
    public ResponseEntity<CommandCenterDto.Response> getWeek(@RequestParam String farmerId,
                                                             @RequestParam(defaultValue = "7") int days) {
        log.info("GET /api/v1/advisor/command-center - farmerId={}, days={}", farmerId, days);
        return ResponseEntity.ok(commandCenterService.getWeek(farmerId, days));
    }
}
