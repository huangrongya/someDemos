package com.etekcity.vbmp.timing.common.service.impl;

import com.alibaba.fastjson.JSONObject;
import com.etekcity.vbmp.timing.common.VBMPResponse;
import com.etekcity.vbmp.timing.common.redis.RedisService;
import com.etekcity.vbmp.timing.common.service.DeviceInfoService;
import com.etekcity.vbmp.timing.common.service.DeviceTypeService;
import com.etekcity.vbmp.timing.constant.CommonConstant;
import com.etekcity.vbmp.timing.constant.ErrorConstant;
import com.etekcity.vbmp.timing.modules.schedule.bean.DeviceSchedule;
import com.etekcity.vbmp.timing.modules.timer.bean.DeviceTimer;
import com.etekcity.vbmp.timing.util.MyDateUtils;
import com.etekcity.vbmp.timing.util.MyStringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpMethod;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;

import java.util.*;

@Service("httpsAPIService")
public class HttpsAPIService {

    private static final Logger logger = LoggerFactory.getLogger(HttpsAPIService.class);

    private static String GET_DEVICE_STATUS_URL = "/shadow/v1/get?";
    private static String REMOVE_DEVICE_URL = "/shadow/v1/remove?";
    private static String CHANGE_DEVICE_STATUS_URL = "/shadow/v1/desired?";
    private static String DELETE_DEVICE_DATA = "/gdpr/v1/deletedevicedata?";


    @Autowired
    RestTemplate restTemplate;
    @Autowired
    private HttpClientService httpClientService;

    @Autowired
    private RedisService redisService;
    @Autowired
    private DeviceInfoService deviceInfoService;
    @Autowired
    private DeviceTypeService deviceTypeService;


    private List<String> getSeqList(int number) {
        List<String> scheduleIds = new ArrayList<>(number);
        for (int i = 0; i < number; i++) {
            scheduleIds.add(i + 1 + "");
        }
        return scheduleIds;
    }

    /**
     * 新增或更新设备的schedule
     *
     * @param deviceSchedule DeviceSchedule
     * @param type           操作类型
     * @throws Exception
     */
    public VBMPResponse addScheduleToDevice(DeviceSchedule deviceSchedule, String type) {
        VBMPResponse VBMPResponse = new VBMPResponse();
        String scheduleId = "";
        short scheduleNumber = deviceTypeService.findDeviceTypeByModel(deviceInfoService.findDeviceByUuid(deviceSchedule.getUuid()).getDeviceType()).getScheduleMaxNumber();
        String unuseKey = CommonConstant.VDMP_DEVICE_SCHEDULE_UNUSED_PREFIX + "-" + deviceSchedule.getUuid();
        String usedKey = CommonConstant.VDMP_DEVICE_SCHEDULE_USED_PREFIX + "-" + deviceSchedule.getUuid();
        if (CommonConstant.ADD.equals(type)) {
            // 获取 id
            if (!redisService.exists(unuseKey)) {
                redisService.addList(unuseKey, getSeqList(scheduleNumber));
            }
            List<String> redisUnusedIds = redisService.getList(unuseKey);
            // 排序，取最小的
            redisUnusedIds.sort(Comparator.comparing(Integer::parseInt));
            scheduleId = redisUnusedIds.get(0);
        } else if (CommonConstant.UPD.equals(type)) {
            // 从已有的map里获取ID
            String key = CommonConstant.DEVICE_SCHEDULE_MAP_PREFIX.concat("-").concat(deviceSchedule.getUuid());
            Map<String, String> schedulesMap = redisService.getMap(key);
            for (Map.Entry<String, String> entry : schedulesMap.entrySet()) {
                // 存入MAP ？？ id ??
                String redisScheduleId = entry.getValue();
                if (Integer.valueOf(redisScheduleId).intValue() == deviceSchedule.getId().intValue()) {
                    scheduleId = entry.getKey();
                    break;
                }
            }
        } else {
            logger.error("type参数错误，只接受 add、upd");
            VBMPResponse.setCode(ErrorConstant.ERR_INVALID_PARAM_FORMAT);
            VBMPResponse.setMsg(ErrorConstant.ERR_INVALID_PARAM_FORMAT_MSG);
            return VBMPResponse;
        }

        if (MyStringUtils.isNullData(scheduleId)) {
            /*logger.error("未获取到生成平台定时的ID");
            VBMPResponse.setCode(ErrorConstant.ERR_NO_VDMP_ID);
            VBMPResponse.setMsg(ErrorConstant.ERR_NO_VDMP_ID_MSG);
            return VBMPResponse;*/
            scheduleId = "1";
        }

        try {
            Object[] msg = getPramsBySchedule(scheduleId, deviceSchedule.getExecuteStartTime(),
                    deviceSchedule.getStartAction(), deviceSchedule.getExecuteEndTime(),
                    deviceSchedule.getEndAction(), deviceSchedule.getScheduleRepeat(),
                    deviceSchedule.getStatus(), deviceSchedule.getEvent(), type);
            JSONObject response = httpClientService.request(msg, deviceSchedule.getUuid(), "schedule", HttpMethod.PUT);
            if ("0".equals(response.getString("code"))) { // 成功
                // 写入redis
                logger.info("add schedule to device success!");
                // scheduleId不存在时会新增，存在时会更新
                // MAP存对应timing的ID
                redisService.addMap(CommonConstant.DEVICE_SCHEDULE_MAP_PREFIX.concat("-")
                                .concat(deviceSchedule.getUuid()), scheduleId,
                        deviceSchedule.getId().toString());
                if (CommonConstant.ADD.equals(type)) {
                    // ID移至已使用的list
                    redisService.removeListValue(unuseKey, scheduleId);
                    redisService.addList(usedKey, scheduleId);
                }
                return VBMPResponse;
            } else {
                logger.info("add schedule to device failure!");
                VBMPResponse.setCode(response.getInteger("code"));
                VBMPResponse.setMsg(response.getString("msg"));
                return VBMPResponse;
            }
        } catch (Exception e) {
            logger.error("新增或更新设备的schedule错误", e);
            VBMPResponse.setCode(ErrorConstant.ERR_INTERNAL_SERVER);
            VBMPResponse.setMsg(ErrorConstant.ERR_INTERNAL_SERVER_MSG);
            return VBMPResponse;
        }
    }

    /**
     * 删除设备SCHEDULE
     *
     * @param deviceSchedule DEVICESCHEDULE
     * @throws Exception
     */
    public VBMPResponse delScheduleFromDevice(DeviceSchedule deviceSchedule) {
        VBMPResponse response = new VBMPResponse();
        String id = "";
        String key = CommonConstant.DEVICE_SCHEDULE_MAP_PREFIX.concat("-").concat(deviceSchedule.getUuid());
        Map<String, String> schedulesMap = redisService.getMap(key);
        for (Map.Entry<String, String> entry : schedulesMap.entrySet()) {
            String redisScheduleId = entry.getValue();
            if (Integer.valueOf(redisScheduleId).intValue() == deviceSchedule.getId().intValue()) {
                id = entry.getKey();
                break;
            }
        }

        if (MyStringUtils.isNullData(id)) {
            /*logger.error("未获取到生成平台定时的ID");
            VBMPResponse.setCode(ErrorConstant.ERR_NO_VDMP_ID);
            VBMPResponse.setMsg(ErrorConstant.ERR_NO_VDMP_ID_MSG);
            return VBMPResponse;*/
            id = "1";
        }

        try {
            Object[] msg = getPramsBySchedule(id, deviceSchedule.getExecuteStartTime(),
                    deviceSchedule.getStartAction(), deviceSchedule.getExecuteEndTime(),
                    deviceSchedule.getEndAction(), deviceSchedule.getScheduleRepeat(),
                    deviceSchedule.getStatus(), deviceSchedule.getEvent(), CommonConstant.DEL);
            JSONObject res = httpClientService.request(msg, deviceSchedule.getUuid(), "schedule", HttpMethod.PUT);

            if ("0".equals(res.getString("code"))) { // 成功
                // 移除此条信息
                redisService.removeMapField(key, id);
                String unuseKey = CommonConstant.VDMP_DEVICE_SCHEDULE_UNUSED_PREFIX + "-" + deviceSchedule.getUuid();
                String usedKey = CommonConstant.VDMP_DEVICE_SCHEDULE_USED_PREFIX + "-" + deviceSchedule.getUuid();
                // 使用过的合集移除
                redisService.removeListValue(usedKey, id);
                // 未使用的添加
                redisService.addList(unuseKey, id);
            } else {
                logger.info("add schedule to device failure!");
                response.setCode(res.getInteger("code"));
                response.setMsg(res.getString("msg"));
            }
            return response;
        } catch (Exception e) {
            logger.error("删除设备的schedule错误", e);
            response.setCode(ErrorConstant.ERR_INTERNAL_SERVER);
            response.setMsg(ErrorConstant.ERR_INTERNAL_SERVER_MSG);
            return response;
        }
    }

    public VBMPResponse addTimerToDevice(DeviceTimer deviceTimer, String type) {
        VBMPResponse VBMPResponse = new VBMPResponse();
        String scheduleId = "";
        int timerNumber = deviceTypeService.findDeviceTypeByModel(deviceInfoService.findDeviceByUuid(deviceTimer.getUuid()).getDeviceType()).getTimerMaxNumber();
        String unuseKey = CommonConstant.VDMP_DEVICE_TIMER_UNUSED_PREFIX + "-" + deviceTimer.getUuid();
        String usedKey = CommonConstant.VDMP_DEVICE_TIMER_USED_PREFIX + "-" + deviceTimer.getUuid();
        if (timerNumber > 1) {
            if (CommonConstant.ADD.equals(type)) {
                // 获取 id
                if (!redisService.exists(unuseKey)) {
                    redisService.addList(unuseKey, getSeqList(timerNumber));
                }
                List<String> redisUnusedIds = redisService.getList(unuseKey);
                // 排序，取最小的
                redisUnusedIds.sort(Comparator.comparing(Integer::parseInt));
                scheduleId = redisUnusedIds.get(0);
            } else if (CommonConstant.UPD.equals(type)) {
                // 从已有的map里获取ID
                String key = CommonConstant.DEVICE_TIMER_MAP_PREFIX.concat("-").concat(deviceTimer.getUuid());
                Map<String, String> schedulesMap = redisService.getMap(key);
                for (Map.Entry<String, String> entry : schedulesMap.entrySet()) {
                    // 存入MAP ？？ id ??
                    String redisScheduleId = entry.getValue();
                    if (Integer.valueOf(redisScheduleId).intValue() == deviceTimer.getId().intValue()) {
                        scheduleId = entry.getKey();
                        break;
                    }
                }
            } else {
                logger.error("type参数错误，只接受 add、upd");
                VBMPResponse.setCode(ErrorConstant.ERR_INVALID_PARAM_FORMAT);
                VBMPResponse.setMsg(ErrorConstant.ERR_INVALID_PARAM_FORMAT_MSG);
                return VBMPResponse;
            }

            if (MyStringUtils.isNullData(scheduleId)) {
            /*logger.error("未获取到生成平台定时的ID");
            VBMPResponse.setCode(ErrorConstant.ERR_NO_VDMP_ID);
            VBMPResponse.setMsg(ErrorConstant.ERR_NO_VDMP_ID_MSG);
            return VBMPResponse;*/
                scheduleId = "1";
            }
        }

        try {
            // 停止也删除
            if (!CommonConstant.ADD.equals(type)) {
                if ("0".equals(deviceTimer.getStatus())) {
                    type = CommonConstant.DEL;
                }
            }

            Object[] msg = getParamsByTimer(scheduleId, deviceTimer.getSeconds(), deviceTimer.getAction(), type);
            JSONObject response = httpClientService.request(msg, deviceTimer.getUuid(), "timer", HttpMethod.PUT);
            if ("0".equals(response.getString("code"))) { // 成功
                // 写入redis
                logger.info("add schedule to device success!");
                // scheduleId不存在时会新增，存在时会更新
                // MAP存对应timing的ID
                redisService.addMap(CommonConstant.DEVICE_TIMER_MAP_PREFIX.concat("-").concat(deviceTimer.getUuid()), scheduleId, deviceTimer.getId().toString());
                if (CommonConstant.ADD.equals(type)) {
                    // ID移至已使用的list
                    redisService.removeListValue(unuseKey, scheduleId);
                    redisService.addList(usedKey, scheduleId);
                }
            } else {
                logger.info("add schedule to device failure!");
                VBMPResponse.setCode(response.getInteger("code"));
                VBMPResponse.setMsg(response.getString("msg"));
            }
            return VBMPResponse;
        } catch (Exception e) {
            logger.error("新增或更新设备的timer错误", e);
            VBMPResponse.setCode(ErrorConstant.ERR_INTERNAL_SERVER);
            VBMPResponse.setMsg(ErrorConstant.ERR_INTERNAL_SERVER_MSG);
            return VBMPResponse;
        }
    }

    public VBMPResponse delTimerFromDevice(DeviceTimer deviceTimer) {
        VBMPResponse VBMPResponse = new VBMPResponse();
        String id = "";
        String key = CommonConstant.DEVICE_TIMER_MAP_PREFIX.concat("-").concat(deviceTimer.getUuid());
        int timerNumber = deviceTypeService.findDeviceTypeByModel(deviceInfoService.findDeviceByUuid(deviceTimer.getUuid()).getDeviceType()).getTimerMaxNumber();
        if (timerNumber > 1) {
            Map<String, String> schedulesMap = redisService.getMap(key);
            for (Map.Entry<String, String> entry : schedulesMap.entrySet()) {
                String redisScheduleId = entry.getValue();
                if (Integer.valueOf(redisScheduleId).intValue() == deviceTimer.getId().intValue()) {
                    id = entry.getKey();
                    break;
                }
            }

            if (MyStringUtils.isNullData(id)) {
            /*logger.error("未获取到生成平台定时的ID");
            VBMPResponse.setCode(ErrorConstant.ERR_NO_VDMP_ID);
            VBMPResponse.setMsg(ErrorConstant.ERR_NO_VDMP_ID_MSG);
            return VBMPResponse;*/
                id = "1";
            }
        }
        try {
            Object[] msg = getParamsByTimer(id, deviceTimer.getSeconds(), deviceTimer.getAction(), CommonConstant.DEL);
            JSONObject response = httpClientService.request(msg, deviceTimer.getUuid(), "timer", HttpMethod.PUT);
            if ("0".equals(response.getString("code"))) { // 成功
                // 移除此条信息
                redisService.removeMapField(key, id);
                String unuseKey = CommonConstant.VDMP_DEVICE_TIMER_UNUSED_PREFIX + "-" + deviceTimer.getUuid();
                String usedKey = CommonConstant.VDMP_DEVICE_TIMER_USED_PREFIX + "-" + deviceTimer.getUuid();
                // 使用过的合集移除
                redisService.removeListValue(usedKey, id);
                // 未使用的添加
                redisService.addList(unuseKey, id);
            } else {
                VBMPResponse.setCode(response.getInteger("code"));
                VBMPResponse.setMsg(response.getString("msg"));
            }
            return VBMPResponse;
        } catch (Exception e) {
            logger.error("删除设备的timer错误", e);
            VBMPResponse.setCode(ErrorConstant.ERR_INTERNAL_SERVER);
            VBMPResponse.setMsg(ErrorConstant.ERR_INTERNAL_SERVER_MSG);
            return VBMPResponse;
        }
    }


    /**
     * 拼装请求的参数
     *
     * @param startTime    开始时间
     * @param startAcction 开始动作
     * @param endTime      结束时间
     * @param endAction    结束动作
     * @param repeat       重复
     * @param status       状态
     * @param type         类型
     * @return
     */
    private String[] getPramsBySchedule(String seq, Date startTime,
                                        String startAcction,
                                        Date endTime,
                                        String endAction,
                                        String repeat,
                                        String status,
                                        String event,
                                        String type) {

        String operation = CommonConstant.DEL.equalsIgnoreCase(type) ? CommonConstant.DEL : CommonConstant.ADD;
        // endTs > startTs
        long startTs = startTime.getTime() / 1000L;
        long endTs = 0L;
        if (endTime != null) {
            endTs = endTime.getTime() / 1000L;
            // 加一天的秒数
            if (endTs < startTs) {
                endTs += 24 * 60 * 60;
            }
        }
        // 0000000 顺序对应星期日、星期六、星期五、星期四、星期三、星期二、星期一、不重复
        String loop = "00000000"; // 不重复
        if (!"7".equals(repeat)) {
            loop = MyDateUtils.getRepeatByteString(repeat);
        }
        return new String[]{"schedule".concat(seq), operation, "1".equals(status) ? "enable" : "disable", startTs + "", endTs + "",
                Integer.parseInt(loop, 2) + "", CommonConstant.EVENT_SWITCH.equals(event) ? "switch1" : "light1", startAcction,
                endTime != null ? endAction : startAcction};
    }

    private Object[] getParamsByTimer(String seq, Integer setTime, String action, String type) {
        return new Object[]{"timer".concat(seq), CommonConstant.DEL.equalsIgnoreCase(type) ? CommonConstant.DEL : CommonConstant.ADD, setTime, "switch1", action};
    }

}
