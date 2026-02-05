package com.seproject.board.common.utils;

import com.seproject.error.errorCode.ErrorCode;
import com.seproject.error.exception.InvalidFileExtensionException;
import com.seproject.file.domain.repository.FileExtensionRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import java.io.File;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.HashSet;
import java.util.Set;
import java.util.UUID;

@Component
@RequiredArgsConstructor
public class FileUtils {
    @Value("${storage.rootPath}")
    private String rootPath;
    private final FileExtensionRepository fileExtensionRepository;
    private Set<String> allowedExtensions = new HashSet<>();

    public void addAllowedExtension(String extension){
        allowedExtensions.add(extension);
    }

    public void removeAllowedExtension(String extension) {
        allowedExtensions.remove(extension);
    }

    private String getExtension(String originalFileName){
        if(allowedExtensions.isEmpty()){
            fileExtensionRepository.findAll().forEach(fileExtension -> {
                allowedExtensions.add(fileExtension.getExtensionName());
            });
        }

        String extension = originalFileName.substring(originalFileName.lastIndexOf("."));
        if(!allowedExtensions.contains(extension.replace(".", ""))){
            throw new InvalidFileExtensionException(ErrorCode.NOT_EXIST_EXTENSION);
        }

        return extension;
    }

    public String getStoredFileName(String originalFileName) {
        String extension = getExtension(originalFileName);
        return UUID.randomUUID().toString().replace("-", "")+extension;
    }

    public String getFilePath(){
        String currentDate = getCurrentDate();

        File directory = new File(rootPath+"/"+currentDate);

        if(!directory.exists()){
            directory.mkdirs();
        }

        return directory.getPath();
    }

    public String getUrlPath(String urlRoot){
        String currentDate = getCurrentDate();

        return urlRoot+"/"+currentDate;
    }

    private String getCurrentDate(){
        return LocalDate.now().format(DateTimeFormatter.ofPattern("yyyy/MM/dd"));
    }

}
