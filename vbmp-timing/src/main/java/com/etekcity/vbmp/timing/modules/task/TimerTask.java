//package com.etekcity.vbmp.timing.modules.task;
//
//import com.etekcity.vbmp.timing.common.redis.RedisService;
//import com.etekcity.vbmp.timing.constant.CommonConstant;
//import com.etekcity.vbmp.timing.modules.timer.bean.DeviceTimer;
//import com.etekcity.vbmp.timing.modules.timer.bean.RedisDeviceTimer;
//import com.etekcity.vbmp.timing.modules.timer.service.TimerService;
//import com.etekcity.vbmp.timing.util.MyDateUtils;
//import org.slf4j.Logger;
//import org.slf4j.LoggerFactory;
//import org.springframework.beans.factory.annotation.Autowired;
//import org.springframework.beans.factory.annotation.Value;
//import org.springframework.scheduling.annotation.Scheduled;
//import org.springframework.stereotype.Component;
//
//import java.util.List;
//import java.util.Set;
//import java.util.UUID;
//
//@Component
//public class TimerTask {
//
//
//    @Autowired
//    private RedisService redisService;
//
//
//    @Autowired
//    private TimerService timerService;
//
//    @Value("${schedule.redis.key}")
//    private String key;
//
//    private String value = UUID.randomUUID().toString();
//
//    private Logger logger = LoggerFactory.getLogger(TimerTask.class);
//
//    @Scheduled(cron = "0 0/1 * * * ?")
//    public void deviceTimerTask() {
//        logger.debug("away task is running. need redis value is {}", value);
//        if (!redisService.exists(key)) {
//            redisService.set(key, value, 300L);
//        }
//        if (value.equals(redisService.get(key))) {
//            Set<RedisDeviceTimer> redisTimers = null;
//            long curr = MyDateUtils.getMinutsByDate(MyDateUtils.getUtcDateTime()); // 获取当前UTC分钟数
//            long start = curr - CommonConstant.OFFSET_TIME;
//            logger.debug("current time is:{}, start time is:{}", curr, start);
//            if (redisService.exists(CommonConstant.TIMING_TIMER)) {
//                redisTimers = (Set<RedisDeviceTimer>) redisService.getZSetRangeByScore(CommonConstant.TIMING_TIMER, start, curr);
//            }
//            if (redisTimers != null && !redisTimers.isEmpty()) {
//                for (RedisDeviceTimer redisTimer : redisTimers) {
//                    boolean startFlag = false; // 是否执行
//                    long minuts = redisTimer.getMinutes();
//                    if (Math.abs(minuts - curr) <= CommonConstant.OFFSET_TIME) {
//                        startFlag = true;
//                    }
//                    // 执行了的Timer
//                    if (startFlag) {
//                        // 更新缓存
//                        String redisDeviceTimerKey = CommonConstant.TIMING_TIMER.concat("-").concat(redisTimer.getUuid());
//                        List<DeviceTimer> redisDeviceTimers = redisService.getList(redisDeviceTimerKey);
//                        if (redisDeviceTimers != null && !redisDeviceTimers.isEmpty()) {
//                            for (DeviceTimer deviceTimer : redisDeviceTimers) {
//                                if (deviceTimer.getId().intValue() == redisTimer.getId().intValue()) {
//                                    redisService.removeListValue(redisDeviceTimerKey, deviceTimer);
//                                    deviceTimer.setStatus("0");
//                                    redisService.addList(redisDeviceTimerKey, deviceTimer);
//                                }
//                            }
//                        }
//                        // 数据库 中更新为停止
//                        timerService.stopTimerByPrimaryKey(redisTimer.getId());
//                        // zset 中移除
//                        redisService.removeZSetValue(CommonConstant.TIMING_TIMER, redisTimer);
//                    }
//                }
//            }
//        }
//
//    }
//
//}
//
