package com.smartagriculture.knowledgeservice.entity;

import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;

import java.time.LocalDateTime;

/**
 * A recommended window for one activity (e.g. Boro transplanting). Stored as month/day so it
 * repeats every year; a window may wrap the year end (start Nov, end Jan).
 */
@Entity
@Table(
    name = "crop_calendar_entries",
    indexes = {
        @Index(name = "idx_calendar_crop_season", columnList = "crop, season"),
        @Index(name = "idx_calendar_deleted",     columnList = "deleted")
    }
)
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class CropCalendarEntry {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    @Column(updatable = false, nullable = false)
    private String id;

    @Column(nullable = false)
    private String crop;

    @Enumerated(EnumType.STRING)
    private KnowledgeEnums.CropSeason season;

    private String region;              // null = applies country-wide

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private KnowledgeEnums.CalendarActivity activity;

    @Column(nullable = false)
    private Integer startMonth;

    @Column(nullable = false)
    private Integer startDay;

    @Column(nullable = false)
    private Integer endMonth;

    @Column(nullable = false)
    private Integer endDay;

    @Column(length = 1000)
    private String notes;

    @Column(nullable = false)
    private String sourceId;

    private String sourcePage;

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
