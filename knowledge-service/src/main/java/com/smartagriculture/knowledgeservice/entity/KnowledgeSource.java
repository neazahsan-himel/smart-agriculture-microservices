package com.smartagriculture.knowledgeservice.entity;

import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;

import java.time.LocalDate;
import java.time.LocalDateTime;

/**
 * An official publication the knowledge rows come from (e.g. a DAE pesticide list, the BARC
 * fertiliser guide). Every fact row references one, so advice can always cite its source.
 */
@Entity
@Table(
    name = "knowledge_sources",
    indexes = {
        @Index(name = "idx_source_publisher", columnList = "publisher"),
        @Index(name = "idx_source_deleted",   columnList = "deleted")
    }
)
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class KnowledgeSource {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    @Column(updatable = false, nullable = false)
    private String id;

    @Column(nullable = false, length = 300)
    private String title;

    @Column(nullable = false)
    private String publisher;   // e.g. "DAE", "BARC", "BRRI"

    private String edition;

    private Integer publicationYear;

    @Column(length = 1000)
    private String url;

    private String language;    // "en", "bn"

    @Column(length = 500)
    private String licenceNote; // what the terms allow; checked in pilot Phase 0

    private LocalDate retrievedAt;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    @Builder.Default
    private KnowledgeEnums.RecordStatus status = KnowledgeEnums.RecordStatus.ACTIVE;

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
}
