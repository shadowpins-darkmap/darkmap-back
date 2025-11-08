package com.sp.community.model.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.Size;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import lombok.ToString;
import org.springframework.format.annotation.DateTimeFormat;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;

/**
 * 게시글 검색 요청 DTO
 */
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
@ToString
public class BoardSearchDTO {

    /**
     * 검색 키워드
     */
    @Schema(description = "검색 키워드")
    @Size(max = 100, message = "검색 키워드는 100자 이하로 입력해주세요.")
    private String keyword;

    /**
     * 검색 타입
     */
    @Builder.Default
    private SearchType searchType = SearchType.ALL;

    /**
     * 카테고리
     */
    @Schema(description = "카테고리")
    @Size(max = 50, message = "카테고리는 50자 이하로 입력해주세요.")
    private String category;

    /**
     * 작성자 ID
     */
    private Long authorId;

    /**
     * 작성자 닉네임
     */
    @Size(max = 50, message = "작성자 닉네임은 50자 이하로 입력해주세요.")
    private String authorNickname;

    /**
     * 검색 시작 날짜
     */
    @DateTimeFormat(pattern = "yyyy-MM-dd")
    private LocalDate startDate;

    /**
     * 검색 종료 날짜
     */
    @DateTimeFormat(pattern = "yyyy-MM-dd")
    private LocalDate endDate;

    /**
     * 공지사항 여부
     */
    private Boolean isNotice;

    /**
     * 신고된 게시글 여부
     */
    private Boolean isReported;

    /**
     * 정렬 기준
     */
    @Builder.Default
    private SortType sortType = SortType.LATEST;

    /**
     * 정렬 방향
     */
    @Builder.Default
    private SortDirection sortDirection = SortDirection.DESC;

    /**
     * 페이지 번호 (0부터 시작)
     */
    @Min(value = 0, message = "페이지 번호는 0 이상이어야 합니다.")
    @Builder.Default
    private Integer page = 0;

    /**
     * 페이지 크기
     */
    @Min(value = 1, message = "페이지 크기는 1 이상이어야 합니다.")
    @Max(value = 100, message = "페이지 크기는 100 이하여야 합니다.")
    @Builder.Default
    private Integer size = 20;

    /**
     * 검색 타입 Enum
     */
    public enum SearchType {
        ALL("전체"),
        TITLE("제목"),
        CONTENT("내용"),
        TITLE_CONTENT("제목+내용"),
        AUTHOR("작성자");

        private final String description;

        SearchType(String description) {
            this.description = description;
        }

        public String getDescription() {
            return description;
        }
    }

    /**
     * 정렬 타입 Enum
     */
    public enum SortType {
        LATEST("최신순"),
        OLDEST("오래된순"),
        LIKES("좋아요순"),
        VIEWS("조회수순"),
        COMMENTS("댓글수순"),
        TITLE("제목순");

        private final String description;

        SortType(String description) {
            this.description = description;
        }

        public String getDescription() {
            return description;
        }
    }

    /**
     * 정렬 방향 Enum
     */
    public enum SortDirection {
        ASC("오름차순"),
        DESC("내림차순");

        private final String description;

        SortDirection(String description) {
            this.description = description;
        }

        public String getDescription() {
            return description;
        }
    }

    /**
     * 키워드 검색 여부 확인
     */
    public boolean hasKeyword() {
        return keyword != null && !keyword.trim().isEmpty();
    }

    /**
     * 날짜 범위 검색 여부 확인
     */
    public boolean hasDateRange() {
        return startDate != null || endDate != null;
    }

    /**
     * 정리된 키워드 반환
     */
    public String getTrimmedKeyword() {
        return hasKeyword() ? keyword.trim() : null;
    }

    /**
     * 검색 시작 일시 반환 (LocalDateTime 변환)
     */
    public LocalDateTime getStartDateTime() {
        return startDate != null ? startDate.atStartOfDay() : null;
    }

    /**
     * 검색 종료 일시 반환 (LocalDateTime 변환)
     */
    public LocalDateTime getEndDateTime() {
        return endDate != null ? endDate.atTime(23, 59, 59) : null;
    }

    /**
     * 정리된 카테고리 반환
     */
    public String getNormalizedCategory() {
        if (category == null || category.trim().isEmpty()) {
            return null;
        }
        return category.trim().toLowerCase();
    }

    /**
     * 정리된 작성자 닉네임 반환
     */
    public String getTrimmedAuthorNickname() {
        if (authorNickname == null || authorNickname.trim().isEmpty()) {
            return null;
        }
        return authorNickname.trim();
    }

    /**
     * 검색 조건 존재 여부 확인
     */
    public boolean hasSearchConditions() {
        return hasKeyword() ||
                getNormalizedCategory() != null ||
                authorId != null ||
                getTrimmedAuthorNickname() != null ||
                hasDateRange() ||
                isNotice != null ||
                isReported != null;
    }

    /**
     * DTO 검증
     */
    public void validate() {
        if (page != null && page < 0) {
            throw new IllegalArgumentException("페이지 번호는 0 이상이어야 합니다.");
        }

        if (size != null && (size < 1 || size > 100)) {
            throw new IllegalArgumentException("페이지 크기는 1 이상 100 이하여야 합니다.");
        }

        if (startDate != null && endDate != null && startDate.isAfter(endDate)) {
            throw new IllegalArgumentException("시작 날짜는 종료 날짜보다 이후일 수 없습니다.");
        }
    }
}