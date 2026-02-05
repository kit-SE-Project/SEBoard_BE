package com.seproject.board.bulletin.controller.dto;

import com.seproject.board.menu.domain.model.Menu;
import lombok.AccessLevel;
import lombok.Builder;
import lombok.Data;
import org.springframework.data.domain.Page;

import static com.seproject.board.post.controller.dto.PostResponse.*;

public class MainPageDTO {

    @Data
    @Builder(access = AccessLevel.PRIVATE)
    public static class RetrieveMainPageResponse {
        private Page<RetrievePostListResponseElement> posts;
        private String menuName;
        private String urlId;

        public static RetrieveMainPageResponse toDTO(Page<RetrievePostListResponseElement> posts, Menu menu) {
            return builder()
                    .posts(posts)
                    .urlId(menu.getUrlInfo())
                    .menuName(menu.getName())
                    .build();
        }
    }
}
