package prototype.xd.scheduler.utilities;

import static prototype.xd.scheduler.utilities.Keys.DAY_FLAG_GLOBAL;
import static prototype.xd.scheduler.utilities.PreferencesStore.preferences;
import static prototype.xd.scheduler.utilities.Utilities.getFile;

import java.io.File;
import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.time.LocalDate;
import java.util.Calendar;
import java.util.Date;
import java.util.Locale;
import java.util.TimeZone;
import java.util.concurrent.TimeUnit;

public class DateManager {
    
    public static final TimeZone timeZone_SYSTEM = TimeZone.getDefault();
    
    public static final LocalDate currentDate = LocalDate.now();
    
    public static long currentDay = DAY_FLAG_GLOBAL;
    public static long currentTimestamp = DAY_FLAG_GLOBAL;
    public static long currentlySelectedDay = DAY_FLAG_GLOBAL;
    
    static final DateFormat dateFormat = new SimpleDateFormat("MM/dd HH:mm", Locale.ROOT);
    
    public static final String[] availableDays = new String[]{"monday", "tuesday", "wednesday", "thursday", "friday", "saturday", "sunday", "default"};
    public static final String defaultBackgroundName = "default.png";
    
    public static void updateDate(long day, boolean updateCurrentlySelected) {
        currentTimestamp = getCurrentTimestamp();
        currentDay = daysFromEpoch(currentTimestamp, timeZone_SYSTEM);
        if (updateCurrentlySelected) {
            if (day == DAY_FLAG_GLOBAL) {
                currentlySelectedDay = currentDay;
            } else {
                currentlySelectedDay = day;
            }
        }
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
    
    static File getBackgroundAccordingToDayAndTime() {
        
        if (!preferences.getBoolean(Keys.ADAPTIVE_BACKGROUND_ENABLED, Keys.SETTINGS_DEFAULT_ADAPTIVE_BACKGROUND_ENABLED)) {
            return getFile(defaultBackgroundName);
        }
        
        final Calendar cal = Calendar.getInstance();
        int day = cal.get(Calendar.DAY_OF_WEEK);
        CharSequence day_string;
        if (day == 1) {
            day_string = availableDays[6];
        } else {
            day_string = availableDays[day - 2];
        }
        
        return getFile(day_string + ".png");
    }
    
    public static long daysFromEpoch(long epoch, TimeZone timezone) {
        return TimeUnit.DAYS.convert(addTimeZoneOffset(epoch, timezone), TimeUnit.MILLISECONDS);
    }
    
    public static String datetimeFromEpoch(long epoch) {
        return dateFormat.format(new Date(epoch));
    }
    
    public static String dateFromEpoch(long epoch) {
        return dateFormat.format(new Date(epoch)).split(" ")[0];
    }
    
    public static String getCurrentTime() {
        return dateFormat.format(new Date()).split(" ")[1];
    }
    
    public static String getCurrentDateTime() {
        return dateFormat.format(new Date());
    }
    
    public static long addTimeZoneOffset(long epoch, TimeZone timeZone) {
        return epoch + timeZone.getOffset(epoch);
    }
    
    public static long getCurrentTimestamp() {
        return System.currentTimeMillis();
    }
    
}
