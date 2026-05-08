package com.seproject.profile.repository;

import com.seproject.profile.domain.PinnedPost;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface PinnedPostRepository extends JpaRepository<PinnedPost, Long> {
    List<PinnedPost> findByAccountIdOrderByOrderIndexAsc(Long accountId);
    Optional<PinnedPost> findByAccountIdAndPostId(Long accountId, Long postId);
    boolean existsByAccountIdAndPostId(Long accountId, Long postId);
    int countByAccountId(Long accountId);
}
