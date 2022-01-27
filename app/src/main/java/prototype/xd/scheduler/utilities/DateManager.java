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
    
    public static long currentDay = DAY_FLAG_GLOBAL;
    public static long currentlySelectedDay = DAY_FLAG_GLOBAL;
    
    static final DateFormat dateFormat = new SimpleDateFormat("MM/dd HH:mm", Locale.ROOT);
    
    public static final String[] availableDays = new String[]{"понедельник", "вторник", "среда", "четверг", "пятница", "суббота", "воскресенье", "общий"};
    
    public static void updateDate(String selectedDate_string, boolean updateCurrentlySelected) {
        long selectedDay = daysFromDate(selectedDate_string);
        currentDay = getCurrentDay();
        if (updateCurrentlySelected) {
            if (selectedDay == DAY_FLAG_GLOBAL) {
                currentlySelectedDay = currentDay;
            } else {
                currentlySelectedDay = selectedDay;
            }
        }
    }
    
    public static boolean isDayTime() {
        int timeOfDay = Calendar.getInstance().get(Calendar.HOUR_OF_DAY);
        return timeOfDay >= 6 && timeOfDay <= 22;
    }
    
    public static long daysFromEpoch(long epoch) {
        return (long) Math.ceil(epoch / (double) 86_400_000);
    }
    
    public static String getTimeSpan(long time1, long time2) {
        String date1 = dateFromEpoch(time1);
        String date2 = dateFromEpoch(time2);
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
    
    public static String dateFromEpoch(long epoch) {
        final Calendar cal = Calendar.getInstance();
        cal.setTimeInMillis(epoch);
        return dateFormat.format(cal.getTime());
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
    
    private static long getCurrentDay() {
        return daysFromEpoch(Calendar.getInstance().getTime().getTime());
    }
    
}
