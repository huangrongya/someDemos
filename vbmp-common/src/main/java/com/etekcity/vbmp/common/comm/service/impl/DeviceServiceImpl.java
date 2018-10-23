package com.etekcity.vbmp.common.comm.service.impl;

import java.nio.charset.Charset;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Iterator;
import java.util.List;
import java.util.Map;

import com.etekcity.vbmp.common.comm.dto.*;
import com.etekcity.vbmp.common.exception.ServiceException;
import com.etekcity.vbmp.common.router.service.DeviceControlService;
import com.etekcity.vbmp.common.comm.service.*;
import com.etekcity.vbmp.common.utils.MyStringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.BeanUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.Assert;
import org.springframework.util.StringUtils;

import com.alibaba.fastjson.JSON;
import com.alibaba.fastjson.JSONArray;
import com.alibaba.fastjson.JSONObject;
import com.etekcity.vbmp.common.config.VBMPRequest;
import com.etekcity.vbmp.common.config.VBMPResponse;
import com.etekcity.vbmp.common.config.redis.RedisService;
import com.etekcity.vbmp.common.constant.CommonConstant;
import com.etekcity.vbmp.common.constant.ErrorConstant;
import com.etekcity.vbmp.common.constant.OldRedisConstant;
import com.etekcity.vbmp.common.comm.dao.mapper.DeviceMapper;
import com.etekcity.vbmp.common.comm.dao.model.DeviceInfo;
import com.etekcity.vbmp.common.comm.dao.model.DeviceSharer;
import com.etekcity.vbmp.common.comm.dao.model.DeviceType;
import com.etekcity.vbmp.common.comm.dao.model.ModuleWifiOutlet;
import com.etekcity.vbmp.common.comm.dto.inner.*;
import com.etekcity.vbmp.common.comm.service.DeviceService;
import com.etekcity.vbmp.common.comm.service.ModuleWifiOutletService;
import com.etekcity.vbmp.common.comm.service.ShareService;
import com.etekcity.vbmp.common.comm.dto.inner.Device;
import com.etekcity.vbmp.common.comm.dto.inner.DeviceOld;
import com.etekcity.vbmp.common.utils.MyBeanUtils;
import com.etekcity.vbmp.common.utils.MyJsonUtils;
import lombok.extern.slf4j.Slf4j;

@Service
@Slf4j
public class DeviceServiceImpl implements DeviceService {

    private static final Logger logger = LoggerFactory.getLogger(DeviceServiceImpl.class);

    @Autowired
    private DeviceMapper deviceMapper;

    @Autowired
    private DeviceTypeServiceImpl deviceTypeService;

    @Autowired
    private ShareService shareService;

    @Autowired
    private RedisService redisService;

    @Autowired
    private HttpClientService httpClientService;

    @Autowired
    private ModuleWifiOutletService wifiOutletService;
    @Autowired
    private CommonUserServiceImpl commonUserService;
    @Autowired
    private DeviceControlService deviceControlService;
    @Autowired
    private SubTableModelService subTableModelService;
    @Autowired
    private DeviceTimingService deviceTimingService;

    @Value("${outlet7a.domin.url}")
    private String outlet7AAddress;
    @Value("${device15a.type}")
    private String device15aType;

    @Override
    public GetDeviceDynamicInfoResponse getDeviceName(String deviceType, GetDeviceDynamicInfoRequest getDeviceDynamicInfoRequest) {
        GetDeviceDynamicInfoResponse getDeviceDynamicInfoResponse = new GetDeviceDynamicInfoResponse();
        Assert.hasLength(deviceType, "getDeviceName方法accountID参数不能为空");
        UserLangInfoRequest requestBaseInfo = new UserLangInfoRequest();
        BeanUtils.copyProperties(getDeviceDynamicInfoRequest, requestBaseInfo);

        JSONObject jsonObject = commonUserService.getUserLangInfo(requestBaseInfo, Arrays.asList(OldRedisConstant.DEVICE_TYPE_MODEL_NAME));
        String typeModelNameJson = "";
        if (jsonObject != null) {
            typeModelNameJson = jsonObject.getString(OldRedisConstant.DEVICE_TYPE_MODEL_NAME);
        }
        String deviceName = null;
        if (StringUtils.hasLength(typeModelNameJson)) {
            Map<String, String> typeModelNameMap = JSONObject.parseObject(typeModelNameJson, Map.class);
            deviceName = typeModelNameMap.get(deviceType);
        }

        //为找到，从数据库devicetype查找
        if (!StringUtils.hasLength(deviceName)) {
            //获取设备名称 deviceType 例如：wifi-switch-1.3
            DeviceType deviceTypeTable = deviceTypeService.getDeviceType(deviceType);
            if (deviceTypeTable == null) {
                getDeviceDynamicInfoResponse.setCode(ErrorConstant.ERR_DATABASE);
                getDeviceDynamicInfoResponse.setMsg(ErrorConstant.ERR_DATABASE_MSG);
                return getDeviceDynamicInfoResponse;
            }
            deviceName = deviceTypeTable.getModelName();
        }
        //获取当前用户+设备类型对应的value值
        log.info("获取当前用户+设备类型对应的value值");
        int num = 0;
        String configModel = getDeviceDynamicInfoRequest.getConfigModel();
        String redisAccountDeviceKey = configModel + ":".concat(CommonConstant.REDIS_KEY_DEVICE_PREFIX.concat("count:")).concat(getDeviceDynamicInfoRequest.getAccountId()).concat("-").concat(deviceType);
        if (redisService.exists(redisAccountDeviceKey)) {
            num = (Integer) redisService.get(redisAccountDeviceKey);
        }
        num++;
        redisService.set(redisAccountDeviceKey, num);
        //默认设备名称= 设备名称 + 序号
        deviceName = deviceName.concat(CommonConstant.BLANK_STRING).concat(String.valueOf(num));
        getDeviceDynamicInfoResponse.setDeviceName(deviceName);
        return getDeviceDynamicInfoResponse;

    }

    @Override
    public DeviceInfo queryDeviceByUuid(String uuid) {
        log.info("请求查询deviceInfo:{}", uuid);
        Assert.hasLength(uuid, "queryDeviceNoCheckByUuid方法参数uuid不能为空");
        String redisUuidDKey = CommonConstant.REDIS_KEY_DEVICE_UUID.concat(uuid);
        String objectField = CommonConstant.UUID_DEVICE_OBJECT;
        Object value;
        String wifiOutlet15DeviceStr;
        DeviceInfo deviceInfo;
        if ((value = redisService.getMapField(redisUuidDKey, objectField)) != null) {
            wifiOutlet15DeviceStr = String.valueOf(value);
            deviceInfo = JSON.parseObject(wifiOutlet15DeviceStr, DeviceInfo.class);
        } else {
            log.info("获取数据库Device信息");
            //根据uuid查找设备信息
            DeviceInfo record = new DeviceInfo();
            record.setUuid(uuid);
            deviceInfo = deviceMapper.selectOne(record);
            if (deviceInfo != null && deviceInfo.getId() != null) {
                redisService.addMap(redisUuidDKey, objectField, JSON.toJSONString(deviceInfo), CommonConstant.SECONDS_OF_ONEDAY);
            }
        }
        return deviceInfo;
    }

    @Override
    public VBMPResponse updateFirmware(String uuid, String accountId) {
        VBMPResponse response = new VBMPResponse();
        // 判断设备信息是否存在
        DeviceInfo deviceInfo = queryDeviceByUuid(uuid);
        if (deviceInfo == null || deviceInfo.getId() == null) {
            response.setCode(ErrorConstant.ERR_DEVICE_NOT_EXIST);
            response.setMsg(ErrorConstant.ERR_DEVICE_NOT_EXIST_MSG);
            log.info("设备信息不存在");
            return response;
        }
        // 判断设备是否属于或分享给该用户
        List<DeviceInfo> deviceInfoList = getAccountDevice(accountId);
        deviceInfoList.removeIf(di -> !uuid.equals(di.getUuid()));
        if (deviceInfoList == null || deviceInfoList.isEmpty()) {
            response.setCode(ErrorConstant.ERR_USER_DONOT_OWN_DEVICE);
            response.setMsg(ErrorConstant.ERR_USER_DONOT_OWN_DEVICE_MSG);
            log.info("该账户没有该设备的权限");
            return response;
        }
        // queryConfigurations获取版本信息
        ConfigurationsResponse configurationsResponse = new ConfigurationsResponse();
        configurationsResponse = queryConfigurations(uuid, accountId, configurationsResponse);
        if (configurationsResponse.getCode() != CommonConstant.COMMON_SUCCESS) {
            response.setCode(configurationsResponse.getCode());
            response.setMsg(configurationsResponse.getMsg());
            return response;
        }
        // 检测版本信息
        if (configurationsResponse.getCurrentFirmVersion().equals(configurationsResponse.getLatestFirmVersion())) {
            response.setCode(ErrorConstant.ERR_LATEST_FIRMWARE_VERSION);
            response.setMsg(ErrorConstant.ERR_LATEST_FIRMWARE_VERSION_MSG);
            return response;
        }
        // 检测在线状态
        JSONObject jsonObject = httpClientService.request(new String[]{uuid}, uuid, "getDeviceStatus");
        Map<String, Object> responseMap = MyJsonUtils.coverDeviceStatusToMap(jsonObject.getJSONObject("jsonStr"));
        if (!CommonConstant.COMMON_CONNECTION_STATUS_ONLINE.equals(responseMap.get("connectionStatus"))) {
            log.info("{}设备离线", uuid);
            response.setCode(ErrorConstant.ERR_CONTROLLER_OFFLINE);
            response.setMsg(ErrorConstant.ERR_CONTROLLER_OFFLINE_MSG);
            return response;
        }
        // 更新固件
        httpClientService.updateDeviceFirmware(uuid, configurationsResponse.getLatestFirmVersion(), deviceInfo.getDeviceType());
        return response;
    }

    @Override
    public VBMPResponse getFirmwareStatus(String accountId, String uuid) {
        FirmwareStatusResponse response = new FirmwareStatusResponse();
        // 检查用户是否存在且是否为有设备或被分享
        List<DeviceInfo> deviceInfoList = getAccountDevice(accountId);
        deviceInfoList.removeIf(di -> !uuid.equals(di.getUuid()));
        if (deviceInfoList == null || deviceInfoList.isEmpty()) {
            response.setCode(ErrorConstant.ERR_USER_DONOT_OWN_DEVICE);
            response.setMsg(ErrorConstant.ERR_USER_DONOT_OWN_DEVICE_MSG);
            log.info("该账户没有该设备的权限");
            return response;
        }
        DeviceInfo deviceInfo = queryDeviceByUuid(uuid);
        // 根据model查找设备硬件版本信息
        String model = deviceInfo.getDeviceType();
        Assert.hasLength(model, "deviceInfo uuid 对应Devicetype栏信息为空");
        DeviceType deviceTypeTable = deviceTypeService.getDeviceType(model);
        Assert.notNull(deviceTypeTable, "devicetype对应的设备类型在DeviceTypeTable为空");
        //设置最新版本号
        response.setLatestFirmVersion(deviceTypeTable.getLatestFirmVersion());

        //获取设备状态
        JSONObject jSONObjectStatus = httpClientService.request(new String[]{uuid}, uuid, "getDeviceStatus");
        Map<String, Object> responseMap = MyJsonUtils.coverDeviceStatusToMap(jSONObjectStatus.getJSONObject("jsonStr"));
        String currentfirmversion = (String) responseMap.get("currentfirmversion");
        response.setCurrentFirmVersion(currentfirmversion);

        JSONObject jsonObjectFirmware = (JSONObject) responseMap.get("firmware");
        if (jsonObjectFirmware != null && jsonObjectFirmware.containsKey("status")) {
            // 0 升级成功 1 升级失败 2 升级启动失败 3 DNS解析失败 4 域名格式错误 5 正在升级中
            Integer firmWareStatus = jsonObjectFirmware.getInteger("status");
            response.setUpdateSuccess(firmWareStatus);
            // 升级成功更新版本数据
            if (firmWareStatus == CommonConstant.COMMON_SUCCESS) {
                httpClientService.updateFirmwareVersion(uuid, currentfirmversion);
            }
        } else {
            response.setCode(ErrorConstant.ERR_VDMP_REQUEST_FORMAT);
            response.setMsg(ErrorConstant.ERR_VDMP_REQUEST_FORMAT_MSG);
        }
        return response;
    }

    @Override
    public DevicesResponse getDevices(VBMPRequest request) {
        DevicesResponse response = new DevicesResponse();
        // 本地库中的设备列表
        List<DeviceInfo> deviceInfoList = getAccountDevice(request.getAccountId());
        List<Device> deviceList = new ArrayList<>();
        // 整合查询设备列表
        for (DeviceInfo deviceInfo : deviceInfoList) {
            Device device = new Device();
            device.setCid(deviceInfo.getDeviceCid());
            device.setConnectionType(deviceInfo.getDeviceConnectionType());
            device.setDeviceImg(deviceInfo.getDeviceImg());
            device.setDeviceName(deviceInfo.getDeviceName());
            device.setDeviceType(deviceInfo.getDeviceType());
            device.setType(deviceInfo.getType());
            device.setUuid(deviceInfo.getUuid());
            device.setConnectionStatus(CommonConstant.COMMON_CONNECTION_STATUS_OFFLINE); // 默认离线
            device.setDeviceStatus(CommonConstant.COMMON_STATUS_OFF); // 默认关
            // 查询设备状态
            try {
                JSONObject jsonObject = httpClientService.request(new String[]{deviceInfo.getUuid()}, deviceInfo.getUuid(), "getDeviceStatus");
                Map<String, Object> responseMap = MyJsonUtils.coverDeviceStatusToMap(jsonObject);
                if (responseMap.get("deviceStatus") != null) {
                    device.setDeviceStatus((String) responseMap.get("deviceStatus"));
                }
                if (responseMap.get("connectionStatus") != null) {
                    device.setConnectionStatus((String) responseMap.get("connectionStatus"));
                }
            } catch (Exception e) {
                log.error("查询设备:{}状态失败", deviceInfo.getUuid(), e);
            }
            // 获取configModel
            DeviceType deviceType = deviceTypeService.getDeviceType(device.getDeviceType());
            if (deviceType != null) {
                device.setConfigModule(deviceType.getConfigModel());
            }
            deviceList.add(device);
        }
        // 查询7A设备将其合并至deviceList
        try {
            HttpHeaders requestHeaders = new HttpHeaders();
            requestHeaders.set("Content-Type", MediaType.APPLICATION_JSON_UTF8_VALUE);
            requestHeaders.setAcceptCharset(Arrays.asList(Charset.forName("utf-8")));
            requestHeaders.set("tk", request.getToken());
            requestHeaders.set("tz", request.getTimeZone());
            requestHeaders.set("accountID", request.getAccountId());
            HttpEntity<String> requestEntity = new HttpEntity<>("", requestHeaders);

            String responseJsonStr = httpClientService.commonRequestUtf8(outlet7AAddress.concat("/v1/thirdparty/user/devices"), HttpMethod.GET, requestEntity, String.class);
            List<Device> deviceList7A = null;
            List<DeviceOld> deviceOldList = null;
            try {
                deviceOldList = JSONObject.parseArray(responseJsonStr, DeviceOld.class);
                deviceList7A = JSONObject.parseArray(responseJsonStr, Device.class);
            } catch (Exception e) {
                log.info("请求获取7A设备列表错误".concat(responseJsonStr), e);
            }
            if (deviceOldList == null) {
                deviceOldList = new ArrayList<>();
                deviceList7A = new ArrayList<>();
            }
            //字段赋值
            for (int i = 0, size = deviceOldList.size(); i < size; i++) {
                Device device = deviceList7A.get(i);
                device.setType(deviceOldList.get(i).getModel());
                // 获取configModel
                DeviceType deviceTypeTable = deviceTypeService.getDeviceType(device.getDeviceType());
                if (deviceTypeTable != null) {
                    device.setConfigModule(deviceTypeTable.getConfigModel());
                }
                deviceList.add(device);
            }
        } catch (Exception e) {
            log.error("获得7A设备列表出错", e);
        }
        response.setDevices(deviceList);
        return response;
    }

    @Override
    public DevicesGoogleHomeResponse getDevicesGoogleHome(VBMPRequest request) {
        DevicesGoogleHomeResponse googleHomeResponse = new DevicesGoogleHomeResponse();
        List<DeviceGoogleHome> deviceGoogleHomes = new ArrayList<>();
        DevicesResponse response = getDevices(request);
        Iterator<Device> iterator = response.getDevices().iterator();
        while (iterator.hasNext()) {
            Device device = iterator.next();
            //获取小夜灯
            if (device.getDeviceType().equals(device15aType)) {
                ModuleWifiOutlet wifiOutlet15a = wifiOutletService.getWifiOutlet(device.getUuid());
                device.setCid(device.getCid().concat(CommonConstant.GOOGLE_HOME_CID_NIGHT_LIGHTT));
                if (StringUtils.hasText(wifiOutlet15a.getNightLightName())) {
                    device.setDeviceName(wifiOutlet15a.getNightLightName());
                } else {
                    device.setDeviceName(device.getDeviceName().concat("'s night light"));
                }
                device.setConnectionStatus(CommonConstant.COMMON_CONNECTION_STATUS_OFFLINE); // 默认离线
                device.setDeviceStatus(CommonConstant.COMMON_STATUS_OFF); // 默认关

                // 查询设备状态
                try {
                    JSONObject jsonObject = httpClientService.request(new String[]{device.getUuid()}, device.getUuid(), "getDeviceStatus");
                    Map<String, Object> responseMap = MyJsonUtils.coverDeviceStatusToMap(jsonObject);
                    if (responseMap.get("nightLightStatus") != null) {
                        device.setDeviceStatus((String) responseMap.get("nightLightStatus"));
                    }
                    if (responseMap.get("connectionStatus") != null) {
                        device.setConnectionStatus((String) responseMap.get("connectionStatus"));
                    }
                    if (responseMap.get("speed") != null) {
                        device.setSpeed((String) responseMap.get("speed"));
                    }
                    if (responseMap.get("mode") != null) {
                        device.setMode((String) responseMap.get("mode"));
                    }
                } catch (Exception e) {
                    log.error("查询设备:{}状态失败", device.getUuid(), e);
                }
                device.setType("nightLight");
            }
            // googlehome device
            DeviceGoogleHome deviceGoogleHome = new DeviceGoogleHome();
            deviceGoogleHome.setAlias(device.getDeviceName());
            deviceGoogleHome.setStatus(device.getConnectionStatus());
            deviceGoogleHome.setType(device.getType());
            deviceGoogleHome.setMode(device.getMode());
            deviceGoogleHome.setSpeed(device.getSpeed());
            // 7A不需要这个字段
            if (MyStringUtils.isNullData(device.getConfigModule())) {
                DeviceType deviceType = deviceTypeService.getDeviceType(device.getDeviceType());
                if (deviceType == null) {
                    log.info("根据DeviceType获取configmodel信息为空异常");
                    continue;
                }
                device.setConfigModule(deviceType.getConfigModel());
            }
            // 7A暂定不传configModel
            if (!"7AOutlet".equals(device.getConfigModule())) {
                deviceGoogleHome.setId(device.getCid().concat(";").concat(device.getConfigModule()));
            } else {
                deviceGoogleHome.setId(device.getCid());
            }
            // open break(设备开或关)
            if (!"open".equalsIgnoreCase(deviceGoogleHome.getRelay())
                    && !"break".equalsIgnoreCase(deviceGoogleHome.getRelay())) {
                deviceGoogleHome.setRelay("break");
                if (CommonConstant.COMMON_STATUS_ON.equals(device.getDeviceStatus())) {
                    deviceGoogleHome.setRelay("open");
                }
            }
            deviceGoogleHomes.add(deviceGoogleHome);
        }
        // googleHomeResponse.setDevices(response.getDevices());
        googleHomeResponse.setDeviceGoogleHomes(deviceGoogleHomes);
        return googleHomeResponse;
    }

    @Override
    public List<String> getUsedDeviceType(String accountId) {
        String usedDeviceType = (String) redisService.getMapField(CommonConstant.UserHash.concat(accountId), CommonConstant.REDIS_FIELD_USED_DEVICE_TYPE);
        List<String> usedDeviceList = JSONArray.parseArray(usedDeviceType, String.class);
        if (usedDeviceList == null) {
            usedDeviceList = new ArrayList<>();
        }
        return usedDeviceList;
    }

    @Override
    public List<DeviceInfo> getAccountDevice(String accountId) {
        // 持有设备
        List<DeviceInfo> ownDeviceList = getOwnDevice(accountId);
        // 分享设备
        List<DeviceInfo> shareDeviceList = getShareDevice(accountId);
        if (ownDeviceList != null && !ownDeviceList.isEmpty()) {
            if (shareDeviceList != null && !shareDeviceList.isEmpty()) {
                ownDeviceList.addAll(shareDeviceList);
            }
            return ownDeviceList;
        } else {
            if (shareDeviceList != null && !shareDeviceList.isEmpty()) {
                return shareDeviceList;
            } else {
                return new ArrayList<>();
            }
        }
    }

    @Override
    public List<DeviceInfo> getOwnDevice(String accountId) {
        String redisOwnDeviceKey = CommonConstant.REDIS_KEY_DEVICE_OWN_ACCOUNT.concat(accountId);
        List<DeviceInfo> deviceInfoList = redisService.getList(redisOwnDeviceKey);
        if (deviceInfoList != null && !deviceInfoList.isEmpty()) {
            return deviceInfoList;
        }
        DeviceInfo record = new DeviceInfo();
        record.setAccountId(accountId);
        deviceInfoList = deviceMapper.select(record);
        if (deviceInfoList == null || deviceInfoList.isEmpty()) {
            deviceInfoList = new ArrayList<>();
        } else {
            redisService.addList(redisOwnDeviceKey, deviceInfoList);
            redisService.setExpireTime(redisOwnDeviceKey, CommonConstant.SECONDS_OF_ONEDAY);
        }
        return deviceInfoList;
    }

    @Override
    public List<DeviceInfo> getShareDevice(String accountId) {
        String redisShareDeviceKey = CommonConstant.REDIS_KEY_DEVICE_SHARE_ACCOUNT.concat(accountId);
        List<DeviceInfo> deviceInfoList = redisService.getList(redisShareDeviceKey);
        if (deviceInfoList != null && !deviceInfoList.isEmpty()) {
            return deviceInfoList;
        }
        // 被分享设备
        List<DeviceSharer> shareDeviceList = shareService.queryDeviceShareListByUserId(accountId);
        deviceInfoList = new ArrayList<>();
        for (DeviceSharer ds : shareDeviceList) {
            DeviceInfo deviceInfo = getDeviceByCid(ds.getDeviceCid());
            if (deviceInfo != null && deviceInfo.getId() != null) {
                deviceInfoList.add(deviceInfo);
            }
        }
        if (!deviceInfoList.isEmpty()) {
            redisService.addList(redisShareDeviceKey, deviceInfoList);
            redisService.setExpireTime(redisShareDeviceKey, CommonConstant.SECONDS_OF_ONEDAY);
        }
        return deviceInfoList;
    }

    @Override
    public DeviceInfo getDeviceByCid(String cid) {
        String deviceCidKey = CommonConstant.REDIS_KEY_DEVICE_CID.concat(cid);
        if (redisService.exists(deviceCidKey)) {
            DeviceInfo di = (DeviceInfo) redisService.get(deviceCidKey);
            if (di != null && di.getId() != null) {
                return di;
            }
        }
        DeviceInfo record = new DeviceInfo();
        record.setDeviceCid(cid);
        record = deviceMapper.selectOne(record);
        if (record != null && record.getId() != null) {
            redisService.set(deviceCidKey, record, CommonConstant.SECONDS_OF_ONEDAY);
        }
        return record;
    }

    @Override
    public int updateByPrimaryKey(DeviceInfo device) {
        return deviceMapper.updateByPrimaryKey(device);
    }

    /**
     * 获取配置页信息
     *
     * @param uuid
     * @param accountId
     * @param response
     * @return
     */
    private ConfigurationsResponse queryConfigurations(String uuid, String accountId, ConfigurationsResponse response) {
        Assert.hasLength(uuid, "queryConfigurations方法deviceUuid参数不能为空");
        log.info("获取数据库设备配置页面信息");
        // 根据uuid查找设备信息
        DeviceInfo deviceInfo = queryDeviceByUuid(uuid);
        if (deviceInfo == null || deviceInfo.getId() == null) {
            response.setCode(ErrorConstant.ERR_DEVICE_NOT_EXIST);
            response.setMsg(ErrorConstant.ERR_DEVICE_NOT_EXIST_MSG);
            log.info("设备信息不存在");
            return response;
        }
        MyBeanUtils.copyPropertiesIgnoreCase(deviceInfo, response);
        // 根据model查找设备硬件版本信息
        String model = deviceInfo.getDeviceType();
        Assert.hasLength(model, "deviceInfo uuid 对应Devicetype栏信息为空");
        DeviceType deviceType = deviceTypeService.getDeviceType(model);
        Assert.notNull(deviceType, "devicetype对应的设备类型在DeviceTypeTable为空");
        response.setLatestFirmVersion(deviceType.getLatestFirmVersion());
        response.setDefaultDeviceImg(deviceType.getDeviceImg());

        // 获取该账户的所属设备信息
        boolean findAccountUuid = false;
        List<DeviceInfo> devices = getOwnDevice(accountId);
        for (DeviceInfo device : devices) {
            if (device.getUuid().equals(uuid)) {
                findAccountUuid = true;
            }
        }
        boolean ownerShip = false;
        // 该设备不属于账户设备
        if (findAccountUuid) {
            ownerShip = true;
        }
        response.setOwnerShip(ownerShip);
        // 获取该账户的分享设备信息
        devices = getShareDevice(accountId);
        for (DeviceInfo device : devices) {
            if (device.getUuid().equals(uuid)) {
                findAccountUuid = true;
            }
        }
        if (!ownerShip && !findAccountUuid) {
            response.setCode(ErrorConstant.ERR_USER_DONOT_OWN_DEVICE);
            response.setMsg(ErrorConstant.ERR_USER_DONOT_OWN_DEVICE_MSG);
            log.info("该用户不拥有该设备！");
            return response;
        }
        // 是否允許通知
        if (CommonConstant.COMMON_INT_STATUS_ON.equals(response.getAllowNotify())) {
            response.setAllowNotify(CommonConstant.COMMON_STATUS_ON);
        } else if (CommonConstant.COMMON_INT_STATUS_OFF.equals(response.getAllowNotify())) {
            response.setAllowNotify(CommonConstant.COMMON_STATUS_OFF);
        }

        // 获取固件版本信息
        try {
            JSONObject jSONObjectStatus = httpClientService.request(new String[]{uuid}, uuid, "getDeviceStatus");
            Map<String, Object> responseMap = MyJsonUtils.coverDeviceStatusToMap(jSONObjectStatus.getJSONObject("jsonStr"));
            String currentfirmversion = (String) responseMap.get("currentfirmversion");
            if (StringUtils.hasText(currentfirmversion)) {
                response.setCurrentFirmVersion(currentfirmversion);
            }
        } catch (Exception e) {
            log.debug("获取固件版本失败", e);
        }

        // 小夜灯自动模式开关状态
        if (CommonConstant.COMMON_INT_STATUS_ON.equals(response.getNightLightAutomode())) {
            response.setNightLightAutomode(CommonConstant.COMMON_STATUS_ON);
        } else if (CommonConstant.COMMON_INT_STATUS_OFF.equals(response.getNightLightAutomode())) {
            response.setNightLightAutomode(CommonConstant.COMMON_STATUS_OFF);
        }
        return response;
    }

    @Override
    public DeviceInfo getDeviceByCidAndAccountId(String cid, String account) {
        DeviceInfo deviceInfo = null;
        List<DeviceInfo> deviceInfoList = getAccountDevice(account);
        for (DeviceInfo di : deviceInfoList) {
            if (di.getDeviceCid().equals(cid)) {
                deviceInfo = di;
                break;
            }
        }
        return deviceInfo;
    }

    /**
     * @return void
     * @Author Ericer
     * @Description 删除单个用户设备信息（包括vdmp平台数据）
     * @Date 下午5:26 18-9-19
     * @Param [accountId, uuid, configModel]
     **/
    @Override
    public void deleteOwnSingleDevice(String accountId, String uuid, String deviceType) {
        Assert.hasLength(accountId, "deleteOwnSingleDevice accountId不能为空");
        Assert.hasLength(uuid, "deleteOwnSingleDevice uuid不能为空");

        logger.info("删除单个用户设备");

        DeviceInfo device = new DeviceInfo();
        device.setAccountId(accountId);
        device.setUuid(uuid);
        deleteOwnVdmpSingleDevice(device);
        //删除此设备所有的分享人信息
        shareService.deleteSharePeopleAllByUuid(uuid);
        // 删除设备
        deleteOwnSingleTableDevice(accountId, device.getUuid(), deviceType);
    }


    /**
     * 根据uuid删除设备（不调用平台）
     *
     * @param uuid
     * @return
     */
    @Override
    @Transactional(rollbackFor = Exception.class)
    public VBMPResponse deleteVbmpOwnDevice(String uuid) {
        Assert.hasText(uuid, "deleteVbmpOwnDevice uuid不能为空");
        VBMPResponse response = new VBMPResponse();
        //设备是否存在
        DeviceInfo currentUuidDevice = queryDeviceByUuid(uuid);
        if (currentUuidDevice == null) {
            response.setCode(ErrorConstant.ERR_DEVICE_NOT_EXIST);
            response.setMsg(ErrorConstant.ERR_DEVICE_NOT_EXIST_MSG);
            return response;
        }
        //删除此设备所有的分享人信息
        shareService.deleteSharePeopleAllByUuid(uuid);
        // 删除设备
        deleteOwnSingleTableDevice(null, uuid, currentUuidDevice.getDeviceType());
        return response;
    }

    /**
     * @return void
     * @Author Ericer
     * @Description 删除数据库和uuidkey的redis数据
     * @Date 下午5:25 18-9-19
     * @Param [accountId, uuid, configModel]
     **/
    @Override
    @Transactional(rollbackFor = Exception.class)
    public void deleteOwnSingleTableDevice(String accountId, String uuid, String deviceType) {
        logger.info("删除数据库redis单个设备");
        DeviceType deviceTypeTable = deviceTypeService.getDeviceType(deviceType);
        if (deviceTypeTable == null) {
            throw new ServiceException(ErrorConstant.ERR_DEVICE_TYPE_NOT_EXIST, ErrorConstant.ERR_DEVICE_TYPE_NOT_EXIST_MSG);
        }

        //先删除子表
        subTableModelService.deleteSubTableUserDataByUuid(deviceTypeTable.getConfigModel(), uuid);

        //删除子表schedule相关数据
        deviceTimingService.deleteTimingAllByUuid(uuid);

        //删除该用户设备redis缓存
        deleteOwnDeviceRedis(accountId, uuid);

        //再删除数据库主表
        DeviceInfo deviceInfo = new DeviceInfo();
        deviceInfo.setUuid(uuid);
        if (StringUtils.hasText(accountId)) {
            deviceInfo.setAccountId(accountId);
        }
        deviceMapper.delete(deviceInfo);
        //删除redis数据
        String redisUuidDKey = CommonConstant.REDIS_KEY_DEVICE_UUID.concat(uuid);
        String objectField = CommonConstant.UUID_DEVICE_OBJECT;
        redisService.removeMapField(redisUuidDKey, objectField);
    }

    /**
     * @return void
     * @Author Ericer
     * @Description 删除单个vdmp平台数据（区分在线离线）
     * @Date 下午5:26 18-9-19
     * @Param [device]
     **/
    @Override
    public void deleteOwnVdmpSingleDevice(DeviceInfo device) {
        logger.info("删除平台设备");
        JSONObject jsonObjectVBMPControl = null;
        try {
            jsonObjectVBMPControl = deviceControlService.getDeviceStatus(device.getUuid());
        } catch (Exception e) {
            log.error("uuid:{} 查询状态失败", device.getUuid());
        }

        JSONObject jsonObjectStr ;
        JSONObject jsonObject;
        //查询vdmp返回结果
        JSONObject jsonObjectVDMPQuery;
        if (jsonObjectVBMPControl != null && jsonObjectVBMPControl.getInteger("code") == 0) {
            //查看请求vdmp结果(不为零抛出错误)
            jsonObjectVDMPQuery = jsonObjectVBMPControl.getJSONObject("jsonStr");
            if(jsonObjectVDMPQuery.getInteger("code") != CommonConstant.COMMON_SUCCESS){
                throw new ServiceException(jsonObjectVDMPQuery.getInteger("code"), jsonObjectVDMPQuery.getString("msg"));
            }
            //调用vbmp删除vdmp平台删除设备
            String connectStatus = (String) MyJsonUtils.getJsonInfo(jsonObjectVDMPQuery, "data", "payload", "state", "reported", "connectionStatus");
            if (!CommonConstant.COMMON_CONNECTION_STATUS_ONLINE.equals(connectStatus)) {
                // 离线删除
                jsonObject = deviceControlService.delUserDataOffline(device.getUuid());
                log.info("删除离线设备");
            } else {
                jsonObject = deviceControlService.restoreDeviceState(device.getUuid());
                log.info("删除在线设备");
            }
            //判断删除状态
            if(jsonObject == null || jsonObject.getInteger("code") != CommonConstant.COMMON_SUCCESS){
                throw new ServiceException(jsonObject.getInteger("code"), jsonObject.getString("msg"));
            }else if((jsonObjectStr = jsonObject.getJSONObject("jsonStr")) == null || jsonObjectStr.getInteger("code") != CommonConstant.COMMON_SUCCESS){
                throw new ServiceException(jsonObjectStr.getInteger("code"), jsonObjectStr.getString("msg"));
            }
        }else{
            throw new ServiceException(jsonObjectVBMPControl.getInteger("code"), jsonObjectVBMPControl.getString("msg"));
        }
    }

    /**
     * @return com.etekcity.vbmp.common.config.VBMPResponse
     * @Author Ericer
     * @Description 删除设备
     * @Date 下午5:26 18-9-19
     * @Param [deviceRequest]
     **/
    @Override
    @Transactional(rollbackFor = Exception.class)
    public VBMPResponse deleteDevice(DeviceRequest deviceRequest) throws Exception {
        VBMPResponse response = new VBMPResponse();

        String accountID = deviceRequest.getAccountId();
        String uuid = deviceRequest.getUuid();
        //设备是否存在
        DeviceInfo currentUuidDevice = queryDeviceByUuid(uuid);
        if (currentUuidDevice == null) {
            response.setCode(ErrorConstant.ERR_DEVICE_NOT_EXIST);
            response.setMsg(ErrorConstant.ERR_DEVICE_NOT_EXIST_MSG);
            return response;
        }

        DeviceInfo currentDevice = null;
        //查询用户该uuid拥有的设备
        List<DeviceInfo> ownDeviceList = getOwnDevice(accountID);
        currentDevice = fillterUuidDevice(ownDeviceList, uuid);
        //删除设备拥有者设备
        if (currentDevice != null) {
            deleteOwnSingleDevice(accountID, uuid, currentUuidDevice.getDeviceType());
            return response;
        }
        //查询用户该uuid分享的设备
        else {
            List<DeviceInfo> shareDeviceList = getShareDevice(accountID);
            currentDevice = fillterUuidDevice(shareDeviceList, uuid);
        }
        //删除分享着设备
        if (currentDevice != null) {
            shareService.deleteSharePeopleByUuidAndUserId(accountID, uuid);
        }
        //用户不拥有设备
        else {
            response.setCode(ErrorConstant.ERR_USER_DONOT_OWN_DEVICE);
            response.setMsg(ErrorConstant.ERR_USER_DONOT_OWN_DEVICE_MSG);
        }

        return response;
    }


    /**
     * @return com.etekcity.vbmp.common.comm.dao.model.DeviceInfo
     * @Author Ericer
     * @Description 过滤设备列表uuid的设备
     * @Date 下午5:27 18-9-19
     * @Param [deviceList, uuid]
     **/
    private DeviceInfo fillterUuidDevice(List<DeviceInfo> deviceList, String uuid) {
        Assert.hasLength(uuid, "fillterUUIDDevice uuid不能为空");
        DeviceInfo currentDevice = null;
        if (deviceList == null) {
            return currentDevice;
        }

        for (DeviceInfo deviceInfo : deviceList) {
            if (uuid.equals(deviceInfo.getUuid())) {
                currentDevice = deviceInfo;
                break;
            }
        }
        return currentDevice;
    }

    /**
     * 删除redis中用户持有设备
     *
     * @param accountId 用户ID
     * @param uuid      UUID
     */
    @Override
    public void deleteOwnDeviceRedis(String accountId, String uuid) {
        String ownDeviceRedisKey = CommonConstant.REDIS_KEY_DEVICE_OWN_ACCOUNT.concat(accountId);
        List<DeviceInfo> deviceInfoList = redisService.getList(ownDeviceRedisKey);
        for (DeviceInfo di : deviceInfoList) {
            if (di.getUuid().equals(uuid)) {
                redisService.removeListValue(ownDeviceRedisKey, di);
            }
        }
    }

    /**
     * 删除redis中用户分享设备
     *
     * @param accountId 用户ID
     * @param uuid      UUID
     */
    @Override
    public void deleteShareDeviceInRedis(String accountId, String uuid) {
        String shareDeviceRedisKey = CommonConstant.REDIS_KEY_DEVICE_SHARE_ACCOUNT.concat(accountId);
        List<DeviceInfo> deviceInfoList = redisService.getList(shareDeviceRedisKey);
        for (DeviceInfo di : deviceInfoList) {
            if (di.getUuid().equals(uuid)) {
                redisService.removeListValue(shareDeviceRedisKey, di);
            }
        }
    }
}
