package com.sp.community.controller;

import com.sp.community.model.dto.PageRequestDTO;
import com.sp.community.model.response.CommonApiResponse;
import com.sp.community.model.vo.BoardVO;
import com.sp.community.service.BoardLikeService;
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
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

/**
 * 게시글 좋아요 관련 API Controller
 */
@Tag(name = "BoardLike", description = "게시글 좋아요 관리 API")
@Slf4j
@RestController
@RequestMapping("/api/v1/boards")
@RequiredArgsConstructor
public class BoardLikeController {

    private final BoardLikeService boardLikeService;

    /**
     * 게시글 좋아요 토글 (추가/취소)
     */
    @Operation(summary = "게시글 좋아요 토글", description = "게시글 좋아요를 추가하거나 취소합니다.")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "성공"),
            @ApiResponse(responseCode = "400", description = "잘못된 요청"),
            @ApiResponse(responseCode = "401", description = "인증 필요"),
            @ApiResponse(responseCode = "403", description = "권한 없음 (자신의 게시글)"),
            @ApiResponse(responseCode = "404", description = "게시글 없음")
    })
    @PostMapping("/{boardId}/like")
    public ResponseEntity<CommonApiResponse<LikeToggleResponse>> toggleLike(
            @Parameter(description = "게시글 ID") @PathVariable Long boardId,
            @AuthenticationPrincipal Long memberId) {

        if (memberId == null) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body(
                    CommonApiResponse.<LikeToggleResponse>builder()
                            .success(false)
                            .message("로그인이 필요합니다")
                            .build()
            );
        }

        String userId = memberId+"";
        String userNickname = userId;

        log.info("좋아요 토글 요청: boardId={}, userId={}", boardId, userId);

        boolean isLiked = boardLikeService.toggleLike(boardId, userId, userNickname);
        Long likeCount = boardLikeService.getLikeCount(boardId);

        LikeToggleResponse response = LikeToggleResponse.builder()
                .boardId(boardId)
                .isLiked(isLiked)
                .likeCount(likeCount)
                .build();

        String message = isLiked ? "좋아요를 추가했습니다" : "좋아요를 취소했습니다";

        return ResponseEntity.ok(
                CommonApiResponse.<LikeToggleResponse>builder()
                        .success(true)
                        .message(message)
                        .data(response)
                        .build()
        );
    }

    /**
     * 게시글 좋아요 상태 확인
     */
    @Operation(summary = "게시글 좋아요 상태 확인", description = "현재 사용자의 게시글 좋아요 상태를 확인합니다.")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "조회 성공"),
            @ApiResponse(responseCode = "404", description = "게시글 없음")
    })
    @GetMapping("/{boardId}/like/status")
    public ResponseEntity<CommonApiResponse<LikeStatusResponse>> getLikeStatus(
            @Parameter(description = "게시글 ID") @PathVariable Long boardId,
            @AuthenticationPrincipal Long memberId) {

        String userId = memberId+"";

        boolean isLiked = boardLikeService.hasUserLiked(boardId, userId);
        Long likeCount = boardLikeService.getLikeCount(boardId);

        LikeStatusResponse response = LikeStatusResponse.builder()
                .boardId(boardId)
                .isLiked(isLiked)
                .likeCount(likeCount)
                .canLike(userId != null)
                .build();

        return ResponseEntity.ok(
                CommonApiResponse.<LikeStatusResponse>builder()
                        .success(true)
                        .message("좋아요 상태 조회 성공")
                        .data(response)
                        .build()
        );
    }

    /**
     * 내가 좋아요한 게시글 목록 조회
     */
    @Operation(summary = "내가 좋아요한 게시글 목록", description = "현재 사용자가 좋아요한 게시글 목록을 조회합니다.")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "조회 성공"),
            @ApiResponse(responseCode = "401", description = "인증 필요")
    })
    @GetMapping("/likes/my")
    public ResponseEntity<CommonApiResponse<List<BoardVO>>> getMyLikedBoards(
            @Parameter(description = "페이징 정보") @ModelAttribute PageRequestDTO pageRequestDTO,
            @AuthenticationPrincipal Long memberId) {

        if (memberId == null) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body(
                    CommonApiResponse.<List<BoardVO>>builder()
                            .success(false)
                            .message("로그인이 필요합니다")
                            .build()
            );
        }

        String userId = memberId+"";
        log.info("좋아요한 게시글 조회: userId={}", userId);

        List<BoardVO> likedBoards = boardLikeService.getUserLikedBoards(userId, pageRequestDTO);

        return ResponseEntity.ok(
                CommonApiResponse.<List<BoardVO>>builder()
                        .success(true)
                        .message("좋아요한 게시글 조회 성공")
                        .data(likedBoards)
                        .build()
        );
    }

    /**
     * 인기 게시글 조회 (좋아요 수 기준)
     */
    @Operation(summary = "인기 게시글 조회", description = "좋아요 수를 기준으로 인기 게시글을 조회합니다.")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "조회 성공")
    })
    @GetMapping("/popular-by-likes")
    public ResponseEntity<CommonApiResponse<List<BoardVO>>> getPopularBoardsByLikes(
            @Parameter(description = "최소 좋아요 수") @RequestParam(defaultValue = "5") int minLikes,
            @Parameter(description = "페이징 정보") @ModelAttribute PageRequestDTO pageRequestDTO) {

        log.info("좋아요 기준 인기 게시글 조회: minLikes={}", minLikes);

        List<BoardVO> popularBoards = boardLikeService.getPopularBoardsByLikes(minLikes, pageRequestDTO);

        return ResponseEntity.ok(
                CommonApiResponse.<List<BoardVO>>builder()
                        .success(true)
                        .message("인기 게시글 조회 성공")
                        .data(popularBoards)
                        .build()
        );
    }

    /**
     * 최근 인기 게시글 조회
     */
    @Operation(summary = "최근 인기 게시글 조회", description = "특정 기간 내 좋아요가 많은 게시글을 조회합니다.")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "조회 성공")
    })
    @GetMapping("/recent-popular-by-likes")
    public ResponseEntity<CommonApiResponse<List<BoardVO>>> getRecentPopularBoards(
            @Parameter(description = "조회 기간 (일)") @RequestParam(defaultValue = "7") int days,
            @Parameter(description = "최소 좋아요 수") @RequestParam(defaultValue = "3") int minLikes,
            @Parameter(description = "페이징 정보") @ModelAttribute PageRequestDTO pageRequestDTO) {

        log.info("최근 인기 게시글 조회: days={}, minLikes={}", days, minLikes);

        List<BoardVO> recentPopularBoards = boardLikeService.getRecentPopularBoards(days, minLikes, pageRequestDTO);

        return ResponseEntity.ok(
                CommonApiResponse.<List<BoardVO>>builder()
                        .success(true)
                        .message("최근 인기 게시글 조회 성공")
                        .data(recentPopularBoards)
                        .build()
        );
    }

    // ============ Response DTOs ============

    /**
     * 좋아요 토글 응답 DTO
     */
    @lombok.Data
    @lombok.Builder
    @lombok.NoArgsConstructor
    @lombok.AllArgsConstructor
    public static class LikeToggleResponse {
        private Long boardId;
        private Boolean isLiked;
        private Long likeCount;
    }

    /**
     * 좋아요 상태 응답 DTO
     */
    @lombok.Data
    @lombok.Builder
    @lombok.NoArgsConstructor
    @lombok.AllArgsConstructor
    public static class LikeStatusResponse {
        private Long boardId;
        private Boolean isLiked;
        private Long likeCount;
        private Boolean canLike;
    }
}