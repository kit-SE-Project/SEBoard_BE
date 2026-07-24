package com.seproject.recruit.service;

import com.seproject.board.post.domain.model.exposeOptions.ExposeOption;
import com.seproject.board.post.domain.model.exposeOptions.ExposeState;
import com.seproject.file.domain.model.AttachableType;
import com.seproject.file.domain.model.FileMetaData;
import com.seproject.file.domain.repository.FileMetaDataRepository;
import com.seproject.member.domain.BoardUser;
import com.seproject.recruit.domain.model.*;
import com.seproject.recruit.domain.repository.RecruitCommentRepository;
import com.seproject.recruit.domain.repository.RecruitPostRepository;
import com.seproject.skill.domain.SkillTag;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class RecruitPostService {

    private final RecruitPostRepository recruitPostRepository;
    private final RecruitCommentRepository recruitCommentRepository;
    private final FileMetaDataRepository fileMetaDataRepository;

    public RecruitPost findById(Long id) {
        RecruitPost post = recruitPostRepository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("게시글을 찾을 수 없습니다."));
        if (post.getStatus() == RecruitStatus.DELETED) throw new IllegalArgumentException("삭제된 게시글입니다.");
        return post;
    }

    @Transactional
    public RecruitPost findByIdAndIncrementView(Long id) {
        RecruitPost post = findById(id);
        post.incrementViewCount();
        return post;
    }

    public List<FileMetaData> findAttachments(Long postId) {
        return fileMetaDataRepository.findByAttachableTypeAndAttachableId(
                AttachableType.RECRUIT_POST, postId);
    }

    public enum SortType { LATEST, DEADLINE, VIEWS }

    public Page<RecruitPost> findAll(RecruitType type, boolean includeClose, SortType sort, Pageable pageable) {
        boolean activeOnly = !includeClose;
        if (sort == SortType.VIEWS) {
            if (type != null) return activeOnly
                    ? recruitPostRepository.findByTypeAndStatusOrderByViewCountDesc(type, RecruitStatus.ACTIVE, pageable)
                    : recruitPostRepository.findByTypeExcludeDeletedOrderByViewCountDesc(type, pageable);
            return activeOnly
                    ? recruitPostRepository.findByStatusOrderByViewCountDesc(RecruitStatus.ACTIVE, pageable)
                    : recruitPostRepository.findAllExcludeDeletedOrderByViewCountDesc(pageable);
        }
        if (sort == SortType.DEADLINE) {
            if (type != null) return activeOnly
                    ? recruitPostRepository.findByTypeAndStatusOrderByDeadline(type, RecruitStatus.ACTIVE, pageable)
                    : recruitPostRepository.findByTypeExcludeDeletedOrderByDeadline(type, pageable);
            return activeOnly
                    ? recruitPostRepository.findByStatusOrderByDeadline(RecruitStatus.ACTIVE, pageable)
                    : recruitPostRepository.findAllExcludeDeletedOrderByDeadline(pageable);
        }
        // LATEST
        if (type != null) return activeOnly
                ? recruitPostRepository.findByTypeAndStatusOrderByCreatedAtDesc(type, RecruitStatus.ACTIVE, pageable)
                : recruitPostRepository.findByTypeExcludeDeletedOrderByCreatedAtDesc(type, pageable);
        return activeOnly
                ? recruitPostRepository.findByStatusOrderByCreatedAtDesc(RecruitStatus.ACTIVE, pageable)
                : recruitPostRepository.findAllExcludeDeletedOrderByCreatedAtDesc(pageable);
    }

    public List<RecruitPost> findForMainPage() {
        return recruitPostRepository.findTop6ByStatusOrderByCreatedAtDesc(RecruitStatus.ACTIVE);
    }

    @Transactional
    public RecruitPost create(BoardUser author, RecruitType type, RecruitTag tag,
                              String title, String contents, List<SkillTag> skills,
                              ExposeState exposeState, String privatePassword,
                              Integer headcount, LocalDate startDate, LocalDate endDate,
                              String portfolioUrl, List<FileMetaData> attachments) {
        ExposeOption exposeOption = ExposeOption.of(exposeState, privatePassword);
        RecruitPost post = recruitPostRepository.save(RecruitPost.builder()
                .type(type).tag(tag).title(title).contents(contents)
                .author(author).exposeOption(exposeOption)
                .headcount(headcount).startDate(startDate).endDate(endDate)
                .portfolioUrl(portfolioUrl).skills(skills)
                .build());
        attachments.forEach(f -> f.attachTo(AttachableType.RECRUIT_POST, post.getId()));
        return post;
    }

    @Transactional
    public RecruitPost update(Long id, Long accountId, String title, String contents,
                              RecruitTag tag, List<SkillTag> skills,
                              Integer headcount, LocalDate startDate, LocalDate endDate,
                              String portfolioUrl, ExposeState exposeState, String privatePassword,
                              List<FileMetaData> newAttachments) {
        RecruitPost post = findById(id);
        if (!post.isWrittenBy(accountId)) throw new IllegalArgumentException("수정 권한이 없습니다.");
        if (post.getStatus() == RecruitStatus.CLOSED) throw new IllegalArgumentException("마감된 게시글은 수정할 수 없습니다.");

        ExposeOption exposeOption = ExposeOption.of(exposeState, privatePassword);
        post.update(title, contents, tag, headcount, startDate, endDate, portfolioUrl, skills, exposeOption);
        syncAttachments(id, newAttachments);
        return post;
    }

    @Transactional
    public RecruitPost forceUpdate(Long id, String title, String contents,
                                   RecruitTag tag, List<SkillTag> skills,
                                   Integer headcount, LocalDate startDate, LocalDate endDate,
                                   String portfolioUrl, ExposeState exposeState, String privatePassword,
                                   List<FileMetaData> newAttachments) {
        RecruitPost post = findById(id);
        if (post.getStatus() == RecruitStatus.CLOSED) throw new IllegalArgumentException("마감된 게시글은 수정할 수 없습니다.");
        ExposeOption exposeOption = ExposeOption.of(exposeState, privatePassword);
        post.update(title, contents, tag, headcount, startDate, endDate, portfolioUrl, skills, exposeOption);
        syncAttachments(id, newAttachments);
        return post;
    }

    @Transactional
    public void close(Long id, Long accountId) {
        RecruitPost post = findById(id);
        if (!post.isWrittenBy(accountId)) throw new IllegalArgumentException("권한이 없습니다.");
        post.close();
    }

    @Transactional
    public void forceClose(Long id) {
        findById(id).close();
    }

    @Transactional
    public void delete(Long id, Long accountId) {
        RecruitPost post = findById(id);
        if (!post.isWrittenBy(accountId)) throw new IllegalArgumentException("권한이 없습니다.");
        removePostCascade(post);
    }

    @Transactional
    public void forceDelete(Long id) {
        removePostCascade(findById(id));
    }

    // ── 공통 삭제 로직 (일반 Post.removePost 와 동일 구조) ──────────────────────
    private void removePostCascade(RecruitPost post) {
        Long postId = post.getId();

        // 1. 댓글 소프트 삭제 (일반 Post: comments.forEach(comment -> comment.delete(true)))
        recruitCommentRepository.softDeleteAllByPostId(postId);

        // 2. 첨부파일 메타 연결 해제 (일반 Post와 동일하게 물리 파일은 보존)
        fileMetaDataRepository.findByAttachableTypeAndAttachableId(AttachableType.RECRUIT_POST, postId)
                .forEach(f -> f.attachTo(null, null));

        // 3. 게시글 소프트 삭제
        post.softDelete();
    }

    // ── 첨부파일 동기화 ──────────────────────────────────────────────────────────
    private void syncAttachments(Long postId, List<FileMetaData> newAttachments) {
        List<FileMetaData> existing = fileMetaDataRepository
                .findByAttachableTypeAndAttachableId(AttachableType.RECRUIT_POST, postId);
        Set<Long> newIds = newAttachments.stream()
                .map(FileMetaData::getFileMetaDataId).collect(Collectors.toSet());
        existing.stream()
                .filter(f -> !newIds.contains(f.getFileMetaDataId()))
                .forEach(f -> f.attachTo(null, null));
        newAttachments.forEach(f -> f.attachTo(AttachableType.RECRUIT_POST, postId));
    }
}
