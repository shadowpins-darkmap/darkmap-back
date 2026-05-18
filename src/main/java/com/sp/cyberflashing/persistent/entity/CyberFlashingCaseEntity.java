package com.sp.cyberflashing.persistent.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.PrePersist;
import jakarta.persistence.Table;
import jakarta.persistence.Index;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Entity
@Table(name = "cyber_flashing_case", indexes = {
        @Index(name = "idx_cyber_flashing_country_code", columnList = "country_code"),
        @Index(name = "idx_cyber_flashing_duplicate_flag", columnList = "duplicate_flag"),
        @Index(name = "idx_cyber_flashing_include_flag", columnList = "include_flag")
})
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class CyberFlashingCaseEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "duplicate_flag")
    private String duplicateFlag;

    @Column(name = "include_flag")
    private String includeFlag;

    @Column(name = "edit_note", columnDefinition = "TEXT")
    private String editNote;

    @Column(name = "article_title", nullable = false, length = 500)
    private String articleTitle;

    @Column(name = "content", columnDefinition = "TEXT")
    private String content;

    @Column(name = "url", nullable = false, length = 1000)
    private String url;

    @Column(name = "occurred_date", nullable = false, length = 20)
    private String occurredDate;

    @Column(name = "rss_url", length = 1000)
    private String rssUrl;

    @Column(name = "press", length = 255)
    private String press;

    @Column(name = "reporter", length = 255)
    private String reporter;

    @Column(name = "note", columnDefinition = "TEXT")
    private String note;

    @Column(name = "country_code", nullable = false, length = 10)
    private String countryCode;

    @PrePersist
    public void prePersist() {
        if (countryCode != null && !countryCode.isBlank()) {
            countryCode = countryCode.trim().toUpperCase();
        }
    }
}
