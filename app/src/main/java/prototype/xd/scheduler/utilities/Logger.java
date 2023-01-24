package prototype.xd.scheduler.utilities;

import static android.util.Log.ASSERT;
import static android.util.Log.DEBUG;
import static android.util.Log.ERROR;
import static android.util.Log.INFO;
import static android.util.Log.VERBOSE;
import static android.util.Log.WARN;
import static prototype.xd.scheduler.utilities.DateManager.getCurrentDateTimeStringLocal;
import static prototype.xd.scheduler.utilities.Keys.LOG_FILE;
import static prototype.xd.scheduler.utilities.Keys.LOG_FILE_OLD;
import static prototype.xd.scheduler.utilities.Keys.ROOT_DIR;
import static prototype.xd.scheduler.utilities.Utilities.throwOnFalse;

import android.util.Log;

import androidx.annotation.NonNull;

import org.jetbrains.annotations.NonNls;

import java.io.BufferedWriter;
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.nio.file.Files;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.LinkedBlockingQueue;

@SuppressWarnings("FieldAccessedSynchronizedAndUnsynchronized")
@NonNls
public final class Logger {
    
    public static final String NAME = Logger.class.getSimpleName();
    
    private static File logFile;
    private static File logFileOld;
    private static BufferedWriter bufferedWriter;
    
    private static BlockingQueue<String> logQueue;
    
    private static volatile boolean fileEnabled = true;
    private static volatile boolean init;
    private static volatile boolean debugEnabled;
    
    private static final long ROTATE_SIZE = 10 * 1024 * 1024L; // 10MB
    
    private Logger() throws InstantiationException {
        throw new InstantiationException(NAME);
    }
    
    @SuppressWarnings("ResultOfMethodCallIgnored")
    private static synchronized void init() {
        if (!init) {
            init = true;
            logFile = new File(ROOT_DIR.get(), LOG_FILE);
            logFileOld = new File(ROOT_DIR.get(), LOG_FILE_OLD);
            logQueue = new LinkedBlockingQueue<>();
            try {
                logFile.createNewFile(); // NOSONAR, we don't need to know if the file existed or not
                bufferedWriter = new BufferedWriter(new FileWriter(logFile, true)); // NOSONAR, using system encoding is fine
                logWorker.setDaemon(true);
                logWorker.start();
            } catch (IOException e) {
                fileEnabled = false;
                e.printStackTrace();
                Log.e(NAME, "Error initializing logger, proceeding without file support: (" + e + ")");
            }
        }
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
        init();
        Log.println(priority, tag, message);
        if (fileEnabled) {
            logQueue.add("\n" + (getCurrentDateTimeStringLocal() + "  [" + priorityToStr(priority) + "]: " + message));
        }
    }
    
    public static void logException(@NonNull String tag, @NonNull Exception e) {
        log(ERROR, tag, Log.getStackTraceString(e));
    }
    
    @NonNull
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
                info(NAME, "Moved to log_old");
            }
        } catch (IOException e) { // NOSONAR
            fileEnabled = false;
            logWorker.interrupt();
            e.printStackTrace();
        }
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
                } catch (IOException e) { // NOSONAR
                    e.printStackTrace();
                } catch (InterruptedException e) {
                    // propagate
                    interrupt();
                }
                
                if (isInterrupted()) {
                    try {
                        bufferedWriter.close();
                    } catch (IOException e) { // NOSONAR
                        e.printStackTrace();
                    }
                    Log.i(NAME, "Logger shut down");
                    break;
                }
            }
        }
    };
}
