/**
 *
 */
package com.etekcity.vbmp.common.comm.dto;

import java.util.List;

import com.etekcity.vbmp.common.config.VBMPResponse;
import com.etekcity.vbmp.common.comm.dto.inner.UserInfo;

import lombok.Data;
import lombok.EqualsAndHashCode;

/**
 * @author puyol
 */
@Data
@EqualsAndHashCode(callSuper = true)
public class UserInfoResponse extends VBMPResponse {

    private List<UserInfo> userList;
}
