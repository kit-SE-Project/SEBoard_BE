package com.seproject.board.bulletin.application;

import com.seproject.board.bulletin.domain.model.MainPageMenu;
import com.seproject.board.bulletin.service.MainPageService;
import com.seproject.board.menu.domain.model.Menu;
import com.seproject.board.post.application.PostSearchAppService;
import com.seproject.board.post.controller.dto.PostResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.List;

import static com.seproject.board.bulletin.controller.dto.MainPageDTO.*;

@Transactional(readOnly = true)
@RequiredArgsConstructor
@Service
public class MainPageAppService {

    private final MainPageService mainPageService;
    private final PostSearchAppService postSearchAppService;

    public List<RetrieveMainPageResponse> findMainPagePosts(int size) {

        List<MainPageMenu> mainPageMenus = mainPageService.findAllWithMenu();

        List<RetrieveMainPageResponse> response = new ArrayList<>();
        for (MainPageMenu mainPageMenu : mainPageMenus) {
            Menu menu = mainPageMenu.getMenu();

            Page<PostResponse.RetrievePostListResponseElement> postList =
                    postSearchAppService.findPostList(menu.getMenuId(), 0, size);

            response.add(RetrieveMainPageResponse.toDTO(postList,menu));
        }

        return response;
    }

}
