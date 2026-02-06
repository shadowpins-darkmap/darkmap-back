package com.sp.mail.controller;

import com.sp.mail.dto.ReportEmailDto;
import com.sp.mail.service.EmailService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.Map;

@Slf4j
@RestController
@RequestMapping("/api/email")
@RequiredArgsConstructor
public class EmailTestController {

    private final EmailService emailService;

    /**
     * 1. 기본 제보 이메일 테스트 (파라미터 없음)
     * GET http://localhost:8080/api/email/test
     */
    @GetMapping("/test")
    public ResponseEntity<Map<String, Object>> sendBasicTest() {
        try {
            ReportEmailDto reportDto = ReportEmailDto.builder()
                    .reportTitle("이메일 기능 기본 테스트")
                    .reportContent("다크맵 제보시스템의 이메일 발송 기능을 테스트합니다.\n\n" +
                            "✅ HTML 이메일 템플릿 테스트\n" +
                            "✅ 한글 인코딩 테스트\n" +
                            "✅ 줄바꿈 처리 테스트\n\n" +
                            "모든 기능이 정상적으로 작동하고 있습니다!")
                    .reportCategory("시스템 테스트")
                    .reportLocation("서울시 테스트구 테스트동")
                    .reporterName("시스템 테스터")
                    .reporterEmail("kdark.report@gmail.com")
                    .reportDate(LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss")))
                    .reportId("TEST_" + System.currentTimeMillis())
                    .attachmentFile(null) // 첨부파일 없이 테스트
                    .build();

            emailService.sendReportEmail(reportDto);

            return ResponseEntity.ok(Map.of(
                    "success", true,
                    "message", "✅ 기본 테스트 이메일 전송 성공!",
                    "detail", "kdark.report@gmail.com에서 HTML 형식의 이메일을 확인하세요.",
                    "reportId", reportDto.getReportId(),
                    "timestamp", LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss"))
            ));

        } catch (Exception e) {
            log.error("기본 테스트 이메일 전송 실패", e);
            return ResponseEntity.internalServerError().body(Map.of(
                    "success", false,
                    "message", "❌ 이메일 전송 실패",
                    "error", e.getMessage(),
                    "timestamp", LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss"))
            ));
        }
    }

    /**
     * 2. JSON 방식으로 커스텀 제보 이메일 테스트
     * POST http://localhost:8080/api/email/json-test
     * Content-Type: application/json
     * {
     *   "title": "JSON 테스트 제목",
     *   "content": "JSON으로 전송한 제보 내용",
     *   "category": "테스트",
     *   "location": "서울시",
     *   "reporterName": "JSON 테스터",
     *   "reporterEmail": "test@example.com"
     * }
     */
    @PostMapping("/json-test")
    public ResponseEntity<Map<String, Object>> sendJsonTest(@RequestBody Map<String, String> request) {
        try {
            String title = request.getOrDefault("title", "JSON 기본 테스트");
            String content = request.getOrDefault("content", "JSON 방식으로 전송된 테스트 내용입니다.");
            String category = request.getOrDefault("category", "JSON 테스트");
            String location = request.getOrDefault("location", "미기재");
            String reporterName = request.getOrDefault("reporterName", "JSON 테스터");
            String reporterEmail = request.getOrDefault("reporterEmail", "");

            ReportEmailDto reportDto = ReportEmailDto.builder()
                    .reportTitle(title)
                    .reportContent(content)
                    .reportCategory(category)
                    .reportLocation(location)
                    .reporterName(reporterName)
                    .reporterEmail(reporterEmail.isEmpty() ? null : reporterEmail)
                    .reportDate(LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss")))
                    .reportId("JSON_" + System.currentTimeMillis())
                    .attachmentFile(null)
                    .build();

            emailService.sendReportEmail(reportDto);

            return ResponseEntity.ok(Map.of(
                    "success", true,
                    "message", "✅ JSON 테스트 이메일 전송 성공!",
                    "detail", Map.of(
                            "title", title,
                            "reporterName", reporterName,
                            "contentLength", content.length() + "자",
                            "reportId", reportDto.getReportId()
                    ),
                    "timestamp", LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss"))
            ));

        } catch (Exception e) {
            log.error("JSON 테스트 이메일 전송 실패", e);
            return ResponseEntity.internalServerError().body(Map.of(
                    "success", false,
                    "message", "❌ JSON 테스트 이메일 전송 실패",
                    "error", e.getMessage(),
                    "timestamp", LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss"))
            ));
        }
    }

    /**
     * 3. 파일 첨부 테스트 (Form 방식)
     * POST http://localhost:8080/api/email/file-test
     * Content-Type: multipart/form-data
     * 파라미터:
     * - title: 제보 제목
     * - content: 제보 내용
     * - reporterName: 제보자 이름
     * - file: 첨부 파일 (선택사항)
     */
    @PostMapping("/file-test")
    public ResponseEntity<Map<String, Object>> sendFileTest(
            @RequestParam("title") String title,
            @RequestParam("content") String content,
            @RequestParam("reporterName") String reporterName,
            @RequestParam(value = "category", required = false) String category,
            @RequestParam(value = "location", required = false) String location,
            @RequestParam(value = "reporterEmail", required = false) String reporterEmail,
            @RequestParam(value = "file", required = false) MultipartFile file) {

        try {
            ReportEmailDto reportDto = ReportEmailDto.builder()
                    .reportTitle(title)
                    .reportContent(content)
                    .reportCategory(category != null ? category : "파일 테스트")
                    .reportLocation(location)
                    .reporterName(reporterName)
                    .reporterEmail(reporterEmail)
                    .attachmentFile(file)
                    .reportDate(LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss")))
                    .reportId("FILE_" + System.currentTimeMillis())
                    .build();

            emailService.sendReportEmail(reportDto);

            Map<String, Object> fileInfo = Map.of();
            if (file != null && !file.isEmpty()) {
                fileInfo = Map.of(
                        "fileName", file.getOriginalFilename(),
                        "fileSize", formatFileSize(file.getSize()),
                        "contentType", file.getContentType()
                );
            }

            return ResponseEntity.ok(Map.of(
                    "success", true,
                    "message", "✅ 파일 첨부 테스트 이메일 전송 성공!",
                    "detail", Map.of(
                            "title", title,
                            "reporterName", reporterName,
                            "hasAttachment", file != null && !file.isEmpty(),
                            "fileInfo", fileInfo,
                            "reportId", reportDto.getReportId()
                    ),
                    "timestamp", LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss"))
            ));

        } catch (Exception e) {
            log.error("파일 첨부 테스트 이메일 전송 실패", e);
            return ResponseEntity.internalServerError().body(Map.of(
                    "success", false,
                    "message", "❌ 파일 첨부 테스트 실패",
                    "error", e.getMessage(),
                    "timestamp", LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss"))
            ));
        }
    }

    /**
     * 4. 실제 제보 상황 시뮬레이션 테스트
     * GET http://localhost:8080/api/email/realistic-test
     */
    @GetMapping("/realistic-test")
    public ResponseEntity<Map<String, Object>> sendRealisticTest() {
        try {
            ReportEmailDto reportDto = ReportEmailDto.builder()
                    .reportTitle("강남역 근처 불법 주차 신고")
                    .reportContent("강남역 2번 출구 근처에 불법 주차된 차량들이 보행자 통행에 불편을 주고 있습니다.\n\n" +
                            "== 상세 정보 ==\n" +
                            "- 발견 시간: 2024년 8월 24일 오후 2시 경\n" +
                            "- 위치: 강남역 2번 출구에서 약 50m 지점\n" +
                            "- 차량 대수: 약 3-4대\n" +
                            "- 주차 위치: 인도 및 횡단보도 근처\n\n" +
                            "== 문제점 ==\n" +
                            "1. 보행자가 차도로 나와서 걸어야 함\n" +
                            "2. 휠체어나 유모차 이용자 통행 불가\n" +
                            "3. 시각 장애인 등의 안전 위험\n\n" +
                            "빠른 조치 부탁드립니다.")
                    .reportCategory("교통/주차")
                    .reportLocation("서울시 강남구 강남대로 지하철 2호선 강남역 2번 출구 근처")
                    .reporterName("김시민")
                    .reporterEmail("citizen.kim@example.com")
                    .reportDate(LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss")))
                    .reportId("REAL_" + System.currentTimeMillis())
                    .attachmentFile(null)
                    .build();

            emailService.sendReportEmail(reportDto);

            return ResponseEntity.ok(Map.of(
                    "success", true,
                    "message", "✅ 실제 제보 시뮬레이션 이메일 전송 성공!",
                    "detail", "실제 제보 상황과 유사한 형태의 이메일이 전송되었습니다.",
                    "reportId", reportDto.getReportId(),
                    "timestamp", LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss"))
            ));

        } catch (Exception e) {
            log.error("실제 제보 시뮬레이션 이메일 전송 실패", e);
            return ResponseEntity.internalServerError().body(Map.of(
                    "success", false,
                    "message", "❌ 실제 제보 시뮬레이션 실패",
                    "error", e.getMessage(),
                    "timestamp", LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss"))
            ));
        }
    }

    /**
     * 파일 크기 포맷팅 헬퍼 메서드
     */
    private String formatFileSize(long size) {
        if (size < 1024) return size + " bytes";
        if (size < 1024 * 1024) return String.format("%.1f KB", size / 1024.0);
        return String.format("%.1f MB", size / (1024.0 * 1024.0));
    }
}
