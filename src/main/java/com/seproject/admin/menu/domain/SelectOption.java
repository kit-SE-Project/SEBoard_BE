package com.seproject.admin.menu.domain;

import com.seproject.error.errorCode.ErrorCode;
import com.seproject.error.exception.CustomIllegalArgumentException;

public enum SelectOption {
    ALL("all"),
    OVER_USER("overUser"),
    OVER_KUMOH("overKumoh"),
    ONLY_ADMIN("onlyAdmin"),
    SELECT("select"),
    ;


    private String name;
    SelectOption(String name) {
        this.name = name;
    }


    public static SelectOption of(String name) {
        switch (name) {
            case "all" : return ALL;
            case "overUser" : return OVER_USER;
            case "overKumoh" : return OVER_KUMOH;
            case "onlyAdmin" : return ONLY_ADMIN;
            case "select" : return SELECT;
        }
        throw new CustomIllegalArgumentException(ErrorCode.CANNOT_FOUND_OPTION,null);
    }

    public String getName() {
        return name;
    }

    public boolean equals(SelectOption selectOption){
        if(selectOption == null) return false;

        return selectOption.name.equals(this.name);

    }
}
