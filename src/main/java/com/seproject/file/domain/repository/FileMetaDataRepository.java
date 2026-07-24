package com.seproject.file.domain.repository;

import com.seproject.file.domain.model.AttachableType;
import com.seproject.file.domain.model.FileMetaData;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface FileMetaDataRepository extends JpaRepository<FileMetaData, Long> {
    List<FileMetaData> findByAttachableTypeAndAttachableId(AttachableType attachableType, Long attachableId);
    List<FileMetaData> findByAttachableTypeAndAttachableIdIn(AttachableType attachableType, List<Long> attachableIds);
    boolean existsByAttachableTypeAndAttachableId(AttachableType attachableType, Long attachableId);
}
