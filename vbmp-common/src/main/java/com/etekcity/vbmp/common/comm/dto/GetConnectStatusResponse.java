package com.etekcity.vbmp.common.comm.dto;


import com.etekcity.vbmp.common.config.VBMPResponse;

import lombok.Data;
import lombok.EqualsAndHashCode;

@Data
@EqualsAndHashCode(callSuper = true)
public class GetConnectStatusResponse extends VBMPResponse {

    private String state;

    private String uuid;

    public String getState() {
        return state;
    }

    public void setState(String state) {
        this.state = state;
    }

    public String getUuid() {
        return uuid;
    }

    public void setUuid(String uuid) {
        this.uuid = uuid;
    }


}
