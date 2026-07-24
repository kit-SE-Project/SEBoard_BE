package com.seproject.board.comment.application;

import com.seproject.account.account.domain.Account;
import com.seproject.account.role.domain.Role;
import com.seproject.account.utils.SecurityUtils;
import com.seproject.admin.banned.domain.repository.SpamWordRepository;
import com.seproject.board.comment.application.dto.ReplyCommand;
import com.seproject.file.domain.model.AttachableType;
import com.seproject.file.domain.model.FileMetaData;
import com.seproject.file.domain.repository.FileMetaDataRepository;
import com.seproject.file.domain.repository.FileRepository;
import com.seproject.board.comment.domain.model.Comment;
import com.seproject.board.comment.domain.model.Reply;
import com.seproject.board.comment.service.CommentService;
import com.seproject.board.comment.service.ReplyService;
import com.seproject.board.menu.domain.model.Category;
import com.seproject.board.menu.domain.model.Menu;
import com.seproject.board.post.domain.model.Post;
import com.seproject.board.post.service.PostService;
import com.seproject.error.errorCode.ErrorCode;
import com.seproject.error.exception.CustomAccessDeniedException;
import com.seproject.error.exception.CustomAuthenticationException;
import com.seproject.error.exception.CustomIllegalArgumentException;
import com.seproject.error.exception.InvalidAuthorizationException;
import com.seproject.member.domain.BoardUser;
import com.seproject.member.service.AnonymousService;
import com.seproject.member.service.MemberService;
import com.seproject.notification.NotificationEventDto;
import com.seproject.notification.NotificationEventPublisher;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

@Slf4j
@RequiredArgsConstructor
@Transactional(readOnly = true)
@Service
public class ReplyAppService {

    private final ReplyService replyService;
    private final CommentService commentService;
    private final PostService postService;
    private final MemberService memberService;

    private final AnonymousService anonymousService;
    private final FileMetaDataRepository fileMetaDataRepository;
    private final FileRepository fileRepository;

    private final SpamWordRepository spamWordRepository;
    private final NotificationEventPublisher notificationEventPublisher;

    private static final int MAX_COMMENT_ATTACHMENTS = 5;

    @Transactional
    public Long writeReply(ReplyCommand.ReplyWriteCommand command){
        Account account = SecurityUtils.getAccount()
                .orElseThrow(() -> new CustomAuthenticationException(ErrorCode.NOT_LOGIN,null));
        List<Role> userRoles = account.getRoles();
        Long postId = command.getPostId();
        Long superCommentId = command.getSuperCommentId();
        Long tagCommentId = command.getTagCommentId();
        String contents = command.getContents();

        boolean onlyReadByAuthor = command.isOnlyReadByAuthor();

        Post post = postService.findById(postId);
        Comment superComment = commentService.findById(superCommentId);
        Comment taggedComment = commentService.findById(tagCommentId);

        Category category = post.getCategory();
        Menu superMenu = category.getSuperMenu();

        while (superMenu != null) {
            boolean accessible = superMenu.accessible(userRoles);

            if (!accessible) {
                throw new CustomAccessDeniedException(ErrorCode.ACCESS_DENIED,null);
            }

            superMenu = superMenu.getSuperMenu();
        }

        List<Comment> comments = commentService.findWithAuthorByPostId(postId);
        BoardUser author = command.isAnonymous() ?
                anonymousService.createAnonymousInPost(account, post,comments) : memberService.findByAccountId(account.getAccountId());

        checkSpamWord(contents);

        List<FileMetaData> attachments = fileMetaDataRepository.findAllById(command.getAttachmentIds());
        if (attachments.size() > MAX_COMMENT_ATTACHMENTS) {
            throw new CustomIllegalArgumentException(ErrorCode.INVALID_FILE_SIZE, null);
        }

        Long replyId = replyService.createReply(superComment, contents, taggedComment, author, onlyReadByAuthor);
        attachments.forEach(f -> f.attachTo(AttachableType.COMMENT, replyId));

        publishReplyNotification(superComment, post, account);

        return replyId;
    }

    private void publishReplyNotification(Comment superComment, Post post, Account replier) {
        try {
            if (superComment.getAuthor().isAnonymous()) return;
            Long commentAuthorId = superComment.getAuthor().getAccount().getAccountId();
            if (commentAuthorId.equals(replier.getAccountId())) return;

            notificationEventPublisher.publish(NotificationEventDto.builder()
                .type("REPLY")
                .receiverId(commentAuthorId)
                .actorName(replier.getName())
                .relatedId(post.getPostId())
                .title(post.getTitle())
                .content(replier.getName() + "님이 대댓글을 달았습니다.")
                .build());
        } catch (Exception e) {
            log.warn("대댓글 알림 발행 실패", e);
        }
    }

    private void checkSpamWord(String contents) {
        List<String> spamWords = spamWordRepository.findAll().stream()
                .map(spamWord -> spamWord.getWord().toLowerCase())
                .collect(Collectors.toList());

        for (String spamWord : spamWords) {
            if (contents.toLowerCase().contains(spamWord)) {
                throw new CustomIllegalArgumentException(ErrorCode.CONTAIN_SPAM_KEYWORD, null);
            }
        }
    }

    @Transactional
    public Long editReply(ReplyCommand.ReplyEditCommand command) {
        Account account = SecurityUtils.getAccount()
                .orElseThrow(() -> new CustomAuthenticationException(ErrorCode.NOT_LOGIN,null));

        Reply reply = replyService.findWithPostAndCategory(command.getReplyId());

        if (reply.isWrittenBy(account.getAccountId())  || reply.getPost().getCategory().manageable(account.getRoles())) {

            checkSpamWord(command.getContents());

            reply.changeContents(command.getContents());
            reply.changeOnlyReadByAuthor(command.isOnlyReadByAuthor());

            List<FileMetaData> newAttachments = fileMetaDataRepository.findAllById(command.getAttachmentIds());
            if (newAttachments.size() > MAX_COMMENT_ATTACHMENTS) {
                throw new CustomIllegalArgumentException(ErrorCode.INVALID_FILE_SIZE, null);
            }

            List<FileMetaData> existing = fileMetaDataRepository.findByAttachableTypeAndAttachableId(
                    AttachableType.COMMENT, reply.getCommentId());
            Set<Long> newIds = newAttachments.stream()
                    .map(FileMetaData::getFileMetaDataId).collect(Collectors.toSet());
            existing.stream()
                    .filter(f -> !newIds.contains(f.getFileMetaDataId()))
                    .forEach(f -> {
                        fileRepository.delete(f.getFilePath());
                        fileMetaDataRepository.delete(f);
                    });
            newAttachments.forEach(f -> f.attachTo(AttachableType.COMMENT, reply.getCommentId()));

            return reply.getCommentId();
        }

        throw new InvalidAuthorizationException(ErrorCode.ACCESS_DENIED);
    }

    @Transactional
    public void removeReply(Long replyId) {
        Account account = SecurityUtils.getAccount()
                .orElseThrow(() -> new CustomAuthenticationException(ErrorCode.NOT_LOGIN,null));

        Reply reply = replyService.findWithPostAndCategory(replyId);

        if (reply.isWrittenBy(account.getAccountId()) || reply.getPost().getCategory().manageable(account.getRoles())) {
            reply.delete(true);
            return;
        }

        throw new InvalidAuthorizationException(ErrorCode.ACCESS_DENIED);
    }
}
