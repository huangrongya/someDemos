package com.etekcity.vbmp.common.comm.dto;

import com.etekcity.vbmp.common.config.VBMPRequest;

import lombok.Data;
import lombok.EqualsAndHashCode;

/**
 * @Description: 删除分享者
 * @Author: royle.Huang
 * @Date: 2018/9/13
 */
@Data
@EqualsAndHashCode(callSuper = true)
public class DeleteSharePeopleRequest extends VBMPRequest {
    private String sharedPeopleId;

    private String uuid;
}
