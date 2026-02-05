package com.seproject.global.data_setup;

import com.seproject.board.comment.domain.model.Comment;
import com.seproject.board.comment.domain.repository.CommentRepository;
import com.seproject.board.common.BaseTime;
import com.seproject.board.post.domain.model.Post;
import com.seproject.member.domain.BoardUser;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.util.UUID;

@Component
public class CommentSetup {

    @Autowired
    private CommentRepository commentRepository;

    public Comment createComment(Post post , BoardUser boardUser) {
        Comment comment = Comment.builder()
                .contents(UUID.randomUUID().toString())
                .baseTime(BaseTime.now())
                .post(post)
                .author(boardUser)
                .isOnlyReadByAuthor(false)
                .reportCount(0)
                .build();

        commentRepository.save(comment);
        return comment;
    }
}
