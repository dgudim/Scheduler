package prototype.xd.scheduler.utilities;

import static java.lang.Math.max;
import static prototype.xd.scheduler.utilities.DateManager.getCurrentDateTime;
import static prototype.xd.scheduler.utilities.Logger.ContentType.ERROR;
import static prototype.xd.scheduler.utilities.Utilities.getRootDir;

import android.content.Context;

import java.io.File;
import java.io.IOException;
import java.io.PrintStream;
import java.io.PrintWriter;
import java.io.StringWriter;
import java.nio.file.Files;

@SuppressWarnings("ResultOfMethodCallIgnored")
public class Logger {
    
    public enum ContentType {INFO, WARNING, ERROR}
    
    private static File logFile;
    
    private static final int MAX_SIZE = 30_000;
    
    public static void initLogger(Context context) {
        if (logFile == null) {
            logFile = new File(getRootDir(context), "log.txt");
        }
    }
    
    public static void log(ContentType contentType, String message) {
        PrintStream stream = System.out;
        if (contentType == ERROR) {
            stream = System.err;
        }
        String msg = getCurrentDateTime() + "  [" + contentType + "]: " + message;
        stream.println(msg);
        try {
            logFile.createNewFile();
            String before = new String(Files.readAllBytes(logFile.toPath()));
            int maxIndex = before.length();
            before = before.substring(max(maxIndex - MAX_SIZE, 0), maxIndex);
            PrintWriter out = new PrintWriter(logFile);
            out.print(before + "\n" + msg);
            out.close();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }
    
    public static void logException(Exception e) {
        StringWriter sw = new StringWriter();
        e.printStackTrace(new PrintWriter(sw));
        String fullStackTrace = sw.toString();
        log(ERROR, fullStackTrace);
    }
    
}
