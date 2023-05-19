package com.seproject.admin.service;

import com.seproject.account.model.Account;
import com.seproject.account.utils.SecurityUtils;
import com.seproject.error.errorCode.ErrorCode;
import com.seproject.error.exception.InvalidAuthorizationException;
import com.seproject.error.exception.NoSuchResourceException;
import com.seproject.seboard.domain.model.user.BoardUser;
import com.seproject.seboard.domain.model.post.Post;
import com.seproject.seboard.domain.repository.user.BoardUserRepository;
import com.seproject.seboard.domain.repository.post.PostRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import static com.seproject.seboard.application.utils.AppServiceHelper.findByIdOrThrow;

@Service
@RequiredArgsConstructor
public class AdminPostAppService {

    private final PostRepository postRepository;
    private final BoardUserRepository boardUserRepository;

    public void enrollPin(Long accountId, Long postId) {
        BoardUser requestUser = findByIdOrThrow(accountId, boardUserRepository, "");
        //TODO: 권한처리

        Post post = findByIdOrThrow(postId, postRepository, "");
        post.changePin(true);
    }

    public void cancelPin(Long accountId, Long postId) {
        BoardUser requestUser = findByIdOrThrow(accountId, boardUserRepository, "");
        //TODO: 권한처리

        Post post = findByIdOrThrow(postId, postRepository, "");
        post.changePin(false);
    }

    public void restorePost(Long postId){
        Account account = SecurityUtils.getAccount()
                .orElseThrow(() -> new InvalidAuthorizationException(ErrorCode.NOT_LOGIN));

        boolean isAdmin = account.getAuthorities().stream().anyMatch(authority -> authority.getAuthority().equals("ROLE_ADMIN"));

        if(!isAdmin){
            throw new InvalidAuthorizationException(ErrorCode.ACCESS_DENIED);
        }

        Post post = postRepository.findById(postId)
                .orElseThrow(() -> new NoSuchResourceException(ErrorCode.NOT_EXIST_POST));

        post.restore();
    }
}