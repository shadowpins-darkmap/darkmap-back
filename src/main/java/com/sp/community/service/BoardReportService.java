package com.sp.community.service;

import com.sp.community.model.dto.BoardReportCreateDTO;
import com.sp.community.model.dto.BoardReportProcessDTO;
import com.sp.community.model.dto.BoardReportSearchDTO;
import com.sp.community.model.vo.BoardReportVO;
import com.sp.community.persistent.entity.BoardEntity;
import com.sp.community.persistent.entity.BoardReportEntity;
import com.sp.community.persistent.repository.BoardReportRepository;
import com.sp.community.persistent.repository.BoardRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Duration;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * 게시글 신고 서비스
 */
@Service
@RequiredArgsConstructor
@Slf4j
@Transactional(readOnly = true)
public class BoardReportService {

    private final BoardReportRepository boardReportRepository;
    private final BoardRepository boardRepository;

    /**
     * 게시글 신고 생성
     */
    @Transactional
    public BoardReportVO createReport(BoardReportCreateDTO createDTO) {
        log.info("게시글 신고 생성 요청: {}", createDTO);

        // DTO 검증
        createDTO.validate();

        // 게시글 존재 여부 확인
        BoardEntity board = boardRepository.findByIdAndNotDeleted(createDTO.getBoardId())
                .orElseThrow(() -> new IllegalArgumentException("존재하지 않는 게시글입니다."));

        // 자기 게시글 신고 방지
        if (board.getAuthorId().equals(createDTO.getReporterId())) {
            throw new IllegalArgumentException("자신이 작성한 게시글은 신고할 수 없습니다.");
        }

        // 중복 신고 방지
        if (boardReportRepository.existsByBoardIdAndReporterId(
                createDTO.getBoardId(), createDTO.getReporterId())) {
            throw new IllegalArgumentException("이미 신고한 게시글입니다.");
        }

        // 신고 엔티티 생성
        BoardReportEntity reportEntity = BoardReportEntity.builder()
                .board(board)
                .reporterId(createDTO.getReporterId())
                .reportType(createDTO.getReportType())
                .reason(createDTO.getFullReason())
                .status(BoardReportEntity.ReportStatus.PENDING)
                .build();

        // 신고 저장
        BoardReportEntity savedReport = boardReportRepository.save(reportEntity);

        // 게시글에 신고 추가
        board.addReport(savedReport);

        // 신고 수가 임계값을 넘으면 게시글을 신고 상태로 변경
        checkAndUpdateBoardReportStatus(board);

        log.info("게시글 신고 생성 완료: reportId={}, boardId={}", savedReport.getReportId(), board.getBoardId());

        return convertToVO(savedReport);
    }

    /**
     * 신고 목록 조회 (검색 조건)
     */
    public Page<BoardReportVO> getReports(BoardReportSearchDTO searchDTO) {
        log.debug("신고 목록 조회 요청: {}", searchDTO);

        searchDTO.validate();
        Pageable pageable = createPageable(searchDTO);

        Page<BoardReportEntity> reports;

        if (searchDTO.getPendingOnly() != null && searchDTO.getPendingOnly()) {
            // 처리 대기 중인 신고만 조회
            reports = boardReportRepository.findPendingReports(pageable);
        } else if (searchDTO.hasSearchConditions()) {
            // 복합 검색 조건
            reports = boardReportRepository.findBySearchConditions(
                    searchDTO.getReportType(),
                    searchDTO.getStatus(),
                    searchDTO.getReporterId(),
                    searchDTO.getProcessorId(),
                    searchDTO.getStartDate(),
                    searchDTO.getEndDate(),
                    pageable
            );
        } else {
            // 전체 조회
            reports = boardReportRepository.findAll(pageable);
        }

        return reports.map(this::convertToVO);
    }

    /**
     * 신고 상세 조회
     */
    public BoardReportVO getReport(Long reportId) {
        log.debug("신고 상세 조회 요청: reportId={}", reportId);

        BoardReportEntity report = boardReportRepository.findById(reportId)
                .orElseThrow(() -> new IllegalArgumentException("존재하지 않는 신고입니다."));

        return convertToVO(report);
    }

    /**
     * 특정 게시글의 신고 목록 조회
     */
    public Page<BoardReportVO> getReportsByBoardId(Long boardId, Pageable pageable) {
        log.debug("게시글 신고 목록 조회 요청: boardId={}", boardId);

        // 게시글 존재 여부 확인
        if (!boardRepository.existsByIdAndNotDeleted(boardId)) {
            throw new IllegalArgumentException("존재하지 않는 게시글입니다.");
        }

        Page<BoardReportEntity> reports = boardReportRepository.findByBoardId(boardId, pageable);
        return reports.map(this::convertToVO);
    }

    /**
     * 특정 사용자의 신고 목록 조회
     */
    public Page<BoardReportVO> getReportsByReporter(String reporterId, Pageable pageable) {
        log.debug("사용자 신고 목록 조회 요청: reporterId={}", reporterId);

        Page<BoardReportEntity> reports = boardReportRepository.findByReporterId(reporterId, pageable);
        return reports.map(this::convertToVO);
    }

    /**
     * 신고 처리 (승인/거부)
     */
    @Transactional
    public BoardReportVO processReport(BoardReportProcessDTO processDTO) {
        log.info("신고 처리 요청: {}", processDTO);

        processDTO.validate();

        // 신고 조회
        BoardReportEntity report = boardReportRepository.findById(processDTO.getReportId())
                .orElseThrow(() -> new IllegalArgumentException("존재하지 않는 신고입니다."));

        // 처리 가능 상태 확인
        if (report.isProcessed()) {
            throw new IllegalArgumentException("이미 처리된 신고입니다.");
        }

        // 신고 처리
        switch (processDTO.getAction()) {
            case APPROVE -> {
                report.approve(processDTO.getProcessorId(), processDTO.getTrimmedResult());
                handleBoardAction(report.getBoard(), processDTO.getBoardAction());
            }
            case REJECT -> report.reject(processDTO.getProcessorId(), processDTO.getTrimmedResult());
        }

        // 저장
        BoardReportEntity processedReport = boardReportRepository.save(report);

        // 게시글 신고 상태 재확인
        checkAndUpdateBoardReportStatus(report.getBoard());

        log.info("신고 처리 완료: reportId={}, action={}, processorId={}",
                processedReport.getReportId(), processDTO.getAction(), processDTO.getProcessorId());

        return convertToVO(processedReport);
    }

    /**
     * 신고 취소 (신고자 본인만 가능)
     */
    @Transactional
    public void cancelReport(Long reportId, String userId) {
        log.info("신고 취소 요청: reportId={}, userId={}", reportId, userId);

        BoardReportEntity report = boardReportRepository.findById(reportId)
                .orElseThrow(() -> new IllegalArgumentException("존재하지 않는 신고입니다."));

        // 신고자 본인 확인
        if (!report.getReporterId().equals(userId)) {
            throw new IllegalArgumentException("신고 취소 권한이 없습니다.");
        }

        // 취소 가능 상태 확인
        if (report.isProcessed()) {
            throw new IllegalArgumentException("이미 처리된 신고는 취소할 수 없습니다.");
        }

        // 신고 취소
        report.cancel();
        boardReportRepository.save(report);

        // 게시글 신고 상태 재확인
        checkAndUpdateBoardReportStatus(report.getBoard());

        log.info("신고 취소 완료: reportId={}", reportId);
    }

    /**
     * 신고 통계 조회
     */
    public Map<String, Object> getReportStatistics() {
        log.debug("신고 통계 조회 요청");

        return Map.of(
                "totalReports", boardReportRepository.count(),
                "pendingReports", boardReportRepository.countPendingReports(),
                "reviewingReports", boardReportRepository.countReviewingReports(),
                "processedReports", boardReportRepository.countProcessedReports(),
                "todayReports", boardReportRepository.countTodayReports(),
                "reportTypeStats", getReportTypeStatistics(),
                "statusStats", getReportStatusStatistics(),
                "processorStats", getProcessorStatistics()
        );
    }

    /**
     * 신고 유형별 통계
     */
    public Map<String, Long> getReportTypeStatistics() {
        List<Object[]> stats = boardReportRepository.findReportTypeStatistics();
        return stats.stream()
                .collect(Collectors.toMap(
                        row -> ((BoardReportEntity.ReportType) row[0]).getDescription(),
                        row -> (Long) row[1]
                ));
    }

    /**
     * 신고 상태별 통계
     */
    public Map<String, Long> getReportStatusStatistics() {
        List<Object[]> stats = boardReportRepository.findReportStatusStatistics();
        return stats.stream()
                .collect(Collectors.toMap(
                        row -> ((BoardReportEntity.ReportStatus) row[0]).getDescription(),
                        row -> (Long) row[1]
                ));
    }

    /**
     * 처리자별 통계
     */
    public Map<String, Map<String, Long>> getProcessorStatistics() {
        List<Object[]> stats = boardReportRepository.findProcessorStatistics();
        return stats.stream()
                .collect(Collectors.groupingBy(
                        row -> (String) row[0],
                        Collectors.toMap(
                                row -> ((BoardReportEntity.ReportStatus) row[1]).getDescription(),
                                row -> (Long) row[2]
                        )
                ));
    }

    /**
     * 월별 신고 통계
     */
    public List<Map<String, Object>> getMonthlyStatistics(int months) {
        LocalDateTime startDate = LocalDateTime.now().minusMonths(months);
        List<Object[]> stats = boardReportRepository.findMonthlyReportStatistics(startDate);

        return stats.stream()
                .map(row -> Map.of(
                        "year", row[0],
                        "month", row[1],
                        "count", row[2]
                ))
                .collect(Collectors.toList());
    }

    /**
     * 처리 대기 시간이 긴 신고 조회
     */
    public List<BoardReportVO> getLongPendingReports(int days) {
        LocalDateTime threeDaysAgo = LocalDateTime.now().minusDays(days);
        List<BoardReportEntity> reports = boardReportRepository.findLongPendingReports(threeDaysAgo);

        return reports.stream()
                .map(this::convertToVO)
                .collect(Collectors.toList());
    }

    /**
     * 관리자 대시보드용 최근 신고 목록
     */
    public List<BoardReportVO> getRecentReportsForAdmin(int limit) {
        Pageable pageable = PageRequest.of(0, limit);
        List<BoardReportEntity> reports = boardReportRepository.findRecentReportsForAdmin(pageable);

        return reports.stream()
                .map(this::convertToVO)
                .collect(Collectors.toList());
    }

    /**
     * 사용자의 신고 여부 확인
     */
    public boolean hasUserReported(Long boardId, String userId) {
        return boardReportRepository.existsByBoardIdAndReporterId(boardId, userId);
    }

    // === Private Methods ===

    /**
     * 게시글 신고 상태 확인 및 업데이트
     */
    private void checkAndUpdateBoardReportStatus(BoardEntity board) {
        // 승인된 신고 수 조회
        Long approvedReports = boardReportRepository.countApprovedReportsByBoardId(board.getBoardId());

        // 임계값 설정 (예: 3개 이상의 승인된 신고)
        int threshold = 3;

        if (approvedReports >= threshold && !board.isReported()) {
            board.markAsReported();
            boardRepository.save(board);
            log.info("게시글을 신고 상태로 변경: boardId={}, approvedReports={}",
                    board.getBoardId(), approvedReports);
        } else if (approvedReports < threshold && board.isReported()) {
            board.unmarkAsReported();
            boardRepository.save(board);
            log.info("게시글 신고 상태 해제: boardId={}, approvedReports={}",
                    board.getBoardId(), approvedReports);
        }
    }

    /**
     * 게시글 조치 처리
     */
    private void handleBoardAction(BoardEntity board, BoardReportProcessDTO.BoardAction action) {
        if (action == null || action == BoardReportProcessDTO.BoardAction.NONE) {
            return;
        }

        switch (action) {
            case HIDE -> {
                // 게시글 숨김 처리 (isReported = true)
                board.markAsReported();
                log.info("게시글 숨김 처리: boardId={}", board.getBoardId());
            }
            case DELETE -> {
                // 게시글 소프트 삭제
                board.softDelete();
                log.info("게시글 삭제 처리: boardId={}", board.getBoardId());
            }
            case WARNING -> {
                // 작성자 경고 (별도 구현 필요)
                log.info("작성자 경고 처리: boardId={}, authorId={}",
                        board.getBoardId(), board.getAuthorId());
                // TODO: 사용자 경고 로직 구현
            }
        }
    }

    /**
     * Pageable 객체 생성
     */
    private Pageable createPageable(BoardReportSearchDTO searchDTO) {
        Sort sort = Sort.by(
                Sort.Direction.fromString(searchDTO.getSortDirection()),
                searchDTO.getSortBy()
        );

        return PageRequest.of(searchDTO.getPage(), searchDTO.getSize(), sort);
    }

    /**
     * Entity를 VO로 변환
     */
    private BoardReportVO convertToVO(BoardReportEntity entity) {
        BoardReportVO vo = BoardReportVO.builder()
                .reportId(entity.getReportId())
                .boardId(entity.getBoard().getBoardId())
                .boardTitle(entity.getBoard().getTitle())
                .boardAuthorNickname(entity.getBoard().getAuthorNickname())
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