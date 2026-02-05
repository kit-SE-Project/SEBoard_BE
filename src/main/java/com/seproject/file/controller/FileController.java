package com.seproject.file.controller;

import com.seproject.file.application.FileAppService;
import com.seproject.file.controller.dto.FileMetaDataResponse.FileMetaDataListResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.util.List;

@RestController
@RequestMapping("/files")
@RequiredArgsConstructor
public class FileController {
    private final FileAppService fileAppService;

    @PostMapping
    public ResponseEntity uploadFiles(@RequestPart List<MultipartFile> files){
        FileMetaDataListResponse res = fileAppService.uploadFiles(files);

        return ResponseEntity.ok(res);
    }

    @DeleteMapping("/{fileId}")
    public ResponseEntity deleteFile(@PathVariable Long fileId){
        fileAppService.deleteFile(fileId);

        return ResponseEntity.ok().build();
    }
}
