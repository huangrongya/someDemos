package com.etekcity.vbmp.common.comm.service.impl;

import java.util.*;

import com.etekcity.vbmp.common.config.VBMPRequest;
import com.etekcity.vbmp.common.comm.dao.model.DeviceInfo;
import com.etekcity.vbmp.common.comm.dto.*;
import com.etekcity.vbmp.common.comm.dto.inner.SupportedModel;
import com.etekcity.vbmp.common.comm.dto.inner.SupportedTypeModel;
import com.etekcity.vbmp.common.comm.service.DeviceService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.util.Assert;
import org.springframework.util.StringUtils;

import com.alibaba.fastjson.JSON;
import com.alibaba.fastjson.JSONObject;
import com.etekcity.vbmp.common.config.redis.RedisService;
import com.etekcity.vbmp.common.constant.CommonConstant;
import com.etekcity.vbmp.common.constant.OldRedisConstant;
import com.etekcity.vbmp.common.comm.dao.mapper.DeviceTypeMapper;
import com.etekcity.vbmp.common.comm.dao.model.DeviceType;
import com.etekcity.vbmp.common.comm.dto.inner.SupportedType;
import com.etekcity.vbmp.common.comm.service.DeviceTypeService;
import com.etekcity.vbmp.common.utils.MyBeanUtils;

import lombok.extern.slf4j.Slf4j;

@Service
@Slf4j
public class DeviceTypeServiceImpl implements DeviceTypeService {

    @Autowired
    RedisService redisService;
    @Autowired
    private DeviceTypeMapper deviceTypeMapper;
    @Autowired
    private CommonUserServiceImpl commonUserService;
    @Autowired
    private DeviceService deviceService;

    /**
     * 获取数据库支持的设备型号
     *
     * @param supportedModelsResponse
     * @param type
     */
    @Override
    public void querySupportedmodels(SupportedModelsResponse supportedModelsResponse,  VBMPRequest requestBaseInfo) {
        //获取支持的设备类型
        log.info("获取数据库支持的设备型号");
        List<SupportedTypeModel> supportedTypeModels = getAllSupportModel(requestBaseInfo);
        //获取多语言信息
        JSONObject jsonObject = commonUserService.getUserLangInfo(requestBaseInfo, Arrays.asList(OldRedisConstant.DEVICE_TYPE_MODEL_NAME));
        String typeModelNameJson = "";
        Map<String, String> typeModelNameMap = new HashMap<>();
        if (jsonObject != null) {
            typeModelNameJson = jsonObject.getString(OldRedisConstant.DEVICE_TYPE_MODEL_NAME);
            if (StringUtils.hasText(typeModelNameJson)) {
                typeModelNameMap = JSONObject.parseObject(typeModelNameJson, Map.class);
            }
        }

        if (typeModelNameMap == null || typeModelNameMap.isEmpty()) {
            supportedModelsResponse.setSupportedTypeModels(supportedTypeModels);
            return;
        }
        for (int i = 0, size = supportedTypeModels.size(); i < size; i++) {
            SupportedTypeModel supportedTypeModel = supportedTypeModels.get(i);
            List<SupportedModel> supportedModels = supportedTypeModel.getSupportedModels();
            for (int j = 0, sizeModel = supportedModels.size(); j < sizeModel; j++) {
                SupportedModel supportedModel = supportedModels.get(j);
                String modelName = typeModelNameMap.get(supportedModel.getModel());
                if (StringUtils.hasLength(modelName)) {
                    supportedModel.setModelName(modelName);
                }
            }
        }
        supportedModelsResponse.setSupportedTypeModels(supportedTypeModels);
    }

    /**
     * @param requestBaseInfo
     * @return
     */
    private List<SupportedTypeModel> getAllSupportModel(VBMPRequest requestBaseInfo) {
        List<SupportedTypeModel> supportedTypeModelList = new ArrayList<>();
        //获取supportedTypes信息
        SupportedTypesResponse supportedTypesResponse = new SupportedTypesResponse();
        List<SupportedType> supportedTypes = new ArrayList<>();

        //判断redis这个键值version
        String redisDeviceSupportVersionKey = CommonConstant.REDIS_KEY_COMMON_FUNC_PREFIX.concat(":type_model_change");
        Integer changeValue = (Integer) redisService.get(redisDeviceSupportVersionKey);
        if (changeValue == null) {
            changeValue = 0;
        }
        boolean updateForce = changeValue > 0;
        //重新设值
        redisService.set(redisDeviceSupportVersionKey, 0);

        //查询获取type
        querySupportedtypes(supportedTypesResponse, requestBaseInfo, updateForce);
        if (supportedTypesResponse.getCode() == CommonConstant.COMMON_SUCCESS) {
            supportedTypes.addAll(supportedTypesResponse.getSupportedTypes());
        }
        //根据supportedTypes信息设置model信息
        for (int i = 0, size = supportedTypes.size(); i < size; i++) {
            SupportedTypeModel supportedTypeModel = new SupportedTypeModel();
            SupportedType supportedType = supportedTypes.get(i);
            //复制supportedType信息
            MyBeanUtils.copyPropertiesIgnoreCase(supportedType, supportedTypeModel);
            //查询大类对应的小类
            List<SupportedModel> typeSupportModels = getSupportModelByType(supportedType.getType(), updateForce);
            if (typeSupportModels != null) {
                supportedTypeModel.setSupportedModels(typeSupportModels);
            }
            supportedTypeModelList.add(supportedTypeModel);
        }
        return supportedTypeModelList;
    }


    /**
     * @return java.util.List<com.etekcity.vbmp.common.comm.dto.inner.SupportedModel>
     * @Author Ericer
     * @Description 根据设备大类查询所有小类
     * @Date 上午10:53 18-9-20
     * @Param [type]
     **/
    private List<SupportedModel> getSupportModelByType(String type, boolean updateForce) {
        List<SupportedModel> supportedModels = new ArrayList<>(8);
        Assert.hasText(type, "getSupportModelByType type 不能为空");
        //获取redis设备小类
        String redisDeviceModelKey = CommonConstant.REDIS_KEY_COMMON_FUNC_PREFIX.concat(":supportedmodels-").concat(type);
        if (!updateForce && redisService.exists(redisDeviceModelKey)) {
            Map<String, String> deviceModelMap = redisService.getMap(redisDeviceModelKey);
            if (deviceModelMap == null) {
                deviceModelMap = new HashMap<>(1);
            }
            for (String key : deviceModelMap.keySet()) {
                SupportedModel supportedModel = JSON.parseObject(deviceModelMap.get(key), SupportedModel.class);
                supportedModels.add(supportedModel);
            }
        } else {
            //数据库查询
            Map<String, String> deviceModelMap = new HashMap<>();
            List<DeviceType> deviceTypeList = findDeviceTypeByType(type);
            if (deviceTypeList == null) {
                deviceTypeList = new ArrayList<>();
            }
            for (DeviceType deviceType : deviceTypeList) {
                //复制信息返回
                SupportedModel supportedModel = new SupportedModel();
                MyBeanUtils.copyPropertiesIgnoreCase(deviceType, supportedModel);
                supportedModels.add(supportedModel);
                deviceModelMap.put(deviceType.getConfigModel(), JSON.toJSONString(deviceType));
            }
            redisService.addMap(redisDeviceModelKey, deviceModelMap);
        }
        return supportedModels;
    }


    /**
     * 获取支持的设备
     *
     * @param supportedTypesResponse
     * @return
     */
    public void querySupportedtypes(SupportedTypesResponse supportedTypesResponse, VBMPRequest requestBaseInfo, boolean updateForce) {

        //查询设备大类
        List<SupportedType> supportedTypeList = getAllSupportType(updateForce);

        //获取多语言信息
        JSONObject jsonObject = commonUserService.getUserLangInfo(requestBaseInfo, Arrays.asList(OldRedisConstant.DEVICE_TYPE_NAME));
        String typeNameJson = "";
        Map<String, String> typeNameMap = new HashMap<>();
        if (jsonObject != null) {
            typeNameJson = jsonObject.getString(OldRedisConstant.DEVICE_TYPE_NAME);
            if (StringUtils.hasText(typeNameJson)) {
                typeNameMap = JSONObject.parseObject(typeNameJson, Map.class);
            }
        }

        if (typeNameMap == null || typeNameMap.isEmpty()) {
            supportedTypesResponse.setSupportedTypes(supportedTypeList);
            return;
        }
        for (int i = 0, size = supportedTypeList.size(); i < size; i++) {
            SupportedType supportedType = supportedTypeList.get(i);
            String typeName = typeNameMap.get(supportedType.getType());
            if (StringUtils.hasLength(typeName)) {
                supportedType.setTypeName(typeName);
            }
        }

        supportedTypesResponse.setSupportedTypes(supportedTypeList);
    }

    /**
     * 获取设备大类
     *
     * @param updateForce
     * @return
     */
    public List<SupportedType> getAllSupportType(boolean updateForce) {
        //获取支持的设备类型
        List<SupportedType> supportedTypes = new ArrayList<>(4);
        //redis中查找
        String redisDeviceSupportKey = CommonConstant.REDIS_KEY_COMMON_FUNC_PREFIX.concat(":supportedtypes");
        if (!updateForce && redisService.exists(redisDeviceSupportKey)) {
            Map<String, String> deviceSupportMap = redisService.getMap(redisDeviceSupportKey);
            if (deviceSupportMap == null) {
                deviceSupportMap = new HashMap<>(0);
            }
            for (String key : deviceSupportMap.keySet()) {
                SupportedType supportedType = JSON.parseObject(deviceSupportMap.get(key), SupportedType.class);
                supportedTypes.add(supportedType);
            }
        } else {
            //查询数据库
            Map<String, String> deviceTypeMap = new HashMap<>();
            List<DeviceType> deviceTypeList = queryTypes();
            if (deviceTypeList != null && !deviceTypeList.isEmpty()) {
                for (int i = 0, size = deviceTypeList.size(); i < size; i++) {
                    SupportedType supportedType = new SupportedType();
                    MyBeanUtils.copyPropertiesIgnoreCase(deviceTypeList.get(i), supportedType);
                    supportedTypes.add(supportedType);
                    deviceTypeMap.put(supportedType.getType(), JSON.toJSONString(supportedType));
                }
                redisService.addMap(redisDeviceSupportKey, deviceTypeMap);
            }
        }
        return supportedTypes;
    }


    @Override
    public DeviceType getDeviceTypeByConfigModel(String configModel) {
            DeviceType dt = new DeviceType();
            dt.setConfigModel(configModel);
        return deviceTypeMapper.selectOne(dt);
    }

    @Override
    public DeviceType getDeviceType(String model) {
        Assert.hasLength(model, "getDeviceType方法model参数不能为空");
        log.info("获取设备类型对应modelname");
        String redisDeviceSupportKey = CommonConstant.REDIS_KEY_COMMON_FUNC_PREFIX.concat(":supportedtypes");
        List<SupportedType> supportedTypes = new ArrayList<>();
        if (redisService.exists(redisDeviceSupportKey)) {
            Map<String, String> deviceSupportMap = redisService.getMap(redisDeviceSupportKey);
            for (String key : deviceSupportMap.keySet()) {
                SupportedType supportedType = JSON.parseObject(deviceSupportMap.get(key), SupportedType.class);
                supportedTypes.add(supportedType);
            }
        }
        DeviceType deviceType = new DeviceType();
        for (SupportedType supportedType : supportedTypes) {
            String redisDeviceModelKey = CommonConstant.REDIS_KEY_COMMON_FUNC_PREFIX.concat(":supportedmodels-").concat(supportedType.getType());
            if (redisService.exists(redisDeviceModelKey)) {
                Map<String, String> deviceModelMap = redisService.getMap(redisDeviceModelKey);
                for (String key : deviceModelMap.keySet()) {
                    String value = deviceModelMap.get(key);
                    deviceType = JSONObject.parseObject(value, DeviceType.class);
                    if (deviceType != null && model.equals(deviceType.getModel())) {
                        return deviceType;
                    }
                }
            }
        }
        return null;
    }

    @Override
    public DeviceType getDeviceTypeByUuid(String uuid) {
        DeviceInfo deviceInfo = deviceService.queryDeviceByUuid(uuid);
        if (deviceInfo == null || deviceInfo.getId() == null) {
            log.error("uuid:{}对应的设备不存在", uuid);
            return null;
        }
        return getDeviceType(deviceInfo.getDeviceType());
    }

    @Override
    public List<DeviceType> findDeviceTypeByType(String type) {
        DeviceType dt = new DeviceType();
        dt.setType(type);
        return deviceTypeMapper.select(dt);
    }

    @Override
    public List<DeviceType> queryTypes() {
        return deviceTypeMapper.selectAll();
    }


}
