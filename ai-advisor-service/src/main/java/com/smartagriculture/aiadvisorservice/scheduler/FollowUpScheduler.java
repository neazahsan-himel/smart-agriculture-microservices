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
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.Locale;

/**
 * Proactive Follow-up Agent: hourly sweep for diagnostic cases and farm tasks that are
 * due, pushing a notification via notification-service for each. Weather-dependent tasks
 * are skipped here — {@link WeatherTaskScheduler} handles them with a weather-aware trigger.
 * Each case and each task due date is notified once; failed sends are retried on the next run.
 */
@Component
@RequiredArgsConstructor
@Slf4j
public class FollowUpScheduler {

    private static final DateTimeFormatter TITLE_FORMAT = DateTimeFormatter.ofPattern("EEE d MMM HH:mm", Locale.ENGLISH);

    private final DiagnosticCaseRepository diagnosticCaseRepository;
    private final FarmTaskRepository farmTaskRepository;
    private final NotificationServiceClient notificationServiceClient;

    @Scheduled(fixedRate = 3600000)
    public void sendDueFollowUps() {
        LocalDateTime now = LocalDateTime.now();

        List<DiagnosticCase> dueCases = diagnosticCaseRepository
                .findByStatusAndFollowUpDueAtLessThanEqual(DiagnosticCase.CaseStatus.AWAITING_FOLLOWUP, now);
        long casesSent = dueCases.stream().filter(this::sendCaseFollowUp).count();

        List<FarmTask> dueTasks = farmTaskRepository
                .findByStatusAndWeatherDependentFalseAndDeletedFalseAndDueDateLessThanEqual(FarmTask.TaskStatus.PENDING, now);
        long remindersSent = dueTasks.stream().filter(this::sendTaskReminder).count();

        if (casesSent > 0 || remindersSent > 0) {
            log.info("FollowUpScheduler: sent {} case follow-ups, {} task reminders", casesSent, remindersSent);
        }
    }

    /** Returns true if a notification was sent. */
    boolean sendCaseFollowUp(DiagnosticCase diagnosticCase) {
        try {
            // Titles carry a date: notification-service rejects an identical PENDING title for the same farmer (409)
            notificationServiceClient.createNotification(NotificationRequest.builder()
                    .farmerId(diagnosticCase.getFarmerId())
                    .title("Follow-up: how is your crop doing? (" + diagnosticCase.getFollowUpDueAt().format(TITLE_FORMAT) + ")")
                    .message("A few days ago you reported: \"" + diagnosticCase.getSymptom()
                            + "\". Please let us know how it's going, or send a new photo/message.")
                    .type("CROP_ADVISORY")
                    .channel("IN_APP")
                    .priority(mapRiskToPriority(diagnosticCase.getRiskLevel()))
                    .build());

            diagnosticCase.setStatus(DiagnosticCase.CaseStatus.FOLLOWUP_SENT);
            diagnosticCaseRepository.save(diagnosticCase);
            return true;
        } catch (Exception e) {
            log.warn("Failed to send follow-up notification for case {}: {}", diagnosticCase.getId(), e.getMessage());
            return false;
        }
    }

    /** Returns true if a reminder was sent; false if already reminded for this due date or the send failed. */
    boolean sendTaskReminder(FarmTask task) {
        // Task itself stays PENDING until the farmer marks it DONE/SKIPPED via the API;
        // remind once per due date — rescheduling the task re-arms the reminder.
        if (task.getDueDate().equals(task.getReminderSentFor())) {
            return false;
        }
        try {
            notificationServiceClient.createNotification(NotificationRequest.builder()
                    .farmerId(task.getFarmerId())
                    .title("Reminder: " + task.getTaskType() + " (due " + task.getDueDate().format(TITLE_FORMAT) + ")")
                    .message("A scheduled farm task is due now: " + task.getTaskType())
                    .type("CROP_ADVISORY")
                    .channel("IN_APP")
                    .priority(task.getPriority().name())
                    .build());

            task.setReminderSentFor(task.getDueDate());
            farmTaskRepository.save(task);
            return true;
        } catch (Exception e) {
            // Not recorded as reminded, so the next hourly run retries
            log.warn("Failed to send task reminder for task {}: {}", task.getId(), e.getMessage());
            return false;
        }
    }

    private String mapRiskToPriority(DiagnosticCase.RiskLevel risk) {
        if (risk == null) return "MEDIUM";
        return risk.name();
    }
}
