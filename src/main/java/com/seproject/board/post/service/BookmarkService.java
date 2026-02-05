package com.seproject.board.post.service;

import com.seproject.board.post.domain.model.Post;
import com.seproject.board.post.domain.repository.BookmarkRepository;
import com.seproject.member.domain.Member;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Transactional(readOnly = true)
@RequiredArgsConstructor
@Service
public class BookmarkService {

    private final BookmarkRepository bookmarkRepository;

    public boolean isBookmarked(Post post, Member member) {
        Long postId = post.getPostId();
        Long boardUserId = member.getBoardUserId();
        return bookmarkRepository.existsByPostIdAndMemberId(postId, boardUserId);
    }
}
