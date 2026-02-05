package com.seproject.board.post.docs;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.seproject.board.post.application.dto.PostCommand;
import com.seproject.board.post.controller.dto.ExposeOptionRequest;
import com.seproject.board.post.controller.dto.PostRequest;
import com.seproject.global.ControllerTestSupport;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.http.MediaType;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.BDDMockito.given;
import static org.springframework.restdocs.mockmvc.MockMvcRestDocumentation.document;
import static org.springframework.restdocs.mockmvc.RestDocumentationRequestBuilders.post;
import static org.springframework.restdocs.payload.PayloadDocumentation.*;
import static org.springframework.test.web.servlet.result.MockMvcResultHandlers.print;

public class PostDocsControllerTest extends ControllerTestSupport {

    @DisplayName("")
    @Test
    void test() throws Exception {
        given(postAppService.writePost(any(PostCommand.PostWriteCommand.class)))
                .willReturn(1L);

        PostRequest.CreatePostRequest createPostRequest = PostRequest.CreatePostRequest.builder()
                .title("title")
                .contents("contents")
                .categoryId(1L)
                .pined(false)
                .exposeOption(new ExposeOptionRequest("PUBLIC"))
                .isAnonymous(true)
                .build();

        String reqBody = om.writeValueAsString(createPostRequest);

        //when //then
        mockMvc.perform(
                post("/posts")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(reqBody)
        )
                .andDo(print())
                .andDo(
                        document(
                                "posts/write-post",
                                requestFields(
                                        fieldWithPath("title").description("제목"),
                                        fieldWithPath("contents").description("내용"),
                                        fieldWithPath("attachmentIds").description("첨부파일 id 리스트"),
                                        fieldWithPath("categoryId").description("카테고리 id"),
                                        fieldWithPath("pined").description("고정 여부"),
                                        subsectionWithPath("exposeOption").description("공개 설정"),
                                        fieldWithPath("anonymous").description("익명 여부"),
                                        fieldWithPath("isSyncOldVersion").description("이전 버전과 동기화 여부")
                                ),
                                responseFields(
                                        fieldWithPath("message").description("메시지"),
                                        fieldWithPath("id").description("생성된 게시글 id")
                                )
                ));
    }
}
