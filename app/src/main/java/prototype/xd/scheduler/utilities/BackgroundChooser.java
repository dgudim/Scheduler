package prototype.xd.scheduler.utilities;

import android.content.SharedPreferences;

import java.io.File;
import java.util.Calendar;

import static prototype.xd.scheduler.utilities.Utilities.rootDir;

public class BackgroundChooser {

    static File getBackgroundAccordingToDayAndTime(SharedPreferences preferences) {

        File defaultName = new File(rootDir, "bg.png");

        if (!preferences.getBoolean("adaptiveBackgroundEnabled", true)) {
            return defaultName;
        }

        final Calendar cal = Calendar.getInstance();
        int day = cal.get(Calendar.DAY_OF_WEEK);
        String day_string = "bg";
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

        File dayName = new File(rootDir, day_string + ".png");

        if (!defaultName.exists()) {
            return defaultName;
        } else {
            return dayName;
        }
    }

}
