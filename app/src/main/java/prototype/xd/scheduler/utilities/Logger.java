package prototype.xd.scheduler.utilities;

import static android.util.Log.DEBUG;
import static android.util.Log.ERROR;
import static android.util.Log.INFO;
import static android.util.Log.WARN;

import static prototype.xd.scheduler.utilities.Keys.LOGCAT_FILE;
import static prototype.xd.scheduler.utilities.Utilities.displayToast;
import static prototype.xd.scheduler.utilities.Utilities.getFile;
import static prototype.xd.scheduler.utilities.Utilities.shareFiles;

import android.content.ClipDescription;
import android.content.Context;
import android.util.Log;

import androidx.annotation.NonNull;

import org.jetbrains.annotations.NonNls;

import java.io.File;
import java.io.IOException;
import java.util.List;

import prototype.xd.scheduler.R;

@SuppressWarnings("FieldAccessedSynchronizedAndUnsynchronized")
@NonNls
public final class Logger {
    
    public static final String NAME = Logger.class.getSimpleName();
    private static volatile boolean debugEnabled;
    
    
    private Logger() throws InstantiationException {
        throw new InstantiationException(NAME);
    }
    
    public static void setDebugEnabled(boolean debugEnabled) {
        Logger.debugEnabled = debugEnabled;
    }
    
    public static void error(@NonNull String tag, @NonNull String message) {
        log(ERROR, tag, message);
    }
    
    public static void warning(@NonNull String tag, @NonNull String message) {
        log(WARN, tag, message);
    }
    
    public static void info(@NonNull String tag, @NonNull String message) {
        log(INFO, tag, message);
    }
    
    public static void debug(@NonNull String tag, @NonNull String message) {
        if (debugEnabled) {
            log(DEBUG, tag, message);
        }
    }
    
    private static void log(int priority, String tag, String message) {
        Log.println(priority, tag, message);
    }
    
    public static void logException(@NonNull String tag, @NonNull Exception e) {
        log(ERROR, tag, Log.getStackTraceString(e));
    }
    
    public static void shareLog(@NonNull Context context) {
        File logcatFile = getFile(LOGCAT_FILE);
        try {
            Runtime.getRuntime().exec("logcat -f " + logcatFile.getAbsolutePath());
            shareFiles(context, ClipDescription.MIMETYPE_TEXT_PLAIN, List.of(logcatFile));
        } catch (IOException e) {
            displayToast(context, R.string.logcat_obtain_fail);
            logException(NAME, e);
        }
    }
}
