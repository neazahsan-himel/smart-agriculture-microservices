package com.smartagriculture.aiadvisorservice.scheduler;

import com.smartagriculture.aiadvisorservice.client.NotificationServiceClient;
import com.smartagriculture.aiadvisorservice.dto.external.NotificationRequest;
import com.smartagriculture.aiadvisorservice.entity.FarmTask;
import com.smartagriculture.aiadvisorservice.service.weather.BestTimeEvaluator.BestTimeResult;
import com.smartagriculture.aiadvisorservice.service.weather.BestTimeEvaluator.Status;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;

import java.time.LocalDateTime;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/** Notification de-duplication rules of {@link WeatherTaskScheduler#notifyIfChanged}. */
class WeatherTaskSchedulerTest {

    private static final LocalDateTime NOW = LocalDateTime.of(2026, 10, 29, 8, 30);

    private NotificationServiceClient notificationClient;
    private WeatherTaskScheduler scheduler;
    private FarmTask task;

    @BeforeEach
    void setUp() {
        notificationClient = mock(NotificationServiceClient.class);
        scheduler = new WeatherTaskScheduler(null, null, notificationClient);
        task = FarmTask.builder()
                .id("t1").farmerId("f1").taskType("SPRAY")
                .dueDate(NOW.plusDays(1)).weatherDependent(true)
                .build();
    }

    @Test
    void recommendedSlot_notifiesOnceThenStaysQuietWhileTheSlotIsUnchanged() {
        BestTimeResult result = recommended(NOW.plusHours(8)); // Thu 16:30 → block 15:00

        assertThat(scheduler.notifyIfChanged(task, result, NOW)).isEqualTo(1);
        assertThat(scheduler.notifyIfChanged(task, result, NOW.plusHours(1))).isZero();

        ArgumentCaptor<NotificationRequest> sent = ArgumentCaptor.forClass(NotificationRequest.class);
        verify(notificationClient, times(1)).createNotification(sent.capture());
        assertThat(sent.getValue().getTitle()).isEqualTo("Weather plan: spray on Thu 29 Oct 16:30");
        assertThat(sent.getValue().getType()).isEqualTo("WEATHER_ALERT");
        assertThat(sent.getValue().getMessage()).isEqualTo("advice");
        assertThat(task.getLastNotifiedSlot()).isEqualTo("2026-10-29T15");
    }

    @Test
    void smallShiftWithinTheSame3hBlock_doesNotRenotify_butMovingBlocksDoes() {
        scheduler.notifyIfChanged(task, recommended(LocalDateTime.of(2026, 10, 29, 16, 0)), NOW);
        scheduler.notifyIfChanged(task, recommended(LocalDateTime.of(2026, 10, 29, 17, 0)), NOW); // same 15-18 block
        scheduler.notifyIfChanged(task, recommended(LocalDateTime.of(2026, 10, 30, 6, 0)), NOW);  // new block

        verify(notificationClient, times(2)).createNotification(any());
        assertThat(task.getLastNotifiedSlot()).isEqualTo("2026-10-30T06");
    }

    @Test
    void slotStartingWithinTheHour_sendsGoodTimeNowOnce() {
        BestTimeResult result = recommended(LocalDateTime.of(2026, 10, 29, 9, 0));

        assertThat(scheduler.notifyIfChanged(task, result, NOW)).isEqualTo(1);
        assertThat(scheduler.notifyIfChanged(task, result, NOW.plusMinutes(20))).isZero();

        ArgumentCaptor<NotificationRequest> sent = ArgumentCaptor.forClass(NotificationRequest.class);
        verify(notificationClient, times(1)).createNotification(sent.capture());
        assertThat(sent.getValue().getTitle()).isEqualTo("Good time to spray from Thu 29 Oct 09:00");
        assertThat(task.getStartNowNotified()).isTrue();
    }

    @Test
    void earlierPlanNotification_isFollowedByGoodTimeNowWhenTheSlotArrives() {
        LocalDateTime slot = LocalDateTime.of(2026, 10, 29, 16, 0);
        scheduler.notifyIfChanged(task, recommended(slot), NOW);              // plan alert
        scheduler.notifyIfChanged(task, recommended(slot), slot.minusMinutes(40)); // now alert

        verify(notificationClient, times(2)).createNotification(any());
        assertThat(task.getStartNowNotified()).isTrue();
    }

    @Test
    void noWindow_notifiesOnce() {
        BestTimeResult result = new BestTimeResult(Status.NO_WINDOW, null, "no safe time");

        assertThat(scheduler.notifyIfChanged(task, result, NOW)).isEqualTo(1);
        assertThat(scheduler.notifyIfChanged(task, result, NOW.plusHours(1))).isZero();
        assertThat(task.getLastNotifiedSlot()).isEqualTo(WeatherTaskScheduler.NO_WINDOW_SLOT);
    }

    @Test
    void noForecast_onlyRemindsOnceTheTaskIsDue() {
        BestTimeResult result = new BestTimeResult(Status.NO_FORECAST, null, "no forecast");

        assertThat(scheduler.notifyIfChanged(task, result, NOW)).isZero(); // due tomorrow
        LocalDateTime afterDue = task.getDueDate().plusMinutes(5);
        assertThat(scheduler.notifyIfChanged(task, result, afterDue)).isEqualTo(1);
        assertThat(scheduler.notifyIfChanged(task, result, afterDue.plusHours(1))).isZero();
    }

    @Test
    void failedNotification_isNotRecorded_soTheNextRunRetries() {
        when(notificationClient.createNotification(any())).thenThrow(new RuntimeException("409 Conflict"));

        assertThat(scheduler.notifyIfChanged(task, recommended(NOW.plusHours(8)), NOW)).isZero();
        assertThat(task.getLastNotifiedSlot()).isNull();
    }

    @Test
    void slotKeyRoundsDownTo3HourBlocks() {
        assertThat(WeatherTaskScheduler.slotKey(LocalDateTime.of(2026, 10, 29, 0, 0))).isEqualTo("2026-10-29T00");
        assertThat(WeatherTaskScheduler.slotKey(LocalDateTime.of(2026, 10, 29, 17, 0))).isEqualTo("2026-10-29T15");
        assertThat(WeatherTaskScheduler.slotKey(LocalDateTime.of(2026, 10, 29, 18, 0))).isEqualTo("2026-10-29T18");
    }

    private static BestTimeResult recommended(LocalDateTime start) {
        return new BestTimeResult(Status.RECOMMENDED, start, "advice");
    }
}
