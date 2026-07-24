package com.seproject.board.menu.domain.model;

import com.seproject.error.errorCode.ErrorCode;
import com.seproject.error.exception.CustomIllegalArgumentException;
import lombok.Builder;
import lombok.NoArgsConstructor;

import javax.persistence.DiscriminatorValue;
import javax.persistence.Entity;

@Entity
@NoArgsConstructor
@DiscriminatorValue("BOARD")
public class BoardMenu extends InternalSiteMenu {
    @Override
    public String getType() {
        return "BOARD";
    }

    @Builder
    public BoardMenu(Long categoryId, Menu superMenu, String name, String description, String categoryPathId) {
        super(categoryId, superMenu, name, description, categoryPathId);

        if(getDepth() > 2){
            throw new CustomIllegalArgumentException(ErrorCode.MAX_DEPTH,null);
        }
    }
}
