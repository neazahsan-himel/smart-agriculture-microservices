package com.smartagriculture.aiadvisorservice.service;

import com.smartagriculture.aiadvisorservice.dto.ConversationDto;
import com.smartagriculture.aiadvisorservice.dto.DiagnosticCaseDto;
import com.smartagriculture.aiadvisorservice.dto.FarmEventDto;
import com.smartagriculture.aiadvisorservice.dto.FarmTaskDto;
import com.smartagriculture.aiadvisorservice.entity.DiagnosticCase;
import com.smartagriculture.aiadvisorservice.entity.FarmTask;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

public interface ConversationalAdvisorService {

    ConversationDto.Response startConversation(ConversationDto.StartRequest request);

    ConversationDto.Response continueConversation(String conversationId, ConversationDto.MessageRequest request);

    ConversationDto.Response getConversation(String conversationId);

    Page<ConversationDto.Response> getConversationsByFarmer(String farmerId, Pageable pageable);

    DiagnosticCaseDto.Response recordOutcome(String diagnosticCaseId, DiagnosticCase.Outcome outcome);

    Page<FarmEventDto.Response> getFarmEvents(String farmerId, String farmAssetId, Pageable pageable);

    Page<FarmTaskDto.Response> getFarmTasks(String farmerId, Pageable pageable);

    FarmTaskDto.Response updateFarmTaskStatus(String taskId, FarmTask.TaskStatus status);
}
