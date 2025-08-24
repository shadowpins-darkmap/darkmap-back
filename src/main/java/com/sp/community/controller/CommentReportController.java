package com.sp.community.controller;

import com.sp.common.mail.model.dto.ReportEmailDto;
import com.sp.common.mail.service.EmailService;
import com.sp.community.model.dto.CommentReportCreateDTO;
import com.sp.community.model.dto.PageRequestDTO;
import com.sp.community.model.response.CommonApiResponse;
import com.sp.community.model.vo.CommentReportVO;
import com.sp.community.model.vo.CommentVO;
import com.sp.community.persistent.entity.CommentEntity;
import com.sp.community.persistent.entity.CommentReportEntity;
import com.sp.community.service.CommentReportService;
import com.sp.community.service.CommentService;
import com.sp.member.persistent.repository.MemberRepository;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.ExampleObject;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.Builder;
import lombok.Data;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.Map;
import java.util.Optional;

/**
 * 댓글 신고 관련 API Controller
 */
@Tag(name = "Comment Report", description = "댓글 신고 관리 API - 댓글 신고의 생성, 조회, 처리 기능을 제공합니다.")
@Slf4j
@RestController
@RequestMapping("/api/v1")
@RequiredArgsConstructor
public class CommentReportController {

    private final CommentService commentService;
    private final CommentReportService commentReportService;
    private final MemberRepository memberRepository;
    private final EmailService emailService;

    /**
     * 댓글 신고 생성
     */
    @Operation(
            summary = "댓글 신고 생성",
            description = "특정 댓글을 신고합니다. 로그인된 사용자만 신고할 수 있으며, 자신의 댓글은 신고할 수 없습니다.",
            requestBody = @io.swagger.v3.oas.annotations.parameters.RequestBody(
                    description = "댓글 신고 생성 정보",
                    required = true,
                    content = @Content(
                            mediaType = "application/json",
                            schema = @Schema(implementation = CommentReportCreateDTO.class),
                            examples = @ExampleObject(
                                    name = "댓글 신고 생성 예시",
                                    value = """
                    {
                        "reportType": "HARASSMENT",
                        "reason": "부적절한 언어를 사용하여 다른 사용자를 괴롭혔습니다.",
                        "additionalInfo": "해당 댓글은 명백히 욕설과 인신공격을 포함하고 있습니다."
                    }
                    """
                            )
                    )
            )
    )
    @ApiResponses({
            @ApiResponse(
                    responseCode = "201",
                    description = "댓글 신고 생성 성공",
                    content = @Content(
                            mediaType = "application/json",
                            examples = @ExampleObject(
                                    value = """
                        {
                            "success": true,
                            "message": "신고가 정상적으로 접수되었습니다.",
                            "data": {
                                "reportId": 1,
                                "commentId": 5,
                                "commentContent": "부적절한 댓글 내용...",
                                "reporterId": "user123",
                                "reportType": "HARASSMENT",
                                "reportTypeDescription": "괴롭힘",
                                "reason": "부적절한 언어를 사용하여 다른 사용자를 괴롭혔습니다.",
                                "status": "PENDING",
                                "statusDescription": "처리 대기",
                                "createdAt": "2024-01-15 10:30:00"
                            }
                        }
                        """
                            )
                    )
            ),
            @ApiResponse(
                    responseCode = "400",
                    description = "잘못된 요청 - 필수 필드 누락, 중복 신고, 자신의 댓글 신고 등",
                    content = @Content(
                            mediaType = "application/json",
                            examples = @ExampleObject(
                                    value = """
                        {
                            "success": false,
                            "message": "이미 신고한 댓글입니다.",
                            "data": null
                        }
                        """
                            )
                    )
            ),
            @ApiResponse(
                    responseCode = "401",
                    description = "인증 필요 - 로그인하지 않은 사용자",
                    content = @Content(
                            mediaType = "application/json",
                            examples = @ExampleObject(
                                    value = """
                        {
                            "success": false,
                            "message": "로그인이 필요합니다",
                            "data": null
                        }
                        """
                            )
                    )
            ),
            @ApiResponse(
                    responseCode = "404",
                    description = "댓글 없음 - 존재하지 않거나 삭제된 댓글",
                    content = @Content(
                            mediaType = "application/json",
                            examples = @ExampleObject(
                                    value = """
                        {
                            "success": false,
                            "message": "존재하지 않는 댓글입니다.",
                            "data": null
                        }
                        """
                            )
                    )
            )
    })
    @PostMapping("/comments/{commentId}/reports")
    public ResponseEntity<CommonApiResponse<CommentReportVO>> createCommentReport(
            @Parameter(description = "댓글 ID", required = true, example = "1") @PathVariable Long commentId,
            @Parameter(description = "댓글 신고 생성 정보", required = true) @Valid @RequestBody CommentReportCreateDTO createDTO,
            @Parameter(hidden = true) @AuthenticationPrincipal Long memberId) {

        if (memberId == null) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body(
                    CommonApiResponse.<CommentReportVO>builder()
                            .success(false)
                            .message("로그인이 필요합니다")
                            .build()
            );
        }

        if (memberId == null) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body(
                    CommonApiResponse.<CommentReportVO>builder()
                            .success(false)
                            .message("로그인이 필요합니다")
                            .build()
            );
        }

        String userId = memberId.toString();
        String userNickname = memberRepository.findNicknameByMemberId(userId)
                .orElse(userId);

        // DTO에 댓글 ID와 신고자 정보 설정
        createDTO.setCommentId(commentId);
        createDTO.setReporterId(userId);

        log.info("댓글 신고 생성 요청: commentId={}, reportType={}, reporterId={}",
                commentId, createDTO.getReportType(), userId);

        CommentReportVO reportVO = commentReportService.createReport(createDTO);

        // 이메일 발송
        /*try {
            emailService.sendCommentReportEmail(
                    commentId, createDTO, reportVO);
        } catch (Exception emailEx) {
            // 이메일 발송 실패해도 신고 접수는 성공으로 처리
            log.warn("댓글 신고 이메일 발송 실패 (신고 접수는 성공): commentId={}, error={}",
                    commentId, emailEx.getMessage());
        }*/

        return ResponseEntity.status(HttpStatus.CREATED).body(
                CommonApiResponse.<CommentReportVO>builder()
                        .success(true)
                        .message("신고가 정상적으로 접수되었습니다.")
                        .data(reportVO)
                        .build()
        );
    }

    /**
     * 댓글 신고 여부 확인
     */
    @Operation(
            summary = "댓글 신고 여부 확인",
            description = "현재 사용자가 특정 댓글을 신고했는지 확인합니다."
    )
    @GetMapping("/comments/{commentId}/reports/check")
    public ResponseEntity<CommonApiResponse<Map<String, Object>>> checkUserReported(
            @Parameter(description = "댓글 ID", required = true, example = "1") @PathVariable Long commentId,
            @Parameter(hidden = true) @AuthenticationPrincipal Long memberId) {

        String userId = memberId != null ? memberId.toString() : null;
        boolean hasReported = commentReportService.hasUserReported(commentId, userId);
        Long reportCount = commentReportService.getCommentReportCount(commentId);

        Map<String, Object> result = Map.of(
                "commentId", commentId,
                "hasReported", hasReported,
                "reportCount", reportCount,
                "canReport", userId != null && !hasReported
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