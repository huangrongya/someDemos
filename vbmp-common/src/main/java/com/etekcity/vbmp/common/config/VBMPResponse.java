package com.etekcity.vbmp.common.config;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class VBMPResponse {
    private int code;

    private String msg;

//    private String traceId;
//
//    public VBMPResponse(int code, String traceId) {
//        this.code = code;
//        this.traceId = traceId;
//    }
//
//    public VBMPResponse(String traceId) {
//        this.traceId = traceId;
//    }

}
