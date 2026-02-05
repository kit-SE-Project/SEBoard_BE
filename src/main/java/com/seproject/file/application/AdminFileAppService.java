package com.seproject.file.application;

import com.seproject.account.account.domain.Account;
import com.seproject.account.utils.SecurityUtils;
import com.seproject.admin.dashboard.domain.DashBoardMenu;
import com.seproject.admin.dashboard.service.AdminDashBoardServiceImpl;
import com.seproject.board.common.utils.FileUtils;
import com.seproject.error.errorCode.ErrorCode;
import com.seproject.error.exception.CustomAccessDeniedException;
import com.seproject.error.exception.CustomAuthenticationException;
import com.seproject.file.controller.dto.FileRequest.AdminFileRetrieveCondition;
import com.seproject.file.controller.dto.FileResponse.AdminFileRetrieveResponse;
import com.seproject.file.controller.dto.FileResponse.FileConfigurationResponse;
import com.seproject.file.controller.dto.FileResponse.FileExtensionResponse;
import com.seproject.file.domain.model.FileConfiguration;
import com.seproject.file.domain.model.FileExtension;
import com.seproject.file.domain.repository.*;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
@RequiredArgsConstructor
@Transactional
public class AdminFileAppService {
    private final FileUtils fileUtils;
    private final FileMetaDataRepository fileMetaDataRepository;
    private final FileRepository fileRepository;
    private final FileConfigurationRepository fileConfigurationRepository;
    private final FileExtensionRepository fileExtensionRepository;
    private final AdminFileMetaDataSearchRepository fileMetaDataSearchRepository;

    private final AdminDashBoardServiceImpl dashBoardService;

    //TODO : 추후 AOP로 변경 필요
    private void checkAuthorization(){
        Account account = SecurityUtils.getAccount()
                .orElseThrow(() -> new CustomAuthenticationException(ErrorCode.NOT_LOGIN, null));

        DashBoardMenu dashboardmenu = dashBoardService.findDashBoardMenuByUrl(DashBoardMenu.FILE_MANAGE_URL);

        if(!dashboardmenu.authorize(account.getRoles())){
            throw new CustomAccessDeniedException(ErrorCode.ACCESS_DENIED, null);
        }
    }

    public Page<AdminFileRetrieveResponse> retrieveFileMetaData(AdminFileRetrieveCondition condition, Pageable pageable) {
        checkAuthorization();

        return fileMetaDataSearchRepository.findFileMetaDataByCondition(condition, pageable);
    }

    public void deleteBulkFile(List<Long> fileIds) {
        checkAuthorization();

        fileMetaDataRepository.findAllById(fileIds).forEach(fileMetaData -> {
            fileRepository.delete(fileMetaData.getFilePath());
            fileMetaDataRepository.delete(fileMetaData);
        });
    }

    public void addFileExtension(List<String> extensions) {
        checkAuthorization();

        //TODO : 중복 검사?
        extensions.forEach(extension -> {
            FileExtension fileExtension = new FileExtension(extension);
            fileUtils.addAllowedExtension(extension);
            fileExtensionRepository.save(fileExtension);
        });
    }

    public void removeFileExtension(List<String> extensions){
        checkAuthorization();

        //TODO : 중복 검사?
        extensions.forEach(extension -> {
            fileUtils.removeAllowedExtension(extension);
            fileExtensionRepository.removeByExtensionName(extension);
        });
    }

    public FileExtensionResponse retrieveFileExtension(){
        checkAuthorization();

        return FileExtensionResponse.of(fileExtensionRepository.findAll());
    }

    public void setFileConfiguration(Long maxSizePerFile, Long maxSizePerPost) {
        checkAuthorization();

        FileConfiguration fileConfiguration = fileConfigurationRepository.findAll().stream().findFirst().orElseGet(() -> {
            return new FileConfiguration(100L, 100L);
        });

        fileConfiguration.setMaxSizePerFile(maxSizePerFile);
        fileConfiguration.setMaxSizePerPost(maxSizePerPost);

        fileConfigurationRepository.save(fileConfiguration);
    }

    public FileConfigurationResponse retrieveFileConfiguration() {
        checkAuthorization();

        FileConfiguration fileConfiguration = fileConfigurationRepository.findAll().stream().findFirst().orElseGet(() -> {
            return new FileConfiguration(100L, 100L);
        });

        return new FileConfigurationResponse(fileConfiguration);
    }
}
