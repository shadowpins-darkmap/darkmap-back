package com.sp.darkmap.persistent.entity;

import com.sp.darkmap.code.InfoType;
import com.sp.darkmap.code.MapCrimeType;
import com.sp.darkmap.persistent.converter.InfoTypeConverter;
import com.sp.darkmap.persistent.converter.MapCrimeTypeConverter;
import jakarta.persistence.Column;
import jakarta.persistence.Convert;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Index;
import jakarta.persistence.Table;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import org.hibernate.annotations.CreationTimestamp;

import java.time.LocalDateTime;

/**
 * 범죄 사례 (뉴스기사 / 회원 경험담) - 신규 독립 테이블.
 * 기존 {@code article} 테이블과 무관.
 */
@Entity
@Table(name = "crime_case", indexes = {
        @Index(name = "idx_crime_case_sido", columnList = "sido"),
        @Index(name = "idx_crime_case_crime_type", columnList = "crime_type"),
        @Index(name = "idx_crime_case_info_type", columnList = "info_type")
})
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class CrimeCaseEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Convert(converter = InfoTypeConverter.class)
    @Column(name = "info_type", nullable = false, length = 20)
    private InfoType infoType;

    @Convert(converter = MapCrimeTypeConverter.class)
    @Column(name = "crime_type", nullable = false, length = 20)
    private MapCrimeType crimeType;

    @Column(name = "sido", nullable = false, length = 50)
    private String sido;

    @Column(name = "sigungu", nullable = false, length = 50)
    private String sigungu;

    @Column(name = "news_url", length = 1000)
    private String newsUrl;

    @Column(name = "latitude", nullable = false)
    private Double latitude;

    @Column(name = "longitude", nullable = false)
    private Double longitude;

    @Column(name = "reporter_id")
    private Long reporterId;

    @CreationTimestamp
    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;
}
