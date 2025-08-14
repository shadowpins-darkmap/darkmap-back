package com.sp.community.model.dto;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import lombok.ToString;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;

/**
 * 페이징 요청 DTO
 */
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
@ToString
public class PageRequestDTO {

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
     * 정렬 필드
     */
    @Builder.Default
    private String sortBy = "createdAt";

    /**
     * 정렬 방향
     */
    @Builder.Default
    private SortDirection direction = SortDirection.DESC;

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

        /**
         * Spring Data Sort.Direction으로 변환
         */
        public Sort.Direction toSpringDirection() {
            return this == ASC ? Sort.Direction.ASC : Sort.Direction.DESC;
        }
    }

    /**
     * Spring Data Pageable로 변환
     */
    public Pageable toPageable() {
        return PageRequest.of(
                page != null ? page : 0,
                size != null ? size : 20,
                Sort.by(direction.toSpringDirection(), sortBy != null ? sortBy : "createdAt")
        );
    }

    /**
     * 다중 정렬 조건으로 Pageable 생성
     */
    public Pageable toPageable(String... additionalSortFields) {
        Sort sort = Sort.by(direction.toSpringDirection(), sortBy != null ? sortBy : "createdAt");

        if (additionalSortFields != null && additionalSortFields.length > 0) {
            for (String field : additionalSortFields) {
                sort = sort.and(Sort.by(direction.toSpringDirection(), field));
            }
        }

        return PageRequest.of(
                page != null ? page : 0,
                size != null ? size : 20,
                sort
        );
    }

    /**
     * 커스텀 정렬 조건으로 Pageable 생성
     */
    public Pageable toPageable(Sort customSort) {
        return PageRequest.of(
                page != null ? page : 0,
                size != null ? size : 20,
                customSort
        );
    }

    /**
     * 게시글 기본 정렬용 Pageable 생성
     */
    public Pageable toBoardPageable() {
        Sort sort;

        switch (sortBy != null ? sortBy : "createdAt") {
            case "likes":
            case "likeCount":
                sort = Sort.by(direction.toSpringDirection(), "likeCount")
                        .and(Sort.by(Sort.Direction.DESC, "createdAt"));
                break;
            case "views":
            case "viewCount":
                sort = Sort.by(direction.toSpringDirection(), "viewCount")
                        .and(Sort.by(Sort.Direction.DESC, "createdAt"));
                break;
            case "comments":
            case "commentCount":
                sort = Sort.by(direction.toSpringDirection(), "commentCount")
                        .and(Sort.by(Sort.Direction.DESC, "createdAt"));
                break;
            case "title":
                sort = Sort.by(direction.toSpringDirection(), "title")
                        .and(Sort.by(Sort.Direction.DESC, "createdAt"));
                break;
            case "author":
            case "authorNickname":
                sort = Sort.by(direction.toSpringDirection(), "authorNickname")
                        .and(Sort.by(Sort.Direction.DESC, "createdAt"));
                break;
            default:
                sort = Sort.by(direction.toSpringDirection(), "createdAt");
        }

        return PageRequest.of(page != null ? page : 0, size != null ? size : 20, sort);
    }

    /**
     * 댓글 기본 정렬용 Pageable 생성
     */
    public Pageable toCommentPageable() {
        Sort sort;

        switch (sortBy != null ? sortBy : "createdAt") {
            case "likes":
            case "likeCount":
                sort = Sort.by(direction.toSpringDirection(), "likeCount")
                        .and(Sort.by(Sort.Direction.ASC, "createdAt"));
                break;
            case "latest":
                sort = Sort.by(Sort.Direction.DESC, "createdAt");
                break;
            case "oldest":
                sort = Sort.by(Sort.Direction.ASC, "createdAt");
                break;
            default:
                // 댓글은 일반적으로 오래된 순으로 정렬
                sort = Sort.by(Sort.Direction.ASC, "createdAt");
        }

        return PageRequest.of(page != null ? page : 0, size != null ? size : 20, sort);
    }

    /**
     * 계층형 댓글 정렬용 Pageable 생성
     */
    public Pageable toHierarchicalCommentPageable() {
        // 계층형 댓글: 부모 댓글 ID 순 → 정렬 순서 순 → 생성 시간 순
        Sort sort = Sort.by(Sort.Direction.ASC, "parentComment.commentId")
                .and(Sort.by(Sort.Direction.ASC, "sortOrder"))
                .and(Sort.by(Sort.Direction.ASC, "createdAt"));

        return PageRequest.of(page != null ? page : 0, size != null ? size : 50, sort);
    }

    /**
     * 파일 정렬용 Pageable 생성
     */
    public Pageable toFilePageable() {
        Sort sort;

        switch (sortBy != null ? sortBy : "sortOrder") {
            case "name":
            case "fileName":
                sort = Sort.by(direction.toSpringDirection(), "originalFileName");
                break;
            case "size":
            case "fileSize":
                sort = Sort.by(direction.toSpringDirection(), "fileSize")
                        .and(Sort.by(Sort.Direction.ASC, "sortOrder"));
                break;
            case "type":
            case "fileType":
                sort = Sort.by(direction.toSpringDirection(), "fileType")
                        .and(Sort.by(Sort.Direction.ASC, "sortOrder"));
                break;
            case "downloads":
            case "downloadCount":
                sort = Sort.by(direction.toSpringDirection(), "downloadCount")
                        .and(Sort.by(Sort.Direction.ASC, "sortOrder"));
                break;
            case "created":
            case "createdAt":
                sort = Sort.by(direction.toSpringDirection(), "createdAt");
                break;
            default:
                sort = Sort.by(Sort.Direction.ASC, "sortOrder")
                        .and(Sort.by(Sort.Direction.ASC, "createdAt"));
        }

        return PageRequest.of(page != null ? page : 0, size != null ? size : 20, sort);
    }

    /**
     * 관리자용 신고 정렬 Pageable 생성
     */
    public Pageable toReportPageable() {
        Sort sort;

        switch (sortBy != null ? sortBy : "createdAt") {
            case "status":
                sort = Sort.by(direction.toSpringDirection(), "status")
                        .and(Sort.by(Sort.Direction.ASC, "createdAt"));
                break;
            case "type":
            case "reportType":
                sort = Sort.by(direction.toSpringDirection(), "reportType")
                        .and(Sort.by(Sort.Direction.ASC, "createdAt"));
                break;
            case "processed":
            case "processedAt":
                sort = Sort.by(direction.toSpringDirection(), "processedAt")
                        .and(Sort.by(Sort.Direction.ASC, "createdAt"));
                break;
            default:
                sort = Sort.by(direction.toSpringDirection(), "createdAt");
        }

        return PageRequest.of(page != null ? page : 0, size != null ? size : 20, sort);
    }

    /**
     * 다음 페이지 번호 반환
     */
    public int getNextPage() {
        return (page != null ? page : 0) + 1;
    }

    /**
     * 이전 페이지 번호 반환
     */
    public int getPreviousPage() {
        int currentPage = page != null ? page : 0;
        return Math.max(0, currentPage - 1);
    }

    /**
     * 첫 번째 페이지 여부 확인
     */
    public boolean isFirstPage() {
        return (page != null ? page : 0) == 0;
    }

    /**
     * 오프셋 계산
     */
    public long getOffset() {
        int currentPage = page != null ? page : 0;
        int currentSize = size != null ? size : 20;
        return (long) currentPage * currentSize;
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

        if (sortBy != null && sortBy.trim().isEmpty()) {
            throw new IllegalArgumentException("정렬 필드는 비어있을 수 없습니다.");
        }
    }

    /**
     * 기본값으로 초기화
     */
    public void setDefaults() {
        if (page == null || page < 0) {
            page = 0;
        }

        if (size == null || size < 1 || size > 100) {
            size = 20;
        }

        if (sortBy == null || sortBy.trim().isEmpty()) {
            sortBy = "createdAt";
        }

        if (direction == null) {
            direction = SortDirection.DESC;
        }
    }
}