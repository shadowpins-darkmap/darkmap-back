package com.sp.api.controller;

import com.sp.community.service.BoardService;
import com.sp.api.service.MemberService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.ExampleObject;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

@Tag(name = "Main", description = "메인 화면 API - 패널 정보 etc ")
@Slf4j
@RestController
@RequestMapping("/api/v1/main")
@RequiredArgsConstructor
public class MainController {
    private final MemberService memberService;
    private final BoardService boardService;

    @Operation(
            summary = "저널 패널 통계 조회",
            description = "전체 회원 수, 전체 게시글 수, 사건제보 카테고리 게시글 수를 조회합니다."
    )
    @ApiResponses(value = {
            @ApiResponse(
                    responseCode = "200",
                    description = "통계 조회 성공",
                    content = @Content(
                            mediaType = "application/json",
                            schema = @Schema(implementation = Map.class),
                            examples = @ExampleObject(
                                    value = """
                        {
                          "success": true,
                          "totalMemberCount": 1523,
                          "totalBoardCount": 4567,
                          "incidentReportCount": 234,
                          "message": "조회 성공"
                        }
                        """
                            )
                    )
            ),
            @ApiResponse(
                    responseCode = "500",
                    description = "서버 내부 오류",
                    content = @Content(
                            mediaType = "application/json",
                            examples = @ExampleObject(
                                    value = """
                        {
                          "error": "서버 처리 중 오류가 발생했습니다.",
                          "code": "INTERNAL_SERVER_ERROR"
                        }
                        """
                            )
                    )
            )
    })
    @GetMapping("/journal-pannel")
    public ResponseEntity<?> journalCount() {
        log.info("전체 회원 수 조회 요청");
        long totalMemberCount = memberService.getTotalMemberCount();

        log.info("전체 게시글 개수 조회 요청");
        Long totalBoardCount = boardService.getTotalBoardCount();

        log.info("사건제보 카테고리 게시글 개수 조회 요청");
        Long incidentReportCount = boardService.getIncidentReportBoardCount();

        return ResponseEntity.ok(Map.of(
                "success", true,
                "totalMemberCount", totalMemberCount,
                "totalBoardCount", totalBoardCount,
                "incidentReportCount", incidentReportCount,
                "message", "조회 성공"
        ));
    }

}
