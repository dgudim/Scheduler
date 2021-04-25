package prototype.xd.scheduler.utilities;

import android.content.SharedPreferences;

import java.io.File;
import java.util.Calendar;

import static prototype.xd.scheduler.utilities.DateManager.availableDays;
import static prototype.xd.scheduler.utilities.Utilities.rootDir;

public class BackgroundChooser {

    static File getBackgroundAccordingToDayAndTime(SharedPreferences preferences) {

        File defaultName = new File(rootDir, "bg.png");

        if (!preferences.getBoolean("adaptiveBackgroundEnabled", true)) {
            return defaultName;
        }

        final Calendar cal = Calendar.getInstance();
        int day = cal.get(Calendar.DAY_OF_WEEK);
        CharSequence day_string;
        if(day == 1){
            day_string = availableDays[6];
        }else{
            day_string = availableDays[day - 2];
        }


        File dayName = new File(rootDir, day_string + ".png");

        if (!defaultName.exists()) {
            return defaultName;
        } else {
            return dayName;
        }
    }

}
