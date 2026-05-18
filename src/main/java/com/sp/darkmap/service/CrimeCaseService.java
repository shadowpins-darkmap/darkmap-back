package com.sp.darkmap.service;

import com.sp.darkmap.model.dto.CrimeCaseSaveRequest;
import com.sp.darkmap.model.vo.CrimeCaseVO;
import com.sp.darkmap.persistent.entity.CrimeCaseEntity;
import com.sp.darkmap.persistent.repository.CrimeCaseRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Slf4j
@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class CrimeCaseService {

    private final CrimeCaseRepository crimeCaseRepository;

    @Transactional
    public CrimeCaseVO create(CrimeCaseSaveRequest request, Long reporterId) {
        CrimeCaseEntity entity = CrimeCaseEntity.builder()
                .infoType(request.getInfoType())
                .crimeType(request.getCrimeType())
                .sido(request.getSido().trim())
                .sigungu(request.getSigungu().trim())
                .newsUrl(normalize(request.getNewsUrl()))
                .latitude(request.getLatitude())
                .longitude(request.getLongitude())
                .reporterId(reporterId)
                .build();

        CrimeCaseEntity saved = crimeCaseRepository.save(entity);
        log.info("범죄 사례 등록: id={}, infoType={}, crimeType={}, reporterId={}",
                saved.getId(), saved.getInfoType(), saved.getCrimeType(), reporterId);

        return toVO(saved);
    }

    private String normalize(String value) {
        if (value == null) {
            return null;
        }
        String trimmed = value.trim();
        return trimmed.isEmpty() ? null : trimmed;
    }

    private CrimeCaseVO toVO(CrimeCaseEntity entity) {
        return CrimeCaseVO.builder()
                .id(entity.getId())
                .infoType(entity.getInfoType())
                .crimeType(entity.getCrimeType())
                .sido(entity.getSido())
                .sigungu(entity.getSigungu())
                .newsUrl(entity.getNewsUrl())
                .latitude(entity.getLatitude())
                .longitude(entity.getLongitude())
                .reporterId(entity.getReporterId())
                .createdAt(entity.getCreatedAt())
                .build();
    }
}
