package com.smartagriculture.aiadvisorservice.scheduler;

import com.smartagriculture.aiadvisorservice.client.NotificationServiceClient;
import com.smartagriculture.aiadvisorservice.dto.external.NotificationRequest;
import com.smartagriculture.aiadvisorservice.entity.DiagnosticCase;
import com.smartagriculture.aiadvisorservice.entity.FarmTask;
import com.smartagriculture.aiadvisorservice.repository.DiagnosticCaseRepository;
import com.smartagriculture.aiadvisorservice.repository.FarmTaskRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.time.LocalDateTime;
import java.util.List;

/**
 * Proactive Follow-up Agent: hourly sweep for diagnostic cases and farm tasks that are
 * due, pushing a notification via notification-service for each. Weather-dependent tasks
 * are skipped here — they need a weather-aware trigger, not a plain time check.
 */
@Component
@RequiredArgsConstructor
@Slf4j
public class FollowUpScheduler {

    private final DiagnosticCaseRepository diagnosticCaseRepository;
    private final FarmTaskRepository farmTaskRepository;
    private final NotificationServiceClient notificationServiceClient;

    @Scheduled(fixedRate = 3600000)
    public void sendDueFollowUps() {
        LocalDateTime now = LocalDateTime.now();

        List<DiagnosticCase> dueCases = diagnosticCaseRepository
                .findByStatusAndFollowUpDueAtLessThanEqual(DiagnosticCase.CaseStatus.AWAITING_FOLLOWUP, now);
        dueCases.forEach(this::sendCaseFollowUp);

        List<FarmTask> dueTasks = farmTaskRepository
                .findByStatusAndWeatherDependentFalseAndDueDateLessThanEqual(FarmTask.TaskStatus.PENDING, now);
        dueTasks.forEach(this::sendTaskReminder);

        if (!dueCases.isEmpty() || !dueTasks.isEmpty()) {
            log.info("FollowUpScheduler: sent {} case follow-ups, {} task reminders", dueCases.size(), dueTasks.size());
        }
    }

    private void sendCaseFollowUp(DiagnosticCase diagnosticCase) {
        try {
            notificationServiceClient.createNotification(NotificationRequest.builder()
                    .farmerId(diagnosticCase.getFarmerId())
                    .title("Follow-up: how is your crop doing?")
                    .message("A few days ago you reported: \"" + diagnosticCase.getSymptom()
                            + "\". Please let us know how it's going, or send a new photo/message.")
                    .type("CROP_ADVISORY")
                    .channel("IN_APP")
                    .priority(mapRiskToPriority(diagnosticCase.getRiskLevel()))
                    .build());

            diagnosticCase.setStatus(DiagnosticCase.CaseStatus.FOLLOWUP_SENT);
            diagnosticCaseRepository.save(diagnosticCase);
        } catch (Exception e) {
            log.warn("Failed to send follow-up notification for case {}: {}", diagnosticCase.getId(), e.getMessage());
        }
    }

    private void sendTaskReminder(FarmTask task) {
        try {
            notificationServiceClient.createNotification(NotificationRequest.builder()
                    .farmerId(task.getFarmerId())
                    .title("Reminder: " + task.getTaskType())
                    .message("A scheduled farm task is due now: " + task.getTaskType())
                    .type("CROP_ADVISORY")
                    .channel("IN_APP")
                    .priority(task.getPriority().name())
                    .build());
        } catch (Exception e) {
            log.warn("Failed to send task reminder for task {}: {}", task.getId(), e.getMessage());
        }
        // Task itself stays PENDING until the farmer marks it DONE/SKIPPED via the API;
        // the reminder can legitimately repeat hourly until then.
    }

    private String mapRiskToPriority(DiagnosticCase.RiskLevel risk) {
        if (risk == null) return "MEDIUM";
        return risk.name();
    }
}
