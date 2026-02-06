package com.sp.community.controller;

import com.sp.mail.service.EmailService;
import com.sp.community.model.dto.CommentReportCreateDTO;
import com.sp.community.model.response.CommonApiResponse;
import com.sp.community.model.vo.CommentReportVO;
import com.sp.community.persistent.entity.CommentReportEntity;
import com.sp.community.service.CommentReportService;
import com.sp.community.service.CommentService;
import com.sp.member.repository.MemberRepository;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.util.Map;

/**
 * 댓글 신고 관련 API Controller
 */
@Tag(name = "Comment Report", description = "댓글 신고 관리 API - 댓글 신고의 생성, 조회, 처리 기능을 제공합니다.")
@Slf4j
@RestController
@RequestMapping("/api/v1/comments")
@RequiredArgsConstructor
public class CommentReportController {

    private final CommentService commentService;
    private final CommentReportService commentReportService;
    private final MemberRepository memberRepository;
    private final EmailService emailService;

    @PostMapping(value = "/{commentId}/reports", consumes = {"multipart/form-data"})
    public ResponseEntity<CommonApiResponse<CommentReportVO>> createCommentReport(
            @Parameter(description = "댓글 ID", required = true, example = "1") @PathVariable Long commentId,
            @Parameter(description = "신고 분류", required = true) @RequestParam("reportType") CommentReportEntity.ReportType reportType,
            @Parameter(description = "신고 사유", required = true) @RequestParam("reason") String reason,
            @Parameter(description = "추가 설명") @RequestParam(value = "additionalInfo", required = false) String additionalInfo,
            @Parameter(description = "첨부파일") @RequestParam(value = "attachmentFile", required = false) MultipartFile attachmentFile,
            @Parameter(hidden = true) @AuthenticationPrincipal Long memberId) {

        if (memberId == null) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body(
                    CommonApiResponse.<CommentReportVO>builder()
                            .success(false)
                            .message("로그인이 필요합니다")
                            .build()
            );
        }

        try {
            // DTO 생성
            CommentReportCreateDTO createDTO = CommentReportCreateDTO.builder()
                    .commentId(commentId)
                    .reporterId(memberId)
                    .reportType(reportType)
                    .reason(reason)
                    .additionalInfo(additionalInfo)
                    .attachmentFile(attachmentFile)
                    .build();

            log.info("댓글 신고 생성 요청: commentId={}, reportType={}, reporterId={}, hasAttachment={}",
                    commentId, reportType, memberId, attachmentFile != null && !attachmentFile.isEmpty());

            CommentReportVO reportVO = commentReportService.createReport(createDTO);

            try {
                emailService.sendCommentReportEmail(commentId, createDTO, reportVO);
                log.info("댓글 신고 이메일 발송 시도: commentId={}", commentId);
            } catch (Exception emailEx) {
                // 이메일 발송 실패해도 신고 접수는 성공으로 처리
                log.warn("댓글 신고 이메일 발송 실패 (신고 접수는 성공): commentId={}, error={}",
                        commentId, emailEx.getMessage());
                // 예외를 다시 던지지 않음
            }

            return ResponseEntity.status(HttpStatus.CREATED).body(
                    CommonApiResponse.<CommentReportVO>builder()
                            .success(true)
                            .message("신고가 정상적으로 접수되었습니다.")
                            .data(reportVO)
                            .build()
            );

        } catch (IllegalArgumentException e) {
            return ResponseEntity.badRequest().body(
                    CommonApiResponse.<CommentReportVO>builder()
                            .success(false)
                            .message(e.getMessage())
                            .build()
            );
        } catch (Exception e) {
            log.error("댓글 신고 생성 중 오류 발생: commentId={}", commentId, e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(
                    CommonApiResponse.<CommentReportVO>builder()
                            .success(false)
                            .message("신고 접수 중 오류가 발생했습니다.")
                            .build()
            );
        }
    }

    /**
     * 댓글 신고 여부 확인
     */
    @Operation(
            summary = "댓글 신고 여부 확인",
            description = "현재 사용자가 특정 댓글을 신고했는지 확인합니다."
    )
    @GetMapping("/{commentId}/reports/check")
    public ResponseEntity<CommonApiResponse<Map<String, Object>>> checkUserReported(
            @Parameter(description = "댓글 ID", required = true, example = "1") @PathVariable Long commentId,
            @Parameter(hidden = true) @AuthenticationPrincipal Long memberId) {

        boolean hasReported = commentReportService.hasUserReported(commentId, memberId);
        Long reportCount = commentReportService.getCommentReportCount(commentId);

        Map<String, Object> result = Map.of(
                "commentId", commentId,
                "hasReported", hasReported,
                "reportCount", reportCount,
                "canReport", memberId != null && !hasReported
        );

        return ResponseEntity.ok(
                CommonApiResponse.<Map<String, Object>>builder()
                        .success(true)
                        .message("신고 여부 확인 성공")
                        .data(result)
                        .build()
        );
    }
}