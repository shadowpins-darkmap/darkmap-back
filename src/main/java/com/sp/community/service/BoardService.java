package com.sp.community.service;

import com.sp.exception.BoardNotFoundException;
import com.sp.exception.UnauthorizedException;
import com.sp.community.model.dto.BoardCreateDTO;
import com.sp.community.model.dto.BoardSearchDTO;
import com.sp.community.model.dto.BoardUpdateDTO;
import com.sp.community.model.dto.PageRequestDTO;
import com.sp.community.model.vo.BoardDetailVO;
import com.sp.community.model.vo.BoardListVO;
import com.sp.community.model.vo.BoardVO;
import com.sp.community.persistent.entity.BoardEntity;
import com.sp.community.persistent.repository.BoardRepository;
import com.sp.community.persistent.repository.BoardLikeRepository;
import com.sp.community.persistent.repository.CommentRepository;
import com.sp.community.model.response.FileUploadResponse;
import com.sp.member.persistent.repository.MemberRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;
import org.springframework.web.multipart.MultipartFile;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

/**
 * 게시글 비즈니스 로직 Service (이미지 한 개 첨부 지원)
 */
@Slf4j
@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class BoardService {

    private final BoardRepository boardRepository;
    private final BoardLikeRepository boardLikeRepository;
    private final CommentRepository commentRepository;
    private final FileService fileService;
    private final BoardLikeService boardLikeService;
    private final MemberRepository memberRepository;

    /**
     * 전체 게시글 개수 조회
     */
    public Long getTotalBoardCount() {
        log.debug("전체 게시글 개수 조회");
        return boardRepository.countAllNotDeleted();
    }

    /**
     * 사건제보 카테고리 게시글 개수 조회
     */
    public Long getIncidentReportBoardCount() {
        log.debug("사건제보 카테고리 게시글 개수 조회");
        return boardRepository.countByCategoryAndNotDeleted(BoardEntity.CATEGORY_INCIDENTREPORT);
    }

    /**
     * 특정 유저가 작성한 승인된 제보글 개수 조회
     *
     * @param authorId 작성자 ID
     * @return 승인된 제보글 개수
     */
    public Long getApprovedReportCountByAuthor(String authorId) {
        if (authorId == null || authorId.trim().isEmpty()) {
            throw new IllegalArgumentException("작성자 ID는 필수입니다.");
        }

        try {
            Long count = boardRepository.countApprovedReportsByAuthor(
                    authorId, BoardEntity.CATEGORY_INCIDENTREPORT);

            log.info("사용자 [{}]의 승인된 제보글 개수: {}", authorId, count);
            return count != null ? count : 0L;

        } catch (Exception e) {
            log.error("승인된 제보글 개수 조회 중 오류 발생 - 사용자: {}", authorId, e);
            throw new RuntimeException("승인된 제보글 개수 조회에 실패했습니다.", e);
        }
    }

    /**
     * 게시글 생성
     */
    @Transactional
    public BoardVO createBoard(BoardCreateDTO createDTO) {
        log.info("게시글 생성 시작: {}", createDTO.getTitle());

        createDTO.validate();

        String nickname = memberRepository.findNicknameByMemberId(createDTO.getAuthorId())
                .orElse(createDTO.getAuthorId());

        // BoardEntity 빌더로 기본 정보 설정
        BoardEntity.BoardEntityBuilder builder = BoardEntity.builder()
                .title(createDTO.getTrimmedTitle())
                .content(createDTO.getTrimmedContent())
                .authorId(createDTO.getAuthorId())
                .authorNickname(nickname)
                .category(createDTO.getNormalizedCategory())
                .isNotice(createDTO.getIsNotice());

        // INCIDENTREPORT 카테고리인 경우 제보 정보 추가
        if (createDTO.isIncidentReportCategory()) {
            builder.reportType(createDTO.getTrimmedReportType())
                    .reportLocation(createDTO.getTrimmedReportLocation());
        }

        BoardEntity boardEntity = builder.build();
        BoardEntity savedBoard = boardRepository.save(boardEntity);


        if (createDTO.hasImage()) {
            try {
                MultipartFile imageFile = createDTO.getImageFile();
                FileUploadResponse uploadResponse =
                        fileService.uploadImageForBoard(savedBoard.getBoardId(), imageFile);
                log.info("게시글 이미지 업로드 완료: boardId={}, fileName={}",
                        savedBoard.getBoardId(), uploadResponse.getStoredFileName());
            } catch (Exception e) {
                log.error("게시글 이미지 업로드 실패: boardId={}", savedBoard.getBoardId(), e);
            }
        }

        return convertToVO(savedBoard);
    }

    /**
     * 게시글 수정
     */
    @Transactional
    public BoardVO updateBoard(BoardUpdateDTO updateDTO) {
        log.info("게시글 수정 시작: ID={}", updateDTO.getBoardId());

        updateDTO.validate();

        BoardEntity boardEntity = boardRepository.findByIdAndNotDeleted(updateDTO.getBoardId())

                .orElseThrow(() -> new BoardNotFoundException("게시글을 찾을 수 없습니다."));
        validateEditPermission(boardEntity, updateDTO.getEditorId());

        // INCIDENTREPORT 카테고리인 경우 제보 전용 업데이트 메서드 사용
        if (updateDTO.isIncidentReportCategory()) {
            boardEntity.updateReportBoard(
                    updateDTO.getTrimmedTitle(),
                    updateDTO.getTrimmedContent(),
                    updateDTO.getNormalizedCategory(),
                    updateDTO.getTrimmedReportType(),
                    updateDTO.getTrimmedReportLocation()
            );
        } else {
            boardEntity.updateBoard(
                    updateDTO.getTrimmedTitle(),
                    updateDTO.getTrimmedContent(),
                    updateDTO.getNormalizedCategory()
            );
        }

        if (updateDTO.getIsNotice() != null) {
            boardEntity.setIsNotice(updateDTO.getIsNotice());
        }

        processImageChanges(boardEntity, updateDTO);

        BoardEntity savedBoard = boardRepository.save(boardEntity);
        log.info("게시글 수정 완료: ID={}", savedBoard.getBoardId());
        return convertToVO(savedBoard);
    }

    /**
     * 게시글 상세 조회
     */
    @Transactional
    public BoardDetailVO getBoardDetail(Long boardId, String currentUserId) {
        log.debug("게시글 상세 조회: ID={}", boardId);
        BoardEntity boardEntity = boardRepository.findByIdAndNotDeleted(boardId)
                .orElseThrow(() -> new BoardNotFoundException("게시글을 찾을 수 없습니다."));
        boardRepository.incrementViewCount(boardId);
        boardEntity.incrementViewCount();
        BoardDetailVO detailVO = convertToDetailVO(boardEntity, currentUserId);
        return detailVO;
    }

    /**
     * 게시글 목록 조회
     */
    public BoardListVO getBoardList(BoardSearchDTO searchDTO, PageRequestDTO pageRequestDTO) {
        log.debug("게시글 목록 조회: {}", searchDTO);
        if (searchDTO != null) {
            searchDTO.validate();
        }
        if (pageRequestDTO != null) {
            pageRequestDTO.setDefaults();
        }
        Pageable pageable = pageRequestDTO != null ?
                pageRequestDTO.toBoardPageable() :
                PageRequestDTO.builder().build().toBoardPageable();
        Page<BoardEntity> boardPage = searchBoards(searchDTO, pageable);
        List<BoardVO> boardVOs = boardPage.getContent().stream()
                .map(this::convertToVO)
                .collect(Collectors.toList());
        return BoardListVO.builder()
                .boards(boardVOs)
                .pageInfo(createPageInfo(boardPage))
                .searchInfo(createSearchInfo(searchDTO))
                .build();
    }

    /**
     * 게시글 삭제 (소프트 삭제)
     */
    @Transactional
    public void deleteBoard(Long boardId, String currentUserId) {
        log.info("게시글 삭제 시작: ID={}", boardId);
        BoardEntity boardEntity = boardRepository.findByIdAndNotDeleted(boardId)
                .orElseThrow(() -> new BoardNotFoundException("게시글을 찾을 수 없습니다."));
        validateDeletePermission(boardEntity, currentUserId);
        try {
            fileService.deleteBoardImage(boardId);
            log.info("게시글 이미지 삭제 완료: boardId={}", boardId);
        } catch (Exception e) {
            log.warn("게시글 이미지 삭제 실패 (계속 진행): boardId={}", boardId, e);
        }
        boardEntity.softDelete();
        commentRepository.deleteAllByBoardId(boardId);

        boardRepository.save(boardEntity);

        log.info("게시글 삭제 완료: ID={}", boardId);
    }

    /**
     * 게시글 이미지만 삭제
     */
    @Transactional
    public void deleteBoardImage(Long boardId, String currentUserId) {
        log.info("게시글 이미지 삭제 시작: boardId={}", boardId);
        BoardEntity boardEntity = boardRepository.findByIdAndNotDeleted(boardId)
                .orElseThrow(() -> new BoardNotFoundException("게시글을 찾을 수 없습니다."));

        validateEditPermission(boardEntity, currentUserId);
        fileService.deleteBoardImage(boardId);

        log.info("게시글 이미지 삭제 완료: boardId={}", boardId);
    }

    /**
     * 인기 게시글 조회
     */
    public List<BoardVO> getPopularBoards(int limit) {
        LocalDateTime oneWeekAgo = LocalDateTime.now().minusDays(7);

        Pageable pageable = PageRequestDTO.builder()
                .size(limit)
                .build()
                .toBoardPageable();

        Page<BoardEntity> popularBoards = boardRepository.findWeeklyPopularBoards(oneWeekAgo, pageable);

        return popularBoards.getContent().stream()
                .map(this::convertToVO)
                .collect(Collectors.toList());
    }

    /**
     * 최근 게시글 조회
     */
    public List<BoardVO> getRecentBoards(int limit) {
        Pageable pageable = PageRequestDTO.builder()
                .size(limit)
                .sortBy("createdAt")
                .direction(PageRequestDTO.SortDirection.DESC)
                .build()
                .toBoardPageable();

        Page<BoardEntity> recentBoards = boardRepository.findAllNotDeleted(pageable);

        return recentBoards.getContent().stream()
                .map(this::convertToVO)
                .collect(Collectors.toList());
    }

    /**
     * 사용자별 게시글 조회
     */
    public BoardListVO getUserBoards(String authorId, PageRequestDTO pageRequestDTO) {
        log.debug("사용자 게시글 조회: authorId={}", authorId);

        if (pageRequestDTO != null) {
            pageRequestDTO.setDefaults();
        }

        Pageable pageable = pageRequestDTO != null ?
                pageRequestDTO.toBoardPageable() :
                PageRequestDTO.builder().build().toBoardPageable();

        Page<BoardEntity> boardPage = boardRepository.findByAuthorIdAndNotDeleted(authorId, pageable);

        List<BoardVO> boardVOs = boardPage.getContent().stream()
                .map(this::convertToVO)
                .collect(Collectors.toList());

        return BoardListVO.builder()
                .boards(boardVOs)
                .pageInfo(createPageInfo(boardPage))
                .build();
    }

    /**
     * 게시글 존재 여부 확인
     */
    public boolean existsBoard(Long boardId) {
        return boardRepository.existsByIdAndNotDeleted(boardId);
    }

    /**
     * 게시글 간단 정보 조회 (권한 확인 없음)
     */
    public Optional<BoardVO> getBoardSimple(Long boardId) {
        return boardRepository.findByIdAndNotDeleted(boardId)
                .map(this::convertToVO);
    }

    // ============ Private Helper Methods ============

    /**
     * 검색 조건에 따른 게시글 조회
     */
    private Page<BoardEntity> searchBoards(BoardSearchDTO searchDTO, Pageable pageable) {
        if (searchDTO == null || !searchDTO.hasSearchConditions()) {
            return boardRepository.findAllNotDeleted(pageable);
        }

        // 키워드 검색
        if (searchDTO.hasKeyword()) {
            String keyword = searchDTO.getTrimmedKeyword();

            return switch (searchDTO.getSearchType()) {
                case TITLE -> boardRepository.findByTitleContainingAndNotDeleted(keyword, pageable);
                case CONTENT -> boardRepository.findByContentContainingAndNotDeleted(keyword, pageable);
                case AUTHOR -> boardRepository.findByAuthorNicknameContainingAndNotDeleted(keyword, pageable);
                default -> boardRepository.findByTitleOrContentContainingAndNotDeleted(keyword, pageable);
            };
        }

        return boardRepository.findAllNotDeleted(pageable);
    }

    /**
     * 이미지 변경사항 처리
     */
    private void processImageChanges(BoardEntity boardEntity, BoardUpdateDTO updateDTO) {
        try {
            // 기존 이미지 삭제 요청 처리
            if (updateDTO.isDeleteImage()) {
                fileService.deleteBoardImage(boardEntity.getBoardId());
                log.info("기존 이미지 삭제 완료: boardId={}", boardEntity.getBoardId());
            }

            // 새 이미지 업로드 처리
            if (updateDTO.hasNewImage()) {
                MultipartFile newImageFile = updateDTO.getNewImageFile();
                FileUploadResponse uploadResponse = fileService.uploadImageForBoard(
                        boardEntity.getBoardId(), newImageFile);
                log.info("새 이미지 업로드 완료: boardId={}, fileName={}",
                        boardEntity.getBoardId(), uploadResponse.getStoredFileName());
            }
        } catch (Exception e) {
            log.error("이미지 변경 처리 실패: boardId={}", boardEntity.getBoardId(), e);
            // 이미지 처리 실패 시 예외를 던져서 전체 업데이트를 롤백할지,
            // 아니면 로그만 남기고 계속 진행할지는 비즈니스 요구사항에 따라 결정
            throw new RuntimeException("이미지 처리 중 오류가 발생했습니다.", e);
        }
    }

    /**
     * 수정 권한 확인
     */
    private void validateEditPermission(BoardEntity boardEntity, String editorId) {
        if (!boardEntity.getAuthorId().equals(editorId)) {
            throw new UnauthorizedException("게시글 수정 권한이 없습니다.");
        }
    }

    /**
     * 삭제 권한 확인
     */
    private void validateDeletePermission(BoardEntity boardEntity, String currentUserId) {
        if (!boardEntity.getAuthorId().equals(currentUserId)) {
            throw new UnauthorizedException("게시글 삭제 권한이 없습니다.");
        }
    }

    /**
     * Entity를 VO로 변환
     */
    private BoardVO convertToVO(BoardEntity entity) {
        // 이미지 파일 존재 여부 확인
        Optional<FileUploadResponse> imageInfo = fileService.getBoardImageInfo(entity.getBoardId());

        return BoardVO.builder()
                .boardId(entity.getBoardId())
                .title(entity.getTitle())
                .authorId(entity.getAuthorId())
                .authorNickname(entity.getAuthorNickname())
                .content(entity.getContent())
                .category(entity.getCategory())
                .reportType(entity.getReportType())
                .reportLocation(entity.getReportLocation())
                .viewCount(entity.getViewCount())
                .likeCount(entity.getLikeCount())
                .commentCount(entity.getCommentCount())
                .hasImage(imageInfo.isPresent())
                .imageUrl(imageInfo.map(FileUploadResponse::getFileUrl).orElse(null))
                .isNotice(entity.getIsNotice())
                .isReported(entity.getIsReported())
                .createdAt(entity.getCreatedAt())
                .updatedAt(entity.getUpdatedAt())
                .build();
    }

    /**
     * Entity를 DetailVO로 변환
     */
    private BoardDetailVO convertToDetailVO(BoardEntity entity, String currentUserId) {
        // 이미지 파일 정보 조회
        Optional<FileUploadResponse> imageInfo = fileService.getBoardImageInfo(entity.getBoardId());

        BoardDetailVO detailVO = BoardDetailVO.builder()
                .boardId(entity.getBoardId())
                .title(entity.getTitle())
                .content(entity.getContent())
                .authorId(entity.getAuthorId())
                .authorNickname(entity.getAuthorNickname())
                .category(entity.getCategory())
                .reportType(entity.getReportType())
                .reportLocation(entity.getReportLocation())
                .viewCount(entity.getViewCount())
                .likeCount(entity.getLikeCount())
                .commentCount(entity.getCommentCount())
                .hasImage(imageInfo.isPresent())
                .imageUrl(imageInfo.map(FileUploadResponse::getFileUrl).orElse(null))
                .imageFileName(imageInfo.map(FileUploadResponse::getOriginalFileName).orElse(null))
                .imageFileSize(imageInfo.map(FileUploadResponse::getFileSize).orElse(null))
                .isNotice(entity.getIsNotice())
                .isReported(entity.getIsReported())
                .createdAt(entity.getCreatedAt())
                .updatedAt(entity.getUpdatedAt())
                .isAuthor(entity.getAuthorId().equals(currentUserId))
                .build();

        // 현재 사용자의 좋아요 여부 확인
        if (StringUtils.hasText(currentUserId)) {
            boolean isLiked = boardLikeService.hasUserLiked(entity.getBoardId(), currentUserId);
            detailVO.setIsLiked(isLiked);
        }

        return detailVO;
    }

    /**
     * 페이지 정보 생성
     */
    private BoardListVO.PageInfoVO createPageInfo(Page<BoardEntity> page) {
        return BoardListVO.PageInfoVO.builder()
                .currentPage(page.getNumber())
                .pageSize(page.getSize())
                .totalElements(page.getTotalElements())
                .totalPages(page.getTotalPages())
                .hasNext(page.hasNext())
                .hasPrevious(page.hasPrevious())
                .isFirst(page.isFirst())
                .isLast(page.isLast())
                .build();
    }

    /**
     * 검색 정보 생성
     */
    private BoardListVO.SearchInfoVO createSearchInfo(BoardSearchDTO searchDTO) {
        if (searchDTO == null) {
            return null;
        }

        return BoardListVO.SearchInfoVO.builder()
                .keyword(searchDTO.getKeyword())
                .searchType(searchDTO.getSearchType().name())
                .category(searchDTO.getCategory())
                .authorId(searchDTO.getAuthorId())
                .authorNickname(searchDTO.getAuthorNickname())
                .startDate(searchDTO.getStartDateTime())
                .endDate(searchDTO.getEndDateTime())
                .build();
    }

    public Optional<BoardVO> getBoardById(Long boardId) {
        return boardRepository.findByIdAndNotDeleted(boardId).map(this::convertToBoardVO);
    }
    /**
     * Entity를 VO로 변환 (예시 - 실제로는 기존 변환 로직 사용)
     */
    private BoardVO convertToBoardVO(BoardEntity entity) {
        return BoardVO.builder()
                .boardId(entity.getBoardId())
                .title(entity.getTitle())
                .authorId(entity.getAuthorId())
                .createdAt(entity.getCreatedAt())
                .updatedAt(entity.getUpdatedAt())
                .isReported(entity.getIsReported())
                .build();
    }
}