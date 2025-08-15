package com.sp.community.controller;

import com.sp.community.model.dto.CommentCreateDTO;
import com.sp.community.model.dto.CommentUpdateDTO;
import com.sp.community.model.dto.PageRequestDTO;
import com.sp.community.model.response.CommonApiResponse;
import com.sp.community.model.dto.CommentVO;
import com.sp.community.service.CommentService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

/**
 * 댓글 관련 API Controller
 */
@Tag(name = "Comment", description = "댓글 관리 API")
@Slf4j
@RestController
@RequestMapping("/api/v1/comments")
@RequiredArgsConstructor
public class CommentController {

    private final CommentService commentService;

    /**
     * 댓글 생성
     */
    @Operation(summary = "댓글 생성", description = "새로운 댓글을 생성합니다.")
    @ApiResponses({
            @ApiResponse(responseCode = "201", description = "생성 성공"),
            @ApiResponse(responseCode = "400", description = "잘못된 요청"),
            @ApiResponse(responseCode = "401", description = "인증 필요"),
            @ApiResponse(responseCode = "404", description = "게시글 없음")
    })
    @PostMapping
    public ResponseEntity<CommonApiResponse<CommentVO>> createComment(
            @Parameter(description = "댓글 생성 정보") @Valid @RequestBody CommentCreateDTO createDTO,
            @AuthenticationPrincipal Long memberId) {

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
    @Operation(summary = "댓글 수정", description = "댓글을 수정합니다.")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "수정 성공"),
            @ApiResponse(responseCode = "400", description = "잘못된 요청"),
            @ApiResponse(responseCode = "401", description = "인증 필요"),
            @ApiResponse(responseCode = "403", description = "수정 권한 없음"),
            @ApiResponse(responseCode = "404", description = "댓글 없음")
    })
    @PutMapping("/{commentId}")
    public ResponseEntity<CommonApiResponse<CommentVO>> updateComment(
            @Parameter(description = "댓글 ID") @PathVariable Long commentId,
            @Parameter(description = "댓글 수정 정보") @Valid @RequestBody CommentUpdateDTO updateDTO,
            @AuthenticationPrincipal Long memberId) {

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
        updateDTO.setAuthorId(userId);

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
    @Operation(summary = "댓글 삭제", description = "댓글을 삭제합니다.")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "삭제 성공"),
            @ApiResponse(responseCode = "401", description = "인증 필요"),
            @ApiResponse(responseCode = "403", description = "삭제 권한 없음"),
            @ApiResponse(responseCode = "404", description = "댓글 없음")
    })
    @DeleteMapping("/{commentId}")
    public ResponseEntity<CommonApiResponse<Void>> deleteComment(
            @Parameter(description = "댓글 ID") @PathVariable Long commentId,
            @AuthenticationPrincipal Long memberId) {

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
    @Operation(summary = "게시글 댓글 목록", description = "특정 게시글의 댓글 목록을 조회합니다.")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "조회 성공"),
            @ApiResponse(responseCode = "404", description = "게시글 없음")
    })
    @GetMapping("/board/{boardId}")
    public ResponseEntity<CommonApiResponse<List<CommentVO>>> getBoardComments(
            @Parameter(description = "게시글 ID") @PathVariable Long boardId,
            @Parameter(description = "페이징 정보") @ModelAttribute PageRequestDTO pageRequestDTO,
            @AuthenticationPrincipal Long memberId) {

        String currentUserId = memberId+"";
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
    @Operation(summary = "사용자 댓글 목록", description = "특정 사용자가 작성한 댓글 목록을 조회합니다.")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "조회 성공")
    })
    @GetMapping("/user/{authorId}")
    public ResponseEntity<CommonApiResponse<List<CommentVO>>> getUserComments(
            @Parameter(description = "작성자 ID") @PathVariable String authorId,
            @Parameter(description = "페이징 정보") @ModelAttribute PageRequestDTO pageRequestDTO,
            @AuthenticationPrincipal Long memberId) {

        String currentUserId = memberId+"";
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
    @Operation(summary = "내 댓글 목록", description = "현재 사용자가 작성한 댓글 목록을 조회합니다.")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "조회 성공"),
            @ApiResponse(responseCode = "401", description = "인증 필요")
    })
    @GetMapping("/my")
    public ResponseEntity<CommonApiResponse<List<CommentVO>>> getMyComments(
            @Parameter(description = "페이징 정보") @ModelAttribute PageRequestDTO pageRequestDTO,
            @AuthenticationPrincipal Long memberId) {

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
    @Operation(summary = "댓글 좋아요 토글", description = "댓글 좋아요를 추가하거나 취소합니다.")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "성공"),
            @ApiResponse(responseCode = "400", description = "잘못된 요청"),
            @ApiResponse(responseCode = "401", description = "인증 필요"),
            @ApiResponse(responseCode = "403", description = "권한 없음 (자신의 댓글)"),
            @ApiResponse(responseCode = "404", description = "댓글 없음")
    })
    @PostMapping("/{commentId}/like")
    public ResponseEntity<CommonApiResponse<CommentLikeResponse>> toggleCommentLike(
            @Parameter(description = "댓글 ID") @PathVariable Long commentId,
            @AuthenticationPrincipal Long memberId) {

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
    @Operation(summary = "댓글 좋아요 상태", description = "현재 사용자의 댓글 좋아요 상태를 확인합니다.")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "조회 성공"),
            @ApiResponse(responseCode = "404", description = "댓글 없음")
    })
    @GetMapping("/{commentId}/like/status")
    public ResponseEntity<CommonApiResponse<CommentLikeStatusResponse>> getCommentLikeStatus(
            @Parameter(description = "댓글 ID") @PathVariable Long commentId,
            @AuthenticationPrincipal Long memberId) {

        String userId = memberId+"";

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
    @Operation(summary = "게시글 댓글 수", description = "특정 게시글의 댓글 수를 조회합니다.")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "조회 성공")
    })
    @GetMapping("/board/{boardId}/count")
    public ResponseEntity<CommonApiResponse<Map<String, Object>>> getBoardCommentCount(
            @Parameter(description = "게시글 ID") @PathVariable Long boardId) {

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
    @lombok.Data
    @lombok.Builder
    @lombok.NoArgsConstructor
    @lombok.AllArgsConstructor
    public static class CommentLikeResponse {
        private Long commentId;
        private Boolean isLiked;
        private Long likeCount;
    }

    /**
     * 댓글 좋아요 상태 응답 DTO
     */
    @lombok.Data
    @lombok.Builder
    @lombok.NoArgsConstructor
    @lombok.AllArgsConstructor
    public static class CommentLikeStatusResponse {
        private Long commentId;
        private Boolean isLiked;
        private Long likeCount;
        private Boolean canLike;
    }
}