package com.seproject.admin.banned.service;

import com.seproject.account.account.domain.Account;
import com.seproject.account.utils.SecurityUtils;
import com.seproject.admin.banned.controller.dto.SpamWordRequest.SpamWordCreateRequest;
import com.seproject.admin.banned.controller.dto.SpamWordResponse;
import com.seproject.admin.banned.domain.SpamWord;
import com.seproject.admin.banned.domain.repository.SpamWordRepository;
import com.seproject.admin.dashboard.domain.DashBoardMenu;
import com.seproject.admin.dashboard.service.AdminDashBoardServiceImpl;
import com.seproject.error.errorCode.ErrorCode;
import com.seproject.error.exception.CustomAccessDeniedException;
import com.seproject.error.exception.CustomAuthenticationException;
import com.seproject.error.exception.CustomIllegalArgumentException;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
@RequiredArgsConstructor
@Transactional
public class SpamWordService {
    private final SpamWordRepository spamWordRepository;

    private final AdminDashBoardServiceImpl dashBoardService;

    //TODO : 추후 AOP로 변경 필요
    private void checkAuthorization(){
        Account account = SecurityUtils.getAccount()
                .orElseThrow(() -> new CustomAuthenticationException(ErrorCode.NOT_LOGIN, null));

        DashBoardMenu dashboardmenu = dashBoardService.findDashBoardMenuByUrl(DashBoardMenu.GENERAL_URL);

        if(!dashboardmenu.authorize(account.getRoles())){
            throw new CustomAccessDeniedException(ErrorCode.ACCESS_DENIED, null);
        }
    }

    public List<SpamWordResponse> findAll() {
        //TODO : 개수 제한?
        return spamWordRepository.findAllResponse();
    }


    public void createSpamWord(SpamWordCreateRequest request) {
        checkAuthorization();

        if(spamWordRepository.existsByWord(request.getWord())){
            throw new CustomIllegalArgumentException(ErrorCode.INVALID_REQUEST, null);
        }

        if(!request.getWord().isBlank()){
            SpamWord spamWord = SpamWord.of(request.getWord());
            spamWordRepository.save(spamWord);
        }
    }

    public void deleteSpamWord(Long id){
        checkAuthorization();

        if(spamWordRepository.findById(id).isEmpty()){
            throw new CustomIllegalArgumentException(ErrorCode.INVALID_REQUEST, null);
        }

        spamWordRepository.deleteById(id);
    }
}
