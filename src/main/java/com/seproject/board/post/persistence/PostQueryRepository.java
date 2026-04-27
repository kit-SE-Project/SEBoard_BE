package com.seproject.board.post.persistence;

import com.querydsl.core.types.dsl.BooleanExpression;
import com.querydsl.jpa.impl.JPAQueryFactory;
import com.seproject.board.common.Status;
import com.seproject.board.post.controller.PostSearchOptions;
import com.seproject.board.post.controller.dto.PostResponse.RetrievePostListResponseElement;
import com.seproject.board.post.domain.model.LikeType;
import com.seproject.board.post.domain.model.Post;
import com.seproject.file.domain.model.FileMetaData;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import java.util.Set;

import static com.querydsl.core.types.Projections.constructor;
import static com.seproject.board.menu.domain.model.QCategory.category;
import static com.seproject.board.post.domain.model.QPost.post;
import static com.seproject.board.post.domain.model.QPostLike.postLike;
import static com.seproject.member.domain.QBoardUser.boardUser;

@Repository
@RequiredArgsConstructor
public class PostQueryRepository {

    private final JPAQueryFactory jpaQueryFactory;

    public Optional<Post> findByIdWithAll(Long id) {

        Post findPost = jpaQueryFactory.select(post)
                .from(post)
                .leftJoin(post.category, category)
                .leftJoin(post.category.superMenu)
                .leftJoin(post.author, boardUser)
                .where(
                        postIdEq(id),
                        normalStatusEq()
                )
                .fetchOne();

        if(findPost!=null){
            Set<FileMetaData> attachments = findPost.getAttachments();
        }
        return Optional.ofNullable(findPost);
    }

    private static BooleanExpression postIdEq(Long id) {
        return post.postId.eq(id);
    }


    public Page<RetrievePostListResponseElement> searchPostList(Long categoryId, PostSearchOptions queryOption, String query, PageRequest pageRequest) {
        List<RetrievePostListResponseElement> list = jpaQueryFactory
                .select(constructor(RetrievePostListResponseElement.class, post))
                .from(post)
                .leftJoin(post.category, category)
                .leftJoin(post.category.superMenu)
                .leftJoin(post.author, boardUser)
                .where(categoryEq(categoryId))
                .where(queryLike(queryOption, query))
                .where(normalStatusEq())
                .orderBy(post.baseTime.createdAt.desc())
                .offset(pageRequest.getOffset())
                .limit(pageRequest.getPageSize())
                .fetch();

        Long count = jpaQueryFactory
                .select(post.count())
                .from(post)
                .leftJoin(post.category, category)
                .leftJoin(post.category.superMenu)
                .leftJoin(post.author, boardUser)
                .where(categoryEq(categoryId))
                .where(queryLike(queryOption, query))
                .where(normalStatusEq())
                .fetchOne();

        return new PageImpl<>(list, pageRequest, count);
    }

    private BooleanExpression normalStatusEq() {
        return post.status.eq(Status.NORMAL);
    }

    private BooleanExpression queryLike(PostSearchOptions queryOption, String query) {
        if(query==null || queryOption==null){
            return null;
        }

        return queryOption.search(query);
    }

    public List<RetrievePostListResponseElement> findTrendingPosts(Long categoryId, LocalDateTime since, int limit) {
        return jpaQueryFactory
                .select(constructor(RetrievePostListResponseElement.class, post))
                .from(post)
                .leftJoin(post.category, category)
                .leftJoin(post.category.superMenu)
                .leftJoin(post.author, boardUser)
                .leftJoin(postLike).on(
                        postLike.post.eq(post)
                                .and(postLike.likeType.eq(LikeType.LIKE))
                                .and(postLike.updatedAt.goe(since))
                )
                .where(categoryEq(categoryId))
                .where(category.popularPostEnabled.isTrue())
                .where(normalStatusEq())
                .groupBy(post.postId)
                .having(postLike.count().gt(0))
                .orderBy(postLike.count().desc())
                .limit(limit)
                .fetch();
    }

    private BooleanExpression categoryEq(Long categoryId) {
        if(categoryId == null){
            return null;
        }

        return post.category.menuId.eq(categoryId).or(post.category.superMenu.menuId.eq(categoryId));
    }

}
