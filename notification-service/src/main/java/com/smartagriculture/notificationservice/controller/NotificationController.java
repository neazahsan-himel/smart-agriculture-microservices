package com.smartagriculture.notificationservice.controller;

import com.smartagriculture.notificationservice.dto.NotificationDto;
import com.smartagriculture.notificationservice.service.NotificationService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/v1/notifications")
@RequiredArgsConstructor
@Slf4j
public class NotificationController {

    private static final int MAX_PAGE_SIZE = 100;

    private final NotificationService notificationService;

    @PostMapping
    public ResponseEntity<NotificationDto.Response> createNotification(
            @Valid @RequestBody NotificationDto.Request request) {
        log.info("POST /api/v1/notifications - farmerId={}", request.getFarmerId());
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(notificationService.createNotification(request));
    }

    @GetMapping
    public ResponseEntity<Page<NotificationDto.Response>> getAllNotifications(
            @RequestParam(defaultValue = "0")         int page,
            @RequestParam(defaultValue = "20")        int size,
            @RequestParam(defaultValue = "createdAt") String sortBy,
            @RequestParam(defaultValue = "desc")      String sortDir
    ) {
        return ResponseEntity.ok(notificationService.getAllNotifications(buildPageable(page, size, sortBy, sortDir)));
    }

    @GetMapping("/{id}")
    public ResponseEntity<NotificationDto.Response> getNotificationById(@PathVariable String id) {
        log.info("GET /api/v1/notifications/{}", id);
        return ResponseEntity.ok(notificationService.getNotificationById(id));
    }

    @GetMapping("/farmer/{farmerId}")
    public ResponseEntity<Page<NotificationDto.Response>> getNotificationsByFarmerId(
            @PathVariable String farmerId,
            @RequestParam(defaultValue = "0")         int page,
            @RequestParam(defaultValue = "20")        int size,
            @RequestParam(defaultValue = "createdAt") String sortBy,
            @RequestParam(defaultValue = "desc")      String sortDir
    ) {
        log.info("GET /api/v1/notifications/farmer/{}", farmerId);
        return ResponseEntity.ok(
                notificationService.getNotificationsByFarmerId(farmerId, buildPageable(page, size, sortBy, sortDir)));
    }

    @PutMapping("/{id}")
    public ResponseEntity<NotificationDto.Response> updateNotification(
            @PathVariable String id,
            @Valid @RequestBody NotificationDto.Request request) {
        log.info("PUT /api/v1/notifications/{}", id);
        return ResponseEntity.ok(notificationService.updateNotification(id, request));
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<String> deleteNotification(@PathVariable String id) {
        log.info("DELETE /api/v1/notifications/{}", id);
        notificationService.deleteNotification(id);
        return ResponseEntity.ok("Notification deleted successfully");
    }

    @PatchMapping("/{id}/sent")
    public ResponseEntity<NotificationDto.Response> markAsSent(@PathVariable String id) {
        log.info("PATCH /api/v1/notifications/{}/sent", id);
        return ResponseEntity.ok(notificationService.markAsSent(id));
    }

    @PatchMapping("/{id}/read")
    public ResponseEntity<NotificationDto.Response> markAsRead(@PathVariable String id) {
        log.info("PATCH /api/v1/notifications/{}/read", id);
        return ResponseEntity.ok(notificationService.markAsRead(id));
    }

    // ── Private Helpers ───────────────────────────────────────────────────────

    private Pageable buildPageable(int page, int size, String sortBy, String sortDir) {
        int safeSize = Math.min(size, MAX_PAGE_SIZE);
        Sort sort = sortDir.equalsIgnoreCase("asc")
                ? Sort.by(sortBy).ascending()
                : Sort.by(sortBy).descending();
        return PageRequest.of(page, safeSize, sort);
    }
}