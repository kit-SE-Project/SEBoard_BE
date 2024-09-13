package com.seproject.board.comment.controller;

import com.seproject.board.comment.application.dto.CommentCommand;
import com.seproject.board.comment.controller.dto.CommentRequest;
import com.seproject.global.ControllerTestSupport;
import org.junit.jupiter.api.Test;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.*;
import static org.springframework.http.MediaType.APPLICATION_JSON;
import static org.springframework.restdocs.mockmvc.MockMvcRestDocumentation.document;
import static org.springframework.restdocs.mockmvc.RestDocumentationRequestBuilders.*;
import static org.springframework.restdocs.payload.PayloadDocumentation.*;
import static org.springframework.restdocs.request.RequestDocumentation.parameterWithName;
import static org.springframework.restdocs.request.RequestDocumentation.pathParameters;
import static org.springframework.test.web.servlet.result.MockMvcResultHandlers.print;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

class CommentDocsControllerTest extends ControllerTestSupport {

	@Test
	void 댓글생성() throws Exception{
		given(commentAppService.writeComment(any(CommentCommand.CommentWriteCommand.class)))
				.willReturn(1L);

		CommentRequest.CreateCommentRequest createCommentRequest = new CommentRequest.CreateCommentRequest();
		createCommentRequest.setPostId(1L);
		createCommentRequest.setContents("contents");
		createCommentRequest.setAnonymous(true);
		createCommentRequest.setReadOnlyAuthor(true);

		String reqBody = om.writeValueAsString(createCommentRequest);

		mockMvc.perform(
				post("/comments")
						.contentType(APPLICATION_JSON)
						.content(reqBody)
		)
				.andDo(print())
				.andDo(
						document(
								"comments/write-comment",
								requestFields(
										fieldWithPath("postId").description("게시글 id"),
										fieldWithPath("contents").description("내용"),
										fieldWithPath("isAnonymous").description("익명 여부"),
										fieldWithPath("isReadOnlyAuthor").description("작성자만 읽기 여부")
								),
								responseFields(
										fieldWithPath("message").description("메시지"),
										fieldWithPath("id").description("생성된 댓글 id")
								)
				));

	}

	@Test
	void updateComment() throws Exception {
		given(commentAppService.editComment(any(CommentCommand.CommentEditCommand.class)))
				.willReturn(1L);

		CommentRequest.UpdateCommentRequest updateCommentRequest = new CommentRequest.UpdateCommentRequest();
		updateCommentRequest.setContents("contents");
		updateCommentRequest.setReadOnlyAuthor(true);

		String reqBody = om.writeValueAsString(updateCommentRequest);

		mockMvc.perform(
				put("/comments/{id}", 1L)
						.contentType(APPLICATION_JSON)
						.content(reqBody)
		)
				.andDo(print())
				.andDo(
						document(
								"comments/update-comment",
								requestFields(
										fieldWithPath("contents").description("내용"),
										fieldWithPath("isReadOnlyAuthor").description("작성자만 읽기 여부")
								),
								responseFields(
										fieldWithPath("message").description("메시지"),
										fieldWithPath("id").description("수정된 댓글 id")
								)
				));

	}

	@Test
	void deleteNamedComment() throws Exception {
		Long commentId = 1L;

		doNothing().when(commentAppService).removeComment(commentId);

		mockMvc.perform(
						delete("/comments/{commentId}", commentId)
								.contentType(APPLICATION_JSON)
				)
				.andExpect(status().isOk())
				.andExpect(content().contentType(APPLICATION_JSON))
				.andExpect(jsonPath("$.message").value("댓글 삭제 성공"))
				.andDo(print())
				.andDo(
						document(
								"comments/delete-comment",
								pathParameters(
										parameterWithName("commentId").description("삭제할 댓글 id")
								),
								responseFields(
										fieldWithPath("message").description("메시지")
								)
						)
				);

		verify(commentAppService, times(1)).removeComment(commentId);
	}
}