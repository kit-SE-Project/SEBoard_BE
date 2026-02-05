package com.seproject.admin.post.service;

import com.seproject.board.common.Status;
import com.seproject.board.common.domain.repository.ReportRepository;
import com.seproject.board.menu.domain.model.Category;
import com.seproject.board.post.domain.model.Post;
import com.seproject.board.post.domain.repository.PostRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
@Transactional(readOnly = true)
@RequiredArgsConstructor
public class AdminPostService {

    private final PostRepository postRepository;
    private final ReportRepository reportRepository;

    @Transactional
    public void deleteAllByIds(List<Long> postIds, boolean isPermanent) {
        Status status = isPermanent ? Status.PERMANENT_DELETED : Status.TEMP_DELETED;
        postRepository.deleteAllByIds(postIds,status);
    }

    @Transactional
    public void restore(Post post) {
        post.restore();
        reportRepository.deleteAllByPostId(post.getPostId());
    }

    @Transactional
    public void restore(List<Long> postIds) {
        postRepository.restorePostByPostIds(postIds);
        reportRepository.deleteAllByPostIds(postIds);
    }

    @Transactional
    public void changeCategory(Category from, Category to) {
        postRepository.changeCategory(from,to);
    }
}
