package com.seproject.board.bulletin.application;

import com.seproject.board.bulletin.domain.model.MainPageMenu;
import com.seproject.board.bulletin.service.MainPageService;
import com.seproject.board.comment.domain.repository.CommentRepository;
import com.seproject.board.menu.domain.model.Menu;
import com.seproject.board.post.application.PostSearchAppService;
import com.seproject.board.post.controller.dto.PostResponse;
import com.seproject.board.post.persistence.PostQueryRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

import static com.seproject.board.bulletin.controller.dto.MainPageDTO.*;

@Transactional(readOnly = true)
@RequiredArgsConstructor
@Service
public class MainPageAppService {

    private final MainPageService mainPageService;
    private final PostSearchAppService postSearchAppService;
    private final PostQueryRepository postQueryRepository;
    private final CommentRepository commentRepository;

    public List<RetrieveMainPageResponse> findMainPagePosts(int size) {

        List<MainPageMenu> mainPageMenus = mainPageService.findAllWithMenu();
        LocalDateTime since = LocalDateTime.now().minusDays(7);

        List<RetrieveMainPageResponse> response = new ArrayList<>();
        for (MainPageMenu mainPageMenu : mainPageMenus) {
            Menu menu = mainPageMenu.getMenu();

            Page<PostResponse.RetrievePostListResponseElement> postList =
                    postSearchAppService.findPostList(menu.getMenuId(), 0, size);

            List<PostResponse.RetrievePostListResponseElement> trendingPosts =
                    postQueryRepository.findTrendingPosts(menu.getMenuId(), since, 3);
            trendingPosts.forEach(postDto -> {
                int commentSize = commentRepository.countCommentsByPostId(postDto.getPostId());
                postDto.setCommentSize(commentSize);
            });

            response.add(RetrieveMainPageResponse.toDTO(postList, trendingPosts, menu));
        }

        return response;
    }

}
