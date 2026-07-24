package com.seproject.profile.service;

import com.seproject.profile.domain.PinnedPost;
import com.seproject.profile.repository.PinnedPostRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class PinnedPostService {

    private final PinnedPostRepository pinnedPostRepository;

    public List<PinnedPost> findByAccountId(Long accountId) {
        return pinnedPostRepository.findByAccountIdOrderByOrderIndexAsc(accountId);
    }

    @Transactional
    public PinnedPost pin(Long accountId, Long postId) {
        if (pinnedPostRepository.existsByAccountIdAndPostId(accountId, postId)) {
            throw new IllegalArgumentException("이미 핀된 게시글입니다.");
        }
        int order = pinnedPostRepository.countByAccountId(accountId);
        return pinnedPostRepository.save(PinnedPost.builder()
                .accountId(accountId).postId(postId).orderIndex(order).build());
    }

    @Transactional
    public void unpin(Long accountId, Long postId) {
        pinnedPostRepository.findByAccountIdAndPostId(accountId, postId)
                .ifPresent(pinnedPostRepository::delete);
    }

    @Transactional
    public void reorder(Long accountId, List<Long> postIds) {
        for (int i = 0; i < postIds.size(); i++) {
            final int idx = i;
            pinnedPostRepository.findByAccountIdAndPostId(accountId, postIds.get(i))
                    .ifPresent(p -> p.updateOrder(idx));
        }
    }
}
