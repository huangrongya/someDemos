package com.etekcity.vbmp.device.control.dao.model;

public class DeviceGetConrtol {
    /**
     * 通用协议
     */
    private String agreement;

    /**
     * 协议集合
     */
    private String paramMap;

    private String url;

    private String httpMethodID;

    public String getHttpMethodID() {
        return httpMethodID;
    }

    public void setHttpMethodID(String httpMethodID) {
        this.httpMethodID = httpMethodID;
    }

    public String getAgreement() {
        return agreement;
    }

    public void setAgreement(String agreement) {
        this.agreement = agreement;
    }

    public String getParamMap() {
        return paramMap;
    }

    public void setParamMap(String paramMap) {
        this.paramMap = paramMap;
    }

    public String getUrl() {
        return url;
    }

    public void setUrl(String url) {
        this.url = url;
    }
}
