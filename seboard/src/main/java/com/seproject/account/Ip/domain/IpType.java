package com.seproject.account.Ip.domain;

import com.seproject.error.errorCode.ErrorCode;
import com.seproject.error.exception.CustomIllegalArgumentException;

public enum IpType {

    ADMIN, SPAM;

    public static IpType of(String ipType) {
        try {
            return valueOf(ipType);
        } catch (IllegalArgumentException e) {
            throw new CustomIllegalArgumentException(ErrorCode.NOT_EXIST_IP, e);
        }
    }

}
