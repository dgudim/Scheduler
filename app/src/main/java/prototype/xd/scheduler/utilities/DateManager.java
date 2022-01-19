package prototype.xd.scheduler.utilities;

import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.time.OffsetDateTime;
import java.time.ZoneOffset;
import java.util.Calendar;
import java.util.Date;
import java.util.Locale;

public class DateManager {
    
    public static String currentDate = "none";
    public static String currentlySelectedDate = "none";
    
    static final DateFormat dateFormat = new SimpleDateFormat("yyyy/M/d", Locale.ROOT);
    public static final CharSequence[] availableDays = new CharSequence[]{"понедельник", "вторник", "среда", "четверг", "пятница", "суббота", "воскресенье", "общий"};
    
    public static void updateDate(String selectedDate, boolean updateCurrentlySelected) {
        currentDate = getCurrentDate();
        if (updateCurrentlySelected) {
            if (selectedDate.equals("none")) {
                currentlySelectedDate = currentDate;
            } else {
                currentlySelectedDate = selectedDate;
            }
        }
    }
    
    public static long daysFromDate(String date) {
        String[] dateParts_current = date.split("_");
        int year = Integer.parseInt(dateParts_current[0]);
        int month = Integer.parseInt(dateParts_current[1]);
        int day = Integer.parseInt(dateParts_current[2]);
        return OffsetDateTime.of(year, month, day, 0, 0, 0, 0, ZoneOffset.UTC)
                .toInstant().getEpochSecond() / 86400;
    }
    
    private static String getCurrentDate() {
        return dateFormat.format(getDate()).replace("/", "_");
    }
    
    private static Date getDate() {
        final Calendar cal = Calendar.getInstance();
        cal.add(Calendar.DATE, 0);
        return cal.getTime();
    }
    
}
