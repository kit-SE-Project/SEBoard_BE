package com.seproject.board.post.application;

import com.seproject.account.account.domain.Account;
import com.seproject.account.account.service.AccountService;
import com.seproject.account.role.domain.Role;
import com.seproject.account.utils.SecurityUtils;
import com.seproject.admin.banned.domain.SpamWord;
import com.seproject.admin.banned.domain.repository.SpamWordRepository;
import com.seproject.admin.post.application.PostSyncService;
import com.seproject.board.comment.domain.model.Comment;
import com.seproject.board.comment.domain.repository.CommentRepository;
import com.seproject.board.common.BaseTime;
import com.seproject.board.menu.domain.model.Category;
import com.seproject.board.menu.service.CategoryService;
import com.seproject.board.post.application.dto.PostCommand.PostEditCommand;
import com.seproject.board.post.application.dto.PostCommand.PostWriteCommand;
import com.seproject.board.post.domain.model.Post;
import com.seproject.board.post.domain.model.exposeOptions.ExposeOption;
import com.seproject.board.post.domain.model.exposeOptions.ExposeState;
import com.seproject.board.post.service.PostService;
import com.seproject.error.errorCode.ErrorCode;
import com.seproject.error.exception.*;
import com.seproject.file.domain.model.FileConfiguration;
import com.seproject.file.domain.model.FileMetaData;
import com.seproject.file.domain.repository.FileConfigurationRepository;
import com.seproject.file.domain.repository.FileMetaDataRepository;
import com.seproject.file.domain.repository.FileRepository;
import com.seproject.member.domain.Anonymous;
import com.seproject.member.domain.BoardUser;
import com.seproject.member.service.AnonymousService;
import com.seproject.member.service.MemberService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;


@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class PostAppService {

    private final FileMetaDataRepository fileMetaDataRepository;
    private final FileRepository fileRepository;
    private final FileConfigurationRepository fileConfigurationRepository;
    private final SpamWordRepository spamWordRepository;

    private final MemberService memberService;
    private final AccountService accountService;
    private final AnonymousService anonymousService;
    private final PostService postService;
    private final CategoryService categoryService;
    private final CommentRepository commentRepository;

    private final PostSyncService postSyncAppService;

    @Transactional
    public Long writePost(PostWriteCommand command){
        Account account = SecurityUtils.getAccount()
                .orElseThrow(() -> new NoSuchResourceException(ErrorCode.NOT_LOGIN));
        Category category = categoryService.findById(command.getCategoryId());
        BoardUser author = command.isAnonymous() ?
                createAnonymous(account) : memberService.findByAccountId(account.getAccountId());

        return createPost(command, author, category);
    }

    private BoardUser createAnonymous(Account account) {
        String name = "익명";
        Long anonymousId = anonymousService.createAnonymous(name, account);
        Anonymous anonymous = anonymousService.findById(anonymousId);
        return anonymous;
    }

    private Long createPost(PostWriteCommand command, BoardUser author, Category category){
        List<FileMetaData> fileMetaDataList =
                fileMetaDataRepository.findAllById(command.getAttachmentIds());

        validFileListSize(fileMetaDataList);

        boolean isPined = command.isPined();
        List<Role> roles = author.getAccount().getRoles();
        if(!category.editable(roles)){
            throw new CustomAccessDeniedException(ErrorCode.ACCESS_DENIED,null);
        }

        String title = command.getTitle();
        String contents = command.getContents();
        BaseTime now = BaseTime.now();

        HashSet<FileMetaData> attachments = new HashSet<>(fileMetaDataList);
        ExposeOption exposeOption = ExposeOption.of(command.getExposeState(), command.getPrivatePassword());

        if (command.getExposeState() == ExposeState.KUMOH) {
            boolean match = roles.stream()
                    .anyMatch((role) -> role.getAuthority().equals(Role.ROLE_KUMOH));
            if (!match) {
                throw new CustomIllegalArgumentException(ErrorCode.ACCESS_DENIED,null);
            }
        }

        checkSpamWord(title, contents);

        if(command.isPined() && !category.manageable(roles)){
            throw new CustomAccessDeniedException(ErrorCode.ACCESS_DENIED, null);
        }

        Long postId = postService.createPost(title, contents, category, author, now, isPined, attachments, exposeOption);

        if(command.isSyncOldVersion()){
            postSyncAppService.exportNewPost(category.getSuperMenu().getUrlInfo(), postId, title, contents, author.getName());
        }

        return postId;
    }

    private void checkSpamWord(String title, String contents) {
        List<SpamWord> spamWords = spamWordRepository.findAll();

        for (SpamWord spamWord : spamWords) {
            String word = spamWord.getWord().toLowerCase();
            if (title.toLowerCase().contains(word) || contents.toLowerCase().contains(word)) {
                throw new CustomIllegalArgumentException(ErrorCode.CONTAIN_SPAM_KEYWORD, null);
            }
        }
    }


    @Transactional
    public Long editPost(PostEditCommand command) {
        Account account = SecurityUtils.getAccount()
                .orElseThrow(() -> new CustomAuthenticationException(ErrorCode.NOT_LOGIN,null));

        Post post = postService.findByIdWithCategory(command.getPostId());

        if(!(post.isWrittenBy(account.getAccountId()) || post.getCategory().manageable(account.getRoles()))){
            throw new InvalidAuthorizationException(ErrorCode.ACCESS_DENIED);
        }

        if(command.isPined()!=post.isPined() && !post.getCategory().manageable(account.getRoles())){
            throw new CustomAccessDeniedException(ErrorCode.ACCESS_DENIED, null);
        }

        if(!command.getCategoryId().equals(post.getCategory().getMenuId())){
            Category category = categoryService.findById(command.getCategoryId());
            post.changeCategory(category);
        }

        checkSpamWord(command.getTitle(), command.getContents());

        post.changeTitle(command.getTitle());
        post.changeContents(command.getContents());
        post.changePin(command.isPined());
        post.changeExposeOption(command.getExposeState(), command.getPrivatePassword());

        //TODO : 좀더 깔끔하게 처리?
        List<FileMetaData> attachments =
                fileMetaDataRepository.findAllById(command.getAttachmentIds()); //요청으로 들어온 attachment PK

        Set<FileMetaData> removalAttachments = post.getAttachments();
        removalAttachments.removeAll(attachments); //요청으로 들어온 PK와 기존의 PK를 비교하고, 새로온 PK에 없는 것은 삭제 대상

        //TODO : N+1 문제
        removalAttachments.forEach(fileMetaData -> fileRepository.delete(fileMetaData.getFilePath())); //file 삭제
        removalAttachments.forEach(fileMetaData -> post.removeAttachment(fileMetaData)); //db에서 정보 삭제
        attachments.forEach(fileMetaData -> post.addAttachment(fileMetaData));

        validFileListSize(new ArrayList<>(post.getAttachments()));


        return post.getPostId();
    }

    private void validFileListSize(List<FileMetaData> fileMetaDataList){
        Long maxSize = fileConfigurationRepository.findAll().stream().findFirst()
                .orElseGet(() -> new FileConfiguration(100L, 100L)).getMaxSizePerPost();

        Long totalSize = fileMetaDataList.stream().mapToLong(fileMetaData -> fileMetaData.getFileSize()/(1024*1024)).sum();

        if(maxSize < totalSize) {
            throw new ExceedFileSizeException(ErrorCode.INVALID_FILE_SIZE);
        }
    }

    @Transactional
    public void removePost(Long postId) {
        Account account = SecurityUtils.getAccount()
                .orElseThrow(() -> new CustomAuthenticationException(ErrorCode.NOT_LOGIN,null));

        Post post = postService.findByIdWithCategory(postId);

        if (post.isWrittenBy(account.getAccountId()) || post.getCategory().manageable(account.getRoles())) {
            post.delete(true);

            List<Comment> comments = commentRepository.findByPostId(postId);

            comments.forEach(comment -> comment.delete(true));
            return;
        }

        throw new InvalidAuthorizationException(ErrorCode.ACCESS_DENIED);

//        post.getAttachments().forEach(fileAppService::deleteFileFromStorage); //TODO : fileSystem에서 transactional 처리 필요
//        postRepository.deleteById(postId);
    }
}
