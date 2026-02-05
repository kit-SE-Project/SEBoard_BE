package com.seproject.board.post.domain.repository;

import com.seproject.board.post.controller.dto.PostResponse.RetrievePostDetailResponse;
import com.seproject.board.post.controller.dto.PostResponse.RetrievePostListResponseElement;
import com.seproject.board.post.domain.model.Post;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.repository.NoRepositoryBean;
import org.springframework.data.repository.Repository;

import java.util.List;
import java.util.Optional;

@NoRepositoryBean
public interface PostSearchRepository extends Repository<Post, Long> {
    Integer countsMemberPostByLoginId(String loginId);
    Integer countsPostByLoginId(String loginId);
    Page<RetrievePostListResponseElement> findBookmarkPostByLoginId(String loginId, Pageable pagingInfo);
    Page<RetrievePostListResponseElement> findMemberPostByLoginId(String loginId, Pageable pagingInfo);
    Page<RetrievePostListResponseElement> findPostByLoginId(String loginId, Pageable pagingInfo);
    List<RetrievePostListResponseElement> findPinedPostByCategoryId(Long categoryId);
    Page<RetrievePostListResponseElement> findPostByCategoryId(Long categoryId, Pageable pagingInfo);
}

