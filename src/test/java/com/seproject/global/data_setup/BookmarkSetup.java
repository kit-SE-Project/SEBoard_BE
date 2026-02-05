package com.seproject.global.data_setup;

import com.seproject.board.post.domain.model.Bookmark;
import com.seproject.board.post.domain.model.Post;
import com.seproject.board.post.domain.repository.BookmarkRepository;
import com.seproject.member.domain.Member;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

@Component
public class BookmarkSetup {

    @Autowired
    private BookmarkRepository bookmarkRepository;

    public Bookmark createBookmark(Post post, Member member) {

        Bookmark bookmark = Bookmark.builder()
                .markedPost(post)
                .member(member)
                .build();

        bookmarkRepository.save(bookmark);

        return bookmark;
    }
}
