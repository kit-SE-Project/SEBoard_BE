package com.seproject.account.authorization.domain;

import com.seproject.account.authorization.utils.AuthorizationProperty;
import com.seproject.admin.menu.domain.SelectOption;
import com.seproject.board.menu.domain.model.Menu;
import lombok.NoArgsConstructor;

import javax.persistence.Entity;
import javax.persistence.EnumType;
import javax.persistence.Enumerated;

@NoArgsConstructor
@Entity
public class MenuEditAuthorization extends MenuAuthorization{

    @Enumerated(EnumType.STRING)
    private SelectOption selectOption;

    public MenuEditAuthorization(Menu menu) {
        super(menu);
        this.selectOption = SelectOption.ALL;
    }

    @Override
    public void setSelectOption(SelectOption selectOption) {
        this.selectOption = selectOption;
    }

    @Override
    public SelectOption getSelectOption() {
        return selectOption;
    }

    @Override
    public AuthorizationProperty getAuthorizationProperty() {
        return AuthorizationProperty.EDITABLE;
    }
}
