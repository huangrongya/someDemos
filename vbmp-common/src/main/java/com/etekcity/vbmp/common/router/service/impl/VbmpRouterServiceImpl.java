package com.etekcity.vbmp.common.router.service.impl;

import com.alibaba.fastjson.JSONObject;
import com.etekcity.vbmp.common.config.redis.RedisService;
import com.etekcity.vbmp.common.router.constant.RouterConstant;
import com.etekcity.vbmp.common.router.dao.mapper.VbmpRouterMapper;
import com.etekcity.vbmp.common.router.dao.model.VbmpRouter;
import com.etekcity.vbmp.common.router.service.VbmpRouterService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.util.Assert;
import org.springframework.util.StringUtils;

import java.util.List;

/**
 * @ClassName VbmpRouterServiceImpl
 * @Description
 * @Author Ericer
 * @Date 9-14 上午10:28
 **/
@Service
@Slf4j
public class VbmpRouterServiceImpl implements VbmpRouterService {
    @Autowired
    private VbmpRouterMapper vbmpRouterMapper;
    @Autowired
    private RedisService redisService;

    public List<VbmpRouter> queryVbmpRouterList(VbmpRouter query) {
        return vbmpRouterMapper.select(query);
    }

    /**
     * @return com.etekcity.vbmp.common.router.dao.model.VbmpRouter
     * @Author Ericer
     * @Description 根据configModel查询信息
     * @Date 上午10:51 18-9-14
     * @Param [configModel]
     **/
    @Override
    public VbmpRouter queryVbmpRouterDatebaseOrRedis(String configModel) {
        Assert.hasLength(configModel, "queryVbmpRouterDatebaseOrRedis configModel不能为空");
        //redis获取信息
        VbmpRouter vbmpRouter = null;
        String vbmpRouterJson = (String) redisService.getMapField(RouterConstant.VBMP_ROUTER, configModel);
        if (StringUtils.hasText(vbmpRouterJson)) {
            vbmpRouter = JSONObject.parseObject(vbmpRouterJson, VbmpRouter.class);
        }
        //redis不存在，查询数据库
        if (vbmpRouter == null) {
            VbmpRouter vbmpRouterQuery = new VbmpRouter();
            vbmpRouterQuery.setConfigModel(configModel);
            vbmpRouter = vbmpRouterMapper.selectOne(vbmpRouterQuery);
            //不为空添加到库
            if (vbmpRouter != null) {
                redisService.addMap(RouterConstant.VBMP_ROUTER, configModel, JSONObject.toJSONString(vbmpRouter));
            }
        }
        return vbmpRouter;
    }


}
