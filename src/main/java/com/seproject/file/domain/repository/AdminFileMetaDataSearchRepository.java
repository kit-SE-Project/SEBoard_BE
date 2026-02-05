package com.seproject.file.domain.repository;

import com.seproject.file.controller.dto.FileRequest.AdminFileRetrieveCondition;
import com.seproject.file.controller.dto.FileResponse.AdminFileRetrieveResponse;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

public interface AdminFileMetaDataSearchRepository {
    Page<AdminFileRetrieveResponse> findFileMetaDataByCondition(AdminFileRetrieveCondition condition, Pageable pageable);
}
