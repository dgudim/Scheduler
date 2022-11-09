package prototype.xd.scheduler.utilities;

import static android.util.Log.ASSERT;
import static android.util.Log.DEBUG;
import static android.util.Log.ERROR;
import static android.util.Log.INFO;
import static android.util.Log.VERBOSE;
import static android.util.Log.WARN;
import static prototype.xd.scheduler.utilities.DateManager.getCurrentDateTimeString;
import static prototype.xd.scheduler.utilities.Keys.ROOT_DIR;
import static prototype.xd.scheduler.utilities.PreferencesStore.preferences;
import static prototype.xd.scheduler.utilities.Utilities.throwOnFalse;

import android.util.Log;

import java.io.BufferedWriter;
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.nio.file.Files;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.atomic.AtomicReference;

public class Logger {
    
    private static final String NAME = "Logger";
    
    private static File logFile;
    private static File logFileOld;
    private static BufferedWriter bufferedWriter;
    
    private static BlockingQueue<String> logQueue;
    
    static AtomicReference<Boolean> fileEnabled = new AtomicReference<>(true);
    static AtomicReference<Boolean> init = new AtomicReference<>(false);
    
    private static final long ROTATE_SIZE = 10*1024*1024L; // 10MB
    
    private Logger() {
        throw new IllegalStateException("Utility logger class");
    }
    
    private static final Thread logWorker = new Thread("Log Worker") {
        
        @Override
        public void run() {
            Log.i(NAME, "Logger is up and running");
            
            while (true) {
                
                try {
                    // if the queue is empty, flush to disk.
                    if (logQueue.isEmpty()) {
                        bufferedWriter.flush();
                    }
                    
                    // get the next element; block if the queue is empty, store the data
                    store(logQueue.take());
                } catch (IOException e) {
                    e.printStackTrace();
                } catch (InterruptedException e) {
                    // propagate
                    interrupt();
                }
                
                if (isInterrupted()) {
                    try {
                        bufferedWriter.close();
                    } catch (IOException e) {
                        e.printStackTrace();
                    }
                    Log.i(NAME, "Logger shut down");
                    break;
                }
            }
        }
    };
    
    @SuppressWarnings("ResultOfMethodCallIgnored")
    public static void tryInit() {
        if (Boolean.FALSE.equals(init.get())) {
            init.set(true);
            logFile = new File(preferences.getString(ROOT_DIR, ""), "log.txt");
            logFileOld = new File(preferences.getString(ROOT_DIR, ""), "log.old.txt");
            try {
                logFile.createNewFile();
                bufferedWriter = new BufferedWriter(new FileWriter(logFile, true));
                logWorker.start();
            } catch (IOException e) {
                fileEnabled.set(false);
                e.printStackTrace();
                Log.e(NAME, "Error initializing logger, proceeding without file support");
            }
            logQueue = new LinkedBlockingQueue<>();
        }
    }
    
    public static void log(int priority, String tag, String message) {
        tryInit();
        Log.println(priority, tag, message);
        if (Boolean.TRUE.equals(fileEnabled.get())) {
            logQueue.add("\n" + (getCurrentDateTimeString() + "  [" + priorityToStr(priority) + "]: " + message));
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
    
    private static synchronized void store(String what) {
        try {
            bufferedWriter.write(what);
            if (logFile.length() > ROTATE_SIZE) {
                Files.delete(logFileOld.toPath());
                throwOnFalse(logFile.renameTo(logFileOld), "Error renaming to old file", IOException.class);
                throwOnFalse(logFile.createNewFile(), "Error creating new file", IOException.class);
                log(INFO, NAME, "moved to log_old");
            }
        } catch (IOException e) {
            fileEnabled.set(false);
            logWorker.interrupt();
            e.printStackTrace();
        }
    }
}
