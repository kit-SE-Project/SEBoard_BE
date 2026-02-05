package com.seproject.board.post.service;

import com.seproject.board.common.BaseTime;
import com.seproject.board.menu.domain.model.Category;
import com.seproject.board.post.domain.model.Post;
import com.seproject.board.post.domain.model.exposeOptions.ExposeOption;
import com.seproject.board.post.domain.repository.PostRepository;
import com.seproject.error.errorCode.ErrorCode;
import com.seproject.error.exception.NoSuchResourceException;
import com.seproject.file.domain.model.FileMetaData;
import com.seproject.member.domain.BoardUser;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.HashSet;
import java.util.List;

@RequiredArgsConstructor
@Transactional(readOnly = true)
@Service
public class PostService {

    private final PostRepository postRepository;

    public Post findById(Long id) {
        return postRepository.findById(id)
                .orElseThrow(()->new NoSuchResourceException(ErrorCode.NOT_EXIST_POST));
    }

    public Post findByIdWithCategory(Long id) {
        return postRepository.findByIdWithCategory(id)
                .orElseThrow(()->new NoSuchResourceException(ErrorCode.NOT_EXIST_POST));
    }

    public List<Post> findAllByIds(List<Long> ids) {
        List<Post> allById = postRepository.findAllById(ids);
        return allById;
    }

    @Transactional
    public Long createPost(String title,
                           String contents,
                           Category category,
                           BoardUser author,
                           BaseTime now,
                           boolean isPined, HashSet<FileMetaData> attachments, ExposeOption exposeOption) {

        Post post = Post.builder()
                .title(title)
                .contents(contents)
                .category(category)
                .author(author)
                .baseTime(now)
                .pined(isPined)
                .attachments(attachments)
                .exposeOption(exposeOption)
                .build();

        postRepository.save(post);

        return post.getPostId();
    }



}
