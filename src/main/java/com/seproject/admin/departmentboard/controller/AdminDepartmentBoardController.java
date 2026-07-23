package com.seproject.admin.departmentboard.controller;

import com.seproject.admin.departmentboard.application.DepartmentBoardApiDownloadService;
import com.seproject.admin.departmentboard.application.DepartmentBoardApiDownloadService.DepartmentBoardArchive;
import com.seproject.admin.departmentboard.controller.dto.DepartmentBoardDTO.DepartmentBoardRequest;
import com.seproject.admin.departmentboard.controller.dto.DepartmentBoardDTO.DepartmentBoardResult;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.servlet.mvc.method.annotation.StreamingResponseBody;

@Tag(name = "학과 게시판 API 다운로드", description = "학과 게시판 게시글 다운로드 API")
@RestController
@RequiredArgsConstructor
@RequestMapping("/admin/department-board-api-download")
public class AdminDepartmentBoardController {

    private final DepartmentBoardApiDownloadService departmentBoardApiDownloadService;

    @PostMapping("/preview")
    @Operation(summary = "학과 게시판 다운로드 미리보기", description = "지정한 기간의 다운로드 대상 게시글 수를 확인한다.")
    public ResponseEntity<DepartmentBoardResult> preview(@RequestBody DepartmentBoardRequest request) {
        return ResponseEntity.ok(departmentBoardApiDownloadService.preview(request));
    }

    @PostMapping("/download")
    @Operation(summary = "학과 게시판 다운로드 실행", description = "지정한 기간의 게시글 상세 정보를 zip 파일로 바로 내려준다.")
    public ResponseEntity<StreamingResponseBody> download(@RequestBody DepartmentBoardRequest request) {
        DepartmentBoardArchive archive = departmentBoardApiDownloadService.download(request);

        return ResponseEntity.ok()
                .contentType(MediaType.APPLICATION_OCTET_STREAM)
                .header(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=\"" + archive.getFileName() + "\"")
                .body(archive.getBody());
    }
}
