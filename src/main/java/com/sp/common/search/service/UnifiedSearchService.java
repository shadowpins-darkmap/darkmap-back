package com.sp.common.search.service;

import com.sp.common.search.model.dto.UnifiedSearchResponseDTO;
import com.sp.common.search.model.dto.UnifiedSearchResultDTO;
import com.sp.community.persistent.entity.BoardEntity;
import com.sp.community.persistent.repository.BoardRepository;
import com.sp.darkmap.persistent.entity.Article;
import com.sp.darkmap.persistent.repository.ArticleRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class UnifiedSearchService {


    private final ArticleRepository articleRepository;
    private final BoardRepository boardRepository;

    public UnifiedSearchResponseDTO unifiedSearch(String keyword, Pageable pageable) {
        List<UnifiedSearchResultDTO> results = new ArrayList<>();

        // Article 검색
        List<Article> articles = articleRepository.searchByKeyword(keyword);
        long newsTotalElements = articles.size();
        results.addAll(articles.stream()
                .map(this::convertArticleToDTO)
                .collect(Collectors.toList()));

        // Board 검색
        List<BoardEntity> boards = boardRepository.searchByKeyword(keyword);
        long communityTotalElements = boards.size();
        results.addAll(boards.stream()
                .map(this::convertBoardToDTO)
                .collect(Collectors.toList()));

        // 최신순 정렬 (Board의 createdAt, Article의 contributionDate 기준)
        results.sort(Comparator.comparing(this::getDateTime).reversed());

        // 페이징 처리
        int start = (int) pageable.getOffset();
        int end = Math.min((start + pageable.getPageSize()), results.size());

        List<UnifiedSearchResultDTO> pagedResults = start >= results.size()
                ? List.of()
                : results.subList(start, end);

        long totalElements = results.size();
        int totalPages = pageable.getPageSize() > 0
                ? (int) Math.ceil((double) totalElements / pageable.getPageSize())
                : 0;

        return UnifiedSearchResponseDTO.builder()
                .results(pagedResults)
                .totalElements(totalElements)
                .newsTotalElements(newsTotalElements)
                .communityTotalElements(communityTotalElements)
                .totalPages(totalPages)
                .page(pageable.getPageNumber() + 1)
                .size(pageable.getPageSize())
                .build();
    }

    // 정렬을 위한 날짜 추출 메서드
    private LocalDateTime getDateTime(UnifiedSearchResultDTO dto) {
        if ("BOARD".equals(dto.getResultType())) {
            return dto.getCreatedAt();
        } else { // "ARTICLE"
            // contributionDate를 LocalDateTime으로 변환
            // contributionDate 형식에 맞게 수정 필요 (예: "2024-01-15" 형식이라고 가정)
            try {
                return LocalDateTime.parse(dto.getContributionDate() + "T00:00:00");
            } catch (Exception e) {
                return LocalDateTime.MIN; // 파싱 실패시 가장 오래된 날짜로 처리
            }
        }
    }

    private UnifiedSearchResultDTO convertArticleToDTO(Article article) {
        return UnifiedSearchResultDTO.builder()
                .resultType("ARTICLE")
                .id(article.getArticleId())
                .title(article.getTitle())
                .press(article.getPress())
                .reporter(article.getReporter())
                .crimeType(article.getCrimeType())
                .url(article.getUrl())
                .contributionDate(article.getContributionDate())
                .build();
    }

    private UnifiedSearchResultDTO convertBoardToDTO(BoardEntity board) {
        return UnifiedSearchResultDTO.builder()
                .resultType("BOARD")
                .id(board.getBoardId())
                .title(board.getTitle())
                .content(board.getContent())
                .category(board.getCategory())
                .url(null)
                .createdAt(board.getCreatedAt())
                .viewCount(board.getViewCount())
                .likeCount(board.getLikeCount())
                .build();
    }
}
