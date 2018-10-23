package com.etekcity.vbmp.common.config;

import lombok.Data;

import java.util.UUID;

@Data
public class VBMPRequest {
    private String token;

    private String accountId;

    private String timeZone;

    private String traceId = UUID.randomUUID().toString();

}
