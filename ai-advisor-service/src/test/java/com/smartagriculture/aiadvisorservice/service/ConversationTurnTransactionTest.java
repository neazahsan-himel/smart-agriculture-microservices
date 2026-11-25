package com.smartagriculture.aiadvisorservice.service;

import com.smartagriculture.aiadvisorservice.client.OllamaApiClient;
import com.smartagriculture.aiadvisorservice.client.speech.SpeechToTextClient;
import com.smartagriculture.aiadvisorservice.client.speech.TextToSpeechClient;
import com.smartagriculture.aiadvisorservice.dto.ConversationDto;
import com.smartagriculture.aiadvisorservice.entity.Conversation;
import com.smartagriculture.aiadvisorservice.entity.DiagnosticCase;
import com.smartagriculture.aiadvisorservice.entity.FarmTask;
import com.smartagriculture.aiadvisorservice.entity.Message;
import com.smartagriculture.aiadvisorservice.entity.WeatherTaskType;
import com.smartagriculture.aiadvisorservice.exception.ExternalServiceException;
import com.smartagriculture.aiadvisorservice.repository.ConversationRepository;
import com.smartagriculture.aiadvisorservice.repository.DiagnosticCaseRepository;
import com.smartagriculture.aiadvisorservice.repository.FarmEventRepository;
import com.smartagriculture.aiadvisorservice.repository.FarmTaskRepository;
import com.smartagriculture.aiadvisorservice.repository.MessageRepository;
import com.smartagriculture.aiadvisorservice.service.weather.WeatherTaskAdvisor;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.orm.ObjectOptimisticLockingFailureException;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.support.SimpleTransactionStatus;
import org.springframework.transaction.support.TransactionTemplate;

import java.util.List;
import java.util.Optional;
import java.util.concurrent.atomic.AtomicBoolean;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyList;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.atLeastOnce;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * Transaction boundaries of a conversation turn (DAY12-PROGRESS §7d): the LLM call must run
 * outside any transaction, the farmer's message must survive a failed turn, and an
 * optimistic-lock conflict while saving the reply must be retried, not turned into a 500.
 */
class ConversationTurnTransactionTest {

    private static final String CONVERSATION_ID = "conv-1";
    private static final String FARMER_ID = "farmer-1";

    private final AtomicBoolean inTransaction = new AtomicBoolean(false);

    private PlatformTransactionManager txManager;
    private MessageRepository messageRepository;
    private DiagnosticCaseRepository diagnosticCaseRepository;
    private FarmTaskRepository farmTaskRepository;
    private OllamaApiClient ollamaApiClient;
    private AdvisoryResponseParser responseParser;
    private ConversationalAdvisorServiceImpl service;

    @BeforeEach
    void setUp() {
        txManager = mock(PlatformTransactionManager.class);
        when(txManager.getTransaction(any())).thenAnswer(inv -> {
            inTransaction.set(true);
            return new SimpleTransactionStatus();
        });
        org.mockito.Mockito.doAnswer(inv -> { inTransaction.set(false); return null; }).when(txManager).commit(any());
        org.mockito.Mockito.doAnswer(inv -> { inTransaction.set(false); return null; }).when(txManager).rollback(any());

        ConversationRepository conversationRepository = mock(ConversationRepository.class);
        messageRepository = mock(MessageRepository.class);
        diagnosticCaseRepository = mock(DiagnosticCaseRepository.class);
        FarmEventRepository farmEventRepository = mock(FarmEventRepository.class);
        farmTaskRepository = mock(FarmTaskRepository.class);
        ExternalContextFetcher contextFetcher = mock(ExternalContextFetcher.class);
        ollamaApiClient = mock(OllamaApiClient.class);
        responseParser = mock(AdvisoryResponseParser.class);
        WeatherTaskAdvisor weatherTaskAdvisor = mock(WeatherTaskAdvisor.class);

        Conversation conversation = Conversation.builder()
                .id(CONVERSATION_ID).farmerId(FARMER_ID)
                .status(Conversation.ConversationStatus.OPEN)
                .build();
        when(conversationRepository.findByIdAndDeletedFalse(CONVERSATION_ID)).thenReturn(Optional.of(conversation));
        when(contextFetcher.fetchCrops()).thenReturn(List.of());
        when(contextFetcher.fetchForecast(any(), any(Integer.class))).thenReturn(List.of());
        when(contextFetcher.fetchWeather()).thenReturn(List.of());
        when(weatherTaskAdvisor.forecastFor(FARMER_ID)).thenReturn(List.of());
        when(diagnosticCaseRepository.findByConversationIdAndStatusInOrderByCreatedAtDesc(eq(CONVERSATION_ID), anyList()))
                .thenReturn(List.of());
        when(diagnosticCaseRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));
        when(farmTaskRepository.findFirstByFarmerIdAndFarmAssetIdAndTaskTypeAndStatusAndDeletedFalse(
                any(), any(), anyString(), any())).thenReturn(Optional.empty());

        service = new ConversationalAdvisorServiceImpl(conversationRepository, messageRepository,
                diagnosticCaseRepository, farmEventRepository, farmTaskRepository, contextFetcher, ollamaApiClient,
                responseParser, mock(SpeechToTextClient.class), mock(TextToSpeechClient.class), weatherTaskAdvisor,
                new TransactionTemplate(txManager));
    }

    @Test
    void llmCall_runsOutsideAnyTransaction() {
        AtomicBoolean txOpenDuringLlmCall = new AtomicBoolean(true);
        when(ollamaApiClient.getAdvice(anyString(), anyString(), anyList())).thenAnswer(inv -> {
            txOpenDuringLlmCall.set(inTransaction.get());
            return "raw";
        });
        when(responseParser.parse("raw")).thenReturn(advice(null));

        service.continueConversation(CONVERSATION_ID, request("Leaves are yellow"));

        assertThat(txOpenDuringLlmCall).isFalse();
        verify(txManager, times(1)).commit(any());
        assertThat(savedSenders()).containsExactly(Message.Sender.FARMER, Message.Sender.AGENT);
    }

    @Test
    void optimisticLockConflict_whileSavingReply_isRetried() {
        when(ollamaApiClient.getAdvice(anyString(), anyString(), anyList())).thenReturn("raw");
        when(responseParser.parse("raw")).thenReturn(advice(new AdvisoryResponseParser.SuggestedTask(WeatherTaskType.SPRAY, 1)));
        // First attempt: a scheduler bumped the task's @Version; second attempt succeeds
        when(farmTaskRepository.save(any(FarmTask.class)))
                .thenThrow(new ObjectOptimisticLockingFailureException(FarmTask.class, "task-1"))
                .thenAnswer(inv -> inv.getArgument(0));

        service.continueConversation(CONVERSATION_ID, request("When should I spray?"));

        verify(txManager, times(1)).rollback(any());
        verify(txManager, times(1)).commit(any());
        verify(ollamaApiClient, times(1)).getAdvice(anyString(), anyString(), anyList());
        assertThat(savedSenders()).containsExactly(Message.Sender.FARMER, Message.Sender.AGENT);
    }

    @Test
    void persistentConflict_givesUpAfterMaxAttempts() {
        when(ollamaApiClient.getAdvice(anyString(), anyString(), anyList())).thenReturn("raw");
        when(responseParser.parse("raw")).thenReturn(advice(new AdvisoryResponseParser.SuggestedTask(WeatherTaskType.SPRAY, 1)));
        when(farmTaskRepository.save(any(FarmTask.class)))
                .thenThrow(new ObjectOptimisticLockingFailureException(FarmTask.class, "task-1"));

        assertThatThrownBy(() -> service.continueConversation(CONVERSATION_ID, request("When should I spray?")))
                .isInstanceOf(ObjectOptimisticLockingFailureException.class);

        verify(txManager, times(ConversationalAdvisorServiceImpl.MAX_PERSIST_ATTEMPTS)).rollback(any());
        // The farmer's message was saved before the LLM call and is not rolled back
        assertThat(savedSenders()).containsExactly(Message.Sender.FARMER);
    }

    @Test
    void llmFailure_keepsTheFarmersMessage() {
        when(ollamaApiClient.getAdvice(anyString(), anyString(), anyList()))
                .thenThrow(new ExternalServiceException("Ollama timed out"));

        assertThatThrownBy(() -> service.continueConversation(CONVERSATION_ID, request("Leaves are yellow")))
                .isInstanceOf(ExternalServiceException.class);

        assertThat(savedSenders()).containsExactly(Message.Sender.FARMER);
        verify(txManager, never()).getTransaction(any());
        verify(diagnosticCaseRepository, never()).save(any());
    }

    private List<Message.Sender> savedSenders() {
        ArgumentCaptor<Message> saved = ArgumentCaptor.forClass(Message.class);
        verify(messageRepository, atLeastOnce()).save(saved.capture());
        return saved.getAllValues().stream().map(Message::getSender).toList();
    }

    private static ConversationDto.MessageRequest request(String text) {
        return ConversationDto.MessageRequest.builder().message(text).build();
    }

    private static AdvisoryResponseParser.ParsedAdvice advice(AdvisoryResponseParser.SuggestedTask task) {
        return new AdvisoryResponseParser.ParsedAdvice("Check the leaves.", DiagnosticCase.ConfidenceLevel.MEDIUM,
                DiagnosticCase.RiskLevel.LOW, null, false, task);
    }
}
