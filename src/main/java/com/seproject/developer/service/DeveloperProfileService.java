package com.seproject.developer.service;

import com.seproject.account.account.domain.Account;
import com.seproject.developer.domain.DeveloperProfile;
import com.seproject.developer.repository.DeveloperProfileRepository;
import com.seproject.skill.domain.SkillTag;
import com.seproject.skill.service.SkillTagService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Optional;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class DeveloperProfileService {

    private final DeveloperProfileRepository developerProfileRepository;
    private final SkillTagService skillTagService;

    public Optional<DeveloperProfile> findByAccount(Account account) {
        return developerProfileRepository.findByAccount(account);
    }

    @Transactional
    public DeveloperProfile upsert(Account account, String intro,
                                    String githubUrl, String portfolioUrl,
                                    String grade, String readmeContent,
                                    List<Long> skillIds) {
        List<SkillTag> skills = skillTagService.findByIds(skillIds);
        return developerProfileRepository.findByAccount(account)
                .map(profile -> {
                    profile.update(intro, githubUrl, portfolioUrl, grade, readmeContent, skills);
                    return profile;
                })
                .orElseGet(() -> {
                    DeveloperProfile profile = DeveloperProfile.builder()
                            .account(account).intro(intro)
                            .githubUrl(githubUrl).portfolioUrl(portfolioUrl)
                            .grade(grade).readmeContent(readmeContent).build();
                    profile.updateSkills(skills);
                    return developerProfileRepository.save(profile);
                });
    }

    public List<Long> findMatchingAccountIds(List<Long> skillIds, Long excludeAccountId) {
        return developerProfileRepository.findAccountIdsBySkills(skillIds, excludeAccountId);
    }
}
