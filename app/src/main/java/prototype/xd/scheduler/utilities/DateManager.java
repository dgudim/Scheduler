package prototype.xd.scheduler.utilities;

import static prototype.xd.scheduler.utilities.Static.DAY_FLAG_GLOBAL;
import static prototype.xd.scheduler.utilities.Static.TIME_RANGE_SEPARATOR;

import android.icu.text.DateFormatSymbols;

import androidx.annotation.NonNull;
import androidx.core.os.LocaleListCompat;

import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.time.DayOfWeek;
import java.time.LocalDate;
import java.time.YearMonth;
import java.util.Calendar;
import java.util.Date;
import java.util.List;
import java.util.Locale;
import java.util.Objects;
import java.util.TimeZone;
import java.util.concurrent.TimeUnit;

import prototype.xd.scheduler.entities.TodoEntry;
import prototype.xd.scheduler.utilities.misc.DefaultedMutableLiveData;

@SuppressWarnings({
        "StaticNonFinalField",
        "SynchronizationOnStaticField",
        "PublicStaticCollectionField",
        "NonPrivateFieldAccessedInSynchronizedContext",
        "FieldAccessedSynchronizedAndUnsynchronized"})
public final class DateManager {
    
    public static final String NAME = DateManager.class.getSimpleName();
    
    private DateManager() throws InstantiationException {
        throw new InstantiationException(NAME);
    }
    
    public static final long ONE_MINUTE_MS = 60000L;
    
    public static final DefaultedMutableLiveData<TimeZone> systemTimeZone = new DefaultedMutableLiveData<>(TimeZone.getDefault());
    public static final TimeZone utcTimeZone = TimeZone.getTimeZone("UTC");
    
    public static long currentDayUTC = DAY_FLAG_GLOBAL;
    public static long currentTimestampUTC = DAY_FLAG_GLOBAL;
    private static LocalDate currentDate = LocalDate.now();
    
    public static long currentlySelectedDayUTC = DAY_FLAG_GLOBAL;
    public static long currentlySelectedTimestampUTC = DAY_FLAG_GLOBAL;
    
    @NonNull
    public static final Locale systemLocale = Objects.requireNonNull(LocaleListCompat.getDefault().get(0));
    private static final DateFormat dateTimeFormatLocal = new SimpleDateFormat("dd/MM HH:mm", systemLocale); // NOSONAR, method is synchronized
    private static final DateFormat dateFormatUTC = new SimpleDateFormat("dd/MM", systemLocale); // NOSONAR
    private static final DateFormat dateFormatMonthNamesUTC = new SimpleDateFormat("MMM d", systemLocale); // NOSONAR
    private static final DateFormat timeFormatLocal = new SimpleDateFormat("HH:mm", systemLocale); // NOSONAR
    
    @NonNull
    public static final List<String> BG_NAMES_ROOT;
    public static final String DEFAULT_BACKGROUND_NAME = "default.png";
    @NonNull
    private static final List<String> WEEK_DAYS_LOCAL;
    
    public static final List<DayOfWeek> FIRST_DAYS_OF_WEEK_ROOT = List.of(DayOfWeek.MONDAY, DayOfWeek.SATURDAY, DayOfWeek.SUNDAY);
    @NonNull
    public static final List<String> FIRST_DAYS_OF_WEEK_LOCAL;
    public static final Static.DefaultedEnum<DayOfWeek> FIRST_DAY_OF_WEEK =
            new Static.DefaultedEnum<>("first_week_day", DayOfWeek.MONDAY, DayOfWeek.class);
    
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
        
        WEEK_DAYS_LOCAL = List.of(weekDaysLocal);
        
        DayOfWeek[] weekDays = DayOfWeek.values();
        String[] bgNamesRoot = new String[8];
        for (int i = 0; i < 7; i++) {
            bgNamesRoot[i] = weekDays[i].name().toLowerCase(Locale.ROOT) + ".png";
        }
        bgNamesRoot[7] = DEFAULT_BACKGROUND_NAME;
        
        BG_NAMES_ROOT = List.of(bgNamesRoot);
        
        String[] localizedWeekdays = new String[FIRST_DAYS_OF_WEEK_ROOT.size()];
        for (int i = 0; i < localizedWeekdays.length; i++) {
            localizedWeekdays[i] = WEEK_DAYS_LOCAL.get(FIRST_DAYS_OF_WEEK_ROOT.get(i).ordinal());
        }
        
        FIRST_DAYS_OF_WEEK_LOCAL = List.of(localizedWeekdays);
        
        dateFormatUTC.setTimeZone(utcTimeZone);
        dateFormatMonthNamesUTC.setTimeZone(utcTimeZone);
    }
    
    public static synchronized void updateTimeZone() {
        TimeZone newTimeZone = TimeZone.getDefault();
        if (systemTimeZone.getValue().equals(newTimeZone)) {
            return;
        }
        Logger.debug(NAME, "Timezone changed to " + newTimeZone.getID());
        // reinitialize all the stuff
        systemTimeZone.postValue(newTimeZone);
        // update timezones of calendar and formatters
        timeFormatLocal.setTimeZone(newTimeZone);
        dateTimeFormatLocal.setTimeZone(newTimeZone);
    }
    
    public static synchronized void updateDate() {
        currentTimestampUTC = getCurrentTimestampUTC();
        currentDate = LocalDate.now();
        currentDayUTC = currentDate.toEpochDay();
    }
    
    public static synchronized void selectDate(@NonNull LocalDate date) {
        updateDate();
        currentlySelectedDayUTC = date.toEpochDay();
        currentlySelectedTimestampUTC = daysToMs(currentlySelectedDayUTC);
    }
    
    public static long getStartOfMonthDayUTC(@NonNull YearMonth month) {
        return month.atDay(1).toEpochDay();
    }
    
    public static long getEndOfMonthDayUTC(@NonNull YearMonth month) {
        return month.atEndOfMonth().toEpochDay();
    }
    
    @NonNull
    public static String getTimeSpan(@NonNull TodoEntry.TimeRange timeRange) {
        return getTimeSpan(timeRange.getStart(), timeRange.getEnd());
    }
    
    @NonNull
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
            return dateFromHourMinute + TIME_RANGE_SEPARATOR + dateToHourMinute;
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
                return dateFromDay + " " + dateFromHourMinute + TIME_RANGE_SEPARATOR + dateToDay + " " + dateToHourMinute;
            } else {
                //24/10 10:40 - 10/11 12:30
                return dateFrom + TIME_RANGE_SEPARATOR + dateTo;
            }
        }
    }
    
    public static long msUTCtoDaysLocal(long msUTC) {
        return msToDays(msUTCtoMsLocal(msUTC));
    }
    
    public static synchronized long msUTCtoMsLocal(long msUTC) {
        return msUTC + systemTimeZone.getValue().getOffset(msUTC);
    }
    
    public static long msToDays(long msUTC) {
        return TimeUnit.DAYS.convert(msUTC, TimeUnit.MILLISECONDS);
    }
    
    public static long daysToMs(long daysUTC) {
        return TimeUnit.MILLISECONDS.convert(daysUTC, TimeUnit.DAYS) + ONE_MINUTE_MS;
    }
    
    // return date and time given a UTC timestamp
    @NonNull
    public static String datetimeStringLocalFromMsUTC(long msUTC) {
        synchronized (dateTimeFormatLocal) {
            return dateTimeFormatLocal.format(new Date(msUTC));
        }
    }
    
    // return date given a UTC timestamp
    @NonNull
    public static String dateStringUTCFromMsUTC(long msUTC) {
        synchronized (dateFormatUTC) {
            return dateFormatUTC.format(new Date(msUTC));
        }
    }
    
    // return date (months are 3 letters instead of numbers) and time given a UTC timestamp
    @NonNull
    public static String dateStringMonthNamesUTCFromMsUTC(long msUTC) {
        synchronized (dateFormatMonthNamesUTC) {
            return dateFormatMonthNamesUTC.format(new Date(msUTC));
        }
    }
    
    // returns current time
    @NonNull
    public static String getCurrentTimeStringLocal() {
        synchronized (timeFormatLocal) {
            return timeFormatLocal.format(new Date());
        }
    }
    
    // returns current timestamp in UTC
    public static long getCurrentTimestampUTC() {
        return System.currentTimeMillis();
    }
    
    @NonNull
    public static String getCurrentWeekdayBgName() {
        return currentDate.getDayOfWeek().name().toLowerCase(Locale.ROOT) + ".png";
    }
    
    @NonNull
    public static String getLocalWeekdayByIndex(int index, @NonNull String defaultValue) {
        return index >= 7 ? defaultValue : WEEK_DAYS_LOCAL.get(index);
    }
    
    @NonNull
    public static LocalDate getCurrentDate() {
        return currentDate;
    }
}
