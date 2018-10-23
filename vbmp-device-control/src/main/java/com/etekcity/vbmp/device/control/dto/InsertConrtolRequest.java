package com.etekcity.vbmp.device.control.dto;

public class InsertConrtolRequest {

    private String agreement;

    private String paramMap;

    private String url;

    private String httpMethodId;

    private String identify;

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

    public String getHttpMethodId() {
        return httpMethodId;
    }

    public void setHttpMethodId(String httpMethodId) {
        this.httpMethodId = httpMethodId;
    }

    public String getIdentify() {
        return identify;
    }

    public void setIdentify(String identify) {
        this.identify = identify;
    }
}
