package com.smartagriculture.aiadvisorservice.client;

import com.smartagriculture.aiadvisorservice.dto.external.NotificationRequest;
import com.smartagriculture.aiadvisorservice.dto.external.NotificationResponse;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;

@FeignClient(name = "notification-service")
public interface NotificationServiceClient {

    @PostMapping("/api/v1/notifications")
    NotificationResponse createNotification(@RequestBody NotificationRequest request);
}
