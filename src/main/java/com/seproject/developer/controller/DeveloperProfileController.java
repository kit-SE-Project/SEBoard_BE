package com.seproject.developer.controller;

import com.seproject.account.account.domain.Account;
import com.seproject.account.utils.SecurityUtils;
import com.seproject.developer.domain.DeveloperProfile;
import com.seproject.developer.service.DeveloperProfileService;
import lombok.Data;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/developer-profile")
@RequiredArgsConstructor
public class DeveloperProfileController {

    private final DeveloperProfileService developerProfileService;

    @PutMapping
    public ResponseEntity<DeveloperProfileResponse> upsert(@RequestBody DeveloperProfileRequest request) {
        Account account = SecurityUtils.getAccount()
                .orElseThrow(() -> new IllegalStateException("로그인이 필요합니다."));
        DeveloperProfile profile = developerProfileService.upsert(
                account, request.getIntro(),
                request.getGithubUrl(), request.getPortfolioUrl(),
                request.getGrade(), request.getReadmeContent(),
                request.getSkillIds());
        return ResponseEntity.ok(new DeveloperProfileResponse(profile));
    }

    @Data
    public static class DeveloperProfileRequest {
        private String intro;
        private String githubUrl;
        private String portfolioUrl;
        private String grade;
        private String readmeContent;
        private List<Long> skillIds;
    }
}
