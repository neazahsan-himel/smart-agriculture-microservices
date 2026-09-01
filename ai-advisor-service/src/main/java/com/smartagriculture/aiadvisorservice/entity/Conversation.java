package com.smartagriculture.aiadvisorservice.entity;

import com.smartagriculture.aiadvisorservice.dto.AdvisorDto;
import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;

import java.time.LocalDateTime;

@Entity
@Table(
    name = "conversations",
    indexes = {
        @Index(name = "idx_conv_farmer",  columnList = "farmerId"),
        @Index(name = "idx_conv_asset",   columnList = "farmAssetId"),
        @Index(name = "idx_conv_status",  columnList = "status"),
        @Index(name = "idx_conv_deleted", columnList = "deleted")
    }
)
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Conversation {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    @Column(updatable = false, nullable = false)
    private String id;

    // Cross-service reference — stored as plain String, no FK
    @Column(nullable = false)
    private String farmerId;

    // Cross-service reference to farm-asset-service — nullable (general questions have no asset)
    private String farmAssetId;

    @Enumerated(EnumType.STRING)
    private AdvisorDto.QueryType topic;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    @Builder.Default
    private ConversationStatus status = ConversationStatus.OPEN;

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

    public enum ConversationStatus {
        OPEN, CLOSED
    }
}
