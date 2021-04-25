package prototype.xd.scheduler.utilities;

import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.Date;
import java.util.Locale;

public class DateManager {

    public static String currentDate = "none";
    public static String currentlySelectedDate = "none";
    public static String yesterdayDate = "none";
    static DateFormat dateFormat = new SimpleDateFormat("yyyy/M/d", Locale.ROOT);
    public static final CharSequence[] availableDays = new CharSequence[]{"понедельник", "вторник", "среда", "четверг", "пятница", "суббота", "воскресенье", "общий"};

    public static void updateDate(String selectedDate, boolean updateCurrentlySelected) {
        currentDate = getCurrentDate();
        yesterdayDate = getYesterdayDate();
        if (updateCurrentlySelected) {
            if (selectedDate.equals("none")) {
                currentlySelectedDate = currentDate;
            } else {
                currentlySelectedDate = selectedDate;
            }
        }
    }

    private static String getYesterdayDate() {
        return dateFormat.format(getDate(-1)).replace("/", "_");
    }

    private static String getCurrentDate() {
        return dateFormat.format(getDate(0)).replace("/", "_");
    }

    private static Date getDate(int dayShift) {
        final Calendar cal = Calendar.getInstance();
        cal.add(Calendar.DATE, dayShift);
        return cal.getTime();
    }

}
