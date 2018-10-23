package com.etekcity.vbmp.common.comm.service.impl;

import com.alibaba.fastjson.JSON;
import com.etekcity.vbmp.common.comm.service.SendFcmUserService;
import com.etekcity.vbmp.common.config.VBMPResponse;
import com.etekcity.vbmp.common.config.redis.RedisService;
import com.etekcity.vbmp.common.constant.CommonConstant;
import com.etekcity.vbmp.common.constant.ErrorConstant;
import com.etekcity.vbmp.common.comm.dao.model.DeviceInfo;
import com.etekcity.vbmp.common.comm.dto.*;
import com.etekcity.vbmp.common.comm.dto.inner.UserInfo;
import com.etekcity.vbmp.common.exception.ServiceException;
import com.etekcity.vbmp.common.comm.dao.mapper.DeviceSharerMapper;
import com.etekcity.vbmp.common.comm.dao.model.DeviceSharer;
import com.etekcity.vbmp.common.comm.service.DeviceService;
import com.etekcity.vbmp.common.comm.service.ShareService;
import com.etekcity.vbmp.common.utils.MyStringUtils;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import java.util.*;

@Service
@Slf4j
public class ShareServiceImpl implements ShareService {

    @Autowired
    RedisService redisService;
    @Autowired
    DeviceSharerMapper deviceSharerMapper;
    @Autowired
    DeviceService deviceService;
    @Autowired
    CommonUserServiceImpl commonUserService;
    @Autowired
    SendFcmUserService sendFcmUserService;


    @Override
    @Transactional
    public VBMPResponse addSharePeople(AddSharePeopleRequest request) {
        log.info("添加共享者, {}", JSON.toJSONString(request));
        String uuid = request.getUuid();
        //查询设备cid
        DeviceInfo deviceInfo = deviceService.queryDeviceByUuid(uuid);
        if (deviceInfo == null || MyStringUtils.isNullData(deviceInfo.getDeviceCid())) {
            throw new ServiceException(ErrorConstant.ERR_DEVICE_NOT_EXIST, ErrorConstant.ERR_DEVICE_NOT_EXIST_MSG);
        }
        String cid = deviceInfo.getDeviceCid();
        VBMPResponse response = new VBMPResponse();
        List<String> newUserNameList = request.getSharedPeoples();
//        //用户接口查询用户信息
        UserInfoByUidRequest userRequest = new UserInfoByUidRequest();
        userRequest.setAccounts(newUserNameList);
        userRequest.setAccountId(request.getAccountId());
        userRequest.setTimeZone(request.getTimeZone());
        userRequest.setToken(request.getToken());
        userRequest.setTraceId(request.getTraceId());
        List<UserInfo> userInfoList = commonUserService.getAccountIdByAccount(userRequest);
        if (userInfoList.isEmpty()) {
            response.setCode(ErrorConstant.ERR_SHARED_USER_NOT_EXIST);
            response.setMsg(ErrorConstant.ERR_SHARED_USER_NOT_EXIST_MSG);
            return response;
        }
        List<String> newUserIdList = new ArrayList<>();
        for (UserInfo userInfo : userInfoList) {
            if (StringUtils.hasText(userInfo.getAccountID())) {
                newUserIdList.add(userInfo.getAccountID());
            }
        }
//        List<String> newUserIdList = request.getSharedPeoples();

        //查询已存在的共享者, 已共享不通知, 其它都要通知; 更新的是已共享和历史共享但is deleted
        List<String> sharedUserIdList = queryDeviceSharerListByUuid(uuid);
        List<String> historyUserIdList = queryDeviceSharerHistoryByUuid(uuid);
        //判断是否已经共享
        List<DeviceSharer> newDeviceSharerList = new ArrayList<>();     //新增共享者
        List<DeviceSharer> sharedList = new ArrayList<>();              //已共享
        List<DeviceSharer> historyShareList = new ArrayList<>();        //当前未共享的历史共享者
        List<DeviceSharer> allHistoryShareList = new ArrayList<>();     //所有历史共享者
        for (String newUserId : newUserIdList) {
            if (!StringUtils.hasText(newUserId)) {
                continue;
            }
            DeviceSharer bean = new DeviceSharer();
            bean.setUserId(newUserId);
            bean.setDeviceCid(cid);
            bean.setIsDeleted(false);
            bean.setIsHistory(true);
            bean.setUpdateTime(new Date());
            if (sharedUserIdList.contains(newUserId)) {          //已共享
                sharedList.add(bean);
                if (historyUserIdList.contains(newUserId)) {         //所有历史
                    allHistoryShareList.add(bean);
                }
            } else if (historyUserIdList.contains(newUserId)) {
                historyShareList.add(bean);                         //未共享但是是历史共享者
                allHistoryShareList.add(bean);                      //所有历史
            } else {                                                 //完全新增
                bean.setCreateTime(new Date());
                newDeviceSharerList.add(bean);
            }
        }
        //未分享新增直接添加
        if (!newDeviceSharerList.isEmpty()) {
            for (DeviceSharer deviceSharer : newDeviceSharerList) {
                deviceSharerMapper.insert(deviceSharer);
            }
        }
        //已分享更新
        if (!sharedList.isEmpty()) {
            deviceSharerMapper.addExistSharers(cid, sharedList);
        }
        //未共享但是是历史共享者 更新
        if (!historyShareList.isEmpty()) {
            deviceSharerMapper.addExistSharers(cid, historyShareList);
        }
        //更新redis缓存的update time
        String redisShareKey = CommonConstant.REDIS_KEY_DEVICE_SHARE.concat(CommonConstant.COLON_STRING).concat(uuid);
        String historyRedisShareKey = CommonConstant.REDIS_KEY_DEVICE_SHARE_HISTORY.concat(CommonConstant.COLON_STRING).concat(uuid);
        updateRedisShareTime(sharedList, redisShareKey);
        updateRedisShareTime(allHistoryShareList, historyRedisShareKey);

        //添加至历史共享者
        redisService.addList(historyRedisShareKey, newDeviceSharerList);
        //合并所有未共享, 添加至共享缓存
        newDeviceSharerList.addAll(historyShareList);
        redisService.addList(redisShareKey, newDeviceSharerList);
        //通知所有新增被分享者
        SendFcmShareRequest fcmShareRequest = new SendFcmShareRequest();
        fcmShareRequest.setModelName(deviceInfo.getDeviceType());
        fcmShareRequest.setMsgKey(CommonConstant.FCM_SHARE_ADD);
        fcmShareRequest.setUuid(uuid);
        fcmShareRequest.setAccountId(request.getAccountId());
        fcmShareRequest.setToken(request.getToken());
        fcmShareRequest.setTraceId(request.getTraceId());
        if (!newDeviceSharerList.isEmpty()) {
            for (DeviceSharer deviceSharer : newDeviceSharerList) {
                fcmShareRequest.setSharedPeopleId(deviceSharer.getUserId());
                sendFcmUserService.sendFcmUserAddOrDel(fcmShareRequest);
            }
        }
        return response;
    }

    /**
     * @Description: 更新共享者和历史共享者的更新时间, 便于查询时排序
     * @Author: royle.Huang
     * @Date: 2018/9/14
     */
    private void updateRedisShareTime(List<DeviceSharer> shareList, String redisKey) {
        if (redisService.exists(redisKey) && !shareList.isEmpty()) {
            List<DeviceSharer> list = redisService.getList(redisKey);
            for (int i = 0; i < list.size(); i++) {
                DeviceSharer deviceSharer = list.get(i);
                for (int j = 0; j < shareList.size(); j++) {
                    DeviceSharer sharer = shareList.get(j);
                    if (deviceSharer.getDeviceCid().equals(sharer.getDeviceCid()) && deviceSharer.getUserId().equals(sharer.getUserId())) {
                        deviceSharer.setUpdateTime(sharer.getUpdateTime());
                    }
                }
            }
            redisService.remove(redisKey);
            redisService.addList(redisKey, list);
        }
    }

    @Override
    @Transactional
    public VBMPResponse deleteSharePeople(DeleteSharePeopleRequest request) {
        log.info("删除共享者, {}", JSON.toJSONString(request));
        VBMPResponse response = new VBMPResponse();
        String userId = request.getSharedPeopleId();
        String uuid = request.getUuid();
        DeviceInfo deviceInfo = deviceService.queryDeviceByUuid(uuid);
        deleteSharePeopleByUuidAndUserId(userId, uuid);

        //发送消息通知设备被分享人
        SendFcmShareRequest fcmShareRequest = new SendFcmShareRequest();
        fcmShareRequest.setModelName(deviceInfo.getDeviceType());
        fcmShareRequest.setMsgKey(CommonConstant.FCM_SHARE_DEL);
        fcmShareRequest.setUuid(uuid);
        fcmShareRequest.setAccountId(request.getAccountId());
        fcmShareRequest.setToken(request.getToken());
        fcmShareRequest.setTraceId(request.getTraceId());
        fcmShareRequest.setSharedPeopleId(request.getSharedPeopleId());
        sendFcmUserService.sendFcmUserAddOrDel(fcmShareRequest);
        return response;
    }

    @Override
    public void deleteSharePeopleByUuidAndUserId(String userId, String uuid) {
        //查询设备cid
        DeviceInfo deviceInfo = deviceService.queryDeviceByUuid(uuid);
        if (deviceInfo == null || MyStringUtils.isNullData(deviceInfo.getDeviceCid())) {
            throw new ServiceException(ErrorConstant.ERR_DEVICE_NOT_EXIST, ErrorConstant.ERR_DEVICE_NOT_EXIST_MSG);
        }
        String cid = deviceInfo.getDeviceCid();
        //删除分享人对应的设备缓存
        //userId为空, 删除该设备所有共享者
        if (userId==null){
            List<String> sharedPeopleIds = queryDeviceSharerListByUuid(uuid);
            if (sharedPeopleIds!=null && sharedPeopleIds.size()>0){
                for (String sharedPeopleId : sharedPeopleIds) {
                    //删除分享人设备缓存
                    deviceService.deleteShareDeviceInRedis(sharedPeopleId, uuid);
                }
            }
        }else {
            //删除分享人设备缓存
            deviceService.deleteShareDeviceInRedis(userId, uuid);
        }

        //删除redis（删除设备对应的分享人缓存）
        String redisShareKey = CommonConstant.REDIS_KEY_DEVICE_SHARE.concat(CommonConstant.COLON_STRING).concat(uuid);
        if (redisService.exists(redisShareKey)) {
            //userId为空, 删除该设备所有共享者
            if (userId==null){
                redisService.remove(redisShareKey);
            }else {
                List<DeviceSharer> deviceSharerList = redisService.getList(redisShareKey);
                for (int i = deviceSharerList.size() - 1; i >= 0; i--) {
                    DeviceSharer deviceSharer = deviceSharerList.get(i);
                    if (deviceSharer.getUserId().equals(userId)) {
                        redisService.removeListValue(redisShareKey, deviceSharer);
                    }
                }
            }
        }
        //删除数据库
        DeviceSharer record = new DeviceSharer();
        record.setDeviceCid(cid);
        record.setUserId(userId);
        record.setUpdateTime(new Date());
        record.setIsDeleted(true);
        deviceSharerMapper.updateByUserIdAndCid(record);
    }

    @Override
    public SharePeopleResponse querySharePeople(SharePeopleRequest request) {
        log.info("查询共享者列表, {}", JSON.toJSONString(request));
        SharePeopleResponse response = new SharePeopleResponse();
        List<String> userIdList = queryDeviceSharerListByUuid(request.getUuid());
        if (userIdList.size()==0){
            return response;
        }
        //调用接口, 查询用户信息
        List<UserInfo> sharedPeopleList = commonUserService.getUserInfoByHttp(request.getAccountId(),userIdList);
        response.setSharedPeople(sharedPeopleList);
        return response;
    }


    @Override
    public SharePeopleResponse querySharePeopleHistory(ShareHistoryRequest request) {
        log.info("查询历史共享者列表, {}", JSON.toJSONString(request));
        int num = request.getNum();
        SharePeopleResponse response = new SharePeopleResponse();
        List<String> userIdList = queryDeviceSharerHistoryByUuid(request.getUuid());
        if (userIdList.isEmpty()){
            return response;
        }

        if (num > userIdList.size()) {
            num = userIdList.size();
        }
        userIdList.subList(0, num);
        //调用接口, 查询用户信息
        List<UserInfo> sharedPeopleList = commonUserService.getUserInfoByHttp(request.getAccountId(),userIdList);
        response.setSharedPeople(sharedPeopleList);
        return response;
    }

    @Override
    @Transactional
    public VBMPResponse deleteSharePeopleHistory(SharePeopleRequest request) {
        log.info("删除历史共享者列表, {}", JSON.toJSONString(request));
        SharePeopleResponse response = new SharePeopleResponse();
        String uuid = request.getUuid();
        deleteSharePeopleHistoryByUuid(uuid);

        return response;
    }

    private void deleteSharePeopleHistoryByUuid(String uuid) {
        //查询设备cid
        DeviceInfo deviceInfo = deviceService.queryDeviceByUuid(uuid);
        if (deviceInfo == null || MyStringUtils.isNullData(deviceInfo.getDeviceCid())) {
            throw new ServiceException(ErrorConstant.ERR_DEVICE_NOT_EXIST, ErrorConstant.ERR_DEVICE_NOT_EXIST_MSG);
        }
        String cid = deviceInfo.getDeviceCid();

        //删除redis
        String redisKey = CommonConstant.REDIS_KEY_DEVICE_SHARE_HISTORY.concat(CommonConstant.COLON_STRING).concat(uuid);
        if (redisService.exists(redisKey)) {
            redisService.remove(redisKey);
        }
        //数据库is history false
        DeviceSharer record = new DeviceSharer();
        record.setIsHistory(false);
        record.setDeviceCid(cid);
        deviceSharerMapper.updateByUserIdAndCid(record);
        //删除分享者已删除但是历史分享者未删除的数据 is history false & is deleted true
        record.setIsDeleted(true);
        deviceSharerMapper.delete(record);
    }

    @Override
    public DeviceSharer queryDeviceSharerByUuidAndUserId(String uuid, String userId) {
        //查询设备cid
        DeviceInfo deviceInfo = deviceService.queryDeviceByUuid(uuid);
        if (deviceInfo == null || MyStringUtils.isNullData(deviceInfo.getDeviceCid())) {
            throw new ServiceException(ErrorConstant.ERR_DEVICE_NOT_EXIST, ErrorConstant.ERR_DEVICE_NOT_EXIST_MSG);
        }
        String cid = deviceInfo.getDeviceCid();
        List<DeviceSharer> deviceSharerList = null;
        DeviceSharer deviceSharer = null;
        //查询redis
        String redisKey = CommonConstant.REDIS_KEY_DEVICE_SHARE_HISTORY.concat(CommonConstant.COLON_STRING).concat(uuid);
        if (redisService.exists(redisKey)) {
            deviceSharerList = redisService.getList(redisKey);
            for (int i = 0; i < deviceSharerList.size(); i++) {
                deviceSharer = deviceSharerList.get(0);
                if (userId.equals(deviceSharer.getUserId())) {
                    return deviceSharer;
                }
            }
        } else {
            DeviceSharer record = new DeviceSharer();
            record.setDeviceCid(cid);
            record.setIsDeleted(false);
            record.setUserId(userId);
            deviceSharer = deviceSharerMapper.selectOne(record);
        }
        return deviceSharer;
    }

    @Override
    public List<DeviceSharer> queryDeviceShareListByUserId(String userId) {
        log.info("queryDeviceShareListByUserId, {}", userId);
        String redisKey = CommonConstant.REDIS_KEY_DEVICE_SHARE_USER.concat(CommonConstant.COLON_STRING).concat(userId);
        List<DeviceSharer> deviceSharerList = null;
        if (redisService.exists(redisKey)) {
            deviceSharerList = redisService.getList(redisKey);
        } else {
            DeviceSharer record = new DeviceSharer();
            record.setUserId(userId);
            record.setIsDeleted(false);
            deviceSharerList = deviceSharerMapper.select(record);
            //添加至redis
            redisService.addList(redisKey, deviceSharerList);
        }
        return deviceSharerList;
    }

    @Override
    public void deleteSharePeopleAllByUuid(String uuid) {
        log.info("删除共享者和历史共享者列表, {}", uuid);
        //删除共享者
        deleteSharePeopleByUuidAndUserId(null, uuid);
        //删除历史共享者
        deleteSharePeopleHistoryByUuid(uuid);
    }

    private List<String> queryDeviceSharerHistoryByUuid(String uuid) {
        //查询设备cid
        DeviceInfo deviceInfo = deviceService.queryDeviceByUuid(uuid);
        if (deviceInfo == null || MyStringUtils.isNullData(deviceInfo.getDeviceCid())) {
            throw new ServiceException(ErrorConstant.ERR_DEVICE_NOT_EXIST, ErrorConstant.ERR_DEVICE_NOT_EXIST_MSG);
        }
        String cid = deviceInfo.getDeviceCid();
        //查询redis
        String redisKey = CommonConstant.REDIS_KEY_DEVICE_SHARE_HISTORY.concat(CommonConstant.COLON_STRING).concat(uuid);
        List<DeviceSharer> deviceSharerList = null;
        if (redisService.exists(redisKey)) {
            deviceSharerList = redisService.getList(redisKey);
        } else {
            DeviceSharer record = new DeviceSharer();
            record.setDeviceCid(cid);
            record.setIsHistory(true);
            deviceSharerList = deviceSharerMapper.select(record);
            //添加至redis
            redisService.addList(redisKey, deviceSharerList);
        }
        List<String> userIdList = new ArrayList<>();
        if (!deviceSharerList.isEmpty()) {
            deviceSharerList.sort(Comparator.comparing(DeviceSharer::getUpdateTime).reversed());
            //封装参数
            for (DeviceSharer deviceSharer : deviceSharerList) {
                if (StringUtils.hasText(deviceSharer.getUserId())) {
                    userIdList.add(deviceSharer.getUserId());
                }
            }
        }
        return userIdList;
    }

    @Override
    public List<String> queryDeviceSharerListByUuid(String uuid) {
        //查询设备cid
        DeviceInfo deviceInfo = deviceService.queryDeviceByUuid(uuid);
        if (deviceInfo == null || MyStringUtils.isNullData(deviceInfo.getDeviceCid())) {
            throw new ServiceException(ErrorConstant.ERR_DEVICE_NOT_EXIST, ErrorConstant.ERR_DEVICE_NOT_EXIST_MSG);
        }
        String cid = deviceInfo.getDeviceCid();
        //查询redis
        String redisKey = CommonConstant.REDIS_KEY_DEVICE_SHARE.concat(CommonConstant.COLON_STRING).concat(uuid);
        List<DeviceSharer> deviceSharerList = null;
        Boolean bool = redisService.exists(redisKey);
        if (redisService.exists(redisKey)) {
            deviceSharerList = redisService.getList(redisKey);
        } else {
            DeviceSharer record = new DeviceSharer();
            record.setDeviceCid(cid);
            record.setIsDeleted(false);
            deviceSharerList = deviceSharerMapper.select(record);
            //添加至redis
            redisService.addList(redisKey, deviceSharerList);
        }
        List<String> userIdList = new ArrayList<>();
        if (!deviceSharerList.isEmpty()) {
            deviceSharerList.sort(Comparator.comparing(DeviceSharer::getUpdateTime).reversed());
            //封装参数
            for (DeviceSharer deviceSharer : deviceSharerList) {
                if (StringUtils.hasText(deviceSharer.getUserId())) {
                    userIdList.add(deviceSharer.getUserId());
                }
            }
        }
        return userIdList;
    }

    @Override
    public VBMPResponse deleteShareDataByUuid(String uuid) {
        VBMPResponse response = new VBMPResponse();
        try {
            //查询设备cid
            DeviceInfo deviceInfo = deviceService.queryDeviceByUuid(uuid);
            if (deviceInfo == null || MyStringUtils.isNullData(deviceInfo.getDeviceCid())) {
                throw new ServiceException(ErrorConstant.ERR_DEVICE_NOT_EXIST, ErrorConstant.ERR_DEVICE_NOT_EXIST_MSG);
            }
            String cid = deviceInfo.getDeviceCid();
            //删除redis
            String redisKey = CommonConstant.REDIS_KEY_DEVICE_SHARE.concat(CommonConstant.COLON_STRING).concat(uuid);
            String historyKey = CommonConstant.REDIS_KEY_DEVICE_SHARE_HISTORY.concat(CommonConstant.COLON_STRING).concat(uuid);
            redisService.remove(redisKey);
            redisService.remove(historyKey);
            //删除数据库
            DeviceSharer deviceSharer = new DeviceSharer();
            deviceSharer.setDeviceCid(cid);
            deviceSharerMapper.deleteByExample(deviceSharer);
        }catch (Exception e){
            response.setCode(-1);
            log.error("delete share data failure");
        }
        return response;
    }
}
