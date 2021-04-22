package prototype.xd.scheduler.utilities;

import java.io.File;
import java.util.Calendar;

import static prototype.xd.scheduler.utilities.Utilities.rootDir;

public class BackgroundChooser {

    static File getBackgroundAccordingToDayAndTime(String defValue) {
        final Calendar cal = Calendar.getInstance();
        int day = cal.get(Calendar.DAY_OF_WEEK);
        int hour = cal.get(Calendar.HOUR_OF_DAY);
        String day_string = "";
        String hour_string = "";
        switch (day) {
            case (1):
                day_string = "воскресенье";
                break;
            case (2):
                day_string = "понедельник";
                break;
            case (3):
                day_string = "вторник";
                break;
            case (4):
                day_string = "среда";
                break;
            case (5):
                day_string = "четверг";
                break;
            case (6):
                day_string = "пятница";
                break;
            case (7):
                day_string = "суббота";
                break;
        }

        if (hour > 7 && hour < 21) {
            hour_string = "день";
        } else {
            hour_string = "ночь";
        }

        if (new File(rootDir, day_string + "_" + hour_string + ".jpg").exists()) {
            return new File(rootDir, day_string + "_" + hour_string + ".jpg");
        }
        return new File(rootDir, defValue);
    }

}
