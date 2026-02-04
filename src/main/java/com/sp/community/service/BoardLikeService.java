package com.sp.community.service;

import com.sp.exception.BoardNotFoundException;
import com.sp.exception.UnauthorizedException;
import com.sp.community.model.dto.PageRequestDTO;
import com.sp.community.model.vo.BoardVO;
import com.sp.community.persistent.entity.BoardEntity;
import com.sp.community.persistent.entity.BoardLikeEntity;
import com.sp.community.persistent.repository.BoardLikeRepository;
import com.sp.community.persistent.repository.BoardRepository;
import com.sp.community.model.response.FileUploadResponse;
import com.sp.api.repository.MemberRepository;
import lombok.*;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

/**
 * 게시글 좋아요 비즈니스 로직 Service (이미지 한 개 첨부 지원)
 */
@Slf4j
@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class BoardLikeService {

    private final BoardLikeRepository boardLikeRepository;
    private final BoardRepository boardRepository;
    private final MemberRepository memberRepository;
    private final FileService fileService;

    /**
     * 게시글 좋아요 추가
     */
    @Transactional
    public boolean addLike(Long boardId, Long userId) {
        log.info("게시글 좋아요 추가: boardId={}, userId={}", boardId, userId);

        // 입력값 검증
        validateInput(boardId, userId);

        // 게시글 존재 확인
        BoardEntity boardEntity = boardRepository.findByIdAndNotDeleted(boardId)
                .orElseThrow(() -> new BoardNotFoundException("게시글을 찾을 수 없습니다."));

        // 자신의 게시글에는 좋아요할 수 없음
        if (boardEntity.getAuthorId().equals(userId)) {
            throw new UnauthorizedException("자신의 게시글에는 좋아요할 수 없습니다.");
        }

        // 기존 좋아요 확인
        Optional<BoardLikeEntity> existingLike = boardLikeRepository
                .findByBoardIdAndUserId(boardId, userId);

        if (existingLike.isPresent()) {
            BoardLikeEntity likeEntity = existingLike.get();

            if (likeEntity.isActive()) {
                // 이미 좋아요한 상태
                log.warn("이미 좋아요한 게시글: boardId={}, userId={}", boardId, userId);
                return false;
            } else {
                // 취소된 좋아요 복구
                likeEntity.restoreLike();
                boardLikeRepository.save(likeEntity);

                // 게시글 좋아요 수 증가
                boardRepository.incrementLikeCount(boardId);

                log.info("좋아요 복구 완료: boardId={}, userId={}", boardId, userId);
                return true;
            }
        }

        // 새로운 좋아요 생성
        BoardLikeEntity newLike = BoardLikeEntity.builder()
                .board(boardEntity)
                .userId(userId)
                .build();

        boardLikeRepository.save(newLike);

        // 게시글 좋아요 수 증가
        boardRepository.incrementLikeCount(boardId);

        log.info("좋아요 추가 완료: boardId={}, userId={}", boardId, userId);
        return true;
    }

    /**
     * 게시글 좋아요 취소
     */
    @Transactional
    public boolean removeLike(Long boardId, Long userId) {
        log.info("게시글 좋아요 취소: boardId={}, userId={}", boardId, userId);

        // 입력값 검증
        if (boardId == null) {
            throw new IllegalArgumentException("필수 파라미터가 누락되었습니다.");
        }

        // 게시글 존재 확인
        if (!boardRepository.existsByIdAndNotDeleted(boardId)) {
            throw new BoardNotFoundException("게시글을 찾을 수 없습니다.");
        }

        // 기존 좋아요 확인
        Optional<BoardLikeEntity> existingLike = boardLikeRepository
                .findByBoardIdAndUserIdAndNotDeleted(boardId, userId);

        if (existingLike.isEmpty()) {
            log.warn("좋아요하지 않은 게시글: boardId={}, userId={}", boardId, userId);
            return false;
        }

        // 좋아요 취소 (소프트 삭제)
        BoardLikeEntity likeEntity = existingLike.get();
        likeEntity.cancelLike();
        boardLikeRepository.save(likeEntity);

        // 게시글 좋아요 수 감소
        boardRepository.decrementLikeCount(boardId);

        log.info("좋아요 취소 완료: boardId={}, userId={}", boardId, userId);
        return true;
    }

    /**
     * 좋아요 토글 (추가/취소)
     */
    @Transactional
    public boolean toggleLike(Long boardId, Long userId) {
        log.debug("좋아요 토글: boardId={}, userId={}", boardId, userId);

        boolean hasLiked = hasUserLiked(boardId, userId);

        if (hasLiked) {
            return !removeLike(boardId, userId); // 취소하면 false 반환
        } else {
            return addLike(boardId, userId); // 추가하면 true 반환
        }
    }

    /**
     * 사용자가 게시글을 좋아요했는지 확인
     */
    public boolean hasUserLiked(Long boardId, Long userId) {
        if (boardId == null) {
            return false;
        }

        return boardLikeRepository.existsByBoardIdAndUserIdAndNotDeleted(boardId, userId);
    }

    /**
     * 게시글의 좋아요 수 조회
     */
    public Long getLikeCount(Long boardId) {
        if (boardId == null) {
            return 0L;
        }

        return boardLikeRepository.countByBoardIdAndNotDeleted(boardId);
    }

    /**
     * 특정 사용자가 좋아요한 게시글 목록 조회
     */
    public List<BoardVO> getUserLikedBoards(Long userId, PageRequestDTO pageRequestDTO) {
        log.debug("사용자 좋아요 게시글 조회: userId={}", userId);
        if (userId == null) {
            return List.of();
        }

        if (pageRequestDTO != null) {
            pageRequestDTO.setDefaults();
        }

        Pageable pageable = pageRequestDTO != null ?
                pageRequestDTO.toBoardPageable() :
                PageRequestDTO.builder().build().toBoardPageable();

        Page<BoardEntity> likedBoards = boardRepository
                .findBoardsLikedByUser(userId, pageable);

        return likedBoards.getContent().stream()
                .map(this::convertBoardToVO)
                .collect(Collectors.toList());
    }

    /**
     * 인기 게시글 조회 (좋아요 수 기준)
     */
    public List<BoardVO> getPopularBoardsByLikes(int minLikes, PageRequestDTO pageRequestDTO) {
        log.debug("인기 게시글 조회: minLikes={}", minLikes);

        if (pageRequestDTO != null) {
            pageRequestDTO.setDefaults();
        }

        Pageable pageable = pageRequestDTO != null ?
                pageRequestDTO.toBoardPageable() :
                PageRequestDTO.builder().build().toBoardPageable();

        Page<BoardEntity> popularBoards = boardRepository
                .findPopularBoards(minLikes, pageable);

        return popularBoards.getContent().stream()
                .map(this::convertBoardToVO)
                .collect(Collectors.toList());
    }

    /**
     * 최근 인기 게시글 조회 (특정 기간 내)
     */
    public List<BoardVO> getRecentPopularBoards(int days, int minLikes, PageRequestDTO pageRequestDTO) {
        log.debug("최근 인기 게시글 조회: days={}, minLikes={}", days, minLikes);

        LocalDateTime fromDate = LocalDateTime.now().minusDays(days);

        if (pageRequestDTO != null) {
            pageRequestDTO.setDefaults();
        }

        Pageable pageable = pageRequestDTO != null ?
                pageRequestDTO.toBoardPageable() :
                PageRequestDTO.builder().build().toBoardPageable();

        Page<BoardEntity> recentPopularBoards = boardRepository
                .findRecentPopularBoards(fromDate, minLikes, pageable);

        return recentPopularBoards.getContent().stream()
                .map(this::convertBoardToVO)
                .collect(Collectors.toList());
    }

    /**
     * 좋아요 통계 VO
     */
    @Getter
    @Setter
    @NoArgsConstructor
    @AllArgsConstructor
    @Builder
    public static class BoardLikeStatsVO {
        private Long boardId;
        private Long totalLikes;
        private Long todayLikes;
        private Long thisWeekLikes;

        public static BoardLikeStatsVO empty() {
            return BoardLikeStatsVO.builder()
                    .totalLikes(0L)
                    .todayLikes(0L)
                    .thisWeekLikes(0L)
                    .build();
        }
    }

    // ============ Private Helper Methods ============

    /**
     * 입력값 검증
     */
    private void validateInput(Long boardId, Long userId) {
        if (boardId == null) {
            throw new IllegalArgumentException("게시글 ID는 필수입니다.");
        }

        if (userId == null) {
            throw new IllegalArgumentException("사용자 ID는 필수입니다.");
        }
    }
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

    private boolean isAuthorDeleted(Long authorId) {
        try {
            return memberRepository.findIsDeletedByMemberId(authorId)
                    .orElse(false);
        } catch (Exception e) {
            log.warn("작성자 탈퇴 여부 조회 실패: authorId={}", authorId);
            return false;
        }
    }

    /**
     * BoardEntity를 BoardVO로 변환 (이미지 한 개 첨부 지원)
     */
    private BoardVO convertBoardToVO(BoardEntity entity) {
        // 이미지 파일 정보 조회
        Optional<FileUploadResponse> imageInfo = fileService.getBoardImageInfo(entity.getBoardId());

        return BoardVO.builder()
                .boardId(entity.getBoardId())
                .title(entity.getTitle())
                .authorId(entity.getAuthorId())
                .content(entity.getContent())
                .authorNickname(getAuthorNickname(entity.getAuthorId()))
                .authorDeleted(isAuthorDeleted(entity.getAuthorId()))
                .category(entity.getCategory())
                .viewCount(entity.getViewCount())
                .likeCount(entity.getLikeCount())
                .commentCount(entity.getCommentCount())
                .hasImage(imageInfo.isPresent())
                .imageUrl(imageInfo.map(FileUploadResponse::getFileUrl).orElse(null))
                .isNotice(entity.getIsNotice())
                .isReported(entity.getIsReported())
                .createdAt(entity.getCreatedAt())
                .updatedAt(entity.getUpdatedAt())
                .build();
    }
}
