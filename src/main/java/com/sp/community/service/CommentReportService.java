package com.sp.community.service;

import com.sp.community.model.dto.CommentReportCreateDTO;
import com.sp.community.model.dto.PageRequestDTO;
import com.sp.community.model.vo.CommentReportVO;
import com.sp.community.persistent.entity.CommentEntity;
import com.sp.community.persistent.entity.CommentReportEntity;
import com.sp.community.persistent.repository.CommentReportRepository;
import com.sp.community.persistent.repository.CommentRepository;
import com.sp.exception.CommentNotFoundException;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Duration;

/**
 * 댓글 신고 서비스
 */
@Service
@RequiredArgsConstructor
@Slf4j
@Transactional(readOnly = true)
public class CommentReportService {

    private final CommentReportRepository commentReportRepository;
    private final CommentRepository commentRepository;

    /**
     * 댓글 신고 생성
     */
    @Transactional
    public CommentReportVO createReport(CommentReportCreateDTO createDTO) {
        log.info("댓글 신고 생성 요청: {}", createDTO);

        // DTO 검증
        createDTO.validate();

        // 댓글 존재 여부 확인
        CommentEntity comment = commentRepository.findById(createDTO.getCommentId())
                .orElseThrow(() -> new CommentNotFoundException("존재하지 않는 댓글입니다."));

        // 삭제된 댓글 확인
        if (comment.getIsDeleted()) {
            throw new IllegalArgumentException("삭제된 댓글은 신고할 수 없습니다.");
        }

        // 자기 댓글 신고 방지
        if (comment.getAuthorId().equals(createDTO.getReporterId())) {
            throw new IllegalArgumentException("자신이 작성한 댓글은 신고할 수 없습니다.");
        }

        // 중복 신고 방지
        if (commentReportRepository.existsByCommentIdAndReporterId(
                createDTO.getCommentId(), createDTO.getReporterId())) {
            throw new IllegalArgumentException("이미 신고한 댓글입니다.");
        }

        // 신고 엔티티 생성
        CommentReportEntity reportEntity = CommentReportEntity.builder()
                .comment(comment)
                .reporterId(createDTO.getReporterId())
                .reportType(createDTO.getReportType())
                .reason(createDTO.getFullReason())
                .status(CommentReportEntity.ReportStatus.PENDING)
                .build();

        // 신고 저장
        CommentReportEntity savedReport = commentReportRepository.save(reportEntity);

        // 댓글에 신고 추가
        comment.getReports().add(savedReport);

        // 신고 수가 임계값을 넘으면 댓글을 신고 상태로 변경
        checkAndUpdateCommentReportStatus(comment);

        log.info("댓글 신고 생성 완료: reportId={}, commentId={}",
                savedReport.getReportId(), comment.getCommentId());

        return convertToVO(savedReport);
    }

    /**
     * 사용자의 신고 여부 확인
     */
    public boolean hasUserReported(Long commentId, String userId) {
        return commentReportRepository.existsByCommentIdAndReporterId(commentId, userId);
    }

    /**
     * 특정 댓글의 신고 수 조회
     */
    public Long getCommentReportCount(Long commentId) {
        return commentReportRepository.countByCommentId(commentId);
    }

    // === Private Helper Methods ===

    /**
     * 댓글 신고 상태 확인 및 업데이트
     */
    private void checkAndUpdateCommentReportStatus(CommentEntity comment) {
        // 승인된 신고 수 조회
        Long approvedReports = commentReportRepository.countApprovedReportsByCommentId(comment.getCommentId());

        // 임계값 설정 (예: 10개 이상의 승인된 신고)
        int threshold = 10;

        if (approvedReports >= threshold && !comment.getIsReported()) {
            comment.setReported(true);
            commentRepository.save(comment);
            log.info("댓글을 신고 상태로 변경: commentId={}, approvedReports={}",
                    comment.getCommentId(), approvedReports);
        } else if (approvedReports < threshold && comment.getIsReported()) {
            comment.setReported(false);
            commentRepository.save(comment);
            log.info("댓글 신고 상태 해제: commentId={}, approvedReports={}",
                    comment.getCommentId(), approvedReports);
        }
    }

    /**
     * Pageable 객체 생성
     */
    private Pageable createPageable(PageRequestDTO pageRequestDTO) {
        Sort sort = Sort.by(
                Sort.Direction.fromString(pageRequestDTO.getDirection().name()),
                pageRequestDTO.getSortBy()
        );

        return PageRequest.of(pageRequestDTO.getPage(), pageRequestDTO.getSize(), sort);
    }

    /**
     * 기본 Pageable 객체 생성
     */
    private Pageable createDefaultPageable() {
        return PageRequest.of(0, 20, Sort.by(Sort.Direction.DESC, "createdAt"));
    }

    /**
     * Entity를 VO로 변환
     */
    private CommentReportVO convertToVO(CommentReportEntity entity) {
        CommentReportVO vo = CommentReportVO.builder()
                .reportId(entity.getReportId())
                .commentId(entity.getComment().getCommentId())
                .commentContent(entity.getComment().getContent())
                .commentAuthorId(entity.getComment().getAuthorId())
                .commentAuthorNickname(entity.getComment().getAuthorNickname())
                .boardId(entity.getComment().getBoard().getBoardId())
                .boardTitle(entity.getComment().getBoard().getTitle())
                .reporterId(entity.getReporterId())
                .reportType(entity.getReportType())
                .reportTypeDescription(entity.getReportType().getDescription())
                .reason(entity.getReason())
                .status(entity.getStatus())
                .statusDescription(entity.getStatus().getDescription())
                .result(entity.getResult())
                .processorId(entity.getProcessorId())
                .createdAt(entity.getCreatedAt())
                .updatedAt(entity.getUpdatedAt())
                .processedAt(entity.getProcessedAt())
                .build();

        // 처리 소요 시간 계산
        if (entity.getCreatedAt() != null && entity.getProcessedAt() != null) {
            Duration duration = Duration.between(entity.getCreatedAt(), entity.getProcessedAt());
            vo.setProcessingHours(duration.toHours());
        }

        return vo;
    }
}