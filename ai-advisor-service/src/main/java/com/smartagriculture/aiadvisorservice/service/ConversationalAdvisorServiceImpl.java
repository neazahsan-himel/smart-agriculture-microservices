package com.smartagriculture.aiadvisorservice.service;

import com.smartagriculture.aiadvisorservice.client.OllamaApiClient;
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
import com.smartagriculture.aiadvisorservice.exception.ResourceNotFoundException;
import com.smartagriculture.aiadvisorservice.repository.ConversationRepository;
import com.smartagriculture.aiadvisorservice.repository.DiagnosticCaseRepository;
import com.smartagriculture.aiadvisorservice.repository.FarmEventRepository;
import com.smartagriculture.aiadvisorservice.repository.FarmTaskRepository;
import com.smartagriculture.aiadvisorservice.repository.MessageRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

@Service
@RequiredArgsConstructor
@Slf4j
public class ConversationalAdvisorServiceImpl implements ConversationalAdvisorService {

    private static final List<DiagnosticCase.CaseStatus> OPEN_STATUSES =
            List.of(DiagnosticCase.CaseStatus.OPEN, DiagnosticCase.CaseStatus.AWAITING_FOLLOWUP);

    private final ConversationRepository conversationRepository;
    private final MessageRepository messageRepository;
    private final DiagnosticCaseRepository diagnosticCaseRepository;
    private final FarmEventRepository farmEventRepository;
    private final FarmTaskRepository farmTaskRepository;

    private final ExternalContextFetcher contextFetcher;
    private final OllamaApiClient ollamaApiClient;
    private final AdvisoryResponseParser responseParser;

    @Override
    @Transactional
    public ConversationDto.Response startConversation(ConversationDto.StartRequest request) {
        log.info("Starting conversation for farmerId={}, queryType={}", request.getFarmerId(), request.getQueryType());

        Conversation conversation = conversationRepository.save(Conversation.builder()
                .farmerId(request.getFarmerId())
                .farmAssetId(request.getFarmAssetId())
                .topic(request.getQueryType())
                .status(Conversation.ConversationStatus.OPEN)
                .build());

        processTurn(conversation, request.getMessage(), request.getImageBase64());
        return buildResponse(conversation);
    }

    @Override
    @Transactional
    public ConversationDto.Response continueConversation(String conversationId, ConversationDto.MessageRequest request) {
        Conversation conversation = conversationRepository.findByIdAndDeletedFalse(conversationId)
                .orElseThrow(() -> new ResourceNotFoundException("Conversation not found with id: " + conversationId));

        processTurn(conversation, request.getMessage(), request.getImageBase64());
        return buildResponse(conversation);
    }

    @Override
    public ConversationDto.Response getConversation(String conversationId) {
        Conversation conversation = conversationRepository.findByIdAndDeletedFalse(conversationId)
                .orElseThrow(() -> new ResourceNotFoundException("Conversation not found with id: " + conversationId));
        return buildResponse(conversation);
    }

    @Override
    public Page<ConversationDto.Response> getConversationsByFarmer(String farmerId, Pageable pageable) {
        return conversationRepository.findByFarmerIdAndDeletedFalse(farmerId, pageable)
                .map(this::buildResponse);
    }

    @Override
    @Transactional
    public DiagnosticCaseDto.Response recordOutcome(String diagnosticCaseId, DiagnosticCase.Outcome outcome) {
        DiagnosticCase diagnosticCase = diagnosticCaseRepository.findByIdAndDeletedFalse(diagnosticCaseId)
                .orElseThrow(() -> new ResourceNotFoundException("Diagnostic case not found with id: " + diagnosticCaseId));

        diagnosticCase.setOutcome(outcome);
        diagnosticCase.setStatus(DiagnosticCase.CaseStatus.RESOLVED);
        DiagnosticCase saved = diagnosticCaseRepository.save(diagnosticCase);

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

    // ── Core turn pipeline ───────────────────────────────────────────────────

    private void processTurn(Conversation conversation, String farmerMessage, String imageBase64) {
        boolean hasImage = imageBase64 != null && !imageBase64.isBlank();
        String farmerContent = (farmerMessage == null || farmerMessage.isBlank())
                ? (hasImage ? "(photo attached)" : farmerMessage)
                : farmerMessage;

        messageRepository.save(Message.builder()
                .conversationId(conversation.getId())
                .sender(Message.Sender.FARMER)
                .content(farmerContent)
                .imageBase64(hasImage ? imageBase64 : null)
                .build());

        FarmerResponse farmer = contextFetcher.fetchFarmer(conversation.getFarmerId());
        List<CropSummary> crops = contextFetcher.fetchCrops();
        List<WeatherSummary> weather = contextFetcher.fetchWeather();
        List<FarmEvent> farmMemory = fetchFarmMemory(conversation);
        List<Message> history = messageRepository.findByConversationIdOrderByCreatedAtAsc(conversation.getId());

        String userMessage = buildUserMessage(conversation, farmer, crops, weather, farmMemory, history, hasImage);
        String rawReply = ollamaApiClient.getAdvice(PromptTemplates.CONVERSATION_SYSTEM_PROMPT, userMessage,
                hasImage ? List.of(imageBase64) : List.of());
        AdvisoryResponseParser.ParsedAdvice parsed = responseParser.parse(rawReply);

        messageRepository.save(Message.builder()
                .conversationId(conversation.getId())
                .sender(Message.Sender.AGENT)
                .content(parsed.visibleText())
                .build());

        DiagnosticCase diagnosticCase = upsertDiagnosticCase(conversation, farmerContent, hasImage, parsed);

        farmEventRepository.save(FarmEvent.builder()
                .farmerId(conversation.getFarmerId())
                .farmAssetId(conversation.getFarmAssetId())
                .eventType(FarmEvent.EventType.ADVICE)
                .description(truncate(parsed.visibleText(), 1500))
                .diagnosticCaseId(diagnosticCase.getId())
                .build());
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
        FarmTask task = farmTaskRepository.findBySourceDiagnosticCaseIdAndDeletedFalse(diagnosticCase.getId())
                .orElseGet(() -> FarmTask.builder()
                        .farmerId(diagnosticCase.getFarmerId())
                        .farmAssetId(diagnosticCase.getFarmAssetId())
                        .taskType("FOLLOW_UP")
                        .sourceDiagnosticCaseId(diagnosticCase.getId())
                        .build());

        task.setDueDate(diagnosticCase.getFollowUpDueAt());
        task.setPriority(mapRiskToPriority(diagnosticCase.getRiskLevel()));
        task.setWeatherDependent(false);
        task.setStatus(FarmTask.TaskStatus.PENDING);
        farmTaskRepository.save(task);
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
                                     List<WeatherSummary> weather, List<FarmEvent> farmMemory, List<Message> history,
                                     boolean hasImage) {
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

        sb.append("\n== RECENT WEATHER CONDITIONS ==\n");
        if (weather.isEmpty()) {
            sb.append("No weather data available.\n");
        } else {
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

    private ConversationDto.Response buildResponse(Conversation conversation) {
        List<ConversationDto.MessageView> messages = messageRepository
                .findByConversationIdOrderByCreatedAtAsc(conversation.getId())
                .stream()
                .map(m -> ConversationDto.MessageView.builder()
                        .id(m.getId())
                        .sender(m.getSender())
                        .content(m.getContent())
                        .hasImage(m.getImageBase64() != null)
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
                .build();
    }

    private String truncate(String text, int maxLen) {
        if (text == null) return null;
        return text.length() <= maxLen ? text : text.substring(0, maxLen);
    }
}
