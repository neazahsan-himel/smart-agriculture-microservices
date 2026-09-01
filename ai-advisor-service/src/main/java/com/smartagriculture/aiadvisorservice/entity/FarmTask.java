package com.smartagriculture.aiadvisorservice.entity;

import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;

import java.time.LocalDateTime;

@Entity
@Table(
    name = "farm_tasks",
    indexes = {
        @Index(name = "idx_task_farmer",   columnList = "farmerId"),
        @Index(name = "idx_task_asset",    columnList = "farmAssetId"),
        @Index(name = "idx_task_status",   columnList = "status"),
        @Index(name = "idx_task_due",      columnList = "dueDate"),
        @Index(name = "idx_task_deleted",  columnList = "deleted")
    }
)
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class FarmTask {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    @Column(updatable = false, nullable = false)
    private String id;

    @Column(nullable = false)
    private String farmerId;

    private String farmAssetId;

    @Column(nullable = false)
    private String taskType;

    private LocalDateTime dueDate;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    @Builder.Default
    private TaskPriority priority = TaskPriority.MEDIUM;

    @Column(nullable = false)
    @Builder.Default
    private Boolean weatherDependent = false;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    @Builder.Default
    private TaskStatus status = TaskStatus.PENDING;

    private String sourceDiagnosticCaseId;

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

    public enum TaskPriority {
        LOW, MEDIUM, HIGH, CRITICAL
    }

    public enum TaskStatus {
        PENDING, DONE, SKIPPED
    }
}
