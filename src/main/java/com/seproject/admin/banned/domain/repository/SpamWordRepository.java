package com.seproject.admin.banned.domain.repository;

import com.seproject.admin.banned.controller.dto.SpamWordResponse;
import com.seproject.admin.banned.domain.SpamWord;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

import java.util.List;

public interface SpamWordRepository extends JpaRepository<SpamWord, Long> {
    @Query("select new com.seproject.admin.banned.controller.dto.SpamWordResponse(s) from SpamWord s")
    List<SpamWordResponse> findAllResponse();

    boolean existsByWord(String word);
}
