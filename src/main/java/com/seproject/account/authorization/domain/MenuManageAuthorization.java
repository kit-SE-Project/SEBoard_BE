package com.seproject.account.authorization.domain;

import com.seproject.account.authorization.utils.AuthorizationProperty;
import com.seproject.admin.menu.domain.SelectOption;
import com.seproject.board.menu.domain.model.Menu;
import com.seproject.error.errorCode.ErrorCode;
import com.seproject.error.exception.CustomIllegalArgumentException;
import lombok.Getter;
import lombok.NoArgsConstructor;

import javax.persistence.Entity;
import javax.persistence.EnumType;
import javax.persistence.Enumerated;

@Getter
@NoArgsConstructor
@Entity
public class MenuManageAuthorization extends MenuAuthorization {

    @Enumerated(EnumType.STRING)
    private SelectOption selectOption;

    public MenuManageAuthorization(Menu menu) {
        super(menu);
        this.selectOption = SelectOption.ONLY_ADMIN;
    }

    @Override
    public void setSelectOption(SelectOption selectOption) {
        if(selectOption.equals(SelectOption.ALL)){
            throw new CustomIllegalArgumentException(ErrorCode.INVALID_REQUEST);
        }

        this.selectOption = selectOption;
    }

    @Override
    public SelectOption getSelectOption() {
        return selectOption;
    }

    @Override
    public AuthorizationProperty getAuthorizationProperty() {
        return AuthorizationProperty.MANAGEABLE;
    }
}
