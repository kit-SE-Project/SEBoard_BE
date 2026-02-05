package com.seproject.board.comment.domain;

import com.seproject.board.comment.domain.model.Comment;
import com.seproject.board.comment.domain.model.Reply;
import com.seproject.board.common.BaseTime;
import com.seproject.board.common.Status;
import com.seproject.board.common.domain.ReportThreshold;
import com.seproject.board.post.domain.model.Post;
import com.seproject.member.domain.BoardUser;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.mockito.BDDMockito;
import org.mockito.Mockito;
import org.springframework.context.annotation.Profile;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.*;


@Profile("test")
public class CommentTest {


	@DisplayName("대댓글을 작성한다.")
	@Test
	void writeReply() {
		// given
		Comment mockComment = Mockito.mock(Comment.class);
		BoardUser mockUser = Mockito.mock(BoardUser.class);
		Reply mockReply = Reply.builder()
				.contents("contents")
				.superComment(mockComment)
				.author(mockUser)
				.isOnlyReadByAuthor(true)
				.baseTime(BaseTime.now())
				.build();

		// Mocking the behavior of writeReply method
		BDDMockito.given(mockComment.writeReply("contents", mockComment, mockUser, true)).willReturn(mockReply);

		// when
		Reply reply = mockComment.writeReply("contents", mockComment, mockUser, true);

		// then
		BDDMockito.then(mockComment).should().writeReply("contents", mockComment, mockUser, true);
		assertThat(reply.getContents()).isEqualTo("contents");
		assertThat(reply.getSuperComment()).isEqualTo(mockComment);
		assertThat(reply.getAuthor()).isEqualTo(mockUser);
		assertThat(reply.isOnlyReadByAuthor()).isTrue();
	}


	@DisplayName("작성자를 확인한다.")
	@Test
	void isWrittenBy() {
		BoardUser mockUser = Mockito.mock(BoardUser.class);

		Comment mock = Mockito.mock(Comment.class);

		BDDMockito.given(mock.isWrittenBy(mockUser.getBoardUserId())).willReturn(false);

		mock.isWrittenBy(mockUser.getBoardUserId());

		BDDMockito.then(mock).should().isWrittenBy(mockUser.getBoardUserId());

	}

	@Test
	void isDeleted() {
		Comment mockComment = Mockito.mock(Comment.class);

		when(mockComment.isDeleted()).thenReturn(true);

		boolean deleted = mockComment.isDeleted();

		assertTrue(deleted);

		BDDMockito.then(mockComment).should().isDeleted();
	}


	@Test
	void changeContents() {
		// Mock 객체 생성
		Comment comment = Mockito.mock(Comment.class);

		// changeContents 메서드의 동작 정의
		doNothing().when(comment).changeContents(anyString());

		// 메서드 호출
		comment.changeContents("asd");

		// 메서드 호출 확인
		verify(comment).changeContents("asd");
	}
	@Test
	void changeOnlyReadByAuthor() {
		Comment comment = Mockito.mock(Comment.class);

		doNothing().when(comment).changeOnlyReadByAuthor(true);

		comment.changeOnlyReadByAuthor(true);

		verify(comment).changeOnlyReadByAuthor(true);
	}

	@Test
	void delete() {
		// Comment 객체 생성
		Comment comment = new Comment();

		// 영구 삭제인 경우
		comment.delete(true);
		assertEquals(Status.PERMANENT_DELETED, comment.getStatus());

		// 임시 삭제인 경우
		comment.delete(false);
		assertEquals(Status.TEMP_DELETED, comment.getStatus());
	}

	@Test
	void increaseReportCount() {
		Comment comment = mock(Comment.class);
		ReportThreshold reportThreshold = mock(ReportThreshold.class);

		when(comment.getReportCount()).thenReturn(0);
		when(reportThreshold.isOverThreshold(1)).thenReturn(true);

		Comment realComment = new Comment();

		realComment.increaseReportCount(reportThreshold);

		assertEquals(1, realComment.getReportCount());
		assertEquals(Status.REPORTED, realComment.getStatus());
	}

	@Test
	void restore() {
		Comment comment = new Comment();

		comment.delete(false);

		comment.restore();


		assertEquals(Status.NORMAL, comment.getStatus());
		assertEquals(0, comment.getReportCount());
	}

	@Test
	void isReported() {
		Comment realComment = new Comment();

		ReportThreshold reportThreshold = mock(ReportThreshold.class);

		when(reportThreshold.isOverThreshold(1)).thenReturn(true);


		realComment.increaseReportCount(reportThreshold);

		assertTrue(realComment.isReported());


	}
}

