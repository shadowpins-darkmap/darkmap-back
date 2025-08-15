package com.sp.community.controller;

import com.sp.community.model.dto.PageRequestDTO;
import com.sp.community.model.response.CommonApiResponse;
import com.sp.community.model.vo.BoardVO;
import com.sp.community.service.BoardLikeService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.ExampleObject;
import io.swagger.v3.oas.annotations.media.Schema;
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
@Tag(name = "BoardLike", description = "게시글 좋아요 관리 API - 게시글 좋아요 추가/취소, 좋아요한 게시글 조회, 인기 게시글 조회 기능을 제공합니다.")
@Slf4j
@RestController
@RequestMapping("/api/v1/boards")
@RequiredArgsConstructor
public class BoardLikeController {

    private final BoardLikeService boardLikeService;

    /**
     * 게시글 좋아요 토글 (추가/취소)
     */
    @Operation(
            summary = "게시글 좋아요 토글",
            description = "게시글에 좋아요를 추가하거나 취소합니다. 이미 좋아요한 게시글이면 취소하고, 좋아요하지 않은 게시글이면 추가합니다. 자신의 게시글에는 좋아요할 수 없습니다."
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
                                            description = "좋아요를 추가한 경우",
                                            value = """
                            {
                                "success": true,
                                "message": "좋아요를 추가했습니다",
                                "data": {
                                    "boardId": 1,
                                    "isLiked": true,
                                    "likeCount": 26
                                }
                            }
                            """
                                    ),
                                    @ExampleObject(
                                            name = "좋아요 취소",
                                            description = "좋아요를 취소한 경우",
                                            value = """
                            {
                                "success": true,
                                "message": "좋아요를 취소했습니다",
                                "data": {
                                    "boardId": 1,
                                    "isLiked": false,
                                    "likeCount": 25
                                }
                            }
                            """
                                    )
                            }
                    )
            ),
            @ApiResponse(
                    responseCode = "400",
                    description = "잘못된 요청 - 유효하지 않은 게시글 ID",
                    content = @Content(
                            mediaType = "application/json",
                            examples = @ExampleObject(
                                    value = """
                        {
                            "success": false,
                            "message": "게시글 ID는 필수입니다.",
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
                    description = "권한 없음 - 자신의 게시글에 좋아요 시도",
                    content = @Content(
                            mediaType = "application/json",
                            examples = @ExampleObject(
                                    value = """
                        {
                            "success": false,
                            "message": "자신의 게시글에는 좋아요할 수 없습니다.",
                            "data": null
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
    @PostMapping("/{boardId}/like")
    public ResponseEntity<CommonApiResponse<LikeToggleResponse>> toggleLike(
            @Parameter(description = "게시글 ID", required = true, example = "1") @PathVariable Long boardId,
            @Parameter(hidden = true) @AuthenticationPrincipal Long memberId) {

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
    @Operation(
            summary = "게시글 좋아요 상태 확인",
            description = "현재 사용자의 특정 게시글에 대한 좋아요 상태와 전체 좋아요 수를 조회합니다."
    )
    @ApiResponses({
            @ApiResponse(
                    responseCode = "200",
                    description = "좋아요 상태 조회 성공",
                    content = @Content(
                            mediaType = "application/json",
                            examples = {
                                    @ExampleObject(
                                            name = "로그인한 사용자 - 좋아요한 상태",
                                            description = "로그인한 사용자가 해당 게시글을 좋아요한 경우",
                                            value = """
                            {
                                "success": true,
                                "message": "좋아요 상태 조회 성공",
                                "data": {
                                    "boardId": 1,
                                    "isLiked": true,
                                    "likeCount": 25,
                                    "canLike": true
                                }
                            }
                            """
                                    ),
                                    @ExampleObject(
                                            name = "로그인한 사용자 - 좋아요하지 않은 상태",
                                            description = "로그인한 사용자가 해당 게시글을 좋아요하지 않은 경우",
                                            value = """
                            {
                                "success": true,
                                "message": "좋아요 상태 조회 성공",
                                "data": {
                                    "boardId": 1,
                                    "isLiked": false,
                                    "likeCount": 25,
                                    "canLike": true
                                }
                            }
                            """
                                    ),
                                    @ExampleObject(
                                            name = "비로그인 사용자",
                                            description = "로그인하지 않은 사용자의 경우",
                                            value = """
                            {
                                "success": true,
                                "message": "좋아요 상태 조회 성공",
                                "data": {
                                    "boardId": 1,
                                    "isLiked": false,
                                    "likeCount": 25,
                                    "canLike": false
                                }
                            }
                            """
                                    )
                            }
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
    @GetMapping("/{boardId}/like/status")
    public ResponseEntity<CommonApiResponse<LikeStatusResponse>> getLikeStatus(
            @Parameter(description = "게시글 ID", required = true, example = "1") @PathVariable Long boardId,
            @Parameter(hidden = true) @AuthenticationPrincipal Long memberId) {

        String userId = memberId != null ? memberId+"" : null;

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
    @Operation(
            summary = "내가 좋아요한 게시글 목록",
            description = "현재 로그인한 사용자가 좋아요한 게시글 목록을 페이징으로 조회합니다."
    )
    @ApiResponses({
            @ApiResponse(
                    responseCode = "200",
                    description = "좋아요한 게시글 목록 조회 성공",
                    content = @Content(
                            mediaType = "application/json",
                            examples = @ExampleObject(
                                    value = """
                        {
                            "success": true,
                            "message": "좋아요한 게시글 조회 성공",
                            "data": [
                                {
                                    "boardId": 1,
                                    "title": "내가 좋아요한 첫 번째 게시글",
                                    "content": "정말 유용한 게시글이었습니다.",
                                    "authorId": "user456",
                                    "authorNickname": "다른사용자456",
                                    "viewCount": 250,
                                    "likeCount": 45,
                                    "commentCount": 12,
                                    "createdAt": "2024-01-10 14:30:00",
                                    "isAuthor": false,
                                    "isLiked": true
                                },
                                {
                                    "boardId": 5,
                                    "title": "내가 좋아요한 두 번째 게시글",
                                    "content": "매우 흥미로운 내용입니다.",
                                    "authorId": "user789",
                                    "authorNickname": "다른사용자789",
                                    "viewCount": 180,
                                    "likeCount": 32,
                                    "commentCount": 8,
                                    "createdAt": "2024-01-12 09:15:00",
                                    "isAuthor": false,
                                    "isLiked": true
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
    @GetMapping("/likes/my")
    public ResponseEntity<CommonApiResponse<List<BoardVO>>> getMyLikedBoards(
            @Parameter(description = "페이징 정보") @ModelAttribute PageRequestDTO pageRequestDTO,
            @Parameter(hidden = true) @AuthenticationPrincipal Long memberId) {

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
    @Operation(
            summary = "인기 게시글 조회 (좋아요 수 기준)",
            description = "설정한 최소 좋아요 수 이상의 게시글을 좋아요 수 내림차순으로 조회합니다."
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
                                    "boardId": 3,
                                    "title": "가장 인기 있는 게시글",
                                    "content": "많은 사람들이 좋아하는 게시글입니다.",
                                    "authorId": "user123",
                                    "authorNickname": "인기작성자123",
                                    "viewCount": 1500,
                                    "likeCount": 89,
                                    "commentCount": 35,
                                    "createdAt": "2024-01-08 11:20:00",
                                    "isAuthor": false,
                                    "isLiked": true
                                },
                                {
                                    "boardId": 7,
                                    "title": "두 번째로 인기 있는 게시글",
                                    "content": "역시 좋은 반응을 얻고 있는 게시글입니다.",
                                    "authorId": "user456",
                                    "authorNickname": "인기작성자456",
                                    "viewCount": 1200,
                                    "likeCount": 67,
                                    "commentCount": 28,
                                    "createdAt": "2024-01-09 16:45:00",
                                    "isAuthor": false,
                                    "isLiked": false
                                }
                            ]
                        }
                        """
                            )
                    )
            )
    })
    @GetMapping("/popular-by-likes")
    public ResponseEntity<CommonApiResponse<List<BoardVO>>> getPopularBoardsByLikes(
            @Parameter(description = "최소 좋아요 수", example = "5") @RequestParam(defaultValue = "5") int minLikes,
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
    @Operation(
            summary = "최근 인기 게시글 조회",
            description = "지정된 기간(일) 내에 작성된 게시글 중에서 설정한 최소 좋아요 수 이상의 게시글을 조회합니다."
    )
    @ApiResponses({
            @ApiResponse(
                    responseCode = "200",
                    description = "최근 인기 게시글 조회 성공",
                    content = @Content(
                            mediaType = "application/json",
                            examples = @ExampleObject(
                                    value = """
                        {
                            "success": true,
                            "message": "최근 인기 게시글 조회 성공",
                            "data": [
                                {
                                    "boardId": 10,
                                    "title": "최근 일주일 내 가장 인기 있는 게시글",
                                    "content": "최근에 올라온 게시글 중 가장 인기가 많습니다.",
                                    "authorId": "user789",
                                    "authorNickname": "최근인기작성자789",
                                    "viewCount": 850,
                                    "likeCount": 42,
                                    "commentCount": 18,
                                    "createdAt": "2024-01-14 13:30:00",
                                    "isAuthor": false,
                                    "isLiked": true
                                },
                                {
                                    "boardId": 12,
                                    "title": "최근 화제가 된 게시글",
                                    "content": "며칠 전에 올라왔는데 벌써 인기가 많네요.",
                                    "authorId": "user101",
                                    "authorNickname": "최근인기작성자101",
                                    "viewCount": 720,
                                    "likeCount": 35,
                                    "commentCount": 15,
                                    "createdAt": "2024-01-13 10:15:00",
                                    "isAuthor": false,
                                    "isLiked": false
                                }
                            ]
                        }
                        """
                            )
                    )
            )
    })
    @GetMapping("/recent-popular-by-likes")
    public ResponseEntity<CommonApiResponse<List<BoardVO>>> getRecentPopularBoards(
            @Parameter(description = "조회 기간 (일)", example = "7") @RequestParam(defaultValue = "7") int days,
            @Parameter(description = "최소 좋아요 수", example = "3") @RequestParam(defaultValue = "3") int minLikes,
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
    @Schema(description = "게시글 좋아요 토글 응답")
    @lombok.Data
    @lombok.Builder
    @lombok.NoArgsConstructor
    @lombok.AllArgsConstructor
    public static class LikeToggleResponse {

        @Schema(description = "게시글 ID", example = "1")
        private Long boardId;

        @Schema(description = "좋아요 여부", example = "true")
        private Boolean isLiked;

        @Schema(description = "총 좋아요 수", example = "26")
        private Long likeCount;
    }

    /**
     * 좋아요 상태 응답 DTO
     */
    @Schema(description = "게시글 좋아요 상태 응답")
    @lombok.Data
    @lombok.Builder
    @lombok.NoArgsConstructor
    @lombok.AllArgsConstructor
    public static class LikeStatusResponse {

        @Schema(description = "게시글 ID", example = "1")
        private Long boardId;

        @Schema(description = "현재 사용자의 좋아요 여부", example = "true")
        private Boolean isLiked;

        @Schema(description = "총 좋아요 수", example = "25")
        private Long likeCount;

        @Schema(description = "좋아요 가능 여부 (로그인 상태)", example = "true")
        private Boolean canLike;
    }
}