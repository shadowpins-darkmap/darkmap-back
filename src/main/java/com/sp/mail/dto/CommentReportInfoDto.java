package com.sp.mail.dto;

import com.sp.community.model.vo.BoardVO;
import com.sp.community.model.vo.CommentVO;
import lombok.Builder;
import lombok.Getter;

import java.time.LocalDateTime;

/**
 * 댓글 신고용 정보 DTO
 */
@Getter
@Builder
public class CommentReportInfoDto {
    // 댓글 정보
    private Long commentId;
    private Long boardId;
    private String commentContent;
    private Long commentAuthorId;
    private String commentAuthorNickname;
    private LocalDateTime commentCreatedAt;
    private Boolean isCommentReported;
    private CommentVO.CommentStatus commentStatus;

    // 게시글 정보
    private String boardTitle;
    private String boardCategory;
    private String boardAuthorNickname;
    private String reportType;        // INCIDENTREPORT 카테고리인 경우
    private String reportLocation;    // INCIDENTREPORT 카테고리인 경우
    private Boolean isBoardReported;
    private BoardVO.BoardStatus boardStatus;

    /**
     * 게시글이 제보 카테고리인지 확인
     */
    public boolean isIncidentReportBoard() {
        return "INCIDENTREPORT".equals(boardCategory);
    }

    /**
     * 게시글 제목 또는 기본값 반환
     */
    public String getBoardTitleOrDefault() {
        return boardTitle != null ? boardTitle : "게시글 정보 없음";
    }
}
