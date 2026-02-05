package com.seproject.board.post.controller;

import com.querydsl.core.types.dsl.BooleanExpression;
import lombok.Getter;

import java.util.function.Function;

import static com.seproject.board.post.domain.model.QPost.post;

@Getter
public enum PostSearchOptions {
    TITLE(query -> post.title.contains(query)),
    CONTENT(query -> post.contents.contains(query)),
    TITLE_OR_CONTENT(query -> post.title.contains(query).or(post.contents.contains(query))),
    AUTHOR(query -> post.author.name.contains(query)),
    ALL(query -> post.title.contains(query)
            .or(post.contents.contains(query))
            .or(post.author.name.contains(query)));

    PostSearchOptions(Function<String, BooleanExpression> searchOp) {
        this.searchOp = searchOp;
    }

    private Function<String, BooleanExpression> searchOp;

    public BooleanExpression search(String query){
        return searchOp.apply(query);
    }
}
