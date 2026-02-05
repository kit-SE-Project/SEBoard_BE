package com.seproject.admin.comment.persistence;

import com.querydsl.core.types.Projections;
import com.querydsl.core.types.dsl.BooleanExpression;
import com.querydsl.jpa.impl.JPAQueryFactory;
import com.seproject.admin.comment.controller.condition.AdminCommentRetrieveCondition;
import com.seproject.board.post.controller.PostSearchOptions;
import com.seproject.board.common.Status;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Repository;


import java.util.List;

import static com.seproject.board.comment.domain.model.QComment.comment;
import static com.seproject.admin.comment.controller.dto.CommentDTO.*;

@RequiredArgsConstructor
@Repository
public class AdminCommentSearchJpaRepository {
    private final JPAQueryFactory queryFactory;

    public Page<AdminCommentListResponse> findCommentListByCondition(AdminCommentRetrieveCondition condition, Pageable pageable) {
        List<AdminCommentListResponse> contents = queryFactory
                .select(Projections.constructor(AdminCommentListResponse.class, comment))
                .from(comment)
                .where(
                        searchOption(condition.getSearchOption(), condition.getQuery()),
                        reportedStatusEq(condition.getIsReported()),
                        readOnlyAuthorEq(condition.getIsReadOnlyAuthor())
                ).orderBy(comment.baseTime.createdAt.desc())
                .offset(pageable.getOffset())
                .limit(pageable.getPageSize())
                .fetch();

        Long count = queryFactory
                .select(comment.count())
                .from(comment)
                .where(
                        searchOption(condition.getSearchOption(), condition.getQuery()),
                        reportedStatusEq(condition.getIsReported()),
                        readOnlyAuthorEq(condition.getIsReadOnlyAuthor())
                )
                .fetchOne();

        return new PageImpl<>(contents, pageable, count);
    }

    public Page<AdminDeletedCommentResponse> findDeletedCommentList(Pageable pageable) {
        List<AdminDeletedCommentResponse> content = queryFactory
                .select(Projections.constructor(AdminDeletedCommentResponse.class,
                        comment))
                .from(comment)
                .where(comment.status.eq(Status.TEMP_DELETED))
                .offset(pageable.getOffset())
                .limit(pageable.getPageSize())
                .orderBy(comment.baseTime.createdAt.desc())
                .fetch();

        Long count = queryFactory
                .select(comment.count())
                .from(comment)
                .where(comment.status.eq(Status.TEMP_DELETED))
                .fetchOne();

        return new PageImpl<>(content, pageable, count);
    }

    private BooleanExpression readOnlyAuthorEq(Boolean isReadOnlyAuthor){
        if(isReadOnlyAuthor == null){
            return null;
        }else if(isReadOnlyAuthor){
            return comment.isOnlyReadByAuthor.isTrue();
        }else{
            return comment.isOnlyReadByAuthor.isFalse();
        }
    }

    private BooleanExpression reportedStatusEq(Boolean isReported){
        if(isReported == null){
            return comment.status.eq(Status.NORMAL)
                    .or(comment.status.eq(Status.REPORTED));
        }else if(!isReported){
            return comment.status.eq(Status.NORMAL);
        } else{
            return comment.status.eq(Status.REPORTED);
        }
    }

    private BooleanExpression searchOption(String searchOption, String query){
        if(searchOption==null || query==null){
            return null;
        }

        BooleanExpression contentOption = comment.contents.contains(query);
        BooleanExpression authorOption = comment.author.name.contains(query);

        switch (PostSearchOptions.valueOf(searchOption)){
            case CONTENT:{
                return contentOption;
            }
            case AUTHOR:{
                return authorOption;
            }
            case ALL:{
                return contentOption
                        .or(authorOption);
            }
            default:{
                return null;
            }
        }
    }
}
