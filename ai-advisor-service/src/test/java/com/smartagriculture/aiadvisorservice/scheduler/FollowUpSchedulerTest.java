package com.smartagriculture.aiadvisorservice.scheduler;

import com.smartagriculture.aiadvisorservice.client.NotificationServiceClient;
import com.smartagriculture.aiadvisorservice.dto.external.NotificationRequest;
import com.smartagriculture.aiadvisorservice.entity.DiagnosticCase;
import com.smartagriculture.aiadvisorservice.entity.FarmTask;
import com.smartagriculture.aiadvisorservice.repository.DiagnosticCaseRepository;
import com.smartagriculture.aiadvisorservice.repository.FarmTaskRepository;
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

/** Reminder de-duplication and success counting of {@link FollowUpScheduler}. */
class FollowUpSchedulerTest {

    private static final LocalDateTime DUE = LocalDateTime.of(2026, 10, 29, 8, 0);

    private NotificationServiceClient notificationClient;
    private FarmTaskRepository farmTaskRepository;
    private DiagnosticCaseRepository diagnosticCaseRepository;
    private FollowUpScheduler scheduler;
    private FarmTask task;

    @BeforeEach
    void setUp() {
        notificationClient = mock(NotificationServiceClient.class);
        farmTaskRepository = mock(FarmTaskRepository.class);
        diagnosticCaseRepository = mock(DiagnosticCaseRepository.class);
        scheduler = new FollowUpScheduler(diagnosticCaseRepository, farmTaskRepository, notificationClient);
        task = FarmTask.builder()
                .id("t1").farmerId("f1").taskType("FOLLOW_UP").dueDate(DUE)
                .build();
    }

    @Test
    void taskReminder_isSentOncePerDueDate() {
        assertThat(scheduler.sendTaskReminder(task)).isTrue();
        assertThat(scheduler.sendTaskReminder(task)).isFalse();

        ArgumentCaptor<NotificationRequest> sent = ArgumentCaptor.forClass(NotificationRequest.class);
        verify(notificationClient, times(1)).createNotification(sent.capture());
        assertThat(sent.getValue().getTitle()).isEqualTo("Reminder: FOLLOW_UP (due Thu 29 Oct 08:00)");
        assertThat(task.getReminderSentFor()).isEqualTo(DUE);
        verify(farmTaskRepository, times(1)).save(task);
    }

    @Test
    void rescheduledTask_getsANewReminder() {
        scheduler.sendTaskReminder(task);
        task.setDueDate(DUE.plusDays(3));

        assertThat(scheduler.sendTaskReminder(task)).isTrue();
        verify(notificationClient, times(2)).createNotification(any());
        assertThat(task.getReminderSentFor()).isEqualTo(DUE.plusDays(3));
    }

    @Test
    void failedReminder_isNotRecorded_soTheNextRunRetries() {
        when(notificationClient.createNotification(any())).thenThrow(new RuntimeException("409 Conflict"));

        assertThat(scheduler.sendTaskReminder(task)).isFalse();
        assertThat(task.getReminderSentFor()).isNull();
        verify(farmTaskRepository, never()).save(any());
    }

    @Test
    void caseFollowUp_titleCarriesTheDueDate_andFailureLeavesTheCaseAwaiting() {
        DiagnosticCase diagnosticCase = DiagnosticCase.builder()
                .id("c1").farmerId("f1").symptom("yellow leaves")
                .status(DiagnosticCase.CaseStatus.AWAITING_FOLLOWUP).followUpDueAt(DUE)
                .build();

        assertThat(scheduler.sendCaseFollowUp(diagnosticCase)).isTrue();
        ArgumentCaptor<NotificationRequest> sent = ArgumentCaptor.forClass(NotificationRequest.class);
        verify(notificationClient).createNotification(sent.capture());
        assertThat(sent.getValue().getTitle()).isEqualTo("Follow-up: how is your crop doing? (Thu 29 Oct 08:00)");
        assertThat(diagnosticCase.getStatus()).isEqualTo(DiagnosticCase.CaseStatus.FOLLOWUP_SENT);

        diagnosticCase.setStatus(DiagnosticCase.CaseStatus.AWAITING_FOLLOWUP);
        when(notificationClient.createNotification(any())).thenThrow(new RuntimeException("down"));
        assertThat(scheduler.sendCaseFollowUp(diagnosticCase)).isFalse();
        assertThat(diagnosticCase.getStatus()).isEqualTo(DiagnosticCase.CaseStatus.AWAITING_FOLLOWUP);
    }
}
