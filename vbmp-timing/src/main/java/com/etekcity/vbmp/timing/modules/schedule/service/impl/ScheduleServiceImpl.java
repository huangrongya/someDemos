package com.etekcity.vbmp.timing.modules.schedule.service.impl;

import com.etekcity.vbmp.timing.common.VBMPResponse;
import com.etekcity.vbmp.timing.common.bean.DeviceInfo;
import com.etekcity.vbmp.timing.common.redis.RedisService;
import com.etekcity.vbmp.timing.common.service.DeviceInfoService;
import com.etekcity.vbmp.timing.common.service.DeviceTypeService;
import com.etekcity.vbmp.timing.common.service.impl.HttpsAPIService;
import com.etekcity.vbmp.timing.constant.CommonConstant;
import com.etekcity.vbmp.timing.constant.ErrorConstant;
import com.etekcity.vbmp.timing.exception.ServiceException;
import com.etekcity.vbmp.timing.modules.away.bean.DeviceAway;
import com.etekcity.vbmp.timing.modules.away.bean.RedisDeviceAway;
import com.etekcity.vbmp.timing.modules.away.dao.DeviceAwayMapper;
import com.etekcity.vbmp.timing.modules.away.service.impl.AwayServiceImpl;
import com.etekcity.vbmp.timing.modules.schedule.bean.*;
import com.etekcity.vbmp.timing.modules.schedule.dao.DeviceScheduleMapper;
import com.etekcity.vbmp.timing.modules.schedule.service.ScheduleService;
import com.etekcity.vbmp.timing.modules.timer.bean.DeviceTimer;
import com.etekcity.vbmp.timing.modules.timer.bean.RedisDeviceTimer;
import com.etekcity.vbmp.timing.modules.timer.dao.DeviceTimerMapper;
import com.etekcity.vbmp.timing.util.MyDateUtils;
import com.etekcity.vbmp.timing.util.MyStringUtils;
import com.etekcity.vbmp.timing.util.SunTimeUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.*;
import java.util.stream.Collectors;

@SuppressWarnings("SpringJavaInjectionPointsAutowiringInspection")
@Service
@Transactional
public class ScheduleServiceImpl implements ScheduleService {
    private Logger logger = LoggerFactory.getLogger(AwayServiceImpl.class);
    @Autowired
    RedisService redisService;
    @Autowired
    DeviceScheduleMapper deviceScheduleMapper;
    @Autowired
    DeviceInfoService deviceInfoService;
    @Autowired
    DeviceTypeService deviceTypeService;
    @Autowired
    DeviceAwayMapper deviceAwayMapper;
    @Autowired
    DeviceTimerMapper deviceTimerMapper;
    @Autowired
    HttpsAPIService httpsAPIService;

    @Override
    public ScheduleResponse addSchedule(ScheduleRequest request) throws Exception {
        ScheduleResponse response = new ScheduleResponse();

        String lockKey = CommonConstant.DEVICE_SCHEDULE_LOCK_PREFIX + "add-" + request.getUuid();
        redisService.lock(lockKey);
        DeviceInfo deviceInfo = deviceInfoService.findDeviceByUuid(request.getUuid());
        String redisDeviceScheduleKey = CommonConstant.TIMING_SCHEDULE.concat(":").concat(deviceInfo.getDeviceType()).concat("-").concat(request.getUuid());
        List<DeviceSchedule> deviceSchedules = redisService.getList(redisDeviceScheduleKey);

        if (deviceSchedules == null || deviceSchedules.isEmpty()) {
            deviceSchedules = deviceScheduleMapper.selectByUuid(request.getUuid());
            if (deviceSchedules != null && !deviceSchedules.isEmpty()) {
                redisService.addList(redisDeviceScheduleKey, deviceSchedules);
            }
        }
        short scheduleMax = deviceTypeService.findDeviceTypeByModel(deviceInfoService.findDeviceByUuid(request.getUuid()).getDeviceType()).getScheduleMaxNumber();
        // 验证schedule上限26个
        if (deviceSchedules != null && deviceSchedules.size() >= scheduleMax) {
            response.setCode(ErrorConstant.ERR_DEVICE_SCHEDULE_MAX);
            response.setMsg(ErrorConstant.ERR_DEVICE_SCHEDULE_MAX_MSG);
            logger.error(response.getMsg());
            return response;
        }

        String requestStartTime = request.getStartTime();
        String requestEndTime = request.getEndTime();
        String startTimeSunTime = null;
        String endTimeSunTime = null;

        // 根据经纬度获取日出日落时间
        if ("1".equals(request.getSunTime())) {
            Double lon = Double.parseDouble(request.getLongitude());
            Double lat = Double.parseDouble(request.getLatitude());
            String[] sunTimes = SunTimeUtils.getSunTime(request.getTimeZone(), lon, lat);
            if ("s".equals(request.getStartTime())) {
                // 选择日出时间
                requestStartTime = sunTimes[0];
                startTimeSunTime = request.getStartTime();
            } else if ("e".equals(request.getStartTime())) {
                // 选择日落时间
                requestStartTime = sunTimes[1];
                startTimeSunTime = request.getStartTime();
            }
            if (!MyStringUtils.isNullData(request.getEndTime())) {
                if ("s".equals(request.getEndTime())) {
                    // 选择日出时间
                    requestEndTime = sunTimes[0];
                    endTimeSunTime = request.getEndTime();
                } else if ("e".equals(request.getEndTime())) {
                    // 选择日落时间
                    requestEndTime = sunTimes[1];
                    endTimeSunTime = request.getEndTime();
                }
            }
        }

        Date executeStartTime = MyDateUtils.getUtcDateByTimeAndZone(requestStartTime, request.getTimeZone());
        Date executeEndTime = MyDateUtils.getUtcDateByTimeAndZone(requestEndTime, request.getTimeZone());
        int startPlusDay = MyDateUtils.getPlusDay(requestStartTime, request.getTimeZone());
        int endPlusDay = MyDateUtils.getPlusDay(requestEndTime, request.getTimeZone());

        Date now = MyDateUtils.getUtcDateTime();
        if ("7".equals(request.getRepeat())) {
            if (executeStartTime.getTime() / 1000L < now.getTime() / 1000L) { // 开始时间当前时间小
                Calendar start = Calendar.getInstance();
                start.setTime(executeStartTime);
                start.add(Calendar.DAY_OF_YEAR, 1);
                executeStartTime = start.getTime();

                if (executeEndTime != null) {
                    Calendar end = Calendar.getInstance();
                    end.setTime(executeEndTime);
                    end.add(Calendar.DAY_OF_YEAR, 1);
                    executeEndTime = end.getTime();
                }
            }
        }
        // 结束时间比开始时间小，则结束时间加一天
        if (executeEndTime != null && executeEndTime.getTime() / 1000L < executeStartTime.getTime() / 1000L) {
            Calendar c = GregorianCalendar.getInstance();
            c.setTime(executeEndTime);
            c.add(Calendar.DAY_OF_YEAR, 1);
            executeEndTime = c.getTime();
        }

        Map<String, Object> params = new HashMap<>();
        params.put("uuid", request.getUuid());
        params.put("awayStatus", "1"); // 开户状态
        params.put("timerStatus", "1"); // 开启状态
        params.put("scheduleState", "1"); // 开启状态

        if (CommonConstant.EVENT_SWITCH.equals(request.getEvent()) || MyStringUtils.isNullData(request.getEvent())) { // 插座
            // 全为空，需要验证是否有冲突
            if ((request.getConflictAwayIds() == null || request.getConflictAwayIds().isEmpty()) &&
                    (request.getConflictScheduleIds() == null || request.getConflictScheduleIds().isEmpty()) &&
                    (request.getConflictTimerIds() == null || request.getConflictTimerIds().isEmpty())) {
                logger.info("验证时间冲突");
                List<Integer> conflictAwayIds = validateScheduleAway(request.getUuid(), executeStartTime, executeEndTime, request.getRepeat(), startPlusDay, endPlusDay);
                List<Integer> conflictScheduleIds = validateScheduleSelf(request.getUuid(), null, executeStartTime, executeEndTime, request.getRepeat(), startPlusDay, endPlusDay, request.getEvent());
                List<Integer> conflictTimerIds = validateScheduleTimer(request.getUuid(), executeStartTime, executeEndTime, request.getRepeat(), startPlusDay, endPlusDay);
                if (!conflictAwayIds.isEmpty() || !conflictScheduleIds.isEmpty() || !conflictTimerIds.isEmpty()) {
                    response.setCode(ErrorConstant.ERR_DEVICE_TIMING);
                    response.setMsg(ErrorConstant.ERR_DEVICE_TIMING_MSG);
                    response.setConflictAwayIds(conflictAwayIds);
                    response.setConflictScheduleIds(conflictScheduleIds);
                    response.setConflictTimerIds(conflictTimerIds);
                    return response;
                } // else 表示 没有冲突
            } else { // 传了冲突的ID过来，表示需要停止冲突timing
                stopConflictAway(request.getUuid(), request.getConflictAwayIds());
                stopConflictSchedule(request.getUuid(), request.getConflictScheduleIds());
                stopConflictTimer(request.getUuid(), request.getConflictTimerIds());
            }
        } else { // 夜灯
            if (request.getConflictScheduleIds() == null || request.getConflictScheduleIds().isEmpty()) {
                List<Integer> conflictScheduleIds = validateScheduleSelf(request.getUuid(), null, executeStartTime, executeEndTime, request.getRepeat(), startPlusDay, endPlusDay, request.getEvent());
                if (!conflictScheduleIds.isEmpty()) {
                    response.setCode(ErrorConstant.ERR_DEVICE_TIMING);
                    response.setMsg(ErrorConstant.ERR_DEVICE_TIMING_MSG);
                    response.setConflictScheduleIds(conflictScheduleIds);
                    return response;
                } // else 为没有冲突
            } else {
                // 传了冲突的ID过来，先停再保存对应的数据
                stopConflictSchedule(request.getUuid(), request.getConflictScheduleIds());
            }
        }
        DeviceSchedule deviceSchedule = new DeviceSchedule();
        deviceSchedule.setDeviceId(deviceInfo.getId());
        deviceSchedule.setDeviceCid(deviceInfo.getDeviceCid());
        deviceSchedule.setUuid(request.getUuid());
        deviceSchedule.setExecuteEndTime(executeEndTime);
        deviceSchedule.setExecuteStartTime(executeStartTime);
        deviceSchedule.setEndAction(request.getEndState());
        deviceSchedule.setStartTime(requestStartTime);
        deviceSchedule.setEndTime(requestEndTime);
        deviceSchedule.setScheduleRepeat(request.getRepeat());
        deviceSchedule.setStartAction(request.getStartState());
        deviceSchedule.setStatus("1"); // 开启
        deviceSchedule.setAccountId(request.getAccountId());
        deviceSchedule.setTimeZone(request.getTimeZone());
        deviceSchedule.setCreateTime(MyDateUtils.getUtcDateTime()); // 创建时间
        deviceSchedule.setTurnonTime(MyDateUtils.getUtcDateTime()); // 把状态改为Start(1)的时间
        deviceSchedule.setStartSunTime(startTimeSunTime);
        deviceSchedule.setEndSunTime(endTimeSunTime);
        deviceSchedule.setEvent(request.getEvent());

        // 保存数据
        deviceScheduleMapper.insertSelective(deviceSchedule);

        // 向设备添加一个schedule
        VBMPResponse res = httpsAPIService.addScheduleToDevice(deviceSchedule, CommonConstant.ADD);
        if (res.getCode() != 0) {
            logger.error("操作设备定时出错");
            throw new ServiceException(res.getCode(), res.getMsg());
        }

        // 更新redis
        if (redisService.exists(redisDeviceScheduleKey)) {
            redisService.addList(redisDeviceScheduleKey, deviceSchedule);
        }
        // 添加定时到redis zset
        RedisDeviceSchedule schedule = new RedisDeviceSchedule();
        schedule.setUuid(deviceSchedule.getUuid());
        schedule.setDeviceCid(deviceSchedule.getDeviceCid());
        schedule.setDeviceId(deviceSchedule.getDeviceId());
        schedule.setExecuteStartTime(executeStartTime);
        schedule.setStartAction(deviceSchedule.getStartAction());
        schedule.setScheduleRepeat(deviceSchedule.getScheduleRepeat());
        schedule.setId(deviceSchedule.getId());
        schedule.setType(CommonConstant.START);
        // 如果开始或结束时间比当前时间小，则在时间上加一天
        if (executeStartTime.compareTo(now) <= 0) {
            Calendar calendar = Calendar.getInstance();
            calendar.setTime(executeStartTime);
            calendar.add(Calendar.DAY_OF_YEAR, 1);
            schedule.setExecuteStartTime(calendar.getTime());
        }
        schedule.setMinutes(MyDateUtils.getMinutsByDate(schedule.getExecuteStartTime()));
        redisService.addZSet(CommonConstant.TIMING_SCHEDULE.concat(":").concat(deviceInfo.getDeviceType()), MyDateUtils.getMinutsByDate(schedule.getExecuteStartTime()), schedule);
        if (executeEndTime != null) {
            schedule.setExecuteEndTime(executeEndTime);
            schedule.setType(CommonConstant.END);
            schedule.setEndAction(deviceSchedule.getEndAction());
            // 如果开始或结束时间比当前时间小，则在时间上加一天
            if (executeEndTime.compareTo(now) <= 0) {
                Calendar calendar = Calendar.getInstance();
                calendar.setTime(executeEndTime);
                calendar.add(Calendar.DAY_OF_YEAR, 1);
                schedule.setExecuteEndTime(calendar.getTime());
            }
            schedule.setMinutes(MyDateUtils.getMinutsByDate(schedule.getExecuteEndTime()));
            redisService.addZSet(CommonConstant.TIMING_SCHEDULE.concat(":").concat(deviceInfo.getDeviceType()), MyDateUtils.getMinutsByDate(schedule.getExecuteEndTime()), schedule);
        }
        // return
        response.setScheduleId(deviceSchedule.getId());
        redisService.unlock(lockKey);
        return response;
    }

    @Override
    public ScheduleResponse deleteSchedule(ScheduleRequest request) {
        ScheduleResponse response = new ScheduleResponse();
        Integer id = request.getScheduleId();
        // 验证schedule存在性
        DeviceSchedule deviceSchedule = deviceScheduleMapper.selectByPrimaryKey(id);
        if (deviceSchedule == null || deviceSchedule.getId() == null) {
            response.setCode(ErrorConstant.ERR_DEVICE_SCHEDULE_NOT_EXIST);
            response.setMsg(ErrorConstant.ERR_DEVICE_SCHEDULE_NOT_EXIST_MSG);
            logger.error(response.getMsg());
            return response;
        }

        String lockKey = CommonConstant.DEVICE_SCHEDULE_LOCK_PREFIX + "del-" + id;
        redisService.lock(lockKey);


        Date executeStartTime = MyDateUtils.getUtcDateByTimeAndZone(deviceSchedule.getStartTime(), deviceSchedule.getTimeZone());
        Date executeEndTime = MyDateUtils.getUtcDateByTimeAndZone(deviceSchedule.getEndTime(), deviceSchedule.getTimeZone());

        // 结束时间比开始时间小，则结束时间加一天
        if (executeEndTime != null && executeEndTime.before(executeStartTime)) {
            Calendar c = GregorianCalendar.getInstance();
            c.setTime(executeEndTime);
            c.add(Calendar.DAY_OF_YEAR, 1);
            executeEndTime = c.getTime();
        }

        deviceSchedule.setExecuteEndTime(executeEndTime);
        deviceSchedule.setExecuteStartTime(executeStartTime);

        // 删除设备的schedule
        VBMPResponse res = httpsAPIService.delScheduleFromDevice(deviceSchedule);
        if (res.getCode() != 0) {
            logger.error("操作设备定时出错");
            throw new ServiceException(res.getCode(), res.getMsg());
        }

        // delete schedule in db
        deviceScheduleMapper.deleteByPrimaryKey(id);
        DeviceInfo deviceInfo = deviceInfoService.findDeviceByUuid(request.getUuid());
        // 更新redis
        String redisDeviceScheduleKey = CommonConstant.TIMING_SCHEDULE.concat(":").concat(deviceInfo.getDeviceType()).concat("-").concat(deviceSchedule.getUuid());
        if (redisService.exists(redisDeviceScheduleKey)) {
            List<DeviceSchedule> redisDeviceSchedules = redisService.getList(redisDeviceScheduleKey);
            if (redisDeviceSchedules != null && !redisDeviceSchedules.isEmpty()) {
                for (DeviceSchedule ds : redisDeviceSchedules) {
                    if (ds.getId().intValue() == deviceSchedule.getId().intValue()) {
                        redisService.removeListValue(redisDeviceScheduleKey, ds);
                        break;
                    }
                }
            }
        }
        if ("1".equals(deviceSchedule.getStatus())) {
            // redis zset 移除定时
            if (redisService.exists(CommonConstant.TIMING_SCHEDULE.concat(":").concat(deviceInfo.getDeviceType()))) {
                Set<RedisDeviceSchedule> scheduleSet = (Set<RedisDeviceSchedule>) redisService.getZSetRange(CommonConstant.TIMING_SCHEDULE.concat(":").concat(deviceInfo.getDeviceType()));
                if (scheduleSet != null && !scheduleSet.isEmpty()) {
                    for (RedisDeviceSchedule schedule : scheduleSet) {
                        if (schedule.getId().intValue() == deviceSchedule.getId().intValue()) {
                            redisService.removeZSetValue(CommonConstant.TIMING_SCHEDULE.concat(":").concat(deviceInfo.getDeviceType()), schedule);
                        }
                    }
                }
            }
        }
        redisService.unlock(lockKey);
        return response;
    }

    @Override
    public ScheduleResponse updateSchedule(ScheduleRequest request) throws Exception {
        ScheduleResponse response = new ScheduleResponse();
        // 验证schedule存在性
        DeviceSchedule deviceSchedule = deviceScheduleMapper.selectByPrimaryKey(Integer.valueOf(request.getScheduleId()));
        if (deviceSchedule == null || deviceSchedule.getId() == null) {
            response.setCode(ErrorConstant.ERR_DEVICE_SCHEDULE_NOT_EXIST);
            response.setMsg(ErrorConstant.ERR_DEVICE_SCHEDULE_NOT_EXIST_MSG);
            logger.error(response.getMsg());
            return response;
        }

        String lockKey = CommonConstant.DEVICE_SCHEDULE_LOCK_PREFIX + "upd-" + request.getScheduleId();
        redisService.lock(lockKey);


        String requestStartTime = request.getStartTime();
        String requestEndTime = request.getEndTime();
        String startTimeSunTime = null;
        String endTimeSunTime = null;

        // 根据经纬度获取日出日落时间
        if ("1".equals(request.getSunTime())) {
            // 把状态改为启动的时间(未启动的不影响)
            deviceSchedule.setStartTime(MyDateUtils.dateToString(MyDateUtils.getUtcDateTime()));
            Double lon = Double.parseDouble(request.getLongitude());
            Double lat = Double.parseDouble(request.getLatitude());
            String[] sunTimes = SunTimeUtils.getSunTime(request.getTimeZone(), lon, lat);
            if ("s".equals(request.getStartTime())) {
                // 选择日出时间
                requestStartTime = sunTimes[0];
                startTimeSunTime = request.getStartTime();
            } else if ("e".equals(request.getStartTime())) {
                // 选择日落时间
                requestStartTime = sunTimes[1];
                startTimeSunTime = request.getStartTime();
            }
            if (!MyStringUtils.isNullData(request.getEndTime())) {
                if ("s".equals(request.getEndTime())) {
                    // 选择日出时间
                    requestEndTime = sunTimes[0];
                    endTimeSunTime = request.getEndTime();
                } else if ("e".equals(request.getEndTime())) {
                    // 选择日落时间
                    requestEndTime = sunTimes[1];
                    endTimeSunTime = request.getEndTime();
                }
            }
        }

        Date executeStartTime = MyDateUtils.getUtcDateByTimeAndZone(requestStartTime, request.getTimeZone());
        Date executeEndTime = MyDateUtils.getUtcDateByTimeAndZone(requestEndTime, request.getTimeZone());
        int startPlusDay = MyDateUtils.getPlusDay(requestStartTime, request.getTimeZone());
        int endPlusDay = MyDateUtils.getPlusDay(requestEndTime, request.getTimeZone());

        Date now = MyDateUtils.getUtcDateTime();
        if ("7".equals(request.getRepeat())) {
            if (executeStartTime.getTime() / 1000L < now.getTime() / 1000L) { // 开始时间当前时间小
                Calendar start = Calendar.getInstance();
                start.setTime(executeStartTime);
                start.add(Calendar.DAY_OF_YEAR, 1);
                executeStartTime = start.getTime();

                if (executeEndTime != null) {
                    Calendar end = Calendar.getInstance();
                    end.setTime(executeEndTime);
                    end.add(Calendar.DAY_OF_YEAR, 1);
                    executeEndTime = end.getTime();
                }
            }
        }
        // 结束时间比开始时间小，则结束时间加一天
        if (executeEndTime != null && executeEndTime.getTime() / 1000L < executeStartTime.getTime() / 1000L) {
            Calendar c = GregorianCalendar.getInstance();
            c.setTime(executeEndTime);
            c.add(Calendar.DAY_OF_YEAR, 1);
            executeEndTime = c.getTime();
        }

        Map<String, Object> params = new HashMap<>();
        params.put("uuid", request.getUuid());
        params.put("awayStatus", "1"); // 开户状态
        params.put("timerStatus", "1"); // 开启状态
        params.put("scheduleState", "1"); // 开启状态


        if ("1".equals(request.getScheduleState())) { // Schedule开始状态
            if (CommonConstant.EVENT_SWITCH.equals(request.getEvent())) { // 插座schedule
                // 全为空，需要验证是否有冲突
                logger.info("验证插座时间冲突");
                if ((request.getConflictAwayIds() == null || request.getConflictAwayIds().isEmpty()) &&
                        (request.getConflictScheduleIds() == null || request.getConflictScheduleIds().isEmpty()) &&
                        (request.getConflictTimerIds() == null || request.getConflictTimerIds().isEmpty())) {
                    List<Integer> conflictAwayIds = validateScheduleAway(request.getUuid(), executeStartTime, executeEndTime, request.getRepeat(), startPlusDay, endPlusDay);
                    List<Integer> conflictScheduleIds = validateScheduleSelf(request.getUuid(), deviceSchedule.getId(), executeStartTime, executeEndTime, request.getRepeat(), startPlusDay, endPlusDay, request.getEvent());
                    List<Integer> conflictTimerIds = validateScheduleTimer(request.getUuid(), executeStartTime, executeEndTime, request.getRepeat(), startPlusDay, endPlusDay);
                    if (!conflictAwayIds.isEmpty() || !conflictScheduleIds.isEmpty() || !conflictTimerIds.isEmpty()) {
                        response.setCode(ErrorConstant.ERR_DEVICE_TIMING);
                        response.setMsg(ErrorConstant.ERR_DEVICE_TIMING_MSG);
                        response.setConflictAwayIds(conflictAwayIds);
                        response.setConflictScheduleIds(conflictScheduleIds);
                        response.setConflictTimerIds(conflictTimerIds);
                        return response;
                    } // else 表示 没有冲突
                } else { // 传了冲突的ID过来，表示需要停止冲突timing
                    stopConflictAway(request.getUuid(), request.getConflictAwayIds());
                    stopConflictSchedule(request.getUuid(), request.getConflictScheduleIds());
                    stopConflictTimer(request.getUuid(), request.getConflictTimerIds());
                }
            } else { // 夜灯
                logger.info("验证夜灯时间冲突");
                if (request.getConflictScheduleIds() == null || request.getConflictScheduleIds().isEmpty()) {
                    List<Integer> conflictScheduleIds = validateScheduleSelf(request.getUuid(), deviceSchedule.getId(), executeStartTime, executeEndTime, request.getRepeat(), startPlusDay, endPlusDay, request.getEvent());
                    if (!conflictScheduleIds.isEmpty()) {
                        response.setCode(ErrorConstant.ERR_DEVICE_TIMING);
                        response.setMsg(ErrorConstant.ERR_DEVICE_TIMING_MSG);
                        response.setConflictScheduleIds(conflictScheduleIds);
                        return response;
                    } // else 为没有冲突
                } else {
                    // 传了冲突的ID过来，先停再保存对应的数据
                    stopConflictSchedule(request.getUuid(), request.getConflictScheduleIds());
                }
            }
        }
        DeviceInfo deviceInfo = deviceInfoService.findDeviceByUuid(request.getUuid());
        deviceSchedule.setExecuteEndTime(executeEndTime);
        deviceSchedule.setDeviceCid(deviceInfo.getDeviceCid());
        deviceSchedule.setDeviceId(deviceInfo.getId());
        deviceSchedule.setExecuteEndTime(executeEndTime);
        deviceSchedule.setExecuteStartTime(executeStartTime);
        deviceSchedule.setEndAction(request.getEndState());
        deviceSchedule.setStartTime(requestStartTime);
        deviceSchedule.setEndTime(requestEndTime);
        deviceSchedule.setScheduleRepeat(request.getRepeat());
        deviceSchedule.setStartAction(request.getStartState());
        deviceSchedule.setStatus(request.getScheduleState());
        deviceSchedule.setTimeZone(request.getTimeZone());
        deviceSchedule.setUuid(request.getUuid());
        deviceSchedule.setStartSunTime(startTimeSunTime);
        deviceSchedule.setEndSunTime(endTimeSunTime);
        deviceSchedule.setEvent(request.getEvent());

        // 更新设备schedule
        ScheduleResponse error = new ScheduleResponse();
//        ScheduleResponse error = httpsAPIService.addScheduleToDevice(deviceSchedule, CommonConstant.UPD);
        if (error.getCode() != 0) {
            logger.error("操作设备定时出错");
            throw new ServiceException(error.getCode(), error.getMsg());
        }

        // 更新数据库
        deviceScheduleMapper.updateByPrimaryKey(deviceSchedule);
        // 更新redis
        String redisDeviceScheduleKey = CommonConstant.TIMING_SCHEDULE.concat(":").concat(deviceInfo.getDeviceType()).concat("-").concat(request.getUuid());
        if (redisService.exists(redisDeviceScheduleKey)) {
            List<DeviceSchedule> redisDeviceSchedules = redisService.getList(redisDeviceScheduleKey);
            if (redisDeviceSchedules != null && !redisDeviceSchedules.isEmpty()) {
                for (DeviceSchedule ds : redisDeviceSchedules) {
                    if (ds.getId().intValue() == deviceSchedule.getId().intValue()) {
                        redisService.removeListValue(redisDeviceScheduleKey, ds);
                    }
                }
            }
        }
        redisService.addList(redisDeviceScheduleKey, deviceSchedule);
        // redis zset 移除定时
        if (redisService.exists(CommonConstant.TIMING_SCHEDULE.concat(":").concat(deviceInfo.getDeviceType()))) {
            Set<RedisDeviceSchedule> scheduleSet = (Set<RedisDeviceSchedule>) redisService.getZSetRange(CommonConstant.TIMING_SCHEDULE.concat(":").concat(deviceInfo.getDeviceType()));
            if (scheduleSet != null && !scheduleSet.isEmpty()) {
                for (RedisDeviceSchedule schedule : scheduleSet) {
                    if (schedule.getId().intValue() == deviceSchedule.getId().intValue()) {
                        redisService.removeZSetValue(CommonConstant.TIMING_SCHEDULE.concat(":").concat(deviceInfo.getDeviceType()), schedule);
                    }
                }
            }
        }
        if ("1".equals(request.getScheduleState())) {
            // 添加定时到redis zset
            RedisDeviceSchedule schedule = new RedisDeviceSchedule();
            schedule.setUuid(deviceSchedule.getUuid());
            schedule.setDeviceId(deviceSchedule.getDeviceId());
            schedule.setDeviceCid(deviceSchedule.getDeviceCid());
            schedule.setExecuteStartTime(executeStartTime);
            schedule.setStartAction(deviceSchedule.getStartAction());
            schedule.setScheduleRepeat(deviceSchedule.getScheduleRepeat());
            schedule.setId(deviceSchedule.getId());
            schedule.setType(CommonConstant.START);
            // 如果开始或结束时间比当前时间小，则在时间上加一天
            if (executeStartTime.compareTo(now) <= 0) {
                Calendar calendar = Calendar.getInstance();
                calendar.setTime(executeStartTime);
                calendar.add(Calendar.DAY_OF_YEAR, 1);
                schedule.setExecuteStartTime(calendar.getTime());
            }
            schedule.setMinutes(MyDateUtils.getMinutsByDate(schedule.getExecuteStartTime()));
            redisService.addZSet(CommonConstant.TIMING_SCHEDULE.concat(":").concat(deviceInfo.getDeviceType()), MyDateUtils.getMinutsByDate(schedule.getExecuteStartTime()), schedule);
            if (executeEndTime != null) {
                schedule.setExecuteEndTime(executeEndTime);
                schedule.setType(CommonConstant.END);
                schedule.setEndAction(deviceSchedule.getEndAction());
                // 如果开始或结束时间比当前时间小，则在时间上加一天
                if (executeEndTime.compareTo(now) <= 0) {
                    Calendar calendar = Calendar.getInstance();
                    calendar.setTime(executeEndTime);
                    calendar.add(Calendar.DAY_OF_YEAR, 1);
                    schedule.setExecuteEndTime(calendar.getTime());
                }
                schedule.setMinutes(MyDateUtils.getMinutsByDate(schedule.getExecuteEndTime()));
                redisService.addZSet(CommonConstant.TIMING_SCHEDULE.concat(":").concat(deviceInfo.getDeviceType()), MyDateUtils.getMinutsByDate(schedule.getExecuteEndTime()), schedule);
            }
        }
        redisService.unlock(lockKey);
        return response;
    }

    @Override
    public ScheduleResponse updateScheduleState(ScheduleRequest request) throws Exception {
        ScheduleResponse response = new ScheduleResponse();
        // 验证schedule存在性
        DeviceSchedule deviceSchedule = deviceScheduleMapper.selectByPrimaryKey(Integer.valueOf(request.getScheduleId()));
        if (deviceSchedule == null || deviceSchedule.getId() == null) {
            response.setCode(ErrorConstant.ERR_DEVICE_SCHEDULE_NOT_EXIST);
            response.setMsg(ErrorConstant.ERR_DEVICE_SCHEDULE_NOT_EXIST_MSG);
            logger.error(response.getMsg());
            return response;
        }

        String lockKey = CommonConstant.DEVICE_SCHEDULE_LOCK_PREFIX + "sta-" + deviceSchedule.getId();
        redisService.lock(lockKey);


        Date executeStartTime = MyDateUtils.getUtcDateByTimeAndZone(deviceSchedule.getStartTime(), request.getTimeZone());
        Date executeEndTime = MyDateUtils.getUtcDateByTimeAndZone(deviceSchedule.getEndTime(), request.getTimeZone());
        int startPlusDay = MyDateUtils.getPlusDay(deviceSchedule.getStartTime(), request.getTimeZone());
        int endPlusDay = MyDateUtils.getPlusDay(deviceSchedule.getEndTime(), request.getTimeZone());
        Date now = MyDateUtils.getUtcDateTime();

        if ("7".equals(deviceSchedule.getScheduleRepeat())) {
            if (executeStartTime.getTime() / 1000L < now.getTime() / 1000L) { // 开始时间当前时间小
                Calendar start = Calendar.getInstance();
                start.setTime(executeStartTime);
                start.add(Calendar.DAY_OF_YEAR, 1);
                executeStartTime = start.getTime();

                if (executeEndTime != null) {
                    Calendar end = Calendar.getInstance();
                    end.setTime(executeEndTime);
                    end.add(Calendar.DAY_OF_YEAR, 1);
                    executeEndTime = end.getTime();
                }
            }
        }

        // 结束时间比开始时间小，则结束时间加一天
        if (executeEndTime != null && executeEndTime.before(executeStartTime)) {
            executeEndTime = MyDateUtils.addDate(executeEndTime, Calendar.DAY_OF_YEAR, 1);
        }

        if ("1".equals(request.getScheduleState())) { // Schedule开始状态
            // 把状态改为Start(1)的时间
            deviceSchedule.setTurnonTime(MyDateUtils.getUtcDateTime());

            if (CommonConstant.EVENT_SWITCH.equals(deviceSchedule.getEvent()) || MyStringUtils.isNullData(deviceSchedule.getEvent())) { // 插座shcedule
                // 全为空，需要验证是否有冲突
                logger.info("验证插座时间冲突");
                if ((request.getConflictAwayIds() == null || request.getConflictAwayIds().isEmpty()) &&
                        (request.getConflictScheduleIds() == null || request.getConflictScheduleIds().isEmpty()) &&
                        (request.getConflictTimerIds() == null || request.getConflictTimerIds().isEmpty())) {
                    List<Integer> conflictAwayIds = validateScheduleAway(deviceSchedule.getUuid(), executeStartTime, executeEndTime, deviceSchedule.getScheduleRepeat(), startPlusDay, endPlusDay);
                    List<Integer> conflictScheduleIds = validateScheduleSelf(deviceSchedule.getUuid(), deviceSchedule.getId(), executeStartTime, executeEndTime, deviceSchedule.getScheduleRepeat(), startPlusDay, endPlusDay, deviceSchedule.getEvent());
                    List<Integer> conflictTimerIds = validateScheduleTimer(deviceSchedule.getUuid(), executeStartTime, executeEndTime, deviceSchedule.getScheduleRepeat(), startPlusDay, endPlusDay);
                    if (!conflictAwayIds.isEmpty() || !conflictScheduleIds.isEmpty() || !conflictTimerIds.isEmpty()) {
                        response.setCode(ErrorConstant.ERR_DEVICE_TIMING);
                        response.setMsg(ErrorConstant.ERR_DEVICE_TIMING_MSG);
                        response.setConflictAwayIds(conflictAwayIds);
                        response.setConflictScheduleIds(conflictScheduleIds);
                        response.setConflictTimerIds(conflictTimerIds);
                        return response;
                    } // else 表示 没有冲突
                } else { // 传了冲突的ID过来，表示需要停止冲突timing
                    stopConflictAway(deviceSchedule.getUuid(), request.getConflictAwayIds());
                    stopConflictSchedule(deviceSchedule.getUuid(), request.getConflictScheduleIds());
                    stopConflictTimer(deviceSchedule.getUuid(), request.getConflictTimerIds());
                }
            } else { // 夜灯
                logger.info("验证夜灯时间冲突");
                if (request.getConflictScheduleIds() == null || request.getConflictScheduleIds().isEmpty()) {
                    List<Integer> conflictScheduleIds = validateScheduleSelf(deviceSchedule.getUuid(), deviceSchedule.getId(), executeStartTime, executeEndTime, deviceSchedule.getScheduleRepeat(), startPlusDay, endPlusDay, deviceSchedule.getEvent());
                    if (!conflictScheduleIds.isEmpty()) {
                        response.setCode(ErrorConstant.ERR_DEVICE_TIMING);
                        response.setMsg(ErrorConstant.ERR_DEVICE_TIMING_MSG);
                        response.setConflictScheduleIds(conflictScheduleIds);
                        return response;
                    } // else 为没有冲突
                } else {
                    // 传了冲突的ID过来，先停再保存对应的数据
                    stopConflictSchedule(deviceSchedule.getUuid(), request.getConflictScheduleIds());
                }
            }
        }

        deviceSchedule.setStatus(request.getScheduleState());
        deviceSchedule.setExecuteStartTime(executeStartTime);
        deviceSchedule.setExecuteEndTime(executeEndTime);

        // 更新设备schedule
        ScheduleResponse error = new ScheduleResponse();
//        ScheduleResponse error = httpsAPIService.addScheduleToDevice(deviceSchedule, CommonConstant.UPD);
        if (error.getCode() != 0) {
            logger.error("操作设备定时出错");
            throw new ServiceException(error.getCode(), error.getMsg());
        }
        DeviceInfo deviceInfo = deviceInfoService.findDeviceByUuid(request.getUuid());
        deviceScheduleMapper.updateByPrimaryKey(deviceSchedule);
        // update redis
        String redisDeviceScheduleKey = CommonConstant.TIMING_SCHEDULE.concat(":").concat(deviceInfo.getDeviceType()).concat("-").concat(deviceSchedule.getUuid());
        if (redisService.exists(redisDeviceScheduleKey)) {
            List<DeviceSchedule> redisDeviceSchedules = redisService.getList(redisDeviceScheduleKey);
            if (redisDeviceSchedules != null && !redisDeviceSchedules.isEmpty()) {
                for (DeviceSchedule ds : redisDeviceSchedules) {
                    if (ds.getId().intValue() == deviceSchedule.getId().intValue()) {
                        redisService.removeListValue(redisDeviceScheduleKey, ds);
                        break;
                    }
                }
            }
        }
        redisService.addList(redisDeviceScheduleKey, deviceSchedule);
        // redis zset 移除定时
        if (redisService.exists(CommonConstant.TIMING_SCHEDULE.concat(":").concat(deviceInfo.getDeviceType()))) {
            Set<RedisDeviceSchedule> scheduleSet = (Set<RedisDeviceSchedule>) redisService.getZSetRange(CommonConstant.TIMING_SCHEDULE.concat(":").concat(deviceInfo.getDeviceType()));
            if (scheduleSet != null && !scheduleSet.isEmpty()) {
                for (RedisDeviceSchedule schedule : scheduleSet) {
                    if (schedule.getId().intValue() == deviceSchedule.getId().intValue()) {
                        redisService.removeZSetValue(CommonConstant.TIMING_SCHEDULE.concat(":").concat(deviceInfo.getDeviceType()), schedule);
                    }
                }
            }
        }
        if ("1".equals(request.getScheduleState())) {
            // 添加定时到redis zset
            // Date executeStartTime = MyDateUtils.convertToUTCDate(MyDateUtils.dateToString(deviceSchedule.getExecutestarttime()), "UTC", MyDateUtils.TIME_HH_MM);
            // Date executeEndTime = MyDateUtils.convertToUTCDate(MyDateUtils.dateToString(deviceSchedule.getExecuteendtime()), "UTC", MyDateUtils.TIME_HH_MM);

            RedisDeviceSchedule schedule = new RedisDeviceSchedule();
            schedule.setUuid(deviceSchedule.getUuid());
            schedule.setExecuteStartTime(executeStartTime);
            schedule.setStartAction(deviceSchedule.getStartAction());
            schedule.setScheduleRepeat(deviceSchedule.getScheduleRepeat());
            schedule.setId(deviceSchedule.getId());
            schedule.setType(CommonConstant.START);
            // 如果开始或结束时间比当前时间小，则在时间上加一天
            if (executeStartTime.before(MyDateUtils.getUtcDateTime())) {
                Calendar calendar = Calendar.getInstance();
                calendar.setTime(executeStartTime);
                calendar.add(Calendar.DAY_OF_YEAR, 1);
                schedule.setExecuteStartTime(calendar.getTime());
            }
            schedule.setMinutes(MyDateUtils.getMinutsByDate(schedule.getExecuteStartTime()));
            redisService.addZSet(CommonConstant.TIMING_SCHEDULE.concat(":").concat(deviceInfo.getDeviceType()), MyDateUtils.getMinutsByDate(schedule.getExecuteStartTime()), schedule);
            if (executeEndTime != null) {
                schedule.setExecuteEndTime(executeEndTime);
                schedule.setType(CommonConstant.END);
                schedule.setEndAction(deviceSchedule.getEndAction());
                // 如果开始或结束时间比当前时间小，则在时间上加一天
                if (executeEndTime.before(MyDateUtils.getUtcDateTime())) {
                    Calendar calendar = Calendar.getInstance();
                    calendar.setTime(executeEndTime);
                    calendar.add(Calendar.DAY_OF_YEAR, 1);
                    schedule.setExecuteEndTime(calendar.getTime());
                }
                schedule.setMinutes(MyDateUtils.getMinutsByDate(schedule.getExecuteEndTime()));
                redisService.addZSet(CommonConstant.TIMING_SCHEDULE.concat(":").concat(deviceInfo.getDeviceType()), MyDateUtils.getMinutsByDate(schedule.getExecuteEndTime()), schedule);
            }
        }
        redisService.unlock(lockKey);
        return response;
    }

    @Override
    public ScheduleResponse getSchedules(ScheduleRequest request) {
        ScheduleResponse response = new ScheduleResponse();
        String uuid = request.getUuid();
        DeviceInfo deviceInfo = deviceInfoService.findDeviceByUuid(request.getUuid());
        String lockKey = CommonConstant.DEVICE_SCHEDULE_LOCK_PREFIX + "get-" + uuid;
        redisService.lock(lockKey);

        List<DeviceSchedule> deviceSchedules = new ArrayList<>();
        List<DeviceScheduleView> re = new ArrayList<>();
        String redisDeviceScheduleKey = CommonConstant.TIMING_SCHEDULE.concat(":").concat(deviceInfo.getDeviceType()).concat("-").concat(uuid);
        // 读redis
        if (redisService.exists(redisDeviceScheduleKey)) {
            deviceSchedules = redisService.getList(redisDeviceScheduleKey);
        }
        if (deviceSchedules == null || deviceSchedules.isEmpty()) {
            deviceSchedules = deviceScheduleMapper.selectByUuid(uuid);
            if (deviceSchedules != null && !deviceSchedules.isEmpty()) {
                // 写入redis
                redisService.addList(redisDeviceScheduleKey, deviceSchedules);
                redisService.setExpireTime(redisDeviceScheduleKey, CommonConstant.SECONDS_OF_ONEDAY);
            }
        }
        if (deviceSchedules == null || deviceSchedules.isEmpty()) {
            response.setCode(ErrorConstant.ERR_DEVICE_SCHEDULE);
            response.setMsg(ErrorConstant.ERR_DEVICE_SCHEDULE_MSG);
            logger.info("无此设备相关schedule");
            return response;
        }
        // 去重
        deviceSchedules = deviceSchedules.stream().distinct().collect(Collectors.toList());
        // 封装返回数据
        List<DeviceSchedule> schedules = new ArrayList<>();
        // 按照schedule startTime升序排序
        deviceSchedules.sort(Comparator.comparing(DeviceSchedule::getStartTime));
        for (DeviceSchedule deviceSchedule : deviceSchedules) {
            DeviceScheduleView schedule = new DeviceScheduleView();
            schedule.setId(deviceSchedule.getId());
            schedule.setDeviceName(deviceInfo.getDeviceName());
            schedule.setEndAction(deviceSchedule.getEndAction());
            // 时间转换
            if (MyStringUtils.isNullData(deviceSchedule.getStartSunTime())) {
                schedule.setStartTime(MyDateUtils.getTimeStringByZone(deviceSchedule.getStartTime(), deviceSchedule.getTimeZone(), request.getTimeZone()));
            } else {
                schedule.setStartTime(deviceSchedule.getStartSunTime());
            }
            if (MyStringUtils.isNullData(deviceSchedule.getEndSunTime())) {
                schedule.setEndTime(MyDateUtils.getTimeStringByZone(deviceSchedule.getEndTime(), deviceSchedule.getTimeZone(), request.getTimeZone()));
            } else {
                schedule.setEndTime(deviceSchedule.getEndSunTime());
            }
            schedule.setScheduleRepeat(deviceSchedule.getScheduleRepeat());
            schedule.setExecuteStartTime(deviceSchedule.getExecuteStartTime());
            schedule.setExecuteEndTime(deviceSchedule.getExecuteEndTime());
            schedule.setId(deviceSchedule.getId());
            schedule.setStartAction(deviceSchedule.getStartAction());
            schedule.setStatus(deviceSchedule.getStatus());
            schedule.setTimeZone(deviceSchedule.getTimeZone());
            schedule.setUuid(deviceSchedule.getUuid());
            schedule.setEvent(deviceSchedule.getEvent());
            schedule.setTurnonTime(deviceSchedule.getTurnonTime());
            Date executeStartTime = MyDateUtils.getUtcDateByTimeAndZone(deviceSchedule.getStartTime(), deviceSchedule.getTimeZone());
            // tomorrow
            if ("7".equals(deviceSchedule.getScheduleRepeat())) {
                if (MyDateUtils.calcTomorrow(executeStartTime, deviceSchedule.getTurnonTime())) {
                    schedule.setOnceTomorrow("1");
                }
            }
            // next day
            if (!MyStringUtils.isNullData(deviceSchedule.getEndTime()) && MyDateUtils.compareTime(deviceSchedule.getStartTime(), deviceSchedule.getEndTime())) {
                schedule.setNextDay("1");
            }
            re.add(schedule);
        }
        response.setSchedules(re);
        redisService.unlock(lockKey);
        return response;
    }

    @Override
    public ScheduleResponse stopScheduleByPrimaryKey(Integer id) {
        ScheduleResponse response = new ScheduleResponse();
        // 验证schedule存在性
        DeviceSchedule deviceSchedule = deviceScheduleMapper.selectByPrimaryKey(id);
        if (deviceSchedule == null || deviceSchedule.getId() == null) {
            response.setCode(ErrorConstant.ERR_DEVICE_SCHEDULE_NOT_EXIST);
            response.setMsg(ErrorConstant.ERR_DEVICE_SCHEDULE_NOT_EXIST_MSG);
            logger.error(response.getMsg());
            return response;
        }
        deviceScheduleMapper.updateStatusByIds(Arrays.asList(id), "0");
        return response;
    }

    @Override
    public ScheduleResponse updateUuidByUuid(String oldUuid, String newUuid) {
        ScheduleResponse response = new ScheduleResponse();
        DeviceInfo deviceInfo = deviceInfoService.findDeviceByUuid(oldUuid);
        String redisKey = CommonConstant.TIMING_SCHEDULE.concat(":").concat(deviceInfo.getDeviceType()).concat("-").concat(oldUuid);
        if (redisService.exists(redisKey)) {
            redisService.remove(redisKey);
        }
        Set<RedisDeviceSchedule> redisAwaySet = null;
        if (redisService.exists(CommonConstant.TIMING_SCHEDULE.concat(":").concat(deviceInfo.getDeviceType()))) {
            redisAwaySet = (Set<RedisDeviceSchedule>) redisService.getZSetRange(CommonConstant.TIMING_SCHEDULE.concat(":").concat(deviceInfo.getDeviceType()));
        }
        if (redisAwaySet != null && !redisAwaySet.isEmpty()) {
            for (RedisDeviceSchedule redisAway : redisAwaySet) {
                if (redisAway.getUuid().equals(oldUuid)) {
                    redisService.removeZSetValue(CommonConstant.TIMING_SCHEDULE.concat(":").concat(deviceInfo.getDeviceType()), redisAway);
                    redisAway.setUuid(newUuid);
                    redisService.addZSet(CommonConstant.TIMING_SCHEDULE.concat(":").concat(deviceInfo.getDeviceType()), MyDateUtils.getMinutsByDate(redisAway.getExecuteStartTime() == null
                            ? redisAway.getExecuteEndTime() : redisAway.getExecuteStartTime()), redisAway);
                }
            }
        }

        deviceScheduleMapper.updateUuidByUuid(oldUuid, newUuid);

        return response;
    }

    @Override
    public ScheduleResponse deleteScheduleByUUID(String uuid) {
        ScheduleResponse response = new ScheduleResponse();
        DeviceInfo deviceInfo = deviceInfoService.findDeviceByUuid(uuid);
        List<DeviceSchedule> startedSchedules = deviceScheduleMapper.selectByUuid(uuid);
        deviceScheduleMapper.deleteByUuid(uuid);
        // delete data in redis
        String redisScheduleKey = CommonConstant.TIMING_SCHEDULE.concat(":").concat(deviceInfo.getDeviceType()).concat("-").concat(uuid);
        if (redisService.exists(redisScheduleKey)) {
            redisService.remove(redisScheduleKey);
        }
        // 删除 redis 中 正在执行的 zset
        Set<RedisDeviceSchedule> scheduleSet = null;
        if (redisService.exists(CommonConstant.TIMING_SCHEDULE.concat(":").concat(deviceInfo.getDeviceType()))) {
            scheduleSet = (Set<RedisDeviceSchedule>) redisService.getZSetRange(CommonConstant.TIMING_SCHEDULE.concat(":").concat(deviceInfo.getDeviceType()));
        }
        if (startedSchedules != null && !startedSchedules.isEmpty() && scheduleSet != null && !scheduleSet.isEmpty()) {
            for (DeviceSchedule startedSchedule : startedSchedules) {
                // redis zset 移除定时
                for (RedisDeviceSchedule schedule : scheduleSet) {
                    if (schedule.getId().intValue() == startedSchedule.getId().intValue()) {
                        redisService.removeZSetValue(CommonConstant.TIMING_SCHEDULE.concat(":").concat(deviceInfo.getDeviceType()), schedule);
                    }
                }
            }
        }
        return response;
    }


    private List<Integer> validateScheduleAway(String uuid, Date startTime, Date endTime, String repeat, int startPlusDay, int endPlusDay) {
        List<Integer> awayIds = new ArrayList<>();
        // 和启动的Away冲突验证
        List<DeviceAway> startAways = deviceAwayMapper.selectByUuid(uuid);
        DeviceAway startAway = null;
        if (startAways != null && !startAways.isEmpty()) {
            startAway = startAways.get(0);
            String[] dbRepeats = startAway.getAwayRepeat().split(",");
            String[] currRepeats = repeat.split(",");
            for (String dbRepeat : dbRepeats) {
                String start = startAway.getStartTime();
                String end = startAway.getEndTime();
                Date dbStartTime = MyDateUtils.getUtcDateByTimeAndZone(start, startAway.getTimeZone());
                Date dbEndTime = MyDateUtils.getUtcDateByTimeAndZone(end, startAway.getTimeZone());
                int dbStartPlusDay = MyDateUtils.getPlusDay(start, startAway.getTimeZone());
                int dbEndPlusDay = MyDateUtils.getPlusDay(end, startAway.getTimeZone());
                // 更换日期时间
                if ("7".equals(dbRepeat)) {
                    // 时间转换为设置状态为"1"那天的时间
                    dbStartTime = MyDateUtils.getUtcDateByUtcDateAndSetDateAndWeek(dbStartTime, startAway.getTurnonTime(), null);
                    dbEndTime = MyDateUtils.getUtcDateByUtcDateAndSetDateAndWeek(dbEndTime, startAway.getTurnonTime(), null);
                    if (dbStartTime.before(startAway.getTurnonTime())) {
                        dbStartTime = MyDateUtils.addDate(dbStartTime, Calendar.DAY_OF_YEAR, 1);
                        if (dbEndTime != null) {
                            dbEndTime = MyDateUtils.addDate(dbEndTime, Calendar.DAY_OF_YEAR, 1);
                        }
                    }
                } else {
                    // 根据repeat设置对应的时间
                    dbStartTime = MyDateUtils.getUtcDateByUtcDateAndSetDateAndWeek(dbStartTime, startAway.getTurnonTime(), dbRepeat);
                    dbEndTime = MyDateUtils.getUtcDateByUtcDateAndSetDateAndWeek(dbEndTime, startAway.getTurnonTime(), dbRepeat);
                    dbStartTime = MyDateUtils.addDate(dbStartTime, Calendar.DAY_OF_YEAR, dbStartPlusDay);
                    dbEndTime = MyDateUtils.addDate(dbEndTime, Calendar.DAY_OF_YEAR, dbEndPlusDay);
                }
                if (dbEndTime != null && dbEndTime.getTime() / 1000L < dbStartTime.getTime() / 1000L) {
                    Calendar calendar = Calendar.getInstance();
                    calendar.setTime(dbEndTime);
                    calendar.add(Calendar.DAY_OF_YEAR, 1);
                    dbEndTime = calendar.getTime();
                }
                long minutes = MyDateUtils.compareMinute(start, end);
                if (minutes > 60L && minutes < 1380L) {
                    dbStartTime = MyDateUtils.addDate(dbStartTime, Calendar.MINUTE, -30);
                    dbEndTime = MyDateUtils.addDate(dbEndTime, Calendar.MINUTE, 30);
                } else if (minutes <= 60L) {
                    dbStartTime = MyDateUtils.addDate(dbStartTime, Calendar.MINUTE, -5);
                    dbEndTime = MyDateUtils.addDate(dbEndTime, Calendar.MINUTE, 5);
                }
                // schedule
                for (String currRepeat : currRepeats) {
                    // 更换日期时间
                    if ("7".equals(currRepeat)) {
                        // 时间转换为设置状态为"1"那天的时间 可以不变
                        startTime = MyDateUtils.getUtcDateByUtcDateAndWeek(startTime, null);
                        endTime = MyDateUtils.getUtcDateByUtcDateAndWeek(endTime, null);
                    } else {
                        // 根据repeat设置对应的时间
                        startTime = MyDateUtils.getUtcDateByUtcDateAndWeek(startTime, currRepeat);
                        endTime = MyDateUtils.getUtcDateByUtcDateAndWeek(endTime, currRepeat);
                        startTime = MyDateUtils.addDate(startTime, Calendar.DAY_OF_YEAR, startPlusDay);
                        endTime = MyDateUtils.addDate(endTime, Calendar.DAY_OF_YEAR, endPlusDay);
                    }
                    if (endTime != null && endTime.getTime() / 1000L < startTime.getTime() / 1000L) {
                        Calendar calendar = Calendar.getInstance();
                        calendar.setTime(endTime);
                        calendar.add(Calendar.DAY_OF_YEAR, 1);
                        endTime = calendar.getTime();
                    }
                    if (endTime == null) { // 单向schedule
                        // 这个单点时间在另一组时间范围内
                        if (MyDateUtils.compareTimeLine(dbStartTime, dbEndTime, startTime)) {
                            awayIds.add(startAway.getId());
                        }
                    } else {
                        // 判断时间线
                        if (MyDateUtils.compareTimeLine(startTime, endTime, dbStartTime, dbEndTime)) {
                            awayIds.add(startAway.getId());
                        }
                    }
                }
            }
        }
        return awayIds.stream().distinct().collect(Collectors.toList());
    }

    private List<Integer> validateScheduleSelf(String uuid, Integer scheduleId, Date startTime, Date endTime, String repeat, int startPlusDay, int endPlusDay, String event) {
        List<Integer> scheduleIds = new ArrayList<>();
        DeviceSchedule query = new DeviceSchedule();
        query.setUuid(uuid);
        query.setEvent(event);
        query.setStatus("1");
        List<DeviceSchedule> startDeviceSchedules = deviceScheduleMapper.select(query);
        // 启动前验证需要排除自己
        if (!Objects.isNull(scheduleId)) {
            startDeviceSchedules.removeIf(schedule -> schedule.getId().intValue() == scheduleId.intValue());
        }
        String[] currRepeats = repeat.split(",");
        for (DeviceSchedule startSchedule : startDeviceSchedules) {
            String[] dbRepeats = startSchedule.getScheduleRepeat().split(",");
            int dbStartPlusDay = MyDateUtils.getPlusDay(startSchedule.getStartTime(), startSchedule.getTimeZone());
            int dbEndPlusDay = MyDateUtils.getPlusDay(startSchedule.getEndTime(), startSchedule.getTimeZone());
            for (String dbRepeat : dbRepeats) {
                Date dbStartTime;
                Date dbEndTime;
                // 更换日期时间
                if ("7".equals(dbRepeat)) {
                    // 时间转换为设置状态为"1"那天的时间
                    dbStartTime = MyDateUtils.getUtcDateByUtcDateAndSetDateAndWeek(startSchedule.getExecuteStartTime(), startSchedule.getTurnonTime(), null);
                    dbEndTime = MyDateUtils.getUtcDateByUtcDateAndSetDateAndWeek(startSchedule.getExecuteEndTime(), startSchedule.getTurnonTime(), null);
                    if (dbStartTime.before(startSchedule.getTurnonTime())) {
                        dbStartTime = MyDateUtils.addDate(dbStartTime, Calendar.DAY_OF_YEAR, 1);
                        if (dbEndTime != null) {
                            dbEndTime = MyDateUtils.addDate(dbEndTime, Calendar.DAY_OF_YEAR, 1);
                        }
                    }
                } else {
                    // 根据repeat设置对应的时间
                    dbStartTime = MyDateUtils.getUtcDateByUtcDateAndSetDateAndWeek(startSchedule.getExecuteStartTime(), startSchedule.getTurnonTime(), dbRepeat);
                    dbEndTime = MyDateUtils.getUtcDateByUtcDateAndSetDateAndWeek(startSchedule.getExecuteEndTime(), startSchedule.getTurnonTime(), dbRepeat);
                    dbStartTime = MyDateUtils.addDate(dbStartTime, Calendar.DAY_OF_YEAR, dbStartPlusDay);
                    dbEndTime = MyDateUtils.addDate(dbEndTime, Calendar.DAY_OF_YEAR, dbEndPlusDay);
                }
                if (dbEndTime != null && dbEndTime.getTime() / 1000L < dbStartTime.getTime() / 1000L) {
                    Calendar calendar = Calendar.getInstance();
                    calendar.setTime(dbEndTime);
                    calendar.add(Calendar.DAY_OF_YEAR, 1);
                    dbEndTime = calendar.getTime();
                }
                for (String currRepeat : currRepeats) {
                    // 更换日期时间
                    if ("7".equals(currRepeat)) {
                        // 时间转换为设置状态为"1"那天的时间
                        startTime = MyDateUtils.getUtcDateByUtcDateAndWeek(startTime, null);
                        endTime = MyDateUtils.getUtcDateByUtcDateAndWeek(endTime, null);
                    } else {
                        // 根据repeat设置对应的时间
                        startTime = MyDateUtils.getUtcDateByUtcDateAndWeek(startTime, currRepeat);
                        endTime = MyDateUtils.getUtcDateByUtcDateAndWeek(endTime, currRepeat);
                        startTime = MyDateUtils.addDate(startTime, Calendar.DAY_OF_YEAR, startPlusDay);
                        endTime = MyDateUtils.addDate(endTime, Calendar.DAY_OF_YEAR, endPlusDay);
                    }
                    if (endTime != null && endTime.getTime() / 1000L < startTime.getTime() / 1000L) {
                        Calendar calendar = Calendar.getInstance();
                        calendar.setTime(endTime);
                        calendar.add(Calendar.DAY_OF_YEAR, 1);
                        endTime = calendar.getTime();
                    }
                    if (endTime == null && dbEndTime == null) {
                        // 单向Schedule开始时间相等，即冲突
                        if (MyDateUtils.compareTimeLine(startTime, dbStartTime)) {
                            scheduleIds.add(startSchedule.getId());
                        }
                    } else if (endTime == null && dbEndTime != null) {
                        // 验证的是单向Schedule,库里为双向Schedule
                        if (MyDateUtils.compareTimeLine(dbStartTime, dbEndTime, startTime)) {
                            scheduleIds.add(startSchedule.getId());
                        }
                    } else if (endTime != null && dbEndTime == null) {
                        // 验证的是双向Schedule,库里为单向Schedule
                        if (MyDateUtils.compareTimeLine(dbStartTime, dbEndTime, startTime)) {
                            scheduleIds.add(startSchedule.getId());
                        }
                    } else { // 两个都不为空
                        // 判断时间线
                        if (MyDateUtils.compareTimeLine(startTime, endTime, dbStartTime, dbEndTime)) {
                            scheduleIds.add(startSchedule.getId());
                        }
                    }
                }
            }
        }
        return scheduleIds.stream().distinct().collect(Collectors.toList());

    }

    private List<Integer> validateScheduleTimer(String uuid, Date startTime, Date endTime, String repeat, int startPlusDay, int endPlusDay) {
        List<Integer> timerIds = new ArrayList<>();
        String[] currRepeats = repeat.split(",");
        DeviceTimer query = new DeviceTimer();
        query.setUuid(uuid);
        query.setStatus("1");
        List<DeviceTimer> startedTimers = deviceTimerMapper.select(query);
        for (DeviceTimer conflictScheduleTimer : startedTimers) {
            Date dbEndTime = conflictScheduleTimer.getExecuteTime();
            for (String currRepeat : currRepeats) {
                // 更换日期时间
                if ("7".equals(currRepeat)) {
                    // 时间转换为设置状态为"1"那天的时间
                    startTime = MyDateUtils.getUtcDateByUtcDateAndWeek(startTime, null);
                    endTime = MyDateUtils.getUtcDateByUtcDateAndWeek(endTime, null);
                } else {
                    // 根据repeat设置对应的时间
                    startTime = MyDateUtils.getUtcDateByUtcDateAndWeek(startTime, currRepeat);
                    endTime = MyDateUtils.getUtcDateByUtcDateAndWeek(endTime, currRepeat);
                    startTime = MyDateUtils.addDate(startTime, Calendar.DAY_OF_YEAR, startPlusDay);
                    endTime = MyDateUtils.addDate(endTime, Calendar.DAY_OF_YEAR, endPlusDay);
                }
                if (endTime != null && endTime.getTime() / 1000L < startTime.getTime() / 1000L) {
                    Calendar calendar = Calendar.getInstance();
                    calendar.setTime(endTime);
                    calendar.add(Calendar.DAY_OF_YEAR, 1);
                    endTime = calendar.getTime();
                }
                if (endTime == null) {
                    // 存入的为单向schedule
                    if (MyDateUtils.compareTimeLine(dbEndTime, startTime)) {
                        timerIds.add(conflictScheduleTimer.getId());
                    }
                } else {
                    // 双向schedule
                    if (MyDateUtils.compareTimeLine(startTime, endTime, dbEndTime)) {
                        timerIds.add(conflictScheduleTimer.getId());
                    }
                }
            }
        }
        return timerIds.stream().distinct().collect(Collectors.toList());

    }

    private void stopConflictAway(String uuid, List<Integer> awayIds) {
        if (awayIds != null && !awayIds.isEmpty()) {
            List<DeviceAway> startAways = deviceAwayMapper.selectByPrimaryKeys(awayIds);
            String redisDeviceTimerKey = CommonConstant.TIMING_AWAY.concat("-").concat(uuid);
            for (DeviceAway startAway : startAways) {
                // update away
                startAway.setStatus("0");
                deviceAwayMapper.updateByPrimaryKeySelective(startAway);
                // update redis
                if (redisService.exists(redisDeviceTimerKey)) {
                    redisService.remove(redisDeviceTimerKey);
                    redisService.set(redisDeviceTimerKey, startAway);
                }
                // 更新 zset in redis
                if (redisService.exists(CommonConstant.TIMING_AWAY)) {
                    Set<RedisDeviceAway> redisAways = (Set<RedisDeviceAway>) redisService.getZSetRange(CommonConstant.TIMING_AWAY);
                    if (redisAways != null && !redisAways.isEmpty()) {
                        for (RedisDeviceAway redisAway : redisAways) {
                            if (startAway.getId().intValue() == redisAway.getId().intValue()) {
                                redisService.removeZSetValue(CommonConstant.TIMING_AWAY, redisAway);
                            }
                        }
                    }
                }
            }
        }
    }

    private void stopConflictSchedule(String uuid, List<Integer> scheduleIds) throws Exception {
        if (scheduleIds != null && !scheduleIds.isEmpty()) {
            List<DeviceSchedule> conflictSchedules = deviceScheduleMapper.selectByPrimaryKeys(scheduleIds);
            Iterator<DeviceSchedule> scheduleIterator = conflictSchedules.iterator();
            while (scheduleIterator.hasNext()) {
                DeviceSchedule schedule = scheduleIterator.next();
                schedule.setStatus("0");
            }
            DeviceInfo deviceInfo = deviceInfoService.findDeviceByUuid(uuid);
            // stop schedule in db
            deviceScheduleMapper.updateStatusByIds(scheduleIds, "0");
            // update redis
            String redisDeviceScheduleKey = CommonConstant.TIMING_SCHEDULE.concat(":").concat(deviceInfo.getDeviceType()).concat("-").concat(uuid);
            if (redisService.exists(redisDeviceScheduleKey)) {
                List<DeviceSchedule> redisDeviceSchedules = redisService.getList(redisDeviceScheduleKey);
                if (redisDeviceSchedules != null && !redisDeviceSchedules.isEmpty()) {
                    for (DeviceSchedule redisDeviceSchedule : redisDeviceSchedules) {
                        for (DeviceSchedule conflictSchedule : conflictSchedules) {
                            if (redisDeviceSchedule.getId().intValue() == conflictSchedule.getId().intValue()) {
                                redisService.removeListValue(redisDeviceScheduleKey, redisDeviceSchedule);
                            }
                        }
                    }
                }
            }
            // 更新后的schedule 重新加入到redis
            redisService.addList(redisDeviceScheduleKey, conflictSchedules);
            // remove timing zset
            if (redisService.exists(CommonConstant.TIMING_SCHEDULE.concat(":").concat(deviceInfo.getDeviceType()))) {
                Set<RedisDeviceSchedule> redisDeviceScheuleSet = (Set<RedisDeviceSchedule>) redisService.getZSetRange(CommonConstant.TIMING_SCHEDULE.concat(":").concat(deviceInfo.getDeviceType()));
                if (redisDeviceScheuleSet != null && !redisDeviceScheuleSet.isEmpty()) {
                    for (RedisDeviceSchedule redisDeviceScheule : redisDeviceScheuleSet) {
                        for (DeviceSchedule conflictSchedule : conflictSchedules) {
                            if (redisDeviceScheule.getId().intValue() == conflictSchedule.getId().intValue()) {
                                redisService.removeZSetValue(CommonConstant.TIMING_SCHEDULE.concat(":").concat(deviceInfo.getDeviceType()), redisDeviceScheule);
                            }
                        }
                    }
                }
            }
            for (DeviceSchedule schedule : conflictSchedules) {
                Date executeStartTime = MyDateUtils.getUtcDateByTimeAndZone(schedule.getStartTime(), schedule.getTimeZone());
                Date executeEndTime = MyDateUtils.getUtcDateByTimeAndZone(schedule.getEndTime(), schedule.getTimeZone());

                // 结束时间比开始时间小，则结束时间加一天
                if (executeEndTime != null && executeEndTime.before(executeStartTime)) {
                    Calendar c = GregorianCalendar.getInstance();
                    c.setTime(executeEndTime);
                    c.add(Calendar.DAY_OF_YEAR, 1);
                    executeEndTime = c.getTime();
                }

                schedule.setExecuteEndTime(executeEndTime);
                schedule.setExecuteStartTime(executeStartTime);
                schedule.setStatus("0");
                // 停设备里的schedule
                httpsAPIService.addScheduleToDevice(schedule, CommonConstant.UPD);
            }
        }
    }

    private void stopConflictTimer(String uuid, List<Integer> timerIds) throws Exception {
        if (timerIds != null && !timerIds.isEmpty()) {
            List<DeviceTimer> conflictTimers = deviceTimerMapper.selectByPrimaryKeys(timerIds);
            Iterator<DeviceTimer> iterator = conflictTimers.iterator();
            while (iterator.hasNext()) {
                DeviceTimer timer = iterator.next();
                timer.setStatus("0");
            }
            // stop timer in db
            deviceTimerMapper.updateStatusByIds(timerIds, "0");
            // update redis
            String redisDeviceTimerKey = CommonConstant.TIMING_TIMER.concat("-").concat(uuid);
            if (redisService.exists(redisDeviceTimerKey)) {
                List<DeviceTimer> redisDeviceTimers = redisService.getList(redisDeviceTimerKey);
                if (redisDeviceTimers != null && !redisDeviceTimers.isEmpty()) {
                    for (DeviceTimer redisDeviceTimer : redisDeviceTimers) {
                        for (DeviceTimer conflictTimer : conflictTimers) {
                            if (redisDeviceTimer.getId().intValue() == conflictTimer.getId().intValue()) {
                                redisService.removeListValue(redisDeviceTimerKey, redisDeviceTimer);
                            }
                        }
                    }
                }
            }
            // 更新后的timer 重写入redis
            redisService.addList(redisDeviceTimerKey, conflictTimers);
            // update zset
            if (redisService.exists(CommonConstant.TIMING_TIMER)) {
                Set<RedisDeviceTimer> redisTimerSet = (Set<RedisDeviceTimer>) redisService.getZSetRange(CommonConstant.TIMING_TIMER);
                if (redisTimerSet != null && !redisTimerSet.isEmpty()) {
                    for (RedisDeviceTimer redisTimer : redisTimerSet) {
                        for (DeviceTimer conflictTimer : conflictTimers) {
                            if (redisTimer.getId().intValue() == conflictTimer.getId().intValue()) {
                                redisService.removeListValue(redisDeviceTimerKey, redisTimer);
                            }
                        }
                    }
                }
            }
            for (DeviceTimer timer : conflictTimers) {
                // 更新设备上的timer
                httpsAPIService.addTimerToDevice(timer, CommonConstant.UPD);
            }
        }

    }

}
