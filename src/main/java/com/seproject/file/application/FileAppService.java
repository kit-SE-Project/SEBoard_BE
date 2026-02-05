package com.seproject.file.application;

import com.seproject.file.domain.model.FileConfiguration;
import com.seproject.file.domain.repository.FileConfigurationRepository;
import com.seproject.error.errorCode.ErrorCode;
import com.seproject.error.exception.ExceedFileSizeException;
import com.seproject.file.controller.dto.FileMetaDataResponse.FileMetaDataListResponse;
import com.seproject.file.domain.model.FileMetaData;
import com.seproject.file.domain.repository.FileMetaDataRepository;
import com.seproject.file.domain.repository.FileRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import java.util.ArrayList;
import java.util.List;

@Service
@RequiredArgsConstructor
@Transactional
public class FileAppService {
    private final FileMetaDataRepository fileMetaDataRepository;
    private final FileRepository fileRepository;
    private final FileConfigurationRepository fileConfigurationRepository;
    private final Long MEGA_BYTE_TO_BYTE = 1024L * 1024L;

    public FileMetaDataListResponse uploadFiles(List<MultipartFile> fileList) {
        List<FileMetaData> fileMetaDataList = new ArrayList<>();
        Long maxFileSize = fileConfigurationRepository.findAll().stream().findFirst().orElseGet(
                () -> new FileConfiguration(100L, 100L)
        ).getMaxSizePerFile();

        fileList.forEach(multipartFile -> {
            if(maxFileSize < multipartFile.getSize()/MEGA_BYTE_TO_BYTE){
                throw new ExceedFileSizeException(ErrorCode.INVALID_FILE_SIZE);
            }

            FileMetaData fileMetaData = fileRepository.save(multipartFile);
            fileMetaDataRepository.save(fileMetaData);
            fileMetaDataList.add(fileMetaData);
        });

        return new FileMetaDataListResponse(fileMetaDataList);
    }

    public void deleteFile(Long fileMetaDataId) { //TODO : return
        FileMetaData fileMetaData = fileMetaDataRepository.findById(fileMetaDataId)
                .orElseThrow(IllegalArgumentException::new);

        fileRepository.delete(fileMetaData.getFilePath());
        fileMetaDataRepository.delete(fileMetaData);
    }

}
