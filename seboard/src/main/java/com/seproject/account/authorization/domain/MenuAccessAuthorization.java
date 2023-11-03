package com.seproject.account.authorization.domain;

import com.seproject.account.authorization.utils.AuthorizationProperty;
import com.seproject.admin.domain.SelectOption;
import com.seproject.board.menu.domain.Menu;
import lombok.NoArgsConstructor;

import javax.persistence.Entity;
import javax.persistence.EnumType;
import javax.persistence.Enumerated;

@NoArgsConstructor
@Entity
public class MenuAccessAuthorization extends MenuAuthorization {

    @Enumerated(EnumType.STRING)
    private SelectOption selectOption;

    public MenuAccessAuthorization(Menu menu) {
        super(menu);
        selectOption = SelectOption.ALL;
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
        return AuthorizationProperty.ACCESS;
    }
}
