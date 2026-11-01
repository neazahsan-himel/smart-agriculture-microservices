package com.smartagriculture.aiadvisorservice.scheduler;

import com.smartagriculture.aiadvisorservice.client.NotificationServiceClient;
import com.smartagriculture.aiadvisorservice.dto.external.NotificationRequest;
import com.smartagriculture.aiadvisorservice.dto.external.WeatherSummary;
import com.smartagriculture.aiadvisorservice.entity.FarmTask;
import com.smartagriculture.aiadvisorservice.entity.WeatherTaskType;
import com.smartagriculture.aiadvisorservice.repository.FarmTaskRepository;
import com.smartagriculture.aiadvisorservice.service.weather.BestTimeEvaluator;
import com.smartagriculture.aiadvisorservice.service.weather.WeatherTaskAdvisor;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

/**
 * Weather-to-Decision trigger for weather-dependent tasks (the ones {@link FollowUpScheduler}
 * skips). Hourly, for each pending task due within the forecast horizon, it re-evaluates the best
 * time and notifies the farmer only when something changes:
 * <ul>
 *   <li>the recommended 3-hour slot moves (or no safe slot is left) → WEATHER_ALERT with the advice;</li>
 *   <li>the recommended slot starts within the hour → "good time now" alert, once per slot;</li>
 *   <li>no forecast is available and the task is due → plain reminder, so it is never dropped.</li>
 * </ul>
 * Like {@link FollowUpScheduler}, the task stays PENDING until the farmer marks it DONE/SKIPPED.
 */
@Component
@RequiredArgsConstructor
@Slf4j
public class WeatherTaskScheduler {

    static final String NO_WINDOW_SLOT = "NO_WINDOW";
    static final String DUE_REMINDER_SLOT = "DUE_REMINDER";

    private static final DateTimeFormatter TITLE_FORMAT = DateTimeFormatter.ofPattern("EEE d MMM HH:mm", Locale.ENGLISH);
    private static final DateTimeFormatter SLOT_KEY_FORMAT = DateTimeFormatter.ofPattern("yyyy-MM-dd'T'HH");

    private final FarmTaskRepository farmTaskRepository;
    private final WeatherTaskAdvisor weatherTaskAdvisor;
    private final NotificationServiceClient notificationServiceClient;

    @Scheduled(fixedRate = 3600000)
    public void checkWeatherDependentTasks() {
        LocalDateTime now = LocalDateTime.now();
        List<FarmTask> tasks = farmTaskRepository.findByStatusAndWeatherDependentTrueAndDeletedFalseAndDueDateLessThanEqual(
                FarmTask.TaskStatus.PENDING, now.plusHours(72));

        // One forecast fetch per farmer per run, however many tasks they have
        Map<String, List<WeatherSummary>> forecasts = new HashMap<>();
        int sent = 0;
        for (FarmTask task : tasks) {
            try {
                List<WeatherSummary> forecast = forecasts.computeIfAbsent(task.getFarmerId(), weatherTaskAdvisor::forecastFor);
                BestTimeEvaluator.BestTimeResult result = weatherTaskAdvisor.evaluate(task, forecast, now);
                sent += notifyIfChanged(task, result, now);
                farmTaskRepository.save(task);
            } catch (Exception e) {
                log.warn("Weather check failed for task {}: {}", task.getId(), e.getMessage());
            }
        }
        if (sent > 0) {
            log.info("WeatherTaskScheduler: checked {} weather-dependent tasks, sent {} notifications", tasks.size(), sent);
        }
    }

    /** Sends at most one notification for the task; returns how many were sent (0 or 1). */
    int notifyIfChanged(FarmTask task, BestTimeEvaluator.BestTimeResult result, LocalDateTime now) {
        WeatherTaskType type = WeatherTaskType.fromTaskType(task.getTaskType());
        String verb = type != null ? type.verb() : task.getTaskType();
        // Titles carry a date: notification-service rejects an identical PENDING title for the same farmer (409)
        String planned = task.getDueDate() != null ? " (planned " + task.getDueDate().format(TITLE_FORMAT) + ")" : "";

        switch (result.status()) {
            case NO_FORECAST -> {
                boolean due = task.getDueDate() == null || !task.getDueDate().isAfter(now);
                if (due && !DUE_REMINDER_SLOT.equals(task.getLastNotifiedSlot())
                        && send(task, "Reminder: " + verb + planned + " — check the weather first", result.advice())) {
                    task.setLastNotifiedSlot(DUE_REMINDER_SLOT);
                    return 1;
                }
                return 0;
            }
            case NO_WINDOW -> {
                if (!NO_WINDOW_SLOT.equals(task.getLastNotifiedSlot())
                        && send(task, "Weather plan: hold off — no safe time to " + verb + planned, result.advice())) {
                    task.setLastNotifiedSlot(NO_WINDOW_SLOT);
                    task.setStartNowNotified(false);
                    return 1;
                }
                return 0;
            }
            case RECOMMENDED -> {
                LocalDateTime start = result.recommendedStart();
                String slotKey = slotKey(start);
                if (!slotKey.equals(task.getLastNotifiedSlot())) {
                    task.setStartNowNotified(false);
                }
                // Scheduler runs hourly, so "within the next hour" is the last chance to say "now"
                boolean startingSoon = !start.isAfter(now.plusHours(1));
                if (startingSoon && !Boolean.TRUE.equals(task.getStartNowNotified())) {
                    if (send(task, "Good time to " + verb + " from " + start.format(TITLE_FORMAT), result.advice())) {
                        task.setLastNotifiedSlot(slotKey);
                        task.setStartNowNotified(true);
                        return 1;
                    }
                    return 0;
                }
                if (!slotKey.equals(task.getLastNotifiedSlot())
                        && send(task, "Weather plan: " + verb + " on " + start.format(TITLE_FORMAT), result.advice())) {
                    task.setLastNotifiedSlot(slotKey);
                    return 1;
                }
                return 0;
            }
        }
        return 0;
    }

    /** Recommended start rounded down to its 3-hour block, so small hour shifts don't re-notify. */
    static String slotKey(LocalDateTime start) {
        return start.withHour(start.getHour() / 3 * 3).format(SLOT_KEY_FORMAT);
    }

    private boolean send(FarmTask task, String title, String message) {
        try {
            notificationServiceClient.createNotification(NotificationRequest.builder()
                    .farmerId(task.getFarmerId())
                    .title(title)
                    .message(message)
                    .type("WEATHER_ALERT")
                    .channel("IN_APP")
                    .priority(task.getPriority().name())
                    .build());
            return true;
        } catch (Exception e) {
            // Not recorded as notified, so the next hourly run retries
            log.warn("Failed to send weather notification for task {}: {}", task.getId(), e.getMessage());
            return false;
        }
    }
}
