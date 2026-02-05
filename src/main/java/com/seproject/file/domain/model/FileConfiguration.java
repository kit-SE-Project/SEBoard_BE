package com.seproject.file.domain.model;

import lombok.Getter;
import lombok.NoArgsConstructor;

import javax.persistence.Entity;
import javax.persistence.GeneratedValue;
import javax.persistence.Id;

@Entity
@NoArgsConstructor
@Getter
public class FileConfiguration{
    @Id @GeneratedValue
    private Long fileConfigurationId;
    private Long maxSizePerFile;
    private Long maxSizePerPost;

    public void setMaxSizePerFile(Long maxSizePerFile){
        this.maxSizePerFile = maxSizePerFile;
    }

    public void setMaxSizePerPost(Long maxSizePerPost){
        this.maxSizePerPost = maxSizePerPost;
    }

    public FileConfiguration(Long maxSizePerFile, Long maxSizePerPost) {
        this.maxSizePerFile = maxSizePerFile;
        this.maxSizePerPost = maxSizePerPost;
    }
}
