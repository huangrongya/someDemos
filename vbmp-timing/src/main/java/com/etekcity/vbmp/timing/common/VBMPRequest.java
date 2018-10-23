package com.etekcity.vbmp.timing.common;

import lombok.Data;

@Data
public class VBMPRequest {
    private String token;

    private String accountId;

    private String timeZone;
}
