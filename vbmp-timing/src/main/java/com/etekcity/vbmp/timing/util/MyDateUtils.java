package com.etekcity.vbmp.timing.util;


import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.util.Assert;
import org.springframework.util.StringUtils;

import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.time.*;
import java.time.format.DateTimeFormatter;
import java.util.*;

public class MyDateUtils {
    private static final Logger logger = LoggerFactory.getLogger(MyDateUtils.class);

    public static final String TIME_HH_MM = "HH:mm";

    public static final String DATE_TIME = "yyyy-MM-dd HH:mm:ss";

    public static final String DATE8_FORMATE = "yyyyMMdd";

    public static final String UTC = "UTC";

    public static final Long SECONDS_OF_ONEDAY = 24L * 60L * 60L;

    /**
     * mongodb使用格式，请勿修改
     */
    public static final String DATE10_FORMATE = "yyyyMMddHH";

    public static int ONE_DAY_MILLISECONDS = 3600 * 24 * 1000;

    /**
     * 获取utc时间, 默认为今天，比当前时间小则为明天
     *
     * @param time     时间格式HH:ss
     * @param timeZone 时区
     * @return Date
     */
    public static Date getUtcDateByTimeAndZone(String time, String timeZone) {
        if (MyStringUtils.isNullData(time, timeZone)) {
            return null;
        }
        SimpleDateFormat sdf = new SimpleDateFormat(DATE_TIME);
        if (time.length() < 5) {
            time = "0" + time;
        }
        LocalTime localTime = LocalTime.parse(time);
        LocalDate localDate = LocalDate.now();
        OffsetDateTime odt = OffsetDateTime.now(ZoneId.of(timeZone)); // 输入的时区
        ZoneOffset zoneOffset = odt.getOffset();
        LocalDateTime ldt = LocalDateTime.of(localDate, localTime);
        Instant in = ldt.toInstant(zoneOffset);

        ZonedDateTime zonedDateTime = in.atZone(ZoneId.of(UTC)); // 转换后的时区，默认UTC
        String dateTime = zonedDateTime.format(DateTimeFormatter.ofPattern(DATE_TIME));

        Date utcDate = null;
        try {
            utcDate = sdf.parse(dateTime);
        } catch (ParseException e) {
            logger.error("把时区:{}的时间:{}转换为标准时间错误", timeZone, time);
        }
        return utcDate;
    }


    /**
     * 获取标准UTC日期
     *
     * @param date     日期
     * @param timeZone 时区
     * @return Date
     */
    public static Date getUtcDateByDateAndZone(Date date, String timeZone) {
        SimpleDateFormat sdf = new SimpleDateFormat(DATE_TIME);

        LocalDateTime localDateTime = LocalDateTime.parse(sdf.format(date), DateTimeFormatter.ofPattern(DATE_TIME));

        OffsetDateTime odt = OffsetDateTime.now(ZoneId.of(timeZone)); // 输入的时区
        Instant in1 = localDateTime.toInstant(odt.getOffset());
        ZonedDateTime zonedDateTime1 = in1.atZone(ZoneId.of(UTC));
        String dateTime = zonedDateTime1.format(DateTimeFormatter.ofPattern(DATE_TIME));

        Date utcDate = null;
        try {
            utcDate = sdf.parse(dateTime);
        } catch (ParseException e) {
            logger.error("把时区:{}的时间:{}转换为标准时间错误", timeZone, date);
        }

        return utcDate;
    }

    /**
     * 转换为对应周几的日期
     *
     * @param date 标准日期
     * @param week 星期几0123456->日一二三四五六
     * @return
     */
    public static Date getUtcDateByUtcDateAndWeek(Date date, String week) {
        if (date == null) {
            return null;
        }
        // 默认星期天
        int dayOfWeek = 7;
        if (!MyStringUtils.isNullData(week) && !"0".equals(week)) {
            dayOfWeek = Integer.parseInt(week);
        }
        Calendar c = Calendar.getInstance();
        c.setTime(date);
        if (!MyStringUtils.isNullData(week) && "0123456".contains(week)) {
            int day_of_week = c.get(Calendar.DAY_OF_WEEK) - 1;
            if (day_of_week == 0)
                day_of_week = 7;
            c.add(Calendar.DATE, dayOfWeek - day_of_week);
        }
        return c.getTime();
    }

    /**
     * 获取标准日期
     *
     * @param millis 毫秒数
     * @return Date
     */
    public static Date getUtcDateByMillis(String millis) {
        SimpleDateFormat sdf = new SimpleDateFormat(DATE_TIME);
        ZonedDateTime zonedDateTime = ZonedDateTime.now(ZoneId.of(UTC));
        long seconds = 0L;
        if (!MyStringUtils.isNullData(millis)) {
            seconds = Long.parseLong(millis);
        }
        zonedDateTime = zonedDateTime.plusSeconds(seconds);
        // 超过0秒向上加一分钟
        if (zonedDateTime.getSecond() > 0) {
            zonedDateTime = zonedDateTime.plusSeconds(60L - zonedDateTime.getSecond());
        }

        String dateTime = zonedDateTime.format(DateTimeFormatter.ofPattern(DATE_TIME));

        Date utcDate = null;
        try {
            utcDate = sdf.parse(dateTime);
        } catch (ParseException e) {
            logger.error("把毫秒数:{}转换为标准日期错误", millis);
        }
        return utcDate;
    }

    public static Date getUtcDateTime() {
        ZonedDateTime zonedDateTime = ZonedDateTime.now(ZoneId.of(UTC));
        String dateTime = zonedDateTime.format(DateTimeFormatter.ofPattern(DATE_TIME));
        SimpleDateFormat sdf = new SimpleDateFormat(DATE_TIME);
        Date utcDate = null;
        try {
            utcDate = sdf.parse(dateTime);
        } catch (ParseException e) {
            logger.error("获取UTC时间错误");
        }
        return utcDate;
    }

    /**
     * 根据重复获取要执行的日期
     *
     * @param utcDate 存在库里的执行日期
     * @param setDate 把任务状态更新为1的当前日期
     * @param week    1234567
     * @return Date
     */
    public static Date getUtcDateByUtcDateAndSetDateAndWeek(Date utcDate, Date setDate, String week) {
        if (utcDate == null || setDate == null) {
            return null;
        }
        int dayOfWeek = 7;
        if (!MyStringUtils.isNullData(week) && !"0".equals(week)) {
            dayOfWeek = Integer.parseInt(week);
        }
        Calendar c = Calendar.getInstance();
        c.setTime(setDate);
        if (!MyStringUtils.isNullData(week) && "0123456".contains(week)) {
            int day_of_week = c.get(Calendar.DAY_OF_WEEK) - 1;
            if (day_of_week == 0)
                day_of_week = 7;
            c.add(Calendar.DATE, dayOfWeek - day_of_week);
        }
        Calendar utc = Calendar.getInstance();
        utc.setTime(utcDate);
        c.set(Calendar.HOUR_OF_DAY, utc.get(Calendar.HOUR_OF_DAY));
        c.set(Calendar.MINUTE, utc.get(Calendar.MINUTE));
        c.set(Calendar.SECOND, utc.get(Calendar.SECOND));
        return c.getTime();
    }

    public static Date convertToDate(String dateTime) throws ParseException {
        if (MyStringUtils.isNullData(dateTime)) {
            return null;
        }
        SimpleDateFormat sdf = new SimpleDateFormat("HH:mm");
        Date date = sdf.parse(dateTime);
        return date;
    }

    public static String dateToString(Date date) {
        if (date == null) {
            return null;
        }
        SimpleDateFormat sdf = new SimpleDateFormat("HH:mm");
        String dateStr = sdf.format(date);
        return dateStr;
    }

    public static String utcDateToString(Date utcDate, String oldTmeZone, String newTimeZone, String partern) {
        if (utcDate == null || MyStringUtils.isNullData(oldTmeZone, newTimeZone, partern)) {
            return "";
        }
        Long targetTime = utcDate.getTime() - TimeZone.getTimeZone(oldTmeZone).getRawOffset()
                + TimeZone.getTimeZone(newTimeZone).getRawOffset();
        SimpleDateFormat localFormater = new SimpleDateFormat(partern);
        String localTime = localFormater.format(new Date(targetTime));

        return localTime;
    }

    public static long getTotalDays(String timeZone) {
        if (MyStringUtils.isNullData(timeZone)) {
            return 0L;
        }
        Calendar today = new GregorianCalendar();

        today.setTimeZone(TimeZone.getTimeZone(timeZone));

        return today.getTime().getTime() / (24 * 3600 * 1000);

    }

    /**
     * 验证两个时间是否相等
     *
     * @param date        时间
     * @param anotherDate 另一个时间
     * @return boolean 相等true、不相等false
     */
    public static boolean compareTimeLine(Date date, Date anotherDate) {
        if (Objects.isNull(date) || Objects.isNull(anotherDate)) {
            return false;
        }
        long time = date.getTime() / 1000L;
        long anotherTime = anotherDate.getTime() / 1000L;
        return time == anotherTime;
    }

    /**
     * 验证一个时间点是否在一个时间段里
     *
     * @param startTime   开始时间
     * @param endTime     结束时间
     * @param anotherTime 另一个时间点
     * @return boolean 在时间段里true、不在时间段false
     */
    public static boolean compareTimeLine(Date startTime, Date endTime, Date anotherTime) {
        if (Objects.isNull(startTime) || Objects.isNull(endTime) || Objects.isNull(anotherTime)) {
            return false;
        }
        long start = startTime.getTime() / 1000L;
        long end = endTime.getTime() / 1000L;
        if (end < start) {
            end += 24 * 60 * 60;
        }
        long another = anotherTime.getTime() / 1000L;
        // startTime <= anotherTime <= endTime
        if (start <= another && another <= end) {
            return true;
        }
        return false;
    }

    /**
     * 验证两个时间段是否有重叠
     *
     * @param startTime        开始时间
     * @param endTime          结束时间
     * @param anotherStartTime 另一个开始时间
     * @param anotherEndTime   另一个结束时间
     * @return boolean 重叠true、不重叠false
     */
    public static boolean compareTimeLine(Date startTime, Date endTime, Date anotherStartTime, Date anotherEndTime) {
        if (Objects.isNull(startTime) || Objects.isNull(endTime) || Objects.isNull(anotherStartTime) || Objects.isNull(anotherEndTime)) {
            return false;
        }
        long start = startTime.getTime() / 1000L;
        long end = endTime.getTime() / 1000L;
        long anotherStart = anotherStartTime.getTime() / 1000L;
        long anotherEnd = anotherEndTime.getTime() / 1000L;
        if (end < start) {
            end += 24 * 60 * 60;
        }
        if (anotherEnd < anotherStart) {
            anotherEnd += 24 * 60 * 60;
        }
        // 另一个开始、结束时间都小于开始时间 或者 另一个开始结束时间都大于结束时间 则不重叠
        if ((start < anotherStart && end < anotherStart) || (start > anotherEnd) && end > anotherEnd) {
            return false;
        }
        return true;
    }

    /**
     * 根据执行时间和设置计算是否tomorrow
     *
     * @param executeTime 执行时间(UTC)
     * @param setDate     设置时间(UTC)
     * @return
     */
    public static boolean calcTomorrow(Date executeTime, Date setDate) {
        boolean flag = false;
        if (executeTime == null || setDate == null) {
            return false;
        }
        Calendar now = Calendar.getInstance();
        Date nowUtc = getUtcDateByMillis("0");
        now.setTime(nowUtc);
        int nowDay = now.get(Calendar.DAY_OF_YEAR);

        // Date setDate = getUtcDateByDateAndZone(setTime, ZoneId.systemDefault().toString());
        Calendar set = Calendar.getInstance();
        set.setTime(setDate);
        int setDay = set.get(Calendar.DAY_OF_YEAR);
        int setHour = set.get(Calendar.HOUR_OF_DAY);
        int setMin = set.get(Calendar.MINUTE);

        Calendar execute = Calendar.getInstance();
        execute.setTime(executeTime);
        int executeDay = execute.get(Calendar.DAY_OF_YEAR);
        int executeHour = execute.get(Calendar.HOUR_OF_DAY);
        int executeMin = execute.get(Calendar.MINUTE);

        /*if(nowDay > setDay) {
            return false;
        } else {
            if(executeHour > setHour) {
                return false;
            } else if (executeHour < setHour) {
                return true;
            } else {
                if(executeMin < setMin) {
                    return true;
                } else {
                    return false;
                }
            }
        }*/
        if (nowDay <= setDay) {
            if (executeDay < setDay) {
                flag = true;
            } else if (executeHour < setHour) {
                flag = true;
            } else if (executeHour == setHour) {
                if (executeMin <= setMin) {
                    flag = true;
                }
            }
        }
        return flag;
    }

    public static Date addDate(Date date, int field, int value) {
        if (date == null) {
            return null;
        }
        Calendar calendar = Calendar.getInstance();
        calendar.setTime(date);
        calendar.add(field, value);
        return calendar.getTime();
    }

    public static Date getDateWithNoTime(Date date) {
        if (date == null) {
            return null;
        }
        Calendar calendar = Calendar.getInstance();
        calendar.setTime(date);
        calendar.set(Calendar.HOUR_OF_DAY, 0);
        calendar.set(Calendar.MINUTE, 0);
        calendar.set(Calendar.SECOND, 0);
        return calendar.getTime();
    }

    public static Date getDateWithNoMMSS(Date date) {
        if (date == null) {
            return null;
        }
        Calendar calendar = Calendar.getInstance();
        calendar.setTime(date);
        calendar.set(Calendar.MINUTE, 0);
        calendar.set(Calendar.SECOND, 0);
        return calendar.getTime();
    }

    public static Date getDateMonthBegin(Date date) {
        if (date == null) {
            return null;
        }
        Calendar calendar = Calendar.getInstance();
        calendar.setTime(date);
        calendar.set(Calendar.DAY_OF_MONTH, 1);
        return calendar.getTime();
    }


    public static Date setDate(Date date, int field, int value) {
        if (date == null) {
            return null;
        }
        Calendar calendar = Calendar.getInstance();
        calendar.setTime(date);
        calendar.set(field, value);
        return calendar.getTime();
    }

    public static String getDateStr(Date date, String dateFormate) {
        if (date == null || MyStringUtils.isNullData(dateFormate)) {
            return null;
        }
        SimpleDateFormat simpleDateFormat = new SimpleDateFormat(dateFormate);
        return simpleDateFormat.format(date);
    }

    public static Date convertStrToDate(String dateTime, String formateDate) {
        Assert.hasText(dateTime, "convertStrToDate方法参数dateTime为空");
        Assert.hasText(formateDate, "convertStrToDate方法参数formateDate为空");
        SimpleDateFormat sdf = new SimpleDateFormat(formateDate);
        Date date = null;
        try {
            date = sdf.parse(dateTime);
        } catch (ParseException e) {
            e.printStackTrace();
        }
        return date;
    }

    public static Date convertStrToDate(String dateTime, String formateDate, String timeZone) {
        Assert.hasText(dateTime, "convertStrToDate方法参数dateTime为空");
        Assert.hasText(formateDate, "convertStrToDate方法参数formateDate为空");
        SimpleDateFormat sdf = new SimpleDateFormat(formateDate);
        sdf.setTimeZone(TimeZone.getTimeZone(timeZone));
        Date date = null;
        try {
            date = sdf.parse(dateTime);
        } catch (ParseException e) {
            e.printStackTrace();
        }
        return date;
    }


    public static long getDateDaysGap(String startTime, String formateDateStart, String endTime, String formateDateEnd) {
        Assert.hasText(startTime, "getDateDaysGap方法参数startTime为空");
        Assert.hasText(formateDateStart, "getDateDaysGap方法参数formateDateStart为空");
        Assert.hasText(endTime, "getDateDaysGap方法参数endTime为空");
        Assert.hasText(formateDateEnd, "getDateDaysGap方法参数formateDateEnd为空");

        SimpleDateFormat sdf = new SimpleDateFormat(formateDateStart);
        Date dateStart = null;
        try {
            dateStart = sdf.parse(startTime);
        } catch (ParseException e) {
            logger.warn("时间转换失败", e);
        }
        SimpleDateFormat sdf2 = new SimpleDateFormat(formateDateStart);
        Date dateEnd = null;
        try {
            dateEnd = sdf2.parse(endTime);
        } catch (ParseException e) {
            logger.warn("时间转换失败", e);
        }

        return (dateEnd.getTime() - dateStart.getTime()) / ONE_DAY_MILLISECONDS;
    }

    public static String getRepeatByteString(String repeat) {
        if (MyStringUtils.isNullData(repeat)) {
            return "0";
        }
        String str = "00000000";
        char[] chars = str.toCharArray();
        String[] repeats = repeat.split(",");
        for (String r : repeats) {
            if ("0".equals(r)) {
                chars[0] = '1';
            } else {
                int index = Integer.valueOf(r);
                chars[7 - index] = '1';
            }
        }
        return new String(chars);
    }

    public static long getMinutsByDate(Date date) {
        if (date == null) {
            return 0L;
        }
        return date.getTime() / 1000L / 60L;
    }

    public static Long getRandomTime(long baseTime, int floating) {
        Random random = new Random();
        long returnValue = Long.valueOf(random.nextInt(floating)) + baseTime;
        return returnValue;
    }

    public static Long getCommonRedisExpireTime(int floating) {
        Random random = new Random();
        long returnValue = Long.valueOf(random.nextInt(floating)) + SECONDS_OF_ONEDAY;
        return returnValue;
    }

    public static void main(String[] args) throws ParseException {
        ZoneId defaultZone = ZoneId.systemDefault();
        System.out.println(defaultZone.getId());


    }

    public static String getTimeStringByZone(String time, String oldTimeZone, String newTimeZone) {
        if (MyStringUtils.isNullData(oldTimeZone, newTimeZone, time)) {
            return "";
        }

        LocalTime lt = LocalTime.parse(time);
        LocalDate ld = LocalDate.now();
        if (lt.isBefore(LocalTime.now())) {
            ld = ld.plusDays(1);
        }
        OffsetDateTime odt = OffsetDateTime.now(ZoneId.of(oldTimeZone)); // 输入的时区
        ZoneOffset zoneOffset = odt.getOffset();
        LocalDateTime ldt = LocalDateTime.of(ld, lt);
        Instant in = ldt.toInstant(zoneOffset);

        ZonedDateTime zonedDateTime = in.atZone(ZoneId.of(newTimeZone)); // 转换后的时区
        String dateTime = zonedDateTime.format(DateTimeFormatter.ofPattern(TIME_HH_MM));

        return dateTime;
    }

    public static String getTimeStringByZone(Date date, String oldTimeZone, String newTimeZone) {
        if (date == null || MyStringUtils.isNullData(oldTimeZone, newTimeZone)) {
            return "";
        }
        SimpleDateFormat sdf = new SimpleDateFormat(TIME_HH_MM);
        String time = sdf.format(date);
        return getTimeStringByZone(time, oldTimeZone, newTimeZone);
    }

    public static boolean compareTime(String starttime, String endtime) {
        if (starttime.length() < 5) {
            starttime = "0" + starttime;
        }
        if (endtime.length() < 5) {
            endtime = "0" + endtime;
        }
        LocalTime start = LocalTime.parse(starttime);
        LocalTime end = LocalTime.parse(endtime);
        return end.isBefore(start);
    }

    public static boolean compareTime(Date startDate, Date setDate) {
        if (startDate == null || setDate == null) {
            return false;
        }
        SimpleDateFormat sdf = new SimpleDateFormat(TIME_HH_MM);
        String start = sdf.format(startDate);
        LocalTime startTime = LocalTime.parse(start);

        String set = sdf.format(setDate);
        LocalTime setTime = LocalTime.parse(set);

        return startTime.isBefore(setTime);
    }

    public static Date getDateYearBegin(Date date) {
        if (date == null) {
            return null;
        }
        Calendar calendar = Calendar.getInstance();
        calendar.setTime(getDateWithNoTime(date));
        calendar.set(Calendar.MONTH, 0);
        calendar.set(Calendar.DATE, 1);
        return calendar.getTime();
    }

    public static long getUTCDateGap(Date dateTime, String timeZone) {
        if (dateTime == null || !StringUtils.hasText(timeZone)) {
            return 0;
        }
        //获取当地时间
        Calendar calendar = Calendar.getInstance();
        calendar.setTimeZone(TimeZone.getTimeZone(timeZone));
        calendar.setTime(dateTime);

        /** 取得时间偏移量 */
        int zoneOffset = calendar.get(Calendar.ZONE_OFFSET);
        /** 取得夏令时差 */
        int dstOffset = calendar.get(Calendar.DST_OFFSET);
        System.out.println(-(zoneOffset + dstOffset) / 1000 / 3600);
        return -(zoneOffset + dstOffset);
    }

    public static Integer getDateProperty(String dateStr, String formate, int calanderProperty) {
        if (!StringUtils.hasText(formate) || !StringUtils.hasText(dateStr)) {
            return null;
        }

        Date date = convertStrToDate(dateStr, formate);
        //获取当地时间
        Calendar calendar = Calendar.getInstance();
        calendar.setTime(date);
        return calendar.get(calanderProperty);
    }

    public static Date convertUTCDateToLocal(Date dateTime, String timeZone) {
        if (dateTime == null || !StringUtils.hasText(timeZone)) {
            return null;
        }
        //获取当地时间
        Calendar calendar = Calendar.getInstance();
        calendar.setTimeZone(TimeZone.getTimeZone(timeZone));
        calendar.setTime(dateTime);

        /** 取得时间偏移量 */
        int zoneOffset = calendar.get(Calendar.ZONE_OFFSET);
        /** 取得夏令时差 */
        int dstOffset = calendar.get(Calendar.DST_OFFSET);
        System.out.println(-(zoneOffset + dstOffset) / 1000 / 3600);
        /** 从本地时间里加上这些差量，即可以取得当地时间 */
        calendar.add(Calendar.MILLISECOND, (zoneOffset + dstOffset));
        return calendar.getTime();
    }

    public static long compareMinute(String startTime, String endTime) {
        LocalTime startLocalTime = LocalTime.parse(startTime);
        LocalTime endLocalTime = LocalTime.parse(endTime);

        long startMinite = startLocalTime.getHour() * 60L + startLocalTime.getMinute();
        long endMinute = endLocalTime.getHour() * 60L + endLocalTime.getMinute();

        if (endMinute < startMinite) {
            endMinute += 24 * 60;
        }

        return endMinute - startMinite;
    }

    /**
     * 根据时间和时区计算是加一天还是减一天
     *
     * @param startTime 开始时间
     * @param timeZone  时区
     * @return
     */
    public static int getPlusDay(String startTime, String timeZone) {
        if (MyStringUtils.isNullData(startTime, timeZone)) {
            return 0;
        }
        // 获取对应时区的偏移量，单位分
        int offset = TimeZone.getTimeZone(timeZone).getOffset(System.currentTimeMillis()) / 1000 / 60;
        // 转换为utcTime
        Date date = MyDateUtils.getUtcDateByTimeAndZone(startTime, timeZone);

        String[] time = startTime.split(":");
        int oldMinutes = Integer.parseInt(time[0]) * 60 + Integer.parseInt(time[1]);

        SimpleDateFormat sdf = new SimpleDateFormat(TIME_HH_MM);
        String[] utcTime = sdf.format(date).split(":");
        int utcMinutes = Integer.parseInt(utcTime[0]) * 60 + Integer.parseInt(utcTime[1]);

        int result = oldMinutes - utcMinutes;

        if (result > offset) {
            return 1;
        } else if (result < offset) {
            return -1;
        } else {
            return 0;
        }
    }
}
