package com.smartagriculture.notificationservice.repository;

import com.smartagriculture.notificationservice.entity.Notification;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface NotificationRepository extends JpaRepository<Notification, String> {

    Page<Notification> findAllByDeletedFalse(Pageable pageable);

    Optional<Notification> findByIdAndDeletedFalse(String id);

    Page<Notification> findAllByFarmerIdAndDeletedFalse(String farmerId, Pageable pageable);

    // Duplicate check: same PENDING notification for a farmer (title + type + channel)
    boolean existsByFarmerIdAndTitleAndTypeAndChannelAndStatusAndDeletedFalse(
            String farmerId,
            String title,
            Notification.NotificationType type,
            Notification.NotificationChannel channel,
            Notification.NotificationStatus status);
}