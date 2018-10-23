package com.etekcity.vbmp.common.constant;

public enum ConfigModelEnum {

    Outlet7A("7AOutlet"),
    Outlet10A("10AOutlet"),
    Outlet15A("15AOutlet"),
    InwallSwitch("InwallSwitch"),
    PowerStrip6A4U("PowerStrip6A4U"),
    AirPurifier131("AirPurifier131"),
    HumiDifier550("AirHumidifier550");


    private String configModel;

    ConfigModelEnum(String configModel) {
        this.configModel = configModel;
    }

    public String getConfigModel() {
        return configModel;
    }

}
