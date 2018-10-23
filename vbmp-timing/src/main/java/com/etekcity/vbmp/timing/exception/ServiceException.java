package com.etekcity.vbmp.timing.exception;

import lombok.Data;

@Data
public class ServiceException extends RuntimeException {
    private int code;

    private String msg;

    public ServiceException(int code, String msg) {
        this.code = code;
        this.msg = msg;
    }

    @Override
    public String toString() {
        return "ServiceException{" +
                "code=" + code +
                ", msg='" + msg + '\'' +
                '}';
    }
}
