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
import com.seproject.board.post.domain.model.Post;
import com.seproject.board.post.domain.repository.PostRepository;
import com.seproject.error.errorCode.ErrorCode;
import com.seproject.error.exception.CustomAccessDeniedException;
import com.seproject.error.exception.CustomAuthenticationException;
import com.seproject.error.exception.CustomIllegalArgumentException;
import com.seproject.error.exception.NoSuchResourceException;
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

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardCopyOption;
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
                .failures(List.of())
                .build();
    }

    public DepartmentBoardResult download(DepartmentBoardRequest request) {
        checkAuthorization();
        SearchCondition condition = validate(request);

        String jobId = createJobId();
        Path downloadRoot = Paths.get("storage", "department-board", condition.getSource().name(), jobId);
        String downloadFileName = createDownloadFileName(condition.getSource(), jobId);
        LocalDateTime startedAt = LocalDateTime.now();
        List<DepartmentBoardFailure> failures = new ArrayList<>();

        try {
            Files.createDirectories(downloadRoot.resolve("posts"));
            Files.createDirectories(downloadRoot.resolve("attachments"));

            List<Post> posts = findPosts(condition);
            Map<Long, List<FileMetaData>> attachments = findAttachments(posts);
            int successCount = 0;

            for (Post post : posts) {
                try {
                    List<ArticleAttachment> articleAttachments = toArticleAttachments(attachments.get(post.getPostId()));

                    if (condition.isIncludeAttachments()) {
                        copyAttachments(downloadRoot, post.getPostId(), articleAttachments, failures);
                    }

                    ArticleDetail detail = toArticleDetail(post, articleAttachments);
                    objectMapper.writerWithDefaultPrettyPrinter()
                            .writeValue(downloadRoot.resolve("posts").resolve(post.getPostId() + ".json").toFile(), detail);
                    successCount++;
                } catch (Exception e) {
                    failures.add(failure(post.getPostId(), "게시글 export 실패", e.getMessage()));
                }
            }

            DepartmentBoardResult result = DepartmentBoardResult.builder()
                    .jobId(jobId)
                    .status(failures.isEmpty() ? "COMPLETED" : "COMPLETED_WITH_FAILURES")
                    .downloadRoot(downloadRoot.toString())
                    .downloadFileName(downloadFileName)
                    .downloadFileUrl(createDownloadFileUrl(condition.getSource(), jobId))
                    .totalCount(posts.size())
                    .successCount(successCount)
                    .failCount(failures.size())
                    .failures(failures)
                    .build();

            writeMetadata(downloadRoot, condition, startedAt, LocalDateTime.now(), result);
            createZip(downloadRoot, getZipPath(condition.getSource(), jobId));
            return result;
        } catch (Exception e) {
            DepartmentBoardResult result = failedResult(
                    jobId,
                    downloadRoot.toString(),
                    null,
                    "다운로드 작업 실패",
                    e.getMessage()
            );

            tryWriteFailureMetadata(downloadRoot, condition, startedAt, result);
            return result;
        }
    }

    public Path findDownloadFile(DepartmentBoardSource source, String jobId) {
        checkAuthorization();

        if (jobId == null || !jobId.matches("\\d{8}-\\d{6}")) {
            throw new NoSuchResourceException(ErrorCode.NOT_EXIST_FILE);
        }

        Path zipPath = getZipPath(source, jobId).normalize();
        Path sourceRoot = Paths.get("storage", "department-board", source.name()).normalize();

        if (!zipPath.startsWith(sourceRoot) || !Files.exists(zipPath)) {
            throw new NoSuchResourceException(ErrorCode.NOT_EXIST_FILE);
        }

        return zipPath;
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

    private List<ArticleAttachment> toArticleAttachments(List<FileMetaData> fileMetaDataList) {
        if (fileMetaDataList == null) {
            return new ArrayList<>();
        }

        return fileMetaDataList.stream()
                .map(file -> ArticleAttachment.builder()
                        .attachNo(file.getFileMetaDataId())
                        .fileName(file.getOriginalFileName())
                        .storedFileName(file.getStoredFileName())
                        .filePath(file.getFilePath())
                        .urlPath(file.getUrlPath())
                        .fileSize(file.getFileSize())
                        .build())
                .collect(Collectors.toList());
    }

    private void copyAttachments(
            Path downloadRoot,
            Long postId,
            List<ArticleAttachment> attachments,
            List<DepartmentBoardFailure> failures
    ) throws IOException {
        if (attachments.isEmpty()) {
            return;
        }

        Path attachmentDir = downloadRoot.resolve("attachments").resolve(String.valueOf(postId));
        Files.createDirectories(attachmentDir);

        for (ArticleAttachment attachment : attachments) {
            try {
                if (attachment.getFilePath() == null || attachment.getFilePath().isBlank()) {
                    failures.add(failure(postId, "첨부파일 경로 없음", attachment.getFileName()));
                    continue;
                }

                Path source = Paths.get(attachment.getFilePath());

                if (!Files.exists(source)) {
                    failures.add(failure(postId, "첨부파일 원본 없음", source.toString()));
                    continue;
                }

                String attachNo = attachment.getAttachNo() == null ? "unknown" : String.valueOf(attachment.getAttachNo());
                Path target = attachmentDir.resolve(attachNo + "_" + sanitizeFileName(attachment.getFileName())).normalize();

                if (!target.startsWith(attachmentDir)) {
                    failures.add(failure(postId, "첨부파일 저장 경로 오류", attachment.getFileName()));
                    continue;
                }

                Files.copy(source, target, StandardCopyOption.REPLACE_EXISTING);
                attachment.setSavedPath(target.toString());
            } catch (Exception e) {
                failures.add(failure(postId, "첨부파일 복사 실패", e.getMessage()));
            }
        }
    }

    private void writeMetadata(
            Path downloadRoot,
            SearchCondition condition,
            LocalDateTime startedAt,
            LocalDateTime finishedAt,
            DepartmentBoardResult result
    ) throws IOException {
        Map<String, Object> metadata = new LinkedHashMap<>();
        metadata.put("source", condition.getSource());
        metadata.put("boardUrlInfo", condition.getSource().getBoardUrlInfo());
        metadata.put("fromDate", condition.getFromDate());
        metadata.put("toDate", condition.getToDate());
        metadata.put("includeAttachments", condition.isIncludeAttachments());
        metadata.put("startedAt", startedAt);
        metadata.put("finishedAt", finishedAt);
        metadata.put("result", result);

        objectMapper.writerWithDefaultPrettyPrinter()
                .writeValue(downloadRoot.resolve("metadata.json").toFile(), metadata);
    }

    private void createZip(Path downloadRoot, Path zipPath) throws IOException {
        Files.createDirectories(zipPath.getParent());

        try (ZipOutputStream zipOutputStream = new ZipOutputStream(Files.newOutputStream(zipPath))) {
            try (var paths = Files.walk(downloadRoot)) {
                for (Path path : paths.filter(Files::isRegularFile).collect(Collectors.toList())) {
                    String entryName = downloadRoot.relativize(path).toString().replace('\\', '/');
                    zipOutputStream.putNextEntry(new ZipEntry(entryName));
                    Files.copy(path, zipOutputStream);
                    zipOutputStream.closeEntry();
                }
            }
        }
    }

    private void tryWriteFailureMetadata(
            Path downloadRoot,
            SearchCondition condition,
            LocalDateTime startedAt,
            DepartmentBoardResult result
    ) {
        try {
            Files.createDirectories(downloadRoot);
            writeMetadata(downloadRoot, condition, startedAt, LocalDateTime.now(), result);
        } catch (IOException ignored) {
        }
    }

    private DepartmentBoardResult failedResult(String jobId, String downloadRoot, Long articleNo, String reason, String message) {
        List<DepartmentBoardFailure> failures = List.of(failure(articleNo, reason, message));

        return DepartmentBoardResult.builder()
                .jobId(jobId)
                .status("FAILED")
                .downloadRoot(downloadRoot)
                .totalCount(0)
                .successCount(0)
                .failCount(failures.size())
                .failures(failures)
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

    private String createDownloadFileUrl(DepartmentBoardSource source, String jobId) {
        return "/admin/department-board-api-download/files/" + source.name() + "/" + jobId;
    }

    private Path getZipPath(DepartmentBoardSource source, String jobId) {
        return Paths.get("storage", "department-board", source.name(), createDownloadFileName(source, jobId));
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
