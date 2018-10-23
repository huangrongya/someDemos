package com.etekcity.vbmp.device.control.dto;

import java.util.Map;

public class GetConrtolRequest {

    /**
     * 设备uuid
     */
    private String uuid;

    /**
     * 协议识别号
     */
    private String identify;

    /**
     * 参数
     */
    private Object[] msg;

    private String configModel;

    private Map<String,Object> paramMap;

    public Map<String, Object> getParamMap() {
        return paramMap;
    }

    public void setParamMap(Map<String, Object> paramMap) {
        this.paramMap = paramMap;
    }

    public String getConfigModel() {
        return configModel;
    }

    public void setConfigModel(String configModel) {
        this.configModel = configModel;
    }

    public String getUuid() {
        return uuid;
    }

    public void setUuid(String uuid) {
        this.uuid = uuid;
    }

    public String getIdentify() {
        return identify;
    }

    public void setIdentify(String identify) {
        this.identify = identify;
    }

    public Object[] getMsg() {
        return msg;
    }

    public void setMsg(Object[] msg) {
        this.msg = msg;
    }
}
