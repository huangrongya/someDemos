package com.etekcity.vbmp.device.control.exception;

public class VdmpException extends RuntimeException {

    private int code;

    private String msg;

    public VdmpException(int code, String msg) {
        this.code = code;
        this.msg = msg;
    }

    public int getCode() {
        return code;
    }

    public void setCode(int code) {
        this.code = code;
    }

    public String getMsg() {
        return msg;
    }

    public void setMsg(String msg) {
        this.msg = msg;
    }
}
