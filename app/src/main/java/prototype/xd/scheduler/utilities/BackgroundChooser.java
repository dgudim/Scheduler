package prototype.xd.scheduler.utilities;

import static prototype.xd.scheduler.MainActivity.preferences;
import static prototype.xd.scheduler.utilities.DateManager.availableDays;
import static prototype.xd.scheduler.utilities.Utilities.rootDir;

import java.io.File;
import java.util.Calendar;

public class BackgroundChooser {

    public static final String defaultBackgroundName = "bg.png";

    static File getBackgroundAccordingToDayAndTime() {

        File defaultName = new File(rootDir, defaultBackgroundName);

        if (!preferences.getBoolean(Keys.ADAPTIVE_BACKGROUND_ENABLED, true)) {
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
