package prototype.xd.scheduler.utilities;

import static android.util.Log.ASSERT;
import static android.util.Log.DEBUG;
import static android.util.Log.ERROR;
import static android.util.Log.INFO;
import static android.util.Log.VERBOSE;
import static android.util.Log.WARN;
import static prototype.xd.scheduler.utilities.DateManager.getCurrentDateTime;
import static prototype.xd.scheduler.utilities.Keys.ROOT_DIR;
import static prototype.xd.scheduler.utilities.PreferencesStore.preferences;
import static prototype.xd.scheduler.utilities.Utilities.throwOnFalse;

import android.util.Log;

import java.io.BufferedWriter;
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.nio.file.Files;

public class Logger {
    
    private static File logFile;
    private static File logFileOld;
    
    private Logger() {
        throw new IllegalStateException("Utility logger class");
    }
    
    private static void checkDirs() {
        if (logFile == null) {
            logFile = new File(preferences.getString(ROOT_DIR, ""), "log.txt");
            logFileOld = new File(preferences.getString(ROOT_DIR, ""), "log.old.txt");
        }
    }
    
    public static void log(int priority, String tag, String message) {
        checkDirs();
        Log.println(priority, tag, message);
        try {
            logFile.createNewFile();
            try (BufferedWriter out = new BufferedWriter(new FileWriter(logFile, true))) {
                out.write("\n" + (getCurrentDateTime() + "  [" + priorityToStr(priority) + "]: " + message));
            }
            if (logFile.length() > 100_000) {
                Files.delete(logFileOld.toPath());
                throwOnFalse(logFile.renameTo(logFileOld), "Error renaming to old file", IOException.class);
                log(INFO, "Logger", "moved to log_old");
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
    }
    
    public static void logException(String tag, Exception e) {
        log(ERROR, tag, Log.getStackTraceString(e));
    }
    
    private static String priorityToStr(int priority) {
        switch (priority) {
            case VERBOSE:
                return "VERBOSE";
            case DEBUG:
                return "DEBUG";
            case INFO:
                return "INFO";
            case WARN:
                return "WARNING";
            case ERROR:
                return "ERROR";
            case ASSERT:
            default:
                return "UNKNOWN";
        }
    }
    
}
