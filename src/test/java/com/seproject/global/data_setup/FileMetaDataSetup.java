package com.seproject.global.data_setup;

import com.seproject.board.common.BaseTime;
import com.seproject.board.common.utils.FileUtils;
import com.seproject.file.domain.model.FileMetaData;
import com.seproject.file.domain.repository.FileMetaDataRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.util.UUID;

@Component
public class FileMetaDataSetup {
    private final FileMetaDataRepository fileMetaDataRepository;
    private final FileUtils fileUtils;

    @Autowired
    public FileMetaDataSetup(FileMetaDataRepository fileMetaDataRepository, FileUtils fileUtils) {
        this.fileMetaDataRepository = fileMetaDataRepository;
        this.fileUtils = fileUtils;
    }

    public FileMetaData createImageFileMetaData(BaseTime baseTime, String extension) {
        fileUtils.addAllowedExtension(extension);
        String originalFileName = UUID.randomUUID().toString().substring(0,8) + "." + extension;
        String storedFileName = fileUtils.getStoredFileName(originalFileName);
        String filePath = fileUtils.getFilePath()+"/"+storedFileName;
        String urlPath = fileUtils.getUrlPath("/files/")+"/"+storedFileName;
        Long fileSize = (long)(Math.random() * 100);

        FileMetaData fileMetaData = new FileMetaData(originalFileName, storedFileName, filePath, urlPath, fileSize, baseTime);
        fileMetaDataRepository.save(fileMetaData);
        return fileMetaData;
    }
}
