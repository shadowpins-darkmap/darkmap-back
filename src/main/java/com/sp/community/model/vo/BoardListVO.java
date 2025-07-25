package com.sp.community.model.vo;

import com.fasterxml.jackson.annotation.JsonFormat;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import lombok.ToString;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;

/**
 * 게시글 목록 응답 VO
 */
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
@ToString
public class BoardListVO {

    /**
     * 게시글 목록
     */
    private List<BoardVO> boards;

    /**
     * 페이징 정보
     */
    private PageInfoVO pageInfo;

    /**
     * 검색 조건 정보
     */
    private SearchInfoVO searchInfo;

    /**
     * 공지사항 목록
     */
    private List<BoardVO> notices;

    /**
     * 인기 게시글 목록 (5개, 상단 캐롯셀)
     */
    private List<PopularBoardVO> popularBoards;

    /**
     * 카테고리별 게시글 수
     */
    private Map<String, Long> categoryStats;

    /**
     * 페이징 정보 VO
     */
    @Getter
    @Setter
    @NoArgsConstructor
    @AllArgsConstructor
    @Builder
    @ToString
    public static class PageInfoVO {
        private Integer currentPage;        // 현재 페이지 (0부터 시작)
        private Integer pageSize;           // 페이지 크기
        private Long totalElements;         // 전체 요소 수
        private Integer totalPages;         // 전체 페이지 수
        private Boolean hasNext;            // 다음 페이지 존재 여부
        private Boolean hasPrevious;        // 이전 페이지 존재 여부
        private Boolean isFirst;            // 첫 번째 페이지 여부
        private Boolean isLast;             // 마지막 페이지 여부
        private List<Integer> pageNumbers;  // 페이지 번호 목록 (페이지네이션용)

        /**
         * 시작 요소 번호 (1부터 시작)
         */
        public Long getStartElement() {
            if (currentPage == null || pageSize == null) {
                return 1L;
            }
            return (long) (currentPage * pageSize) + 1;
        }

        /**
         * 종료 요소 번호
         */
        public Long getEndElement() {
            if (currentPage == null || pageSize == null || totalElements == null) {
                return 0L;
            }
            return Math.min((long) ((currentPage + 1) * pageSize), totalElements);
        }

        /**
         * 페이지네이션 번호 목록 생성 (최대 10개)
         */
        public List<Integer> generatePageNumbers() {
            if (totalPages == null || totalPages <= 1) {
                return List.of();
            }

            int current = currentPage != null ? currentPage : 0;
            int total = totalPages;

            int start = Math.max(0, current - 4);
            int end = Math.min(total - 1, start + 9);

            // 끝에서 10개가 안 되면 시작 조정
            if (end - start < 9) {
                start = Math.max(0, end - 9);
            }

            return java.util.stream.IntStream.rangeClosed(start, end)
                    .boxed()
                    .toList();
        }
    }

    /**
     * 검색 조건 정보 VO
     */
    @Getter
    @Setter
    @NoArgsConstructor
    @AllArgsConstructor
    @Builder
    @ToString
    public static class SearchInfoVO {
        private String keyword;
        private String searchType;
        private String category;
        private String authorId;
        private String authorNickname;
        private List<String> tags;
        @JsonFormat(pattern = "yyyy-MM-dd")
        private LocalDateTime startDate;
        @JsonFormat(pattern = "yyyy-MM-dd")
        private LocalDateTime endDate;
        private String sortType;
        private String sortDirection;
        private Boolean hasFiles;
        private Boolean isNotice;

        /**
         * 검색 조건 존재 여부
         */
        public boolean hasSearchConditions() {
            return (keyword != null && !keyword.trim().isEmpty()) ||
                    (category != null && !category.trim().isEmpty()) ||
                    (authorId != null && !authorId.trim().isEmpty()) ||
                    (authorNickname != null && !authorNickname.trim().isEmpty()) ||
                    (tags != null && !tags.isEmpty()) ||
                    startDate != null ||
                    endDate != null ||
                    hasFiles != null ||
                    isNotice != null;
        }

        /**
         * 검색 조건 요약 문자열
         */
        public String getSearchSummary() {
            if (!hasSearchConditions()) {
                return "전체 게시글";
            }

            StringBuilder summary = new StringBuilder();

            if (keyword != null && !keyword.trim().isEmpty()) {
                summary.append("'").append(keyword).append("' 검색결과");
            }

            if (category != null && !category.trim().isEmpty()) {
                if (summary.length() > 0) summary.append(" | ");
                summary.append("카테고리: ").append(category);
            }

            if (tags != null && !tags.isEmpty()) {
                if (summary.length() > 0) summary.append(" | ");
                summary.append("태그: ").append(String.join(", ", tags));
            }

            return summary.toString();
        }
    }

    /**
     * 인기 게시글 VO
     */
    @Getter
    @Setter
    @NoArgsConstructor
    @AllArgsConstructor
    @Builder
    @ToString
    public static class PopularBoardVO {
        private Long boardId;
        private String title;
        private String authorNickname;
        private Integer likeCount;
        private Integer commentCount;
        private Integer viewCount;
        @JsonFormat(pattern = "yyyy-MM-dd HH:mm")
        private LocalDateTime createdAt;
        private String thumbnailUrl;
        private Integer rank; // 인기 순위
    }

    /**
     * 게시글 목록 존재 여부
     */
    public boolean hasBoards() {
        return boards != null && !boards.isEmpty();
    }

    /**
     * 공지사항 존재 여부
     */
    public boolean hasNotices() {
        return notices != null && !notices.isEmpty();
    }

    /**
     * 인기 게시글 존재 여부
     */
    public boolean hasPopularBoards() {
        return popularBoards != null && !popularBoards.isEmpty();
    }

    /**
     * 검색 결과 여부 확인
     */
    public boolean isSearchResult() {
        return searchInfo != null && searchInfo.hasSearchConditions();
    }

    /**
     * 페이징 필요 여부 확인
     */
    public boolean needsPagination() {
        return pageInfo != null &&
                pageInfo.getTotalPages() != null &&
                pageInfo.getTotalPages() > 1;
    }

    /**
     * 검색 결과 요약 문자열
     */
    public String getResultSummary() {
        if (pageInfo == null || pageInfo.getTotalElements() == null) {
            return "결과가 없습니다.";
        }

        long total = pageInfo.getTotalElements();

        if (isSearchResult()) {
            return String.format("검색 결과 %,d개 게시글", total);
        } else {
            return String.format("전체 %,d개 게시글", total);
        }
    }
}