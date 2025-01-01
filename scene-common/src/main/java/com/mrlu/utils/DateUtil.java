
package com.mrlu.utils;

import org.apache.commons.lang.StringUtils;

import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.Calendar;
import java.util.Date;
import java.util.GregorianCalendar;
import java.util.TimeZone;

/**
 * <p>Description: </p>
 * 日期工具类
 *
 * @author Wenas
 * @version 1.0.0
 * @date 2019/12/4 0004 15:39
 **/
public class DateUtil {


	public static final String YEAR_CN = "年";
	public static final String MONTH_CN = "月";
	public static final String DAY_CN = "日";
	public static final String HOUR_CN = "时";
	public static final String MINUTE_CN = "分";
	public static final String SECOND_CN = "秒";

	public static String DATETIME_PATTERN = "yyyyMMddHHmmss";
	public static String DATETIME_PATTERN_DATE = "yyyy-MM-dd";
	public static String DATETIME_PATTERN_YEAR = "yyyy";
	public static String DATETIME_PATTERN_DATE_YYYYMM = "yyyyMM";
	public static String DATETIME_PATTERN_DATE_YYYY_MM = "yyyy-MM";
	public static String DATETIME_PATTERN_YYYYMMDDHHMM00 = "yyyy-MM-dd HH:mm:00";
	public static String DATETIME_PATTERN_DATE_MM = "mm";
	public static String DATETIME_PATTERN_DATE_TIME = "yyyy-MM-dd HH:mm:ss";
	public static String DATETIME_PATTERN_DATE_INTEGRAL_POINT = "yyyy-MM-dd HH:00:00";

	public static String DATETIME_PATTERN_DATE_INTEGRAL_DAY = "yyyy-MM-dd 00:00:00";
	public static String DATETIME_PATTERN_YYYYMMDDHHMMSSSSS = "yyyyMMddHHmmssSSS";
	public static String DATETIME_PATTERN_YYYYMMDDHHMM = "yyyyMMddHHmm";
	public static String DATETIME_PATTERN_YYYYMMDDHH0000 = "yyyyMMddHH0000";
	public static String DATETIME_PATTERN_YYYYMMDD = "yyyyMMdd";
	private static SimpleDateFormat sdf14_sec = new SimpleDateFormat("yyyyMMddHHmmss");

	public static final String ESDB_DATE_STR_PATTERN = "yyyy-MM-dd'T'HH:mm:ss.SSS'Z'";

	public static final String ESDB_DATE_STR_PATTERN_2 = "yyyy-MM-dd'T'HH:mm:ss.SSS+08:00";
	public static String DATETIME_PATTERN_WITHOUT_SPACE = "yyyy-MM-dd+HH:mm:ss";

	public static String formatUtil(Date d, String format) {
		SimpleDateFormat dateFormat = new SimpleDateFormat(format);
		return dateFormat.format(d);
	}

	public static String date2Str14(Date date) {
		synchronized (sdf14_sec) {
			return sdf14_sec.format(date);
		}
	}

	public static String getUTCDate(String beijinDateStr, String informat, String outformat) {
		Date beijinDate = new Date();
		try {
			beijinDate = DateUtil.parse(beijinDateStr, informat);
		} catch (ParseException e) {
			e.printStackTrace();
		}
		Date utcDate = new Date(beijinDate.getTime() - 8 * 60 * 60 * 1000);
		return DateUtil.format(utcDate, outformat);
	}

	public static Date beforeByMouth(int month) {
		Calendar c = Calendar.getInstance();
		c.add(Calendar.MONTH, -month);
		return c.getTime();
	}

	/**
	 *  把传入的一个时间字符串转为世界时或北京时
	 *  @param time 如：2016-01-22 12:00:00
	 *  @param hour  相差的小时数
	 */
	public static String changeTime(String time, int hour) {
		SimpleDateFormat sdf = new SimpleDateFormat(DATETIME_PATTERN_DATE_TIME);
		Calendar calendar = Calendar.getInstance();
		Date date = null;
		try {
			date = sdf.parse(time);
		} catch (ParseException e) {
			e.printStackTrace();
		}
		calendar.setTime(date);
		calendar.add(Calendar.HOUR_OF_DAY, 0 - hour);
		date = calendar.getTime();
		String requestTime = sdf.format(date);
		return requestTime;
	}

	/**
	 * 把format1转为format2的形式
	 * 如：20160504140000 转为 2016-05-04 14:00:00
	 *
	 * @param requestTime           时间点字符串
	 * @param nowTimeFormats        传入当前时间类型
	 * @param disposeTimeFormats    处理后的时间类型
	 * @return String
	 */
	public static String formatRequestTime(String requestTime, String nowTimeFormats, String disposeTimeFormats) {
		SimpleDateFormat sdf1 = new SimpleDateFormat(nowTimeFormats);
		Date date;
		String timeStr = "";
		try {
			date = (Date) sdf1.parse(requestTime);
			SimpleDateFormat sdf2 = new SimpleDateFormat(disposeTimeFormats);
			timeStr = sdf2.format(date);
		} catch (ParseException e) {
			e.printStackTrace();
		}
		return timeStr;
	}

	/**
	 * 获取当前时间减去x个月
	 *
	 * @param date
	 * @param amount
	 * @return
	 */
	public static Date getAfterTimeByMonth(Date date, int amount) {
		Calendar c = Calendar.getInstance();
		c.setTime(date);
		c.add(Calendar.MONTH, amount);
		return c.getTime();
	}

	/**
	 * 取时间差值 精确到秒
	 * @param start
	 * @param end
	 * @return
	 */
	public static Long diffS(Date start,Date end){
		try {
			long diff = end.getTime() - start.getTime();
			return (diff / 1000);
		} catch (Exception e) {
			e.printStackTrace();
		}
		return 0L;
	}

	public static Date parse(String strDate, String pattern) throws ParseException {
		return StringUtils.isBlank(strDate) ? null : new SimpleDateFormat(pattern).parse(strDate);
	}

	public static String getBeijinDate(String utcDateStr) {
		Date utcDate = new Date();
		try {
			utcDate = DateUtil.parse(utcDateStr, "yyyy-MM-dd HH:mm:ss.SS");
		} catch (ParseException e) {
			e.printStackTrace();
		}
		Date beijinDate = new Date(utcDate.getTime() + 8 * 60 * 60 * 1000);
		return DateUtil.format(beijinDate, "yyyy-MM-dd HH:mm:ss.SS");
	}

	/**
	 * 获取最靠近6分钟倍数的日期 国际时间
	 */
	public static Date getDateSix(Date date) {
		Date utcDate = new Date(date.getTime() - 8 * 60 * 60 * 1000);
		String datestr = format(utcDate, "yyyy-MM-dd HH:mm");
		String datestrBegin = datestr.substring(0, datestr.length() - 2);
		String mini = datestr.substring(datestr.length() - 2, datestr.length());
		String newmini = (((int) ((Integer.valueOf(mini)) / 6)) * 6) + "";
		String datenewstr = datestrBegin + newmini + ":00";
		Date datenew = formatDate(datenewstr);
		return datenew;
	}

	/**
	 * 时间格式化为分钟2或者32
	 */
	@SuppressWarnings("deprecation")
	public static Date getDateRain(Date date) {
		int hourse = date.getHours();
		if (hourse > 20) {
			hourse = 12;
		} else if (hourse > 8) {
			hourse = 00;
		} else {
			date = getDateBeforeHourTime(date, 24);
			hourse = 12;
		}
		String datestr = format(date, "yyyy-MM-dd HH");
		String datestrBegin = datestr.substring(0, datestr.length() - 2);
		String datenewstr = datestrBegin + hourse + ":00:00";
		Date datenew = formatDate(datenewstr);
		return datenew;
	}

	/**
	 * 获取当前时间减xx分钟 ，返回日期date
	 *
	 * @return
	 */
	public static Date getBeforeMiniTime(Date date, int mini) {
		Calendar calendar = Calendar.getInstance();
		calendar.setTime(date);
		calendar.add(Calendar.MINUTE, 0 - mini);
		return calendar.getTime();
	}

	/**
	 * 获取当前时间减xx分钟
	 * @return
	 */
	public static String getBeforeMiniTime(String time, int mini) {
		SimpleDateFormat formatter = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");
		try {
			Date date = formatter.parse(time);
			Calendar calendar = Calendar.getInstance();
			calendar.setTime(date);
			calendar.add(Calendar.MINUTE, 0 - mini);
			return formatter.format(calendar.getTime());
		} catch (ParseException e) {
			e.printStackTrace();
		}
		return null;
	}

	/**
	 * 获取当前时间减xx分钟 ，返回日期date
	 *
	 * @return
	 */
	public static Date getAfterMiniTime(Date date, int mini) {
		Calendar calendar = Calendar.getInstance();
		calendar.setTime(date);
		calendar.add(Calendar.MINUTE, mini);
		return calendar.getTime();
	}

	public static Date getBeijingDate(Date utcDate) {
		Date beijinDate = new Date(utcDate.getTime() + 8 * 60 * 60 * 1000);
		return beijinDate;
	}

	/**
	 * 获取当前时间减xx小时,返回日期 date
	 *
	 * @return
	 */
	public static Date getDateBeforeHourTime(Date date, int hour) {
		Calendar calendar = Calendar.getInstance();
		calendar.setTime(date);
		calendar.add(Calendar.HOUR_OF_DAY, 0 - hour);
		return calendar.getTime();
	}

	/**
	 * 获取当前时间加上xx小时，返回日期date
	 * @param date
	 * @param hour
	 * @return
	 */
	public static Date getDateAfterHourTime(Date date, int hour) {
		Calendar calendar = Calendar.getInstance();
		calendar.setTime(date);
		calendar.add(Calendar.HOUR_OF_DAY, hour);
		return calendar.getTime();
	}

	/**
	 * 获取当前时间减xx小时,返回日期 date
	 * @return
	 */
	public static String getDateBeforeHourTime(String time, int hour) {
		SimpleDateFormat formatter = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");
		try {
			Date date = formatter.parse(time);
			Calendar calendar = Calendar.getInstance();
			calendar.setTime(date);
			calendar.add(Calendar.HOUR_OF_DAY, 0 - hour);
			return formatter.format(calendar.getTime());
		} catch (ParseException e) {
			e.printStackTrace();
		}
		return null;
	}

	/**
	 * 时间格式化为分钟2或者32
	 */
	@SuppressWarnings("deprecation")
	public static Date getDateGpf(Date date) {
		Date utcDate = new Date(date.getTime() - 8 * 60 * 60 * 1000);
		int minic = utcDate.getMinutes();
		if (minic < 32) {
			minic = 2;
		} else {
			minic = 32;
		}
		String datestr = format(utcDate, "yyyy-MM-dd HH:mm");
		String datestrBegin = datestr.substring(0, datestr.length() - 2);
		String datenewstr = datestrBegin + minic + ":00";
		Date datenew = formatDate(datenewstr);
		return datenew;
	}

	/**
	 * 格式化日期输出
	 *
	 * @param d
	 * @return
	 */
	public static Date formatDate(String d) {
		try {
			return new SimpleDateFormat("yyyy-MM-dd HH:mm:ss").parse(d);
		} catch (ParseException e) {
			e.printStackTrace();
		}
		return null;
	}

	public static String getBeijinDate(String utcDateStr, String inFormat, String outFormat) {
		Date utcDate = new Date();
		try {
			utcDate = DateUtil.parse(utcDateStr, inFormat);
		} catch (ParseException e) {
			e.printStackTrace();
		}
		Date beijinDate = new Date(utcDate.getTime() + 8 * 60 * 60 * 1000);
		return DateUtil.format(beijinDate, outFormat);
	}

	public static Date hoursTimeDifference(int hour) {
		Date data = new Date();
		Date reDate = new Date(data.getTime() - (hour * 60 * 60 * 1000));
		return reDate;
	}

	public static Date hoursTimeDifference(Date date, int hour) {
		Date reDate = new Date(date.getTime() - (hour * 60 * 60 * 1000));
		return reDate;
	}

	/**
	 * 获取当前日期,精确小时
	 */
	public static String getCurrentHourTime(Date date) {
		return DateUtil.format(date, "yyyyMMddHH") + "0000";
	}

	/**
	 * 获取当前日期,精确分钟
	 */
	public static String getCurrentMinuteTime(Date date) {
		return DateUtil.format(date, "yyyyMMddHHmm") + "00";
	}

	/**
	 * 获取当前日期,精确半小时
	 *
	 * @return
	 */
	public static String getCurrentHalfHourTime() {
		Calendar calendar = Calendar.getInstance();
		int m = calendar.get(Calendar.MINUTE);
		String minute = m < 30 ? "00" : "30";
		return DateUtil.format(new Date(), "yyyyMMddHH") + "" + minute + "00";
	}

	/**
	 * 获取当前日期,精确小时
	 *
	 * @return
	 */
	public static String getCurrentHourTime() {
		return DateUtil.format(new Date(), "yyyyMMddHH") + "0000";
	}

	/**
	 * 获取当前日期 年月日
	 *
	 * @return
	 */
	public static String getWholeTimeYMD() {
		return DateUtil.format(new Date(), "yyyyMMddHH");
	}

	public static String format(Date date, String pattern) {
		return date == null ? "" : new SimpleDateFormat(pattern).format(date);
	}

	/**
	 * 年-月-日 时:分:秒
	 *
	 * @param d <CODE>Date</CODE>
	 * @return String
	 */
	public static String detailFormat(Date d) {
		return formatUtil(d, DATETIME_PATTERN_DATE_TIME);
	}

	/**
	 * 年-月-日
	 *
	 * @param d <CODE>Date</CODE>
	 * @return String
	 */
	public static String simpleFormat(Date d) {
		return formatUtil(d, "yyyy-MM-dd");
	}

	/**
	 * 年月日时分秒
	 *
	 * @param d <CODE>Date</CODE>
	 * @return String
	 */
	public static String dataFormat(Date d) {
		return formatUtil(d, "yyyyMMddHHmmss");
	}

	/**
	 * 年月日时分
	 *
	 * @param d <CODE>Date</CODE>
	 * @return String
	 */
	public static String dataFormatNoS(Date d) {
		return formatUtil(d, "yyyyMMddHHmm");
	}

	/**
	 * 比较时间
	 *
	 * @param a <CODE>String</CODE>
	 * @param b <CODE>String</CODE>
	 * @return int
	 */
	public static int compare(String a, String b) {
		SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");
		Date ad = new Date();
		Date bd = new Date();

		try {
			ad = sdf.parse(a);
			bd = sdf.parse(b);
		} catch (Exception e) {
			e.printStackTrace();
		}

		long t = ad.getTime() - bd.getTime();
		if (t > 0) {
			return 1;
		} else if (t < 0) {
			return -1;
		} else {
			return 0;
		}
	}

	/**
	 * 时间加上多少分钟
	 *
	 * @param time   如201309011200
	 * @param minute
	 * @return
	 */
	public static long timeAddMinute(long time, int minute) {
		Calendar cal = Calendar.getInstance();
		SimpleDateFormat dateFormat = new SimpleDateFormat("yyyyMMddHHmm");
		try {
			cal.setTime(dateFormat.parse(time + ""));
		} catch (ParseException e) {
			e.printStackTrace();
		}
		cal.add(Calendar.MINUTE, minute);
		return timeToLong(cal.getTime(), "yyyyMMddHHmm");

	}

	/**
	 * 时间加上多少分钟
	 *
	 * @param time   如201309011200
	 * @param minute
	 * @return
	 */
	public static Date timeAddMinute(Date time, int minute) {
		Calendar cal = Calendar.getInstance();
		cal.setTime(time);
		cal.add(Calendar.MINUTE, minute);
		return cal.getTime();
	}

	/**
	 * 延迟多少分钟时间,并转换成对应的格式
	 *
	 * @param time   时间节点,格式为 yyyy-MM-dd hh:mm:ss
	 * @param minute 延迟多少分钟
	 * @return String
	 */
	public static String delayFiveMinute(String time, int minute) {
		Date date = StringFormatDate1(time);
		return detailFormat(timeAddMinute(date, minute));
	}

	/**
	 * 返回时间格式long类型数据(如201309011200)
	 *
	 * @param date
	 * @param format
	 * @return
	 */
	public static long timeToLong(Date date, String format) {
		SimpleDateFormat dateFormat = new SimpleDateFormat(format);
		String retTime = dateFormat.format(date);
		return Long.valueOf(retTime);
	}

	/**
	 * 时间加上多少小时
	 *
	 * @param date
	 * @param hour
	 * @return
	 */
	public static Date timeAddHour(Date date, int hour) {
		Calendar cal = Calendar.getInstance();
		cal.setTime(date);
		cal.add(Calendar.HOUR, hour);
		return cal.getTime();
	}

	/**
	 * 获取当前时间整5分钟数据
	 *
	 * @param date 时间节点
	 * @return Date
	 */
	@SuppressWarnings("deprecation")
	public static Date time5min(Date date) {
		// 获取当前日期整5分钟日期
		Calendar cal = Calendar.getInstance();
		cal.setTime(date);
		int i = date.getMinutes();
		if (i % 5 != 0) {
			for (int j = 0; j < 4; j++) {
				cal.add(Calendar.MINUTE, -1);
				if (cal.getTime().getMinutes() % 5 == 0) {
					date = cal.getTime();
					break;
				}
			}
		}
		return date;
	}

	public static long dateFormat(String dateTime) {
		SimpleDateFormat smdf = new SimpleDateFormat(DATETIME_PATTERN);
		SimpleDateFormat smdf1 = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");
		Date d = null;
		String returnDate = null;
		try {
			d = smdf1.parse(dateTime);
			returnDate = smdf.format(d);
		} catch (ParseException e) {
			return 0;
		}
		return Long.parseLong(returnDate);
	}

	public static boolean isValidDate(String str) {
		if (str == null || str.length() == 0 || str.length() != 14) {
			return false;
		}
		SimpleDateFormat dateFormat = new SimpleDateFormat("yyyyMMddHHmmss");
		try {
			dateFormat.parse(str);
			return true;
		} catch (Exception e) {
			// 如果throw java.text.ParseException或者NullPointerException，就说明格式不对
			return false;
		}
	}

	public static String dateFormatString1(Date d) {
		SimpleDateFormat smdf1 = new SimpleDateFormat(DATETIME_PATTERN_DATE);
		return smdf1.format(d);
	}

	public static String dataFormatString2(Date d) {
		SimpleDateFormat smdf = new SimpleDateFormat("yyyyMMddHH");
		return smdf.format(d) + "0000";
	}

	public static String dataFormatYMdH(Date d) {
		SimpleDateFormat smdf = new SimpleDateFormat("yyyyMMddHH");
		return smdf.format(d);
	}

	public static String dateFormatmm(Date d) {
		SimpleDateFormat smdf1 = new SimpleDateFormat(DATETIME_PATTERN_DATE_MM);
		return smdf1.format(d);
	}

	public static Date StringFormatDate1(String dateTime) {
		SimpleDateFormat smdf = new SimpleDateFormat(DATETIME_PATTERN_DATE_TIME);
		Date date = null;
		try {
			date = smdf.parse(dateTime);
		} catch (ParseException e) {
			e.printStackTrace();
		}
		return date;
	}

	public static Date StringFormatDate2(String dateTime) {
		SimpleDateFormat smdf = new SimpleDateFormat("yyyy-MM-dd");
		Date date = null;
		try {
			date = smdf.parse(dateTime);
		} catch (ParseException e) {
			e.printStackTrace();
		}
		return date;
	}

	public static Date StringFormatDate3(String dateTime) {
		SimpleDateFormat smdf = new SimpleDateFormat("yyyy-MM-dd HH:mm");
		Date date = null;
		try {
			date = smdf.parse(dateTime);
		} catch (ParseException e) {
			e.printStackTrace();
		}
		return date;
	}

	public static Date StringFormatDate4(String dateTime) {
		SimpleDateFormat smdf = new SimpleDateFormat("yyyyMMddHHmmss");
		Date date = null;
		try {
			date = smdf.parse(dateTime);
		} catch (ParseException e) {
			e.printStackTrace();
		}
		return date;
	}

	public static Date StringFormatDate5(String dateTime) {
		SimpleDateFormat smdf = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");
		Date date = null;
		try {
			date = smdf.parse(dateTime);
		} catch (ParseException e) {
			e.printStackTrace();
		}
		return date;
	}

	public static Date StringFormatDate(String dateTime, String format) {
		SimpleDateFormat smdf = new SimpleDateFormat(format);
		Date date = null;
		try {
			date = smdf.parse(dateTime);
		} catch (ParseException e) {
			e.printStackTrace();
		}
		return date;
	}

	public static boolean checkToday(Date date) {
		Date d = new Date();
		String dStr = DateUtil.dateFormatString1(d);
		String dateStr = DateUtil.dateFormatString1(date);
		if (dStr.equals(dateStr))
			return true;
		return false;
	}

	public static String getCurDateTime() {
		String curTime = dateToString(new Date(), "yyyyMMddHHmmss");
		Calendar cal = Calendar.getInstance();

		int iHour = cal.get(Calendar.HOUR_OF_DAY);
		String hour = "" + iHour;
		while (hour.length() < 2) {
			hour = "0" + hour;
		}

		curTime = curTime.substring(0, 8) + hour + curTime.substring(10);
		return curTime;
	}

	public static String dateToString(Date date, String pattern) {
		if (date == null) {
			return null;
		}

		try {
			SimpleDateFormat sfDate = new SimpleDateFormat(pattern);
			sfDate.setLenient(false);

			return sfDate.format(date);
		} catch (Exception e) {

			return null;
		}
	}

	/**
	 * 某个日期增加天数获取结果日期
	 *
	 * @param date 日期
	 * @param days 天数
	 * @return java.util.Date
	 */
	public static Date dateIncreaseByDay(Date date, int days) {
		Calendar cal = GregorianCalendar.getInstance(TimeZone.getTimeZone("GMT"));
		cal.setTime(date);
		cal.add(Calendar.DATE, days);
		return cal.getTime();
	}

	/**
	 * 某个日期增加小时获取结果日期
	 *
	 * @param date  日期
	 * @param hours 小时
	 * @return java.util.Date
	 */
	public static Date dateIncreaseByHours(Date date, int hours) {
		Calendar cal = GregorianCalendar.getInstance(TimeZone.getTimeZone("GMT"));
		cal.setTime(date);
		cal.add(Calendar.HOUR, hours);
		return cal.getTime();
	}

	public static String getMonth(long time) {
		String timeStr = time + "";
		return timeStr.substring(4, 6);
	}

	/**
	 * 查询time时间距当前时间间隔的分钟数
	 *
	 * @param time
	 * @return
	 */
	public static int getTimeIntelMinute(long time) {
		SimpleDateFormat sdf = new SimpleDateFormat("yyyyMMddHHmm");
		try {
			Date date = sdf.parse(time + "");
			Date curDate = new Date();
			long interMM = curDate.getTime() - date.getTime();
			int intervalMin = (int) (interMM / (1000 * 60));
			return intervalMin;
		} catch (ParseException e) {
			e.printStackTrace();
		}
		return 0;
	}

	/**
	 * 年-月-日 时:分:秒
	 *
	 * @param d <CODE>Date</CODE>
	 * @return String
	 */
	public static String detailFormatYMDH(Date d) {
		return formatUtil(d, "yyyy-MM-dd HH") + ":00:00";
	}

	public static long getDiffMinutes(long fctime) {
		return (System.currentTimeMillis() - fctime) / 60000;
	}

	public static long getDiffMinutes(long time, long fctime) {
		return (time - fctime) / 60000;
	}

	/**
	 * 获取当前时间加上xx小时
	 *
	 * @param date
	 * @param amount
	 * @return
	 */
	public static Date getAfterTimeByHours(Date date, int amount) {
		Calendar c = Calendar.getInstance();
		c.setTime(date);
		c.add(Calendar.HOUR_OF_DAY, amount);
		return c.getTime();
	}

	/**
	 * 字符串转换成日期 如果转换格式为空，则利用默认格式进行转换操作
	 *
	 * @param str    字符串
	 * @param format 日期格式
	 * @return 日期
	 * @throws ParseException
	 */
	public static Date str2Date(String str, String format) {
		if (null == str || "".equals(str)) {
			return null;
		}
		// 如果没有指定字符串转换的格式，则用默认格式进行转换
		if (null == format || "".equals(format)) {
			format = DATETIME_PATTERN_DATE_TIME;
		}
		SimpleDateFormat sdf = new SimpleDateFormat(format);
		Date date = null;
		try {
			date = sdf.parse(str);
			return date;
		} catch (ParseException e) {
			e.printStackTrace();
		}
		return null;
	}

	/**
	 * 日期转换为字符串
	 *
	 * @param date   日期
	 * @param format 日期格式
	 * @return 字符串
	 */
	public static String date2Str(Date date, String format) {
		if (null == date) {
			return null;
		}
		SimpleDateFormat sdf = new SimpleDateFormat(format);
		return sdf.format(date);
	}

	/*
	 * 计算时差: 分钟
	 */
	public static long diffMin(Date start, Date end) {
		try {
			long diff = end.getTime() - start.getTime();
			return (diff / (1000 * 60));
		} catch (Exception e) {
			e.printStackTrace();
		}
		return 0;
	}

	public static String date2Str13(Date date, String format) {
		if (null == date) {
			return null;
		}
		SimpleDateFormat sdf = new SimpleDateFormat(format);
		return sdf.format(date);
	}

	/**
	 * 获取日期的分钟数
	 *
	 * @param date
	 * @return
	 */
	public static int getMinute(Date date) {
		Calendar calendar = Calendar.getInstance();
		calendar.setTime(date);
		return calendar.get(Calendar.MINUTE);
	}

	/**
	 * 传入日期减去xx小时,返回format格式的日期字符串
	 *
	 * @param date
	 * @param subHour
	 * @param format  日期格式
	 */
	public static String getBeforeHourTime(String date, int subHour, String format) {
		Calendar calendar = Calendar.getInstance();
		SimpleDateFormat sdf = new SimpleDateFormat(format);
		Date datetime = null;
		try {
			datetime = sdf.parse(date);
		} catch (ParseException e) {
			e.printStackTrace();
		}
		calendar.setTime(datetime);
		calendar.add(Calendar.HOUR_OF_DAY, -subHour);

		Date now = calendar.getTime();
		String nowtime = sdf.format(now);
		return nowtime;
	}


	public static Date addtime(Date date, String field, int n) {
		GregorianCalendar gc = new GregorianCalendar();
		gc.setTime(date);
		if (field == "day") {
			gc.add(5, n);
		} else if (field == "year") {
			gc.add(1, n);
		} else if (field == "month") {
			gc.add(2, n);
		} else if (field == "week") {
			gc.add(4, n);
		} else if (field == "hour") {
			gc.add(GregorianCalendar.HOUR_OF_DAY, n);
		} else if (field == "minute") {
			gc.add(GregorianCalendar.MINUTE, n);
		} else {
			System.out.println("Wrong Field! Pls input year,month,week,day");
		}
		return gc.getTime();
	}

	public static Date stringToDatetime(String ddate) {
		int year = Integer.valueOf(ddate.substring(0, 4));
		int month = Integer.valueOf(ddate.substring(4, 6));
		int day = Integer.valueOf(ddate.substring(6, 8));
		int hour = Integer.valueOf(ddate.substring(8, 10));
		Calendar calendar = new GregorianCalendar(year, month - 1, day, hour, 0);
		Date date = calendar.getTime();
		return date;
	}

	/**
	 * 把format1转为format2的形式
	 * 如：20160504140000 转为 2016-05-04 14:00:00
	 *
	 * @param dateTime 时间点
	 * @param format1 当前时间点类型
	 * @param format2 返回时间点类型
	 * @return String
	 */
	public static String formatStr(String dateTime, String format1, String format2) {
		if (StringUtils.isBlank(dateTime)){
			return "";
		}
		SimpleDateFormat sdf1 = new SimpleDateFormat(format1);
		Date date;
		String timeStr = "";
		try {
			date = (Date) sdf1.parse(dateTime);
			SimpleDateFormat sdf2 = new SimpleDateFormat(format2);
			timeStr = sdf2.format(date);
		} catch (ParseException e) {
			e.printStackTrace();
		}
		return timeStr;
	}

	/**
	 * 时间字符串转时间戳
	 *
	 * @param date_str
	 * @param format
	 * @return
	 */
	public static String date2TimeStamp(String date_str, String format) {
		try {
			SimpleDateFormat sdf = new SimpleDateFormat(format);
			return String.valueOf(sdf.parse(date_str).getTime());
		} catch (Exception e) {
			e.printStackTrace();
		}
		return "";
	}

	/**
	 * 将LocalDateTime转换为Date
	 */
	public static Date locateDateTime2Date(LocalDateTime locateDate, String format) {
		DateTimeFormatter dateTimeFormatter = DateTimeFormatter.ofPattern(format);
		String dateStr = dateTimeFormatter.format(locateDate);
		Date date = null;
		try {
			SimpleDateFormat sdf = new SimpleDateFormat(format);
			date = sdf.parse(dateStr);
		} catch (ParseException e) {
			e.printStackTrace();
		}
		return date;
	}

	/**
	 * Date转LocalDateTime
	 * @param date
	 * @return
	 */
	public static LocalDateTime Date2locateDateTime(Date date) {
		SimpleDateFormat sdf = new SimpleDateFormat(DATETIME_PATTERN_DATE_TIME);
		try {
			String dateString = sdf.format(date);
			DateTimeFormatter dtf = DateTimeFormatter.ofPattern(DATETIME_PATTERN_DATE_TIME);
			return LocalDateTime.parse(dateString, dtf);
		} catch (Exception e) {
			e.printStackTrace();
		}
		return null;
	}

	public static String locateDate2Str(LocalDate locateDate, String format) {
		DateTimeFormatter dateTimeFormatter = DateTimeFormatter.ofPattern(format);
		String dateStr = dateTimeFormatter.format(locateDate);
		return dateStr;
	}

	/**
	 * 判断目标时间target是否大于等于source时间
	 */
	public static boolean isAfterOrEquals(LocalDateTime source, LocalDateTime target) {
		Date source2Date = locateDateTime2Date(source, DATETIME_PATTERN_DATE_TIME);
		Date target2Date = locateDateTime2Date(target, DATETIME_PATTERN_DATE_TIME);
		return target2Date.getTime() >= source2Date.getTime();
	}

	/**
	 * 判断目标时间target是否小于等于source时间
	 */
	public static boolean isLessOrEquals(LocalDateTime source, LocalDateTime target) {
		Date source2Date = locateDateTime2Date(source, DATETIME_PATTERN_DATE_TIME);
		Date target2Date = locateDateTime2Date(target, DATETIME_PATTERN_DATE_TIME);
		return target2Date.getTime() <= source2Date.getTime();
	}

	/**
	 * 判断当前时间current是否满足 begin <= current <= end
	 */
	public static boolean isBetween(LocalDateTime begin, LocalDateTime end, LocalDateTime current) {
		return isAfterOrEquals(begin, current) && isLessOrEquals(end, current);
	}

	/**
	 * 日期字符串根据pattern转为LocalDateTime
	 * @param dateStr
	 * @param pattern
	 * @return
	 */
	public static LocalDateTime dateStr2LocalDateTime(String dateStr, String pattern) {
		DateTimeFormatter dateTimeFormatter = DateTimeFormatter.ofPattern(pattern);
		return LocalDateTime.parse(dateStr, dateTimeFormatter);
	}

	public static String localDateTime2DateStr(LocalDateTime localDateTime, String pattern) {
		DateTimeFormatter dateTimeFormatter = DateTimeFormatter.ofPattern(pattern);
		return localDateTime.format(dateTimeFormatter);
	}

	/**
	 * 返回这个月的第一天的日期
	 * @param date
	 * @return
	 */
	public static Date getFirstDayOfMonthDate(Date date) {
		final Calendar calendar = Calendar.getInstance();
		calendar.setTime(date);
		calendar.set(Calendar.HOUR_OF_DAY, 0);
		calendar.set(Calendar.MINUTE, 0);
		calendar.set(Calendar.SECOND, 0);
		calendar.set(Calendar.MILLISECOND, 0);
		calendar.set(Calendar.DAY_OF_MONTH, 1);
		return calendar.getTime();
	}

	/**
	 * 返回这个月的最后一天的日期
	 * @param date
	 * @return
	 */
	public static Date getLastDayOfMonthDate(Date date) {
		Calendar now = Calendar.getInstance();
		now.setTime(date);
		Calendar end = Calendar.getInstance();
		//月份+1，天设置为0。下个月第0天，就是这个月最后一天
		now.add(Calendar.MONTH, 1);
		now.set(Calendar.DAY_OF_MONTH, 0);
		end.set(now.get(Calendar.YEAR), now.get(Calendar.MONTH), now.get(Calendar.DAY_OF_MONTH),23, 59, 59);
		Date endDate = end.getTime();
		return endDate;
	}


	/**
	 * 获取当天开始时间
	 * @return
	 */
	public static Date getDayBeginTime() {
		Calendar now = Calendar.getInstance();
		Calendar begin = Calendar.getInstance();
		begin.set(now.get(Calendar.YEAR), now.get(Calendar.MONTH), now.get(Calendar.DAY_OF_MONTH),0, 0, 0);
		Date beginTime = begin.getTime();
		return beginTime;
	}

	/**
	 * 获取当天结束时间
	 */
	public static Date getDayEndTime() {
		Calendar now = Calendar.getInstance();
		Calendar end = Calendar.getInstance();
		end.set(now.get(Calendar.YEAR), now.get(Calendar.MONTH), now.get(Calendar.DAY_OF_MONTH),23, 59, 59);
		Date endTime = end.getTime();
		return endTime;
	}


	/**
	 * 判断开始到结束两个时间之间是否大于目标时间段
	 * @param beginDate 开始时间
	 * @param endDate   结束时间
	 * @param targetTimeFrame 时间段;单位:小时
 	 * @return
	 */
	public static Boolean isGreaterTargetTimeFrame(Date beginDate, Date endDate, Integer targetTimeFrame) throws ParseException {
		SimpleDateFormat sdf = new SimpleDateFormat("yyyy-M-d HH:mm:ss");
		long cha = endDate.getTime() - beginDate.getTime();
		double result = cha * 1.0 / (1000 * 60 * 60);
		if (result <= targetTimeFrame) {
			return Boolean.FALSE;
		} else {
			return Boolean.TRUE;
		}
	}

	/**
	 * 判断当前时间是否在时间范围内
	 * @param targetTime 目标时间
	 * @param startTime 开始时间
	 * @param endTime 结束时间
	 * @return
	 */
	public static boolean isEffectiveDate(Date targetTime, Date startTime, Date endTime) {
		if (targetTime.getTime() == startTime.getTime()
				|| targetTime.getTime() == endTime.getTime()) {
			return true;
		}

		Calendar date = Calendar.getInstance();
		date.setTime(targetTime);

		Calendar begin = Calendar.getInstance();
		begin.setTime(startTime);

		Calendar end = Calendar.getInstance();
		end.setTime(endTime);

		if (date.after(begin) && date.before(end)) {
			return true;
		} else {
			return false;
		}
	}


	/**
	 * 将小时数转为时分秒时间格式
	 * @param hours
	 * @return
	 */
	public static String hours2DateStr(double hours) {
		int hoursInt = (int) hours;
		int minutes = (int) ((hours - hoursInt) * 60);
		int seconds = (int) (((hours - hoursInt) * 60 - minutes) * 60);
		return String.format("%02d:%02d:%02d", hoursInt, minutes, seconds);
	}

}
