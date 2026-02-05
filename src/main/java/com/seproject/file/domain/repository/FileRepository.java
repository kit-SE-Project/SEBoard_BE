package com.seproject.file.domain.repository;

import com.seproject.file.domain.model.FileMetaData;
import org.springframework.web.multipart.MultipartFile;

public interface FileRepository {
    FileMetaData save(MultipartFile file);
    void delete(String fileName);
}
