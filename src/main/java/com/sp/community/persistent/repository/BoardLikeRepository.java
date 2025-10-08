package com.sp.community.persistent.repository;

import com.sp.community.persistent.entity.BoardLikeEntity;
import com.sp.community.persistent.entity.CommentEntity;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

/**
 * 게시글 좋아요 Repository
 */
@Repository
public interface BoardLikeRepository extends JpaRepository<BoardLikeEntity, Long> {
    /**
     * 특정 사용자의 게시글에 달린 새 좋아요 수 조회 (특정 시간 이후)
     */
    @Query("""
        SELECT COUNT(bl) 
        FROM BoardLikeEntity bl 
        JOIN bl.board b 
        WHERE b.authorId = :authorId 
        AND bl.createdAt >= :since 
        AND bl.isDeleted = false 
        AND b.isDeleted = false
        """)
    Long countNewLikesOnUserBoards(@Param("authorId") Long authorId,
                                   @Param("since") LocalDateTime since);

    /**
     * 특정 사용자의 게시글에 달린 새 좋아요 목록 조회 (특정 시간 이후)
     */
    @Query("""
        SELECT bl 
        FROM BoardLikeEntity bl 
        JOIN FETCH bl.board b 
        WHERE b.authorId = :authorId 
        AND bl.createdAt >= :since 
        AND bl.isDeleted = false 
        AND b.isDeleted = false 
        ORDER BY bl.createdAt DESC
        """)
    Page<BoardLikeEntity> findNewLikesOnUserBoards(@Param("authorId") Long authorId,
                                                   @Param("since") LocalDateTime since,
                                                   Pageable pageable);

    /**
     * 특정 게시글의 특정 사용자 좋아요 조회 (활성 상태)
     */
    @Query("SELECT bl FROM BoardLikeEntity bl WHERE bl.board.boardId = :boardId AND bl.userId = :userId AND bl.isDeleted = false")
    Optional<BoardLikeEntity> findByBoardIdAndUserIdAndNotDeleted(@Param("boardId") Long boardId, @Param("userId") Long userId);

    /**
     * 특정 게시글의 특정 사용자 좋아요 조회 (삭제된 것 포함)
     */
    @Query("SELECT bl FROM BoardLikeEntity bl WHERE bl.board.boardId = :boardId AND bl.userId = :userId")
    Optional<BoardLikeEntity> findByBoardIdAndUserId(@Param("boardId") Long boardId, @Param("userId") Long userId);

    /**
     * 특정 게시글의 좋아요 수 조회 (활성 상태만)
     */
    @Query("SELECT COUNT(bl) FROM BoardLikeEntity bl WHERE bl.board.boardId = :boardId AND bl.isDeleted = false")
    Long countByBoardIdAndNotDeleted(@Param("boardId") Long boardId);

    /**
     * 특정 게시글의 좋아요 목록 조회 (활성 상태, 페이징)
     */
    @Query("SELECT bl FROM BoardLikeEntity bl WHERE bl.board.boardId = :boardId AND bl.isDeleted = false ORDER BY bl.createdAt DESC")
    Page<BoardLikeEntity> findByBoardIdAndNotDeleted(@Param("boardId") Long boardId, Pageable pageable);

    /**
     * 특정 사용자가 좋아요한 게시글 목록 조회 (활성 상태)
     */
    @Query("SELECT bl FROM BoardLikeEntity bl WHERE bl.userId = :userId AND bl.isDeleted = false ORDER BY bl.createdAt DESC")
    Page<BoardLikeEntity> findByUserIdAndNotDeleted(@Param("userId") Long userId, Pageable pageable);

    /**
     * 사용자가 좋아요를 눌렀는지 확인
     */
    @Query("SELECT COUNT(bl) > 0 FROM BoardLikeEntity bl WHERE bl.board.boardId = :boardId AND bl.userId = :userId AND bl.isDeleted = false")
    boolean existsByBoardIdAndUserIdAndNotDeleted(@Param("boardId") Long boardId, @Param("userId") Long userId);

    /**
     * 특정 기간 내 좋아요 목록 조회
     */
    @Query("SELECT bl FROM BoardLikeEntity bl WHERE bl.createdAt BETWEEN :startDate AND :endDate AND bl.isDeleted = false ORDER BY bl.createdAt DESC")
    Page<BoardLikeEntity> findByCreatedAtBetweenAndNotDeleted(@Param("startDate") LocalDateTime startDate, @Param("endDate") LocalDateTime endDate, Pageable pageable);

    /**
     * 가장 많은 좋아요를 받은 게시글들의 좋아요 목록 조회
     */
    @Query("SELECT bl FROM BoardLikeEntity bl WHERE bl.isDeleted = false GROUP BY bl.board ORDER BY COUNT(bl) DESC")
    List<BoardLikeEntity> findMostLikedBoardLikes(Pageable pageable);

    /**
     * 특정 사용자의 좋아요 수 조회
     */
    @Query("SELECT COUNT(bl) FROM BoardLikeEntity bl WHERE bl.userId = :userId AND bl.isDeleted = false")
    Long countByUserIdAndNotDeleted(@Param("userId") Long userId);

    /**
     * 특정 게시글의 최근 좋아요 목록 조회 (제한된 개수)
     */
    @Query("SELECT bl FROM BoardLikeEntity bl WHERE bl.board.boardId = :boardId AND bl.isDeleted = false ORDER BY bl.createdAt DESC")
    List<BoardLikeEntity> findRecentLikesByBoardId(@Param("boardId") Long boardId, Pageable pageable);

    /**
     * 좋아요 취소 (소프트 삭제)
     */
    @Modifying
    @Query("UPDATE BoardLikeEntity bl SET bl.isDeleted = true, bl.deletedAt = CURRENT_TIMESTAMP WHERE bl.board.boardId = :boardId AND bl.userId = :userId")
    int cancelLikeByBoardIdAndUserId(@Param("boardId") Long boardId, @Param("userId") Long userId);

    /**
     * 좋아요 복구
     */
    @Modifying
    @Query("UPDATE BoardLikeEntity bl SET bl.isDeleted = false, bl.deletedAt = null WHERE bl.board.boardId = :boardId AND bl.userId = :userId")
    int restoreLikeByBoardIdAndUserId(@Param("boardId") Long boardId, @Param("userId") Long userId);

    /**
     * 특정 게시글의 모든 좋아요 삭제 (물리적 삭제)
     */
    @Modifying
    @Query("DELETE FROM BoardLikeEntity bl WHERE bl.board.boardId = :boardId")
    int deleteAllByBoardId(@Param("boardId") Long boardId);

    /**
     * 특정 사용자의 모든 좋아요 조회
     */
    @Query("SELECT bl FROM BoardLikeEntity bl WHERE bl.userId = :userId ORDER BY bl.createdAt DESC")
    List<BoardLikeEntity> findAllByUserId(@Param("userId") Long userId);

    /**
     * 오늘 좋아요한 개수 조회
     */
    @Query("SELECT COUNT(bl) FROM BoardLikeEntity bl WHERE DATE(bl.createdAt) = CURRENT_DATE AND bl.isDeleted = false")
    Long countTodayLikes();

    /**
     * 특정 기간 내 좋아요 개수 조회
     */
    @Query("SELECT COUNT(bl) FROM BoardLikeEntity bl WHERE bl.createdAt BETWEEN :startDate AND :endDate AND bl.isDeleted = false")
    Long countByCreatedAtBetweenAndNotDeleted(@Param("startDate") LocalDateTime startDate, @Param("endDate") LocalDateTime endDate);

    /**
     * 게시글별 좋아요 수 통계 조회
     */
    @Query("SELECT bl.board.boardId, COUNT(bl) FROM BoardLikeEntity bl WHERE bl.isDeleted = false GROUP BY bl.board.boardId ORDER BY COUNT(bl) DESC")
    List<Object[]> findLikeCountStatsByBoard();

    /**
     * 사용자별 좋아요 수 통계 조회
     */
    @Query("SELECT bl.userId, COUNT(bl) FROM BoardLikeEntity bl WHERE bl.isDeleted = false GROUP BY bl.userId ORDER BY COUNT(bl) DESC")
    List<Object[]> findLikeCountStatsByUser();

    /**
     * 특정 게시글의 좋아요한 사용자 ID 목록 조회
     */
    @Query("SELECT bl.userId FROM BoardLikeEntity bl WHERE bl.board.boardId = :boardId AND bl.isDeleted = false ORDER BY bl.createdAt DESC")
    List<String> findUserIdsByBoardIdAndNotDeleted(@Param("boardId") Long boardId);


    /**
     * 최근 일주일 내 가장 인기 있는 게시글의 좋아요 조회
     */
    @Query("SELECT bl FROM BoardLikeEntity bl WHERE bl.createdAt >= :oneWeekAgo AND bl.isDeleted = false GROUP BY bl.board ORDER BY COUNT(bl) DESC")
    List<BoardLikeEntity> findWeeklyPopularBoardLikes(@Param("oneWeekAgo") LocalDateTime oneWeekAgo, Pageable pageable);
}