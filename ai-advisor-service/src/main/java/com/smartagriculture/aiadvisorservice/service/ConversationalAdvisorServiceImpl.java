package com.smartagriculture.aiadvisorservice.service;

import com.smartagriculture.aiadvisorservice.client.OllamaApiClient;
import com.smartagriculture.aiadvisorservice.client.speech.SpeechToTextClient;
import com.smartagriculture.aiadvisorservice.client.speech.TextToSpeechClient;
import com.smartagriculture.aiadvisorservice.exception.ExternalServiceException;
import com.smartagriculture.aiadvisorservice.dto.ConversationDto;
import com.smartagriculture.aiadvisorservice.dto.DiagnosticCaseDto;
import com.smartagriculture.aiadvisorservice.dto.FarmEventDto;
import com.smartagriculture.aiadvisorservice.dto.FarmTaskDto;
import com.smartagriculture.aiadvisorservice.dto.external.CropSummary;
import com.smartagriculture.aiadvisorservice.dto.external.FarmerResponse;
import com.smartagriculture.aiadvisorservice.dto.external.WeatherSummary;
import com.smartagriculture.aiadvisorservice.entity.Conversation;
import com.smartagriculture.aiadvisorservice.entity.DiagnosticCase;
import com.smartagriculture.aiadvisorservice.entity.FarmEvent;
import com.smartagriculture.aiadvisorservice.entity.FarmTask;
import com.smartagriculture.aiadvisorservice.entity.Message;
import com.smartagriculture.aiadvisorservice.entity.WeatherTaskType;
import com.smartagriculture.aiadvisorservice.exception.DuplicateResourceException;
import com.smartagriculture.aiadvisorservice.exception.ResourceNotFoundException;
import com.smartagriculture.aiadvisorservice.repository.ConversationRepository;
import com.smartagriculture.aiadvisorservice.repository.DiagnosticCaseRepository;
import com.smartagriculture.aiadvisorservice.repository.FarmEventRepository;
import com.smartagriculture.aiadvisorservice.repository.FarmTaskRepository;
import com.smartagriculture.aiadvisorservice.repository.MessageRepository;
import com.smartagriculture.aiadvisorservice.service.weather.ForecastFormatter;
import com.smartagriculture.aiadvisorservice.service.weather.WeatherTaskAdvisor;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.dao.OptimisticLockingFailureException;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.transaction.support.TransactionTemplate;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

@Service
@RequiredArgsConstructor
@Slf4j
public class ConversationalAdvisorServiceImpl implements ConversationalAdvisorService {

    private static final List<DiagnosticCase.CaseStatus> OPEN_STATUSES =
            List.of(DiagnosticCase.CaseStatus.OPEN, DiagnosticCase.CaseStatus.AWAITING_FOLLOWUP);

    private static final String FOLLOW_UP_TASK_TYPE = "FOLLOW_UP";
    private static final int PROMPT_FORECAST_HOURS = 48;
    // A scheduler can still bump a task's @Version during the short persist transaction; retry with fresh reads
    static final int MAX_PERSIST_ATTEMPTS = 3;

    private final ConversationRepository conversationRepository;
    private final MessageRepository messageRepository;
    private final DiagnosticCaseRepository diagnosticCaseRepository;
    private final FarmEventRepository farmEventRepository;
    private final FarmTaskRepository farmTaskRepository;

    private final ExternalContextFetcher contextFetcher;
    private final OllamaApiClient ollamaApiClient;
    private final AdvisoryResponseParser responseParser;
    private final SpeechToTextClient speechToTextClient;
    private final TextToSpeechClient textToSpeechClient;
    private final WeatherTaskAdvisor weatherTaskAdvisor;
    private final TransactionTemplate transactionTemplate;

    // Not @Transactional: a turn includes a multi-minute LLM call, see processTurn
    @Override
    public ConversationDto.Response startConversation(ConversationDto.StartRequest request) {
        log.info("Starting conversation for farmerId={}, queryType={}", request.getFarmerId(), request.getQueryType());

        Conversation conversation = conversationRepository.saveAndFlush(Conversation.builder()
                .farmerId(request.getFarmerId())
                .farmAssetId(request.getFarmAssetId())
                .topic(request.getQueryType())
                .status(Conversation.ConversationStatus.OPEN)
                .build());

        Optional<String> replyAudio = processTurn(conversation, request.getMessage(),
                request.getImageBase64(), request.getAudioBase64());
        return buildResponse(conversation, replyAudio.orElse(null));
    }

    // Not @Transactional: a turn includes a multi-minute LLM call, see processTurn
    @Override
    public ConversationDto.Response continueConversation(String conversationId, ConversationDto.MessageRequest request) {
        Conversation conversation = conversationRepository.findByIdAndDeletedFalse(conversationId)
                .orElseThrow(() -> new ResourceNotFoundException("Conversation not found with id: " + conversationId));

        Optional<String> replyAudio = processTurn(conversation, request.getMessage(),
                request.getImageBase64(), request.getAudioBase64());
        return buildResponse(conversation, replyAudio.orElse(null));
    }

    @Override
    public ConversationDto.Response getConversation(String conversationId) {
        Conversation conversation = conversationRepository.findByIdAndDeletedFalse(conversationId)
                .orElseThrow(() -> new ResourceNotFoundException("Conversation not found with id: " + conversationId));
        return buildResponse(conversation, null);
    }

    @Override
    public Page<ConversationDto.Response> getConversationsByFarmer(String farmerId, Pageable pageable) {
        return conversationRepository.findByFarmerIdAndDeletedFalse(farmerId, pageable)
                .map(c -> buildResponse(c, null));
    }

    @Override
    @Transactional
    public DiagnosticCaseDto.Response recordOutcome(String diagnosticCaseId, DiagnosticCase.Outcome outcome) {
        DiagnosticCase diagnosticCase = diagnosticCaseRepository.findByIdAndDeletedFalse(diagnosticCaseId)
                .orElseThrow(() -> new ResourceNotFoundException("Diagnostic case not found with id: " + diagnosticCaseId));

        diagnosticCase.setOutcome(outcome);
        diagnosticCase.setStatus(DiagnosticCase.CaseStatus.RESOLVED);
        DiagnosticCase saved = diagnosticCaseRepository.saveAndFlush(diagnosticCase);

        farmEventRepository.save(FarmEvent.builder()
                .farmerId(saved.getFarmerId())
                .farmAssetId(saved.getFarmAssetId())
                .eventType(FarmEvent.EventType.OUTCOME)
                .description("Outcome recorded: " + outcome)
                .diagnosticCaseId(saved.getId())
                .build());

        return toDiagnosticCaseResponse(saved);
    }

    @Override
    public Page<FarmEventDto.Response> getFarmEvents(String farmerId, String farmAssetId, Pageable pageable) {
        Page<FarmEvent> page = (farmAssetId != null && !farmAssetId.isBlank())
                ? farmEventRepository.findByFarmerIdAndFarmAssetId(farmerId, farmAssetId, pageable)
                : farmEventRepository.findByFarmerId(farmerId, pageable);
        return page.map(this::toFarmEventResponse);
    }

    @Override
    public Page<FarmTaskDto.Response> getFarmTasks(String farmerId, Pageable pageable) {
        return farmTaskRepository.findByFarmerIdAndDeletedFalse(farmerId, pageable).map(this::toFarmTaskResponse);
    }

    @Override
    @Transactional
    public FarmTaskDto.Response updateFarmTaskStatus(String taskId, FarmTask.TaskStatus status) {
        FarmTask task = farmTaskRepository.findByIdAndDeletedFalse(taskId)
                .orElseThrow(() -> new ResourceNotFoundException("Farm task not found with id: " + taskId));
        task.setStatus(status);
        return toFarmTaskResponse(farmTaskRepository.save(task));
    }

    @Override
    public FarmTaskDto.Response createWeatherTask(FarmTaskDto.Request request) {
        String taskType = request.getTaskType().name();
        if (farmTaskRepository.findFirstByFarmerIdAndFarmAssetIdAndTaskTypeAndStatusAndDeletedFalse(
                request.getFarmerId(), request.getFarmAssetId(), taskType, FarmTask.TaskStatus.PENDING).isPresent()) {
            throw new DuplicateResourceException("A pending " + taskType + " task already exists for farmer "
                    + request.getFarmerId() + (request.getFarmAssetId() != null ? " / asset " + request.getFarmAssetId() : ""));
        }

        FarmTask task = FarmTask.builder()
                .farmerId(request.getFarmerId())
                .farmAssetId(request.getFarmAssetId())
                .taskType(taskType)
                .dueDate(request.getDueDate())
                .priority(request.getPriority() != null ? request.getPriority() : FarmTask.TaskPriority.MEDIUM)
                .weatherDependent(true)
                .build();

        evaluateBestEffort(task);
        return toFarmTaskResponse(farmTaskRepository.saveAndFlush(task));
    }

    @Override
    public FarmTaskDto.Response refreshBestTime(String taskId) {
        FarmTask task = farmTaskRepository.findByIdAndDeletedFalse(taskId)
                .orElseThrow(() -> new ResourceNotFoundException("Farm task not found with id: " + taskId));
        if (WeatherTaskType.fromTaskType(task.getTaskType()) == null) {
            throw new IllegalArgumentException("Task " + taskId + " (" + task.getTaskType() + ") is not weather-dependent");
        }
        weatherTaskAdvisor.evaluate(task);
        return toFarmTaskResponse(farmTaskRepository.saveAndFlush(task));
    }

    // Forecast problems must never fail task creation or a conversation turn; the scheduler retries hourly
    private void evaluateBestEffort(FarmTask task) {
        try {
            weatherTaskAdvisor.evaluate(task);
        } catch (Exception e) {
            log.warn("Best-time evaluation failed for {} task of farmer {}: {}",
                    task.getTaskType(), task.getFarmerId(), e.getMessage());
        }
    }

    // ── Core turn pipeline ───────────────────────────────────────────────────
    //
    // A turn runs in three phases so no database transaction stays open during the LLM call
    // (it can take minutes on CPU). Previously the whole turn was one transaction: its
    // REPEATABLE READ snapshot went stale while Ollama ran, an hourly scheduler bumped a task's
    // @Version, the final save threw StaleObjectStateException and the rollback lost both the
    // farmer's message and the reply (DAY12-PROGRESS §7d).
    //   1. save the farmer's message (own short transaction; kept even if a later phase fails)
    //   2. gather context and call the LLM (no transaction)
    //   3. persist case / tasks / reply / event in one short transaction with fresh reads,
    //      retried on optimistic-lock conflicts

    private Optional<String> processTurn(Conversation conversation, String farmerMessage, String imageBase64,
                                          String audioBase64) {
        boolean hasImage = imageBase64 != null && !imageBase64.isBlank();
        boolean hasAudio = audioBase64 != null && !audioBase64.isBlank();

        // Phase 1
        String farmerContent = resolveFarmerContent(farmerMessage, hasImage, hasAudio, audioBase64);
        messageRepository.save(Message.builder()
                .conversationId(conversation.getId())
                .sender(Message.Sender.FARMER)
                .content(farmerContent)
                .imageBase64(hasImage ? imageBase64 : null)
                .audioBase64(hasAudio ? audioBase64 : null)
                .build());

        // Phase 2
        FarmerResponse farmer = contextFetcher.fetchFarmer(conversation.getFarmerId());
        List<CropSummary> crops = contextFetcher.fetchCrops();
        // Location-specific forecast when the farmer has coordinates; otherwise the latest records as before
        List<WeatherSummary> forecast = contextFetcher.fetchForecast(farmer, PROMPT_FORECAST_HOURS);
        List<WeatherSummary> weather = forecast.isEmpty() ? contextFetcher.fetchWeather() : List.of();
        List<FarmEvent> farmMemory = fetchFarmMemory(conversation);
        List<Message> history = messageRepository.findByConversationIdOrderByCreatedAtAsc(conversation.getId());

        String userMessage = buildUserMessage(conversation, farmer, crops, forecast, weather, farmMemory, history, hasImage);
        String rawReply = ollamaApiClient.getAdvice(PromptTemplates.CONVERSATION_SYSTEM_PROMPT, userMessage,
                hasImage ? List.of(imageBase64) : List.of());
        AdvisoryResponseParser.ParsedAdvice parsed = responseParser.parse(rawReply);

        // Fetched here so phase 3 makes no HTTP calls inside its transaction (never throws; empty when unavailable)
        List<WeatherSummary> taskForecast = parsed.suggestedTask() != null
                ? weatherTaskAdvisor.forecastFor(conversation.getFarmerId())
                : List.of();

        // Phase 3
        String agentText = persistReplyWithRetry(conversation, farmerContent, hasImage, parsed, taskForecast);

        return hasAudio ? textToSpeechClient.synthesize(agentText) : Optional.empty();
    }

    private String resolveFarmerContent(String farmerMessage, boolean hasImage, boolean hasAudio, String audioBase64) {
        String effectiveMessage = farmerMessage;
        if (hasAudio) {
            boolean hasTypedFallback = effectiveMessage != null && !effectiveMessage.isBlank();
            try {
                String transcribed = speechToTextClient.transcribe(audioBase64);
                if (!hasTypedFallback) {
                    effectiveMessage = transcribed;
                }
            } catch (ExternalServiceException e) {
                if (!hasTypedFallback) {
                    throw e;
                }
                log.warn("Speech-to-text failed, falling back to the farmer's typed message: {}", e.getMessage());
            }
        }

        return (effectiveMessage == null || effectiveMessage.isBlank())
                ? (hasImage ? "(photo attached)" : effectiveMessage)
                : effectiveMessage;
    }

    private String persistReplyWithRetry(Conversation conversation, String farmerContent, boolean hasImage,
                                         AdvisoryResponseParser.ParsedAdvice parsed, List<WeatherSummary> taskForecast) {
        for (int attempt = 1; ; attempt++) {
            try {
                return transactionTemplate.execute(status ->
                        persistReply(conversation, farmerContent, hasImage, parsed, taskForecast));
            } catch (OptimisticLockingFailureException e) {
                if (attempt >= MAX_PERSIST_ATTEMPTS) {
                    throw e;
                }
                log.warn("Concurrent update while saving the reply for conversation {} (attempt {}/{}), retrying: {}",
                        conversation.getId(), attempt, MAX_PERSIST_ATTEMPTS, e.getMessage());
            }
        }
    }

    private String persistReply(Conversation conversation, String farmerContent, boolean hasImage,
                                AdvisoryResponseParser.ParsedAdvice parsed, List<WeatherSummary> taskForecast) {
        DiagnosticCase diagnosticCase = upsertDiagnosticCase(conversation, farmerContent, hasImage, parsed);

        // The rule-based best time is appended to the reply so the chat always agrees with the
        // notifications the scheduler sends later — the model's own timing can be off (observed:
        // "spray at 00-03" because those blocks were dry).
        String agentText = parsed.visibleText();
        if (parsed.suggestedTask() != null) {
            FarmTask task = upsertSuggestedWeatherTask(diagnosticCase, parsed.suggestedTask(), taskForecast);
            if (task.getWeatherAdvice() != null) {
                agentText = agentText + "\n\nForecast check: " + task.getWeatherAdvice();
            }
        }

        messageRepository.save(Message.builder()
                .conversationId(conversation.getId())
                .sender(Message.Sender.AGENT)
                .content(agentText)
                .build());

        farmEventRepository.save(FarmEvent.builder()
                .farmerId(conversation.getFarmerId())
                .farmAssetId(conversation.getFarmAssetId())
                .eventType(FarmEvent.EventType.ADVICE)
                .description(truncate(agentText, 1500))
                .diagnosticCaseId(diagnosticCase.getId())
                .build());

        return agentText;
    }

    private List<FarmEvent> fetchFarmMemory(Conversation conversation) {
        if (conversation.getFarmAssetId() != null && !conversation.getFarmAssetId().isBlank()) {
            return farmEventRepository.findTop5ByFarmerIdAndFarmAssetIdOrderByOccurredAtDesc(
                    conversation.getFarmerId(), conversation.getFarmAssetId());
        }
        return farmEventRepository.findTop5ByFarmerIdOrderByOccurredAtDesc(conversation.getFarmerId());
    }

    private DiagnosticCase upsertDiagnosticCase(Conversation conversation, String latestFarmerMessage, boolean hasImage,
                                                 AdvisoryResponseParser.ParsedAdvice parsed) {
        List<DiagnosticCase> open = diagnosticCaseRepository
                .findByConversationIdAndStatusInOrderByCreatedAtDesc(conversation.getId(), OPEN_STATUSES);

        String symptom = hasImage ? "[with photo] " + latestFarmerMessage : latestFarmerMessage;

        DiagnosticCase diagnosticCase = open.isEmpty()
                ? DiagnosticCase.builder()
                    .conversationId(conversation.getId())
                    .farmerId(conversation.getFarmerId())
                    .farmAssetId(conversation.getFarmAssetId())
                    .symptom(truncate(symptom, 1000))
                    .status(DiagnosticCase.CaseStatus.OPEN)
                    .build()
                : open.get(0);

        diagnosticCase.setHypothesis(truncate(parsed.visibleText(), 1000));
        diagnosticCase.setRecommendedAction(truncate(parsed.visibleText(), 1500));
        diagnosticCase.setConfidenceLevel(parsed.confidence());
        diagnosticCase.setRiskLevel(parsed.risk());

        if (parsed.escalate()) {
            diagnosticCase.setStatus(DiagnosticCase.CaseStatus.ESCALATED);
        } else if (parsed.followUpDays() != null) {
            diagnosticCase.setStatus(DiagnosticCase.CaseStatus.AWAITING_FOLLOWUP);
            diagnosticCase.setFollowUpDueAt(LocalDateTime.now().plusDays(parsed.followUpDays()));
        }

        DiagnosticCase saved = diagnosticCaseRepository.save(diagnosticCase);

        if (saved.getStatus() == DiagnosticCase.CaseStatus.AWAITING_FOLLOWUP) {
            upsertFollowUpTask(saved);
        }

        return saved;
    }

    private void upsertFollowUpTask(DiagnosticCase diagnosticCase) {
        FarmTask task = farmTaskRepository
                .findBySourceDiagnosticCaseIdAndTaskTypeAndDeletedFalse(diagnosticCase.getId(), FOLLOW_UP_TASK_TYPE)
                .orElseGet(() -> FarmTask.builder()
                        .farmerId(diagnosticCase.getFarmerId())
                        .farmAssetId(diagnosticCase.getFarmAssetId())
                        .taskType(FOLLOW_UP_TASK_TYPE)
                        .sourceDiagnosticCaseId(diagnosticCase.getId())
                        .build());

        task.setDueDate(diagnosticCase.getFollowUpDueAt());
        task.setPriority(mapRiskToPriority(diagnosticCase.getRiskLevel()));
        task.setWeatherDependent(false);
        task.setStatus(FarmTask.TaskStatus.PENDING);
        farmTaskRepository.save(task);
    }

    // Upsert, not 409: the model may repeat the same TASK tag on several turns of one conversation,
    // which should just move the planned date of the existing pending task.
    private FarmTask upsertSuggestedWeatherTask(DiagnosticCase diagnosticCase, AdvisoryResponseParser.SuggestedTask suggested,
                                                List<WeatherSummary> forecast) {
        String taskType = suggested.type().name();
        FarmTask task = farmTaskRepository.findFirstByFarmerIdAndFarmAssetIdAndTaskTypeAndStatusAndDeletedFalse(
                        diagnosticCase.getFarmerId(), diagnosticCase.getFarmAssetId(), taskType, FarmTask.TaskStatus.PENDING)
                .orElseGet(() -> FarmTask.builder()
                        .farmerId(diagnosticCase.getFarmerId())
                        .farmAssetId(diagnosticCase.getFarmAssetId())
                        .taskType(taskType)
                        .weatherDependent(true)
                        .build());

        task.setDueDate(LocalDateTime.now().plusDays(suggested.daysFromNow()));
        task.setPriority(mapRiskToPriority(diagnosticCase.getRiskLevel()));
        task.setSourceDiagnosticCaseId(diagnosticCase.getId());
        // Uses the forecast fetched before the transaction, so no HTTP call happens while it is open
        try {
            weatherTaskAdvisor.evaluate(task, forecast, LocalDateTime.now());
        } catch (Exception e) {
            log.warn("Best-time evaluation failed for {} task of farmer {}: {}",
                    taskType, diagnosticCase.getFarmerId(), e.getMessage());
        }
        boolean isNew = task.getId() == null;
        FarmTask saved = farmTaskRepository.save(task);
        log.info("Advisor suggested {} in {} day(s) for farmer {} — task {}", taskType, suggested.daysFromNow(),
                diagnosticCase.getFarmerId(), isNew ? "created" : "updated");
        return saved;
    }

    private FarmTask.TaskPriority mapRiskToPriority(DiagnosticCase.RiskLevel risk) {
        if (risk == null) return FarmTask.TaskPriority.MEDIUM;
        return switch (risk) {
            case LOW -> FarmTask.TaskPriority.LOW;
            case MEDIUM -> FarmTask.TaskPriority.MEDIUM;
            case HIGH -> FarmTask.TaskPriority.HIGH;
            case CRITICAL -> FarmTask.TaskPriority.CRITICAL;
        };
    }

    // ── Prompt Builder ───────────────────────────────────────────────────────

    private String buildUserMessage(Conversation conversation, FarmerResponse farmer, List<CropSummary> crops,
                                     List<WeatherSummary> forecast, List<WeatherSummary> weather,
                                     List<FarmEvent> farmMemory, List<Message> history, boolean hasImage) {
        StringBuilder sb = new StringBuilder();

        sb.append("Query Type: ").append(conversation.getTopic()).append("\n\n");

        sb.append("== FARMER PROFILE ==\n");
        if (farmer != null) {
            sb.append("Name: ").append(farmer.getName()).append("\n");
            sb.append("Location: ").append(farmer.getRegion()).append(", ").append(farmer.getCountryCode()).append("\n");
            if (farmer.getSoilType() != null)
                sb.append("Soil Type: ").append(farmer.getSoilType()).append("\n");
            if (farmer.getFarmSizeHectares() != null)
                sb.append("Farm Size: ").append(farmer.getFarmSizeHectares()).append(" hectares\n");
        } else {
            sb.append("(No farmer profile linked)\n");
        }

        sb.append("\n== AVAILABLE CROPS IN CATALOG ==\n");
        if (crops.isEmpty()) {
            sb.append("No crop data available.\n");
        } else {
            crops.stream().limit(15).forEach(crop -> {
                sb.append("- ").append(crop.getName());
                if (crop.getVariety() != null) sb.append(" (").append(crop.getVariety()).append(")");
                sb.append(" | Type: ").append(crop.getCropType());
                sb.append(" | Season: ").append(crop.getSeason());
                sb.append("\n");
            });
        }

        if (!forecast.isEmpty()) {
            sb.append("\n== WEATHER FORECAST FOR THE FARMER'S LOCATION (next ")
                    .append(PROMPT_FORECAST_HOURS).append("h, 3-hour blocks, local time) ==\n");
            ForecastFormatter.toThreeHourBlocks(forecast).forEach(line -> sb.append(line).append("\n"));
        } else if (weather.isEmpty()) {
            sb.append("\n== RECENT WEATHER CONDITIONS ==\n");
            sb.append("No weather data available.\n");
        } else {
            sb.append("\n== RECENT WEATHER CONDITIONS ==\n");
            weather.forEach(w -> {
                sb.append("- ").append(w.getRecordType()).append(" at ").append(w.getRecordedAt());
                sb.append(" | ").append(w.getWeatherCondition());
                if (w.getTemperatureCelsius() != null)
                    sb.append(" | Temp: ").append(w.getTemperatureCelsius()).append("°C");
                if (w.getRainfallMm() != null)
                    sb.append(" | Rainfall: ").append(w.getRainfallMm()).append("mm");
                sb.append("\n");
            });
        }

        sb.append("\n== FARM MEMORY (past events for this farmer/asset) ==\n");
        if (farmMemory.isEmpty()) {
            sb.append("No prior recorded events.\n");
        } else {
            farmMemory.forEach(e -> sb.append("- [").append(e.getOccurredAt()).append("] ")
                    .append(e.getEventType()).append(": ").append(e.getDescription()).append("\n"));
        }

        sb.append("\n== CONVERSATION SO FAR ==\n");
        history.forEach(m -> sb.append(m.getSender() == Message.Sender.FARMER ? "Farmer: " : "Agent: ")
                .append(m.getContent()).append("\n"));

        if (hasImage) {
            sb.append(PromptTemplates.IMAGE_ANALYSIS_INSTRUCTION);
        }

        return sb.toString();
    }

    // ── Mappers ──────────────────────────────────────────────────────────────

    private ConversationDto.Response buildResponse(Conversation conversation, String replyAudioBase64) {
        List<ConversationDto.MessageView> messages = messageRepository
                .findByConversationIdOrderByCreatedAtAsc(conversation.getId())
                .stream()
                .map(m -> ConversationDto.MessageView.builder()
                        .id(m.getId())
                        .sender(m.getSender())
                        .content(m.getContent())
                        .hasImage(m.getImageBase64() != null)
                        .hasAudio(m.getAudioBase64() != null)
                        .createdAt(m.getCreatedAt())
                        .build())
                .toList();

        ConversationDto.DiagnosticCaseView latestCase = diagnosticCaseRepository
                .findFirstByConversationIdOrderByCreatedAtDesc(conversation.getId())
                .map(this::toCaseView)
                .orElse(null);

        return ConversationDto.Response.builder()
                .id(conversation.getId())
                .farmerId(conversation.getFarmerId())
                .farmAssetId(conversation.getFarmAssetId())
                .topic(conversation.getTopic())
                .status(conversation.getStatus())
                .messages(messages)
                .latestCase(latestCase)
                .replyAudioBase64(replyAudioBase64)
                .createdAt(conversation.getCreatedAt())
                .updatedAt(conversation.getUpdatedAt())
                .build();
    }

    private ConversationDto.DiagnosticCaseView toCaseView(DiagnosticCase c) {
        return ConversationDto.DiagnosticCaseView.builder()
                .id(c.getId())
                .confidenceLevel(c.getConfidenceLevel())
                .riskLevel(c.getRiskLevel())
                .status(c.getStatus())
                .recommendedAction(c.getRecommendedAction())
                .followUpDueAt(c.getFollowUpDueAt())
                .outcome(c.getOutcome())
                .build();
    }

    private DiagnosticCaseDto.Response toDiagnosticCaseResponse(DiagnosticCase c) {
        return DiagnosticCaseDto.Response.builder()
                .id(c.getId())
                .conversationId(c.getConversationId())
                .farmerId(c.getFarmerId())
                .farmAssetId(c.getFarmAssetId())
                .symptom(c.getSymptom())
                .hypothesis(c.getHypothesis())
                .confidenceLevel(c.getConfidenceLevel())
                .riskLevel(c.getRiskLevel())
                .status(c.getStatus())
                .recommendedAction(c.getRecommendedAction())
                .followUpDueAt(c.getFollowUpDueAt())
                .outcome(c.getOutcome())
                .createdAt(c.getCreatedAt())
                .updatedAt(c.getUpdatedAt())
                .build();
    }

    private FarmEventDto.Response toFarmEventResponse(FarmEvent e) {
        return FarmEventDto.Response.builder()
                .id(e.getId())
                .farmerId(e.getFarmerId())
                .farmAssetId(e.getFarmAssetId())
                .eventType(e.getEventType())
                .description(e.getDescription())
                .diagnosticCaseId(e.getDiagnosticCaseId())
                .occurredAt(e.getOccurredAt())
                .build();
    }

    private FarmTaskDto.Response toFarmTaskResponse(FarmTask t) {
        return FarmTaskDto.Response.builder()
                .id(t.getId())
                .farmerId(t.getFarmerId())
                .farmAssetId(t.getFarmAssetId())
                .taskType(t.getTaskType())
                .dueDate(t.getDueDate())
                .priority(t.getPriority())
                .weatherDependent(t.getWeatherDependent())
                .status(t.getStatus())
                .sourceDiagnosticCaseId(t.getSourceDiagnosticCaseId())
                .recommendedStartAt(t.getRecommendedStartAt())
                .weatherAdvice(t.getWeatherAdvice())
                .weatherCheckedAt(t.getWeatherCheckedAt())
                .build();
    }

    private String truncate(String text, int maxLen) {
        if (text == null) return null;
        return text.length() <= maxLen ? text : text.substring(0, maxLen);
    }
}
