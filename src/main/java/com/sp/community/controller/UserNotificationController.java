package com.sp.community.controller;

import com.sp.community.model.dto.*;
import com.sp.community.model.response.CommonApiResponse;
import com.sp.community.service.UserNotificationService;
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
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

/**
 * 사용자 알림 관련 API Controller
 */
@Tag(name = "UserNotification", description = "사용자 알림 관리 API - 내 게시글에 달린 새 댓글과 좋아요 알림을 제공합니다.")
@Slf4j
@RestController
@RequestMapping("/api/v1/users/notifications")
@RequiredArgsConstructor
public class UserNotificationController {

    private final UserNotificationService userNotificationService;

    /**
     * 새 댓글 수 조회
     */
    @Operation(
            summary = "새 댓글 수 조회",
            description = "현재 사용자의 게시글에 달린 새 댓글 수를 조회합니다. 기본적으로 48시간 내의 댓글을 카운트합니다."
    )
    @ApiResponses({
            @ApiResponse(
                    responseCode = "200",
                    description = "새 댓글 수 조회 성공",
                    content = @Content(
                            mediaType = "application/json",
                            examples = {
                                    @ExampleObject(
                                            name = "새 댓글이 있는 경우",
                                            description = "48시간 내에 새 댓글이 5개 달린 경우",
                                            value = """
                            {
                                "success": true,
                                "message": "새 댓글 수 조회 성공",
                                "data": {
                                    "newCommentsCount": 5,
                                    "periodHours": 48,
                                    "since": "2024-01-13 15:30:00"
                                }
                            }
                            """
                                    ),
                                    @ExampleObject(
                                            name = "새 댓글이 없는 경우",
                                            description = "48시간 내에 새 댓글이 없는 경우",
                                            value = """
                            {
                                "success": true,
                                "message": "새 댓글 수 조회 성공",
                                "data": {
                                    "newCommentsCount": 0,
                                    "periodHours": 48,
                                    "since": "2024-01-13 15:30:00"
                                }
                            }
                            """
                                    )
                            }
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
    @GetMapping("/new-comments")
    public ResponseEntity<CommonApiResponse<Map<String, Object>>> getNewCommentsCount(
            @Parameter(description = "조회 기간 (시간)", example = "48") @RequestParam(defaultValue = "48") int hours,
            @Parameter(hidden = true) @AuthenticationPrincipal Long memberId) {

        if (memberId == null) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body(
                    CommonApiResponse.<Map<String, Object>>builder()
                            .success(false)
                            .message("로그인이 필요합니다")
                            .build()
            );
        }

        String userId = memberId+"";
        log.info("새 댓글 수 조회: userId={}, hours={}", userId, hours);

        Long newCommentsCount = userNotificationService.getNewCommentsCount(userId, hours);

        Map<String, Object> result = Map.of(
                "newCommentsCount", newCommentsCount,
                "periodHours", hours,
                "since", java.time.LocalDateTime.now().minusHours(hours).format(java.time.format.DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss"))
        );

        return ResponseEntity.ok(
                CommonApiResponse.<Map<String, Object>>builder()
                        .success(true)
                        .message("새 댓글 수 조회 성공")
                        .data(result)
                        .build()
        );
    }

    /**
     * 새 좋아요 수 조회
     */
    @Operation(
            summary = "새 좋아요 수 조회",
            description = "현재 사용자의 게시글에 달린 새 좋아요 수를 조회합니다. 기본적으로 48시간 내의 좋아요를 카운트합니다."
    )
    @ApiResponses({
            @ApiResponse(
                    responseCode = "200",
                    description = "새 좋아요 수 조회 성공",
                    content = @Content(
                            mediaType = "application/json",
                            examples = {
                                    @ExampleObject(
                                            name = "새 좋아요가 있는 경우",
                                            description = "48시간 내에 새 좋아요가 12개 달린 경우",
                                            value = """
                            {
                                "success": true,
                                "message": "새 좋아요 수 조회 성공",
                                "data": {
                                    "newLikesCount": 12,
                                    "periodHours": 48,
                                    "since": "2024-01-13 15:30:00"
                                }
                            }
                            """
                                    ),
                                    @ExampleObject(
                                            name = "새 좋아요가 없는 경우",
                                            description = "48시간 내에 새 좋아요가 없는 경우",
                                            value = """
                            {
                                "success": true,
                                "message": "새 좋아요 수 조회 성공",
                                "data": {
                                    "newLikesCount": 0,
                                    "periodHours": 48,
                                    "since": "2024-01-13 15:30:00"
                                }
                            }
                            """
                                    )
                            }
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
    @GetMapping("/new-likes")
    public ResponseEntity<CommonApiResponse<Map<String, Object>>> getNewLikesCount(
            @Parameter(description = "조회 기간 (시간)", example = "48") @RequestParam(defaultValue = "48") int hours,
            @Parameter(hidden = true) @AuthenticationPrincipal Long memberId) {

        if (memberId == null) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body(
                    CommonApiResponse.<Map<String, Object>>builder()
                            .success(false)
                            .message("로그인이 필요합니다")
                            .build()
            );
        }

        String userId = memberId+"";
        log.info("새 좋아요 수 조회: userId={}, hours={}", userId, hours);

        Long newLikesCount = userNotificationService.getNewLikesCount(userId, hours);

        Map<String, Object> result = Map.of(
                "newLikesCount", newLikesCount,
                "periodHours", hours,
                "since", java.time.LocalDateTime.now().minusHours(hours).format(java.time.format.DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss"))
        );

        return ResponseEntity.ok(
                CommonApiResponse.<Map<String, Object>>builder()
                        .success(true)
                        .message("새 좋아요 수 조회 성공")
                        .data(result)
                        .build()
        );
    }

    /**
     * 사용자 활동 요약 조회
     */
    @Operation(
            summary = "사용자 활동 요약 조회",
            description = "현재 사용자의 게시글에 대한 활동 요약(새 댓글 수 + 새 좋아요 수)을 조회합니다."
    )
    @ApiResponses({
            @ApiResponse(
                    responseCode = "200",
                    description = "활동 요약 조회 성공",
                    content = @Content(
                            mediaType = "application/json",
                            examples = {
                                    @ExampleObject(
                                            name = "활동이 있는 경우",
                                            description = "48시간 내에 새 댓글 5개, 새 좋아요 12개가 있는 경우",
                                            value = """
                            {
                                "success": true,
                                "message": "활동 요약 조회 성공",
                                "data": {
                                    "newCommentsCount": 5,
                                    "newLikesCount": 12,
                                    "since": "2024-01-13 15:30:00",
                                    "until": "2024-01-15 15:30:00",
                                    "periodHours": 48,
                                    "totalActivityCount": 17,
                                    "hasActivity": true
                                }
                            }
                            """
                                    ),
                                    @ExampleObject(
                                            name = "활동이 없는 경우",
                                            description = "48시간 내에 새로운 활동이 없는 경우",
                                            value = """
                            {
                                "success": true,
                                "message": "활동 요약 조회 성공",
                                "data": {
                                    "newCommentsCount": 0,
                                    "newLikesCount": 0,
                                    "since": "2024-01-13 15:30:00",
                                    "until": "2024-01-15 15:30:00",
                                    "periodHours": 48,
                                    "totalActivityCount": 0,
                                    "hasActivity": false
                                }
                            }
                            """
                                    )
                            }
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
    @GetMapping("/activity-summary")
    public ResponseEntity<CommonApiResponse<UserActivitySummaryDTO>> getActivitySummary(
            @Parameter(description = "조회 기간 (시간)", example = "48") @RequestParam(defaultValue = "48") int hours,
            @Parameter(hidden = true) @AuthenticationPrincipal Long memberId) {

        if (memberId == null) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body(
                    CommonApiResponse.<UserActivitySummaryDTO>builder()
                            .success(false)
                            .message("로그인이 필요합니다")
                            .build()
            );
        }

        String userId = memberId+"";
        log.info("활동 요약 조회: userId={}, hours={}", userId, hours);

        UserActivitySummaryDTO summary = userNotificationService.getActivitySummary(userId, hours);

        return ResponseEntity.ok(
                CommonApiResponse.<UserActivitySummaryDTO>builder()
                        .success(true)
                        .message("활동 요약 조회 성공")
                        .data(summary)
                        .build()
        );
    }

    /**
     * 새 댓글 알림 목록 조회
     */
    @Operation(
            summary = "새 댓글 알림 목록 조회",
            description = "현재 사용자의 게시글에 달린 새 댓글 목록을 상세히 조회합니다."
    )
    @ApiResponses({
            @ApiResponse(
                    responseCode = "200",
                    description = "새 댓글 알림 목록 조회 성공",
                    content = @Content(
                            mediaType = "application/json",
                            examples = @ExampleObject(
                                    value = """
                        {
                            "success": true,
                            "message": "새 댓글 알림 목록 조회 성공",
                            "data": [
                                {
                                    "commentId": 123,
                                    "content": "정말 유용한 게시글이네요! 감사합니다.",
                                    "commenterUserId": "user456",
                                    "commenterNickname": "댓글러456",
                                    "boardId": 1,
                                    "boardTitle": "내가 작성한 게시글 제목",
                                    "createdAt": "2024-01-15 14:30:00",
                                    "contentPreview": "정말 유용한 게시글이네요! 감사합니다."
                                },
                                {
                                    "commentId": 124,
                                    "content": "이 정보 덕분에 문제를 해결했어요!",
                                    "commenterUserId": "user789",
                                    "commenterNickname": "댓글러789",
                                    "boardId": 2,
                                    "boardTitle": "또 다른 내 게시글",
                                    "createdAt": "2024-01-15 13:15:00",
                                    "contentPreview": "이 정보 덕분에 문제를 해결했어요!"
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
    @GetMapping("/new-comments/list")
    public ResponseEntity<CommonApiResponse<List<NewCommentNotificationDTO>>> getNewCommentNotifications(
            @Parameter(description = "조회 기간 (시간)", example = "48") @RequestParam(defaultValue = "48") int hours,
            @Parameter(description = "페이징 정보") @ModelAttribute PageRequestDTO pageRequestDTO,
            @Parameter(hidden = true) @AuthenticationPrincipal Long memberId) {

        if (memberId == null) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body(
                    CommonApiResponse.<List<NewCommentNotificationDTO>>builder()
                            .success(false)
                            .message("로그인이 필요합니다")
                            .build()
            );
        }

        String userId = memberId+"";
        log.info("새 댓글 알림 목록 조회: userId={}, hours={}", userId, hours);

        List<NewCommentNotificationDTO> notifications = userNotificationService.getNewCommentNotifications(userId, hours, pageRequestDTO);

        return ResponseEntity.ok(
                CommonApiResponse.<List<NewCommentNotificationDTO>>builder()
                        .success(true)
                        .message("새 댓글 알림 목록 조회 성공")
                        .data(notifications)
                        .build()
        );
    }

    /**
     * 새 좋아요 알림 목록 조회
     */
    @Operation(
            summary = "새 좋아요 알림 목록 조회",
            description = "현재 사용자의 게시글에 달린 새 좋아요 목록을 상세히 조회합니다."
    )
    @ApiResponses({
            @ApiResponse(
                    responseCode = "200",
                    description = "새 좋아요 알림 목록 조회 성공",
                    content = @Content(
                            mediaType = "application/json",
                            examples = @ExampleObject(
                                    value = """
                        {
                            "success": true,
                            "message": "새 좋아요 알림 목록 조회 성공",
                            "data": [
                                {
                                    "likeId": 789,
                                    "likerUserId": "user101",
                                    "likerNickname": "좋아요러101",
                                    "boardId": 1,
                                    "boardTitle": "내가 작성한 게시글 제목",
                                    "boardContentPreview": "게시글 내용의 앞부분입니다...",
                                    "createdAt": "2024-01-15 14:45:00"
                                },
                                {
                                    "likeId": 790,
                                    "likerUserId": "user202",
                                    "likerNickname": "좋아요러202",
                                    "boardId": 1,
                                    "boardTitle": "내가 작성한 게시글 제목",
                                    "boardContentPreview": "게시글 내용의 앞부분입니다...",
                                    "createdAt": "2024-01-15 13:30:00"
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
    @GetMapping("/new-likes/list")
    public ResponseEntity<CommonApiResponse<List<NewLikeNotificationDTO>>> getNewLikeNotifications(
            @Parameter(description = "조회 기간 (시간)", example = "48") @RequestParam(defaultValue = "48") int hours,
            @Parameter(description = "페이징 정보") @ModelAttribute PageRequestDTO pageRequestDTO,
            @Parameter(hidden = true) @AuthenticationPrincipal Long memberId) {

        if (memberId == null) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body(
                    CommonApiResponse.<List<NewLikeNotificationDTO>>builder()
                            .success(false)
                            .message("로그인이 필요합니다")
                            .build()
            );
        }

        String userId = memberId+"";
        log.info("새 좋아요 알림 목록 조회: userId={}, hours={}", userId, hours);

        List<NewLikeNotificationDTO> notifications = userNotificationService.getNewLikeNotifications(userId, hours, pageRequestDTO);

        return ResponseEntity.ok(
                CommonApiResponse.<List<NewLikeNotificationDTO>>builder()
                        .success(true)
                        .message("새 좋아요 알림 목록 조회 성공")
                        .data(notifications)
                        .build()
        );
    }

    /**
     * 통합 알림 조회
     */
    @Operation(
            summary = "통합 알림 조회",
            description = "새 댓글과 새 좋아요를 포함한 통합 알림 정보를 조회합니다. 대시보드나 알림 센터에서 사용하기 적합합니다."
    )
    @ApiResponses({
            @ApiResponse(
                    responseCode = "200",
                    description = "통합 알림 조회 성공",
                    content = @Content(
                            mediaType = "application/json",
                            examples = @ExampleObject(
                                    value = """
                        {
                            "success": true,
                            "message": "통합 알림 조회 성공",
                            "data": {
                                "newComments": [
                                    {
                                        "commentId": 123,
                                        "content": "정말 유용한 게시글이네요!",
                                        "commenterUserId": "user456",
                                        "commenterNickname": "댓글러456",
                                        "boardId": 1,
                                        "boardTitle": "내가 작성한 게시글",
                                        "createdAt": "2024-01-15 14:30:00"
                                    }
                                ],
                                "newLikes": [
                                    {
                                        "likeId": 789,
                                        "likerUserId": "user101",
                                        "likerNickname": "좋아요러101",
                                        "boardId": 1,
                                        "boardTitle": "내가 작성한 게시글",
                                        "createdAt": "2024-01-15 14:45:00"
                                    }
                                ],
                                "summary": {
                                    "newCommentsCount": 5,
                                    "newLikesCount": 12,
                                    "totalActivityCount": 17,
                                    "hasActivity": true,
                                    "periodHours": 48
                                },
                                "totalElements": 17,
                                "currentPage": 1,
                                "pageSize": 10,
                                "hasNext": true
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
    @GetMapping
    public ResponseEntity<CommonApiResponse<UserNotificationListDTO>> getUserNotifications(
            @Parameter(description = "조회 기간 (시간)", example = "48") @RequestParam(defaultValue = "48") int hours,
            @Parameter(description = "페이징 정보") @ModelAttribute PageRequestDTO pageRequestDTO,
            @Parameter(hidden = true) @AuthenticationPrincipal Long memberId) {

        if (memberId == null) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body(
                    CommonApiResponse.<UserNotificationListDTO>builder()
                            .success(false)
                            .message("로그인이 필요합니다")
                            .build()
            );
        }

        String userId = memberId+"";
        log.info("통합 알림 조회: userId={}, hours={}", userId, hours);

        UserNotificationListDTO notifications = userNotificationService.getUserNotifications(userId, hours, pageRequestDTO);

        return ResponseEntity.ok(
                CommonApiResponse.<UserNotificationListDTO>builder()
                        .success(true)
                        .message("통합 알림 조회 성공")
                        .data(notifications)
                        .build()
        );
    }
}