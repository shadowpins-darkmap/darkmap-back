package com.sp.common.mail.service;

import com.sp.common.mail.model.dto.CommentReportInfoDto;
import com.sp.common.mail.model.dto.EmailDto;
import com.sp.common.mail.model.dto.ReportEmailDto;
import com.sp.community.model.dto.BoardReportCreateDTO;
import com.sp.community.model.dto.CommentReportCreateDTO;
import com.sp.community.model.vo.BoardReportVO;
import com.sp.community.model.vo.BoardVO;
import com.sp.community.model.vo.CommentReportVO;
import com.sp.community.service.BoardService;
import com.sp.community.service.CommentReportService;
import com.sp.community.service.CommentService;
import com.sp.member.persistent.repository.MemberRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.io.ByteArrayResource;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.mail.javamail.MimeMessageHelper;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import jakarta.mail.MessagingException;
import jakarta.mail.internet.MimeMessage;

import java.io.IOException;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;

@Slf4j
@Service
@RequiredArgsConstructor
public class EmailService {

    // Spring Boot가 자동으로 JavaMailSender 빈을 생성해줍니다
    private final JavaMailSender mailSender;
    private final CommentService commentService;
    private final CommentReportService commentReportService;
    private final BoardService boardService;
    private final MemberRepository memberRepository;

    @Value("${spring.mail.username}")
    private String reportEmail;

    @Value("${spring.mail.username}")
    private String fromEmail;

    @Value("${mail.report.subject-prefix:[제보접수]}")
    private String subjectPrefix;
    /**
     * 게시글 신고 이메일 발송
     */
    public void sendBoardReportEmail(Long boardId,
                                     BoardReportCreateDTO createDTO,
                                     BoardReportVO reportVO) {
        try {
            // 게시글 정보 조회
            BoardVO boardInfo = boardService.getBoardById(boardId)
                    .orElse(createDefaultBoardInfo(boardId));

            // 신고자 닉네임 조회
            String reporterNickname = memberRepository.findNicknameByMemberId(createDTO.getReporterId())
                    .orElse(createDTO.getReporterId());

            // 이메일 발송
            ReportEmailDto reportDto = createBoardReportEmailDto(
                    boardInfo, createDTO, reportVO, reporterNickname);

            sendReportEmail(reportDto);

            log.info("게시글 신고 이메일 발송 성공 - boardId: {}", boardId);

        } catch (Exception e) {
            log.error("게시글 신고 이메일 발송 실패 - boardId: {}", boardId, e);
            throw e;
        }
    }

    /**
     * 게시글 정보 조회 실패시 기본 정보 생성
     */
    private BoardVO createDefaultBoardInfo(Long boardId) {
        return BoardVO.builder()
                .boardId(boardId)
                .title("게시글 정보 조회 실패")
                .authorNickname("알 수 없음")
                .category("UNKNOWN")
                .createdAt(LocalDateTime.now())
                .build();
    }

    /**
     * 게시글 신고 이메일 DTO 생성
     */
    private ReportEmailDto createBoardReportEmailDto(BoardVO boardInfo,
                                                     BoardReportCreateDTO createDTO,
                                                     BoardReportVO reportVO,
                                                     String reporterNickname) {

        String reportId = generateBoardReportId(boardInfo.getBoardId(), reportVO.getReportId());
        String emailContent = createBoardReportEmailContent(boardInfo, createDTO, reportId, reporterNickname);

        return ReportEmailDto.builder()
                .reportTitle("게시글 신고 접수")
                .reportContent(emailContent)
                .reportCategory("게시글 신고 - " + createDTO.getReportType().toString())
                .reporterName(reporterNickname)
                .reporterEmail(null) // 필요시 사용자 이메일 추가
                .reportDate(LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss")))
                .reportId(reportId)
                .attachmentFile(createDTO.getAttachmentFile()) // 첨부파일 추가
                .build();
    }

    /**
     * 게시글 신고 ID 생성
     */
    private String generateBoardReportId(Long boardId, Long reportVOId) {
        return String.format("BOARD_REPORT_%d_%d_%d",
                boardId, reportVOId, System.currentTimeMillis());
    }

    /**
     * 게시글 신고 이메일 내용 생성
     */
    private String createBoardReportEmailContent(BoardVO boardInfo,
                                                 BoardReportCreateDTO createDTO,
                                                 String reportId,
                                                 String reporterNickname) {
        return String.format("""
            게시글 신고가 접수되었습니다.
            
            === 신고 정보 ===
            • 신고 ID: %s
            • 신고자: %s
            • 신고 유형: %s
            • 신고 시간: %s
            %s
            
            === 신고 대상 게시글 ===
            • 게시글 ID: %s
            • 게시글 제목: %s
            • 게시글 작성자: %s
            • 게시글 카테고리: %s
            • 게시글 작성시간: %s
            %s
            
            === 신고 사유 ===
            %s
            %s
            
            === 처리 안내 ===
            관리자 페이지에서 게시글 ID %s로 검색하여 확인 및 처리해주세요.
            
            ---
            다크맵 게시글 신고 시스템에서 자동 발송된 메일입니다.
            """,
                reportId,
                reporterNickname,
                createDTO.getReportType().getDescription(),
                LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyy년 MM월 dd일 HH시 mm분")),
                createDTO.hasAttachment() ? "• 첨부파일: " + createDTO.getAttachmentFile().getOriginalFilename() : "",
                boardInfo.getBoardId(),
                truncateText(boardInfo.getTitle(), 100),
                boardInfo.getAuthorNickname(),
                boardInfo.getCategory(),
                boardInfo.getCreatedAt() != null ?
                        boardInfo.getCreatedAt().format(DateTimeFormatter.ofPattern("yyyy년 MM월 dd일 HH시 mm분")) : "알 수 없음",
                boardInfo.isIncidentReportCategory() && boardInfo.getReportLocation() != null ?
                        "• 제보 위치: " + boardInfo.getReportLocation() : "",
                createDTO.getReason(),
                createDTO.getAdditionalInfo() != null && !createDTO.getAdditionalInfo().trim().isEmpty() ?
                        "\n\n[추가 설명]\n" + createDTO.getAdditionalInfo() : "",
                boardInfo.getBoardId()
        );
    }

    /**
     * 댓글 신고 이메일 발송
     *
     * @param commentId 댓글 ID
     * @param createDTO 신고 생성 정보
     * @param reportVO 생성된 신고 정보
     */
    public void sendCommentReportEmail(Long commentId,
                                       CommentReportCreateDTO createDTO,
                                       CommentReportVO reportVO) {
        try {
            log.info("댓글 신고 이메일 발송 시작: commentId={}, reportId={}",
                    commentId, reportVO.getReportId());

            // 댓글 정보 조회 (안전하게)
            CommentReportInfoDto commentInfo = null;
            try {
                commentInfo = commentService.getCommentReportInfo(commentId)
                        .orElse(null);
            } catch (Exception e) {
                log.warn("댓글 정보 조회 실패, 기본 정보로 대체: commentId={}", commentId, e);
            }

            // 댓글 정보가 없으면 기본 정보 생성
            if (commentInfo == null) {
                commentInfo = createDefaultCommentInfo(commentId);
            }

            // 신고자 정보 조회
            String reporterNickname = "알 수 없음";
            try {
                reporterNickname = memberRepository.findNicknameByMemberId(createDTO.getReporterId())
                        .orElse(createDTO.getReporterId());
            } catch (Exception e) {
                log.warn("신고자 닉네임 조회 실패: reporterId={}", createDTO.getReporterId());
            }

            // 이메일 발송
            ReportEmailDto reportDto = createReportEmailDto(
                    commentInfo, createDTO, reportVO, reporterNickname);

            sendReportEmail(reportDto);

            log.info("댓글 신고 이메일 발송 성공: commentId={}, reportId={}",
                    commentId, reportVO.getReportId());

        } catch (Exception e) {
            log.error("댓글 신고 이메일 발송 실패: commentId={}, reportId={}",
                    commentId, reportVO.getReportId(), e);
            // 이메일 발송 실패해도 예외를 다시 던지지 않음
        }
    }

    /**
     * 댓글 정보 조회 실패시 기본 정보 생성 (수정)
     */
    private CommentReportInfoDto createDefaultCommentInfo(Long commentId) {
        log.warn("댓글 정보를 기본값으로 설정: commentId={}", commentId);

        return CommentReportInfoDto.builder()
                .commentId(commentId)
                .boardId(0L)
                .commentContent("댓글 정보 조회 실패")
                .commentAuthorId("unknown")
                .commentAuthorNickname("알 수 없음")
                .commentCreatedAt(LocalDateTime.now())
                .boardTitle("게시글 정보 없음")
                .boardCategory("UNKNOWN")
                .boardAuthorNickname("알 수 없음")
                .build();
    }

    /**
     * 신고 이메일 DTO 생성 (수정)
     */
    private ReportEmailDto createReportEmailDto(CommentReportInfoDto commentInfo,
                                                CommentReportCreateDTO createDTO,
                                                CommentReportVO reportVO,
                                                String reporterNickname) {

        String reportId = generateReportId(commentInfo.getCommentId(), reportVO.getReportId());
        String emailContent = createEmailContent(commentInfo, createDTO, reportId, reporterNickname);

        return ReportEmailDto.builder()
                .reportTitle("댓글 신고 접수")
                .reportContent(emailContent)
                .reportCategory("댓글 신고 - " + createDTO.getReportType().toString())
                .reporterName(reporterNickname)
                .reporterEmail(null) // 필요시 사용자 이메일 추가
                .reportDate(LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss")))
                .reportId(reportId)
                .attachmentFile(createDTO.getAttachmentFile()) // 첨부파일 추가
                .build();
    }

    /**
     * 신고 ID 생성
     */
    private String generateReportId(Long commentId, Long reportVOId) {
        return String.format("CMT_REPORT_%d_%d_%d",
                commentId, reportVOId, System.currentTimeMillis());
    }

    /**
     * 이메일 내용 생성 (첨부파일 정보 포함)
     */
    private String createEmailContent(CommentReportInfoDto commentInfo,
                                      CommentReportCreateDTO createDTO,
                                      String reportId, String reporterNickname) {
        return String.format("""
            댓글 신고가 접수되었습니다.
            
            === 신고 정보 ===
            • 신고 ID: %s
            • 신고 유형: %s
            • 신고 시간: %s
            %s
            
            === 신고 대상 댓글 ===
            • 댓글 ID: %s
            • 댓글 내용: %s
            • 댓글 작성자: %s
            • 댓글 작성시간: %s
            
            === 관련 게시글 ===
            • 게시글 제목: %s
            
            === 신고 사유 ===
            %s
            %s
            
            === 처리 안내 ===
            관리자 페이지에서 댓글 ID %s로 검색하여 확인 및 처리해주세요.
            
            ---
            다크맵 댓글 신고 시스템에서 자동 발송된 메일입니다.
            """,
                reportId,
                createDTO.getReportType().getDescription(),
                LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyy년 MM월 dd일 HH시 mm분")),
                createDTO.hasAttachment() ? "• 첨부파일: " + createDTO.getAttachmentFile().getOriginalFilename() : "",
                commentInfo.getCommentId(),
                truncateText(commentInfo.getCommentContent(), 200),
                commentInfo.getCommentAuthorDisplayName(),
                commentInfo.getCommentCreatedAt() != null ?
                        commentInfo.getCommentCreatedAt().format(DateTimeFormatter.ofPattern("yyyy년 MM월 dd일 HH시 mm분")) : "알 수 없음",
                commentInfo.getBoardTitleOrDefault(),
                createDTO.getReason(),
                createDTO.getAdditionalInfo() != null && !createDTO.getAdditionalInfo().trim().isEmpty() ?
                        "\n\n[추가 설명]\n" + createDTO.getAdditionalInfo() : "",
                commentInfo.getCommentId()
        );
    }

    /**
     * 텍스트 길이 제한
     */
    private String truncateText(String text, int maxLength) {
        if (text == null) return "내용 없음";
        if (text.length() <= maxLength) return text;
        return text.substring(0, maxLength) + "...";
    }

    /// ///////////////////////////////////////////////////////////////////////////////////////
    /**
     * 제보 이메일 전송
     */
    public void sendReportEmail(ReportEmailDto reportDto) {
        try {
            validateReportDto(reportDto);

            String htmlContent = createReportEmailTemplate(reportDto);
            String subject = subjectPrefix + " " + reportDto.getReportTitle();

            EmailDto emailDto = EmailDto.builder()
                    .to(reportEmail)
                    .from(fromEmail)
                    .subject(subject)
                    .content(htmlContent)
                    .isHtml(true)
                    .attachment(reportDto.getAttachmentFile())
                    .attachmentName(generateAttachmentName(reportDto.getAttachmentFile()))
                    .build();

            sendEmail(emailDto);
            log.info("제보 이메일 전송 완료 - 제목: {}, 제보자: {}", subject, reportDto.getReporterName());

        } catch (MessagingException e) {
            log.error("이메일 전송 실패 - SMTP 오류", e);
            throw new RuntimeException("SMTP 서버 연결 오류로 이메일 전송에 실패했습니다.", e);
        } catch (IOException e) {
            log.error("이메일 전송 실패 - 첨부파일 처리 오류", e);
            throw new RuntimeException("첨부파일 처리 오류로 이메일 전송에 실패했습니다.", e);
        } catch (Exception e) {
            log.error("이메일 전송 실패 - 알 수 없는 오류", e);
            throw new RuntimeException("알 수 없는 오류로 이메일 전송에 실패했습니다.", e);
        }
    }

    /**
     * 일반 이메일 전송
     */
    public void sendEmail(EmailDto emailDto) throws MessagingException, IOException {
        MimeMessage message = mailSender.createMimeMessage();
        MimeMessageHelper helper = new MimeMessageHelper(message, true, "UTF-8");

        helper.setTo(emailDto.getTo());
        helper.setFrom(emailDto.getFrom());
        helper.setSubject(emailDto.getSubject());
        helper.setText(emailDto.getContent(), emailDto.isHtml());

        // 첨부파일이 있는 경우
        if (emailDto.getAttachment() != null && !emailDto.getAttachment().isEmpty()) {
            String attachmentName = emailDto.getAttachmentName() != null
                    ? emailDto.getAttachmentName()
                    : emailDto.getAttachment().getOriginalFilename();

            helper.addAttachment(attachmentName,
                    new ByteArrayResource(emailDto.getAttachment().getBytes()));
        }

        mailSender.send(message);
    }

    /**
     * 제보 DTO 유효성 검사
     */
    private void validateReportDto(ReportEmailDto reportDto) {
        if (reportDto == null) {
            throw new IllegalArgumentException("제보 정보가 없습니다.");
        }
        if (reportDto.getReportTitle() == null || reportDto.getReportTitle().trim().isEmpty()) {
            throw new IllegalArgumentException("제보 제목이 없습니다.");
        }
        if (reportDto.getReportContent() == null || reportDto.getReportContent().trim().isEmpty()) {
            throw new IllegalArgumentException("제보 내용이 없습니다.");
        }
        if (reportDto.getReporterName() == null || reportDto.getReporterName().trim().isEmpty()) {
            throw new IllegalArgumentException("제보자 이름이 없습니다.");
        }

        // 첨부파일 크기 검사 (10MB 제한)
        if (reportDto.getAttachmentFile() != null && !reportDto.getAttachmentFile().isEmpty()) {
            long maxSize = 10 * 1024 * 1024; // 10MB
            if (reportDto.getAttachmentFile().getSize() > maxSize) {
                throw new IllegalArgumentException("첨부파일 크기는 10MB를 초과할 수 없습니다.");
            }
        }
    }

    /**
     * 제보 이메일 HTML 템플릿 생성
     */
    private String createReportEmailTemplate(ReportEmailDto reportDto) {
        StringBuilder html = new StringBuilder();

        html.append("<!DOCTYPE html>")
                .append("<html lang='ko'>")
                .append("<head>")
                .append("<meta charset='UTF-8'>")
                .append("<meta name='viewport' content='width=device-width, initial-scale=1.0'>")
                .append("<title>다크맵 제보 접수</title>")
                .append("<style>")
                .append("body { font-family: 'Malgun Gothic', 'Arial', sans-serif; line-height: 1.6; color: #333; margin: 0; padding: 0; }")
                .append(".container { max-width: 600px; margin: 0 auto; padding: 20px; }")
                .append(".header { background: linear-gradient(135deg, #667eea 0%, #764ba2 100%); color: white; padding: 30px; text-align: center; border-radius: 8px 8px 0 0; }")
                .append(".header h1 { margin: 0; font-size: 24px; }")
                .append(".header p { margin: 10px 0 0 0; opacity: 0.9; }")
                .append(".content { background-color: #ffffff; padding: 30px; border: 1px solid #e0e0e0; border-top: none; border-radius: 0 0 8px 8px; }")
                .append(".field { margin-bottom: 20px; }")
                .append(".label { font-weight: bold; color: #555; display: inline-block; width: 120px; vertical-align: top; }")
                .append(".value { color: #333; display: inline-block; width: calc(100% - 130px); word-wrap: break-word; }")
                .append(".content-box { background-color: #f9f9f9; padding: 20px; border-radius: 6px; border-left: 4px solid #667eea; margin-top: 10px; }")
                .append(".footer { margin-top: 30px; padding-top: 20px; border-top: 2px solid #e0e0e0; font-size: 14px; color: #666; text-align: center; }")
                .append(".badge { background-color: #667eea; color: white; padding: 4px 12px; border-radius: 20px; font-size: 12px; font-weight: bold; }")
                .append("</style>")
                .append("</head>")
                .append("<body>")
                .append("<div class='container'>");

        // 헤더
        html.append("<div class='header'>")
                .append("<h1>🚨 새로운 제보가 접수되었습니다</h1>")
                .append("<p>다크맵 제보시스템 • ").append(LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyy년 MM월 dd일 HH:mm"))).append("</p>")
                .append("</div>");

        // 내용
        html.append("<div class='content'>");

        if (reportDto.getReportId() != null) {
            html.append("<div class='field'>")
                    .append("<span class='label'>제보 ID:</span>")
                    .append("<span class='value'><span class='badge'>").append(reportDto.getReportId()).append("</span></span>")
                    .append("</div>");
        }

        html.append("<div class='field'>")
                .append("<span class='label'>제목:</span>")
                .append("<span class='value'><strong>").append(escapeHtml(reportDto.getReportTitle())).append("</strong></span>")
                .append("</div>");

        if (reportDto.getReportCategory() != null) {
            html.append("<div class='field'>")
                    .append("<span class='label'>카테고리:</span>")
                    .append("<span class='value'>").append(escapeHtml(reportDto.getReportCategory())).append("</span>")
                    .append("</div>");
        }

        html.append("<div class='field'>")
                .append("<span class='label'>제보자:</span>")
                .append("<span class='value'>").append(escapeHtml(reportDto.getReporterName())).append("</span>")
                .append("</div>");

        if (reportDto.getReporterEmail() != null && !reportDto.getReporterEmail().trim().isEmpty()) {
            html.append("<div class='field'>")
                    .append("<span class='label'>이메일:</span>")
                    .append("<span class='value'><a href='mailto:").append(reportDto.getReporterEmail()).append("'>")
                    .append(reportDto.getReporterEmail()).append("</a></span>")
                    .append("</div>");
        }

        if (reportDto.getReportLocation() != null && !reportDto.getReportLocation().trim().isEmpty()) {
            html.append("<div class='field'>")
                    .append("<span class='label'>위치:</span>")
                    .append("<span class='value'>📍 ").append(escapeHtml(reportDto.getReportLocation())).append("</span>")
                    .append("</div>");
        }

        html.append("<div class='field'>")
                .append("<span class='label'>내용:</span>")
                .append("<div class='content-box'>")
                .append(escapeHtml(reportDto.getReportContent()).replace("\n", "<br>"))
                .append("</div>")
                .append("</div>");

        if (reportDto.getAttachmentFile() != null && !reportDto.getAttachmentFile().isEmpty()) {
            html.append("<div class='field'>")
                    .append("<span class='label'>첨부파일:</span>")
                    .append("<span class='value'>📎 ").append(escapeHtml(reportDto.getAttachmentFile().getOriginalFilename()))
                    .append(" (").append(formatFileSize(reportDto.getAttachmentFile().getSize())).append(")</span>")
                    .append("</div>");
        }

        html.append("</div>");

        // 푸터
        html.append("<div class='footer'>")
                .append("<p><strong>다크맵 제보시스템</strong></p>")
                .append("<p>본 메일은 시스템에서 자동으로 발송되었습니다.</p>")
                .append("<p>문의사항이 있으시면 관리자에게 연락해주세요.</p>")
                .append("</div>");

        html.append("</div></body></html>");

        return html.toString();
    }

    /**
     * HTML 이스케이프 처리
     */
    private String escapeHtml(String text) {
        if (text == null) return "";
        return text.replace("&", "&amp;")
                .replace("<", "&lt;")
                .replace(">", "&gt;")
                .replace("\"", "&quot;")
                .replace("'", "&#x27;");
    }

    /**
     * 파일 크기 포맷팅
     */
    private String formatFileSize(long size) {
        if (size < 1024) return size + " bytes";
        if (size < 1024 * 1024) return String.format("%.1f KB", size / 1024.0);
        return String.format("%.1f MB", size / (1024.0 * 1024.0));
    }

    /**
     * 첨부파일명 생성
     */
    private String generateAttachmentName(MultipartFile file) {
        if (file == null || file.isEmpty()) {
            return null;
        }

        String originalName = file.getOriginalFilename();
        String timestamp = LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyyMMdd_HHmmss"));

        if (originalName != null && originalName.contains(".")) {
            String extension = originalName.substring(originalName.lastIndexOf("."));
            return "darkmap_report_" + timestamp + extension;
        }

        return "darkmap_report_" + timestamp;
    }
}