package com.seproject.board.post.domain.repository;

import com.seproject.board.post.domain.model.Bookmark;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Optional;

public interface BookmarkRepository extends JpaRepository<Bookmark,Long> {
    @Query("select count(*) from Bookmark b where b.member.account.loginId = :loginId")
    Integer countsBookmarkByLoginId(@Param("loginId") String loginId);
    @Query(value = "select exists(select * from bookmarks where post_id = :postId and member_id = :memberId)", nativeQuery = true)
    boolean existsByPostIdAndMemberId(@Param("postId") Long postId,@Param("memberId")  Long memberId);

    @Query(value="select * from bookmarks where post_id = :postId and member_id = :memberId", nativeQuery = true)
    Optional<Bookmark> findByPostIdAndMemberId(@Param("postId") Long postId,@Param("memberId") Long memberId);

    @Query("select b from Bookmark b join b.member join b.member.account where b.member.account.accountId = :accountId")
    List<Bookmark> findBookmarkByAccountId(@Param("accountId") Long accountId);

    @Query("select b from Bookmark b join b.member join b.member.account where b.member.account.accountId in :accountIds")
    List<Bookmark> findAllByAccountIds(@Param("accountIds") List<Long> accountIds);
}