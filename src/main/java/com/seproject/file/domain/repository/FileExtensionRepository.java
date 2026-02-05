package com.seproject.file.domain.repository;

import com.seproject.file.domain.model.FileExtension;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface FileExtensionRepository extends JpaRepository<FileExtension, Long> {
    void removeByExtensionName(String extensionName);

    @Query("select count(fe)>0 from FileExtension fe where fe.extensionName=:name")
    boolean existsExtensionName(@Param("name") String name);
}
