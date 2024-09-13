package com.seproject.admin.post.controller;

import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

import java.util.Collections;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

import com.seproject.admin.post.application.AdminPostAppService;
import com.seproject.admin.post.application.PostSyncService;
import com.seproject.admin.post.controller.dto.AdminOldPost;
import com.seproject.admin.post.controller.dto.PostRequest.MigratePostRequest;
import com.seproject.admin.post.controller.dto.PostResponse;
import com.seproject.admin.post.controller.dto.PostResponse.DeletedPostResponse;
import com.seproject.board.common.controller.dto.MessageResponse;

@ExtendWith(MockitoExtension.class)
class AdminPostControllerTest {

	@Mock
	private AdminPostAppService adminPostAppService;

	@Mock
	private PostSyncService postSyncService;

	@InjectMocks
	private AdminPostController adminPostController;

	private MockMvc mockMvc;

	@BeforeEach
	void setUp() {
		mockMvc = MockMvcBuilders.standaloneSetup(adminPostController).build();
	}

	@Test
	@DisplayName("Create Old Posts - Success")
	void createOldSePosts() throws Exception {
		AdminOldPost request = new AdminOldPost();
		request.setTitle("Old Post Title");

		when(postSyncService.importOldPost(any(AdminOldPost.class))).thenReturn("Success");

		mockMvc.perform(post("/admin/posts/old")
				.contentType(MediaType.APPLICATION_JSON)
				.content("{\"title\":\"Old Post Title\"}"))
			.andExpect(status().isCreated())
			.andExpect(content().string("Success"));
	}

	@Test
	@DisplayName("Retrieve All Posts - Success")
	void retrieveAllPost() throws Exception {
		Page<PostResponse.PostRetrieveResponse> pageResponse = new PageImpl<>(Collections.emptyList());
		when(adminPostAppService.findPostList(any(), anyInt(), anyInt())).thenReturn(pageResponse);

		mockMvc.perform(get("/admin/posts")
				.param("page", "0")
				.param("perPage", "25")
				.contentType(MediaType.APPLICATION_JSON))
			.andExpect(status().isOk())
			.andExpect(jsonPath("$.content").isEmpty());
	}

	@Test
	@DisplayName("Restore Post - Success")
	void restorePost() throws Exception {
		doNothing().when(adminPostAppService).restorePost(anyLong());

		mockMvc.perform(post("/admin/posts/1/restore"))
			.andExpect(status().isOk())
			.andExpect(jsonPath("$.message").value("게시글 복구 성공"));
	}

	@Test
	@DisplayName("Retrieve Deleted Posts - Success")
	void retrieveDeletedPostList() throws Exception {
		Page<DeletedPostResponse> pageResponse = new PageImpl<>(Collections.emptyList(), PageRequest.of(0, 25), 0);
		when(adminPostAppService.findDeletedPostList(any(PageRequest.class))).thenReturn(pageResponse);

		mockMvc.perform(get("/admin/posts/deleted")
				.param("page", "0")
				.param("perPage", "25")
				.contentType(MediaType.APPLICATION_JSON))
			.andExpect(status().isOk())
			.andExpect(jsonPath("$.content").isEmpty());
	}

	@Test
	@DisplayName("Migrate Posts - Success")
	void migratePost() throws Exception {
		MigratePostRequest request = new MigratePostRequest();
		request.setFromCategoryId(1L);
		request.setToCategoryId(2L);

		doNothing().when(adminPostAppService).migratePost(anyLong(), anyLong());

		mockMvc.perform(post("/admin/posts/migrate")
				.contentType(MediaType.APPLICATION_JSON)
				.content("{\"fromCategoryId\":1,\"toCategoryId\":2}"))
			.andExpect(status().isOk())
			.andExpect(jsonPath("$.message").value("게시글 전체 이동 성공"));
	}
}