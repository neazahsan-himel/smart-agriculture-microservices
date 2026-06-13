package com.smartagriculture.notificationservice.service;

import com.smartagriculture.notificationservice.dto.NotificationDto;
import com.smartagriculture.notificationservice.entity.Notification;
import com.smartagriculture.notificationservice.exception.DuplicateResourceException;
import com.smartagriculture.notificationservice.exception.ResourceNotFoundException;
import com.smartagriculture.notificationservice.repository.NotificationRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
@Slf4j
public class NotificationServiceImpl implements NotificationService {

    private final NotificationRepository notificationRepository;

    @Override
    @Transactional
    public NotificationDto.Response createNotification(NotificationDto.Request request) {
        log.info("Creating notification: farmerId={}, type={}, channel={}", request.getFarmerId(), request.getType(), request.getChannel());
        if (notificationRepository.existsByFarmerIdAndTitleAndTypeAndChannelAndStatusAndDeletedFalse(
                request.getFarmerId(), request.getTitle(),
                request.getType(), request.getChannel(),
                Notification.NotificationStatus.PENDING)) {
            throw new DuplicateResourceException(
                    "A PENDING notification with the same title, type, and channel already exists for this farmer");
        }
        return mapToResponse(notificationRepository.save(mapToEntity(request)));
    }

    @Override
    @Transactional(readOnly = true)
    public Page<NotificationDto.Response> getAllNotifications(Pageable pageable) {
        log.info("Fetching notifications - page={}, size={}", pageable.getPageNumber(), pageable.getPageSize());
        return notificationRepository.findAllByDeletedFalse(pageable).map(this::mapToResponse);
    }

    @Override
    @Transactional(readOnly = true)
    public NotificationDto.Response getNotificationById(String id) {
        log.info("Fetching notification id={}", id);
        return mapToResponse(findActiveById(id));
    }

    @Override
    @Transactional(readOnly = true)
    public Page<NotificationDto.Response> getNotificationsByFarmerId(String farmerId, Pageable pageable) {
        log.info("Fetching notifications for farmerId={}", farmerId);
        return notificationRepository.findAllByFarmerIdAndDeletedFalse(farmerId, pageable).map(this::mapToResponse);
    }

    @Override
    @Transactional
    public NotificationDto.Response updateNotification(String id, NotificationDto.Request request) {
        log.info("Updating notification id={}", id);
        Notification notification = findActiveById(id);

        notification.setTitle(request.getTitle());     // always required by @NotBlank
        notification.setFarmerId(request.getFarmerId()); // always required by @NotBlank

        if (request.getMessage() != null)  notification.setMessage(request.getMessage());
        if (request.getType() != null)     notification.setType(request.getType());
        if (request.getChannel() != null)  notification.setChannel(request.getChannel());
        if (request.getPriority() != null) notification.setPriority(request.getPriority());

        return mapToResponse(notificationRepository.save(notification));
    }

    @Override
    @Transactional
    public void deleteNotification(String id) {
        log.info("Soft-deleting notification id={}", id);
        Notification notification = findActiveById(id);
        notification.setDeleted(true);
        notificationRepository.save(notification);
    }

    @Override
    @Transactional
    public NotificationDto.Response markAsSent(String id) {
        log.info("Marking notification id={} as SENT", id);
        Notification notification = findActiveById(id);
        if (notification.getStatus() != Notification.NotificationStatus.PENDING) {
            throw new IllegalStateException("Only PENDING notifications can be marked as SENT");
        }
        notification.setStatus(Notification.NotificationStatus.SENT);
        return mapToResponse(notificationRepository.save(notification));
    }

    @Override
    @Transactional
    public NotificationDto.Response markAsRead(String id) {
        log.info("Marking notification id={} as READ", id);
        Notification notification = findActiveById(id);
        if (notification.getStatus() != Notification.NotificationStatus.SENT) {
            throw new IllegalStateException("Only SENT notifications can be marked as READ");
        }
        notification.setStatus(Notification.NotificationStatus.READ);
        return mapToResponse(notificationRepository.save(notification));
    }

    // ── Private Helpers ───────────────────────────────────────────────────────

    private Notification findActiveById(String id) {
        return notificationRepository.findByIdAndDeletedFalse(id)
                .orElseThrow(() -> new ResourceNotFoundException("Notification not found with id: " + id));
    }

    private Notification mapToEntity(NotificationDto.Request request) {
        return Notification.builder()
                .farmerId(request.getFarmerId())
                .title(request.getTitle())
                .message(request.getMessage())
                .type(request.getType())
                .channel(request.getChannel())
                .priority(request.getPriority() != null
                        ? request.getPriority()
                        : Notification.NotificationPriority.MEDIUM)
                .build();
    }

    private NotificationDto.Response mapToResponse(Notification n) {
        return NotificationDto.Response.builder()
                .id(n.getId())
                .farmerId(n.getFarmerId())
                .title(n.getTitle())
                .message(n.getMessage())
                .type(n.getType())
                .channel(n.getChannel())
                .status(n.getStatus())
                .priority(n.getPriority())
                .createdAt(n.getCreatedAt())
                .updatedAt(n.getUpdatedAt())
                .build();
    }
}