package com.seproject.board.common.utils;

import com.seproject.board.common.Status;

public class StatusUtils {

    public static boolean contains(String arg) {
        Status[] values = Status.values();
        for(int i = 0 ; i < values.length; i++) {
            if(values[i].name().equals(arg)) {
                return true;
            }
        }

        return false;
    }

}
