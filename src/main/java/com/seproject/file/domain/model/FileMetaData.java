package com.seproject.file.domain.model;


import com.seproject.board.common.BaseTime;
import lombok.Getter;
import lombok.NoArgsConstructor;

import javax.persistence.*;

@Entity
@Table(name = "file_meta_data")
@Getter
@NoArgsConstructor
public class FileMetaData {
    @Id @GeneratedValue
    @Column(name = "file_meta_data_id")
    private Long fileMetaDataId;
    private String originalFileName;
    private String storedFileName;
    private String filePath;
    private String urlPath;
    private Long fileSize;
    private BaseTime baseTime;

    @Enumerated(EnumType.STRING)
    @Column(name = "attachable_type")
    private AttachableType attachableType;

    @Column(name = "attachable_id")
    private Long attachableId;

    public FileMetaData(String originalFileName, String storedFileName, String filePath, String urlPath, Long fileSize, BaseTime baseTime) {
        this.originalFileName = originalFileName;
        this.storedFileName = storedFileName;
        this.filePath = filePath;
        this.urlPath = urlPath;
        this.fileSize = fileSize;
        this.baseTime = baseTime;
    }

    public void attachTo(AttachableType type, Long id) {
        this.attachableType = type;
        this.attachableId = id;
    }

}
