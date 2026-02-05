package com.seproject.admin.banned.service;

import com.seproject.admin.banned.domain.BannedNickname;
import com.seproject.admin.banned.domain.repository.BannedNicknameRepository;
import com.seproject.error.errorCode.ErrorCode;
import com.seproject.error.exception.CustomIllegalArgumentException;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.Optional;

import static com.seproject.admin.banned.controller.dto.BannedNicknameDTO.*;
@RequiredArgsConstructor
@Service
public class BannedNicknameService {
    private final BannedNicknameRepository bannedNicknameRepository;

    public boolean possibleNickname(String nickname) {
        return !bannedNicknameRepository.existsByBannedNickname(nickname);
    }

    public Optional<BannedNickname> findById(Long id) {
        return bannedNicknameRepository.findById(id);
    }

    public Optional<BannedNickname> findByNickname(String nickname) {
        return bannedNicknameRepository.findByBannedNickname(nickname);
    }

    public Long createBannedNickname(String nickname) {
        BannedNickname bannedNickname = BannedNickname.builder()
                .bannedNickname(nickname)
                .build();

        bannedNicknameRepository.save(bannedNickname);
        return bannedNickname.getId();
    }

    public void deleteBannedNickname(String nickname) {
        BannedNickname bannedNickname = bannedNicknameRepository.findByBannedNickname(nickname)
                .orElseThrow(() -> new CustomIllegalArgumentException(ErrorCode.NOT_EXIST_BANNED_NICKNAME,null));
        bannedNicknameRepository.delete(bannedNickname);
    }

}
