//package com.etekcity.vbmp.timing.modules.task;
//
//import com.etekcity.vbmp.timing.common.redis.RedisService;
//import com.etekcity.vbmp.timing.common.service.impl.HttpsAPIService;
//import com.etekcity.vbmp.timing.constant.CommonConstant;
//import com.etekcity.vbmp.timing.modules.schedule.bean.DeviceSchedule;
//import com.etekcity.vbmp.timing.modules.schedule.bean.RedisDeviceSchedule;
//import com.etekcity.vbmp.timing.modules.schedule.dao.DeviceScheduleMapper;
//import com.etekcity.vbmp.timing.modules.schedule.service.ScheduleService;
//import com.etekcity.vbmp.timing.util.MyDateUtils;
//import org.slf4j.Logger;
//import org.slf4j.LoggerFactory;
//import org.springframework.beans.factory.annotation.Autowired;
//import org.springframework.beans.factory.annotation.Value;
//import org.springframework.scheduling.annotation.Scheduled;
//import org.springframework.stereotype.Component;
//
//import java.util.*;
//
//@Component
//public class ScheduleTask {
//
//    @Value("${schedule.redis.key}")
//    private String key;
//
//    private String value = UUID.randomUUID().toString();
//    @Autowired
//    private RedisService redisService;
//
//    @Autowired
//    private ScheduleService scheduleService;
//    @Autowired
//    private DeviceScheduleMapper scheduleMapper;
//
//    @Autowired
//    private HttpsAPIService httpsAPIService;
//
//    private Logger logger = LoggerFactory.getLogger(ScheduleTask.class);
//
//    @Scheduled(cron = "0 0/1 * * * ?")
//    public void deviceScheduleTask() {
//        logger.debug("away task is running. need redis value is {}", value);
//        if (!redisService.exists(key)) {
//            redisService.set(key, value, 300L);
//        }
//        if (value.equals(redisService.get(key))) {
//            Set<RedisDeviceSchedule> redisSchedules = null;
//            long curr = MyDateUtils.getMinutsByDate(MyDateUtils.getUtcDateTime()); // 获取当前UTC分钟数
//            long start = curr - CommonConstant.OFFSET_TIME;
//            logger.debug("current time is:{}, start time is:{}", curr, start);
//            if (redisService.exists(CommonConstant.TIMING_SCHEDULE)) {
//                redisSchedules = (Set<RedisDeviceSchedule>) redisService.getZSetRangeByScore(CommonConstant.TIMING_SCHEDULE, start, curr);
//            }
//            if (redisSchedules != null && !redisSchedules.isEmpty()) {
//                for (RedisDeviceSchedule redisSchedule : redisSchedules) {
//                    String[] repeats = redisSchedule.getRepeat().split(",");
//                    for (String repeat : repeats) {
//                        boolean startFlag = false; // 是否执行
//                        Date startTime = redisSchedule.getExecuteStartTime();
//                        if (redisSchedule.getExecuteStartTime() == null) {
//                            startTime = redisSchedule.getExecuteEndTime();
//                        }
//                        long minutes = redisSchedule.getMinutes();
//                        if (!"7".equals(repeat)) { // 7不重复 0-6重复
//                            startTime = MyDateUtils.getUtcDateByUtcDateAndWeek(startTime, repeat);
//                            minutes = MyDateUtils.getMinutsByDate(startTime);
//                        }
//                        if (Math.abs(minutes - curr) <= CommonConstant.OFFSET_TIME) {
//                            startFlag = true;
//                        }
//                        // 不重复定时执行后，需要删除ZSET并在数据中停止此定时
//                        if ("7".equals(redisSchedule.getRepeat()) && startFlag) {
//                            redisService.removeZSetValue(CommonConstant.TIMING_SCHEDULE, redisSchedule);
//                            DeviceSchedule deviceSchedule = scheduleMapper.selectByPrimaryKey(redisSchedule.getId());
//                            if ((deviceSchedule.getExecuteEndTime() == null && CommonConstant.START.equals(redisSchedule.getType())) || CommonConstant.END.equals(redisSchedule.getType())) {
//                                // 更新缓存
//                                String redisDeviceScheduleKey = CommonConstant.TIMING_SCHEDULE.concat("-").concat(deviceSchedule.getUuid());
//                                if (redisService.exists(redisDeviceScheduleKey)) {
//                                    List<DeviceSchedule> redisDeviceSchedules = redisService.getList(redisDeviceScheduleKey);
//                                    if (redisDeviceSchedules != null && !redisDeviceSchedules.isEmpty()) {
//                                        for (DeviceSchedule ds : redisDeviceSchedules) {
//                                            if (ds.getId().intValue() == deviceSchedule.getId().intValue()) {
//                                                redisService.removeListValue(redisDeviceScheduleKey, ds);
//                                                deviceSchedule.setStatus("0");
//                                            }
//                                        }
//                                    }
//                                }
//                                redisService.addList(redisDeviceScheduleKey, deviceSchedule);
//                                // 更新数据库为停止
//                                scheduleService.stopScheduleByPrimaryKey(redisSchedule.getId());
//                            }
//                        }
//                    }
//                    // 重复任务，增加加一天
//                    if (!"7".equals(redisSchedule.getRepeat())) {
//                        // 移除当前记录
//                        redisService.removeZSetValue(CommonConstant.TIMING_SCHEDULE, redisSchedule);
//                        // 增加一天
//                        Calendar calendar = Calendar.getInstance();
//                        int type = 0;//0开始 1结束
//                        calendar.setTime(redisSchedule.getExecuteStartTime());
//                        if (redisSchedule.getExecuteStartTime() == null) {
//                            type = 1;
//                            calendar.setTime(redisSchedule.getExecuteEndTime());
//                        }
//                        calendar.add(Calendar.DAY_OF_YEAR, 1);
//                        if (type == 0) {
//                            redisSchedule.setExecuteStartTime(calendar.getTime());
//                        } else {
//                            redisSchedule.setExecuteEndTime(calendar.getTime());
//                        }
//                        redisSchedule.setMinutes(MyDateUtils.getMinutsByDate(calendar.getTime()));
//                        // 更新后的加入到redis
//                        redisService.addZSet(CommonConstant.TIMING_SCHEDULE, MyDateUtils.getMinutsByDate(calendar.getTime()), redisSchedule);
//                    }
//                }
//            }
//        }
//    }
//
//
//}
