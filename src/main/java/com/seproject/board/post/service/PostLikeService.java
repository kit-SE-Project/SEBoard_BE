package com.seproject.board.post.service;

import com.seproject.board.post.domain.model.LikeType;
import com.seproject.board.post.domain.repository.PostLikeRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Optional;

@Transactional(readOnly = true)
@RequiredArgsConstructor
@Service
public class PostLikeService {

    private final PostLikeRepository postLikeRepository;

    public int countLikes(Long postId) {
        return postLikeRepository.countByPostIdAndLikeType(postId, LikeType.LIKE);
    }

    public int countDislikes(Long postId) {
        return postLikeRepository.countByPostIdAndLikeType(postId, LikeType.DISLIKE);
    }

    public Optional<LikeType> getMyReaction(Long postId, Long memberId) {
        return postLikeRepository.findByPostIdAndMemberId(postId, memberId)
                .map(postLike -> postLike.getLikeType());
    }
}