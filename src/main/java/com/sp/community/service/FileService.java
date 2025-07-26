package com.sp.community.service;

import com.sp.config.FileProperties;
import com.sp.community.model.response.FileUploadResponse;
import com.sp.community.persistent.entity.BoardEntity;
import com.sp.community.persistent.entity.BoardFileEntity;
import com.sp.community.persistent.repository.BoardFileRepository;
import com.sp.community.persistent.repository.BoardRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.core.io.Resource;
import org.springframework.core.io.UrlResource;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;
import org.springframework.web.multipart.MultipartFile;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardCopyOption;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.Arrays;
import java.util.Optional;
import java.util.UUID;

@Service
@RequiredArgsConstructor
@Slf4j
@Transactional
public class FileService {

    private final FileProperties fileProperties;
    private final BoardFileRepository boardFileRepository;
    private final BoardRepository boardRepository;

    /**
     * 게시글에 이미지 파일 업로드 (기존 파일이 있으면 교체)
     */
    public FileUploadResponse uploadImageForBoard(Long boardId, MultipartFile file) {
        // 게시글 존재 확인
        BoardEntity board = boardRepository.findById(boardId)
                .orElseThrow(() -> new IllegalArgumentException("게시글을 찾을 수 없습니다."));

        // 파일 유효성 검증
        validateImageFile(file);

        // 기존 파일이 있으면 삭제
        Optional<BoardFileEntity> existingFile = boardFileRepository.findByBoard(board);
        if (existingFile.isPresent()) {
            deleteFile(existingFile.get());
        }

        // 파일 저장
        String storedFileName = saveFile(file);

        // DB에 파일 정보 저장
        BoardFileEntity boardFile = BoardFileEntity.builder()
                .board(board)
                .originalFileName(file.getOriginalFilename())
                .storedFileName(storedFileName)
                .fileSize(file.getSize())
                .contentType(file.getContentType())
                .uploadPath(getUploadPath())
                .createdAt(LocalDateTime.now())
                .build();

        boardFileRepository.save(boardFile);

        // 응답 DTO 생성
        return FileUploadResponse.builder()
                .fileId(boardFile.getFileId())
                .originalFileName(boardFile.getOriginalFileName())
                .storedFileName(boardFile.getStoredFileName())
                .fileUrl(fileProperties.getBaseUrl() + storedFileName)
                .fileSize(boardFile.getFileSize())
                .contentType(boardFile.getContentType())
                .build();
    }

    /**
     * 파일 다운로드/조회
     */
    @Transactional(readOnly = true)
    public Resource loadFileAsResource(String fileName) {
        try {
            Path filePath = Paths.get(getUploadPath()).resolve(fileName).normalize();
            Resource resource = new UrlResource(filePath.toUri());

            if (resource.exists() && resource.isReadable()) {
                return resource;
            } else {
                throw new RuntimeException("파일을 찾을 수 없습니다: " + fileName);
            }
        } catch (Exception e) {
            throw new RuntimeException("파일을 읽을 수 없습니다: " + fileName, e);
        }
    }

    /**
     * 게시글의 이미지 파일 정보 조회
     */
    @Transactional(readOnly = true)
    public Optional<FileUploadResponse> getBoardImageInfo(Long boardId) {
        BoardEntity board = boardRepository.findById(boardId)
                .orElseThrow(() -> new IllegalArgumentException("게시글을 찾을 수 없습니다."));

        return boardFileRepository.findByBoard(board)
                .map(file -> FileUploadResponse.builder()
                        .fileId(file.getFileId())
                        .originalFileName(file.getOriginalFileName())
                        .storedFileName(file.getStoredFileName())
                        .fileUrl(fileProperties.getBaseUrl() + file.getStoredFileName())
                        .fileSize(file.getFileSize())
                        .contentType(file.getContentType())
                        .build());
    }

    /**
     * 게시글의 이미지 파일 삭제
     */
    public void deleteBoardImage(Long boardId) {
        BoardEntity board = boardRepository.findById(boardId)
                .orElseThrow(() -> new IllegalArgumentException("게시글을 찾을 수 없습니다."));

        Optional<BoardFileEntity> fileEntity = boardFileRepository.findByBoard(board);
        if (fileEntity.isPresent()) {
            deleteFile(fileEntity.get());
        }
    }

    /**
     * 파일 유효성 검증
     */
    private void validateImageFile(MultipartFile file) {
        if (file == null || file.isEmpty()) {
            throw new IllegalArgumentException("파일이 비어있습니다.");
        }

        // 파일 크기 검증
        if (file.getSize() > fileProperties.getMaxFileSize()) {
            throw new IllegalArgumentException("파일 크기가 너무 큽니다. 최대 " +
                    (fileProperties.getMaxFileSize() / 1024 / 1024) + "MB까지 업로드 가능합니다.");
        }

        // 파일 확장자 검증
        String fileName = StringUtils.cleanPath(file.getOriginalFilename());
        String extension = getFileExtension(fileName).toLowerCase();

        if (!Arrays.asList(fileProperties.getAllowedExtensions()).contains(extension)) {
            throw new IllegalArgumentException("지원하지 않는 파일 형식입니다. " +
                    "허용된 확장자: " + String.join(", ", fileProperties.getAllowedExtensions()));
        }

        // MIME 타입 검증
        String contentType = file.getContentType();
        if (contentType == null || !contentType.startsWith("image/")) {
            throw new IllegalArgumentException("이미지 파일만 업로드 가능합니다.");
        }
    }

    /**
     * 파일 물리적 저장
     */
    private String saveFile(MultipartFile file) {
        try {
            // 업로드 디렉토리 생성
            Path uploadPath = Paths.get(getUploadPath());
            if (!Files.exists(uploadPath)) {
                Files.createDirectories(uploadPath);
            }

            // 고유한 파일명 생성
            String originalFileName = StringUtils.cleanPath(file.getOriginalFilename());
            String extension = getFileExtension(originalFileName);
            String storedFileName = generateUniqueFileName(extension);

            // 파일 저장
            Path targetLocation = uploadPath.resolve(storedFileName);
            Files.copy(file.getInputStream(), targetLocation, StandardCopyOption.REPLACE_EXISTING);

            return storedFileName;
        } catch (IOException e) {
            throw new RuntimeException("파일 저장에 실패했습니다.", e);
        }
    }

    /**
     * 파일 삭제
     */
    private void deleteFile(BoardFileEntity fileEntity) {
        try {
            // 물리적 파일 삭제
            Path filePath = Paths.get(fileEntity.getUploadPath(), fileEntity.getStoredFileName());
            Files.deleteIfExists(filePath);

            // DB에서 파일 정보 삭제
            boardFileRepository.delete(fileEntity);

            log.info("파일 삭제 완료: {}", fileEntity.getStoredFileName());
        } catch (IOException e) {
            log.error("파일 삭제 실패: {}", fileEntity.getStoredFileName(), e);
            // DB에서는 삭제하되, 물리적 파일 삭제 실패는 로그만 남김
            boardFileRepository.delete(fileEntity);
        }
    }

    /**
     * 고유한 파일명 생성
     */
    private String generateUniqueFileName(String extension) {
        String dateTime = LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyyMMdd_HHmmss"));
        String uuid = UUID.randomUUID().toString().substring(0, 8);
        return dateTime + "_" + uuid + "." + extension;
    }

    /**
     * 파일 확장자 추출
     */
    private String getFileExtension(String fileName) {
        if (fileName == null || !fileName.contains(".")) {
            return "";
        }
        return fileName.substring(fileName.lastIndexOf(".") + 1);
    }

    /**
     * 업로드 경로 반환
     */
    private String getUploadPath() {
        return fileProperties.getUploadDir();
    }
}