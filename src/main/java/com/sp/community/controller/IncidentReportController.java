package com.sp.community.controller;

import com.sp.mail.service.EmailService;
import com.sp.community.model.dto.*;
import com.sp.community.model.response.CommonApiResponse;
import com.sp.community.model.vo.*;
import com.sp.community.service.IncidentReportService;
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

/**
 * 제보 관련 API Controller
 */
@Tag(name = "IncidentReport", description = "제보 API")
@Slf4j
@RestController
@RequestMapping("/api/v1/incidentReport")
@RequiredArgsConstructor
public class IncidentReportController {

    private final EmailService emailService;
    private final IncidentReportService incidentReportService;

    /**
     * 제보글 신고 생성
     */
    @Operation(
            summary = "제보글 신고 생성",
            description = "특정 게시글을 신고합니다. 첨부파일을 포함할 수 있습니다."
    )
    @ApiResponses({
            @ApiResponse(responseCode = "201", description = "신고 생성 성공"),
            @ApiResponse(responseCode = "400", description = "잘못된 요청"),
            @ApiResponse(responseCode = "401", description = "인증 필요")
    })
    @PostMapping(value = "/reports", consumes = {"multipart/form-data"})
    public ResponseEntity<CommonApiResponse<IncidentReportVO>> createIncidentReport(
            @Parameter(description = "제보 유형", required = true) @RequestParam("reportType") String reportType,
            @Parameter(description = "제보 위치", required = true) @RequestParam("reportLocation") String reportLocation,
            @Parameter(description = "내용", required = true) @RequestParam("content") String content,
            @Parameter(description = "뉴스기사") @RequestParam(value = "url", required = false) String url,
            @Parameter(description = "첨부파일") @RequestParam(value = "imageFile", required = false) MultipartFile imageFile,
            @Parameter(hidden = true) @AuthenticationPrincipal Long memberId) {

        if (memberId == null) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body(
                    CommonApiResponse.<IncidentReportVO>builder()
                            .success(false)
                            .message("로그인이 필요합니다")
                            .build()
            );
        }

        try {
            // DTO 생성
            IncidentReportCreateDTO createDTO = IncidentReportCreateDTO.builder()
                    .reportType(reportType)
                    .reportLocation(reportLocation)
                    .content(content)
                    .url(url)
                    .reporterId(memberId)
                    .imageFile(imageFile)
                    .build();

            log.info("제보글 신고 생성 요청: reportType={}, reporterId={}, hasAttachment={}",
                    reportType, memberId, imageFile != null && !imageFile.isEmpty());

            IncidentReportVO incidentReportVO = incidentReportService.createIncidentReport(createDTO);

            // 첨부파일 처리 후 이메일 발송
            try {
                emailService.sendIncidentReportCreationEmail(createDTO, incidentReportVO);
            } catch (Exception emailEx) {
                // 이메일 발송 실패해도 신고 접수는 성공으로 처리
                log.warn("제보글 신고 이메일 발송 실패 (신고 접수는 성공): error={}",
                        emailEx.getMessage());
            }

            return ResponseEntity.status(HttpStatus.CREATED).body(
                    CommonApiResponse.<IncidentReportVO>builder()
                            .success(true)
                            .message("신고가 정상적으로 접수되었습니다.")
                            .data(incidentReportVO)
                            .build()
            );

        } catch (IllegalArgumentException e) {
            return ResponseEntity.badRequest().body(
                    CommonApiResponse.<IncidentReportVO>builder()
                            .success(false)
                            .message(e.getMessage())
                            .build()
            );
        } catch (Exception e) {
            log.error("제보글 신고 생성 중 오류 발생: ", e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(
                    CommonApiResponse.<IncidentReportVO>builder()
                            .success(false)
                            .message("신고 접수 중 오류가 발생했습니다.")
                            .build()
            );
        }
    }

}