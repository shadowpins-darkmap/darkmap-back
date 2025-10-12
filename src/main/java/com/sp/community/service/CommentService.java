package com.sp.community.service;

import com.sp.common.mail.model.dto.CommentReportInfoDto;
import com.sp.community.model.vo.BoardVO;
import com.sp.exception.BoardNotFoundException;
import com.sp.exception.CommentNotFoundException;
import com.sp.exception.UnauthorizedException;
import com.sp.community.model.dto.CommentCreateDTO;
import com.sp.community.model.dto.CommentUpdateDTO;
import com.sp.community.model.dto.PageRequestDTO;
import com.sp.community.model.vo.CommentVO;
import com.sp.community.persistent.entity.BoardEntity;
import com.sp.community.persistent.entity.CommentEntity;
import com.sp.community.persistent.entity.CommentLikeEntity;
import com.sp.community.persistent.repository.BoardRepository;
import com.sp.community.persistent.repository.CommentRepository;
import com.sp.community.persistent.repository.CommentLikeRepository;
import com.sp.member.persistent.repository.MemberRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

/**
 * 댓글 비즈니스 로직 Service
 */
@Slf4j
@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class CommentService {

    private final CommentRepository commentRepository;
    private final BoardRepository boardRepository;
    private final CommentLikeRepository commentLikeRepository;
    private final MemberRepository memberRepository;
    private final BoardService boardService;

    /**
     * 댓글 생성
     */
    @Transactional
    public CommentVO createComment(CommentCreateDTO createDTO, Long userId) {
        log.info("댓글 생성 시작: boardId={}, authorId={}", createDTO.getBoardId());

        // DTO 검증
        createDTO.validate();

        // 게시글 존재 확인
        BoardEntity boardEntity = boardRepository.findByIdAndNotDeleted(createDTO.getBoardId())
                .orElseThrow(() -> new BoardNotFoundException("게시글을 찾을 수 없습니다."));

        // 댓글 생성
        CommentEntity commentEntity = CommentEntity.builder()
                .board(boardEntity)
                .content(createDTO.getTrimmedContent())
                .authorId(userId)
                //.authorNickname(memberRepository.findNicknameByMemberId(userId)+"")
                .build();

        CommentEntity savedComment = commentRepository.save(commentEntity);

        // 게시글 댓글 수 증가
        boardRepository.incrementCommentCount(createDTO.getBoardId());

        log.info("댓글 생성 완료: commentId={}", savedComment.getCommentId());

        return convertToVO(savedComment, userId);
    }

    /**
     * 댓글 수정
     */
    @Transactional
    public CommentVO updateComment(CommentUpdateDTO updateDTO) {
        log.info("댓글 수정 시작: commentId={}", updateDTO.getCommentId());

        // DTO 검증
        updateDTO.validate();

        // 댓글 조회
        CommentEntity commentEntity = commentRepository.findById(updateDTO.getCommentId())
                .orElseThrow(() -> new CommentNotFoundException("댓글을 찾을 수 없습니다."));

        // 삭제된 댓글 확인
        if (commentEntity.getIsDeleted()) {
            throw new CommentNotFoundException("삭제된 댓글입니다.");
        }

        // 수정 권한 확인
        validateEditPermission(commentEntity, updateDTO.getAuthorId());

        // 댓글 내용 수정
        commentEntity.updateContent(updateDTO.getTrimmedContent());
        CommentEntity savedComment = commentRepository.save(commentEntity);

        log.info("댓글 수정 완료: commentId={}", savedComment.getCommentId());

        return convertToVO(savedComment, updateDTO.getAuthorId());
    }

    /**
     * 댓글 삭제 (소프트 삭제)
     */
    @Transactional
    public void deleteComment(Long commentId, Long currentUserId) {
        log.info("댓글 삭제 시작: commentId={}", commentId);

        // 댓글 조회
        CommentEntity commentEntity = commentRepository.findById(commentId)
                .orElseThrow(() -> new CommentNotFoundException("댓글을 찾을 수 없습니다."));

        // 이미 삭제된 댓글 확인
        if (commentEntity.getIsDeleted()) {
            throw new CommentNotFoundException("이미 삭제된 댓글입니다.");
        }

        // 삭제 권한 확인
        validateDeletePermission(commentEntity, currentUserId);

        // 소프트 삭제
        commentEntity.deleteComment();
        commentRepository.save(commentEntity);

        // 게시글 댓글 수 감소
        boardRepository.decrementCommentCount(commentEntity.getBoard().getBoardId());

        log.info("댓글 삭제 완료: commentId={}", commentId);
    }

    /**
     * 특정 게시글의 댓글 목록 조회
     */
    public List<CommentVO> getBoardComments(Long boardId, Long currentUserId, PageRequestDTO pageRequestDTO) {
        log.debug("게시글 댓글 목록 조회: boardId={}", boardId);

        // 게시글 존재 확인
        if (!boardRepository.existsByIdAndNotDeleted(boardId)) {
            throw new BoardNotFoundException("게시글을 찾을 수 없습니다.");
        }

        // 페이징 정보 설정
        if (pageRequestDTO != null) {
            pageRequestDTO.setDefaults();
        }

        Pageable pageable = pageRequestDTO != null ?
                pageRequestDTO.toCommentPageable() :
                PageRequestDTO.builder().build().toCommentPageable();

        // 댓글 조회 (보이는 댓글만)
        Page<CommentEntity> commentPage = commentRepository.findByBoardIdAndVisible(boardId, pageable);

        return commentPage.getContent().stream()
                .map(comment -> convertToVO(comment, currentUserId))
                .collect(Collectors.toList());
    }

    /**
     * 댓글 상세 조회
     */
    public CommentVO getComment(Long commentId, Long currentUserId) {
        log.debug("댓글 상세 조회: commentId={}", commentId);

        CommentEntity commentEntity = commentRepository.findById(commentId)
                .orElseThrow(() -> new CommentNotFoundException("댓글을 찾을 수 없습니다."));

        // 삭제되거나 숨겨진 댓글 확인
        if (!commentEntity.isVisible()) {
            throw new CommentNotFoundException("댓글을 볼 수 없습니다.");
        }

        return convertToVO(commentEntity, currentUserId);
    }

    /**
     * 사용자별 댓글 목록 조회
     */
    public List<CommentVO> getUserComments(Long authorId, Long currentUserId, PageRequestDTO pageRequestDTO) {
        log.debug("사용자 댓글 목록 조회: authorId={}", authorId);

        if (pageRequestDTO != null) {
            pageRequestDTO.setDefaults();
        }

        Pageable pageable = pageRequestDTO != null ?
                pageRequestDTO.toCommentPageable() :
                PageRequestDTO.builder().build().toCommentPageable();

        Page<CommentEntity> commentPage = commentRepository.findByAuthorIdAndVisible(authorId, pageable);

        return commentPage.getContent().stream()
                .map(comment -> convertToVO(comment, currentUserId))
                .collect(Collectors.toList());
    }

    /**
     * 댓글 좋아요 토글
     */
    @Transactional
    public boolean toggleCommentLike(Long commentId, Long userId) {
        log.info("댓글 좋아요 토글: commentId={}, userId={}", commentId, userId);

        // 입력값 검증
        validateLikeInput(commentId, userId);

        // 댓글 존재 확인
        CommentEntity commentEntity = commentRepository.findById(commentId)
                .orElseThrow(() -> new CommentNotFoundException("댓글을 찾을 수 없습니다."));

        // 자신의 댓글에는 좋아요할 수 없음
        if (commentEntity.getAuthorId().equals(userId)) {
            throw new UnauthorizedException("자신의 댓글에는 좋아요할 수 없습니다.");
        }

        // 기존 좋아요 확인
        Optional<CommentLikeEntity> existingLike = commentLikeRepository
                .findByCommentIdAndUserId(commentId, userId);

        if (existingLike.isPresent()) {
            CommentLikeEntity likeEntity = existingLike.get();

            if (likeEntity.isActive()) {
                // 좋아요 취소
                likeEntity.cancelLike();
                commentLikeRepository.save(likeEntity);
                commentRepository.decrementLikeCount(commentId);
                log.info("댓글 좋아요 취소: commentId={}, userId={}", commentId, userId);
                return false;
            } else {
                // 좋아요 복구
                likeEntity.restoreLike();
                commentLikeRepository.save(likeEntity);
                commentRepository.incrementLikeCount(commentId);
                log.info("댓글 좋아요 복구: commentId={}, userId={}", commentId, userId);
                return true;
            }
        }

        // 새로운 좋아요 생성
        CommentLikeEntity newLike = CommentLikeEntity.builder()
                .comment(commentEntity)
                .userId(userId)
                .build();

        commentLikeRepository.save(newLike);
        commentRepository.incrementLikeCount(commentId);

        log.info("댓글 좋아요 추가: commentId={}, userId={}", commentId, userId);
        return true;
    }

    /**
     * 댓글 좋아요 여부 확인
     */
    public boolean hasUserLikedComment(Long commentId, Long userId) {
        if (commentId == null || userId == null) {
            return false;
        }

        return commentLikeRepository.findByCommentIdAndUserIdAndNotDeleted(commentId, userId)
                .isPresent();
    }

    /**
     * 댓글 좋아요 수 조회
     */
    public Long getCommentLikeCount(Long commentId) {
        if (commentId == null) {
            return 0L;
        }

        return commentLikeRepository.countByCommentIdAndNotDeleted(commentId);
    }

    /**
     * 인기 댓글 조회
     */
    public List<CommentVO> getPopularComments(int minLikes, Long currentUserId, PageRequestDTO pageRequestDTO) {
        log.debug("인기 댓글 조회: minLikes={}", minLikes);

        if (pageRequestDTO != null) {
            pageRequestDTO.setDefaults();
        }

        Pageable pageable = pageRequestDTO != null ?
                pageRequestDTO.toCommentPageable() :
                PageRequestDTO.builder().build().toCommentPageable();

        Page<CommentEntity> popularComments = commentRepository.findPopularComments(minLikes, pageable);

        return popularComments.getContent().stream()
                .map(comment -> convertToVO(comment, currentUserId))
                .collect(Collectors.toList());
    }

    /**
     * 특정 게시글의 댓글 수 조회
     */
    public Long getBoardCommentCount(Long boardId) {
        return commentRepository.countByBoardIdAndVisible(boardId);
    }

    /**
     * 특정 사용자의 댓글 수 조회
     */
    public Long getMemberCommentCount(Long memberId) {
        return commentRepository.countByAuthorIdAndNotDeleted(memberId);
    }

    /**
     * 댓글 신고용 정보 조회 (VO 기반)
     * CommentVO와 BoardVO를 조합해서 신고에 필요한 정보를 DTO로 반환
     */
    @Transactional(readOnly = true)
    public Optional<CommentReportInfoDto> getCommentReportInfo(Long commentId) {
        try {
            // Entity 조회
            Optional<CommentEntity> commentEntityOpt = commentRepository.findById(commentId);

            if (commentEntityOpt.isEmpty()) {
                log.warn("댓글을 찾을 수 없습니다: commentId={}", commentId);
                return Optional.empty();
            }

            CommentEntity commentEntity = commentEntityOpt.get();

            // 게시글 정보 조회
            BoardEntity boardEntity = null;
            if (commentEntity.getBoard().getBoardId() != null) {
                boardEntity = boardRepository.findById(commentEntity.getBoard().getBoardId()).orElse(null);
            }

            // CommentReportInfoDto 생성
            CommentReportInfoDto dto = CommentReportInfoDto.builder()
                    .commentId(commentEntity.getCommentId())
                    .boardId(commentEntity.getBoard().getBoardId())
                    .commentContent(commentEntity.getContent())
                    .commentAuthorId(commentEntity.getAuthorId())
                    .commentAuthorNickname(getAuthorNickname(commentEntity.getAuthorId()))
                    .commentCreatedAt(commentEntity.getCreatedAt())
                    //.commentStatus(commentEntity.getStatus())
                    .isCommentReported(false) // 필요시 별도 조회
                    // 게시글 정보
                    .boardTitle(boardEntity != null ? boardEntity.getTitle() : "게시글 정보 없음")
                    //.boardCategory(boardEntity != null ? boardEntity.getCategory().name() : "UNKNOWN")
                    .boardAuthorNickname(boardEntity != null ? getAuthorNickname(boardEntity.getAuthorId()) : "알 수 없음")
                    //.boardStatus(boardEntity != null ? boardEntity.getStatus() : null)
                    .isBoardReported(false) // 필요시 별도 조회
                    .build();

            return Optional.of(dto);

        } catch (Exception e) {
            log.error("댓글 신고 정보 조회 중 오류 발생: commentId={}", commentId, e);
            return Optional.empty();
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

    // ============ Private Helper Methods ============

    /**
     * 수정 권한 확인
     */
    private void validateEditPermission(CommentEntity commentEntity, Long editorId) {
        if (!commentEntity.getAuthorId().equals(editorId)) {
            throw new UnauthorizedException("댓글 수정 권한이 없습니다.");
        }
    }

    /**
     * 삭제 권한 확인
     */
    private void validateDeletePermission(CommentEntity commentEntity, Long currentUserId) {
        if (!commentEntity.getAuthorId().equals(currentUserId)) {
            throw new UnauthorizedException("댓글 삭제 권한이 없습니다.");
        }
    }

    /**
     * 좋아요 입력값 검증
     */
    private void validateLikeInput(Long commentId, Long userId) {
        if (commentId == null) {
            throw new IllegalArgumentException("댓글 ID는 필수입니다.");
        }

        if (userId == null) {
            throw new IllegalArgumentException("사용자 ID는 필수입니다.");
        }
    }

    /**
     * Entity를 VO로 변환
     */
    private CommentVO convertToVO(CommentEntity entity, Long currentUserId) {
        CommentVO commentVO = CommentVO.builder()
                .commentId(entity.getCommentId())
                .boardId(entity.getBoard().getBoardId())
                .content(entity.getContent())
                .authorId(entity.getAuthorId())
                .authorNickname(getAuthorNickname(entity.getAuthorId()))
                .likeCount(entity.getLikeCount())
                .createdAt(entity.getCreatedAt())
                .updatedAt(entity.getUpdatedAt())
                .isAuthor(entity.getAuthorId().equals(currentUserId))
                .isReported(entity.getIsReported())
                .isHidden(entity.getIsHidden())
                .build();

        // 현재 사용자의 좋아요 여부 확인
        if (currentUserId != null) {
            boolean isLiked = hasUserLikedComment(entity.getCommentId(), currentUserId);
            commentVO.setIsLiked(isLiked);
        }

        // 댓글 상태 설정
        if (entity.getIsDeleted()) {
            commentVO.setStatus(CommentVO.CommentStatus.DELETED);
        } else if (entity.getIsHidden()) {
            commentVO.setStatus(CommentVO.CommentStatus.HIDDEN);
        } else if (entity.getIsReported()) {
            commentVO.setStatus(CommentVO.CommentStatus.REPORTED);
        } else {
            commentVO.setStatus(CommentVO.CommentStatus.ACTIVE);
        }

        return commentVO;
    }

}