package com.smartagriculture.aiadvisorservice.repository;

import com.smartagriculture.aiadvisorservice.entity.DiagnosticCase;
import org.springframework.data.jpa.repository.JpaRepository;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

public interface DiagnosticCaseRepository extends JpaRepository<DiagnosticCase, String> {

    Optional<DiagnosticCase> findByIdAndDeletedFalse(String id);

    List<DiagnosticCase> findByConversationIdAndStatusInOrderByCreatedAtDesc(
            String conversationId, List<DiagnosticCase.CaseStatus> statuses);

    Optional<DiagnosticCase> findFirstByConversationIdOrderByCreatedAtDesc(String conversationId);

    List<DiagnosticCase> findByStatusAndFollowUpDueAtLessThanEqual(
            DiagnosticCase.CaseStatus status, LocalDateTime now);
}
