package com.seproject.admin.banned.service;

import com.seproject.admin.banned.domain.BannedId;
import com.seproject.admin.banned.domain.repository.BannedIdRepository;
import com.seproject.error.errorCode.ErrorCode;
import com.seproject.error.exception.CustomIllegalArgumentException;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Optional;

import static com.seproject.admin.banned.controller.dto.BannedIdDTO.*;

@RequiredArgsConstructor
@Transactional(readOnly = true)
@Service
public class BannedIdService {

    private final BannedIdRepository bannedIdRepository;

    public Optional<BannedId> findById(Long id) {
        return bannedIdRepository.findById(id);
    }

    public Optional<BannedId> findById(String bannedId) {
        return bannedIdRepository.findByBannedId(bannedId);
    }

    @Transactional
    public Long createBannedId(String id) {
        BannedId bannedId = BannedId.builder()
                .bannedId(id)
                .build();

        bannedIdRepository.save(bannedId);

        return bannedId.getId();
    }

    @Transactional
    public void deleteBannedId(String id) {
        BannedId bannedId = bannedIdRepository.findByBannedId(id)
                .orElseThrow(() -> new CustomIllegalArgumentException(ErrorCode.NOT_EXIST_BANNED_ID,null));
        bannedIdRepository.delete(bannedId);
    }

    public boolean possibleId(String loginId) {
        String prefix = loginId.split("@")[0];
        Optional<BannedId> byId = findById(prefix);
        return byId.isEmpty();
    }
}
