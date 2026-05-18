package com.sp.cyberflashing.service;

import com.sp.community.model.dto.PageRequestDTO;
import com.sp.cyberflashing.model.dto.CyberFlashingSearchDTO;
import com.sp.cyberflashing.model.vo.CyberFlashingCaseListVO;
import com.sp.cyberflashing.model.vo.CyberFlashingCaseVO;
import com.sp.cyberflashing.persistent.entity.CyberFlashingCaseEntity;
import com.sp.cyberflashing.persistent.repository.CyberFlashingCaseRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Slf4j
@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class CyberFlashingService {

    private final CyberFlashingCaseRepository cyberFlashingCaseRepository;

    public CyberFlashingCaseListVO getCases(CyberFlashingSearchDTO searchDTO, PageRequestDTO pageRequestDTO) {
        CyberFlashingSearchDTO normalizedSearch = normalizeSearch(searchDTO);
        Pageable pageable = createPageable(pageRequestDTO);

        Page<CyberFlashingCaseEntity> page = cyberFlashingCaseRepository.search(
                normalizedSearch.getCountryCode(),
                normalizedSearch.getDuplicateFlag(),
                normalizedSearch.getIncludeFlag(),
                pageable
        );

        List<CyberFlashingCaseVO> items = page.getContent().stream()
                .map(this::toVO)
                .toList();

        return CyberFlashingCaseListVO.builder()
                .items(items)
                .pageInfo(CyberFlashingCaseListVO.PageInfoVO.builder()
                        .currentPage(page.getNumber() + 1)
                        .pageSize(page.getSize())
                        .totalElements(page.getTotalElements())
                        .totalPages(page.getTotalPages())
                        .hasNext(page.hasNext())
                        .hasPrevious(page.hasPrevious())
                        .isFirst(page.isFirst())
                        .isLast(page.isLast())
                        .build())
                .filterInfo(CyberFlashingCaseListVO.FilterInfoVO.builder()
                        .countryCode(normalizedSearch.getCountryCode())
                        .duplicateFlag(normalizedSearch.getDuplicateFlag())
                        .includeFlag(normalizedSearch.getIncludeFlag())
                        .build())
                .build();
    }

    public long getCaseCount(CyberFlashingSearchDTO searchDTO) {
        CyberFlashingSearchDTO normalizedSearch = normalizeSearch(searchDTO);
        return cyberFlashingCaseRepository.countByFilters(
                normalizedSearch.getCountryCode(),
                normalizedSearch.getDuplicateFlag(),
                normalizedSearch.getIncludeFlag()
        );
    }

    private CyberFlashingSearchDTO normalizeSearch(CyberFlashingSearchDTO searchDTO) {
        CyberFlashingSearchDTO normalizedSearch = searchDTO != null
                ? searchDTO
                : CyberFlashingSearchDTO.builder().build();
        normalizedSearch.normalize();
        return normalizedSearch;
    }

    private Pageable createPageable(PageRequestDTO pageRequestDTO) {
        PageRequestDTO pageDTO = pageRequestDTO != null
                ? pageRequestDTO
                : PageRequestDTO.builder().build();
        pageDTO.setDefaults();
        pageDTO.setSortBy("id");
        pageDTO.setDirection(PageRequestDTO.SortDirection.DESC);
        return pageDTO.toPageable();
    }

    private CyberFlashingCaseVO toVO(CyberFlashingCaseEntity entity) {
        return CyberFlashingCaseVO.builder()
                .id(entity.getId())
                .countryCode(entity.getCountryCode())
                .duplicateFlag(entity.getDuplicateFlag())
                .includeFlag(entity.getIncludeFlag())
                .editNote(entity.getEditNote())
                .articleTitle(entity.getArticleTitle())
                .content(entity.getContent())
                .url(entity.getUrl())
                .occurredDate(entity.getOccurredDate())
                .rssUrl(entity.getRssUrl())
                .press(entity.getPress())
                .reporter(entity.getReporter())
                .note(entity.getNote())
                .build();
    }
}
