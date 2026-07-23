package com.seproject.admin.departmentboard.application;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.seproject.account.account.domain.Account;
import com.seproject.account.utils.SecurityUtils;
import com.seproject.admin.dashboard.domain.DashBoardMenu;
import com.seproject.admin.dashboard.service.AdminDashBoardServiceImpl;
import com.seproject.admin.departmentboard.controller.dto.DepartmentBoardDTO.DepartmentBoardFailure;
import com.seproject.admin.departmentboard.controller.dto.DepartmentBoardDTO.DepartmentBoardRequest;
import com.seproject.admin.departmentboard.controller.dto.DepartmentBoardDTO.DepartmentBoardResult;
import com.seproject.admin.departmentboard.controller.dto.DepartmentBoardDTO.DepartmentBoardSource;
import com.seproject.admin.departmentboard.controller.dto.DepartmentBoardDTO.DepartmentBoardSuccess;
import com.seproject.board.post.domain.model.Post;
import com.seproject.board.post.domain.repository.PostRepository;
import com.seproject.error.errorCode.ErrorCode;
import com.seproject.error.exception.CustomAccessDeniedException;
import com.seproject.error.exception.CustomAuthenticationException;
import com.seproject.error.exception.CustomIllegalArgumentException;
import com.seproject.file.domain.model.AttachableType;
import com.seproject.file.domain.model.FileMetaData;
import com.seproject.file.domain.repository.FileMetaDataRepository;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.servlet.mvc.method.annotation.StreamingResponseBody;

import java.io.IOException;
import java.io.OutputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeParseException;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;
import java.util.zip.ZipEntry;
import java.util.zip.ZipOutputStream;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class DepartmentBoardApiDownloadService {

    private static final DateTimeFormatter JOB_ID_FORMAT = DateTimeFormatter.ofPattern("yyyyMMdd-HHmmss");

    private final AdminDashBoardServiceImpl dashBoardService;
    private final FileMetaDataRepository fileMetaDataRepository;
    private final ObjectMapper objectMapper;
    private final PostRepository postRepository;

    public DepartmentBoardResult preview(DepartmentBoardRequest request) {
        checkAuthorization();
        SearchCondition condition = validate(request);
        List<Post> posts = findPosts(condition);

        return DepartmentBoardResult.builder()
                .jobId(createJobId())
                .status("COMPLETED")
                .totalCount(posts.size())
                .successCount(posts.size())
                .failCount(0)
                .successes(toSuccesses(posts))
                .failures(List.of())
                .build();
    }

    public DepartmentBoardArchive download(DepartmentBoardRequest request) {
        checkAuthorization();
        SearchCondition condition = validate(request);

        String jobId = createJobId();
        String downloadFileName = createDownloadFileName(condition.getSource(), jobId);
        LocalDateTime startedAt = LocalDateTime.now();
        List<DepartmentBoardFailure> failures = new ArrayList<>();
        List<ArticleZipEntry> articles = new ArrayList<>();
        List<DepartmentBoardSuccess> successes = new ArrayList<>();

        List<Post> posts = findPosts(condition);
        Map<Long, List<FileMetaData>> attachments = condition.isIncludeAttachments() ? findAttachments(posts) : Map.of();

        for (Post post : posts) {
            try {
                List<ArticleAttachment> articleAttachments = condition.isIncludeAttachments()
                        ? toArticleAttachments(post.getPostId(), attachments.get(post.getPostId()), failures)
                        : List.of();
                ArticleDetail detail = toArticleDetail(post, articleAttachments);

                articles.add(ArticleZipEntry.builder()
                        .entryName("posts/" + post.getPostId() + ".json")
                        .detail(detail)
                        .build());
                successes.add(toSuccess(post));
            } catch (Exception e) {
                failures.add(failure(post.getPostId(), "게시글 export 실패", e.getMessage()));
            }
        }

        DepartmentBoardResult result = DepartmentBoardResult.builder()
                .jobId(jobId)
                .status(failures.isEmpty() ? "COMPLETED" : "COMPLETED_WITH_FAILURES")
                .downloadFileName(downloadFileName)
                .totalCount(posts.size())
                .successCount(successes.size())
                .failCount(failures.size())
                .successes(successes)
                .failures(failures)
                .build();
        Map<String, Object> metadata = toMetadata(condition, startedAt, LocalDateTime.now(), result);
        StreamingResponseBody body = outputStream -> writeZip(outputStream, metadata, articles);

        return DepartmentBoardArchive.builder()
                .fileName(downloadFileName)
                .body(body)
                .build();
    }

    private void checkAuthorization() {
        Account account = SecurityUtils.getAccount()
                .orElseThrow(() -> new CustomAuthenticationException(ErrorCode.NOT_LOGIN, null));

        DashBoardMenu dashboardMenu = dashBoardService.findDashBoardMenuByUrl(DashBoardMenu.DEPARTMENT_BOARD_API_DOWNLOAD_URL);

        if (!dashboardMenu.authorize(account.getRoles())) {
            throw new CustomAccessDeniedException(ErrorCode.ACCESS_DENIED, null);
        }
    }

    private SearchCondition validate(DepartmentBoardRequest request) {
        if (request == null || request.getSource() == null || request.getFromDate() == null || request.getToDate() == null) {
            throw new CustomIllegalArgumentException(ErrorCode.INVALID_REQUEST, null);
        }

        LocalDate fromDate = parseDate(request.getFromDate());
        LocalDate toDate = parseDate(request.getToDate());

        if (fromDate.isAfter(toDate) || toDate.isAfter(LocalDate.now())) {
            throw new CustomIllegalArgumentException(ErrorCode.INVALID_DATE, null);
        }

        if (ChronoUnit.DAYS.between(fromDate, toDate) > 365) {
            throw new CustomIllegalArgumentException(ErrorCode.INVALID_DATE, null);
        }

        return SearchCondition.builder()
                .source(request.getSource())
                .fromDate(fromDate)
                .toDate(toDate)
                .includeAttachments(request.isIncludeAttachments())
                .build();
    }

    private LocalDate parseDate(String value) {
        try {
            return LocalDate.parse(value);
        } catch (DateTimeParseException e) {
            throw new CustomIllegalArgumentException(ErrorCode.INVALID_DATE, new IllegalArgumentException(e));
        }
    }

    private List<Post> findPosts(SearchCondition condition) {
        return postRepository.findNormalPostsForDepartmentBoardExport(
                condition.getSource().getBoardUrlInfo(),
                condition.getFromDate().atStartOfDay(),
                condition.getToDate().plusDays(1).atStartOfDay()
        );
    }

    private Map<Long, List<FileMetaData>> findAttachments(List<Post> posts) {
        List<Long> postIds = posts.stream()
                .map(Post::getPostId)
                .collect(Collectors.toList());

        if (postIds.isEmpty()) {
            return Map.of();
        }

        return fileMetaDataRepository.findByAttachableTypeAndAttachableIdIn(AttachableType.POST, postIds)
                .stream()
                .collect(Collectors.groupingBy(FileMetaData::getAttachableId));
    }

    private ArticleDetail toArticleDetail(Post post, List<ArticleAttachment> attachments) {
        return ArticleDetail.builder()
                .articleNo(post.getPostId())
                .postId(post.getPostId())
                .title(post.getTitle())
                .writer(post.getAuthor() == null ? "" : post.getAuthor().getName())
                .createdAt(post.getBaseTime().getCreatedAt())
                .modifiedAt(post.getBaseTime().getModifiedAt())
                .views(post.getViews())
                .categoryId(post.getCategory().getMenuId())
                .categoryName(post.getCategory().getName())
                .boardName(post.getCategory().getSuperMenu().getName())
                .contentsHtml(post.getContents())
                .contentsText(stripTags(post.getContents()))
                .attachments(attachments)
                .build();
    }

    private List<ArticleAttachment> toArticleAttachments(
            Long postId,
            List<FileMetaData> fileMetaDataList,
            List<DepartmentBoardFailure> failures
    ) {
        if (fileMetaDataList == null) {
            return new ArrayList<>();
        }

        List<ArticleAttachment> attachments = new ArrayList<>();

        for (FileMetaData file : fileMetaDataList) {
            if (file.getFilePath() == null || file.getFilePath().isBlank()) {
                failures.add(failure(postId, "첨부파일 경로 없음", file.getOriginalFileName()));
                continue;
            }

            Path source = Paths.get(file.getFilePath()).normalize();

            if (!Files.exists(source)) {
                failures.add(failure(postId, "첨부파일 원본 없음", source.toString()));
                continue;
            }

            ArticleAttachment attachment = ArticleAttachment.builder()
                    .attachNo(file.getFileMetaDataId())
                    .fileName(file.getOriginalFileName())
                    .storedFileName(file.getStoredFileName())
                    .filePath(source.toString())
                    .urlPath(file.getUrlPath())
                    .fileSize(file.getFileSize())
                    .savedPath(buildAttachmentEntryName(postId, file))
                    .build();
            attachments.add(attachment);
        }

        return attachments;
    }

    private void writeZip(
            OutputStream outputStream,
            Map<String, Object> metadata,
            List<ArticleZipEntry> articles
    ) throws IOException {
        try (ZipOutputStream zipOutputStream = new ZipOutputStream(outputStream)) {
            writeJsonEntry(zipOutputStream, "metadata.json", metadata);

            for (ArticleZipEntry article : articles) {
                writeJsonEntry(zipOutputStream, article.getEntryName(), article.getDetail());
            }

            for (ArticleZipEntry article : articles) {
                for (ArticleAttachment attachment : article.getDetail().getAttachments()) {
                    writeAttachmentEntry(zipOutputStream, attachment);
                }
            }
        }
    }

    private void writeJsonEntry(ZipOutputStream zipOutputStream, String entryName, Object value) throws IOException {
        zipOutputStream.putNextEntry(new ZipEntry(entryName));
        zipOutputStream.write(objectMapper.writerWithDefaultPrettyPrinter().writeValueAsBytes(value));
        zipOutputStream.closeEntry();
    }

    private void writeAttachmentEntry(ZipOutputStream zipOutputStream, ArticleAttachment attachment) throws IOException {
        zipOutputStream.putNextEntry(new ZipEntry(attachment.getSavedPath()));
        Files.copy(Paths.get(attachment.getFilePath()), zipOutputStream);
        zipOutputStream.closeEntry();
    }

    private Map<String, Object> toMetadata(
            SearchCondition condition,
            LocalDateTime startedAt,
            LocalDateTime finishedAt,
            DepartmentBoardResult result
    ) {
        Map<String, Object> metadata = new LinkedHashMap<>();
        metadata.put("source", condition.getSource());
        metadata.put("boardUrlInfo", condition.getSource().getBoardUrlInfo());
        metadata.put("fromDate", condition.getFromDate());
        metadata.put("toDate", condition.getToDate());
        metadata.put("includeAttachments", condition.isIncludeAttachments());
        metadata.put("startedAt", startedAt);
        metadata.put("finishedAt", finishedAt);
        metadata.put("result", result);
        return metadata;
    }

    private List<DepartmentBoardSuccess> toSuccesses(List<Post> posts) {
        return posts.stream()
                .map(this::toSuccess)
                .collect(Collectors.toList());
    }

    private DepartmentBoardSuccess toSuccess(Post post) {
        return DepartmentBoardSuccess.builder()
                .articleNo(post.getPostId())
                .title(post.getTitle())
                .build();
    }

    private DepartmentBoardFailure failure(Long articleNo, String reason, String message) {
        return DepartmentBoardFailure.builder()
                .articleNo(articleNo)
                .reason(reason)
                .message(message == null || message.isBlank() ? reason : message)
                .build();
    }

    private String stripTags(String value) {
        if (value == null) {
            return "";
        }

        return value.replaceAll("<[^>]*>", " ")
                .replace("&nbsp;", " ")
                .replaceAll("\\s+", " ")
                .trim();
    }

    private String sanitizeFileName(String value) {
        String sanitized = value == null ? "" : value.trim().replaceAll("[\\\\/:*?\"<>|]", "_");
        return sanitized.isEmpty() ? "attachment" : sanitized;
    }

    private String createJobId() {
        return LocalDateTime.now().format(JOB_ID_FORMAT);
    }

    private String createDownloadFileName(DepartmentBoardSource source, String jobId) {
        return "department-board-" + source.name().toLowerCase() + "-" + jobId + ".zip";
    }

    private String buildAttachmentEntryName(Long postId, FileMetaData file) {
        String attachNo = file.getFileMetaDataId() == null ? "unknown" : String.valueOf(file.getFileMetaDataId());
        return "attachments/" + postId + "/" + attachNo + "_" + sanitizeFileName(file.getOriginalFileName());
    }

    @Data
    @Builder
    private static class SearchCondition {
        private DepartmentBoardSource source;
        private LocalDate fromDate;
        private LocalDate toDate;
        private boolean includeAttachments;
    }

    @Data
    @Builder
    public static class DepartmentBoardArchive {
        private String fileName;
        private StreamingResponseBody body;
    }

    @Data
    @Builder
    private static class ArticleZipEntry {
        private String entryName;
        private ArticleDetail detail;
    }

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class ArticleDetail {
        private Long articleNo;
        private Long postId;
        private String title;
        private String writer;
        private LocalDateTime createdAt;
        private LocalDateTime modifiedAt;
        private int views;
        private Long categoryId;
        private String categoryName;
        private String boardName;
        private String contentsHtml;
        private String contentsText;
        private List<ArticleAttachment> attachments;
    }

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class ArticleAttachment {
        private Long attachNo;
        private String fileName;
        private String storedFileName;
        private String filePath;
        private String urlPath;
        private Long fileSize;
        private String savedPath;
    }
}
