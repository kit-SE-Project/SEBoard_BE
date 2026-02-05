package com.seproject.board.post.domain.repository;

import com.seproject.admin.post.controller.dto.PostRequest.AdminPostRetrieveCondition;
import com.seproject.admin.post.controller.dto.PostResponse.DeletedPostResponse;
import com.seproject.admin.post.controller.dto.PostResponse.PostRetrieveResponse;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;


public interface AdminPostSearchRepository{
    Page<PostRetrieveResponse> findPostListByCondition(AdminPostRetrieveCondition condition, Pageable pageable);
    Page<DeletedPostResponse> findDeletedPostList(Pageable pageable);
}
