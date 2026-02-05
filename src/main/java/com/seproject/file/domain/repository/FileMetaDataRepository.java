package com.seproject.file.domain.repository;

import com.seproject.file.domain.model.FileMetaData;
import org.springframework.data.jpa.repository.JpaRepository;

public interface FileMetaDataRepository extends JpaRepository<FileMetaData, Long> {
}
