package com.seproject.board.menu.domain.model;

import com.seproject.board.menu.service.MenuService;
import com.seproject.error.errorCode.ErrorCode;
import com.seproject.error.exception.CustomIllegalArgumentException;
import lombok.NoArgsConstructor;

import javax.persistence.DiscriminatorValue;
import javax.persistence.Entity;

@NoArgsConstructor
@Entity
@DiscriminatorValue("CATEGORY")
public class Category extends BoardMenu {
    public Category(Long categoryId, Menu superMenu, String name, String description, String categoryPathId) {
        super(categoryId, superMenu, name, description, categoryPathId);

        if(superMenu.getClass() != BoardMenu.class){
            throw new CustomIllegalArgumentException(ErrorCode.CATEGORY_CREATE_ERROR,null);
        }
    }

    @Override
    public String getType(){
        return "CATEGORY";
    }

    @Override
    public boolean isRemovable(MenuService categoryService) {
        return !categoryService.hasPost(getMenuId());
    }
}
