package com.seproject.admin.departmentboard.controller;

import com.seproject.admin.departmentboard.application.DepartmentBoardApiDownloadService;
import com.seproject.admin.departmentboard.controller.dto.DepartmentBoardDTO.DepartmentBoardRequest;
import com.seproject.admin.departmentboard.controller.dto.DepartmentBoardDTO.DepartmentBoardResult;
import com.seproject.admin.departmentboard.controller.dto.DepartmentBoardDTO.DepartmentBoardSource;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.core.io.InputStreamResource;
import org.springframework.core.io.Resource;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;

@Tag(name = "학과 게시판 API 다운로드", description = "학과 홈페이지 게시글과 첨부파일 다운로드 API")
@RestController
@RequiredArgsConstructor
@RequestMapping("/admin/department-board-api-download")
public class AdminDepartmentBoardController {

    private final DepartmentBoardApiDownloadService departmentBoardApiDownloadService;

    @PostMapping("/preview")
    @Operation(summary = "학과 게시판 다운로드 미리보기", description = "지정한 기간의 수집 대상 게시글 수를 확인한다.")
    public ResponseEntity<DepartmentBoardResult> preview(@RequestBody DepartmentBoardRequest request) {
        return ResponseEntity.ok(departmentBoardApiDownloadService.preview(request));
    }

    @PostMapping("/download")
    @Operation(summary = "학과 게시판 다운로드 실행", description = "지정한 기간의 게시글 상세 정보와 첨부파일을 파일 시스템에 저장한다.")
    public ResponseEntity<DepartmentBoardResult> download(@RequestBody DepartmentBoardRequest request) {
        return ResponseEntity.ok(departmentBoardApiDownloadService.download(request));
    }

    @GetMapping("/files/{source}/{jobId}")
    @Operation(summary = "학과 게시판 다운로드 파일 조회", description = "다운로드 작업 결과 zip 파일을 내려받는다.")
    public ResponseEntity<Resource> downloadFile(
            @PathVariable DepartmentBoardSource source,
            @PathVariable String jobId
    ) throws IOException {
        Path zipFile = departmentBoardApiDownloadService.findDownloadFile(source, jobId);

        return ResponseEntity.ok()
                .contentType(MediaType.APPLICATION_OCTET_STREAM)
                .contentLength(Files.size(zipFile))
                .header(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=\"" + zipFile.getFileName() + "\"")
                .body(new InputStreamResource(Files.newInputStream(zipFile)));
    }
}
