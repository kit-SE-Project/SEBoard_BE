package com.seproject.admin.post.docs;

import static org.mockito.ArgumentMatchers.*;
import static org.mockito.BDDMockito.doNothing;
import static org.mockito.Mockito.*;
import static org.springframework.restdocs.mockmvc.MockMvcRestDocumentation.*;
import static org.springframework.restdocs.mockmvc.RestDocumentationRequestBuilders.*;
import static org.springframework.restdocs.operation.preprocess.Preprocessors.*;
import static org.springframework.restdocs.payload.PayloadDocumentation.*;
import static org.springframework.restdocs.request.RequestDocumentation.*;
import static org.springframework.test.web.servlet.result.MockMvcResultHandlers.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

import java.util.List;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.Pageable;
import org.springframework.http.MediaType;
import org.springframework.restdocs.payload.JsonFieldType;

import com.seproject.admin.post.application.AdminPostAppService;
import com.seproject.admin.post.application.PostSyncService;
import com.seproject.admin.post.controller.AdminPostController;
import com.seproject.admin.post.controller.dto.AdminOldPost;
import com.seproject.admin.post.controller.dto.PostRequest;
import com.seproject.admin.post.controller.dto.PostRequest.BulkPostRequest;
import com.seproject.admin.post.controller.dto.PostResponse;

class AdminPostDocsControllerTest extends RestDocsSupport {

	private AdminPostAppService adminPostAppService = mock(AdminPostAppService.class);
	private PostSyncService postSyncService = mock(PostSyncService.class);

	@Override
	protected Object initController() {
		return new AdminPostController(adminPostAppService, postSyncService);
	}

	@DisplayName("Old 게시글 가져오기")
	@Test
	void createOldSePosts() throws Exception {
		AdminOldPost adminOldPost = new AdminOldPost();
		adminOldPost.setTitle("Old Post Title");
		// Set other fields if necessary to match the actual payload

		when(postSyncService.importOldPost(any(AdminOldPost.class)))
			.thenReturn("Success");

		mockMvc.perform(post("/admin/posts/old")
				.contentType(MediaType.APPLICATION_JSON)
				.content(objectMapper.writeValueAsString(adminOldPost)))
			.andDo(print())
			.andExpect(status().isCreated())
			.andDo(document("admin-post-create-old",
				preprocessRequest(prettyPrint()),
				preprocessResponse(prettyPrint()),
				requestFields(
					fieldWithPath("title").type(JsonFieldType.STRING).description("Old 게시글 제목"),
					fieldWithPath("author").type(JsonFieldType.STRING).optional().description("게시글 작성자"),
					fieldWithPath("contents").type(JsonFieldType.STRING).optional().description("게시글 내용"),
					fieldWithPath("views").type(JsonFieldType.NUMBER).optional().description("조회수"),
					fieldWithPath("createdAt").type(JsonFieldType.STRING).optional().description("생성 날짜"),
					fieldWithPath("updatedAt").type(JsonFieldType.STRING).optional().description("수정 날짜"),
					fieldWithPath("categoryId").type(JsonFieldType.NUMBER).optional().description("카테고리 ID"),
					fieldWithPath("comments").type(JsonFieldType.ARRAY).optional().description("댓글 목록")
				),
				responseBody()));
	}

	@DisplayName("게시글 목록 조회")
	@Test
	void retrieveAllPost() throws Exception {
		PageImpl<PostResponse.PostRetrieveResponse> postPage = new PageImpl<>(List.of());

		when(adminPostAppService.findPostList(any(PostRequest.AdminPostRetrieveCondition.class), anyInt(), anyInt()))
			.thenReturn(postPage);

		mockMvc.perform(get("/admin/posts")
				.param("page", "0")
				.param("perPage", "25")
				.contentType(MediaType.APPLICATION_JSON))
			.andDo(print())
			.andExpect(status().isOk())
			.andDo(document("admin-post-retrieve-all",
				preprocessRequest(prettyPrint()),
				preprocessResponse(prettyPrint()),
				requestParameters(
					parameterWithName("page").description("페이지 번호"),
					parameterWithName("perPage").description("한 페이지 당 항목 수"),
					parameterWithName("categoryId").optional().description("카테고리 ID"),
					parameterWithName("exposeOption").optional().description("노출 옵션"),
					parameterWithName("searchOption").optional().description("검색 옵션"),
					parameterWithName("isReported").optional().description("신고 여부"),
					parameterWithName("query").optional().description("검색어")
				),
				responseFields(
					fieldWithPath("content").type(JsonFieldType.ARRAY).description("게시글 목록"),
					fieldWithPath("pageable").type(JsonFieldType.STRING).description("페이징 정보 (String 타입, 값은 'INSTANCE')"),
					fieldWithPath("totalPages").type(JsonFieldType.NUMBER).description("총 페이지 수"),
					fieldWithPath("totalElements").type(JsonFieldType.NUMBER).description("총 요소 수"),
					fieldWithPath("last").type(JsonFieldType.BOOLEAN).description("마지막 페이지 여부"),
					fieldWithPath("number").type(JsonFieldType.NUMBER).description("현재 페이지 번호"),
					fieldWithPath("size").type(JsonFieldType.NUMBER).description("페이지 크기"),
					fieldWithPath("numberOfElements").type(JsonFieldType.NUMBER).description("현재 페이지의 요소 수"),
					fieldWithPath("first").type(JsonFieldType.BOOLEAN).description("첫 페이지 여부"),
					fieldWithPath("empty").type(JsonFieldType.BOOLEAN).description("페이지가 비어있는지 여부"),
					fieldWithPath("sort.empty").type(JsonFieldType.BOOLEAN).description("정렬 정보가 비어있는지 여부"),
					fieldWithPath("sort.unsorted").type(JsonFieldType.BOOLEAN).description("정렬되지 않았는지 여부"),
					fieldWithPath("sort.sorted").type(JsonFieldType.BOOLEAN).description("정렬 여부")
				)
			));
	}


	@DisplayName("게시글 복구 - 단일")
	@Test
	void restorePost() throws Exception {
		doNothing().when(adminPostAppService).restorePost(any(Long.class));

		mockMvc.perform(post("/admin/posts/{postId}/restore", 1L)
				.contentType(MediaType.APPLICATION_JSON))
			.andDo(print())
			.andExpect(status().isOk())
			.andDo(document("admin-post-restore",
				preprocessRequest(prettyPrint()),
				preprocessResponse(prettyPrint()),
				responseFields(
					fieldWithPath("message").type(JsonFieldType.STRING).description("응답 메시지")
				)));
	}

	@DisplayName("게시글 복구 - 대량")
	@Test
	void restoreBulkPost() throws Exception {
		BulkPostRequest bulkPostRequest = new BulkPostRequest();
		bulkPostRequest.setPostIds(List.of(1L, 2L, 3L));

		doNothing().when(adminPostAppService).restoreBulkPost(any());

		mockMvc.perform(post("/admin/posts/restore")
				.contentType(MediaType.APPLICATION_JSON)
				.content(objectMapper.writeValueAsString(bulkPostRequest)))
			.andDo(print())
			.andExpect(status().isOk())
			.andDo(document("admin-post-bulk-restore",
				preprocessRequest(prettyPrint()),
				preprocessResponse(prettyPrint()),
				requestFields(
					fieldWithPath("postIds").type(JsonFieldType.ARRAY).description("복구할 게시글 ID 목록")
				),
				responseFields(
					fieldWithPath("message").type(JsonFieldType.STRING).description("응답 메시지")
				)));
	}

	@DisplayName("게시글 휴지통 이동")
	@Test
	void deleteBulkPost() throws Exception {
		BulkPostRequest bulkPostRequest = new BulkPostRequest();
		bulkPostRequest.setPostIds(List.of(1L, 2L, 3L));

		doNothing().when(adminPostAppService).deleteBulkPost(any(), any(Boolean.class));

		mockMvc.perform(delete("/admin/posts")
				.contentType(MediaType.APPLICATION_JSON)
				.content(objectMapper.writeValueAsString(bulkPostRequest)))
			.andDo(print())
			.andExpect(status().isOk())
			.andDo(document("admin-post-delete",
				preprocessRequest(prettyPrint()),
				preprocessResponse(prettyPrint()),
				requestFields(
					fieldWithPath("postIds").type(JsonFieldType.ARRAY).description("삭제할 게시글 ID 목록")
				),
				responseFields(
					fieldWithPath("message").type(JsonFieldType.STRING).description("응답 메시지")
				)));
	}

	@DisplayName("게시글 영구 삭제")
	@Test
	void deleteBulkPostPermanent() throws Exception {
		BulkPostRequest bulkPostRequest = new BulkPostRequest();
		bulkPostRequest.setPostIds(List.of(1L, 2L, 3L));

		doNothing().when(adminPostAppService).deleteBulkPost(any(), any(Boolean.class));

		mockMvc.perform(delete("/admin/posts/permanent")
				.contentType(MediaType.APPLICATION_JSON)
				.content(objectMapper.writeValueAsString(bulkPostRequest)))
			.andDo(print())
			.andExpect(status().isOk())
			.andDo(document("admin-post-delete-permanent",
				preprocessRequest(prettyPrint()),
				preprocessResponse(prettyPrint()),
				requestFields(
					fieldWithPath("postIds").type(JsonFieldType.ARRAY).description("영구 삭제할 게시글 ID 목록")
				),
				responseFields(
					fieldWithPath("message").type(JsonFieldType.STRING).description("응답 메시지")
				)));
	}

	@DisplayName("휴지통 게시글 조회")
	@Test
	void retrieveDeletedPostList() throws Exception {
		PageImpl<PostResponse.DeletedPostResponse> deletedPostPage = new PageImpl<>(List.of());

		when(adminPostAppService.findDeletedPostList(any(Pageable.class)))
			.thenReturn(deletedPostPage);

		mockMvc.perform(get("/admin/posts/deleted")
				.param("page", "0")
				.param("perPage", "25")
				.contentType(MediaType.APPLICATION_JSON))
			.andDo(print())
			.andExpect(status().isOk())
			.andDo(document("admin-post-retrieve-deleted",
				preprocessRequest(prettyPrint()),
				preprocessResponse(prettyPrint()),
				requestParameters(
					parameterWithName("page").description("페이지 번호"),
					parameterWithName("perPage").description("한 페이지 당 항목 수")
				),
				responseFields(
					fieldWithPath("content").type(JsonFieldType.ARRAY).description("삭제된 게시글 목록"),
					fieldWithPath("pageable").type(JsonFieldType.STRING).description("페이징 정보 (String 타입, 값은 'INSTANCE')"),
					fieldWithPath("totalPages").type(JsonFieldType.NUMBER).description("총 페이지 수"),
					fieldWithPath("totalElements").type(JsonFieldType.NUMBER).description("총 요소 수"),
					fieldWithPath("last").type(JsonFieldType.BOOLEAN).description("마지막 페이지 여부"),
					fieldWithPath("number").type(JsonFieldType.NUMBER).description("현재 페이지 번호"),
					fieldWithPath("size").type(JsonFieldType.NUMBER).description("페이지 크기"),
					fieldWithPath("numberOfElements").type(JsonFieldType.NUMBER).description("현재 페이지의 요소 수"),
					fieldWithPath("first").type(JsonFieldType.BOOLEAN).description("첫 페이지 여부"),
					fieldWithPath("empty").type(JsonFieldType.BOOLEAN).description("페이지가 비어있는지 여부"),
					fieldWithPath("sort.empty").type(JsonFieldType.BOOLEAN).description("정렬 정보가 비어있는지 여부"),
					fieldWithPath("sort.unsorted").type(JsonFieldType.BOOLEAN).description("정렬되지 않았는지 여부"),
					fieldWithPath("sort.sorted").type(JsonFieldType.BOOLEAN).description("정렬 여부")
				)));
	}

	@DisplayName("게시글 카테고리 이동")
	@Test
	void migratePost() throws Exception {
		PostRequest.MigratePostRequest migratePostRequest = new PostRequest.MigratePostRequest();
		migratePostRequest.setFromCategoryId(1L);
		migratePostRequest.setToCategoryId(2L);

		doNothing().when(adminPostAppService).migratePost(anyLong(), anyLong());

		mockMvc.perform(post("/admin/posts/migrate")
				.contentType(MediaType.APPLICATION_JSON)
				.content(objectMapper.writeValueAsString(migratePostRequest)))
			.andDo(print())
			.andExpect(status().isOk())
			.andDo(document("admin-post-migrate",
				preprocessRequest(prettyPrint()),
				preprocessResponse(prettyPrint()),
				requestFields(
					fieldWithPath("fromCategoryId").type(JsonFieldType.NUMBER).description("이동할 게시글의 기존 카테고리 ID"),
					fieldWithPath("toCategoryId").type(JsonFieldType.NUMBER).description("이동할 게시글의 새 카테고리 ID")
				),
				responseFields(
					fieldWithPath("message").type(JsonFieldType.STRING).description("응답 메시지")
				)));
	}
}