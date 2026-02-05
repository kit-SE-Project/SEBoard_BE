package com.seproject.file.domain.repository;

import com.seproject.file.domain.model.FileConfiguration;
import org.springframework.data.jpa.repository.JpaRepository;

public interface FileConfigurationRepository extends JpaRepository<FileConfiguration, Long> {
}
