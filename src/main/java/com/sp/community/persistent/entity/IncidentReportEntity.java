package com.sp.community.persistent.entity;

import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.CreationTimestamp;

import java.time.LocalDateTime;

/**
 * 제보글 엔티티
 */
@Entity
@Table(name = "incident_report")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
@ToString(exclude = "imagePath")
public class IncidentReportEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "id")
    private Long id;

    @Lob
    @Column(name = "report_type", columnDefinition = "TEXT")
    private String reportType;

    @Lob
    @Column(name = "report_location", columnDefinition = "TEXT")
    private String reportLocation;

    @Lob
    @Column(name = "content", columnDefinition = "MEDIUMTEXT", nullable = false)
    private String content;

    @Column(name = "image_path", length = 500)
    private String imagePath;

    @Lob
    @Column(name = "url", columnDefinition = "MEDIUMTEXT")
    private String url;

    @Column(name = "reporter_id", nullable = false)
    private Long reporterId;

    @CreationTimestamp
    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;

    // ============ 비즈니스 메서드 ============

    /**
     * 이미지 첨부 여부
     */
    public boolean hasImage() {
        return imagePath != null && !imagePath.trim().isEmpty();
    }

}