package com.etekcity.vbmp.common.router.service;

import com.etekcity.vbmp.common.router.dao.model.VbmpRouter;

/**
 * @ClassName VbmpRouterService
 * @Description
 * @Author Ericer
 * @Date 9-14 上午10:27
 **/
public interface VbmpRouterService {
    /**
     * @return com.etekcity.vbmp.common.router.dao.model.VbmpRouter
     * @Author Ericer
     * @Description 根据configModel查询信息
     * @Date 上午10:51 18-9-14
     * @Param [configModel]
     **/
    VbmpRouter queryVbmpRouterDatebaseOrRedis(String configModel);
}
