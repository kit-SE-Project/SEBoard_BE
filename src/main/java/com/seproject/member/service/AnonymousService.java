package com.seproject.member.service;

import com.seproject.account.account.domain.Account;
import com.seproject.board.comment.domain.model.Comment;
import com.seproject.board.post.domain.model.Post;
import com.seproject.error.errorCode.ErrorCode;
import com.seproject.error.exception.CustomIllegalArgumentException;
import com.seproject.member.domain.Anonymous;
import com.seproject.member.domain.BoardUser;
import com.seproject.member.domain.repository.AnonymousRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@RequiredArgsConstructor
@Transactional(readOnly = true)
@Service
public class AnonymousService {

    private final AnonymousRepository anonymousRepository;


    public Anonymous findById(Long id) {
        Anonymous anonymous = anonymousRepository.findById(id)
                .orElseThrow(() -> new CustomIllegalArgumentException(ErrorCode.NOT_EXIST_ANONYMOUS, null));
        return anonymous;
    }


    @Transactional
    public Long createAnonymous(String name, Account account) {
        Anonymous anonymous = Anonymous.builder()
                .name(name)
                .account(account)
                .build();
        anonymousRepository.save(anonymous);
        return anonymous.getBoardUserId();
    }

    @Transactional
    public Anonymous createAnonymousInPost(Account account, Post post, List<Comment> comments) {

        Long accountId = account.getAccountId();
        BoardUser postAuthor = post.getAuthor();

        if(post.isWrittenBy(accountId) && postAuthor.isAnonymous()){
            return (Anonymous) postAuthor;
        }

        for (Comment comment : comments) {
            BoardUser author = comment.getAuthor();
            if (author.isOwnAccountId(accountId) && author.isAnonymous()) {
                return (Anonymous) author;
            }
        }

        Anonymous createdAnonymous = post.createAnonymous(account);
        anonymousRepository.save(createdAnonymous);

        return createdAnonymous;
    }
}
