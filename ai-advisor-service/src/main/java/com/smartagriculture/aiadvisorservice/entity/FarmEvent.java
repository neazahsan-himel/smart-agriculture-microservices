package com.smartagriculture.aiadvisorservice.entity;

import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.CreationTimestamp;

import java.time.LocalDateTime;

@Entity
@Table(
    name = "farm_events",
    indexes = {
        @Index(name = "idx_event_farmer", columnList = "farmerId"),
        @Index(name = "idx_event_asset",  columnList = "farmAssetId"),
        @Index(name = "idx_event_type",   columnList = "eventType")
    }
)
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class FarmEvent {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    @Column(updatable = false, nullable = false)
    private String id;

    @Column(nullable = false)
    private String farmerId;

    private String farmAssetId;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private EventType eventType;

    @Column(nullable = false, length = 1500)
    private String description;

    private String diagnosticCaseId;

    @CreationTimestamp
    @Column(updatable = false)
    private LocalDateTime occurredAt;

    public enum EventType {
        OBSERVATION, TREATMENT, DIAGNOSIS, ADVICE, OUTCOME
    }
}
