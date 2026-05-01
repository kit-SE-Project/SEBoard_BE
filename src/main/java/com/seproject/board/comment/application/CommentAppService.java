package com.seproject.board.comment.application;

import com.seproject.account.account.domain.Account;
import com.seproject.account.role.domain.Role;
import com.seproject.account.utils.SecurityUtils;
import com.seproject.admin.banned.domain.repository.SpamWordRepository;
import com.seproject.board.comment.application.dto.CommentCommand.CommentEditCommand;
import com.seproject.board.comment.application.dto.CommentCommand.CommentListFindCommand;
import com.seproject.board.comment.application.dto.CommentCommand.CommentWriteCommand;
import com.seproject.board.comment.controller.dto.CommentResponse.BestCommentResponse;
import com.seproject.board.comment.controller.dto.CommentResponse.CommentListElement;
import com.seproject.board.comment.controller.dto.CommentResponse.CommentListResponse;
import com.seproject.board.comment.controller.dto.PaginationResponse;
import com.seproject.board.comment.controller.dto.ReplyResponse;
import com.seproject.board.comment.domain.model.Comment;
import com.seproject.board.comment.domain.model.Reply;
import com.seproject.board.comment.domain.repository.CommentSearchRepository;
import com.seproject.board.comment.persistence.CommentQueryRepository;
import com.seproject.board.comment.service.CommentLikeService;
import com.seproject.board.comment.service.CommentService;
import com.seproject.board.post.domain.model.LikeType;
import com.seproject.file.controller.dto.FileMetaDataResponse.FileMetaDataElement;
import com.seproject.file.domain.model.AttachableType;
import com.seproject.file.domain.model.FileMetaData;
import com.seproject.file.domain.repository.FileMetaDataRepository;
import com.seproject.file.domain.repository.FileRepository;
import com.seproject.board.menu.domain.model.Category;
import com.seproject.board.menu.domain.model.Menu;
import com.seproject.board.post.domain.model.Post;
import com.seproject.board.post.domain.model.exposeOptions.ExposeOption;
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
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;
import java.util.stream.Stream;

@Service
@RequiredArgsConstructor
@Slf4j
@Transactional(readOnly = true)
public class CommentAppService {
    private final CommentSearchRepository commentSearchRepository;
    private final AnonymousService anonymousService;
    private final CommentService commentService;
    private final PostService postService;
    private final MemberService memberService;
    private final CommentLikeService commentLikeService;
    private final CommentQueryRepository commentQueryRepository;
    private final FileMetaDataRepository fileMetaDataRepository;
    private final FileRepository fileRepository;

    private final SpamWordRepository spamWordRepository;
    private final NotificationEventPublisher notificationEventPublisher;

    private static final int MAX_COMMENT_ATTACHMENTS = 5;

    @Transactional
    public Long writeComment(CommentWriteCommand command){
        Account account = SecurityUtils.getAccount()
                .orElseThrow(() -> new CustomAuthenticationException(ErrorCode.NOT_LOGIN,null));
        List<Role> userRole = account.getRoles();
        Long postId = command.getPostId();
        String contents = command.getContents();
        boolean onlyReadByAuthor = command.isOnlyReadByAuthor();

        Post post = postService.findById(postId);

        Category category = post.getCategory();
        Menu superMenu = category.getSuperMenu();

        while (superMenu != null) {
            boolean accessible = superMenu.accessible(userRole);

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

        Long commentId = commentService.createComment(post, author, contents, onlyReadByAuthor);
        attachments.forEach(f -> f.attachTo(AttachableType.COMMENT, commentId));

        publishCommentNotification(post, account);

        return commentId;
    }

    private void publishCommentNotification(Post post, Account commenter) {
        try {
            if (post.getAuthor().isAnonymous()) return;
            Long postAuthorId = post.getAuthor().getAccount().getAccountId();
            if (postAuthorId.equals(commenter.getAccountId())) return;

            notificationEventPublisher.publish(NotificationEventDto.builder()
                .type("COMMENT")
                .receiverId(postAuthorId)
                .actorName(commenter.getName())
                .relatedId(post.getPostId())
                .title(post.getTitle())
                .content(commenter.getName() + "님이 댓글을 달았습니다.")
                .build());
        } catch (Exception e) {
            log.warn("댓글 알림 발행 실패", e);
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
    public Long editComment(CommentEditCommand command) {

        Long commentId = command.getCommentId();
        Account account = SecurityUtils.getAccount()
                .orElseThrow(() -> new CustomAuthenticationException(ErrorCode.NOT_LOGIN,null));
        Comment comment = commentService.findWithPostAndCategory(commentId);

        Post post = comment.getPost();
        Category category = post.getCategory();

        Long accountId = account.getAccountId();
        List<Role> roles = account.getRoles();

        if (comment.isWrittenBy(accountId) || category.manageable(roles) || category.editable(roles)) {
            checkSpamWord(command.getContents());

            comment.changeContents(command.getContents());
            comment.changeOnlyReadByAuthor(command.isOnlyReadByAuthor());

            List<FileMetaData> newAttachments = fileMetaDataRepository.findAllById(command.getAttachmentIds());
            if (newAttachments.size() > MAX_COMMENT_ATTACHMENTS) {
                throw new CustomIllegalArgumentException(ErrorCode.INVALID_FILE_SIZE, null);
            }

            List<FileMetaData> existing = fileMetaDataRepository.findByAttachableTypeAndAttachableId(
                    AttachableType.COMMENT, comment.getCommentId());
            Set<Long> newIds = newAttachments.stream()
                    .map(FileMetaData::getFileMetaDataId).collect(Collectors.toSet());
            existing.stream()
                    .filter(f -> !newIds.contains(f.getFileMetaDataId()))
                    .forEach(f -> {
                        fileRepository.delete(f.getFilePath());
                        fileMetaDataRepository.delete(f);
                    });
            newAttachments.forEach(f -> f.attachTo(AttachableType.COMMENT, comment.getCommentId()));

            return comment.getCommentId();
        }

        throw new InvalidAuthorizationException(ErrorCode.ACCESS_DENIED);
    }

    @Transactional
    public void removeComment(Long commentId) {
        Account account = SecurityUtils.getAccount()
                .orElseThrow(() -> new CustomAuthenticationException(ErrorCode.NOT_LOGIN,null));

        Comment comment = commentService.findWithPostAndCategory(commentId);
        Post post = comment.getPost();
        Category category = post.getCategory();

        if (comment.isWrittenBy(account.getAccountId()) || category.manageable(account.getRoles())) {
            comment.delete(true);
            return;
        }

        throw new InvalidAuthorizationException(ErrorCode.ACCESS_DENIED);
    }
    public CommentListResponse retrieveCommentList(CommentListFindCommand command) {
        Long postId = command.getPostId();
        Post post = postService.findById(postId);
        String password = command.getPassword();
        Account account = SecurityUtils.getAccount().orElse(null);

        List<Role> userRoles = account == null ? null : account.getRoles();

        Category category = post.getCategory();
        boolean accessible = category.accessible(userRoles);

        if (!accessible) {
            throw new CustomAccessDeniedException(ErrorCode.ACCESS_DENIED,null);
        }

        ExposeOption exposeOption = post.getExposeOption();
        boolean pass = exposeOption.pass(userRoles, password);

        if(account!=null){
            if(post.isWrittenBy(account.getAccountId())){
                pass = true;
            }

            if(category.manageable(userRoles)){
                pass = true;
            }
        }

        if (!pass) {
            throw new InvalidAuthorizationException(ErrorCode.INCORRECT_POST_PASSWORD);
        }

        int page = command.getPage();
        int perPage = command.getPerPage();

        Page<Comment> commentPage = commentSearchRepository.findCommentListByPostId(postId, PageRequest.of(page, perPage));
        long totalReplySize = commentSearchRepository.countReplyByPostId(postId);
        Long accountId = account == null ? null : account.getAccountId();
        Long memberId = account == null ? null : memberService.findByAccountId(accountId).getBoardUserId();

        List<CommentListElement> commentDtoList = commentPage.getContent().stream()
                .map(comment -> {
                    List<ReplyResponse> subComments = commentSearchRepository.findReplyListByCommentId(comment.getCommentId())
                            .stream()
                            .map(reply -> {
                                int replyLikeCount = commentLikeService.countLikes(reply.getCommentId());
                                int replyDislikeCount = commentLikeService.countDislikes(reply.getCommentId());
                                String replyMyReaction = memberId == null ? null :
                                        commentLikeService.getMyReaction(reply.getCommentId(), memberId)
                                                .map(LikeType::name).orElse(null);
                                List<FileMetaDataElement> replyAttachments = fileMetaDataRepository
                                        .findByAttachableTypeAndAttachableId(AttachableType.COMMENT, reply.getCommentId())
                                        .stream().map(FileMetaDataElement::new).collect(Collectors.toList());
                                return ReplyResponse.toDto(reply,
                                        accountId != null && (reply.isWrittenBy(accountId)
                                                || userRoles != null && post.getCategory().manageable(userRoles)),
                                        accountId != null && reply.getPost().isWrittenBy(accountId),
                                        replyLikeCount, replyDislikeCount, replyMyReaction, replyAttachments);
                            }).collect(Collectors.toList());

                    int likeCount = commentLikeService.countLikes(comment.getCommentId());
                    int dislikeCount = commentLikeService.countDislikes(comment.getCommentId());
                    String myReaction = memberId == null ? null :
                            commentLikeService.getMyReaction(comment.getCommentId(), memberId)
                                    .map(LikeType::name).orElse(null);
                    List<FileMetaDataElement> commentAttachments = fileMetaDataRepository
                            .findByAttachableTypeAndAttachableId(AttachableType.COMMENT, comment.getCommentId())
                            .stream().map(FileMetaDataElement::new).collect(Collectors.toList());

                    return CommentListElement.toDto(
                            comment,
                            accountId != null && comment.isWrittenBy(accountId)
                                    || userRoles != null && post.getCategory().manageable(userRoles),
                            accountId != null && comment.getPost().isWrittenBy(accountId),
                            subComments, likeCount, dislikeCount, myReaction, commentAttachments);
                }).collect(Collectors.toList());


        // 작성자 프로필 이미지 일괄 조회
        List<Long> authorIds = commentDtoList.stream()
                .flatMap(c -> Stream.concat(
                        c.getAuthor().getUserId() != null ? Stream.of(c.getAuthor().getUserId()) : Stream.empty(),
                        c.getSubComments().stream()
                                .filter(r -> r.getAuthor().getUserId() != null)
                                .map(r -> r.getAuthor().getUserId())
                )).distinct().collect(Collectors.toList());

        if (!authorIds.isEmpty()) {
            Map<Long, String> profileImageMap = fileMetaDataRepository
                    .findByAttachableTypeAndAttachableIdIn(AttachableType.PROFILE, authorIds)
                    .stream()
                    .collect(Collectors.toMap(FileMetaData::getAttachableId, FileMetaData::getUrlPath, (a, b) -> a));

            commentDtoList.forEach(c -> {
                if (c.getAuthor().getUserId() != null) {
                    c.getAuthor().setProfileImageUrl(profileImageMap.get(c.getAuthor().getUserId()));
                }
                c.getSubComments().forEach(r -> {
                    if (r.getAuthor().getUserId() != null) {
                        r.getAuthor().setProfileImageUrl(profileImageMap.get(r.getAuthor().getUserId()));
                    }
                });
            });
        }

        PaginationResponse paginationResponse = PaginationResponse.builder()
                .totalCommentSize(commentPage.getTotalElements())
                .last(commentPage.isLast())
                .pageNum(commentPage.getNumber())
                .totalAllSize(commentPage.getTotalElements() + totalReplySize)
                .build();

        return CommentListResponse.toDto(commentDtoList, paginationResponse);
    }

    public List<BestCommentResponse> retrieveBestComments(Long postId, int limit) {
        List<Comment> bestComments = commentQueryRepository.findBestCommentsByPostId(postId, limit);

        Long memberId = null;
        Optional<Account> accountOpt = SecurityUtils.getAccount();
        if (accountOpt.isPresent()) {
            memberId = memberService.findByAccountId(accountOpt.get().getAccountId()).getBoardUserId();
        }
        final Long finalMemberId = memberId;

        return bestComments.stream().map(comment -> {
            int likeCount = commentLikeService.countLikes(comment.getCommentId());
            String myReaction = finalMemberId == null ? null :
                    commentLikeService.getMyReaction(comment.getCommentId(), finalMemberId)
                            .map(LikeType::name).orElse(null);

            boolean isReply = comment instanceof Reply;
            long rootCommentsBefore;
            if (isReply) {
                rootCommentsBefore = commentSearchRepository.countRootCommentsBefore(
                        postId, ((Reply) comment).getSuperComment().getBaseTime().getCreatedAt());
            } else {
                rootCommentsBefore = commentSearchRepository.countRootCommentsBefore(
                        postId, comment.getBaseTime().getCreatedAt());
            }
            int pageNumber = (int) (rootCommentsBefore / 25);

            return BestCommentResponse.toDto(comment, likeCount, myReaction, isReply, pageNumber);
        }).collect(Collectors.toList());
    }








}
