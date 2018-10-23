package com.etekcity.vbmp.common.comm.dto;

import com.etekcity.vbmp.common.config.VBMPResponse;
import lombok.Data;
import lombok.EqualsAndHashCode;

import java.util.List;

/**
 * @Description: 返回用户idlist
 * @Author: royle
 * @Date: 2018/09/27
 */
@Data
@EqualsAndHashCode(callSuper = true)
public class AccountIdResponse extends VBMPResponse {

    List<String> accountIds;
}
