package com.sp.community.controller;

import com.sp.community.model.dto.BoardReportCreateDTO;
import com.sp.community.model.vo.BoardReportVO;
import com.sp.community.model.response.CommonApiResponse;
import com.sp.community.service.BoardReportService;
import io.swagger.v3.oas.annotations.Parameter;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

/**
 * 게시글 신고 컨트롤러
 */
@RestController
@RequestMapping("/api/reports")
@RequiredArgsConstructor
@Slf4j
public class BoardReportController {

    private final BoardReportService boardReportService;

    /**
     * 게시글 신고 생성
     * POST /api/reports
     */
    @PostMapping
    public ResponseEntity<CommonApiResponse<BoardReportVO>> createReport(
            @Parameter(description = "신고 생성 정보", required = true) @Valid @RequestBody BoardReportCreateDTO createDTO,
            @Parameter(hidden = true) @AuthenticationPrincipal Long memberId) {

        log.info("게시글 신고 생성 요청: boardId={}, reportType={}, reporterId={}",
                createDTO.getBoardId(), createDTO.getReportType(), memberId);

        createDTO.setReporterId(memberId.toString());
        BoardReportVO reportVO = boardReportService.createReport(createDTO);

        return ResponseEntity.status(HttpStatus.CREATED).body(
                CommonApiResponse.<BoardReportVO>builder()
                        .success(true)
                        .message("신고가 정상적으로 접수되었습니다.")
                        .data(reportVO)
                        .build()
        );
    }

}