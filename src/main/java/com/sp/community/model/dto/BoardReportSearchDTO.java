package com.sp.community.model.dto;

import com.sp.community.persistent.entity.BoardReportEntity;
import lombok.*;
import org.springframework.format.annotation.DateTimeFormat;

import java.time.LocalDateTime;

/**
 * 게시글 신고 검색 조건 DTO
 */
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
@ToString
public class BoardReportSearchDTO {

    /**
     * 신고 분류
     */
    private BoardReportEntity.ReportType reportType;

    /**
     * 신고 상태
     */
    private BoardReportEntity.ReportStatus status;

    /**
     * 신고자 ID
     */
    private Long reporterId;

    /**
     * 처리자 ID
     */
    private Long processorId;

    /**
     * 검색 시작 날짜
     */
    @DateTimeFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    private LocalDateTime startDate;

    /**
     * 검색 종료 날짜
     */
    @DateTimeFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    private LocalDateTime endDate;

    /**
     * 게시글 ID
     */
    private Long boardId;

    /**
     * 게시글 제목 키워드
     */
    private String boardTitleKeyword;

    /**
     * 신고 사유 키워드
     */
    private String reasonKeyword;

    /**
     * 페이지 번호 (기본값: 0)
     */
    @Builder.Default
    private Integer page = 0;

    /**
     * 페이지 크기 (기본값: 20)
     */
    @Builder.Default
    private Integer size = 20;

    /**
     * 정렬 기준 (기본값: createdAt)
     */
    @Builder.Default
    private String sortBy = "createdAt";

    /**
     * 정렬 방향 (기본값: desc)
     */
    @Builder.Default
    private String sortDirection = "desc";

    /**
     * 처리 대기 중인 신고만 조회
     */
    @Builder.Default
    private Boolean pendingOnly = false;

    /**
     * 검색 조건 유효성 검증
     */
    public void validate() {
        if (page != null && page < 0) {
            page = 0;
        }

        if (size != null && (size <= 0 || size > 100)) {
            size = 20;
        }

        if (startDate != null && endDate != null && startDate.isAfter(endDate)) {
            throw new IllegalArgumentException("검색 시작 날짜는 종료 날짜보다 이전이어야 합니다.");
        }
    }

    /**
     * 검색 조건 존재 여부 확인
     */
    public boolean hasSearchConditions() {
        return reportType != null ||
                status != null ||
                reporterId != null ||
                processorId != null ||
                startDate != null ||
                endDate != null ||
                boardId != null ||
                (boardTitleKeyword != null && !boardTitleKeyword.trim().isEmpty()) ||
                (reasonKeyword != null && !reasonKeyword.trim().isEmpty());
    }
}