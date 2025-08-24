package com.sp.community.controller;

import com.sp.common.mail.service.EmailService;
import com.sp.community.model.dto.BoardReportCreateDTO;
import com.sp.community.model.vo.BoardReportVO;
import com.sp.community.model.response.CommonApiResponse;
import com.sp.community.persistent.entity.BoardReportEntity;
import com.sp.community.service.BoardReportService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
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
 * 게시글 신고 컨트롤러
 */
@Tag(name = "Board Report", description = "게시글 신고 관리 API")
@RestController
@RequestMapping("/api/v1/boards")
@RequiredArgsConstructor
@Slf4j
public class BoardReportController {

    private final BoardReportService boardReportService;
    private final EmailService emailService;

    /**
     * 게시글 신고 생성
     */
    @Operation(
            summary = "게시글 신고 생성",
            description = "특정 게시글을 신고합니다. 첨부파일을 포함할 수 있습니다."
    )
    @ApiResponses({
            @ApiResponse(responseCode = "201", description = "신고 생성 성공"),
            @ApiResponse(responseCode = "400", description = "잘못된 요청"),
            @ApiResponse(responseCode = "401", description = "인증 필요"),
            @ApiResponse(responseCode = "404", description = "게시글 없음")
    })
    @PostMapping(value = "/{boardId}/reports", consumes = {"multipart/form-data"})
    public ResponseEntity<CommonApiResponse<BoardReportVO>> createBoardReport(
            @Parameter(description = "게시글 ID", required = true) @PathVariable Long boardId,
            @Parameter(description = "신고 분류", required = true) @RequestParam("reportType") BoardReportEntity.ReportType reportType,
            @Parameter(description = "신고 사유", required = true) @RequestParam("reason") String reason,
            @Parameter(description = "추가 설명") @RequestParam(value = "additionalInfo", required = false) String additionalInfo,
            @Parameter(description = "첨부파일") @RequestParam(value = "attachmentFile", required = false) MultipartFile attachmentFile,
            @Parameter(hidden = true) @AuthenticationPrincipal Long memberId) {

        if (memberId == null) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body(
                    CommonApiResponse.<BoardReportVO>builder()
                            .success(false)
                            .message("로그인이 필요합니다")
                            .build()
            );
        }

        try {
            String userId = memberId.toString();

            // DTO 생성
            BoardReportCreateDTO createDTO = BoardReportCreateDTO.builder()
                    .boardId(boardId)
                    .reporterId(userId)
                    .reportType(reportType)
                    .reason(reason)
                    .additionalInfo(additionalInfo)
                    .attachmentFile(attachmentFile)
                    .build();

            log.info("게시글 신고 생성 요청: boardId={}, reportType={}, reporterId={}, hasAttachment={}",
                    boardId, reportType, userId, attachmentFile != null && !attachmentFile.isEmpty());

            BoardReportVO reportVO = boardReportService.createReport(createDTO);

            // 첨부파일 처리 후 이메일 발송
            try {
                emailService.sendBoardReportEmail(boardId, createDTO, reportVO);
            } catch (Exception emailEx) {
                // 이메일 발송 실패해도 신고 접수는 성공으로 처리
                log.warn("게시글 신고 이메일 발송 실패 (신고 접수는 성공): boardId={}, error={}",
                        boardId, emailEx.getMessage());
            }

            return ResponseEntity.status(HttpStatus.CREATED).body(
                    CommonApiResponse.<BoardReportVO>builder()
                            .success(true)
                            .message("신고가 정상적으로 접수되었습니다.")
                            .data(reportVO)
                            .build()
            );

        } catch (IllegalArgumentException e) {
            return ResponseEntity.badRequest().body(
                    CommonApiResponse.<BoardReportVO>builder()
                            .success(false)
                            .message(e.getMessage())
                            .build()
            );
        } catch (Exception e) {
            log.error("게시글 신고 생성 중 오류 발생: boardId={}", boardId, e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(
                    CommonApiResponse.<BoardReportVO>builder()
                            .success(false)
                            .message("신고 접수 중 오류가 발생했습니다.")
                            .build()
            );
        }
    }

    /**
     * 게시글 신고 여부 확인
     */
    @Operation(
            summary = "게시글 신고 여부 확인",
            description = "현재 사용자가 특정 게시글을 신고했는지 확인합니다."
    )
    @GetMapping("/{boardId}/reports/check")
    public ResponseEntity<CommonApiResponse<Map<String, Object>>> checkUserReported(
            @Parameter(description = "게시글 ID", required = true) @PathVariable Long boardId,
            @Parameter(hidden = true) @AuthenticationPrincipal Long memberId) {

        String userId = memberId != null ? memberId.toString() : null;
        boolean hasReported = boardReportService.hasUserReported(boardId, userId);

        // 신고 수는 관리자만 볼 수 있도록 제한하거나, 일반 사용자에게는 제공하지 않을 수 있음
        Map<String, Object> result = Map.of(
                "boardId", boardId,
                "hasReported", hasReported,
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