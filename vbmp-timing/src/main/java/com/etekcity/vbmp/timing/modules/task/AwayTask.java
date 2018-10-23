//package com.etekcity.vbmp.timing.modules.task;
//
//import com.alibaba.fastjson.JSONObject;
//import com.etekcity.vbmp.timing.common.redis.RedisService;
//import com.etekcity.vbmp.timing.common.service.impl.HttpsAPIService;
//import com.etekcity.vbmp.timing.constant.CommonConstant;
//import com.etekcity.vbmp.timing.modules.away.bean.DeviceAway;
//import com.etekcity.vbmp.timing.modules.away.bean.RedisDeviceAway;
//import com.etekcity.vbmp.timing.modules.away.dao.DeviceAwayMapper;
//import com.etekcity.vbmp.timing.modules.away.service.AwayService;
//import com.etekcity.vbmp.timing.util.MyDateUtils;
//import org.slf4j.Logger;
//import org.slf4j.LoggerFactory;
//import org.springframework.beans.factory.annotation.Autowired;
//import org.springframework.beans.factory.annotation.Value;
//import org.springframework.scheduling.annotation.Scheduled;
//import org.springframework.stereotype.Component;
//
//import java.util.Calendar;
//import java.util.Date;
//import java.util.Set;
//import java.util.UUID;
//
//@Component
//public class AwayTask {
//
//    private Logger logger = LoggerFactory.getLogger(AwayTask.class);
//
//    @Value("${schedule.redis.key}")
//    private String key;
//
//    private String value = UUID.randomUUID().toString();
//
//    @Autowired
//    private RedisService redisService;
//
//    @Autowired
//    private AwayService awayService;
//
//    @Autowired
//    private DeviceAwayMapper awayMapper;
//
//    @Autowired
//    private HttpsAPIService httpsAPIService;
//
//    @Scheduled(cron = "0 0/1 * * * ?")
//    public void awayTask() {
//        logger.debug("away task is running. need redis value is {}", value);
//        if (!redisService.exists(key)) {
//            redisService.set(key, value, 300L);
//        }
//        if (value.equals(redisService.get(key))) {
//            Set<RedisDeviceAway> redisAways = null;
//            long curr = MyDateUtils.getMinutsByDate(MyDateUtils.getUtcDateTime()); // 获取当前UTC分钟数
//            long start = curr - CommonConstant.OFFSET_TIME;
//            logger.debug("current time is:{}, start time is:{}", curr, start);
//            if (redisService.exists(CommonConstant.TIMING_AWAY)) {
//                redisAways = (Set<RedisDeviceAway>) redisService.getZSetRangeByScore(CommonConstant.TIMING_AWAY, start, curr);
//            }
//            if (redisAways != null && !redisAways.isEmpty()) {
//                for (RedisDeviceAway redisAway : redisAways) {
//                    String[] repeats = redisAway.getRepeat().split(",");
//                    for (String repeat : repeats) {
//                        boolean startFlag = false; // 是否执行
//                        Date startTime = redisAway.getExecuteTime();
//                        long minutes = redisAway.getMinutes();
//                        if (!"7".equals(repeat)) { // 7不重复 0-6重复
//                            startTime = MyDateUtils.getUtcDateByUtcDateAndWeek(startTime, repeat);
//                            minutes = MyDateUtils.getMinutsByDate(startTime);
//                        }
//                        if (Math.abs(minutes - curr) <= CommonConstant.OFFSET_TIME) {
//                            // 执行开始定时
//                            executeAway(redisAway.getUuid(), redisAway.getOperator());
//                            startFlag = true;
//                        }
//                        // 不重复定时执行后，需要删除ZSET并在数据中停止此定时
//                        if ("7".equals(redisAway.getRepeat()) && startFlag) {
//                            redisService.removeZSetValue(CommonConstant.TIMING_AWAY, redisAway);
//                            if (CommonConstant.END.equals(redisAway.getType())) {
//                                DeviceAway deviceAway = awayMapper.selectByPrimaryKey(redisAway.getId());
//                                // 更新查询缓存
//                                String redisAwayKey = CommonConstant.TIMING_AWAY.concat("-").concat(deviceAway.getUuid());
//                                if (redisService.exists(redisAwayKey)) {
//                                    // 只有一个不需要对比
//                                    redisService.remove(redisAwayKey);
//                                }
//                                deviceAway.setStatus("0");
//                                redisService.set(redisAwayKey, deviceAway, CommonConstant.SECONDS_OF_ONEDAY);
//                                // 更新数据库状态为停止
//                                awayService.stopAwayByPrimaryKey(redisAway.getId());
//                            }
//                        }
//                    }
//                    // 重复任务，增加加一天
//                    if (!"7".equals(redisAway.getRepeat())) {
//                        redisService.removeZSetValue(CommonConstant.TIMING_AWAY, redisAway);
//                        Calendar calendar = Calendar.getInstance();
//                        calendar.setTime(redisAway.getExecuteTime());
//                        calendar.add(Calendar.DAY_OF_YEAR, 1);
//                        redisAway.setExecuteTime(calendar.getTime());
//                        redisAway.setMinutes(MyDateUtils.getMinutsByDate(redisAway.getExecuteTime()));
//                        redisService.addZSet(CommonConstant.TIMING_AWAY, MyDateUtils.getMinutsByDate(redisAway.getExecuteTime()), redisAway);
//                    }
//                }
//            }
//        }
//    }
//
//
//    private void executeAway(String uuid, String status) {
//        boolean operator = true;
//        if (CommonConstant.COMMON_STATUS_OFF.equals(status)) {
//            operator = false;
//        }
//        try {
//            JSONObject jsonObject = httpsAPIService.changeDeviceSwitchState(uuid, operator);
//            logger.info("invoke vdmp platform operator device result:{}", jsonObject.toJSONString());
//        } catch (Exception e) {
//            logger.error("invoke vdmp platform operator device error.", e);
//        }
//    }
//
//}
