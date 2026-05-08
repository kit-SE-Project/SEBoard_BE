package com.seproject.recruit.application;

import com.seproject.account.account.domain.Account;
import com.seproject.account.role.domain.Role;
import com.seproject.account.utils.SecurityUtils;
import com.seproject.board.menu.domain.model.RecruitMenu;
import com.seproject.board.menu.domain.repository.RecruitMenuRepository;
import com.seproject.board.post.domain.model.exposeOptions.ExposeState;
import com.seproject.error.errorCode.ErrorCode;
import com.seproject.error.exception.CustomAccessDeniedException;
import com.seproject.error.exception.CustomIllegalArgumentException;
import com.seproject.file.domain.model.FileMetaData;
import com.seproject.file.domain.repository.FileMetaDataRepository;
import com.seproject.member.domain.Member;
import com.seproject.member.service.MemberService;
import com.seproject.recruit.controller.dto.RecruitPostRequest;
import com.seproject.recruit.controller.dto.RecruitPostResponse;
import com.seproject.recruit.domain.model.RecruitPost;
import com.seproject.recruit.domain.model.RecruitTag;
import com.seproject.recruit.domain.model.RecruitType;
import com.seproject.recruit.service.RecruitPostService;
import com.seproject.skill.domain.SkillTag;
import com.seproject.skill.service.SkillTagService;
import lombok.RequiredArgsConstructor;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;

import javax.transaction.Transactional;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

@Transactional
@Service
@RequiredArgsConstructor
public class RecruitAppService {

    private final RecruitPostService recruitPostService;
    private final SkillTagService skillTagService;
    private final MemberService memberService;
    private final FileMetaDataRepository fileMetaDataRepository;
    private final RecruitMenuRepository recruitMenuRepository;
    private final ApplicationEventPublisher eventPublisher;

    /* ── 접근 권한 체크 공통 ── */
    private RecruitMenu getRecruitMenu() {
        return recruitMenuRepository.findWithAuthorizations()
                .orElseThrow(() -> new CustomIllegalArgumentException(ErrorCode.NOT_EXIST_MENU, null));
    }

    private List<Role> getCurrentRoles() {
        return SecurityUtils.getAccount()
                .map(Account::getRoles)
                .orElse(List.of());
    }

    private void checkAccess(RecruitMenu menu) {
        if (!menu.accessible(getCurrentRoles()))
            throw new CustomAccessDeniedException(ErrorCode.ACCESS_DENIED, null);
    }

    private void checkEditable(RecruitMenu menu) {
        if (!menu.editable(getCurrentRoles()))
            throw new CustomAccessDeniedException(ErrorCode.ACCESS_DENIED, null);
    }

    /* ── 목록 조회 ── */
    public Page<RecruitPostResponse.RecruitPostListItem> getList(
            RecruitType type, boolean includeClose, RecruitPostService.SortType sort, Pageable pageable) {
        checkAccess(getRecruitMenu());
        return recruitPostService.findAll(type, includeClose, sort, pageable)
                .map(RecruitPostResponse.RecruitPostListItem::new);
    }

    /* ── 상세 조회 ── */
    public RecruitPostResponse.RecruitPostDetail getDetail(Long id) {
        checkAccess(getRecruitMenu());
        RecruitPost post = recruitPostService.findByIdAndIncrementView(id);
        List<FileMetaData> files = recruitPostService.findAttachments(id);
        return new RecruitPostResponse.RecruitPostDetail(post, files);
    }

    /* ── 작성 ── */
    public RecruitPostResponse.RecruitPostDetail create(RecruitPostRequest.Create request) {
        Long accountId = getAccountId();
        checkEditable(getRecruitMenu());

        List<SkillTag> skills = skillTagService.findByIds(request.getSkillIds());
        Member author = memberService.findByAccountId(accountId);
        List<FileMetaData> attachments = resolveAttachments(request.getAttachmentIds());

        RecruitPost post = recruitPostService.create(
                author,
                RecruitType.valueOf(request.getType()),
                RecruitTag.valueOf(request.getTag()),
                request.getTitle(), request.getContents(),
                skills,
                ExposeState.valueOf(request.getExposeState()),
                request.getPrivatePassword(),
                request.getHeadcount(),
                request.getStartDate(), request.getEndDate(),
                request.getPortfolioUrl(),
                attachments
        );

        List<Long> skillIds = skills.stream().map(SkillTag::getId).collect(Collectors.toList());
        eventPublisher.publishEvent(new RecruitPostCreatedEvent(
                post.getId(), post.getTitle(), post.getType(), accountId, skillIds));

        return new RecruitPostResponse.RecruitPostDetail(post, attachments);
    }

    /* ── 수정 ── */
    public RecruitPostResponse.RecruitPostDetail update(Long id, RecruitPostRequest.Update request) {
        Long accountId = getAccountId();
        RecruitMenu menu = getRecruitMenu();
        checkEditable(menu);

        List<SkillTag> skills = skillTagService.findByIds(request.getSkillIds());
        List<FileMetaData> attachments = resolveAttachments(request.getAttachmentIds());

        RecruitPost post;
        if (menu.manageable(getCurrentRoles())) {
            post = recruitPostService.forceUpdate(
                    id,
                    request.getTitle(), request.getContents(),
                    RecruitTag.valueOf(request.getTag()),
                    skills,
                    request.getHeadcount(),
                    request.getStartDate(), request.getEndDate(),
                    request.getPortfolioUrl(),
                    ExposeState.valueOf(request.getExposeState()),
                    request.getPrivatePassword(),
                    attachments
            );
        } else {
            post = recruitPostService.update(
                    id, accountId,
                    request.getTitle(), request.getContents(),
                    RecruitTag.valueOf(request.getTag()),
                    skills,
                    request.getHeadcount(),
                    request.getStartDate(), request.getEndDate(),
                    request.getPortfolioUrl(),
                    ExposeState.valueOf(request.getExposeState()),
                    request.getPrivatePassword(),
                    attachments
            );
        }

        List<FileMetaData> updatedFiles = recruitPostService.findAttachments(id);
        return new RecruitPostResponse.RecruitPostDetail(post, updatedFiles);
    }

    /* ── 마감 ── */
    public void close(Long id) {
        Long accountId = getAccountId();
        if (getRecruitMenu().manageable(getCurrentRoles())) {
            recruitPostService.forceClose(id);
        } else {
            recruitPostService.close(id, accountId);
        }
    }

    /* ── 삭제 ── */
    public void delete(Long id) {
        Long accountId = getAccountId();
        if (getRecruitMenu().manageable(getCurrentRoles())) {
            recruitPostService.forceDelete(id);
        } else {
            recruitPostService.delete(id, accountId);
        }
    }

    private List<FileMetaData> resolveAttachments(List<Long> ids) {
        if (ids == null || ids.isEmpty()) return new ArrayList<>();
        return fileMetaDataRepository.findAllById(ids);
    }

    private Long getAccountId() {
        return SecurityUtils.getAccount()
                .orElseThrow(() -> new IllegalStateException("로그인이 필요합니다."))
                .getAccountId();
    }
}
