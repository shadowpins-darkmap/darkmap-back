package com.sp.community.service;

import com.sp.community.model.dto.*;
import com.sp.community.persistent.entity.BoardLikeEntity;
import com.sp.community.persistent.entity.CommentEntity;
import com.sp.community.persistent.repository.BoardLikeRepository;
import com.sp.community.persistent.repository.CommentRepository;
import com.sp.member.repository.MemberRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;
import java.util.stream.Collectors;

/**
 * 사용자 알림 서비스
 */
@Slf4j
@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class UserNotificationService {

    private final CommentRepository commentRepository;
    private final BoardLikeRepository boardLikeRepository;
    private final MemberRepository memberRepository;

    /**
     * 사용자의 게시글에 달린 새 댓글 수 조회
     */
    public Long getNewCommentsCount(Long userId, int hours) {
        LocalDateTime since = LocalDateTime.now().minusHours(hours);
        return commentRepository.countNewCommentsOnUserBoards(userId, since);
    }

    /**
     * 사용자의 게시글에 달린 새 좋아요 수 조회
     */
    public Long getNewLikesCount(Long userId, int hours) {
        LocalDateTime since = LocalDateTime.now().minusHours(hours);
        return boardLikeRepository.countNewLikesOnUserBoards(userId, since);
    }

    /**
     * 사용자 활동 요약 조회
     */
    public UserActivitySummaryDTO getActivitySummary(Long userId, int hours) {
        LocalDateTime since = LocalDateTime.now().minusHours(hours);
        LocalDateTime now = LocalDateTime.now();

        Long newCommentsCount = commentRepository.countNewCommentsOnUserBoards(userId, since);
        Long newLikesCount = boardLikeRepository.countNewLikesOnUserBoards(userId, since);

        log.debug("사용자 활동 요약 조회: userId={}, hours={}, newComments={}, newLikes={}",
                userId, hours, newCommentsCount, newLikesCount);

        return UserActivitySummaryDTO.builder()
                .newCommentsCount(newCommentsCount)
                .newLikesCount(newLikesCount)
                .since(since)
                .until(now)
                .periodHours(hours)
                .build();
    }

    /**
     * 사용자의 게시글에 달린 새 댓글 목록 조회
     */
    public List<NewCommentNotificationDTO> getNewCommentNotifications(Long userId, int hours, PageRequestDTO pageRequestDTO) {
        LocalDateTime since = LocalDateTime.now().minusHours(hours);

        if (pageRequestDTO == null) {
            pageRequestDTO = PageRequestDTO.builder().build();
        }
        pageRequestDTO.setDefaults();

        Pageable pageable = PageRequest.of(
                pageRequestDTO.getPage() - 1,
                pageRequestDTO.getSize(),
                Sort.by(Sort.Direction.DESC, "createdAt")
        );

        Page<CommentEntity> commentPage = commentRepository.findNewCommentsOnUserBoards(userId, since, pageable);

        return commentPage.getContent().stream()
                .map(this::convertToNewCommentNotificationDTO)
                .collect(Collectors.toList());
    }

    /**
     * 사용자의 게시글에 달린 새 좋아요 목록 조회
     */
    public List<NewLikeNotificationDTO> getNewLikeNotifications(Long userId, int hours, PageRequestDTO pageRequestDTO) {
        LocalDateTime since = LocalDateTime.now().minusHours(hours);

        if (pageRequestDTO == null) {
            pageRequestDTO = PageRequestDTO.builder().build();
        }
        pageRequestDTO.setDefaults();

        Pageable pageable = PageRequest.of(
                pageRequestDTO.getPage() - 1,
                pageRequestDTO.getSize(),
                Sort.by(Sort.Direction.DESC, "createdAt")
        );

        Page<BoardLikeEntity> likePage = boardLikeRepository.findNewLikesOnUserBoards(userId, since, pageable);

        return likePage.getContent().stream()
                .map(this::convertToNewLikeNotificationDTO)
                .collect(Collectors.toList());
    }

    /**
     * 사용자 알림 통합 조회 (댓글 + 좋아요)
     */
    public UserNotificationListDTO getUserNotifications(Long userId, int hours, PageRequestDTO pageRequestDTO) {
        log.info("사용자 알림 통합 조회: userId={}, hours={}", userId, hours);

        // 활동 요약
        UserActivitySummaryDTO summary = getActivitySummary(userId, hours);

        // 새 댓글 목록 (최대 5개)
        PageRequestDTO commentPageDTO = PageRequestDTO.builder()
                .page(1)
                .size(5)
                .build();
        List<NewCommentNotificationDTO> newComments = getNewCommentNotifications(userId, hours, commentPageDTO);

        // 새 좋아요 목록 (최대 5개)
        PageRequestDTO likePageDTO = PageRequestDTO.builder()
                .page(1)
                .size(5)
                .build();
        List<NewLikeNotificationDTO> newLikes = getNewLikeNotifications(userId, hours, likePageDTO);

        // 전체 개수 계산
        Long totalElements = summary.getTotalActivityCount();

        return UserNotificationListDTO.builder()
                .newComments(newComments)
                .newLikes(newLikes)
                .summary(summary)
                .totalElements(totalElements)
                .currentPage(1)
                .pageSize(10)
                .hasNext(totalElements > 10)
                .build();
    }

    // ============ Private Helper Methods ============

    /**
     * 사용자 닉네임 조회 헬퍼 메서드
     */
    private String getAuthorNickname(Long authorId) {
        try {
            return memberRepository.findNicknameByMemberId(authorId)
                    .orElse(authorId.toString());
        } catch (Exception e) {
            log.warn("닉네임 조회 실패: authorId={}", authorId);
            return authorId.toString();
        }
    }

    /**
     * CommentEntity를 NewCommentNotificationDTO로 변환
     */
    private NewCommentNotificationDTO convertToNewCommentNotificationDTO(CommentEntity comment) {
        return NewCommentNotificationDTO.builder()
                .commentId(comment.getCommentId())
                .content(comment.getContent())
                .commenterUserId(comment.getAuthorId())
                .commenterNickname(getAuthorNickname(comment.getAuthorId()))
                .boardId(comment.getBoard().getBoardId())
                .boardTitle(comment.getBoard().getTitle())
                .createdAt(comment.getCreatedAt())
                .build();
    }

    /**
     * BoardLikeEntity를 NewLikeNotificationDTO로 변환
     */
    private NewLikeNotificationDTO convertToNewLikeNotificationDTO(BoardLikeEntity like) {
        String contentPreview = like.getBoard().getContent();
        if (contentPreview != null && contentPreview.length() > 100) {
            contentPreview = contentPreview.substring(0, 100) + "...";
        }

        return NewLikeNotificationDTO.builder()
                .likeId(like.getLikeId())
                .likerUserId(like.getUserId())
                .boardId(like.getBoard().getBoardId())
                .boardTitle(like.getBoard().getTitle())
                .boardContentPreview(contentPreview)
                .createdAt(like.getCreatedAt())
                .build();
    }
}