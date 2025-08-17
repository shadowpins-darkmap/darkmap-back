package com.sp.community.controller;

import com.sp.community.model.dto.CommentCreateDTO;
import com.sp.community.model.dto.CommentUpdateDTO;
import com.sp.community.model.dto.PageRequestDTO;
import com.sp.community.model.response.CommonApiResponse;
import com.sp.community.model.vo.CommentVO;
import com.sp.community.service.CommentService;
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
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

/**
 * 댓글 관련 API Controller
 */
@Tag(name = "Comment", description = "댓글 관리 API - 게시글 댓글의 생성, 조회, 수정, 삭제 및 좋아요 기능을 제공합니다.")
@Slf4j
@RestController
@RequestMapping("/api/v1/comments")
@RequiredArgsConstructor
public class CommentController {

    private final CommentService commentService;

    /**
     * 댓글 생성
     */
    @Operation(
            summary = "댓글 생성",
            description = "새로운 댓글을 생성합니다. 로그인된 사용자만 댓글을 작성할 수 있습니다.",
            requestBody = @io.swagger.v3.oas.annotations.parameters.RequestBody(
                    description = "댓글 생성 정보",
                    required = true,
                    content = @Content(
                            mediaType = "application/json",
                            schema = @Schema(implementation = CommentCreateDTO.class),
                            examples = @ExampleObject(
                                    name = "댓글 생성 예시",
                                    value = """
                    {
                        "boardId": 1,
                        "content": "좋은 게시글이네요! 감사합니다."
                    }
                    """
                            )
                    )
            )
    )
    @ApiResponses({
            @ApiResponse(
                    responseCode = "201",
                    description = "댓글 생성 성공",
                    content = @Content(
                            mediaType = "application/json",
                            examples = @ExampleObject(
                                    value = """
                        {
                            "success": true,
                            "message": "댓글 생성 성공",
                            "data": {
                                "commentId": 1,
                                "boardId": 1,
                                "content": "좋은 게시글이네요! 감사합니다.",
                                "authorId": "user123",
                                "authorNickname": "사용자123",
                                "likeCount": 0,
                                "createdAt": "2024-01-15 10:30:00",
                                "isAuthor": true,
                                "isLiked": false
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
                            "message": "댓글 내용은 필수입니다.",
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
    @PostMapping
    public ResponseEntity<CommonApiResponse<CommentVO>> createComment(
            @Parameter(description = "댓글 생성 정보", required = true) @Valid @RequestBody CommentCreateDTO createDTO,
            @Parameter(hidden = true) @AuthenticationPrincipal Long memberId) {

        if (memberId == null) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body(
                    CommonApiResponse.<CommentVO>builder()
                            .success(false)
                            .message("로그인이 필요합니다")
                            .build()
            );
        }

        String userId = memberId+"";
        String userNickname = userId; // 임시로 ID를 닉네임으로 사용

        log.info("댓글 생성 요청: boardId={}, authorId={}", createDTO.getBoardId(), userId);

        CommentVO createdComment = commentService.createComment(createDTO, userId);

        return ResponseEntity.status(HttpStatus.CREATED).body(
                CommonApiResponse.<CommentVO>builder()
                        .success(true)
                        .message("댓글 생성 성공")
                        .data(createdComment)
                        .build()
        );
    }

    /**
     * 댓글 수정
     */
    @Operation(
            summary = "댓글 수정",
            description = "기존 댓글의 내용을 수정합니다. 댓글 작성자만 수정할 수 있습니다.",
            requestBody = @io.swagger.v3.oas.annotations.parameters.RequestBody(
                    description = "댓글 수정 정보",
                    required = true,
                    content = @Content(
                            mediaType = "application/json",
                            schema = @Schema(implementation = CommentUpdateDTO.class),
                            examples = @ExampleObject(
                                    name = "댓글 수정 예시",
                                    value = """
                    {
                        "content": "수정된 댓글 내용입니다."
                    }
                    """
                            )
                    )
            )
    )
    @ApiResponses({
            @ApiResponse(
                    responseCode = "200",
                    description = "댓글 수정 성공",
                    content = @Content(
                            mediaType = "application/json",
                            examples = @ExampleObject(
                                    value = """
                        {
                            "success": true,
                            "message": "댓글 수정 성공",
                            "data": {
                                "commentId": 1,
                                "boardId": 1,
                                "content": "수정된 댓글 내용입니다.",
                                "authorId": "user123",
                                "authorNickname": "사용자123",
                                "likeCount": 3,
                                "createdAt": "2024-01-15 10:30:00",
                                "updatedAt": "2024-01-15 11:45:00",
                                "isAuthor": true,
                                "isLiked": false
                            }
                        }
                        """
                            )
                    )
            ),
            @ApiResponse(
                    responseCode = "400",
                    description = "잘못된 요청 - 유효하지 않은 댓글 내용",
                    content = @Content(
                            mediaType = "application/json",
                            examples = @ExampleObject(
                                    value = """
                        {
                            "success": false,
                            "message": "댓글은 1,000자 이하로 입력해주세요.",
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
                    responseCode = "403",
                    description = "수정 권한 없음 - 댓글 작성자가 아님",
                    content = @Content(
                            mediaType = "application/json",
                            examples = @ExampleObject(
                                    value = """
                        {
                            "success": false,
                            "message": "댓글 수정 권한이 없습니다.",
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
                            "message": "댓글을 찾을 수 없습니다.",
                            "data": null
                        }
                        """
                            )
                    )
            )
    })
    @PutMapping("/{commentId}")
    public ResponseEntity<CommonApiResponse<CommentVO>> updateComment(
            @Parameter(description = "댓글 ID", required = true, example = "1") @PathVariable Long commentId,
            @Parameter(description = "댓글 수정 정보", required = true) @Valid @RequestBody CommentUpdateDTO updateDTO,
            @Parameter(hidden = true) @AuthenticationPrincipal Long memberId) {

        if (memberId == null) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body(
                    CommonApiResponse.<CommentVO>builder()
                            .success(false)
                            .message("로그인이 필요합니다")
                            .build()
            );
        }

        String userId = memberId+"";
        updateDTO.setCommentId(commentId);
        updateDTO.setAuthorId(userId); // 권한 확인을 위해 추가

        log.info("댓글 수정 요청: commentId={}, editorId={}", commentId, userId);

        CommentVO updatedComment = commentService.updateComment(updateDTO);

        return ResponseEntity.ok(
                CommonApiResponse.<CommentVO>builder()
                        .success(true)
                        .message("댓글 수정 성공")
                        .data(updatedComment)
                        .build()
        );
    }

    /**
     * 댓글 삭제
     */
    @Operation(
            summary = "댓글 삭제",
            description = "댓글을 삭제합니다. 댓글 작성자만 삭제할 수 있으며, 소프트 삭제로 처리됩니다."
    )
    @ApiResponses({
            @ApiResponse(
                    responseCode = "200",
                    description = "댓글 삭제 성공",
                    content = @Content(
                            mediaType = "application/json",
                            examples = @ExampleObject(
                                    value = """
                        {
                            "success": true,
                            "message": "댓글 삭제 성공"
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
                            "message": "로그인이 필요합니다"
                        }
                        """
                            )
                    )
            ),
            @ApiResponse(
                    responseCode = "403",
                    description = "삭제 권한 없음 - 댓글 작성자가 아님",
                    content = @Content(
                            mediaType = "application/json",
                            examples = @ExampleObject(
                                    value = """
                        {
                            "success": false,
                            "message": "댓글 삭제 권한이 없습니다."
                        }
                        """
                            )
                    )
            ),
            @ApiResponse(
                    responseCode = "404",
                    description = "댓글 없음 - 존재하지 않거나 이미 삭제된 댓글",
                    content = @Content(
                            mediaType = "application/json",
                            examples = @ExampleObject(
                                    value = """
                        {
                            "success": false,
                            "message": "댓글을 찾을 수 없습니다."
                        }
                        """
                            )
                    )
            )
    })
    @DeleteMapping("/{commentId}")
    public ResponseEntity<CommonApiResponse<Void>> deleteComment(
            @Parameter(description = "댓글 ID", required = true, example = "1") @PathVariable Long commentId,
            @Parameter(hidden = true) @AuthenticationPrincipal Long memberId) {

        if (memberId == null) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body(
                    CommonApiResponse.<Void>builder()
                            .success(false)
                            .message("로그인이 필요합니다")
                            .build()
            );
        }

        String userId = memberId+"";
        log.info("댓글 삭제 요청: commentId={}, userId={}", commentId, userId);

        commentService.deleteComment(commentId, userId);

        return ResponseEntity.ok(
                CommonApiResponse.<Void>builder()
                        .success(true)
                        .message("댓글 삭제 성공")
                        .build()
        );
    }

    /**
     * 특정 게시글의 댓글 목록 조회
     */
    @Operation(
            summary = "게시글 댓글 목록 조회",
            description = "특정 게시글의 댓글 목록을 페이징으로 조회합니다. 생성일자 순으로 정렬됩니다."
    )
    @ApiResponses({
            @ApiResponse(
                    responseCode = "200",
                    description = "댓글 목록 조회 성공",
                    content = @Content(
                            mediaType = "application/json",
                            examples = @ExampleObject(
                                    value = """
                        {
                            "success": true,
                            "message": "댓글 목록 조회 성공",
                            "data": [
                                {
                                    "commentId": 1,
                                    "boardId": 1,
                                    "content": "첫 번째 댓글입니다.",
                                    "authorId": "user123",
                                    "authorNickname": "사용자123",
                                    "likeCount": 5,
                                    "createdAt": "2024-01-15 10:30:00",
                                    "isAuthor": false,
                                    "isLiked": true,
                                    "status": "ACTIVE"
                                }
                            ]
                        }
                        """
                            )
                    )
            ),
            @ApiResponse(
                    responseCode = "404",
                    description = "게시글 없음 - 존재하지 않는 게시글",
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
    @GetMapping("/board/{boardId}")
    public ResponseEntity<CommonApiResponse<List<CommentVO>>> getBoardComments(
            @Parameter(description = "게시글 ID", required = true, example = "1") @PathVariable Long boardId,
            @Parameter(description = "페이징 정보") @ModelAttribute PageRequestDTO pageRequestDTO,
            @Parameter(hidden = true) @AuthenticationPrincipal Long memberId) {

        String currentUserId = memberId != null ? memberId+"" : null;
        log.debug("게시글 댓글 목록 조회: boardId={}", boardId);

        List<CommentVO> comments = commentService.getBoardComments(boardId, currentUserId, pageRequestDTO);

        return ResponseEntity.ok(
                CommonApiResponse.<List<CommentVO>>builder()
                        .success(true)
                        .message("댓글 목록 조회 성공")
                        .data(comments)
                        .build()
        );
    }

    /**
     * 사용자별 댓글 목록 조회
     */
    @Operation(
            summary = "사용자 댓글 목록 조회",
            description = "특정 사용자가 작성한 댓글 목록을 조회합니다."
    )
    @ApiResponses({
            @ApiResponse(
                    responseCode = "200",
                    description = "사용자 댓글 목록 조회 성공",
                    content = @Content(
                            mediaType = "application/json",
                            examples = @ExampleObject(
                                    value = """
                        {
                            "success": true,
                            "message": "사용자 댓글 목록 조회 성공",
                            "data": [
                                {
                                    "commentId": 1,
                                    "boardId": 1,
                                    "content": "사용자가 작성한 댓글입니다.",
                                    "authorId": "user123",
                                    "authorNickname": "사용자123",
                                    "likeCount": 2,
                                    "createdAt": "2024-01-15 10:30:00",
                                    "isAuthor": true,
                                    "isLiked": false
                                }
                            ]
                        }
                        """
                            )
                    )
            )
    })
    @GetMapping("/user/{authorId}")
    public ResponseEntity<CommonApiResponse<List<CommentVO>>> getUserComments(
            @Parameter(description = "작성자 ID", required = true, example = "user123") @PathVariable String authorId,
            @Parameter(description = "페이징 정보") @ModelAttribute PageRequestDTO pageRequestDTO,
            @Parameter(hidden = true) @AuthenticationPrincipal Long memberId) {

        String currentUserId = memberId != null ? memberId+"" : null;
        log.debug("사용자 댓글 목록 조회: authorId={}", authorId);

        List<CommentVO> comments = commentService.getUserComments(authorId, currentUserId, pageRequestDTO);

        return ResponseEntity.ok(
                CommonApiResponse.<List<CommentVO>>builder()
                        .success(true)
                        .message("사용자 댓글 목록 조회 성공")
                        .data(comments)
                        .build()
        );
    }

    /**
     * 내가 작성한 댓글 목록 조회
     */
    @Operation(
            summary = "내 댓글 목록 조회",
            description = "현재 로그인한 사용자가 작성한 댓글 목록을 조회합니다."
    )
    @ApiResponses({
            @ApiResponse(
                    responseCode = "200",
                    description = "내 댓글 목록 조회 성공",
                    content = @Content(
                            mediaType = "application/json",
                            examples = @ExampleObject(
                                    value = """
                        {
                            "success": true,
                            "message": "내 댓글 목록 조회 성공",
                            "data": [
                                {
                                    "commentId": 1,
                                    "boardId": 1,
                                    "content": "내가 작성한 댓글입니다.",
                                    "authorId": "user123",
                                    "authorNickname": "사용자123",
                                    "likeCount": 3,
                                    "createdAt": "2024-01-15 10:30:00",
                                    "isAuthor": true,
                                    "isLiked": false
                                }
                            ]
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
    public ResponseEntity<CommonApiResponse<List<CommentVO>>> getMyComments(
            @Parameter(description = "페이징 정보") @ModelAttribute PageRequestDTO pageRequestDTO,
            @Parameter(hidden = true) @AuthenticationPrincipal Long memberId) {

        if (memberId == null) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body(
                    CommonApiResponse.<List<CommentVO>>builder()
                            .success(false)
                            .message("로그인이 필요합니다")
                            .build()
            );
        }

        String userId = memberId+"";
        log.debug("내 댓글 목록 조회: userId={}", userId);

        List<CommentVO> comments = commentService.getUserComments(userId, userId, pageRequestDTO);

        return ResponseEntity.ok(
                CommonApiResponse.<List<CommentVO>>builder()
                        .success(true)
                        .message("내 댓글 목록 조회 성공")
                        .data(comments)
                        .build()
        );
    }

    /**
     * 댓글 좋아요 토글
     */
    @Operation(
            summary = "댓글 좋아요 토글",
            description = "댓글에 좋아요를 추가하거나 취소합니다. 자신의 댓글에는 좋아요할 수 없습니다."
    )
    @ApiResponses({
            @ApiResponse(
                    responseCode = "200",
                    description = "좋아요 토글 성공",
                    content = @Content(
                            mediaType = "application/json",
                            examples = {
                                    @ExampleObject(
                                            name = "좋아요 추가",
                                            value = """
                            {
                                "success": true,
                                "message": "댓글 좋아요를 추가했습니다",
                                "data": {
                                    "commentId": 1,
                                    "isLiked": true,
                                    "likeCount": 6
                                }
                            }
                            """
                                    ),
                                    @ExampleObject(
                                            name = "좋아요 취소",
                                            value = """
                            {
                                "success": true,
                                "message": "댓글 좋아요를 취소했습니다",
                                "data": {
                                    "commentId": 1,
                                    "isLiked": false,
                                    "likeCount": 5
                                }
                            }
                            """
                                    )
                            }
                    )
            ),
            @ApiResponse(
                    responseCode = "400",
                    description = "잘못된 요청 - 유효하지 않은 댓글 ID",
                    content = @Content(
                            mediaType = "application/json",
                            examples = @ExampleObject(
                                    value = """
                        {
                            "success": false,
                            "message": "댓글 ID는 필수입니다.",
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
                    responseCode = "403",
                    description = "권한 없음 - 자신의 댓글에 좋아요 시도",
                    content = @Content(
                            mediaType = "application/json",
                            examples = @ExampleObject(
                                    value = """
                        {
                            "success": false,
                            "message": "자신의 댓글에는 좋아요할 수 없습니다.",
                            "data": null
                        }
                        """
                            )
                    )
            ),
            @ApiResponse(
                    responseCode = "404",
                    description = "댓글 없음 - 존재하지 않는 댓글",
                    content = @Content(
                            mediaType = "application/json",
                            examples = @ExampleObject(
                                    value = """
                        {
                            "success": false,
                            "message": "댓글을 찾을 수 없습니다.",
                            "data": null
                        }
                        """
                            )
                    )
            )
    })
    @PostMapping("/{commentId}/like")
    public ResponseEntity<CommonApiResponse<CommentLikeResponse>> toggleCommentLike(
            @Parameter(description = "댓글 ID", required = true, example = "1") @PathVariable Long commentId,
            @Parameter(hidden = true) @AuthenticationPrincipal Long memberId) {

        if (memberId == null) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body(
                    CommonApiResponse.<CommentLikeResponse>builder()
                            .success(false)
                            .message("로그인이 필요합니다")
                            .build()
            );
        }

        String userId = memberId+"";
        String userNickname = userId; // 임시로 ID를 닉네임으로 사용

        log.info("댓글 좋아요 토글 요청: commentId={}, userId={}", commentId, userId);

        boolean isLiked = commentService.toggleCommentLike(commentId, userId, userNickname);
        Long likeCount = commentService.getCommentLikeCount(commentId);

        CommentLikeResponse response = CommentLikeResponse.builder()
                .commentId(commentId)
                .isLiked(isLiked)
                .likeCount(likeCount)
                .build();

        String message = isLiked ? "댓글 좋아요를 추가했습니다" : "댓글 좋아요를 취소했습니다";

        return ResponseEntity.ok(
                CommonApiResponse.<CommentLikeResponse>builder()
                        .success(true)
                        .message(message)
                        .data(response)
                        .build()
        );
    }

    /**
     * 댓글 좋아요 상태 확인
     */
    @Operation(
            summary = "댓글 좋아요 상태 조회",
            description = "현재 사용자의 댓글 좋아요 상태와 전체 좋아요 수를 확인합니다."
    )
    @ApiResponses({
            @ApiResponse(
                    responseCode = "200",
                    description = "좋아요 상태 조회 성공",
                    content = @Content(
                            mediaType = "application/json",
                            examples = @ExampleObject(
                                    value = """
                        {
                            "success": true,
                            "message": "댓글 좋아요 상태 조회 성공",
                            "data": {
                                "commentId": 1,
                                "isLiked": true,
                                "likeCount": 5,
                                "canLike": true
                            }
                        }
                        """
                            )
                    )
            ),
            @ApiResponse(
                    responseCode = "404",
                    description = "댓글 없음 - 존재하지 않는 댓글",
                    content = @Content(
                            mediaType = "application/json",
                            examples = @ExampleObject(
                                    value = """
                        {
                            "success": false,
                            "message": "댓글을 찾을 수 없습니다.",
                            "data": null
                        }
                        """
                            )
                    )
            )
    })
    @GetMapping("/{commentId}/like/status")
    public ResponseEntity<CommonApiResponse<CommentLikeStatusResponse>> getCommentLikeStatus(
            @Parameter(description = "댓글 ID", required = true, example = "1") @PathVariable Long commentId,
            @Parameter(hidden = true) @AuthenticationPrincipal Long memberId) {

        String userId = memberId != null ? memberId+"" : null;

        boolean isLiked = commentService.hasUserLikedComment(commentId, userId);
        Long likeCount = commentService.getCommentLikeCount(commentId);

        CommentLikeStatusResponse response = CommentLikeStatusResponse.builder()
                .commentId(commentId)
                .isLiked(isLiked)
                .likeCount(likeCount)
                .canLike(userId != null)
                .build();

        return ResponseEntity.ok(
                CommonApiResponse.<CommentLikeStatusResponse>builder()
                        .success(true)
                        .message("댓글 좋아요 상태 조회 성공")
                        .data(response)
                        .build()
        );
    }

    /**
     * 게시글별 댓글 수 조회
     */
    @Operation(
            summary = "게시글 댓글 수 조회",
            description = "특정 게시글의 전체 댓글 수를 조회합니다."
    )
    @ApiResponses({
            @ApiResponse(
                    responseCode = "200",
                    description = "댓글 수 조회 성공",
                    content = @Content(
                            mediaType = "application/json",
                            examples = @ExampleObject(
                                    value = """
                        {
                            "success": true,
                            "message": "댓글 수 조회 성공",
                            "data": {
                                "boardId": 1,
                                "commentCount": 15
                            }
                        }
                        """
                            )
                    )
            )
    })
    @GetMapping("/board/{boardId}/count")
    public ResponseEntity<CommonApiResponse<Map<String, Object>>> getBoardCommentCount(
            @Parameter(description = "게시글 ID", required = true, example = "1") @PathVariable Long boardId) {

        Long commentCount = commentService.getBoardCommentCount(boardId);

        Map<String, Object> result = Map.of(
                "boardId", boardId,
                "commentCount", commentCount
        );

        return ResponseEntity.ok(
                CommonApiResponse.<Map<String, Object>>builder()
                        .success(true)
                        .message("댓글 수 조회 성공")
                        .data(result)
                        .build()
        );
    }

    // ============ Response DTOs ============

    /**
     * 댓글 좋아요 응답 DTO
     */
    @Schema(description = "댓글 좋아요 토글 응답")
    @lombok.Data
    @lombok.Builder
    @lombok.NoArgsConstructor
    @lombok.AllArgsConstructor
    public static class CommentLikeResponse {

        @Schema(description = "댓글 ID", example = "1")
        private Long commentId;

        @Schema(description = "좋아요 여부", example = "true")
        private Boolean isLiked;

        @Schema(description = "총 좋아요 수", example = "5")
        private Long likeCount;
    }

    /**
     * 댓글 좋아요 상태 응답 DTO
     */
    @Schema(description = "댓글 좋아요 상태 응답")
    @lombok.Data
    @lombok.Builder
    @lombok.NoArgsConstructor
    @lombok.AllArgsConstructor
    public static class CommentLikeStatusResponse {

        @Schema(description = "댓글 ID", example = "1")
        private Long commentId;

        @Schema(description = "현재 사용자의 좋아요 여부", example = "true")
        private Boolean isLiked;

        @Schema(description = "총 좋아요 수", example = "5")
        private Long likeCount;

        @Schema(description = "좋아요 가능 여부 (로그인 상태)", example = "true")
        private Boolean canLike;
    }
}