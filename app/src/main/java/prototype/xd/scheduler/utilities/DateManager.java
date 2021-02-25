package prototype.xd.scheduler.utilities;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;

public class DateManager {

    public static String currentDate = "none";
    public static String currentlySelectedDate = "none";
    public static String yesterdayDate = "none";
    private static DateTimeFormatter dtf = DateTimeFormatter.ofPattern("yyyy/M/d");

    public static void updateDate(String selectedDate, boolean updateCurrentlySelected) {
        currentDate = dtf.format(LocalDateTime.now()).replace("/", "_");
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
        String day = currentDate.substring(currentDate.length() - 1);
        String otherPart = currentDate.substring(0, currentDate.length() - 1);
        if (Integer.parseInt(day) > 0) {
            return otherPart + (Integer.parseInt(day) - 1);
        } else return "";
    }

}
