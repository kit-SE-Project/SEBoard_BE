package com.seproject.board.post.application;

import com.seproject.account.account.domain.Account;
import com.seproject.account.role.domain.Role;
import com.seproject.account.utils.SecurityUtils;
import com.seproject.board.comment.domain.model.Comment;
import com.seproject.board.menu.domain.model.Category;
import com.seproject.board.post.controller.PostSearchOptions;
import com.seproject.board.post.controller.dto.PostSearchRequest;
import com.seproject.board.post.persistence.PostQueryRepository;
import com.seproject.board.post.service.BookmarkService;
import com.seproject.board.post.service.PostLikeService;
import com.seproject.board.post.service.PostService;
import com.seproject.error.errorCode.ErrorCode;
import com.seproject.file.domain.model.AttachableType;
import com.seproject.file.domain.model.FileMetaData;
import com.seproject.file.domain.repository.FileMetaDataRepository;
import com.seproject.error.exception.*;
import com.seproject.board.post.controller.dto.PostResponse.RetrievePostDetailResponse;
import com.seproject.board.post.controller.dto.PostResponse.RetrievePostListResponseElement;
import com.seproject.board.menu.domain.model.BoardMenu;
import com.seproject.board.post.domain.model.Post;
import com.seproject.board.post.domain.model.exposeOptions.ExposeState;
import com.seproject.member.domain.Member;
import com.seproject.board.menu.domain.repository.BoardMenuRepository;
import com.seproject.board.comment.domain.repository.CommentRepository;
import com.seproject.board.post.domain.repository.PostSearchRepository;
import com.seproject.member.service.MemberService;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;
import java.util.Optional;

@Transactional(readOnly = true)
@RequiredArgsConstructor
@Service
public class PostSearchAppService {
    private final PostSearchRepository postSearchRepository;
    private final CommentRepository commentRepository;
    private final BoardMenuRepository boardMenuRepository;


    private final PostQueryRepository postQueryRepository;
    private final MemberService memberService;
    private final PostService postService;
    private final BookmarkService bookmarkService;
    private final PostLikeService postLikeService;
    private final FileMetaDataRepository fileMetaDataRepository;


    @Transactional // TODO : 조회수 늘리기 다른 방법으로 변경
    public RetrievePostDetailResponse findPostDetail(Long postId){
        Account account = SecurityUtils.getAccount().orElse(null);
        Post post = postQueryRepository.findByIdWithAll(postId)
                .orElseThrow(() -> new NoSuchResourceException(ErrorCode.NOT_EXIST_POST));
        Category category = post.getCategory();
        List<FileMetaData> attachments = fileMetaDataRepository.findByAttachableTypeAndAttachableId(AttachableType.POST, postId);
        RetrievePostDetailResponse postDetailResponse = new RetrievePostDetailResponse(post, attachments);

        // 게시글 작성자 프로필 이미지 세팅
        if (!post.getAuthor().isAnonymous()) {
            fileMetaDataRepository.findByAttachableTypeAndAttachableId(AttachableType.PROFILE, post.getAuthor().getBoardUserId())
                    .stream().findFirst()
                    .ifPresent(f -> postDetailResponse.getAuthor().setProfileImageUrl(f.getUrlPath()));
        }

        boolean isEditable = false;
        boolean isBookmarked = false;
        boolean isAuthor = false;

        if(account != null) {
            Member member = memberService.findByAccountId(account.getAccountId());
            isBookmarked = bookmarkService.isBookmarked(post, member);
            isAuthor = post.isWrittenBy(account.getAccountId());
            isEditable = category.manageable(account.getRoles()) || isAuthor;

            postLikeService.getMyReaction(postId, member.getBoardUserId())
                    .ifPresent(likeType -> postDetailResponse.setMyReaction(likeType.name()));
        }

        postDetailResponse.setEditable(isEditable);
        postDetailResponse.setBookmarked(isBookmarked);
        postDetailResponse.setLikeCount(postLikeService.countLikes(postId));
        postDetailResponse.setDislikeCount(postLikeService.countDislikes(postId));

        ExposeState exposeState = post.getExposeOption().getExposeState();
        if(exposeState == ExposeState.PRIVACY) {
            if (account!=null && (isAuthor || category.manageable(account.getRoles()))) {
                post.increaseViews();
                return postDetailResponse;
            }

            throw new CustomIllegalArgumentException(ErrorCode.NOT_POST_AUTHOR, null);
        } else if(exposeState == ExposeState.KUMOH) {

            if (account == null) {
                throw new CustomAuthenticationException(ErrorCode.NOT_LOGIN,null);
            }

            if (isAuthor || category.manageable(account.getRoles())) {
                post.increaseViews();
                return postDetailResponse;
            }

            boolean isKumoh = account.getRoles().stream()
                    .anyMatch(role -> role.getAuthority().equals(Role.ROLE_KUMOH));

            if (!isKumoh) {
                throw new CustomAccessDeniedException(ErrorCode.ACCESS_DENIED,null);
            }
        }

        post.increaseViews();
        return postDetailResponse;
    }

    @Transactional
    public RetrievePostDetailResponse findPrivacyPostDetail(Long postId, String password){
        Account account = SecurityUtils.getAccount().orElse(null);

        Post post = postQueryRepository.findByIdWithAll(postId)
                .orElseThrow(() -> new NoSuchResourceException(ErrorCode.NOT_EXIST_POST));

        if(post.getExposeOption().getExposeState()!=ExposeState.PRIVACY){
            throw new CustomIllegalArgumentException(ErrorCode.INVALID_POST_TYPE, null);
        }

        if(!post.checkPassword(password)){
            throw new CustomIllegalArgumentException(ErrorCode.INCORRECT_POST_PASSWORD, null);
        }

        Category category = post.getCategory();
        List<FileMetaData> attachments = fileMetaDataRepository.findByAttachableTypeAndAttachableId(AttachableType.POST, postId);
        RetrievePostDetailResponse postDetailResponse = new RetrievePostDetailResponse(post, attachments);

        // 게시글 작성자 프로필 이미지 세팅 (비밀글)
        if (!post.getAuthor().isAnonymous()) {
            fileMetaDataRepository.findByAttachableTypeAndAttachableId(AttachableType.PROFILE, post.getAuthor().getBoardUserId())
                    .stream().findFirst()
                    .ifPresent(f -> postDetailResponse.getAuthor().setProfileImageUrl(f.getUrlPath()));
        }

        boolean isEditable = false;
        boolean isBookmarked = false;
        boolean isAuthor = false;

        if(account != null) {
            Member member = memberService.findByAccountId(account.getAccountId());
            isBookmarked = bookmarkService.isBookmarked(post, member);
            isAuthor = post.isWrittenBy(account.getAccountId());
            isEditable = category.manageable(account.getRoles()) || isAuthor;

            postLikeService.getMyReaction(postId, member.getBoardUserId())
                    .ifPresent(likeType -> postDetailResponse.setMyReaction(likeType.name()));
        }

        postDetailResponse.setEditable(isEditable);
        postDetailResponse.setBookmarked(isBookmarked);
        postDetailResponse.setLikeCount(postLikeService.countLikes(postId));
        postDetailResponse.setDislikeCount(postLikeService.countDislikes(postId));

        post.increaseViews();
        return postDetailResponse;
    }

    public List<RetrievePostListResponseElement> findPinedPostList(Long categoryId) {
        BoardMenu boardMenu = boardMenuRepository.findById(categoryId)
                .orElseThrow(() -> new NoSuchResourceException(ErrorCode.NOT_EXIST_CATEGORY));

        Optional<Account> optional = SecurityUtils.getAccount();
        List<Role> roles = optional.isPresent() ? optional.get().getRoles() : Collections.emptyList();

        List<RetrievePostListResponseElement> postPage;

        if(boardMenu.accessible(roles)) {
            postPage = postSearchRepository.findPinedPostByCategoryId(categoryId);

            postPage.forEach(postDto -> { // TODO : N+1
                int commentSize = commentRepository.countCommentsByPostId(postDto.getPostId());
                postDto.setCommentSize(commentSize);
            });

            return postPage;
        }

        throw new InvalidAuthorizationException(ErrorCode.ACCESS_DENIED);
    }

    public Page<RetrievePostListResponseElement> findPostList(Long categoryId, int page, int size){
        BoardMenu boardMenu = boardMenuRepository.findById(categoryId)
                .orElseThrow(() -> new NoSuchResourceException(ErrorCode.NOT_EXIST_CATEGORY));

        Optional<Account> optional = SecurityUtils.getAccount();
        List<Role> roles = optional.isPresent() ? optional.get().getRoles() : Collections.emptyList();

        if(boardMenu.accessible(roles)) {
            Page<RetrievePostListResponseElement> postPage =
                    postSearchRepository.findPostByCategoryId(categoryId, PageRequest.of(page, size));
            setCommentSize(postPage);

            return postPage;
        }

        throw new InvalidAuthorizationException(ErrorCode.ACCESS_DENIED);
    }

    public Page<RetrievePostListResponseElement> searchPost(PostSearchRequest request, int page, int size){
        try {
            PageRequest pageRequest = PageRequest.of(page, size);
            Long categoryId = request.getCategoryId();
            PostSearchOptions searchOptions = PostSearchOptions.valueOf(request.getSearchOption());
            String query = request.getQuery();

            Page<RetrievePostListResponseElement> res =
                    postQueryRepository.searchPostList(categoryId, searchOptions, query, pageRequest);

            setCommentSize(res);

            return res;
        }catch (IllegalArgumentException e){
            throw new CustomIllegalArgumentException(ErrorCode.INVALID_SEARCH_OPTION, e);
        }
    }

    public List<RetrievePostListResponseElement> findTrendingPosts(Long categoryId, int days, int limit) {
        BoardMenu boardMenu = boardMenuRepository.findById(categoryId)
                .orElseThrow(() -> new NoSuchResourceException(ErrorCode.NOT_EXIST_CATEGORY));

        Optional<Account> optional = SecurityUtils.getAccount();
        List<Role> roles = optional.isPresent() ? optional.get().getRoles() : Collections.emptyList();

        if (!boardMenu.accessible(roles)) {
            throw new InvalidAuthorizationException(ErrorCode.ACCESS_DENIED);
        }

        LocalDateTime since = LocalDateTime.now().minusDays(days);
        List<RetrievePostListResponseElement> trendingPosts =
                postQueryRepository.findTrendingPosts(categoryId, since, limit);

        trendingPosts.forEach(postDto -> {
            int commentSize = commentRepository.countCommentsByPostId(postDto.getPostId());
            postDto.setCommentSize(commentSize);
        });

        return trendingPosts;
    }

    //TODO : 쿼리문으로 처리?
    private void setCommentSize(Page<RetrievePostListResponseElement> postPage){
        postPage.forEach(postDto -> { // TODO : N+1
            List<Comment> comments = commentRepository.findByPostId(postDto.getPostId());
            LocalDateTime resModifiedAt = postDto.getModifiedAt();

            Optional<Comment> recentComment = comments.stream()
                    .filter(comment -> comment.getBaseTime().getModifiedAt().isAfter(resModifiedAt))
                    .max(Comparator.comparing(c -> c.getBaseTime().getModifiedAt()));

            if(recentComment.isPresent() && postDto.getModifiedAt().isBefore(recentComment.get().getBaseTime().getModifiedAt())){
                postDto.setModifiedAt(recentComment.get().getBaseTime().getModifiedAt());
            }

            postDto.setCommentSize(comments.size());
        });
    }

}
