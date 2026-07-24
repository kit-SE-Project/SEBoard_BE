package com.seproject.board.menu.domain.model;

import lombok.NoArgsConstructor;
import org.apache.commons.lang3.RandomStringUtils;

import javax.persistence.Entity;

@Entity
@NoArgsConstructor
public abstract class InternalSiteMenu extends Menu {

    protected InternalSiteMenu(Long categoryId, Menu superMenu, String name, String description, String categoryPathId) {
        super(categoryId, superMenu, name, description);

        this.urlInfo = categoryPathId;

        if(categoryPathId==null || categoryPathId.isEmpty()){
            this.urlInfo = RandomStringUtils.randomAlphabetic(10);
        }
    }

    public void changeCategoryPathId(String urlId) {
        this.urlInfo = urlId;
    }
}
