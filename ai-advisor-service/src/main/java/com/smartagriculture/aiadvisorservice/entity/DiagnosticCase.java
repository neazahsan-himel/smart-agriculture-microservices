package com.smartagriculture.aiadvisorservice.entity;

import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;

import java.time.LocalDateTime;

@Entity
@Table(
    name = "diagnostic_cases",
    indexes = {
        @Index(name = "idx_case_conversation", columnList = "conversationId"),
        @Index(name = "idx_case_farmer",       columnList = "farmerId"),
        @Index(name = "idx_case_asset",        columnList = "farmAssetId"),
        @Index(name = "idx_case_status",       columnList = "status"),
        @Index(name = "idx_case_followup_due", columnList = "followUpDueAt"),
        @Index(name = "idx_case_deleted",      columnList = "deleted")
    }
)
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class DiagnosticCase {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    @Column(updatable = false, nullable = false)
    private String id;

    @Column(nullable = false)
    private String conversationId;

    @Column(nullable = false)
    private String farmerId;

    private String farmAssetId;

    @Column(length = 1000)
    private String symptom;

    @Column(length = 1000)
    private String hypothesis;

    @Enumerated(EnumType.STRING)
    private ConfidenceLevel confidenceLevel;

    @Enumerated(EnumType.STRING)
    private RiskLevel riskLevel;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    @Builder.Default
    private CaseStatus status = CaseStatus.OPEN;

    @Column(length = 1500)
    private String recommendedAction;

    private LocalDateTime followUpDueAt;

    @Enumerated(EnumType.STRING)
    @Builder.Default
    private Outcome outcome = Outcome.UNKNOWN;

    @Column(nullable = false)
    @Builder.Default
    private Boolean deleted = false;

    @CreationTimestamp
    @Column(updatable = false)
    private LocalDateTime createdAt;

    @UpdateTimestamp
    private LocalDateTime updatedAt;

    @Version
    private Long version;

    public enum ConfidenceLevel {
        LOW, MEDIUM, HIGH
    }

    public enum RiskLevel {
        LOW, MEDIUM, HIGH, CRITICAL
    }

    public enum CaseStatus {
        OPEN, AWAITING_FOLLOWUP, FOLLOWUP_SENT, RESOLVED, ESCALATED
    }

    public enum Outcome {
        IMPROVED, UNCHANGED, WORSENED, UNKNOWN
    }
}
