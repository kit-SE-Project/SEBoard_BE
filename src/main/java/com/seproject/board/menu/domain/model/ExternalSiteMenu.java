package com.seproject.board.menu.domain.model;

import lombok.NoArgsConstructor;

import javax.persistence.DiscriminatorValue;
import javax.persistence.Entity;

@Entity
@NoArgsConstructor
@DiscriminatorValue("EXTERNAL")
public class ExternalSiteMenu extends Menu {

    public ExternalSiteMenu(Long menuId, Menu superMenu, String name, String description,String externalSiteUrl) {
        super(menuId, superMenu, name, description);
        this.urlInfo = externalSiteUrl;
        if(getDepth()>2){
            throw new IllegalArgumentException();
        }
    }

    @Override
    public String getType(){
        return "EXTERNAL";
    }

    public void changeExternalSiteUrl(String externalUrl) {
        this.urlInfo = externalUrl;
    }
}
