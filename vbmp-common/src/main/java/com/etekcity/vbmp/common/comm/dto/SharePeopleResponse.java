package com.etekcity.vbmp.common.comm.dto;

import java.util.List;

import com.etekcity.vbmp.common.config.VBMPResponse;

import com.etekcity.vbmp.common.comm.dto.inner.UserInfo;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.NoArgsConstructor;

@Data
@EqualsAndHashCode(callSuper = true)
@NoArgsConstructor
@AllArgsConstructor
public class SharePeopleResponse extends VBMPResponse {

    List<UserInfo> sharedPeople;

//    public SharePeopleResponse(String traceId) {
//        super(traceId);
//    }
}
