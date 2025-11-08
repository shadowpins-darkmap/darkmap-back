package com.sp.community.controller;

import com.sp.common.mail.service.EmailService;
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
import io.swagger.v3.oas.annotations.media.Schema;
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
@Tag(name = "Board", description = "게시글 관리 API - 게시글의 생성, 조회, 수정, 삭제 및 검색 기능을 제공합니다.")
@Slf4j
@RestController
@RequestMapping("/api/v1/boards")
@RequiredArgsConstructor
public class BoardController {

    private final BoardService boardService;
    private final EmailService emailService;


    /**
     * 전체 게시글 개수 조회
     */
    @Operation(
            summary = "전체 게시글 개수 조회",
            description = "삭제되지 않은 전체 게시글의 개수를 조회합니다."
    )
    @ApiResponses({
            @ApiResponse(
                    responseCode = "200",
                    description = "전체 게시글 개수 조회 성공",
                    content = @Content(
                            mediaType = "application/json",
                            examples = @ExampleObject(
                                    value = """
                    {
                        "success": true,
                        "message": "전체 게시글 개수 조회 성공",
                        "data": 1250
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
                        "success": false,
                        "message": "서버 내부 오류가 발생했습니다.",
                        "data": null
                    }
                    """
                            )
                    )
            )
    })
    @GetMapping("/count")
    public ResponseEntity<CommonApiResponse<Long>> getTotalBoardCount() {
        log.info("전체 게시글 개수 조회 요청");

        Long totalCount = boardService.getTotalBoardCount();

        return ResponseEntity.ok(
                CommonApiResponse.<Long>builder()
                        .success(true)
                        .message("전체 게시글 개수 조회 성공")
                        .data(totalCount)
                        .build()
        );
    }

    /**
     * 사건제보 카테고리 게시글 개수 조회
     */
    @Operation(
            summary = "사건제보 게시글 개수 조회",
            description = "삭제되지 않은 사건제보 카테고리 게시글의 개수를 조회합니다."
    )
    @ApiResponses({
            @ApiResponse(
                    responseCode = "200",
                    description = "사건제보 게시글 개수 조회 성공",
                    content = @Content(
                            mediaType = "application/json",
                            examples = @ExampleObject(
                                    value = """
                    {
                        "success": true,
                        "message": "사건제보 게시글 개수 조회 성공",
                        "data": 85
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
                        "success": false,
                        "message": "서버 내부 오류가 발생했습니다.",
                        "data": null
                    }
                    """
                            )
                    )
            )
    })
    @GetMapping("/count/incident-reports")
    public ResponseEntity<CommonApiResponse<Long>> getIncidentReportBoardCount() {
        log.info("사건제보 카테고리 게시글 개수 조회 요청");

        Long incidentReportCount = boardService.getIncidentReportBoardCount();

        return ResponseEntity.ok(
                CommonApiResponse.<Long>builder()
                        .success(true)
                        .message("사건제보 게시글 개수 조회 성공")
                        .data(incidentReportCount)
                        .build()
        );
    }

    /**
     * 게시글 목록 조회
     */
    @Operation(
            summary = "게시글 목록 조회",
            description = "검색 조건과 페이징 정보에 따른 게시글 목록을 조회합니다. 제목, 내용, 작성자로 검색할 수 있습니다."
    )
    @ApiResponses(value = {
            @ApiResponse(
                    responseCode = "200",
                    description = "게시글 목록 조회 성공",
                    content = @Content(
                            mediaType = "application/json",
                            examples = @ExampleObject(
                                    value = """
                        {
                            "success": true,
                            "message": "게시글 목록 조회 성공",
                            "data": {
                                "boards": [
                                    {
                                        "boardId": 1,
                                        "title": "첫 번째 게시글",
                                        "content": "게시글 내용입니다.",
                                        "authorId": 1,
                                        "authorNickname": "사용자123",
                                        "viewCount": 150,
                                        "likeCount": 25,
                                        "commentCount": 8,
                                        "createdAt": "2024-01-15 10:30:00",
                                        "isAuthor": true
                                    }
                                ],
                                "totalElements": 100,
                                "totalPages": 10,
                                "currentPage": 1,
                                "pageSize": 10,
                                "hasNext": true,
                                "hasPrevious": false
                            }
                        }
                        """
                            )
                    )
            ),
            @ApiResponse(
                    responseCode = "400",
                    description = "잘못된 요청 - 유효하지 않은 검색 조건 또는 페이징 파라미터",
                    content = @Content(
                            mediaType = "application/json",
                            examples = @ExampleObject(
                                    value = """
                        {
                            "success": false,
                            "message": "페이지 번호는 1 이상이어야 합니다.",
                            "data": null
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
                            "success": false,
                            "message": "서버 내부 오류가 발생했습니다.",
                            "data": null
                        }
                        """
                            )
                    )
            )
    })
    @GetMapping
    public ResponseEntity<CommonApiResponse<BoardListVO>> getBoardList(
            @ModelAttribute BoardSearchDTO searchDTO,
            @ModelAttribute PageRequestDTO pageRequestDTO) {

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
    @Operation(
            summary = "게시글 상세 조회",
            description = "게시글 ID로 특정 게시글의 상세 정보를 조회합니다. 조회 시 조회수가 1 증가합니다."
    )
    @ApiResponses({
            @ApiResponse(
                    responseCode = "200",
                    description = "게시글 상세 조회 성공",
                    content = @Content(
                            mediaType = "application/json",
                            examples = @ExampleObject(
                                    value = """
                        {
                            "success": true,
                            "message": "게시글 조회 성공",
                            "data": {
                                "boardId": 1,
                                "title": "게시글 제목",
                                "content": "게시글 상세 내용입니다.",
                                "authorId": 1,
                                "authorNickname": "사용자123",
                                "category": "FREE",
                                "viewCount": 151,
                                "likeCount": 25,
                                "commentCount": 8,
                                "createdAt": "2024-01-15 10:30:00",
                                "updatedAt": "2024-01-15 11:45:00",
                                "isAuthor": true,
                                "isLiked": false,
                                "attachments": [
                                    {
                                        "fileId": 1,
                                        "fileName": "image.jpg",
                                        "fileSize": 1024000,
                                        "downloadUrl": "/api/v1/files/download/1"
                                    }
                                ]
                            }
                        }
                        """
                            )
                    )
            ),
            @ApiResponse(
                    responseCode = "403",
                    description = "접근 권한 없음 - 비공개 게시글이거나 삭제된 게시글",
                    content = @Content(
                            mediaType = "application/json",
                            examples = @ExampleObject(
                                    value = """
                        {
                            "success": false,
                            "message": "게시글에 접근할 권한이 없습니다.",
                            "data": null
                        }
                        """
                            )
                    )
            ),
            @ApiResponse(
                    responseCode = "404",
                    description = "게시글 없음 - 존재하지 않는 게시글 ID",
                    content = @Content(
                            mediaType = "application/json",
                            examples = @ExampleObject(
                                    value = """
                        {
                            "success": false,
                            "message": "게시글을 찾을 수 없습니다.",
                            "data": null
                        }
                        """
                            )
                    )
            )
    })
    @GetMapping("/{boardId}")
    public ResponseEntity<CommonApiResponse<BoardDetailVO>> getBoardDetail(
            @Parameter(description = "게시글 ID", required = true, example = "1") @PathVariable Long boardId,
            @Parameter(hidden = true) @AuthenticationPrincipal UserDetails userDetails,
            @Parameter(hidden = true) @AuthenticationPrincipal Long memberId) {

        log.info("게시글 상세 조회: boardId={}", boardId);
        BoardDetailVO boardDetail = boardService.getBoardDetail(boardId, memberId);

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
    @Operation(
            summary = "게시글 생성",
            description = "새로운 게시글을 생성합니다. 파일 첨부도 가능합니다. 로그인된 사용자만 작성할 수 있습니다.",
            requestBody = @io.swagger.v3.oas.annotations.parameters.RequestBody(
                    description = "게시글 생성 정보 (multipart/form-data)",
                    required = true,
                    content = @Content(
                            mediaType = "multipart/form-data",
                            schema = @Schema(implementation = BoardCreateDTO.class)
                    )
            )
    )
    @ApiResponses({
            @ApiResponse(
                    responseCode = "201",
                    description = "게시글 생성 성공",
                    content = @Content(
                            mediaType = "application/json",
                            examples = @ExampleObject(
                                    value = """
                        {
                            "success": true,
                            "message": "게시글 생성 성공",
                            "data": {
                                "boardId": 1,
                                "title": "새로운 게시글",
                                "content": "게시글 내용입니다.",
                                "authorId": 1,
                                "authorNickname": "사용자123",
                                "category": "FREE",
                                "viewCount": 0,
                                "likeCount": 0,
                                "commentCount": 0,
                                "createdAt": "2024-01-15 10:30:00",
                                "isAuthor": true
                            }
                        }
                        """
                            )
                    )
            ),
            @ApiResponse(
                    responseCode = "400",
                    description = "잘못된 요청 - 필수 필드 누락 또는 유효하지 않은 데이터",
                    content = @Content(
                            mediaType = "application/json",
                            examples = @ExampleObject(
                                    value = """
                        {
                            "success": false,
                            "message": "게시글 제목은 필수입니다.",
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
                            "message": "로그인이 필요합니다.",
                            "data": null
                        }
                        """
                            )
                    )
            )
    })
    @PostMapping(consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public ResponseEntity<CommonApiResponse<BoardVO>> createBoard(
            @Valid @ModelAttribute BoardCreateDTO createDTO,
            @Parameter(hidden = true) @AuthenticationPrincipal Long memberId) {

        log.info("게시글 생성 요청: title={}, authorId={}", createDTO.getTitle(), createDTO.getAuthorId());

        // 현재 사용자 정보 설정
        createDTO.setAuthorId(memberId);

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
    @Operation(
            summary = "게시글 수정",
            description = "기존 게시글을 수정합니다. 게시글 작성자만 수정할 수 있습니다.",
            requestBody = @io.swagger.v3.oas.annotations.parameters.RequestBody(
                    description = "게시글 수정 정보 (multipart/form-data)",
                    required = true,
                    content = @Content(
                            mediaType = "multipart/form-data",
                            schema = @Schema(implementation = BoardUpdateDTO.class)
                    )
            )
    )
    @ApiResponses({
            @ApiResponse(
                    responseCode = "200",
                    description = "게시글 수정 성공",
                    content = @Content(
                            mediaType = "application/json",
                            examples = @ExampleObject(
                                    value = """
                        {
                            "success": true,
                            "message": "게시글 수정 성공",
                            "data": {
                                "boardId": 1,
                                "title": "수정된 게시글 제목",
                                "content": "수정된 게시글 내용입니다.",
                                "authorId": "user123",
                                "authorNickname": "사용자123",
                                "category": "FREE",
                                "viewCount": 150,
                                "likeCount": 25,
                                "commentCount": 8,
                                "createdAt": "2024-01-15 10:30:00",
                                "updatedAt": "2024-01-15 12:00:00",
                                "isAuthor": true
                            }
                        }
                        """
                            )
                    )
            ),
            @ApiResponse(
                    responseCode = "400",
                    description = "잘못된 요청 - 유효하지 않은 수정 데이터",
                    content = @Content(
                            mediaType = "application/json",
                            examples = @ExampleObject(
                                    value = """
                        {
                            "success": false,
                            "message": "게시글 제목은 100자 이하로 입력해주세요.",
                            "data": null
                        }
                        """
                            )
                    )
            ),
            @ApiResponse(
                    responseCode = "403",
                    description = "수정 권한 없음 - 게시글 작성자가 아님",
                    content = @Content(
                            mediaType = "application/json",
                            examples = @ExampleObject(
                                    value = """
                        {
                            "success": false,
                            "message": "게시글 수정 권한이 없습니다.",
                            "data": null
                        }
                        """
                            )
                    )
            ),
            @ApiResponse(
                    responseCode = "404",
                    description = "게시글 없음 - 존재하지 않거나 삭제된 게시글",
                    content = @Content(
                            mediaType = "application/json",
                            examples = @ExampleObject(
                                    value = """
                        {
                            "success": false,
                            "message": "게시글을 찾을 수 없습니다.",
                            "data": null
                        }
                        """
                            )
                    )
            )
    })
    @PutMapping(value = "/{boardId}", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public ResponseEntity<CommonApiResponse<BoardVO>> updateBoard(
            @Parameter(description = "게시글 ID", required = true, example = "1")
            @PathVariable Long boardId,
            @Valid @ModelAttribute BoardUpdateDTO updateDTO,
            @Parameter(hidden = true) @AuthenticationPrincipal Long memberId)  {

        log.info("게시글 수정 요청: boardId={}", boardId);

        // 게시글 ID 설정
        updateDTO.setBoardId(boardId);

        // 현재 사용자 정보 설정
        updateDTO.setEditorId(memberId);

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
    @Operation(
            summary = "게시글 삭제",
            description = "게시글을 삭제합니다. 게시글 작성자만 삭제할 수 있으며, 소프트 삭제로 처리됩니다."
    )
    @ApiResponses({
            @ApiResponse(
                    responseCode = "200",
                    description = "게시글 삭제 성공",
                    content = @Content(
                            mediaType = "application/json",
                            examples = @ExampleObject(
                                    value = """
                        {
                            "success": true,
                            "message": "게시글 삭제 성공"
                        }
                        """
                            )
                    )
            ),
            @ApiResponse(
                    responseCode = "403",
                    description = "삭제 권한 없음 - 게시글 작성자가 아님",
                    content = @Content(
                            mediaType = "application/json",
                            examples = @ExampleObject(
                                    value = """
                        {
                            "success": false,
                            "message": "게시글 삭제 권한이 없습니다."
                        }
                        """
                            )
                    )
            ),
            @ApiResponse(
                    responseCode = "404",
                    description = "게시글 없음 - 존재하지 않거나 이미 삭제된 게시글",
                    content = @Content(
                            mediaType = "application/json",
                            examples = @ExampleObject(
                                    value = """
                        {
                            "success": false,
                            "message": "게시글을 찾을 수 없습니다."
                        }
                        """
                            )
                    )
            )
    })
    @DeleteMapping("/{boardId}")
    public ResponseEntity<CommonApiResponse<Void>> deleteBoard(
            @Parameter(description = "게시글 ID", required = true, example = "1") @PathVariable Long boardId,
            @Parameter(hidden = true) @AuthenticationPrincipal Long memberId) {

        log.info("게시글 삭제 요청: boardId={}", boardId);

        boardService.deleteBoard(boardId, memberId);

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
    @Operation(
            summary = "인기 게시글 조회",
            description = "좋아요 수나 조회수를 기준으로 인기 게시글 목록을 조회합니다."
    )
    @ApiResponses({
            @ApiResponse(
                    responseCode = "200",
                    description = "인기 게시글 조회 성공",
                    content = @Content(
                            mediaType = "application/json",
                            examples = @ExampleObject(
                                    value = """
                        {
                            "success": true,
                            "message": "인기 게시글 조회 성공",
                            "data": [
                                {
                                    "boardId": 1,
                                    "title": "가장 인기 있는 게시글",
                                    "content": "많은 사람들이 좋아하는 게시글입니다.",
                                    "authorId": "user123",
                                    "authorNickname": "사용자123",
                                    "viewCount": 2500,
                                    "likeCount": 150,
                                    "commentCount": 45,
                                    "createdAt": "2024-01-10 09:00:00",
                                    "isAuthor": false
                                },
                                {
                                    "boardId": 2,
                                    "title": "두 번째 인기 게시글",
                                    "content": "역시 인기가 많은 게시글입니다.",
                                    "authorId": "user456",
                                    "authorNickname": "사용자456",
                                    "viewCount": 2200,
                                    "likeCount": 120,
                                    "commentCount": 38,
                                    "createdAt": "2024-01-12 14:30:00",
                                    "isAuthor": false
                                }
                            ]
                        }
                        """
                            )
                    )
            )
    })
    @GetMapping("/popular")
    public ResponseEntity<CommonApiResponse<List<BoardVO>>> getPopularBoards(
            @Parameter(description = "조회할 게시글 수", example = "10") @RequestParam(defaultValue = "10") int limit) {

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
    @Operation(
            summary = "최근 게시글 조회",
            description = "가장 최근에 작성된 게시글 목록을 조회합니다."
    )
    @ApiResponses({
            @ApiResponse(
                    responseCode = "200",
                    description = "최근 게시글 조회 성공",
                    content = @Content(
                            mediaType = "application/json",
                            examples = @ExampleObject(
                                    value = """
                        {
                            "success": true,
                            "message": "최근 게시글 조회 성공",
                            "data": [
                                {
                                    "boardId": 10,
                                    "title": "방금 작성한 게시글",
                                    "content": "최신 게시글입니다.",
                                    "authorId": "user789",
                                    "authorNickname": "사용자789",
                                    "viewCount": 5,
                                    "likeCount": 0,
                                    "commentCount": 0,
                                    "createdAt": "2024-01-15 15:30:00",
                                    "isAuthor": false
                                },
                                {
                                    "boardId": 9,
                                    "title": "어제 작성한 게시글",
                                    "content": "어제 올린 게시글입니다.",
                                    "authorId": "user456",
                                    "authorNickname": "사용자456",
                                    "viewCount": 25,
                                    "likeCount": 3,
                                    "commentCount": 2,
                                    "createdAt": "2024-01-14 20:15:00",
                                    "isAuthor": false
                                }
                            ]
                        }
                        """
                            )
                    )
            )
    })
    @GetMapping("/recent_")
    public ResponseEntity<CommonApiResponse<List<BoardVO>>> getRecentBoards_(
            @Parameter(description = "조회할 게시글 수", example = "10") @RequestParam(defaultValue = "10") int limit) {

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
     * 최근 게시글 조회
     */
    @Operation(
            summary = "최근 게시글 조회",
            description = "가장 최근에 작성된 게시글 목록을 페이징하여 조회합니다."
    )
    @ApiResponses({
            @ApiResponse(
                    responseCode = "200",
                    description = "최근 게시글 조회 성공",
                    content = @Content(
                            mediaType = "application/json",
                            examples = @ExampleObject(
                                    value = """
                    {
                        "success": true,
                        "message": "최근 게시글 조회 성공",
                        "data": {
                            "boards": [
                                {
                                    "boardId": 10,
                                    "title": "방금 작성한 게시글",
                                    "content": "최신 게시글입니다.",
                                    "authorId": 6,
                                    "authorNickname": "사용자789",
                                    "viewCount": 5,
                                    "likeCount": 0,
                                    "commentCount": 0,
                                    "createdAt": "2024-01-15 15:30:00"
                                }
                            ],
                            "pageInfo": {
                                "currentPage": 0,
                                "pageSize": 20,
                                "totalElements": 150,
                                "totalPages": 8,
                                "hasNext": true,
                                "hasPrevious": false,
                                "isFirst": true,
                                "isLast": false
                            }
                        }
                    }
                    """
                            )
                    )
            )
    })
    @GetMapping("/recent")
    public ResponseEntity<CommonApiResponse<BoardListVO>> getRecentBoards(
            @Parameter(description = "페이징 정보") @ModelAttribute PageRequestDTO pageRequestDTO) {

        log.info("최근 게시글 조회: page={}, size={}",
                pageRequestDTO.getPage(), pageRequestDTO.getSize());

        BoardListVO recentBoards = boardService.getRecentBoardList(pageRequestDTO);

        return ResponseEntity.ok(
                CommonApiResponse.<BoardListVO>builder()
                        .success(true)
                        .message("최근 게시글 조회 성공")
                        .data(recentBoards)
                        .build()
        );
    }

    /**
     * 내가 작성한 게시글 조회
     */
    @Operation(
            summary = "내 게시글 조회",
            description = "현재 로그인한 사용자가 작성한 게시글 목록을 조회합니다."
    )
    @ApiResponses({
            @ApiResponse(
                    responseCode = "200",
                    description = "내 게시글 조회 성공",
                    content = @Content(
                            mediaType = "application/json",
                            examples = @ExampleObject(
                                    value = """
                        {
                            "success": true,
                            "message": "내 게시글 조회 성공",
                            "data": {
                                "boards": [
                                    {
                                        "boardId": 1,
                                        "title": "내가 작성한 첫 번째 게시글",
                                        "content": "내가 쓴 게시글입니다.",
                                        "authorId": "user123",
                                        "authorNickname": "사용자123",
                                        "viewCount": 150,
                                        "likeCount": 25,
                                        "commentCount": 8,
                                        "createdAt": "2024-01-15 10:30:00",
                                        "isAuthor": true
                                    }
                                ],
                                "totalElements": 15,
                                "totalPages": 2,
                                "currentPage": 1,
                                "pageSize": 10,
                                "hasNext": true,
                                "hasPrevious": false
                            }
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
            )
    })
    @GetMapping("/my")
    public ResponseEntity<CommonApiResponse<BoardListVO>> getMyBoards(
            @Parameter(description = "페이징 정보") @ModelAttribute PageRequestDTO pageRequestDTO,
            @Parameter(hidden = true) @AuthenticationPrincipal Long memberId) {

        log.info("내 게시글 조회: userId={}", memberId);

        if (memberId == null) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body(
                    CommonApiResponse.<BoardListVO>builder()
                            .success(false)
                            .message("로그인이 필요합니다")
                            .build()
            );
        }

        BoardListVO myBoards = boardService.getUserBoards(memberId, pageRequestDTO);

        return ResponseEntity.ok(
                CommonApiResponse.<BoardListVO>builder()
                        .success(true)
                        .message("내 게시글 조회 성공")
                        .data(myBoards)
                        .build()
        );
    }
}