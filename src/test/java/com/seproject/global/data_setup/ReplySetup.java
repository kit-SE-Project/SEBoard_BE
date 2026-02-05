package com.seproject.global.data_setup;

import com.seproject.board.comment.domain.model.Comment;
import com.seproject.board.comment.domain.model.Reply;
import com.seproject.board.comment.domain.repository.ReplyRepository;
import com.seproject.board.common.BaseTime;
import com.seproject.board.post.domain.model.Post;
import com.seproject.member.domain.BoardUser;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.util.UUID;

@Component
public class ReplySetup {

    @Autowired ReplyRepository replyRepository;

    public Reply createReply(Post post, BoardUser author, Comment superComment, Comment tagComment) {
        Reply reply = Reply.builder()
                .superComment(superComment)
                .tag(tagComment)
                .contents(UUID.randomUUID().toString())
                .baseTime(BaseTime.now())
                .post(post)
                .author(author)
                .isOnlyReadByAuthor(false)
                .reportCount(0)
                .build();

        replyRepository.save(reply);
        return reply;
    }
}
