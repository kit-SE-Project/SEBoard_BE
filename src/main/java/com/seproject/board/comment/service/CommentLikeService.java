package com.seproject.board.comment.service;

import com.seproject.board.comment.domain.model.CommentLike;
import com.seproject.board.comment.domain.repository.CommentLikeRepository;
import com.seproject.board.post.domain.model.LikeType;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Optional;

@Transactional(readOnly = true)
@RequiredArgsConstructor
@Service
public class CommentLikeService {

    private final CommentLikeRepository commentLikeRepository;

    public int countLikes(Long commentId) {
        return commentLikeRepository.countByCommentIdAndLikeType(commentId, LikeType.LIKE);
    }

    public int countDislikes(Long commentId) {
        return commentLikeRepository.countByCommentIdAndLikeType(commentId, LikeType.DISLIKE);
    }

    public Optional<LikeType> getMyReaction(Long commentId, Long memberId) {
        return commentLikeRepository.findByCommentIdAndMemberId(commentId, memberId)
                .map(CommentLike::getLikeType);
    }
}