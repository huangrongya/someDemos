package com.etekcity.vbmp.timing.modules.timer.service.impl;

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
import com.etekcity.vbmp.timing.modules.schedule.bean.DeviceSchedule;
import com.etekcity.vbmp.timing.modules.schedule.bean.RedisDeviceSchedule;
import com.etekcity.vbmp.timing.modules.schedule.dao.DeviceScheduleMapper;
import com.etekcity.vbmp.timing.modules.timer.bean.*;
import com.etekcity.vbmp.timing.modules.timer.dao.DeviceTimerMapper;
import com.etekcity.vbmp.timing.modules.timer.service.TimerService;
import com.etekcity.vbmp.timing.util.MyDateUtils;
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
public class TimerServiceImpl implements TimerService {
    @Autowired
    private RedisService redisService;
    @Autowired
    DeviceTimerMapper deviceTimerMapper;
    @Autowired
    DeviceInfoService deviceInfoService;
    @Autowired
    DeviceTypeService deviceTypeService;
    @Autowired
    DeviceAwayMapper deviceAwayMapper;
    @Autowired
    DeviceScheduleMapper deviceScheduleMapper;
    @Autowired
    HttpsAPIService httpsAPIService;

    private Logger logger = LoggerFactory.getLogger(TimerServiceImpl.class);

    /**
     * 添加Timer
     *
     * @param request
     * @return
     * @throws Exception
     */
    @Override
    public TimerResponse addTimer(TimerRequest request) {
        TimerResponse response = new TimerResponse();

        String lockKey = CommonConstant.DEVICE_TIMER_LOCK_PREFIX + "add-" + request.getUuid();
        redisService.lock(lockKey);
        DeviceInfo deviceInfo = deviceInfoService.findDeviceByUuid(request.getUuid());
        String redisDeviceTimerKey = CommonConstant.TIMING_TIMER.concat(":").concat(deviceInfo.getDeviceType()).concat("-").concat(request.getUuid());
        List<DeviceTimer> deviceTimers = redisService.getList(redisDeviceTimerKey);
        if (deviceTimers == null || deviceTimers.isEmpty()) {
            deviceTimers = deviceTimerMapper.selectByUuid(request.getUuid());
            if (deviceTimers != null && !deviceTimers.isEmpty()) {
                redisService.addList(redisDeviceTimerKey, deviceTimers);
            }
        }
        short timerMax = deviceTypeService.findDeviceTypeByModel(deviceInfoService.findDeviceByUuid(request.getUuid()).getDeviceType()).getTimerMaxNumber();
        // 验证上限 5个
        if (deviceTimers != null && !deviceTimers.isEmpty() && deviceTimers.size() >= timerMax) {
            logger.error(response.getMsg());
            return new TimerResponse(ErrorConstant.ERR_DEVICE_TIMER_MAX, ErrorConstant.ERR_DEVICE_TIMING_MSG);
        }

        // 根据counterTime获取执行时间
        Date endTime = MyDateUtils.getUtcDateByMillis(request.getCounterTime());

        // 全为空，需要验证是否有冲突
        if ((request.getConflictAwayIds() == null || request.getConflictAwayIds().isEmpty()) &&
                (request.getConflictScheduleIds() == null || request.getConflictScheduleIds().isEmpty()) &&
                (request.getConflictTimerIds() == null || request.getConflictTimerIds().isEmpty())) {
            logger.info("验证时间冲突");
            List<Integer> conflictAwayIds = validateTimerAway(request.getUuid(), endTime);
            List<Integer> conflictScheduleIds = validateTimerSchedule(request.getUuid(), endTime);
            List<Integer> conflictTimerIds = validateTimerSelf(request.getUuid(), null, endTime);
            if (!conflictAwayIds.isEmpty() || !conflictScheduleIds.isEmpty() || !conflictTimerIds.isEmpty()) {
                // response.setCode(ErrorConstant.ERR_DEVICE_TIMING);
//                response.setMsg(ErrorConstant.ERR_DEVICE_TIMING_MSG);
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
        // save
        DeviceTimer deviceTimer = new DeviceTimer();
        deviceTimer.setExecuteTime(endTime);
        // 启动次数1
        deviceTimer.setStartTimes(1);
        deviceTimer.setSeconds(Integer.valueOf(request.getCounterTime()));
        deviceTimer.setStatus("1");
        deviceTimer.setUuid(request.getUuid());
        deviceTimer.setDeviceId(deviceInfo.getId());
        deviceTimer.setDeviceCid(deviceInfo.getDeviceCid());
        deviceTimer.setAction(request.getAction());
        deviceTimer.setAccountId(request.getAccountId());
        deviceTimer.setCreateTime(MyDateUtils.getUtcDateTime());
        deviceTimer.setTurnonTime(MyDateUtils.getUtcDateTime());

        deviceTimerMapper.insertSelective(deviceTimer);

        // 设备增加timer定时
        VBMPResponse error = httpsAPIService.addTimerToDevice(deviceTimer, CommonConstant.ADD);
        if (error.getCode() != 0) {
            logger.error("操作设备定时出错");
            throw new ServiceException(error.getCode(), error.getMsg());
        }

        // 更新redis
        if (redisService.exists(redisDeviceTimerKey)) {
            redisService.addList(redisDeviceTimerKey, deviceTimer);
        }
        // 增加zset
        RedisDeviceTimer redisDeviceTimer = new RedisDeviceTimer();
        redisDeviceTimer.setId(deviceTimer.getId());
        redisDeviceTimer.setDeviceCid(deviceTimer.getDeviceCid());
        redisDeviceTimer.setDeviceId(deviceTimer.getDeviceId());
        redisDeviceTimer.setUuid(deviceTimer.getUuid());
        redisDeviceTimer.setExecuteTime(endTime);
        redisDeviceTimer.setAction(deviceTimer.getAction());
        redisDeviceTimer.setMinutes(MyDateUtils.getMinutsByDate(redisDeviceTimer.getExecuteTime()));
        redisService.addZSet(CommonConstant.TIMING_TIMER.concat(":").concat(deviceInfo.getDeviceType()), MyDateUtils.getMinutsByDate(redisDeviceTimer.getExecuteTime()), redisDeviceTimer);
        response.setTimerId(deviceTimer.getId());
        redisService.unlock(lockKey);
        return response;
    }

    private List<Integer> validateTimerAway(String uuid, Date endTime) {
        List<Integer> timerIds = new ArrayList<>();
        DeviceAway startAway = null;
        List<DeviceAway> startAways = deviceAwayMapper.selectByUuid(uuid);
        if (startAways != null && !startAways.isEmpty()) {
            startAway = startAways.get(0);
        }
        if (startAway != null && startAway.getId() != null && "1".equals(startAway.getStatus())) {
            String[] repeats = startAway.getAwayRepeat().split(",");
            int dbStartPlusDay = MyDateUtils.getPlusDay(startAway.getStartTime(), startAway.getTimeZone());
            int dbEndPlusDay = MyDateUtils.getPlusDay(startAway.getEndTime(), startAway.getTimeZone());
            for (String dbRepeat : repeats) {
                String start = startAway.getStartTime();
                String end = startAway.getEndTime();
                Date dbStartTime = MyDateUtils.getUtcDateByTimeAndZone(start, startAway.getTimeZone());
                Date dbEndTime = MyDateUtils.getUtcDateByTimeAndZone(end, startAway.getTimeZone());
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
                long minutes = MyDateUtils.compareMinute(start, end);
                if (minutes > 60L && minutes < 1380L) {
                    dbStartTime = MyDateUtils.addDate(dbStartTime, Calendar.MINUTE, -30);
                    dbEndTime = MyDateUtils.addDate(dbEndTime, Calendar.MINUTE, 30);
                } else if (minutes <= 60L) {
                    dbStartTime = MyDateUtils.addDate(dbStartTime, Calendar.MINUTE, -5);
                    dbEndTime = MyDateUtils.addDate(dbEndTime, Calendar.MINUTE, 5);
                }
                if (dbEndTime != null && dbEndTime.getTime() / 1000L < dbStartTime.getTime() / 1000L) {
                    Calendar calendar = Calendar.getInstance();
                    calendar.setTime(dbEndTime);
                    calendar.add(Calendar.DAY_OF_YEAR, 1);
                    dbEndTime = calendar.getTime();
                }
                // 判断时间线
                if (MyDateUtils.compareTimeLine(dbStartTime, dbEndTime, endTime)) {
                    timerIds.add(startAway.getId());
                }
            }
        }
        return timerIds.stream().distinct().collect(Collectors.toList());
    }

    private List<Integer> validateTimerSchedule(String uuid, Date endTime) {
        List<Integer> timerIds = new ArrayList<>();
        DeviceSchedule query = new DeviceSchedule();
        query.setUuid(uuid);
        query.setStatus("1");
        query.setEvent(CommonConstant.EVENT_SWITCH);
        List<DeviceSchedule> startSchedules = deviceScheduleMapper.select(query);
        for (DeviceSchedule startSchedule : startSchedules) {
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
                // 单点
                if (dbEndTime == null) {
                    // 这个单点时间在另一组时间范围内
                    if (MyDateUtils.compareTimeLine(endTime, dbStartTime)) {
                        timerIds.add(startSchedule.getId());
                    }
                } else {
                    // 开始时间和结束时间都小于已经开始了的开始时间
                    if (MyDateUtils.compareTimeLine(dbStartTime, dbEndTime, endTime)) {
                        timerIds.add(startSchedule.getId());
                    }
                }
            }

        }
        return timerIds.stream().distinct().collect(Collectors.toList());
    }

    private List<Integer> validateTimerSelf(String uuid, Integer timerId, Date endTime) {
        List<Integer> timerIds = new ArrayList<>();
        DeviceTimer query = new DeviceTimer();
        query.setUuid(uuid);
        query.setStatus("1");
        List<DeviceTimer> startedTimers = deviceTimerMapper.select(query);
        if (timerId != null) {
            startedTimers.removeIf(timer -> timer.getId().intValue() == timerId.intValue());
        }
        for (DeviceTimer startedTimer : startedTimers) {
            Date dbEndTime = startedTimer.getExecuteTime();
            // 判断时间线
            if (MyDateUtils.compareTimeLine(endTime, dbEndTime)) {
                timerIds.add(startedTimer.getId());
            }
        }
        return timerIds.stream().distinct().collect(Collectors.toList());
    }

    private void stopConflictAway(String uuid, List<Integer> conflictAwayIds) {
        if (conflictAwayIds != null && !conflictAwayIds.isEmpty()) {
            List<DeviceAway> startAways = deviceAwayMapper.selectByPrimaryKeys(conflictAwayIds);
            String redisDeviceTimerKey = CommonConstant.TIMING_AWAY.concat("-").concat(uuid);
            for (DeviceAway startAway : startAways) {
                // update away
                startAway.setStatus("0");
                deviceAwayMapper.updateByPrimaryKey(startAway);
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

    private void stopConflictSchedule(String uuid, List<Integer> conflictTimerIds) {
        if (conflictTimerIds != null && !conflictTimerIds.isEmpty()) {
            List<DeviceSchedule> conflictSchedules = deviceScheduleMapper.selectByPrimaryKeys(conflictTimerIds);
            Iterator<DeviceSchedule> scheduleIterator = conflictSchedules.iterator();
            while (scheduleIterator.hasNext()) {
                DeviceSchedule schedule = scheduleIterator.next();
                schedule.setStatus("0");
            }
            // stop schedule in db
            deviceScheduleMapper.updateStatusByIds(conflictTimerIds, "0");
            // update redis
            String redisDeviceScheduleKey = CommonConstant.TIMING_SCHEDULE.concat("-").concat(uuid);
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
            if (redisService.exists(CommonConstant.TIMING_SCHEDULE)) {
                Set<RedisDeviceSchedule> redisDeviceScheuleSet = (Set<RedisDeviceSchedule>) redisService.getZSetRange(CommonConstant.TIMING_SCHEDULE);
                if (redisDeviceScheuleSet != null && !redisDeviceScheuleSet.isEmpty()) {
                    for (RedisDeviceSchedule redisDeviceScheule : redisDeviceScheuleSet) {
                        for (DeviceSchedule conflictSchedule : conflictSchedules) {
                            if (redisDeviceScheule.getId().intValue() == conflictSchedule.getId().intValue()) {
                                redisService.removeZSetValue(CommonConstant.TIMING_SCHEDULE, redisDeviceScheule);
                            }
                        }
                    }
                }
            }
            // 停设备里的schedule
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
                httpsAPIService.addScheduleToDevice(schedule, CommonConstant.UPD);
            }

        }
    }

    private void stopConflictTimer(String uuid, List<Integer> conflictTimerIds) {
        if (conflictTimerIds != null && !conflictTimerIds.isEmpty()) {
            DeviceInfo deviceInfo = deviceInfoService.findDeviceByUuid(uuid);
            List<DeviceTimer> conflictTimers = deviceTimerMapper.selectByPrimaryKeys(conflictTimerIds);
            Iterator<DeviceTimer> iterator = conflictTimers.iterator();
            while (iterator.hasNext()) {
                DeviceTimer timer = iterator.next();
                timer.setStatus("0");
            }
            // stop timer in db
            deviceTimerMapper.updateStatusByIds(conflictTimerIds, "0");
            // update redis
            String redisDeviceTimerKey = CommonConstant.TIMING_TIMER.concat(":").concat(deviceInfo.getDeviceType()).concat("-").concat(uuid);
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
            if (redisService.exists(CommonConstant.TIMING_TIMER.concat(":").concat(deviceInfo.getDeviceType()))) {
                Set<RedisDeviceTimer> redisTimerSet = (Set<RedisDeviceTimer>) redisService.getZSetRange(CommonConstant.TIMING_TIMER.concat(":").concat(deviceInfo.getDeviceType()));
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
            // 更新设备上的timer
            for (DeviceTimer timer : conflictTimers) {
                httpsAPIService.addTimerToDevice(timer, CommonConstant.UPD);
            }

        }
    }

    /**
     * 删除Timer
     *
     * @param timerId
     * @return
     * @throws Exception
     */
    @Override
    public TimerResponse deleteTimer(Integer timerId) {
        TimerResponse response = new TimerResponse();
        // 验证存在性
        DeviceTimer deviceTimer = deviceTimerMapper.selectByPrimaryKey(timerId);
        if (deviceTimer == null || deviceTimer.getId() == null) {
            response.setCode(ErrorConstant.ERR_DEVICE_TIMER_NOT_EXIST);
            response.setMsg(ErrorConstant.ERR_DEVICE_TIMER_NOT_EXIST_MSG);
            logger.error(response.getMsg());
            return response;
        }

        String lockKey = CommonConstant.DEVICE_TIMER_LOCK_PREFIX + "del-" + deviceTimer.getUuid();
        redisService.lock(lockKey);


        // 删除设备timer
        VBMPResponse error = httpsAPIService.delTimerFromDevice(deviceTimer);
        if (error.getCode() != 0) {
            logger.error("操作设备定时出错");
            throw new ServiceException(error.getCode(), error.getMsg());
        }

        deviceTimerMapper.deleteByPrimaryKey(timerId);
        DeviceInfo deviceInfo = deviceInfoService.findDeviceByUuid(deviceTimer.getUuid());
        // 更新redis
        String redisDeviceTimerKey = CommonConstant.TIMING_TIMER.concat(":").concat(deviceInfo.getDeviceType()).concat("-").concat(deviceTimer.getUuid());
        if (redisService.exists(redisDeviceTimerKey)) {
            List<DeviceTimer> redisDeviceTimers = redisService.getList(redisDeviceTimerKey);
            if (redisDeviceTimers != null && !redisDeviceTimers.isEmpty()) {
                for (DeviceTimer ds : redisDeviceTimers) {
                    if (ds.getId().intValue() == timerId.intValue()) {
                        redisService.removeListValue(redisDeviceTimerKey, ds);
                        break;
                    }
                }
            }
        }
        if ("1".equals(deviceTimer.getStatus())) {
            if (redisService.exists(CommonConstant.TIMING_TIMER.concat(":").concat(deviceInfo.getDeviceType()))) {
                Set<RedisDeviceTimer> redisDeivceTimerSet = (Set<RedisDeviceTimer>) redisService.getZSetRange(CommonConstant.TIMING_TIMER.concat(":").concat(deviceInfo.getDeviceType()));
                if (redisDeivceTimerSet != null && !redisDeivceTimerSet.isEmpty()) {
                    for (RedisDeviceTimer redisDeviceTimer : redisDeivceTimerSet) {
                        if (redisDeviceTimer.getId().intValue() == deviceTimer.getId().intValue()) {
                            redisService.removeZSetValue(CommonConstant.TIMING_TIMER.concat(":").concat(deviceInfo.getDeviceType()), redisDeviceTimer);
                            break;
                        }
                    }
                }
            }
        }
        redisService.unlock(lockKey);
        return response;
    }

    /**
     * 更新Timer
     *
     * @param request
     * @return
     * @throws Exception
     */
    @Override
    @Transactional(rollbackFor = Exception.class)
    public TimerResponse updateTimer(TimerRequest request) {
        TimerResponse response = new TimerResponse();
        // 验证存在性
        DeviceTimer deviceTimer = deviceTimerMapper.selectByPrimaryKey(request.getTimerId());
        if (deviceTimer == null || deviceTimer.getId() == null) {
            response.setCode(ErrorConstant.ERR_DEVICE_TIMER_NOT_EXIST);
            response.setMsg(ErrorConstant.ERR_DEVICE_TIMER_NOT_EXIST_MSG);
            logger.error(response.getMsg());
            return response;
        }

        String lockKey = CommonConstant.DEVICE_TIMER_LOCK_PREFIX + "upd-" + deviceTimer.getUuid();
        redisService.lock(lockKey);


        // 根据counterTime获取执行时间
        Date endTime = MyDateUtils.getUtcDateByMillis(request.getCounterTime());


        Map<String, Object> params = new HashMap<>();
        params.put("uuid", deviceTimer.getUuid());
        params.put("timerStatus", "1");
        params.put("awayStatus", "1");
        params.put("scheduleState", "1"); // 开启状态


        if ("1".equals(request.getStatus())) {
            deviceTimer.setExecuteTime(endTime);
            deviceTimer.setTurnonTime(MyDateUtils.getUtcDateTime());
            // 启动次数+1
            if ("0".equals(deviceTimer.getStatus())) {
                deviceTimer.setStartTimes(deviceTimer.getStartTimes() == null ? 1 : deviceTimer.getStartTimes() + 1);
            }

            // 全为空，需要验证是否有冲突
            if ((request.getConflictAwayIds() == null || request.getConflictAwayIds().isEmpty()) &&
                    (request.getConflictScheduleIds() == null || request.getConflictScheduleIds().isEmpty()) &&
                    (request.getConflictTimerIds() == null || request.getConflictTimerIds().isEmpty())) {
                logger.info("验证时间冲突");
                List<Integer> conflictAwayIds = validateTimerAway(deviceTimer.getUuid(), endTime);
                List<Integer> conflictScheduleIds = validateTimerSchedule(deviceTimer.getUuid(), endTime);
                List<Integer> conflictTimerIds = validateTimerSelf(deviceTimer.getUuid(), deviceTimer.getId(), endTime);
                if (!conflictAwayIds.isEmpty() || !conflictScheduleIds.isEmpty() || !conflictTimerIds.isEmpty()) {
                    response.setCode(ErrorConstant.ERR_DEVICE_TIMING);
                    response.setMsg(ErrorConstant.ERR_DEVICE_TIMING_MSG);
                    response.setConflictAwayIds(conflictAwayIds);
                    response.setConflictScheduleIds(conflictScheduleIds);
                    response.setConflictTimerIds(conflictTimerIds);
                    return response;
                } // else 表示 没有冲突
            } else { // 传了冲突的ID过来，表示需要停止冲突timing
                stopConflictAway(deviceTimer.getUuid(), request.getConflictAwayIds());
                stopConflictSchedule(deviceTimer.getUuid(), request.getConflictScheduleIds());
                stopConflictTimer(deviceTimer.getUuid(), request.getConflictTimerIds());
            }

        }
        deviceTimer.setStatus(request.getStatus());
        deviceTimer.setSeconds(Integer.valueOf(request.getCounterTime()));
        deviceTimer.setAction(request.getAction());

        // 更新设备timer
        VBMPResponse error = httpsAPIService.addTimerToDevice(deviceTimer, CommonConstant.UPD);
        if (error.getCode() != 0) {
            logger.error("操作设备定时出错");
            throw new ServiceException(error.getCode(), error.getMsg());
        }

        deviceTimerMapper.updateByPrimaryKey(deviceTimer);
        DeviceInfo deviceInfo = deviceInfoService.findDeviceByUuid(deviceTimer.getUuid());
        // 更新redis
        String redisDeviceTimerKey = CommonConstant.TIMING_TIMER.concat(":").concat(deviceInfo.getDeviceType()).concat("-").concat(deviceTimer.getUuid());
        if (redisService.exists(redisDeviceTimerKey)) {
            List<DeviceTimer> redisDeviceTimers = redisService.getList(redisDeviceTimerKey);
            if (redisDeviceTimers != null && !redisDeviceTimers.isEmpty()) {
                for (DeviceTimer ds : redisDeviceTimers) {
                    if (ds.getId().intValue() == deviceTimer.getId().intValue()) {
                        redisService.removeListValue(redisDeviceTimerKey, ds);
                        break;
                    }
                }
            }
        }
        redisService.addList(redisDeviceTimerKey, deviceTimer);
        // remove timer zset
        if (redisService.exists(CommonConstant.TIMING_TIMER.concat(":").concat(deviceInfo.getDeviceType()))) {
            Set<RedisDeviceTimer> redisDeivceTimerSet = (Set<RedisDeviceTimer>) redisService.getZSetRange(CommonConstant.TIMING_TIMER.concat(":").concat(deviceInfo.getDeviceType()));
            if (redisDeivceTimerSet != null && !redisDeivceTimerSet.isEmpty()) {
                for (RedisDeviceTimer redisDeviceTimer : redisDeivceTimerSet) {
                    if (redisDeviceTimer.getId().intValue() == deviceTimer.getId().intValue()) {
                        redisService.removeZSetValue(CommonConstant.TIMING_TIMER.concat(":").concat(deviceInfo.getDeviceType()), redisDeviceTimer);
                        break;
                    }
                }
            }
        }
        // 开启的时候还需要增加进去
        if ("1".equals(request.getStatus())) {
            RedisDeviceTimer RedisDeviceTimer = new RedisDeviceTimer();
            RedisDeviceTimer.setId(deviceTimer.getId());
            RedisDeviceTimer.setUuid(deviceTimer.getUuid());
            RedisDeviceTimer.setExecuteTime(endTime);
            RedisDeviceTimer.setAction(deviceTimer.getAction());
            RedisDeviceTimer.setMinutes(MyDateUtils.getMinutsByDate(RedisDeviceTimer.getExecuteTime()));
            redisService.addZSet(CommonConstant.TIMING_TIMER.concat(":").concat(deviceInfo.getDeviceType()), MyDateUtils.getMinutsByDate(RedisDeviceTimer.getExecuteTime()), RedisDeviceTimer);
        }
        redisService.unlock(lockKey);
        return response;
    }

    /**
     * 更新Timer状态
     *
     * @param request request
     * @return
     * @throws Exception
     */
    @Override
    public TimerResponse updateTimerStatus(TimerRequest request) {
        TimerResponse response = new TimerResponse();
        // 验证存在性
        DeviceTimer deviceTimer = deviceTimerMapper.selectByPrimaryKey(request.getTimerId());
        if (deviceTimer == null || deviceTimer.getId() == null) {
            response.setCode(ErrorConstant.ERR_DEVICE_TIMER_NOT_EXIST);
            response.setMsg(ErrorConstant.ERR_DEVICE_TIMER_NOT_EXIST_MSG);
            logger.error(response.getMsg());
            return response;
        }

        String lockKey = CommonConstant.DEVICE_TIMER_LOCK_PREFIX + "sta-" + deviceTimer.getUuid();
        redisService.lock(lockKey);


        if ("1".equals(request.getStatus())) { // 开始倒计时 需要计算startTime，并且关闭其它冲突的Timer

            // 根据counterTime获取执行时间
            Date endTime = MyDateUtils.getUtcDateByMillis(deviceTimer.getSeconds().toString());

            deviceTimer.setTurnonTime(MyDateUtils.getUtcDateTime());
            Map<String, Object> params = new HashMap<>();
            params.put("uuid", deviceTimer.getUuid());
            params.put("timerStatus", "1");
            params.put("awayStatus", "1");
            params.put("scheduleState", "1"); // 开启状态

            deviceTimer.setExecuteTime(endTime);
            // 启动次数+1
            if ("0".equals(deviceTimer.getStatus())) {
                deviceTimer.setStartTimes(deviceTimer.getStartTimes() == null ? 1 : deviceTimer.getStartTimes() + 1);
            }

            // 全为空，需要验证是否有冲突
            if ((request.getConflictAwayIds() == null || request.getConflictAwayIds().isEmpty()) &&
                    (request.getConflictScheduleIds() == null || request.getConflictScheduleIds().isEmpty()) &&
                    (request.getConflictTimerIds() == null || request.getConflictTimerIds().isEmpty())) {
                logger.info("验证时间冲突");
                List<Integer> conflictAwayIds = validateTimerAway(deviceTimer.getUuid(), endTime);
                List<Integer> conflictScheduleIds = validateTimerSchedule(deviceTimer.getUuid(), endTime);
                List<Integer> conflictTimerIds = validateTimerSelf(deviceTimer.getUuid(), deviceTimer.getId(), endTime);
                if (!conflictAwayIds.isEmpty() || !conflictScheduleIds.isEmpty() || !conflictTimerIds.isEmpty()) {
                    response.setCode(ErrorConstant.ERR_DEVICE_TIMING);
                    response.setMsg(ErrorConstant.ERR_DEVICE_TIMING_MSG);
                    response.setConflictAwayIds(conflictAwayIds);
                    response.setConflictScheduleIds(conflictScheduleIds);
                    response.setConflictTimerIds(conflictTimerIds);
                    return response;
                } // else 表示 没有冲突
            } else { // 传了冲突的ID过来，表示需要停止冲突timing
                stopConflictAway(deviceTimer.getUuid(), request.getConflictAwayIds());
                stopConflictSchedule(deviceTimer.getUuid(), request.getConflictScheduleIds());
                stopConflictTimer(deviceTimer.getUuid(), request.getConflictTimerIds());
            }

        }
        deviceTimer.setStatus(request.getStatus());

        /**********************启动时硬件不响应BUG************************/
        int temp = deviceTimer.getSeconds();
        deviceTimer.setSeconds(0);
        VBMPResponse rempError = httpsAPIService.addTimerToDevice(deviceTimer, CommonConstant.UPD);
        if (rempError.getCode() != 0) {
            logger.error("操作设备定时出错");
            throw new ServiceException(rempError.getCode(), rempError.getMsg());
        }
        /**********************启动时硬件不响应BUG************************/

        deviceTimer.setSeconds(temp);
        VBMPResponse error = httpsAPIService.addTimerToDevice(deviceTimer, CommonConstant.UPD);
        if (error.getCode() != 0) {
            logger.error("操作设备定时出错");
            throw new ServiceException(error.getCode(), error.getMsg());
        }
        DeviceInfo deviceInfo = deviceInfoService.findDeviceByUuid(deviceTimer.getUuid());
        deviceTimerMapper.updateByPrimaryKey(deviceTimer);
        // update redis current timer
        String redisDeviceTimerKey = CommonConstant.TIMING_TIMER.concat(":").concat(deviceInfo.getDeviceType()).concat("-").concat(deviceTimer.getUuid());
        if (redisService.exists(redisDeviceTimerKey)) {
            List<DeviceTimer> redisDeviceTimes = redisService.getList(redisDeviceTimerKey);
            if (redisDeviceTimes != null && !redisDeviceTimes.isEmpty()) {
                for (DeviceTimer rdt : redisDeviceTimes) {
                    if (rdt.getId().intValue() == deviceTimer.getId().intValue()) {
                        redisService.removeListValue(redisDeviceTimerKey, rdt);
                        break;
                    }
                }
            }
        }
        redisService.addList(redisDeviceTimerKey, deviceTimer);

        // remove timer zset
        if (redisService.exists(CommonConstant.TIMING_TIMER.concat(":").concat(deviceInfo.getDeviceType()))) {
            Set<RedisDeviceTimer> redisDeivceTimerSet = (Set<RedisDeviceTimer>) redisService.getZSetRange(CommonConstant.TIMING_TIMER.concat(":").concat(deviceInfo.getDeviceType()));
            if (redisDeivceTimerSet != null && !redisDeivceTimerSet.isEmpty()) {
                for (RedisDeviceTimer redisDeviceTimer : redisDeivceTimerSet) {
                    if (redisDeviceTimer.getId().intValue() == deviceTimer.getId().intValue()) {
                        redisService.removeZSetValue(CommonConstant.TIMING_TIMER.concat(":").concat(deviceInfo.getDeviceType()), redisDeviceTimer);
                        break;
                    }
                }
            }
        }
        // 开启的时候还需要增加进去
        if ("1".equals(request.getStatus())) {
            RedisDeviceTimer RedisDeviceTimer = new RedisDeviceTimer();
            RedisDeviceTimer.setId(deviceTimer.getId());
            RedisDeviceTimer.setDeviceId(deviceTimer.getDeviceId());
            RedisDeviceTimer.setDeviceCid(deviceTimer.getDeviceCid());
            RedisDeviceTimer.setUuid(deviceTimer.getUuid());
            RedisDeviceTimer.setExecuteTime(deviceTimer.getExecuteTime());
            RedisDeviceTimer.setAction(deviceTimer.getAction());
            RedisDeviceTimer.setMinutes(MyDateUtils.getMinutsByDate(RedisDeviceTimer.getExecuteTime()));
            redisService.addZSet(CommonConstant.TIMING_TIMER.concat(":").concat(deviceInfo.getDeviceType()), MyDateUtils.getMinutsByDate(RedisDeviceTimer.getExecuteTime()), RedisDeviceTimer);
        }
        redisService.unlock(lockKey);
        return response;
    }

    /**
     * 获取Timer
     *
     * @param request
     * @return
     * @throws Exception
     */
    @Override
    public TimerResponse getTimers(TimerRequest request) {
        TimerResponse response = new TimerResponse();
        String uuid = request.getUuid();

        String lockKey = CommonConstant.DEVICE_TIMER_LOCK_PREFIX + "get-" + uuid;
        redisService.lock(lockKey);
        DeviceInfo deviceInfo = deviceInfoService.findDeviceByUuid(request.getUuid());
        List<DeviceTimer> redisDeviceTimers = null;
        List<DeviceTimer> deviceTimers = new ArrayList<>();
        String redisDeviceTimerKey = CommonConstant.TIMING_TIMER.concat(":").concat(deviceInfo.getDeviceType()).concat("-").concat(uuid);
        if (redisService.exists(redisDeviceTimerKey)) {
            redisDeviceTimers = redisService.getList(redisDeviceTimerKey);
        }
        if (redisDeviceTimers == null || redisDeviceTimers.isEmpty()) {
            redisDeviceTimers = deviceTimerMapper.selectByUuid(uuid);
            if (redisDeviceTimers != null && !redisDeviceTimers.isEmpty()) {
                // 存入redis
                redisService.addList(redisDeviceTimerKey, redisDeviceTimers);
                redisService.setExpireTime(redisDeviceTimerKey, CommonConstant.SECONDS_OF_ONEDAY);
            }
        }
        if (redisDeviceTimers == null || redisDeviceTimers.isEmpty()) {
            response.setCode(ErrorConstant.ERR_DEVICE_TIMER);
            response.setMsg(ErrorConstant.ERR_DEVICE_TIMER_NOT_EXIST_MSG);
            logger.info("无此设备相关Timer");
            return response;
        }
        // 去重
        redisDeviceTimers = redisDeviceTimers.stream().distinct().collect(Collectors.toList());
        // 按启动次数倒序
        redisDeviceTimers.sort(Comparator.comparing(DeviceTimer::getCreateTime).reversed());
        redisDeviceTimers.sort(Comparator.comparing(DeviceTimer::getStartTimes).reversed());
        // 封装返回
        for (DeviceTimer timer : redisDeviceTimers) {
            DeviceTimerView deviceTimer = new DeviceTimerView();
            deviceTimer.setId(timer.getId());
            deviceTimer.setCreateTime(timer.getCreateTime());
            deviceTimer.setSeconds(timer.getSeconds());
            deviceTimer.setAction(timer.getAction());
            deviceTimer.setUuid(timer.getUuid());
            deviceTimer.setStatus(timer.getStatus());
            // 计算已经开始的Timer剩余时间
            if ("1".equals(timer.getStatus())) {
                Date now = MyDateUtils.getUtcDateTime();
                Long time = (now.getTime() - timer.getTurnonTime().getTime()) / 1000L;
                Long restTime = Long.valueOf(timer.getSeconds()) - time;
                if (restTime < 0L) {
                    restTime = 0L;
                }
                deviceTimer.setCounterTimer(restTime.intValue());
            } else {
                deviceTimer.setCounterTimer(timer.getSeconds());
            }
            deviceTimers.add(deviceTimer);
        }
        response.setTimers(deviceTimers);
        redisService.unlock(lockKey);
        return response;
    }

    /**
     * 根据UUID删除所有Timer
     *
     * @param uuid UUID
     */
    @Override
    public TimerResponse deleteTimerByUuid(String uuid) {
        TimerResponse response = new TimerResponse();
        DeviceTimer query = new DeviceTimer();
        query.setUuid(uuid);
        List<DeviceTimer> startedDeviceTimers = deviceTimerMapper.select(query);
        deviceTimerMapper.delete(query);
        DeviceInfo deviceInfo = deviceInfoService.findDeviceByUuid(uuid);
        String redisTimerKey = CommonConstant.TIMING_TIMER.concat(":").concat(deviceInfo.getDeviceType()).concat("-").concat(uuid);
        if (redisService.exists(redisTimerKey)) {
            redisService.remove(redisTimerKey);
        }
        Set<RedisDeviceTimer> redisDeviceTimers = null;
        if (redisService.exists(CommonConstant.TIMING_TIMER.concat(":").concat(deviceInfo.getDeviceType()))) {
            redisDeviceTimers = (Set<RedisDeviceTimer>) redisService.getZSetRange(CommonConstant.TIMING_TIMER.concat(":").concat(deviceInfo.getDeviceType()));
        }
        if (startedDeviceTimers != null && !startedDeviceTimers.isEmpty() && redisDeviceTimers != null && !redisDeviceTimers.isEmpty()) {
            for (DeviceTimer startedDeviceTimer : startedDeviceTimers) {
                for (RedisDeviceTimer redisDeviceTimer : redisDeviceTimers) {
                    if (redisDeviceTimer.getId().intValue() == startedDeviceTimer.getId().intValue()) {
                        redisService.removeZSetValue(CommonConstant.TIMING_TIMER.concat(":").concat(deviceInfo.getDeviceType()), redisDeviceTimer);
                    }
                }
            }
        }
        return response;
    }

    @Override
    public TimerResponse stopTimerByPrimaryKey(Integer timerId) {
        TimerResponse response = new TimerResponse();
        // 验证存在性
        DeviceTimer deviceTimer = deviceTimerMapper.selectByPrimaryKey(timerId);
        if (deviceTimer == null || deviceTimer.getId() == null) {
            response.setCode(ErrorConstant.ERR_DEVICE_TIMER_NOT_EXIST);
            response.setMsg(ErrorConstant.ERR_DEVICE_TIMER_NOT_EXIST_MSG);
            logger.error(response.getMsg());
            return response;
        }
        List<Integer> ids = new ArrayList<>();
        ids.add(timerId);
        deviceTimerMapper.updateStatusByIds(ids, "0");
        return response;
    }

    @Override
    public TimerResponse updateUuidByUuid(String oldUuid, String newUuid) {
        TimerResponse response = new TimerResponse();
        DeviceInfo deviceInfo = deviceInfoService.findDeviceByUuid(oldUuid);
        String redisKey = CommonConstant.TIMING_TIMER.concat(":").concat(deviceInfo.getDeviceType()).concat("-").concat(oldUuid);
        if (redisService.exists(redisKey)) {
            redisService.remove(redisKey);
        }
        Set<RedisDeviceTimer> redisAwaySet = null;
        if (redisService.exists(CommonConstant.TIMING_TIMER.concat(":").concat(deviceInfo.getDeviceType()))) {
            redisAwaySet = (Set<RedisDeviceTimer>) redisService.getZSetRange(CommonConstant.TIMING_TIMER.concat(":").concat(deviceInfo.getDeviceType()));
        }
        if (redisAwaySet != null && !redisAwaySet.isEmpty()) {
            for (RedisDeviceTimer redisAway : redisAwaySet) {
                if (redisAway.getUuid().equals(oldUuid)) {
                    redisService.removeZSetValue(CommonConstant.TIMING_TIMER.concat(":").concat(deviceInfo.getDeviceType()), redisAway);
                    redisAway.setUuid(newUuid);
                    redisService.addZSet(CommonConstant.TIMING_TIMER.concat(":").concat(deviceInfo.getDeviceType()), MyDateUtils.getMinutsByDate(redisAway.getExecuteTime()), redisAway);
                }
            }
        }

        deviceTimerMapper.updateUuidByUuid(oldUuid, newUuid);

        return response;
    }
}
