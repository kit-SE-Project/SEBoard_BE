package com.seproject.board.bulletin.controller.dto;

import com.seproject.board.menu.domain.model.Menu;
import lombok.AccessLevel;
import lombok.Builder;
import lombok.Data;
import org.springframework.data.domain.Page;

import java.util.List;

import static com.seproject.board.post.controller.dto.PostResponse.*;

public class MainPageDTO {

    @Data
    @Builder(access = AccessLevel.PRIVATE)
    public static class RetrieveMainPageResponse {
        private Page<RetrievePostListResponseElement> posts;
        private List<RetrievePostListResponseElement> trendingPosts;
        private String menuName;
        private String urlId;

        public static RetrieveMainPageResponse toDTO(
                Page<RetrievePostListResponseElement> posts,
                List<RetrievePostListResponseElement> trendingPosts,
                Menu menu) {
            return builder()
                    .posts(posts)
                    .trendingPosts(trendingPosts)
                    .urlId(menu.getUrlInfo())
                    .menuName(menu.getName())
                    .build();
        }
    }
}
