package com.etekcity.vbmp.timing.common;

import lombok.Data;

@Data
public class VBMPResponse {
    private int code;

    private String msg;

    public VBMPResponse() {

    }

    public VBMPResponse(int code) {
        this.code = code;
    }

    public VBMPResponse(int code, String msg) {
        this.code = code;
        this.msg = msg;
    }
}
