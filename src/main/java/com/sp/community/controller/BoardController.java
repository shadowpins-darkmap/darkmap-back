package com.sp.community.controller;

import com.sp.community.model.dto.BoardCreateDTO;
import com.sp.community.model.dto.BoardSearchDTO;
import com.sp.community.model.dto.BoardUpdateDTO;
import com.sp.community.model.dto.PageRequestDTO;
import com.sp.community.model.response.CommonApiResponse;
import com.sp.community.model.vo.BoardDetailVO;
import com.sp.community.model.vo.BoardListVO;
import com.sp.community.model.vo.BoardVO;
import com.sp.community.service.BoardService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.ExampleObject;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/**
 * 게시글 관련 API Controller
 */
@Tag(name = "Board", description = "게시글 관리 API")
@Slf4j
@RestController
@RequestMapping("/api/v1/boards")
@RequiredArgsConstructor
public class BoardController {

    private final BoardService boardService;

    /**
     * 게시글 목록 조회
     */
    @Operation(summary = "게시글 목록 조회", description = "검색 조건에 따른 게시글 목록을 조회합니다.")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "조회 성공"),
            @ApiResponse(responseCode = "400", description = "잘못된 요청"),
            @ApiResponse(responseCode = "500", description = "서버 오류",
                    content = @Content(mediaType = "application/json",
                            examples = {@ExampleObject()}))
    })
    @GetMapping
    public ResponseEntity<CommonApiResponse<BoardListVO>> getBoardList(
            @Parameter(description = "검색 조건") @ModelAttribute BoardSearchDTO searchDTO,
            @Parameter(description = "페이징 정보") @ModelAttribute PageRequestDTO pageRequestDTO) {

        log.info("게시글 목록 조회 요청: search={}, page={}", searchDTO, pageRequestDTO);

        BoardListVO boardList = boardService.getBoardList(searchDTO, pageRequestDTO);

        return ResponseEntity.ok(
                CommonApiResponse.<BoardListVO>builder()
                        .success(true)
                        .message("게시글 목록 조회 성공")
                        .data(boardList)
                        .build()
        );
    }

    /**
     * 게시글 상세 조회
     */
    @Operation(summary = "게시글 상세 조회", description = "게시글 상세 정보를 조회합니다.")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "조회 성공"),
            @ApiResponse(responseCode = "404", description = "게시글 없음"),
            @ApiResponse(responseCode = "403", description = "접근 권한 없음")
    })
    @GetMapping("/{boardId}")
    public ResponseEntity<CommonApiResponse<BoardDetailVO>> getBoardDetail(
            @Parameter(description = "게시글 ID") @PathVariable Long boardId,
            @AuthenticationPrincipal UserDetails userDetails) {

        log.info("게시글 상세 조회: boardId={}", boardId);

        String currentUserId = userDetails != null ? userDetails.getUsername() : null;
        BoardDetailVO boardDetail = boardService.getBoardDetail(boardId, currentUserId);

        return ResponseEntity.ok(
                CommonApiResponse.<BoardDetailVO>builder()
                        .success(true)
                        .message("게시글 조회 성공")
                        .data(boardDetail)
                        .build()
        );
    }

    /**
     * 게시글 생성
     */
    @Operation(summary = "게시글 생성", description = "새로운 게시글을 생성합니다.")
    @ApiResponses({
            @ApiResponse(responseCode = "201", description = "생성 성공"),
            @ApiResponse(responseCode = "400", description = "잘못된 요청"),
            @ApiResponse(responseCode = "401", description = "인증 필요")
    })
    @PostMapping(consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public ResponseEntity<CommonApiResponse<BoardVO>> createBoard(
            @Parameter(description = "게시글 생성 정보") @Valid @ModelAttribute BoardCreateDTO createDTO,
            @AuthenticationPrincipal Long memberId) {

        log.info("게시글 생성 요청: title={}, authorId={}", createDTO.getTitle(), createDTO.getAuthorId());

        // 현재 사용자 정보 설정
        createDTO.setAuthorId(memberId+"");

        BoardVO createdBoard = boardService.createBoard(createDTO);

        return ResponseEntity.status(HttpStatus.CREATED).body(
                CommonApiResponse.<BoardVO>builder()
                        .success(true)
                        .message("게시글 생성 성공")
                        .data(createdBoard)
                        .build()
        );
    }

    /**
     * 게시글 수정
     */
    @Operation(summary = "게시글 수정", description = "게시글을 수정합니다.")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "수정 성공"),
            @ApiResponse(responseCode = "400", description = "잘못된 요청"),
            @ApiResponse(responseCode = "403", description = "수정 권한 없음"),
            @ApiResponse(responseCode = "404", description = "게시글 없음")
    })
    @PutMapping(value = "/{boardId}", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public ResponseEntity<CommonApiResponse<BoardVO>> updateBoard(
            @Parameter(description = "게시글 ID") @PathVariable Long boardId,
            @Parameter(description = "게시글 수정 정보") @Valid @ModelAttribute BoardUpdateDTO updateDTO,
            @AuthenticationPrincipal Long memberId) {

        log.info("게시글 수정 요청: boardId={}", boardId);

        // 게시글 ID 설정
        updateDTO.setBoardId(boardId);

        // 현재 사용자 정보 설정
        updateDTO.setEditorId(memberId+"");

        BoardVO updatedBoard = boardService.updateBoard(updateDTO);

        return ResponseEntity.ok(
                CommonApiResponse.<BoardVO>builder()
                        .success(true)
                        .message("게시글 수정 성공")
                        .data(updatedBoard)
                        .build()
        );
    }

    /**
     * 게시글 삭제
     */
    @Operation(summary = "게시글 삭제", description = "게시글을 삭제합니다.")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "삭제 성공"),
            @ApiResponse(responseCode = "403", description = "삭제 권한 없음"),
            @ApiResponse(responseCode = "404", description = "게시글 없음")
    })
    @DeleteMapping("/{boardId}")
    public ResponseEntity<CommonApiResponse<Void>> deleteBoard(
            @Parameter(description = "게시글 ID") @PathVariable Long boardId,
            @AuthenticationPrincipal Long memberId) {

        log.info("게시글 삭제 요청: boardId={}", boardId);

        boardService.deleteBoard(boardId, memberId+"");

        return ResponseEntity.ok(
                CommonApiResponse.<Void>builder()
                        .success(true)
                        .message("게시글 삭제 성공")
                        .build()
        );
    }

    /**
     * 인기 게시글 조회
     */
    @Operation(summary = "인기 게시글 조회", description = "인기 게시글 목록을 조회합니다.")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "조회 성공")
    })
    @GetMapping("/popular")
    public ResponseEntity<CommonApiResponse<List<BoardVO>>> getPopularBoards(
            @Parameter(description = "조회할 게시글 수") @RequestParam(defaultValue = "10") int limit) {

        log.info("인기 게시글 조회: limit={}", limit);

        List<BoardVO> popularBoards = boardService.getPopularBoards(limit);

        return ResponseEntity.ok(
                CommonApiResponse.<List<BoardVO>>builder()
                        .success(true)
                        .message("인기 게시글 조회 성공")
                        .data(popularBoards)
                        .build()
        );
    }

    /**
     * 최근 게시글 조회
     */
    @Operation(summary = "최근 게시글 조회", description = "최근 게시글 목록을 조회합니다.")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "조회 성공")
    })
    @GetMapping("/recent")
    public ResponseEntity<CommonApiResponse<List<BoardVO>>> getRecentBoards(
            @Parameter(description = "조회할 게시글 수") @RequestParam(defaultValue = "10") int limit) {

        log.info("최근 게시글 조회: limit={}", limit);

        List<BoardVO> recentBoards = boardService.getRecentBoards(limit);

        return ResponseEntity.ok(
                CommonApiResponse.<List<BoardVO>>builder()
                        .success(true)
                        .message("최근 게시글 조회 성공")
                        .data(recentBoards)
                        .build()
        );
    }

    /**
     * 내가 작성한 게시글 조회
     */
    @Operation(summary = "내 게시글 조회", description = "현재 사용자가 작성한 게시글을 조회합니다.")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "조회 성공"),
            @ApiResponse(responseCode = "401", description = "인증 필요")
    })
    @GetMapping("/my")
    public ResponseEntity<CommonApiResponse<BoardListVO>> getMyBoards(
            @Parameter(description = "페이징 정보") @ModelAttribute PageRequestDTO pageRequestDTO,
            @AuthenticationPrincipal Long memberId) {

        log.info("내 게시글 조회: userId={}", memberId);

        if (memberId == null) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body(
                    CommonApiResponse.<BoardListVO>builder()
                            .success(false)
                            .message("로그인이 필요합니다")
                            .build()
            );
        }

        BoardListVO myBoards = boardService.getUserBoards(memberId+"", pageRequestDTO);

        return ResponseEntity.ok(
                CommonApiResponse.<BoardListVO>builder()
                        .success(true)
                        .message("내 게시글 조회 성공")
                        .data(myBoards)
                        .build()
        );
    }
}