package com.seproject.board.post.application;

import com.seproject.account.account.domain.Account;
import com.seproject.account.account.domain.repository.AccountRepository;
import com.seproject.error.errorCode.ErrorCode;
import com.seproject.error.exception.CustomUserNotFoundException;
import com.seproject.error.exception.NoSuchResourceException;
import com.seproject.board.post.domain.model.Bookmark;
import com.seproject.board.post.domain.model.Post;
import com.seproject.member.domain.Member;
import com.seproject.board.post.domain.repository.BookmarkRepository;
import com.seproject.board.post.domain.repository.PostRepository;
import com.seproject.member.domain.repository.MemberRepository;
import lombok.AllArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Optional;

@Service
@AllArgsConstructor
@Transactional
public class BookmarkAppService {
    private final BookmarkRepository bookmarkRepository;
    private final PostRepository postRepository;
    private final MemberRepository memberRepository;
    private final AccountRepository accountRepository;

    public void enrollBookmark(Long postId, String loginId) {
        Account account = accountRepository.findByLoginIdWithRole(loginId)
                .orElseThrow(() -> new CustomUserNotFoundException(ErrorCode.USER_NOT_FOUND,null));

        //TODO : member 없을 때 처리 필요, member 생성 or error?
        Member member = memberRepository.findByAccountId(account.getAccountId())
                .orElseThrow(() -> new NoSuchResourceException(ErrorCode.NOT_EXIST_MEMBER));
        Post post = postRepository.findById(postId)
                .orElseThrow(() -> new NoSuchResourceException(ErrorCode.NOT_EXIST_POST));

        boolean existBookmark = bookmarkRepository.existsByPostIdAndMemberId(postId, member.getBoardUserId());

        if (!existBookmark) {
            Bookmark bookmark = Bookmark.builder()
                    .markedPost(post)
                    .member(member)
                    .build();

            bookmarkRepository.save(bookmark);
        }
    }

    public void cancelBookmark(Long postId, String loginId) {
        Account account = accountRepository.findByLoginIdWithRole(loginId)
                .orElseThrow(() -> new CustomUserNotFoundException(ErrorCode.USER_NOT_FOUND,null));
        //TODO : member 없을 때 처리 필요, member 생성 or error?
        Member member = memberRepository.findByAccountId(account.getAccountId())
                .orElseThrow(() -> new NoSuchResourceException(ErrorCode.NOT_EXIST_MEMBER));

        Optional<Bookmark> bookmarkBox = bookmarkRepository.findByPostIdAndMemberId(postId, member.getBoardUserId());

        if(bookmarkBox.isPresent()){
            bookmarkRepository.delete(bookmarkBox.get());
        }
    }

    //내가 즐겨찾기한 게시물 목록 조회 -> BookmarkAppService에 있어야 할까 PostAppService에 있어야 할까?
//    public List<BookmarkDTO.BookmarkListResponseDTO> retrieveBookmarkPosts(Long userId) {
//        List<Bookmark> bookmarks = bookmarkRepository.findByUserId(userId);
//        if (bookmarks.size() == 0) {
//            throw new IllegalArgumentException(); // TODO : 메세지 추가
//        }
//
//        //TODO : JPQL 최적화,
//        return bookmarks.stream().map(bookmark -> {
//            Long postId = bookmark.getPostId();
//            Post post = findByIdOrThrow(postId, postRepository, "");
//            int commentSize = commentRepository.getCommentSizeByPostId(post.getPostId());
//            return BookmarkDTO.BookmarkListResponseDTO.toDTO(post, commentSize);
//        }).collect(Collectors.toList());
//
//    }
}