package com.haoliang.common.utils;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.text.SimpleDateFormat;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.time.temporal.ChronoUnit;
import java.util.Calendar;
import java.util.Date;
import java.util.concurrent.TimeUnit;

public class DateUtil {

    public static final String SIMPLE_FORMAT = "yyyy-MM-dd";

    public static final String DETAIL_FORMAT_NO_UNIT = "yyyyMMddHHmmssSSS";


    /**
     * 把日期字符转换成时间戳
     */
    public static long parseDateStr(String dateStr) {
        SimpleDateFormat myFormat = new SimpleDateFormat("yyyy-MM");
        try {
            Date date = myFormat.parse(dateStr);
            return date.getTime();
        } catch (Exception e) {
            return 0L;
        }
    }

    /**
     * 把日期字符转换成时间戳
     */
    public static LocalDate parseDate(String dateStr) {
        return LocalDate.parse(dateStr, DateTimeFormatter.ofPattern(SIMPLE_FORMAT));
    }

    /**
     * 把日期字符转换成时间戳
     */
    public static LocalDate parseDate(String dateStr, String format) {
        return LocalDate.parse(dateStr, DateTimeFormatter.ofPattern(format));
    }


    /***
     * 生成详细日期，不要单位。格式YYMMDDhhmmss
     * @return
     */
    public static String getDetailTimeIgnoreUnit() {
        return LocalDateTime.now().format(DateTimeFormatter.ofPattern(DETAIL_FORMAT_NO_UNIT));
    }

    /**
     * 获取当前日期加一天的时间字符串
     */
    public static String getDateStrIncrement(String dateStr) {
        String result;
        try {
            SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd");
            Calendar cal = Calendar.getInstance();
            cal.setTime(sdf.parse(dateStr));//设置起时间
            //cal.add(Calendar.YEAR, 1);//增加一年
            cal.add(Calendar.DATE, 1);//增加一天
            //cal.add(Calendar.DATE, -10);//减10天
            //cal.add(Calendar.MONTH, n);//增加一个月
            result = sdf.format(cal.getTime());
            return result;
        } catch (Exception e) {
            e.printStackTrace();
            return null;
        }
    }


    /**
     * 获取当前日期增加指定时间的格式化字符
     *
     * @param date     原始时间
     * @param time     增加的数值
     * @param timeUnit 单位
     * @return
     */
    public static LocalDateTime getDateStrIncrement(LocalDateTime date, Integer time, TimeUnit timeUnit) {
        LocalDateTime newDate = LocalDateTime.now();
        switch (timeUnit) {
            case SECONDS:
                newDate = date.plusSeconds(time);
                break;
            case MINUTES:
                //增加分钟
                newDate = date.plusMinutes(time);
                break;
            case HOURS:
                //增加小时
                newDate = date.plusHours(time);
                break;
            case DAYS:
                //增加天
                newDate = date.plusDays(time);
                break;
        }
        return newDate;
    }


    /**
     * 检查日期是否失效了
     */
    public static boolean checkIsInvalid(Date dealine) {
        Date nowDate = new Date();
        if (dealine.getTime() < nowDate.getTime()) {
            return true;
        }
        return false;
    }

    /**
     * 获取今日的时间
     *
     * @return
     */
    public static LocalDate getNowDateNotHours() {
        return LocalDate.now();
    }

    public static void main(String[] args) {
        BigDecimal bigDecimal = new BigDecimal("1.1234561");
        bigDecimal = bigDecimal.setScale(6, RoundingMode.UP);
        System.out.println(bigDecimal);
    }


    /**
     * 计算两个日期之前相差的月数
     *
     * @param startLocalDate 开始日期
     * @param endLocalDate   结束日期
     * @return
     */
    public static int betweenMonths(LocalDate startLocalDate, LocalDate endLocalDate) {
        LocalDate start = LocalDate.of(startLocalDate.getYear(), startLocalDate.getMonth(), 1);
        LocalDate end = LocalDate.of(endLocalDate.getYear(), endLocalDate.getMonth(), 1);
        return (int) ChronoUnit.MONTHS.between(start, end);
    }

    public static String getMonthEnglish(Integer month) {
        switch (month) {
            case 1:
                return "Jan";
            case 2:
                return "Feb";
            case 3:
                return "Mar";
            case 4:
                return "Apr";
            case 5:
                return "May";
            case 6:
                return "Jun";
            case 7:
                return "Jul";
            case 8:
                return "Aug";
            case 9:
                return "Sept";
            case 10:
                return "Oct";
            case 11:
                return "Nov";
            case 12:
                return "Dec";
            default:
                return "";
        }
    }
}
