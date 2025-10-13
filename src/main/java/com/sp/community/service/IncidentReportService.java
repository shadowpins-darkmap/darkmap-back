package com.sp.community.service;

import com.sp.community.model.dto.IncidentReportCreateDTO;
import com.sp.community.model.response.FileUploadResponse;
import com.sp.community.model.vo.IncidentReportVO;
import com.sp.community.persistent.entity.IncidentReportEntity;
import com.sp.community.persistent.repository.IncidentReportRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;


/**
 * 제보 비즈니스 로직 Service (이미지 한 개 첨부 지원)
 */
@Slf4j
@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class IncidentReportService {

    private final IncidentReportRepository incidentReportRepository;
    private final FileService fileService;


    /**
     * 제보글 생성
     */
    @Transactional
    public IncidentReportVO createIncidentReport(IncidentReportCreateDTO createDTO) {
        log.info("제보글 생성 시작: {}", createDTO.getReportType());

        createDTO.validate();

        // IncidentReportEntity 빌더로 기본 정보 설정
        IncidentReportEntity.IncidentReportEntityBuilder builder = IncidentReportEntity.builder()
                .reportType(createDTO.getReportType())
                .reportLocation(createDTO.getReportLocation())
                .content(createDTO.getContent())
                .url(createDTO.getUrl())
                .reporterId(createDTO.getReporterId());

        IncidentReportEntity incidentReportEntity = builder.build();
        IncidentReportEntity savedIncidentReport = incidentReportRepository.save(incidentReportEntity);

        if (createDTO.hasImage()) {
            try {
                MultipartFile imageFile = createDTO.getImageFile();
                FileUploadResponse uploadResponse =
                        fileService.uploadImageForIncidentReport(savedIncidentReport.getId(), imageFile);
                log.info("게시글 이미지 업로드 완료: IncidentReportId={}, fileName={}",
                        savedIncidentReport.getId(), uploadResponse.getStoredFileName());
            } catch (Exception e) {
                log.error("게시글 이미지 업로드 실패: IncidentReportId={}", savedIncidentReport.getId(), e);
            }
        }

        return convertToVO(savedIncidentReport);
    }

    /**
     * Entity를 VO로 변환
     */
    private IncidentReportVO convertToVO(IncidentReportEntity entity) {
        if (entity == null) {
            return null;
        }

        return IncidentReportVO.builder()
                .id(entity.getId())
                .reportType(entity.getReportType())
                .reportLocation(entity.getReportLocation())
                .content(entity.getContent())
                .url(entity.getUrl())
                .reporterId(entity.getReporterId())
                .createdAt(entity.getCreatedAt())
                .build();
    }

}