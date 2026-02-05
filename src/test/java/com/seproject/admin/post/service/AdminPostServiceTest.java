package com.seproject.admin.post.service;

import com.seproject.board.common.Status;
import com.seproject.board.common.domain.repository.ReportRepository;
import com.seproject.board.menu.domain.Category;
import com.seproject.board.post.domain.model.Post;
import com.seproject.board.post.domain.repository.PostRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.Arrays;
import java.util.List;

import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
public class AdminPostServiceTest {

	@Mock
	private PostRepository postRepository;

	@Mock
	private ReportRepository reportRepository;

	@InjectMocks
	private AdminPostService adminPostService;

	@Test
	public void 게시글_여러개_삭제() {
		List<Long> postIds = Arrays.asList(1L, 2L, 3L);

		adminPostService.deleteAllByIds(postIds, true);

		verify(postRepository, times(1)).deleteAllByIds(postIds, Status.PERMANENT_DELETED);
	}

	@Test
	public void 게시글_딘건_복구() {
		Post post = mock((Post.class));

		when(post.getPostId()).thenReturn(1L);
		adminPostService.restore(post);

		verify(post, times(1)).restore();
		verify(reportRepository, times(1)).deleteAllByPostId(1L);
	}

	@Test
	public void 게시글_다건_복구() {
		List<Long> postIds = Arrays.asList(1L, 2L, 3L);

		adminPostService.restore(postIds);

		verify(postRepository, times(1)).restorePostByPostIds(postIds);
		verify(reportRepository, times(1)).deleteAllByPostIds(postIds);
	}

	@Test
	public void 카테고리_변경() {
		Category from = mock(Category.class);
		Category to = mock(Category.class);

		adminPostService.changeCategory(from, to);

		verify(postRepository, times(1)).changeCategory(from, to);
	}
}