package prototype.xd.scheduler.utilities;

import static prototype.xd.scheduler.utilities.Static.DAY_FLAG_GLOBAL;
import static prototype.xd.scheduler.utilities.Static.TIME_RANGE_SEPARATOR;

import android.content.Context;
import android.icu.text.DateFormatSymbols;
import android.os.LocaleList;

import androidx.annotation.NonNull;

import java.time.DayOfWeek;
import java.time.Instant;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.time.YearMonth;
import java.time.ZoneId;
import java.time.ZoneOffset;
import java.time.format.DateTimeFormatter;
import java.util.Calendar;
import java.util.List;
import java.util.Locale;
import java.util.Objects;
import java.util.TimeZone;
import java.util.concurrent.TimeUnit;

import prototype.xd.scheduler.R;
import prototype.xd.scheduler.utilities.misc.DefaultedMutableLiveData;

@SuppressWarnings({
        "StaticNonFinalField",
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
    
    public static long currentDayUTC = DAY_FLAG_GLOBAL;
    public static long currentTimestampUTC = DAY_FLAG_GLOBAL;
    private static LocalDate currentDate = LocalDate.now();
    
    public static long currentlySelectedDayUTC = DAY_FLAG_GLOBAL;
    
    @NonNull
    public static final Locale systemLocale = Objects.requireNonNull(LocaleList.getDefault().get(0));
    private static final DateTimeFormatter dayMonthTimeFormat = DateTimeFormatter.ofPattern("dd/MM HH:mm", systemLocale);
    private static final DateTimeFormatter dayMonthFormat = DateTimeFormatter.ofPattern("dd/MM", systemLocale);
    private static final DateTimeFormatter dayMonthNameFormat = DateTimeFormatter.ofPattern("d MMM", systemLocale);
    private static final DateTimeFormatter monthNameFormat = DateTimeFormatter.ofPattern("MMM", systemLocale);
    private static final DateTimeFormatter dayTimeFormat = DateTimeFormatter.ofPattern("d HH:mm", systemLocale);
    private static final DateTimeFormatter dayFormat = DateTimeFormatter.ofPattern("d", systemLocale);
    private static final DateTimeFormatter timeFormat = DateTimeFormatter.ofPattern("HH:mm", systemLocale);
    
    private static final DateTimeFormatter fullFormat = DateTimeFormatter.ofPattern("HH:mm dd/MM yyyy", systemLocale);
    private static final DateTimeFormatter dayMonthNameYearFormat = DateTimeFormatter.ofPattern("dd MMM yyyy", systemLocale);
    private static final DateTimeFormatter monthNameYearFormat = DateTimeFormatter.ofPattern("MMM yyyy", systemLocale);
    private static final DateTimeFormatter yearFormat = DateTimeFormatter.ofPattern("yyyy", systemLocale);
    
    
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
    }
    
    public static synchronized void updateTimeZone() {
        TimeZone newTimeZone = TimeZone.getDefault();
        if (Objects.equals(systemTimeZone.getValue(), newTimeZone)) {
            return;
        }
        Logger.debug(NAME, "Timezone changed to " + newTimeZone.getID());
        // reinitialize all the stuff
        systemTimeZone.postValue(newTimeZone);
    }
    
    public static synchronized void updateDate() {
        currentTimestampUTC = getCurrentTimestampUTC();
        currentDate = LocalDate.now();
        currentDayUTC = currentDate.toEpochDay();
    }
    
    public static synchronized void selectDate(@NonNull LocalDate date) {
        updateDate();
        currentlySelectedDayUTC = date.toEpochDay();
    }
    
    public static long getStartOfMonthDayUTC(@NonNull YearMonth month) {
        return month.atDay(1).toEpochDay();
    }
    
    public static long getEndOfMonthDayUTC(@NonNull YearMonth month) {
        return month.atEndOfMonth().toEpochDay();
    }
    
    @NonNull
    public static LocalDate msUTCtoLocalDate(long msUTC, @NonNull ZoneId zone) {
        return LocalDate.ofInstant(Instant.ofEpochMilli(msUTC), zone);
    }
    
    @NonNull
    public static LocalDateTime msUTCtoLocalDateTime(long msUTC, @NonNull ZoneId zone) {
        return LocalDateTime.ofInstant(Instant.ofEpochMilli(msUTC), zone);
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
        return TimeUnit.MILLISECONDS.convert(daysUTC, TimeUnit.DAYS);
    }
    
    // return currently selected date date
    @NonNull
    public static String getCurrentlySelectedDate() {
        return dayMonthFormat.format(msUTCtoLocalDate(daysToMs(currentlySelectedDayUTC), ZoneOffset.UTC));
    }
    
    // return date (months are 3 letters instead of numbers) and time given a UTC timestamp
    @NonNull
    public static String dateStringMonthNamesUTCFromMsUTC(long msUTC) {
        return dayMonthNameFormat.format(msUTCtoLocalDate(msUTC, ZoneOffset.UTC));
    }
    
    @NonNull
    public static String getTimeSpan(@NonNull TimeRange timeRange, long referenceMsUTC, boolean isAllDay, @NonNull Context context) {
        ZoneId zone = isAllDay ? ZoneOffset.UTC : ZoneId.systemDefault();
        LocalDateTime from = msUTCtoLocalDateTime(timeRange.getStart(), zone);
        LocalDateTime to = msUTCtoLocalDateTime(timeRange.getEnd(), zone);
        LocalDate reference = msUTCtoLocalDateTime(referenceMsUTC, ZoneOffset.UTC).toLocalDate();
        
        String allDayStrBare = context.getString(R.string.calendar_event_all_day);
        String allDayStr = " (" + allDayStrBare + ")";
        
        if (from.getYear() == to.getYear()) {
            if (from.getMonth() == to.getMonth()) {
                if (from.getDayOfMonth() == to.getDayOfMonth()) {
                    // day, month and year are the same
                    if (isAllDay) {
                        if (Objects.equals(from.toLocalDate(), reference)) {
                            // All day
                            return allDayStrBare;
                        }
                        if (reference.getYear() == from.getYear()) {
                            // 24 Jan (All day)
                            return dayMonthNameFormat.format(from) + allDayStr;
                        }
                        // 24 Jan 2023 (All day)
                        return dayMonthNameYearFormat.format(from) + allDayStr;
                    }
                    String timeStr = timeFormat.format(from) + TIME_RANGE_SEPARATOR + timeFormat.format(to);
                    if (Objects.equals(from.toLocalDate(), reference)) {
                        // 20:30 - 23:10
                        return timeStr;
                    }
                    if (reference.getYear() == from.getYear()) {
                        // (20:30 - 23:10) 24 Jan
                        return "(" + timeStr + ") " + dayMonthNameFormat.format(from);
                    }
                    // (20:30 - 23:10) 24 Jan 2023
                    return "(" + timeStr + ") " + dayMonthNameYearFormat.format(from);
                } else {
                    // month and year are the same
                    String coreStr = isAllDay ?
                            dayFormat.format(from) + TIME_RANGE_SEPARATOR + dayFormat.format(to) :
                            dayTimeFormat.format(from) + TIME_RANGE_SEPARATOR + dayTimeFormat.format(to);
                    
                    if (reference.getYear() == from.getYear()) {
                        // (20 - 30) Jan (All day)
                        // (20 20:40 - 30 19:20) Jan
                        return "(" + coreStr + ") " + monthNameFormat.format(from) + (isAllDay ? allDayStr : "");
                    }
                    // (20 - 30) Jan 2023 (All day)
                    // (20 20:40 - 30 19:20) 2023 Jan
                    return "(" + coreStr + ") " + monthNameYearFormat.format(from) + (isAllDay ? allDayStr : "");
                }
            } else {
                // year is the same
                String coreStr = isAllDay ?
                        dayMonthFormat.format(from) + TIME_RANGE_SEPARATOR + dayMonthFormat.format(to) :
                        dayMonthTimeFormat.format(from) + TIME_RANGE_SEPARATOR + dayMonthTimeFormat.format(to);
                
                if (reference.getYear() == from.getYear()) {
                    // 20/01 - 30/02 (All day)
                    // 20/01 20:30 - 30/02 23:10
                    return coreStr + (isAllDay ? allDayStr : "");
                }
                // (20/01 - 30/02) 2023 (All day)
                // (20/01 20:30 - 30/02 23:10) 2023
                return "(" + coreStr + ") " + yearFormat.format(from) + allDayStr;
            }
        }
        if (isAllDay) {
            // 24/10 2023 - 30/10 2024 (All day)
            return dayMonthNameYearFormat.format(from) + TIME_RANGE_SEPARATOR + dayMonthNameYearFormat.format(to) + allDayStr;
        }
        // 20:40 24/10 2023 - 13:40 30/10 2024
        return fullFormat.format(from) + TIME_RANGE_SEPARATOR + fullFormat.format(to);
    }
    
    // returns current time
    @NonNull
    public static String getCurrentTimeStringLocal() {
        return timeFormat.format(LocalTime.now());
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
