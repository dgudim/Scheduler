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
    
    private DateManager() {
        throw new IllegalStateException("Utility date manager class");
    }
    
    public static final TimeZone timeZone_SYSTEM = TimeZone.getDefault();
    
    public static final LocalDate currentDate = LocalDate.now();
    
    public static long currentDay = DAY_FLAG_GLOBAL;
    public static long currentTimestamp = DAY_FLAG_GLOBAL;
    public static long currentlySelectedDay = DAY_FLAG_GLOBAL;
    
    private static final DateFormat dateFormat = new SimpleDateFormat("MM/dd HH:mm", Locale.ROOT);
    
    public static final String[] AVAILABLE_DAYS = new String[]{"monday", "tuesday", "wednesday", "thursday", "friday", "saturday", "sunday", "default"};
    public static final String DEFAULT_BACKGROUND_NAME = "default.png";
    
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
    
    public static String getTimeSpan(long timeFrom, long timeTo) {
        String dateFrom = datetimeFromEpoch(timeFrom);
        String dateTo = datetimeFromEpoch(timeTo);
        String[] dateFromSplit = dateFrom.split(" ");
        String[] dateToSplit = dateTo.split(" ");
        if (dateFrom.equals(dateTo) || dateFromSplit[0].equals(dateToSplit[0])) {
            return dateFromSplit[1] + " - " + dateToSplit[1];
        } else {
            String[] dateFromMonthDay = dateFromSplit[0].split("/");
            String[] dateToMonthDay = dateToSplit[0].split("/");
            //month is the same
            if (dateFromMonthDay[0].equals(dateToMonthDay[0])) {
                return dateFromMonthDay[1] + " " + dateFromSplit[1] + " - " + dateToMonthDay[1] + " " + dateToSplit[1];
            } else {
                return dateFrom + " - " + dateTo;
            }
        }
    }
    
    static File getBackgroundAccordingToDayAndTime() {
        
        if (!preferences.getBoolean(Keys.ADAPTIVE_BACKGROUND_ENABLED, Keys.SETTINGS_DEFAULT_ADAPTIVE_BACKGROUND_ENABLED)) {
            return getFile(DEFAULT_BACKGROUND_NAME);
        }
        
        int day = Calendar.getInstance().get(Calendar.DAY_OF_WEEK);
        CharSequence dayString;
        if (day == 1) {
            dayString = AVAILABLE_DAYS[6];
        } else {
            dayString = AVAILABLE_DAYS[day - 2];
        }
        
        return getFile(dayString + ".png");
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
