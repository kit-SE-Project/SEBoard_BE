package com.seproject.file.domain.model;

import lombok.NoArgsConstructor;

import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.GeneratedValue;
import javax.persistence.Id;

@Entity
@NoArgsConstructor
public class FileExtension {
    public FileExtension(String extensionName) {
        this.extensionName = extensionName;
    }

    @Id @GeneratedValue
    private Long fileExtensionId;
    private String extensionName;

    public String getExtensionName() {
        return extensionName;
    }
}
