package com.seproject.board.menu.domain.model;

import lombok.NoArgsConstructor;

import javax.persistence.DiscriminatorValue;
import javax.persistence.Entity;

@Entity
@NoArgsConstructor
@DiscriminatorValue("RECRUIT")
public class RecruitMenu extends InternalSiteMenu {

    public RecruitMenu(Menu superMenu, String name, String description) {
        super(null, superMenu, name, description, "recruit");
    }

    @Override
    public String getType() {
        return "RECRUIT";
    }
}
