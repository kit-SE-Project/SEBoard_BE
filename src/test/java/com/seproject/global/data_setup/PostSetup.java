package com.seproject.global.data_setup;

import com.seproject.board.common.BaseTime;
import com.seproject.board.menu.domain.model.Category;
import com.seproject.board.post.domain.model.Post;
import com.seproject.board.post.domain.model.exposeOptions.ExposeOption;
import com.seproject.board.post.domain.model.exposeOptions.ExposeState;
import com.seproject.board.post.domain.repository.PostRepository;
import com.seproject.member.domain.BoardUser;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.util.HashSet;
import java.util.UUID;

@Component
public class PostSetup {

    @Autowired
    private PostRepository postRepository;

    public Post createPost(BoardUser boardUser, Category category) {
        Post post = Post.builder()
                .title(UUID.randomUUID().toString())
                .contents(UUID.randomUUID().toString())
                .views(0)
                .pined(false)
                .category(category)
                .exposeOption(ExposeOption.of(ExposeState.PUBLIC, null))
                .anonymousCount(0)
                .reportCount(0)
                .attachments(new HashSet<>())
                .author(boardUser)
                .baseTime(BaseTime.now())
                .build();

        postRepository.save(post);
        return post;
    }

    public Post createPost(BoardUser boardUser, Category category, ExposeState exposeState, String password) {
        Post post = Post.builder()
                .title(UUID.randomUUID().toString())
                .contents(UUID.randomUUID().toString())
                .views(0)
                .pined(false)
                .category(category)
                .exposeOption(ExposeOption.of(exposeState, password))
                .anonymousCount(0)
                .reportCount(0)
                .attachments(new HashSet<>())
                .author(boardUser)
                .baseTime(BaseTime.now())
                .build();

        postRepository.save(post);
        return post;
    }
}
