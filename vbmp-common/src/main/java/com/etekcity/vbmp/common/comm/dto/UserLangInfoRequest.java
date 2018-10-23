/**
 *
 */
package com.etekcity.vbmp.common.comm.dto;

import java.util.List;

import com.etekcity.vbmp.common.config.VBMPRequest;

import lombok.Data;
import lombok.EqualsAndHashCode;

/**
 * @author puyol
 */
@Data
@EqualsAndHashCode(callSuper = true)
public class UserLangInfoRequest extends VBMPRequest {

    private List<String> fields;
}
