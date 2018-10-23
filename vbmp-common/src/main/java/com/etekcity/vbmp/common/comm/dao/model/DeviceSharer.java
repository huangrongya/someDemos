package com.etekcity.vbmp.common.comm.dao.model;

import javax.persistence.GeneratedValue;
import javax.persistence.GenerationType;
import javax.persistence.Id;
import java.io.Serializable;
import java.util.Date;

/**
 * device_sharer
 *
 * @author
 */
public class DeviceSharer implements Serializable {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY, generator = "SELECT next value for MYCATSEQ_GLOBAL")
    private Integer id;

    private String deviceCid;

    /**
     * 共享者id
     */
    private String userId;

    /**
     * 是否历史共享者 1.是历史共享者 0.非历史共享者
     */
    private Boolean isHistory;

    /**
     * 是否删除 1.已删除 0.未删除(已删除还有可能出现在历史共享者中)
     */
    private Boolean isDeleted;

    /**
     * 创建时间
     */
    private Date createTime;

    /**
     * 更新时间, 排序用
     */
    private Date updateTime;

    private static final long serialVersionUID = 1L;

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
        this.deviceCid = deviceCid;
    }

    public String getUserId() {
        return userId;
    }

    public void setUserId(String userId) {
        this.userId = userId;
    }

    public Boolean getIsHistory() {
        return isHistory;
    }

    public void setIsHistory(Boolean isHistory) {
        this.isHistory = isHistory;
    }

    public Boolean getIsDeleted() {
        return isDeleted;
    }

    public void setIsDeleted(Boolean isDeleted) {
        this.isDeleted = isDeleted;
    }

    public Date getCreateTime() {
        return createTime;
    }

    public void setCreateTime(Date createTime) {
        this.createTime = createTime;
    }

    public Date getUpdateTime() {
        return updateTime;
    }

    public void setUpdateTime(Date updateTime) {
        this.updateTime = updateTime;
    }
}