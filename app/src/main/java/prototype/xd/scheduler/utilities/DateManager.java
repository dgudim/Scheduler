package prototype.xd.scheduler.utilities;

import static prototype.xd.scheduler.utilities.Keys.DAY_FLAG_GLOBAL;
import static prototype.xd.scheduler.utilities.Keys.DAY_FLAG_GLOBAL_STR;

import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.time.OffsetDateTime;
import java.time.ZoneOffset;
import java.util.Calendar;
import java.util.Locale;
import java.util.TimeZone;

public class DateManager {
    
    public static final TimeZone timeZone_UTC = TimeZone.getTimeZone("UTC");
    public static final TimeZone timeZone_SYSTEM = TimeZone.getDefault();
    
    public static long currentDay = DAY_FLAG_GLOBAL;
    public static long currentTimestamp = DAY_FLAG_GLOBAL;
    public static long currentlySelectedDay = DAY_FLAG_GLOBAL;
    
    static final DateFormat dateFormat = new SimpleDateFormat("MM/dd HH:mm", Locale.ROOT);
    
    public static final String[] availableDays = new String[]{"monday", "tuesday", "wednesday", "thursday", "friday", "saturday", "sunday", "default"};
    
    public static void updateDate(String selectedDate_string, boolean updateCurrentlySelected) {
        long selectedDay = daysFromDate(selectedDate_string);
        currentTimestamp = getCurrentTimestamp();
        currentDay = daysFromEpoch(currentTimestamp);
        if (updateCurrentlySelected) {
            if (selectedDay == DAY_FLAG_GLOBAL) {
                currentlySelectedDay = currentDay;
            } else {
                currentlySelectedDay = selectedDay;
            }
        }
    }
    
    public static boolean isDayTime() {
        return Calendar.getInstance().get(Calendar.HOUR_OF_DAY) >= 5;
    }
    
    public static long daysFromEpoch(long epoch) {
        final Calendar cal = Calendar.getInstance();
        cal.setTimeInMillis(epoch);
        return cal.getTime().toInstant().getEpochSecond() / 86400;
    }
    
    public static String getTimeSpan(long time1, long time2) {
        String date1 = datetimeFromEpoch(time1);
        String date2 = datetimeFromEpoch(time2);
        String[] date1_split = date1.split(" ");
        String[] date2_split = date2.split(" ");
        if (date1.equals(date2) || date1_split[0].equals(date2_split[0])) {
            return date1_split[1] + " - " + date2_split[1];
        } else {
            String[] date1_split_split = date1_split[0].split("/");
            String[] date2_split_split = date2_split[0].split("/");
            boolean month_same = date1_split_split[0].equals(date2_split_split[0]);
            if (month_same) {
                return date1_split_split[1] + " " + date1_split[1] + " - " + date2_split_split[1] + " " + date2_split[1];
            } else {
                return date1 + " - " + date2;
            }
        }
    }
    
    public static long dateToEpoch(int year, int month, int dayOfMonth) {
        Calendar calendar = Calendar.getInstance();
        calendar.set(year, month, dayOfMonth);
        return calendar.getTimeInMillis();
    }
    
    public static String datetimeFromEpoch(long epoch) {
        final Calendar cal = Calendar.getInstance();
        cal.setTimeInMillis(epoch);
        return dateFormat.format(cal.getTime());
    }
    
    public static String getCurrentTime() {
        final Calendar cal = Calendar.getInstance();
        return dateFormat.format(cal.getTime()).split(" ")[1];
    }
    
    public static long daysFromDate(String date) {
        if (date.equals(DAY_FLAG_GLOBAL_STR)) {
            return DAY_FLAG_GLOBAL;
        }
        String[] dateParts_current = date.split("_");
        int year = Integer.parseInt(dateParts_current[0]);
        int month = Integer.parseInt(dateParts_current[1]);
        int day = Integer.parseInt(dateParts_current[2]);
        return OffsetDateTime.of(year, month, day, 0, 0, 0, 0, ZoneOffset.UTC)
                .toInstant().getEpochSecond() / 86400;
    }
    
    public static long addTimeZoneOffset(long epoch) {
        return epoch + timeZone_SYSTEM.getOffset(epoch);
    }
    
    private static long getCurrentTimestamp() {
        return addTimeZoneOffset(Calendar.getInstance().getTime().getTime());
    }
    
}
