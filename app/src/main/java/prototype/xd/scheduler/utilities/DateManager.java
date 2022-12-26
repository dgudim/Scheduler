package prototype.xd.scheduler.utilities;

import static prototype.xd.scheduler.utilities.Keys.DAY_FLAG_GLOBAL;

import androidx.annotation.NonNull;
import androidx.core.os.LocaleListCompat;

import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.time.LocalDate;
import java.time.YearMonth;
import java.time.ZoneOffset;
import java.util.Date;
import java.util.Locale;
import java.util.Objects;
import java.util.TimeZone;
import java.util.concurrent.TimeUnit;

public class DateManager {
    
    private DateManager() {
        throw new IllegalStateException("Utility date manager class");
    }
    
    public static final long ONE_MINUTE_MS = 60000L;
    
    public static LocalDate currentDate = LocalDate.now(ZoneOffset.UTC);
    public static final TimeZone systemTimeZone = TimeZone.getDefault();
    public static final TimeZone utcTimeZone = TimeZone.getTimeZone("UTC");
    
    public static long currentDayUTC = DAY_FLAG_GLOBAL;
    public static long currentTimestampUTC = DAY_FLAG_GLOBAL;
    
    public static long currentlySelectedDayUTC = DAY_FLAG_GLOBAL;
    public static long currentlySelectedTimestampUTC = DAY_FLAG_GLOBAL;
    
    @NonNull
    public static final Locale systemLocale = Objects.requireNonNull(LocaleListCompat.getDefault().get(0));
    private static final DateFormat dateTimeFormat = new SimpleDateFormat("dd/MM HH:mm", systemLocale);
    
    private static final DateFormat dateFormat = new SimpleDateFormat("dd/MM", systemLocale);
    private static final DateFormat dateFormatMonthNames = new SimpleDateFormat("MMM d", systemLocale);
    private static final DateFormat timeFormat = new SimpleDateFormat("HH:mm", systemLocale);
    
    public static void updateDate() {
        currentTimestampUTC = getCurrentTimestampUTC();
        currentDate = LocalDate.now(ZoneOffset.UTC);
        currentDayUTC = currentDate.toEpochDay();
    }
    
    public static void selectDate(LocalDate date) {
        updateDate();
        currentlySelectedTimestampUTC = date.atStartOfDay().toInstant(ZoneOffset.UTC).toEpochMilli() + ONE_MINUTE_MS; // add one minute because some stuff breaks at midnight
        currentlySelectedDayUTC = date.toEpochDay();
    }
    
    public static long getStartOfMonthDayUTC(YearMonth month) {
        return month.atDay(1).toEpochDay();
    }
    
    public static long getEndOfMonthDayUTC(YearMonth month) {
        return month.atEndOfMonth().toEpochDay();
    }
    
    public static String getTimeSpan(long timeFromMsUTC, long timeToMsUTC) {
        String dateFrom = datetimeStringFromMsUTC(timeFromMsUTC);
        String dateTo = datetimeStringFromMsUTC(timeToMsUTC);
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
    
    public static long daysUTCFromMsUTC(long msUTC) {
        return TimeUnit.DAYS.convert(msUTC, TimeUnit.MILLISECONDS);
    }
    
    public static long msUTCFromDaysUTC(long daysUTC) {
        return TimeUnit.MILLISECONDS.convert(daysUTC, TimeUnit.DAYS);
    }
    
    // return date and time given a UTC timestamp
    public static String datetimeStringFromMsUTC(long msUTC) {
        return dateTimeFormat.format(new Date(msUTC));
    }
    
    // return date given a UTC timestamp
    public static String dateStringFromMsUTC(long msUTC) {
        return dateFormat.format(new Date(msUTC));
    }
    
    // return date (months are 3 letters instead of numbers) and time given a UTC timestamp
    public static String dateStringMonthNamesFromMsUTC(long msUTC) {
        return dateFormatMonthNames.format(new Date(msUTC));
    }
    
    // returns current time
    public static String getCurrentTimeString() {
        return timeFormat.format(new Date());
    }
    
    // returns current date and time
    public static String getCurrentDateTimeString() {
        return dateTimeFormat.format(new Date());
    }
    
    // returns current timestamp in UTC
    public static long getCurrentTimestampUTC() {
        return System.currentTimeMillis();
    }
    
}
