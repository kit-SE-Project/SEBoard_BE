package com.seproject.file.controller;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.seproject.file.controller.dto.FileRequest.AdminFileRetrieveCondition;
import com.seproject.file.controller.dto.FileRequest.BulkFileRequest;
import com.seproject.file.controller.dto.FileRequest.FileConfigurationRequest;
import com.seproject.file.controller.dto.FileRequest.FileExtensionRequest;
import com.seproject.file.controller.dto.FileResponse.AdminFileRetrieveResponse;
import com.seproject.file.controller.dto.FileResponse.FileConfigurationResponse;
import com.seproject.file.controller.dto.FileResponse.FileExtensionResponse;
import com.seproject.file.application.AdminFileAppService;
import com.seproject.board.common.controller.dto.MessageResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RequestMapping("/admin/files")
@RestController
@RequiredArgsConstructor
public class AdminFileController {
    private final AdminFileAppService adminFileAppService;

    @GetMapping
    public ResponseEntity<Page<AdminFileRetrieveResponse>> retrieveFileList(@RequestParam(name = "isOrphan", required = false) Boolean isOrphan,
                                                                            @RequestParam(required = false) String searchOption,
                                                                            @RequestParam(required = false) String query,
                                                                            @RequestParam(defaultValue = "0") int page,
                                                                            @RequestParam(defaultValue = "25") int perPage) {

        AdminFileRetrieveCondition condition = new AdminFileRetrieveCondition();
        condition.setIsOrphan(isOrphan);
        condition.setSearchOption(searchOption);
        condition.setQuery(query);

        Page<AdminFileRetrieveResponse> res = adminFileAppService.retrieveFileMetaData(condition, PageRequest.of(page, perPage));
        return ResponseEntity.ok(res);
    }

    @DeleteMapping
    public ResponseEntity<MessageResponse> deleteBulkFile(@RequestBody BulkFileRequest fileIds){
        adminFileAppService.deleteBulkFile(fileIds.getFileIds());
        return ResponseEntity.ok(MessageResponse.of("파일 삭제 완료"));
    }

    @PostMapping("/configuration")
    public ResponseEntity<MessageResponse> setFileConfiguration(@RequestBody FileConfigurationRequest request){
        adminFileAppService.setFileConfiguration(request.getMaxSizePerFile(), request.getMaxSizePerPost());
        return ResponseEntity.ok(MessageResponse.of("파일 설정 변경 완료"));
    }

    @GetMapping("/configuration")
    public ResponseEntity<FileConfigurationResponse> retrieveFileConfiguration(){
        FileConfigurationResponse res = adminFileAppService.retrieveFileConfiguration();
        return ResponseEntity.ok(res);
    }

    @PostMapping("/extension")
    public ResponseEntity<MessageResponse> addAllowedFileExtension(@RequestBody FileExtensionRequest fileExtensionRequest){
        adminFileAppService.addFileExtension(fileExtensionRequest.getExtensions());
        return ResponseEntity.ok(MessageResponse.of("허용 확장자 추가 완료"));
    }

    @DeleteMapping("/extension")
    public ResponseEntity<MessageResponse> removeAllowedFileExtension(@RequestBody FileExtensionRequest fileExtensionRequest){
        adminFileAppService.removeFileExtension(fileExtensionRequest.getExtensions());
        return ResponseEntity.ok(MessageResponse.of("허용 확장자 삭제 완료"));
    }

    @GetMapping("/extension")
    public ResponseEntity<FileExtensionResponse> retrieveAllowedFileExtension(){
        FileExtensionResponse fileExtensionResponse = adminFileAppService.retrieveFileExtension();
        return ResponseEntity.ok(fileExtensionResponse);
    }
}
