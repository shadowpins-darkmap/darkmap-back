package com.sp.community.persistent.repository;

import com.sp.community.persistent.entity.BoardEntity;
import com.sp.community.persistent.entity.BoardFileEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface BoardFileRepository extends JpaRepository<BoardFileEntity, Long> {

    /**
     * 특정 게시글의 파일 조회 (한 개만 존재)
     */
    Optional<BoardFileEntity> findByBoard(BoardEntity board);

    /**
     * 특정 게시글의 파일 존재 여부 확인
     */
    boolean existsByBoard(BoardEntity board);

    /**
     * 특정 게시글의 파일 삭제
     */
    void deleteByBoard(BoardEntity board);
}