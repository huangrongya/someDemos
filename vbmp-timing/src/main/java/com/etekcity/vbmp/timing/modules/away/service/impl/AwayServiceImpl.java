package com.etekcity.vbmp.timing.modules.away.service.impl;

import com.etekcity.vbmp.timing.common.bean.DeviceInfo;
import com.etekcity.vbmp.timing.common.redis.RedisService;
import com.etekcity.vbmp.timing.common.service.DeviceInfoService;
import com.etekcity.vbmp.timing.common.service.DeviceTypeService;
import com.etekcity.vbmp.timing.common.service.impl.HttpsAPIService;
import com.etekcity.vbmp.timing.constant.CommonConstant;
import com.etekcity.vbmp.timing.constant.ErrorConstant;
import com.etekcity.vbmp.timing.modules.away.bean.*;
import com.etekcity.vbmp.timing.modules.away.dao.DeviceAwayMapper;
import com.etekcity.vbmp.timing.modules.away.service.AwayService;
import com.etekcity.vbmp.timing.modules.schedule.bean.DeviceSchedule;
import com.etekcity.vbmp.timing.modules.schedule.bean.RedisDeviceSchedule;
import com.etekcity.vbmp.timing.modules.schedule.dao.DeviceScheduleMapper;
import com.etekcity.vbmp.timing.modules.timer.bean.DeviceTimer;
import com.etekcity.vbmp.timing.modules.timer.bean.RedisDeviceTimer;
import com.etekcity.vbmp.timing.modules.timer.dao.DeviceTimerMapper;
import com.etekcity.vbmp.timing.util.MyDateUtils;
import com.etekcity.vbmp.timing.util.MyStringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.text.ParseException;
import java.util.*;
import java.util.stream.Collectors;

@SuppressWarnings("SpringJavaInjectionPointsAutowiringInspection")
@Service
public class AwayServiceImpl implements AwayService {
    private Logger logger = LoggerFactory.getLogger(AwayServiceImpl.class);
    @Autowired
    DeviceScheduleMapper deviceScheduleMapper;
    @Autowired
    DeviceAwayMapper deviceAwayMapper;
    @Autowired
    DeviceTimerMapper deviceTimerMapper;
    @Autowired
    DeviceInfoService deviceInfoService;
    @Autowired
    DeviceTypeService deviceTypeService;
    @Autowired
    RedisService redisService;
    @Autowired
    HttpsAPIService httpsAPIService;

    @Override
    public AwayResponse addAway(AwayRequest request) throws ParseException {
        AwayResponse response = new AwayResponse();
        String lockKey = CommonConstant.DEVICE_AWAY_LOCK_PREFIX + "add-" + request.getUuid();
        redisService.lock(lockKey);
        DeviceInfo deviceInfo = deviceInfoService.findDeviceByUuid(request.getUuid());
        String redisDeviceAwayKey = CommonConstant.TIMING_AWAY.concat(":").concat(deviceInfo.getDeviceType()).concat("-").concat(request.getUuid());
        DeviceAway deviceAway = (DeviceAway) redisService.get(redisDeviceAwayKey);
        if (deviceAway == null || deviceAway.getId() == null) {
            deviceAway = deviceAwayMapper.selectByUuid(request.getUuid()).get(0);
            redisService.set(redisDeviceAwayKey, deviceAway);
        }
        if (deviceAway != null) {
            response.setCode(ErrorConstant.ERR_DEVICE_AWAY_MAX);
            response.setMsg(ErrorConstant.ERR_DEVICE_AWAY_MAX_MSG);
            logger.error(response.getMsg());
            return response;
        }

        // 根据请求传过来的时间转换为日期，方便验证时间冲突
        Date startTime = MyDateUtils.getUtcDateByTimeAndZone(request.getStartTime(), request.getTimeZone());
        Date endTime = MyDateUtils.getUtcDateByTimeAndZone(request.getEndTime(), request.getTimeZone());

        Date now = MyDateUtils.getUtcDateTime();
        if ("7".equals(request.getRepeat())) {
            if (startTime.getTime() / 1000L < now.getTime() / 1000L) { // 开始时间当前时间小
                Calendar start = Calendar.getInstance();
                start.setTime(startTime);
                start.add(Calendar.DAY_OF_YEAR, 1);
                startTime = start.getTime();
                if (endTime != null) {
                    Calendar end = Calendar.getInstance();
                    end.setTime(endTime);
                    end.add(Calendar.DAY_OF_YEAR, 1);
                    endTime = end.getTime();
                }
            }
        }

        // 结束时间比开始时间小，则结束时间加一天
        if (endTime != null && endTime.getTime() / 1000L < startTime.getTime() / 1000L) {
            Calendar c = Calendar.getInstance();
            c.setTime(endTime);
            c.add(Calendar.DAY_OF_YEAR, 1);
            endTime = c.getTime();
        }

        Date validateStartTime = startTime;
        Date validateEndTime = endTime;
        long minutes = MyDateUtils.compareMinute(request.getStartTime(), request.getEndTime());

        // 计算随机时间
        Random random = new Random();
        if (minutes >= 1380L) { // 大于等于23小时
            int startRandom = random.nextInt(30);
            int endRandom = random.nextInt(30);
            Calendar startCanlendar = Calendar.getInstance();
            startCanlendar.setTime(startTime);
            startCanlendar.add(Calendar.MINUTE, startRandom);
            startTime = startCanlendar.getTime();

            Calendar endCanlerdar = Calendar.getInstance();
            endCanlerdar.setTime(endTime);
            endCanlerdar.add(Calendar.MINUTE, -endRandom);
            endTime = endCanlerdar.getTime();

        } else if (minutes > 60L) { // 大于60分钟随机60分钟
            validateStartTime = MyDateUtils.addDate(startTime, Calendar.MINUTE, -30);
            validateEndTime = MyDateUtils.addDate(endTime, Calendar.MINUTE, 30);

            int startRandom = random.nextInt(30);
            if (startRandom % 2 == 1) {
                startRandom = -startRandom;
            }
            int endRandom = random.nextInt(30);
            if (endRandom % 2 == 1) {
                endRandom = -endRandom;
            }
            Calendar startCanlendar = Calendar.getInstance();
            startCanlendar.setTime(startTime);
            startCanlendar.add(Calendar.MINUTE, startRandom);
            startTime = startCanlendar.getTime();

            Calendar endCanlerdar = Calendar.getInstance();
            endCanlerdar.setTime(endTime);
            endCanlerdar.add(Calendar.MINUTE, endRandom);
            endTime = endCanlerdar.getTime();

        } else { // 小于等于60分钟随机5分钟
            validateStartTime = MyDateUtils.addDate(startTime, Calendar.MINUTE, -5);
            validateEndTime = MyDateUtils.addDate(endTime, Calendar.MINUTE, 5);

            int startRandom = random.nextInt(5);
            int endRandom = random.nextInt(5);
            Calendar startCanlendar = Calendar.getInstance();
            startCanlendar.setTime(startTime);
            startCanlendar.add(Calendar.MINUTE, -startRandom);
            startTime = startCanlendar.getTime();

            Calendar endCanlerdar = Calendar.getInstance();
            endCanlerdar.setTime(endTime);
            endCanlerdar.add(Calendar.MINUTE, endRandom);
            endTime = endCanlerdar.getTime();
        }

        int startPlusDay = MyDateUtils.getPlusDay(MyDateUtils.getTimeStringByZone(validateStartTime, "UTC", request.getTimeZone()), request.getTimeZone());
        int endPlusDay = MyDateUtils.getPlusDay(MyDateUtils.getTimeStringByZone(validateEndTime, "UTC", request.getTimeZone()), request.getTimeZone());

        // 不能比当前时间小
        if (startTime.compareTo(now) <= 0) {
            Calendar startCanlendar = Calendar.getInstance();
            startCanlendar.setTime(startTime);
            startCanlendar.add(Calendar.DAY_OF_YEAR, 1);
            startTime = startCanlendar.getTime();
        }

        Map<String, Object> params = new HashMap<>();
        params.put("uuid", request.getUuid());
        params.put("timerStatus", "1"); // 开启状态
        params.put("scheduleState", "1"); // 开启状态

        // 全为空，需要验证是否有冲突
        if ((request.getConflictScheduleIds() == null || request.getConflictScheduleIds().isEmpty()) &&
                (request.getConflictTimerIds() == null || request.getConflictTimerIds().isEmpty())) {
            logger.info("验证时间冲突");
            List<Integer> conflictScheduleIds = validateAwaySchedule(request.getUuid(), validateStartTime, validateEndTime, request.getRepeat(), startPlusDay, endPlusDay);
            //List<String> conflictAwayIds = validateAwayAway(request.getUuid(), validateStartTime, validateEndTime, request.getRepeat(), startPlusDay, endPlusDay);
            List<Integer> conflictTimerIds = validateAwayTimer(request.getUuid(), validateStartTime, validateEndTime, request.getRepeat(), startPlusDay, endPlusDay);
            if (!conflictScheduleIds.isEmpty() || !conflictTimerIds.isEmpty()) {
                response.setCode(ErrorConstant.ERR_DEVICE_TIMING);
                response.setMsg(ErrorConstant.ERR_DEVICE_TIMING_MSG);
                response.setConflictScheduleIds(conflictScheduleIds);
                response.setConflictTimerIds(conflictTimerIds);
                return response;
            } // else 表示 没有冲突
        } else { // 传了冲突的ID过来，表示需要停止冲突timing
            stopConflictSchedule(request.getUuid(), request.getConflictScheduleIds());
            stopConflictTimer(request.getUuid(), request.getConflictTimerIds());
        }

        // insert data
        deviceAway = new DeviceAway();
        deviceAway.setUuid(request.getUuid());
        deviceAway.setDeviceId(deviceInfo.getId());
        deviceAway.setDeviceCid(deviceInfo.getDeviceCid());
        deviceAway.setAccountId(request.getAccountId());
        deviceAway.setExecuteStartTime(startTime);
        deviceAway.setExecuteEndTime(endTime);

        deviceAway.setStartTime(request.getStartTime());
        deviceAway.setEndTime(request.getEndTime());
        deviceAway.setAwayRepeat(request.getRepeat());
        deviceAway.setStatus("1");
        deviceAway.setTimeZone(request.getTimeZone());
        // 设置启动Away的时间
        deviceAway.setTurnonTime(MyDateUtils.getUtcDateTime());
        deviceAway.setCreateTime(MyDateUtils.getUtcDateTime());

        deviceAwayMapper.insertSelective(deviceAway);

        // update redis
        redisService.set(redisDeviceAwayKey, deviceAway, CommonConstant.SECONDS_OF_ONEDAY);
        // add away zset
        // 如果开始或结束时间比当前时间小，则在时间上加一天
        if (startTime.compareTo(now) <= 0) {
            Calendar calendar = Calendar.getInstance();
            calendar.setTime(startTime);
            calendar.add(Calendar.DAY_OF_YEAR, 1);
            deviceAway.setStartTime(MyDateUtils.dateToString(calendar.getTime()));
            startTime = calendar.getTime();
        }
        if (endTime.compareTo(now) <= 0) {
            Calendar calendar = Calendar.getInstance();
            calendar.setTime(endTime);
            calendar.add(Calendar.DAY_OF_YEAR, 1);
            deviceAway.setEndTime(MyDateUtils.dateToString(calendar.getTime()));
            endTime = calendar.getTime();
        }

        RedisDeviceAway RedisDeviceAway = new RedisDeviceAway();
        RedisDeviceAway.setId(deviceAway.getId());
        RedisDeviceAway.setDeviceId(deviceAway.getDeviceId());
        RedisDeviceAway.setDeviceCid(deviceAway.getDeviceCid());
        RedisDeviceAway.setUuid(deviceAway.getUuid());
        RedisDeviceAway.setAwayRepeat(deviceAway.getAwayRepeat());
        RedisDeviceAway.setExecuteTime(startTime);
        RedisDeviceAway.setType(CommonConstant.START);
        RedisDeviceAway.setOperator(CommonConstant.COMMON_STATUS_ON);
        RedisDeviceAway.setMinutes(MyDateUtils.getMinutsByDate(RedisDeviceAway.getExecuteTime()));
        redisService.addZSet(CommonConstant.TIMING_AWAY.concat(":").concat(deviceInfo.getDeviceType()), MyDateUtils.getMinutsByDate(RedisDeviceAway.getExecuteTime()), RedisDeviceAway);

        RedisDeviceAway.setExecuteTime(endTime);
        RedisDeviceAway.setType(CommonConstant.END);
        RedisDeviceAway.setOperator(CommonConstant.COMMON_STATUS_OFF);
        RedisDeviceAway.setMinutes(MyDateUtils.getMinutsByDate(RedisDeviceAway.getExecuteTime()));
        redisService.addZSet(CommonConstant.TIMING_AWAY.concat(":").concat(deviceInfo.getDeviceType()), MyDateUtils.getMinutsByDate(RedisDeviceAway.getExecuteTime()), RedisDeviceAway);

        response.setAwayId(deviceAway.getId());
        redisService.unlock(lockKey);
        return response;
    }

    @Override
    public AwayResponse deleteAway(AwayRequest request) {
        AwayResponse response = new AwayResponse();
        Integer id = request.getAwayId();
        DeviceAway deviceAway = deviceAwayMapper.selectByPrimaryKey(id);
        if (deviceAway == null || deviceAway.getId() == null) {
            response.setCode(ErrorConstant.ERR_DEVICE_AWAY_NOT_EXIST);
            response.setMsg(ErrorConstant.ERR_DEVICE_AWAY_NOT_EXIST_MSG);
            logger.error(response.getMsg());
            return response;
        }
        String lockKey = CommonConstant.DEVICE_AWAY_LOCK_PREFIX + "del-" + request.getAwayId();
        redisService.lock(lockKey);
        DeviceInfo deviceInfo = deviceInfoService.findDeviceByUuid(request.getUuid());
        deviceAwayMapper.deleteByPrimaryKey(id);
        // update redis
        String redisAwayKey = CommonConstant.TIMING_AWAY.concat(":").concat(deviceInfo.getDeviceType()).concat("-").concat(request.getUuid());
        if (redisService.exists(redisAwayKey)) {
            // 只有一个，不需要比较
            redisService.remove(redisAwayKey);
        }

        if ("1".equals(deviceAway.getStatus())) {
            if (redisService.exists(CommonConstant.TIMING_AWAY.concat(":").concat(deviceInfo.getDeviceType()))) {
                Set<RedisDeviceAway> redisAwaySet = (Set<RedisDeviceAway>) redisService.getZSetRange(CommonConstant.TIMING_AWAY.concat(":").concat(deviceInfo.getDeviceType()));
                if (redisAwaySet != null && !redisAwaySet.isEmpty()) {
                    for (RedisDeviceAway redisAway : redisAwaySet) {
                        if (redisAway.getId().intValue() == deviceAway.getId().intValue()) {
                            redisService.removeZSetValue(CommonConstant.TIMING_AWAY.concat(":").concat(deviceInfo.getDeviceType()), redisAway);
                        }
                    }
                }
            }
        }
        redisService.unlock(lockKey);
        return response;
    }

    @Override
    public AwayResponse updateAway(AwayRequest request) {
        AwayResponse response = new AwayResponse();
        // 验证存在性
        DeviceAway deviceAway = deviceAwayMapper.selectByPrimaryKey(request.getAwayId());
        if (deviceAway == null || deviceAway.getId() == null) {
            response.setCode(ErrorConstant.ERR_DEVICE_AWAY_NOT_EXIST);
            response.setMsg(ErrorConstant.ERR_DEVICE_AWAY_NOT_EXIST_MSG);
            logger.error(response.getMsg());
            return response;
        }

        String lockKey = CommonConstant.DEVICE_AWAY_LOCK_PREFIX + "upd-" + request.getAwayId();
        redisService.lock(lockKey);
        DeviceInfo deviceInfo = deviceInfoService.findDeviceByUuid(request.getUuid());
        // 根据请求传过来的时间转换为日期，方便验证时间冲突
        Date startTime = MyDateUtils.getUtcDateByTimeAndZone(request.getStartTime(), request.getTimeZone());
        Date endTime = MyDateUtils.getUtcDateByTimeAndZone(request.getEndTime(), request.getTimeZone());

        Date now = MyDateUtils.getUtcDateTime();
        if ("7".equals(request.getRepeat())) {
            if (startTime.getTime() / 1000L < now.getTime() / 1000L) { // 开始时间当前时间小
                Calendar start = Calendar.getInstance();
                start.setTime(startTime);
                start.add(Calendar.DAY_OF_YEAR, 1);
                startTime = start.getTime();

                if (endTime != null) {
                    Calendar end = Calendar.getInstance();
                    end.setTime(endTime);
                    end.add(Calendar.DAY_OF_YEAR, 1);
                    endTime = end.getTime();
                }
            }
        }

        // 结束时间比开始时间小，则结束时间加一天
        if (endTime != null && endTime.getTime() / 1000L < startTime.getTime() / 1000L) {
            Calendar c = Calendar.getInstance();
            c.setTime(endTime);
            c.add(Calendar.DAY_OF_YEAR, 1);
            endTime = c.getTime();
        }

        Date validateStartTime = startTime;
        Date validateEndTime = endTime;
        long minutes = MyDateUtils.compareMinute(request.getStartTime(), request.getEndTime());

        // 计算随机时间
        Random random = new Random();
        if (minutes >= 1380) { // 大于等于23小时
            int startRandom = random.nextInt(30);
            int endRandom = random.nextInt(30);
            Calendar startCanlendar = Calendar.getInstance();
            startCanlendar.setTime(startTime);
            startCanlendar.add(Calendar.MINUTE, startRandom);
            startTime = startCanlendar.getTime();

            Calendar endCanlerdar = Calendar.getInstance();
            endCanlerdar.setTime(endTime);
            endCanlerdar.add(Calendar.MINUTE, -endRandom);
            endTime = endCanlerdar.getTime();

        } else if (minutes > 60) { // 大于60分钟随机60分钟
            validateStartTime = MyDateUtils.addDate(startTime, Calendar.MINUTE, -30);
            validateEndTime = MyDateUtils.addDate(endTime, Calendar.MINUTE, 30);

            int startRandom = random.nextInt(30);
            if (startRandom % 2 == 1) {
                startRandom = -startRandom;
            }
            int endRandom = random.nextInt(30);
            if (endRandom % 2 == 1) {
                endRandom = -endRandom;
            }
            Calendar startCanlendar = Calendar.getInstance();
            startCanlendar.setTime(startTime);
            startCanlendar.add(Calendar.MINUTE, startRandom);
            startTime = startCanlendar.getTime();

            Calendar endCanlerdar = Calendar.getInstance();
            endCanlerdar.setTime(endTime);
            endCanlerdar.add(Calendar.MINUTE, endRandom);
            endTime = endCanlerdar.getTime();

        } else { // 小于等于60分钟随机5分钟
            validateStartTime = MyDateUtils.addDate(startTime, Calendar.MINUTE, -5);
            validateEndTime = MyDateUtils.addDate(endTime, Calendar.MINUTE, 5);

            int startRandom = random.nextInt(5);
            int endRandom = random.nextInt(5);
            Calendar startCanlendar = Calendar.getInstance();
            startCanlendar.setTime(startTime);
            startCanlendar.add(Calendar.MINUTE, -startRandom);
            startTime = startCanlendar.getTime();

            Calendar endCanlerdar = Calendar.getInstance();
            endCanlerdar.setTime(endTime);
            endCanlerdar.add(Calendar.MINUTE, endRandom);
            endTime = endCanlerdar.getTime();
        }

        int startPlusDay = MyDateUtils.getPlusDay(MyDateUtils.getTimeStringByZone(validateStartTime, "UTC", request.getTimeZone()), request.getTimeZone());
        int endPlusDay = MyDateUtils.getPlusDay(MyDateUtils.getTimeStringByZone(validateEndTime, "UTC", request.getTimeZone()), request.getTimeZone());

        // 不能比当前时间小
        if (startTime.compareTo(now) <= 0) {
            Calendar startCanlendar = Calendar.getInstance();
            startCanlendar.setTime(startTime);
            startCanlendar.add(Calendar.DAY_OF_YEAR, 1);
            startTime = startCanlendar.getTime();
        }

        // 启动状态，需要停止相关冲突定时，再更新Away和redis  停止状态，直接更新Away和redis
        if ("1".equals(request.getAwayStatus())) {

            // 设置启动Away的时间
            deviceAway.setTurnonTime(MyDateUtils.getUtcDateTime());

            // 全为空，需要验证是否有冲突
            if ((request.getConflictScheduleIds() == null || request.getConflictScheduleIds().isEmpty()) &&
                    (request.getConflictTimerIds() == null || request.getConflictTimerIds().isEmpty())) {
                logger.info("验证时间冲突");
                List<Integer> conflictScheduleIds = validateAwaySchedule(deviceAway.getUuid(), validateStartTime, validateEndTime, request.getRepeat(), startPlusDay, endPlusDay);
                List<Integer> conflictTimerIds = validateAwayTimer(deviceAway.getUuid(), validateStartTime, validateEndTime, request.getRepeat(), startPlusDay, endPlusDay);
                if (!conflictScheduleIds.isEmpty() || !conflictTimerIds.isEmpty()) {
                    response.setCode(ErrorConstant.ERR_DEVICE_TIMING);
                    response.setMsg(ErrorConstant.ERR_DEVICE_TIMING_MSG);
                    response.setConflictScheduleIds(conflictScheduleIds);
                    response.setConflictTimerIds(conflictTimerIds);
                    return response;
                } // else 是 两个ID 都为空 则直接更新
            } else { // 传了冲突的ID过来，表示需要停止冲突timing
                stopConflictSchedule(deviceAway.getUuid(), request.getConflictScheduleIds());
                stopConflictTimer(deviceAway.getUuid(), request.getConflictTimerIds());
            }
        }

        deviceAway.setStatus(request.getAwayStatus());
        deviceAway.setTimeZone(request.getTimeZone());
        deviceAway.setExecuteStartTime(startTime);
        deviceAway.setExecuteEndTime(endTime);
        deviceAway.setStartTime(request.getStartTime());
        deviceAway.setEndTime(request.getEndTime());
        deviceAway.setAwayRepeat(request.getRepeat());

        deviceAwayMapper.updateByPrimaryKey(deviceAway);
        // update data in redis
        String redisAwayKey = CommonConstant.TIMING_AWAY.concat(":").concat(deviceInfo.getDeviceType()).concat("-").concat(deviceAway.getUuid());
        if (redisService.exists(redisAwayKey)) {
            // 只有一个不需要对比
            redisService.remove(redisAwayKey);
        }
        redisService.set(redisAwayKey, deviceAway, CommonConstant.SECONDS_OF_ONEDAY);
        // 更新away zset
        if (redisService.exists(CommonConstant.TIMING_AWAY.concat(":").concat(deviceInfo.getDeviceType()))) {
            Set<RedisDeviceAway> redisAwaySet = (Set<RedisDeviceAway>) redisService.getZSetRange(CommonConstant.TIMING_AWAY.concat(":").concat(deviceInfo.getDeviceType()));
            if (redisAwaySet != null && !redisAwaySet.isEmpty()) {
                for (RedisDeviceAway redisAway : redisAwaySet) {
                    if (redisAway.getId().intValue() == deviceAway.getId().intValue()) {
                        redisService.removeZSetValue(CommonConstant.TIMING_AWAY.concat(":").concat(deviceInfo.getDeviceType()), redisAway);
                    }
                }
            }
        }
        if ("1".equals(deviceAway.getStatus())) {
            // add away zset
            // 如果开始或结束时间比当前时间小，则在时间上加一天
            if (startTime.compareTo(now) <= 0) {
                Calendar calendar = Calendar.getInstance();
                calendar.setTime(startTime);
                calendar.add(Calendar.DAY_OF_YEAR, 1);
                deviceAway.setStartTime(MyDateUtils.dateToString(calendar.getTime()));
                startTime = calendar.getTime();
            }
            if (endTime.compareTo(now) <= 0) {
                Calendar calendar = Calendar.getInstance();
                calendar.setTime(endTime);
                calendar.add(Calendar.DAY_OF_YEAR, 1);
                deviceAway.setEndTime(MyDateUtils.dateToString(calendar.getTime()));
                endTime = calendar.getTime();
            }

            RedisDeviceAway RedisDeviceAway = new RedisDeviceAway();
            RedisDeviceAway.setId(deviceAway.getId());
            RedisDeviceAway.setDeviceCid(deviceAway.getDeviceCid());
            RedisDeviceAway.setDeviceId(deviceAway.getDeviceId());
            RedisDeviceAway.setUuid(deviceAway.getUuid());
            RedisDeviceAway.setAwayRepeat(deviceAway.getAwayRepeat());
            RedisDeviceAway.setExecuteTime(startTime);
            RedisDeviceAway.setType(CommonConstant.START);
            RedisDeviceAway.setOperator(CommonConstant.COMMON_STATUS_ON);
            RedisDeviceAway.setMinutes(MyDateUtils.getMinutsByDate(RedisDeviceAway.getExecuteTime()));
            redisService.addZSet(CommonConstant.TIMING_AWAY.concat(":").concat(deviceInfo.getDeviceType()), MyDateUtils.getMinutsByDate(RedisDeviceAway.getExecuteTime()), RedisDeviceAway);

            RedisDeviceAway.setExecuteTime(endTime);
            RedisDeviceAway.setType(CommonConstant.END);
            RedisDeviceAway.setOperator(CommonConstant.COMMON_STATUS_OFF);
            RedisDeviceAway.setMinutes(MyDateUtils.getMinutsByDate(RedisDeviceAway.getExecuteTime()));
            redisService.addZSet(CommonConstant.TIMING_AWAY.concat(":").concat(deviceInfo.getDeviceType()), MyDateUtils.getMinutsByDate(RedisDeviceAway.getExecuteTime()), RedisDeviceAway);

        }
        redisService.unlock(lockKey);
        return response;
    }

    @Override
    public AwayResponse updateAwayStatus(AwayRequest request) {
        AwayResponse response = new AwayResponse();
        // 验证存在性
        DeviceAway deviceAway = deviceAwayMapper.selectByPrimaryKey(Integer.valueOf(request.getAwayId()));
        if (deviceAway == null || deviceAway.getId() == null) {
            response.setCode(ErrorConstant.ERR_DEVICE_AWAY_NOT_EXIST);
            response.setMsg(ErrorConstant.ERR_DEVICE_AWAY_NOT_EXIST_MSG);
            logger.error(response.getMsg());
            return response;
        }

        String lockKey = CommonConstant.DEVICE_AWAY_LOCK_PREFIX + "sta-" + deviceAway.getId();
        redisService.lock(lockKey);


        String start = deviceAway.getStartTime();
        String end = deviceAway.getEndTime();
        Date executeStartTime = MyDateUtils.getUtcDateByTimeAndZone(start, request.getTimeZone());
        Date executeEndTime = MyDateUtils.getUtcDateByTimeAndZone(end, request.getTimeZone());
        Date now = MyDateUtils.getUtcDateTime();

        if ("7".equals(deviceAway.getAwayRepeat())) {
            if (executeStartTime.getTime() / 1000L < now.getTime() / 1000L) { // 开始时间当前时间小
                executeStartTime = MyDateUtils.addDate(executeStartTime, Calendar.DAY_OF_YEAR, 1);

                if (executeEndTime != null) {
                    executeEndTime = MyDateUtils.addDate(executeEndTime, Calendar.DAY_OF_YEAR, 1);
                }
            }
        }

        // 结束时间比开始时间小，则结束时间加一天
        if (executeEndTime != null && executeEndTime.getTime() / 1000L < executeStartTime.getTime() / 1000L) {
            executeEndTime = MyDateUtils.addDate(executeEndTime, Calendar.DAY_OF_YEAR, 1);
        }

        Date validateStartTime = executeStartTime;
        Date validateEndTime = executeEndTime;
        long minutes = MyDateUtils.compareMinute(start, end);

        // 计算随机时间
        Random random = new Random();
        if (minutes >= 1380) { // 大于等于23小时
            int startRandom = random.nextInt(30);
            int endRandom = random.nextInt(30);
            Calendar startCanlendar = Calendar.getInstance();
            startCanlendar.setTime(executeStartTime);
            startCanlendar.add(Calendar.MINUTE, startRandom);
            executeStartTime = startCanlendar.getTime();

            Calendar endCanlerdar = Calendar.getInstance();
            endCanlerdar.setTime(executeEndTime);
            endCanlerdar.add(Calendar.MINUTE, -endRandom);
            executeEndTime = endCanlerdar.getTime();

        } else if (minutes > 60) { // 大于60分钟随机60分钟
            validateStartTime = MyDateUtils.addDate(executeStartTime, Calendar.MINUTE, -30);
            validateEndTime = MyDateUtils.addDate(executeEndTime, Calendar.MINUTE, 30);

            int startRandom = random.nextInt(30);
            if (startRandom % 2 == 1) {
                startRandom = -startRandom;
            }
            int endRandom = random.nextInt(30);
            if (endRandom % 2 == 1) {
                endRandom = -endRandom;
            }
            Calendar startCanlendar = Calendar.getInstance();
            startCanlendar.setTime(executeStartTime);
            startCanlendar.add(Calendar.MINUTE, startRandom);
            executeStartTime = startCanlendar.getTime();

            Calendar endCanlerdar = Calendar.getInstance();
            endCanlerdar.setTime(executeEndTime);
            endCanlerdar.add(Calendar.MINUTE, endRandom);
            executeEndTime = endCanlerdar.getTime();

        } else { // 小于等于60分钟随机5分钟
            validateStartTime = MyDateUtils.addDate(executeStartTime, Calendar.MINUTE, -5);
            validateEndTime = MyDateUtils.addDate(executeEndTime, Calendar.MINUTE, 5);

            int startRandom = random.nextInt(5);
            int endRandom = random.nextInt(5);
            Calendar startCanlendar = Calendar.getInstance();
            startCanlendar.setTime(executeStartTime);
            startCanlendar.add(Calendar.MINUTE, -startRandom);
            executeStartTime = startCanlendar.getTime();

            Calendar endCanlerdar = Calendar.getInstance();
            endCanlerdar.setTime(executeEndTime);
            endCanlerdar.add(Calendar.MINUTE, endRandom);
            executeEndTime = endCanlerdar.getTime();

        }

        int startPlusDay = MyDateUtils.getPlusDay(MyDateUtils.getTimeStringByZone(validateStartTime, "UTC", request.getTimeZone()), request.getTimeZone());
        int endPlusDay = MyDateUtils.getPlusDay(MyDateUtils.getTimeStringByZone(validateEndTime, "UTC", request.getTimeZone()), request.getTimeZone());

        // 不能比当前时间小
        if (executeStartTime.compareTo(now) <= 0) {
            Calendar startCanlendar = Calendar.getInstance();
            startCanlendar.setTime(executeStartTime);
            startCanlendar.add(Calendar.DAY_OF_YEAR, 1);
            executeStartTime = startCanlendar.getTime();
        }

        if ("7".equals(deviceAway.getAwayRepeat())) {
            if (executeStartTime.getTime() / 1000L < now.getTime() / 1000L) { // 开始时间当前时间小
                Calendar cstart = Calendar.getInstance();
                cstart.setTime(executeStartTime);
                cstart.add(Calendar.DAY_OF_YEAR, 1);
                executeStartTime = cstart.getTime();

                if (executeEndTime != null) {
                    Calendar cend = Calendar.getInstance();
                    cend.setTime(executeEndTime);
                    cend.add(Calendar.DAY_OF_YEAR, 1);
                    executeEndTime = cend.getTime();
                }
            }
        }

        // 启动状态，需要停止相关冲突定时，再更新Away和redis
        if ("1".equals(request.getAwayStatus())) {

            // 设置启动Away的时间
            deviceAway.setTurnonTime(MyDateUtils.getUtcDateTime());

            Map<String, Object> params = new HashMap<>();
            params.put("uuid", deviceAway.getUuid());
            params.put("timerStatus", "1"); // 开启状态
            params.put("scheduleState", "1"); // 开启状态

            // 全为空，需要验证是否有冲突
            if ((request.getConflictScheduleIds() == null || request.getConflictScheduleIds().isEmpty()) &&
                    (request.getConflictTimerIds() == null || request.getConflictTimerIds().isEmpty())) {
                logger.info("验证时间冲突");
                List<Integer> conflictScheduleIds = validateAwaySchedule(deviceAway.getUuid(), validateStartTime, validateEndTime, deviceAway.getAwayRepeat(), startPlusDay, endPlusDay);
                List<Integer> conflictTimerIds = validateAwayTimer(deviceAway.getUuid(), validateStartTime, validateEndTime, deviceAway.getAwayRepeat(), startPlusDay, endPlusDay);
                if (!conflictScheduleIds.isEmpty() || !conflictTimerIds.isEmpty()) {
                    response.setCode(ErrorConstant.ERR_DEVICE_TIMING);
                    response.setMsg(ErrorConstant.ERR_DEVICE_TIMING_MSG);
                    response.setConflictScheduleIds(conflictScheduleIds);
                    response.setConflictTimerIds(conflictTimerIds);
                    return response;
                } // else 是 两个ID 都为空 则直接更新
            } else { // 传了冲突的ID过来，表示需要停止冲突timing
                stopConflictSchedule(deviceAway.getUuid(), request.getConflictScheduleIds());
                stopConflictTimer(deviceAway.getUuid(), request.getConflictTimerIds());
            }
        }

        deviceAway.setStatus(request.getAwayStatus());
        deviceAway.setExecuteStartTime(executeStartTime);
        deviceAway.setExecuteEndTime(executeEndTime);

        deviceAwayMapper.updateByPrimaryKey(deviceAway);
        DeviceInfo deviceInfo = deviceInfoService.findDeviceByUuid(request.getUuid());
        // update data in redis
        String redisAwayKey = CommonConstant.TIMING_AWAY.concat(":").concat(deviceInfo.getDeviceType()).concat("-").concat(deviceAway.getUuid());
        if (redisService.exists(redisAwayKey)) {
            // 只有一个，不需要比较
            redisService.remove(redisAwayKey);
        }
        redisService.set(redisAwayKey, deviceAway, CommonConstant.SECONDS_OF_ONEDAY);
        // 更新away zset
        if (redisService.exists(CommonConstant.TIMING_AWAY.concat(":").concat(deviceInfo.getDeviceType()))) {
            Set<RedisDeviceAway> redisAwaySet = (Set<RedisDeviceAway>) redisService.getZSetRange(CommonConstant.TIMING_AWAY.concat(":").concat(deviceInfo.getDeviceType()));
            if (redisAwaySet != null && !redisAwaySet.isEmpty()) {
                for (RedisDeviceAway redisAway : redisAwaySet) {
                    if (redisAway.getId().intValue() == deviceAway.getId().intValue()) {
                        redisService.removeZSetValue(CommonConstant.TIMING_AWAY.concat(":").concat(deviceInfo.getDeviceType()), redisAway);
                    }
                }
            }
        }
        if ("1".equals(deviceAway.getStatus())) {
            // 启动的时候才加入zset

            // 如果开始或结束时间比当前时间小，则在时间上加一天
            if (executeStartTime.compareTo(now) <= 0) {
                Calendar calendar = Calendar.getInstance();
                calendar.setTime(executeStartTime);
                calendar.add(Calendar.DAY_OF_YEAR, 1);
                deviceAway.setStartTime(MyDateUtils.dateToString(calendar.getTime()));
                executeStartTime = calendar.getTime();
            }
            if (executeEndTime.compareTo(now) <= 0) {
                Calendar calendar = Calendar.getInstance();
                calendar.setTime(executeEndTime);
                calendar.add(Calendar.DAY_OF_YEAR, 1);
                deviceAway.setEndTime(MyDateUtils.dateToString(calendar.getTime()));
                executeEndTime = calendar.getTime();
            }

            RedisDeviceAway RedisDeviceAway = new RedisDeviceAway();
            RedisDeviceAway.setId(deviceAway.getId());
            RedisDeviceAway.setDeviceCid(deviceAway.getDeviceCid());
            RedisDeviceAway.setDeviceId(deviceAway.getDeviceId());
            RedisDeviceAway.setUuid(deviceAway.getUuid());
            RedisDeviceAway.setAwayRepeat(deviceAway.getAwayRepeat());
            RedisDeviceAway.setExecuteTime(executeStartTime);
            RedisDeviceAway.setType(CommonConstant.START);
            RedisDeviceAway.setOperator(CommonConstant.COMMON_STATUS_ON);
            RedisDeviceAway.setMinutes(MyDateUtils.getMinutsByDate(RedisDeviceAway.getExecuteTime()));
            redisService.addZSet(CommonConstant.TIMING_AWAY.concat(":").concat(deviceInfo.getDeviceType()), MyDateUtils.getMinutsByDate(RedisDeviceAway.getExecuteTime()), RedisDeviceAway);

            RedisDeviceAway.setExecuteTime(executeEndTime);
            RedisDeviceAway.setType(CommonConstant.END);
            RedisDeviceAway.setOperator(CommonConstant.COMMON_STATUS_OFF);
            RedisDeviceAway.setMinutes(MyDateUtils.getMinutsByDate(RedisDeviceAway.getExecuteTime()));
            redisService.addZSet(CommonConstant.TIMING_AWAY.concat(":").concat(deviceInfo.getDeviceType()), MyDateUtils.getMinutsByDate(RedisDeviceAway.getExecuteTime()), RedisDeviceAway);
        }
        redisService.unlock(lockKey);
        return response;
    }

    @Override
    public AwayResponse getAways(AwayRequest request) throws ParseException {
        String uuid = request.getUuid();
        DeviceInfo deviceInfo = deviceInfoService.findDeviceByUuid(request.getUuid());
        AwayResponse response = new AwayResponse();
        String lockKey = CommonConstant.DEVICE_AWAY_LOCK_PREFIX + "get-" + uuid;
        redisService.lock(lockKey);
        String redisAwayKey = CommonConstant.TIMING_AWAY.concat(":").concat(deviceInfo.getDeviceType()).concat("-").concat(uuid);
        DeviceAway deviceAway = null;
        if (redisService.exists(redisAwayKey)) {
            deviceAway = (DeviceAway) redisService.get(redisAwayKey);
        }
        if (deviceAway == null || deviceAway.getId() == null) {
            List<DeviceAway> deviceAways = deviceAwayMapper.selectByUuid(request.getUuid());
            if (deviceAways != null && !deviceAways.isEmpty()) {
                deviceAway = deviceAways.get(0);
                // 存入redis
                redisService.set(redisAwayKey, deviceAway, CommonConstant.SECONDS_OF_ONEDAY);
            }
        }
        if (deviceAway == null || deviceAway.getId() == null) {
            logger.info("无此设备相关Away");
            return response;
        }
        // 封装response
        DeviceAwayView away = new DeviceAwayView();
        away.setId(deviceAway.getId());
        // 时间转换
        String startTime = MyDateUtils.getTimeStringByZone(deviceAway.getStartTime(), deviceAway.getTimeZone(), request.getTimeZone());
        String endTime = MyDateUtils.getTimeStringByZone(deviceAway.getEndTime(), deviceAway.getTimeZone(), request.getTimeZone());
        away.setStartTime(startTime);
        away.setEndTime(endTime);
        away.setTimeZone(deviceAway.getTimeZone());
        away.setAwayRepeat(deviceAway.getAwayRepeat());
        away.setStatus(deviceAway.getStatus());
        away.setUuid(deviceAway.getUuid());

        Date executeStartTime = MyDateUtils.getUtcDateByTimeAndZone(startTime, deviceAway.getTimeZone());
        // tomorrow
        if ("7".equals(deviceAway.getAwayRepeat())) {
            if (MyDateUtils.calcTomorrow(executeStartTime, deviceAway.getTurnonTime())) {
                away.setOnceTomorrow("1");
            }
        }
        // next day
        if (!MyStringUtils.isNullData(endTime) && MyDateUtils.convertToDate(endTime).before(MyDateUtils.convertToDate(startTime))) {
            away.setNextDay("1");
        }
        response.setAway(away);
        redisService.unlock(lockKey);
        return response;
    }

    @Override
    public AwayResponse stopAwayByPrimaryKey(Integer awayId) {
        AwayResponse response = new AwayResponse();
        DeviceAway deviceAway = deviceAwayMapper.selectByPrimaryKey(awayId);
        if (deviceAway == null || deviceAway.getId() == null) {
            response.setCode(ErrorConstant.ERR_DEVICE_AWAY_NOT_EXIST);
            response.setMsg(ErrorConstant.ERR_DEVICE_AWAY_NOT_EXIST_MSG);
            logger.error(response.getMsg());
            return response;
        }
        deviceAwayMapper.updateStatusByIds(Arrays.asList(awayId), "0");

        return response;
    }

    @Override
    public AwayResponse updateUuidByUuid(String oldUuid, String newUuid) {
        AwayResponse response = new AwayResponse();
        DeviceInfo deviceInfo = deviceInfoService.findDeviceByUuid(oldUuid);
        String redisKey = CommonConstant.TIMING_AWAY.concat(":").concat(deviceInfo.getDeviceType()).concat("-").concat(oldUuid);
        if (redisService.exists(redisKey)) {
            redisService.remove(redisKey);
        }
        Set<RedisDeviceAway> redisAwaySet = null;
        if (redisService.exists(CommonConstant.TIMING_AWAY.concat(":").concat(deviceInfo.getDeviceType()))) {
            redisAwaySet = (Set<RedisDeviceAway>) redisService.getZSetRange(CommonConstant.TIMING_AWAY.concat(":").concat(deviceInfo.getDeviceType()));
        }
        if (redisAwaySet != null && !redisAwaySet.isEmpty()) {
            for (RedisDeviceAway redisAway : redisAwaySet) {
                if (redisAway.getUuid().equals(oldUuid)) {
                    redisService.removeZSetValue(CommonConstant.TIMING_AWAY.concat(":").concat(deviceInfo.getDeviceType()), redisAway);
                    redisAway.setUuid(newUuid);
                    redisService.addZSet(CommonConstant.TIMING_AWAY.concat(":").concat(deviceInfo.getDeviceType()), MyDateUtils.getMinutsByDate(redisAway.getExecuteTime()), redisAway);
                }
            }
        }

        deviceAwayMapper.updateUuidByUuid(oldUuid, newUuid);

        return response;
    }

    @Override
    public AwayResponse deleteTimerByUuid(String uuid) {
        AwayResponse response = new AwayResponse();
        // 用MAP为了方便以后扩展多个参数删除
        DeviceAway deviceAway = null;
        List<DeviceAway> deviceAways = deviceAwayMapper.selectByUuid(uuid);
        if (deviceAways != null && !deviceAways.isEmpty()) {
            deviceAway = deviceAways.get(0);
        }
        deviceAwayMapper.deleteByUuid(uuid);
        DeviceInfo deviceInfo = deviceInfoService.findDeviceByUuid(uuid);
        // delete data in redis
//        String redisAwayKey = CommonConstant.REDIS_KEY_DEVICE_PREFIX.concat("aways-").concat(uuid);
        String redisAwayKey = CommonConstant.TIMING_AWAY.concat(":").concat(deviceInfo.getDeviceType()).concat("-").concat(uuid);
        if (redisService.exists(redisAwayKey)) {
            // 一个设备只有一个离家模式
            redisService.remove(redisAwayKey);
        }
        Set<RedisDeviceAway> redisAwaySet = null;
        if (redisService.exists(CommonConstant.TIMING_AWAY.concat(":").concat(deviceInfo.getDeviceType()))) {
            redisAwaySet = (Set<RedisDeviceAway>) redisService.getZSetRange(CommonConstant.TIMING_AWAY.concat(":").concat(deviceInfo.getDeviceType()));
        }
        if (redisAwaySet != null && !redisAwaySet.isEmpty() && deviceAway != null) {
            for (RedisDeviceAway redisAway : redisAwaySet) {
                if (redisAway.getId().intValue() == deviceAway.getId().intValue()) {
                    redisService.removeZSetValue(CommonConstant.TIMING_AWAY.concat(":").concat(deviceInfo.getDeviceType()), redisAway);
                }
            }
        }
        return response;
    }

    /**
     * 验证away和schedule的冲突
     *
     * @param uuid
     * @param startTime
     * @param endTime
     * @param repeat
     * @param startPlusDay
     * @param endPlusDay
     * @return
     */
    private List<Integer> validateAwaySchedule(String uuid, Date startTime, Date endTime, String repeat, int startPlusDay, int endPlusDay) {
        List<Integer> ids = new ArrayList<>();
        DeviceSchedule query = new DeviceSchedule();
        query.setStatus("1");
        query.setUuid(uuid);
        query.setEvent(CommonConstant.EVENT_SWITCH);
        List<DeviceSchedule> startedSchedules = deviceScheduleMapper.select(query);
        String[] currRepeats = repeat.split(",");
        for (DeviceSchedule startSchedule : startedSchedules) {
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
                    // 单点Schedule
                    if (dbEndTime == null) {
                        // 这个单点时间在另一组时间范围内
                        if (MyDateUtils.compareTimeLine(startTime, endTime, dbStartTime)) {
                            ids.add(startSchedule.getId());
                        }
                    } else {
                        // 开始时间和结束时间都小于已经开始了的开始时间
                        if (MyDateUtils.compareTimeLine(startTime, endTime, dbStartTime, dbEndTime)) {
                            ids.add(startSchedule.getId());
                        }
                    }
                }
            }
        }
        return ids.stream().distinct().collect(Collectors.toList());
    }

    /**
     * 验证away和timer的冲突
     *
     * @param uuid
     * @param startTime
     * @param endTime
     * @param repeat
     * @param startPlusDay
     * @param endPlusDay
     * @return
     */
    private List<Integer> validateAwayTimer(String uuid, Date startTime, Date endTime, String repeat, int startPlusDay, int endPlusDay) {

        List<Integer> ids = new ArrayList<>();
        String[] repeats = repeat.split(",");

        Map<String, Object> params = new HashMap<>(2);
        params.put("uuid", uuid);
        params.put("timerStatus", "1");

        DeviceTimer query = new DeviceTimer();
        query.setUuid(uuid);
        query.setStatus("1");

        List<DeviceTimer> startedTimers = deviceTimerMapper.select(query);
        for (DeviceTimer startTimer : startedTimers) {
            Date dbEndTime = startTimer.getExecuteTime();

            for (String currRepeat : repeats) {
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
                // 验证时间重叠
                if (MyDateUtils.compareTimeLine(startTime, endTime, dbEndTime)) {
                    ids.add(startTimer.getId());
                }
            }
        }
        return ids.stream().distinct().collect(Collectors.toList());
    }

    /**
     * 关闭冲突的schedule
     *
     * @param uuid
     * @param conflictScheduleIds
     */
    private void stopConflictSchedule(String uuid, List<Integer> conflictScheduleIds) {
        if (conflictScheduleIds != null && !conflictScheduleIds.isEmpty()) {
            List<DeviceSchedule> confictSchedus = deviceScheduleMapper.selectByPrimaryKeys(conflictScheduleIds);
            List<Integer> scheduleIds = new ArrayList<>(confictSchedus.size());
            Iterator<DeviceSchedule> scheduleIterator = confictSchedus.iterator();
            while (scheduleIterator.hasNext()) {
                DeviceSchedule schedule = scheduleIterator.next();
                schedule.setStatus("0");
                scheduleIds.add(schedule.getId());
            }
            // stop schedule in db
            deviceScheduleMapper.updateStatusByIds(conflictScheduleIds, "0");
            // update redis
            String redisDeviceScheduleKey = CommonConstant.TIMING_SCHEDULE.concat("-").concat(uuid);
            if (redisService.exists(redisDeviceScheduleKey)) {
                List<DeviceSchedule> redisDeviceSchedules = redisService.getList(redisDeviceScheduleKey);
                if (redisDeviceSchedules != null && !redisDeviceSchedules.isEmpty()) {
                    for (DeviceSchedule redisDeviceSchedule : redisDeviceSchedules) {
                        for (DeviceSchedule conflictSchedule : confictSchedus) {
                            if (redisDeviceSchedule.getId().intValue() == conflictSchedule.getId().intValue()) {
                                redisService.removeListValue(redisDeviceScheduleKey, redisDeviceSchedule);
                            }
                        }
                    }
                }
            }
            // 更新后的schedule 重新加入到redis
            redisService.addList(redisDeviceScheduleKey, confictSchedus);

            // remove running schedule zset
            if (redisService.exists(CommonConstant.TIMING_SCHEDULE)) {
                Set<RedisDeviceSchedule> redisDeviceScheuleSet = (Set<RedisDeviceSchedule>) redisService.getZSetRange(CommonConstant.TIMING_SCHEDULE);
                if (redisDeviceScheuleSet != null && !redisDeviceScheuleSet.isEmpty()) {
                    for (RedisDeviceSchedule redisDeviceScheule : redisDeviceScheuleSet) {
                        for (DeviceSchedule conflictSchedule : confictSchedus) {
                            if (redisDeviceScheule.getId().intValue() == conflictSchedule.getId().intValue()) {
                                redisService.removeZSetValue(CommonConstant.TIMING_SCHEDULE, redisDeviceScheule);
                            }
                        }
                    }
                }
            }

            // 更新设备中的schedule
            for (DeviceSchedule schedule : confictSchedus) {
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
                httpsAPIService.addScheduleToDevice(schedule, CommonConstant.UPD);
            }
        }
    }

    private void stopConflictTimer(String uuid, List<Integer> conflictTimerIds) {
        if (conflictTimerIds != null && !conflictTimerIds.isEmpty()) {
            List<DeviceTimer> conflictAwayAndTimes = deviceTimerMapper.selectByPrimaryKeys(conflictTimerIds);
            Iterator<DeviceTimer> iterator = conflictAwayAndTimes.iterator();
            while (iterator.hasNext()) {
                DeviceTimer conflictTimer = iterator.next();
                conflictTimer.setStatus("0");
            }
            // 停止其它Timer
            deviceTimerMapper.updateStatusByIds(conflictTimerIds, "0");
            // 更新 redis other Timer
            String redisDeviceTimerKey = CommonConstant.TIMING_TIMER.concat("-").concat(uuid);
            if (redisService.exists(redisDeviceTimerKey)) {
                List<DeviceTimer> redisDeviceTimers = redisService.getList(redisDeviceTimerKey);
                if (redisDeviceTimers != null && !redisDeviceTimers.isEmpty()) {
                    for (DeviceTimer rdt : redisDeviceTimers) {
                        for (DeviceTimer confilctTimer : conflictAwayAndTimes) {
                            if (rdt.getId().intValue() == confilctTimer.getId().intValue()) {
                                // 移除冲突的Timer
                                redisService.removeListValue(redisDeviceTimerKey, rdt);
                            }
                        }
                    }
                }
            }
            // 把冲突的Timer更新后存入redis
            redisService.addList(redisDeviceTimerKey, conflictAwayAndTimes);

            // update zset
            if (redisService.exists(CommonConstant.TIMING_TIMER)) {
                Set<RedisDeviceTimer> redisTimerSet = (Set<RedisDeviceTimer>) redisService.getZSetRange(CommonConstant.TIMING_TIMER);
                if (redisTimerSet != null && !redisTimerSet.isEmpty()) {
                    for (RedisDeviceTimer redisTimer : redisTimerSet) {
                        for (DeviceTimer conflictTimer : conflictAwayAndTimes) {
                            if (redisTimer.getId().intValue() == conflictTimer.getId().intValue()) {
                                redisService.removeListValue(redisDeviceTimerKey, redisTimer);
                            }
                        }
                    }
                }
            }
            // 更新设备中的timer
            for (DeviceTimer timer : conflictAwayAndTimes) {
                httpsAPIService.addTimerToDevice(timer, CommonConstant.UPD);
            }

        }

    }
}
