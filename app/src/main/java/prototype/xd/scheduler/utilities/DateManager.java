package prototype.xd.scheduler.utilities;

import static prototype.xd.scheduler.utilities.Keys.DAY_FLAG_GLOBAL;

import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.time.LocalDate;
import java.util.Date;
import java.util.Locale;
import java.util.TimeZone;
import java.util.concurrent.TimeUnit;

public class DateManager {
    
    private DateManager() {
        throw new IllegalStateException("Utility date manager class");
    }
    
    public static final TimeZone timeZone_SYSTEM = TimeZone.getDefault();
    
    public static LocalDate currentDate = LocalDate.now();
    
    public static long currentDay = DAY_FLAG_GLOBAL;
    public static long currentTimestamp = DAY_FLAG_GLOBAL;
    public static long currentlySelectedDay = DAY_FLAG_GLOBAL;
    
    private static final DateFormat dateTimeFormat = new SimpleDateFormat("dd/MM HH:mm", Locale.ROOT);
    private static final DateFormat dateFormat = new SimpleDateFormat("dd/MM", Locale.ROOT);
    private static final DateFormat timeFormat = new SimpleDateFormat("HH:mm", Locale.ROOT);
    
    public static void updateDate() {
        currentTimestamp = getCurrentTimestamp();
        currentDate = LocalDate.now();
        currentDay = currentDate.toEpochDay();
    }
    
    public static void selectDay(long day) {
        updateDate();
        currentlySelectedDay = day;
    }
    
    public static void selectCurrentDay() {
        updateDate();
        currentlySelectedDay = currentDay;
    }
    
    public static String getTimeSpan(long timeFromMsUTC, long timeToMsUTC) {
        String dateFrom = datetimeFromMsUTC(timeFromMsUTC);
        String dateTo = datetimeFromMsUTC(timeToMsUTC);
        String[] dateFromSplit = dateFrom.split(" ");
        String[] dateToSplit = dateTo.split(" ");
        
        String dateFromDayMonth = dateFromSplit[0];
        String dateFromHourMinute = dateFromSplit[1];
    
        String dateToDayMonth = dateToSplit[0];
        String dateToHourMinute = dateToSplit[1];
        
        // month and day is the same
        if (dateFrom.equals(dateTo) || dateFromDayMonth.equals(dateToDayMonth)) {
            // 20:30 - 23:10
            return dateFromHourMinute + " - " + dateToHourMinute;
        } else {
            String[] dateFromDayMonthSplit = dateFromDayMonth.split("/");
            String[] dateToDayMonthSplit = dateToDayMonth.split("/");
            
            String dateFromDay = dateFromDayMonthSplit[0];
            String dateFromMonth = dateFromDayMonthSplit[1];
            
            String dateToDay = dateToDayMonthSplit[0];
            String dateToMonth = dateToDayMonthSplit[1];
            
            //month is the same
            if (dateFromMonth.equals(dateToMonth)) {
                //24 20:40 - 30 21:30
                return dateFromDay + " " + dateFromHourMinute + " - " + dateToDay + " " + dateToHourMinute;
            } else {
                //24/10 10:40 - 10/11 12:30
                return dateFrom + " - " + dateTo;
            }
        }
    }
    
    public static long daysFromMsUTC(long msUTC, TimeZone timezone) {
        return TimeUnit.DAYS.convert(addTimeZoneOffset(msUTC, timezone), TimeUnit.MILLISECONDS);
    }
    
    public static String datetimeFromMsUTC(long msUTC) {
        return dateTimeFormat.format(new Date(msUTC));
    }
    
    public static String dateStringFromMsUTC(long msUTC) {
        return dateFormat.format(new Date(msUTC));
    }
    
    public static String getCurrentTimeString() {
        return timeFormat.format(new Date());
    }
    
    public static String getCurrentDateTimeString() {
        return dateTimeFormat.format(new Date());
    }
    
    public static long addTimeZoneOffset(long epoch, TimeZone timeZone) {
        return epoch + timeZone.getOffset(epoch);
    }
    
    public static long getCurrentTimestamp() {
        return System.currentTimeMillis();
    }
    
}
