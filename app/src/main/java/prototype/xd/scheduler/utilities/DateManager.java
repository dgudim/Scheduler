package prototype.xd.scheduler.utilities;

import static prototype.xd.scheduler.utilities.Keys.DAY_FLAG_GLOBAL;

import android.icu.text.DateFormatSymbols;

import androidx.annotation.NonNull;
import androidx.core.os.LocaleListCompat;

import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.time.DayOfWeek;
import java.time.LocalDate;
import java.time.YearMonth;
import java.util.Arrays;
import java.util.Calendar;
import java.util.Collections;
import java.util.Date;
import java.util.List;
import java.util.Locale;
import java.util.Objects;
import java.util.TimeZone;
import java.util.concurrent.TimeUnit;

import prototype.xd.scheduler.entities.TodoListEntry;

public class DateManager {
    
    private DateManager() {
        throw new IllegalStateException("Utility date manager class");
    }
    
    public static final long ONE_MINUTE_MS = 60000L;
    
    public static TimeZone systemTimeZone = TimeZone.getDefault();
    public static final TimeZone utcTimeZone = TimeZone.getTimeZone("UTC");
    
    public static long currentDayUTC = DAY_FLAG_GLOBAL;
    public static long currentTimestampUTC = DAY_FLAG_GLOBAL;
    public static LocalDate currentDate = LocalDate.now();
    
    public static long currentlySelectedDayUTC = DAY_FLAG_GLOBAL;
    public static long currentlySelectedTimestampUTC = DAY_FLAG_GLOBAL;
    
    @NonNull
    public static final Locale systemLocale = Objects.requireNonNull(LocaleListCompat.getDefault().get(0));
    private static final DateFormat dateTimeFormatLocal = new SimpleDateFormat("dd/MM HH:mm", systemLocale);
    private static final DateFormat dateFormatUTC = new SimpleDateFormat("dd/MM", systemLocale);
    private static final DateFormat dateFormatMonthNamesUTC = new SimpleDateFormat("MMM d", systemLocale);
    private static final DateFormat timeFormatLocal = new SimpleDateFormat("HH:mm", systemLocale);
    
    public static final List<String> WEEK_DAYS_ROOT = Collections.unmodifiableList(Arrays.asList("monday", "tuesday", "wednesday", "thursday", "friday", "saturday", "sunday", "default"));
    public static final String DEFAULT_BACKGROUND_NAME = DateManager.WEEK_DAYS_ROOT.get(7) + ".png"; // get "default"
    private static final List<String> WEEK_DAYS_LOCAL;
    
    public static final List<DayOfWeek> FIRST_DAYS_OF_WEEK = Collections.unmodifiableList(
            Arrays.asList(DayOfWeek.MONDAY, DayOfWeek.SATURDAY, DayOfWeek.SUNDAY));
    public static final Keys.DefaultedEnum<DayOfWeek> FIRST_DAY_OF_WEEK =
            new Keys.DefaultedEnum<>("first_week_day", DayOfWeek.MONDAY, DayOfWeek.class);
    
    private static final Calendar localLookupCalendar = Calendar.getInstance(systemTimeZone, systemLocale);
    
    static {
        // init local weekdays (remap)
        String[] weekDaysLocal = new String[7];
        String[] newDays = new DateFormatSymbols(systemLocale).getWeekdays();
        weekDaysLocal[0] = newDays[Calendar.MONDAY];
        weekDaysLocal[1] = newDays[Calendar.TUESDAY];
        weekDaysLocal[2] = newDays[Calendar.WEDNESDAY];
        weekDaysLocal[3] = newDays[Calendar.THURSDAY];
        weekDaysLocal[4] = newDays[Calendar.FRIDAY];
        weekDaysLocal[5] = newDays[Calendar.SATURDAY];
        weekDaysLocal[6] = newDays[Calendar.SUNDAY];
        
        WEEK_DAYS_LOCAL = Collections.unmodifiableList(Arrays.asList(weekDaysLocal));
        
        dateFormatUTC.setTimeZone(utcTimeZone);
        dateFormatMonthNamesUTC.setTimeZone(utcTimeZone);
    }
    
    public static synchronized boolean checkIfTimeSettingsChanged() {
        TimeZone newTimeZone = TimeZone.getDefault();
        if (!newTimeZone.equals(systemTimeZone)) {
            Logger.debug("DateManager", "Timezone changed! Old: " + systemTimeZone.getID() + " | New: " + newTimeZone.getID());
            // reinitialize all the stuff
            systemTimeZone = newTimeZone;
            // update timezones of calendar and formatters
            localLookupCalendar.setTimeZone(systemTimeZone);
            timeFormatLocal.setTimeZone(systemTimeZone);
            dateTimeFormatLocal.setTimeZone(systemTimeZone);
            updateDate();
            return true;
        }
        updateDate();
        return false;
    }
    
    private static synchronized void updateDate() {
        currentTimestampUTC = getCurrentTimestampUTC();
        currentDate = LocalDate.now();
        currentDayUTC = currentDate.toEpochDay();
    }
    
    public static void selectDate(LocalDate date) {
        updateDate();
        currentlySelectedDayUTC = date.toEpochDay();
        currentlySelectedTimestampUTC = daysToMs(currentlySelectedDayUTC);
    }
    
    public static long getStartOfMonthDayUTC(YearMonth month) {
        return month.atDay(1).toEpochDay();
    }
    
    public static long getEndOfMonthDayUTC(YearMonth month) {
        return month.atEndOfMonth().toEpochDay();
    }
    
    public static String getTimeSpan(TodoListEntry.TimeRange timeRange) {
        return getTimeSpan(timeRange.getStart(), timeRange.getEnd());
    }
    
    public static String getTimeSpan(long timeFromMsUTC, long timeToMsUTC) {
        if (timeFromMsUTC == timeToMsUTC) {
            return datetimeStringLocalFromMsUTC(timeFromMsUTC);
        }
        String dateFrom = datetimeStringLocalFromMsUTC(timeFromMsUTC);
        String dateTo = datetimeStringLocalFromMsUTC(timeToMsUTC);
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
    
    public static long msUTCtoDaysLocal(long msUTC) {
        return msToDays(msUTCtoMsLocal(msUTC));
    }
    
    public static long msUTCtoMsLocal(long msUTC) {
        return msUTC + systemTimeZone.getOffset(msUTC);
    }
    
    public static long msToDays(long msUTC) {
        return TimeUnit.DAYS.convert(msUTC, TimeUnit.MILLISECONDS);
    }
    
    public static long daysToMs(long daysUTC) {
        return TimeUnit.MILLISECONDS.convert(daysUTC, TimeUnit.DAYS) + ONE_MINUTE_MS;
    }
    
    // return date and time given a UTC timestamp
    public static String datetimeStringLocalFromMsUTC(long msUTC) {
        synchronized (dateTimeFormatLocal) {
            return dateTimeFormatLocal.format(new Date(msUTC));
        }
    }
    
    // return date given a UTC timestamp
    public static String dateStringUTCFromMsUTC(long msUTC) {
        synchronized (dateFormatUTC) {
            return dateFormatUTC.format(new Date(msUTC));
        }
    }
    
    // return date (months are 3 letters instead of numbers) and time given a UTC timestamp
    public static String dateStringMonthNamesUTCFromMsUTC(long msUTC) {
        synchronized (dateFormatMonthNamesUTC) {
            return dateFormatMonthNamesUTC.format(new Date(msUTC));
        }
    }
    
    // returns current time
    public static String getCurrentTimeStringLocal() {
        synchronized (timeFormatLocal) {
            return timeFormatLocal.format(new Date());
        }
    }
    
    // returns current date and time
    public static String getCurrentDateTimeStringLocal() {
        synchronized (dateTimeFormatLocal) {
            return dateTimeFormatLocal.format(new Date());
        }
    }
    
    // returns current timestamp in UTC
    public static long getCurrentTimestampUTC() {
        return System.currentTimeMillis();
    }
    
    public static String getCurrentWeekdayLocaleAgnosticString() {
        localLookupCalendar.setTimeInMillis(System.currentTimeMillis());
        return WEEK_DAYS_ROOT.get(normalizeCalendarDayOffset(localLookupCalendar.get(Calendar.DAY_OF_WEEK)));
    }
    
    public static String[] getFirstDaysOfWeekLocal() {
        String[] localizedWeekdays = new String[FIRST_DAYS_OF_WEEK.size()];
        for (int i = 0; i < localizedWeekdays.length; i++) {
            localizedWeekdays[i] = WEEK_DAYS_LOCAL.get(FIRST_DAYS_OF_WEEK.get(i).ordinal());
        }
        return localizedWeekdays;
    }
    
    public static String getLocalWeekdayByIndex(int index, String defaultValue) {
        return index >= 7 ? defaultValue : WEEK_DAYS_LOCAL.get(index);
    }
    
    private static int normalizeCalendarDayOffset(int day) {
        // Calendar.SUNDAY == 1
        return day == 1 ? 6 : day - 2;
    }
    
}
