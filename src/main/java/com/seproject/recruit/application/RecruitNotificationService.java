package com.seproject.recruit.application;

import com.seproject.account.account.domain.repository.AccountRepository;
import com.seproject.developer.repository.DeveloperProfileRepository;
import com.seproject.notification.NotificationEventDto;
import com.seproject.notification.NotificationEventPublisher;
import com.seproject.recruit.domain.model.RecruitType;
import com.seproject.recruit.domain.repository.RecruitPostRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.transaction.event.TransactionPhase;
import org.springframework.transaction.event.TransactionalEventListener;

import java.util.List;

@Slf4j
@Service
@RequiredArgsConstructor
public class RecruitNotificationService {

    private final NotificationEventPublisher notificationEventPublisher;
    private final DeveloperProfileRepository developerProfileRepository;
    private final RecruitPostRepository recruitPostRepository;
    private final AccountRepository accountRepository;

    @Async
    @Transactional(readOnly = true)
    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
    public void handleRecruitPostCreated(RecruitPostCreatedEvent event) {
        try {
            if (event.getType() == RecruitType.RECRUIT) {
                publishForRecruitPost(event);
            } else {
                publishForSeekPost(event);
            }
        } catch (Exception e) {
            log.warn("Recruit 알림 발행 실패 - postId: {}", event.getPostId(), e);
        }
    }

    /** 구인 글 등록 시 알림 */
    private void publishForRecruitPost(RecruitPostCreatedEvent event) {
        // 알림 3: 구인 글 전체 브로드캐스트 (NEW_RECRUIT_POST)
        List<Long> allIds = accountRepository.findAllActiveAccountIds();
        for (Long accountId : allIds) {
            if (accountId.equals(event.getAuthorAccountId())) continue;
            notificationEventPublisher.publish(NotificationEventDto.builder()
                    .type("NEW_RECRUIT_POST")
                    .receiverId(accountId)
                    .relatedId(event.getPostId())
                    .title("새 구인 글이 등록되었습니다.")
                    .content(event.getTitle())
                    .build());
        }

        // 알림 1: 내 기술 스택과 맞는 구인 글 (RECRUIT_SKILL_MATCH)
        if (!event.getSkillIds().isEmpty()) {
            List<Long> matching = developerProfileRepository
                    .findAccountIdsBySkills(event.getSkillIds(), event.getAuthorAccountId());
            for (Long accountId : matching) {
                notificationEventPublisher.publish(NotificationEventDto.builder()
                        .type("RECRUIT_SKILL_MATCH")
                        .receiverId(accountId)
                        .relatedId(event.getPostId())
                        .title("내 기술 스택과 맞는 구인 글이 등록되었습니다.")
                        .content(event.getTitle())
                        .build());
            }
        }
    }

    /** 구직 글 등록 시 알림 */
    private void publishForSeekPost(RecruitPostCreatedEvent event) {
        // 알림 4: 구직 글 전체 브로드캐스트 (NEW_SEEK_POST)
        List<Long> allIds = accountRepository.findAllActiveAccountIds();
        for (Long accountId : allIds) {
            if (accountId.equals(event.getAuthorAccountId())) continue;
            notificationEventPublisher.publish(NotificationEventDto.builder()
                    .type("NEW_SEEK_POST")
                    .receiverId(accountId)
                    .relatedId(event.getPostId())
                    .title("새 구직 글이 등록되었습니다.")
                    .content(event.getTitle())
                    .build());
        }

        // 알림 2: 내 구인 글과 기술이 맞는 구직자 (SEEK_SKILL_MATCH)
        if (!event.getSkillIds().isEmpty()) {
            List<Long> recruitAuthors = recruitPostRepository
                    .findActiveRecruitAuthorAccountIdsBySkillIds(
                            event.getSkillIds(), event.getAuthorAccountId());
            for (Long accountId : recruitAuthors) {
                notificationEventPublisher.publish(NotificationEventDto.builder()
                        .type("SEEK_SKILL_MATCH")
                        .receiverId(accountId)
                        .relatedId(event.getPostId())
                        .title("내 구인 글과 기술이 맞는 구직자가 등록되었습니다.")
                        .content(event.getTitle())
                        .build());
            }
        }
    }
}
