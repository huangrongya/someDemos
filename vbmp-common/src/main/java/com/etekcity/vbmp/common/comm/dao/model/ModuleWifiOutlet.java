package com.etekcity.vbmp.common.comm.dao.model;

public class ModuleWifiOutlet {
    private Integer id;

    private String deviceCid;

    private String uuid;

    private String energySavingStatus;

    private Double maxCost;

    private String currency;

    private Double costPerKilowattHour;

    private String powerProtectionStatus;

    private Float threshold;

    private Float maxPower;

    private String nightLightAutomode;

    private String nightLightStatus;

    private Integer nightLightBrightness;

    private String nightLightName;

    private String lightSwitch;

    private String outletJsonName;

    private String outletJsonImg;

    public Integer getId() {
        return id;
    }

    public void setId(Integer id) {
        this.id = id;
    }

    public String getDeviceCid() {
        return deviceCid;
    }

    public void setDeviceCid(String deviceCid) {
        this.deviceCid = deviceCid == null ? null : deviceCid.trim();
    }

    public String getUuid() {
        return uuid;
    }

    public void setUuid(String uuid) {
        this.uuid = uuid == null ? null : uuid.trim();
    }

    public String getEnergySavingStatus() {
        return energySavingStatus;
    }

    public void setEnergySavingStatus(String energySavingStatus) {
        this.energySavingStatus = energySavingStatus == null ? null : energySavingStatus.trim();
    }

    public Double getMaxCost() {
        return maxCost;
    }

    public void setMaxCost(Double maxCost) {
        this.maxCost = maxCost;
    }

    public String getCurrency() {
        return currency;
    }

    public void setCurrency(String currency) {
        this.currency = currency == null ? null : currency.trim();
    }

    public Double getCostPerKilowattHour() {
        return costPerKilowattHour;
    }

    public void setCostPerKilowattHour(Double costPerKilowattHour) {
        this.costPerKilowattHour = costPerKilowattHour;
    }

    public String getPowerProtectionStatus() {
        return powerProtectionStatus;
    }

    public void setPowerProtectionStatus(String powerProtectionStatus) {
        this.powerProtectionStatus = powerProtectionStatus == null ? null : powerProtectionStatus.trim();
    }

    public Float getThreshold() {
        return threshold;
    }

    public void setThreshold(Float threshold) {
        this.threshold = threshold;
    }

    public Float getMaxPower() {
        return maxPower;
    }

    public void setMaxPower(Float maxPower) {
        this.maxPower = maxPower;
    }

    public String getNightLightAutomode() {
        return nightLightAutomode;
    }

    public void setNightLightAutomode(String nightLightAutomode) {
        this.nightLightAutomode = nightLightAutomode == null ? null : nightLightAutomode.trim();
    }

    public String getNightLightStatus() {
        return nightLightStatus;
    }

    public void setNightLightStatus(String nightLightStatus) {
        this.nightLightStatus = nightLightStatus == null ? null : nightLightStatus.trim();
    }

    public Integer getNightLightBrightness() {
        return nightLightBrightness;
    }

    public void setNightLightBrightness(Integer nightLightBrightness) {
        this.nightLightBrightness = nightLightBrightness;
    }

    public String getNightLightName() {
        return nightLightName;
    }

    public void setNightLightName(String nightLightName) {
        this.nightLightName = nightLightName == null ? null : nightLightName.trim();
    }

    public String getLightSwitch() {
        return lightSwitch;
    }

    public void setLightSwitch(String lightSwitch) {
        this.lightSwitch = lightSwitch == null ? null : lightSwitch.trim();
    }

    public String getOutletJsonName() {
        return outletJsonName;
    }

    public void setOutletJsonName(String outletJsonName) {
        this.outletJsonName = outletJsonName == null ? null : outletJsonName.trim();
    }

    public String getOutletJsonImg() {
        return outletJsonImg;
    }

    public void setOutletJsonImg(String outletJsonImg) {
        this.outletJsonImg = outletJsonImg == null ? null : outletJsonImg.trim();
    }
}