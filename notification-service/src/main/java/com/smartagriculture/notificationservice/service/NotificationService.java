package com.smartagriculture.notificationservice.service;

import com.smartagriculture.notificationservice.dto.NotificationDto;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

public interface NotificationService {

    NotificationDto.Response createNotification(NotificationDto.Request request);

    Page<NotificationDto.Response> getAllNotifications(Pageable pageable);

    NotificationDto.Response getNotificationById(String id);

    Page<NotificationDto.Response> getNotificationsByFarmerId(String farmerId, Pageable pageable);

    NotificationDto.Response updateNotification(String id, NotificationDto.Request request);

    void deleteNotification(String id);

    NotificationDto.Response markAsSent(String id);

    NotificationDto.Response markAsRead(String id);
}