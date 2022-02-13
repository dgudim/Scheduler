package prototype.xd.scheduler.utilities;

import static java.lang.Math.max;
import static prototype.xd.scheduler.utilities.Logger.ContentType.ERROR;
import static prototype.xd.scheduler.utilities.Utilities.rootDir;

import java.io.File;
import java.io.IOException;
import java.io.PrintStream;
import java.io.PrintWriter;
import java.io.StringWriter;
import java.nio.file.Files;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;

@SuppressWarnings("ResultOfMethodCallIgnored")
public class Logger {
    
    private static final DateTimeFormatter dtf = DateTimeFormatter.ofPattern("yyyy.MM.dd  hh:mm");
    
    public enum ContentType {INFO, WARNING, ERROR}
    
    private static final File logFile = new File(rootDir, "log.txt");
    
    private static final int MAX_SIZE = 30_000;
    
    public static void log(ContentType contentType, String message) {
        PrintStream stream = System.out;
        if (contentType == ERROR) {
            stream = System.err;
        }
        String msg = dtf.format(LocalDateTime.now()) + "  [" + contentType + "]: " + message;
        stream.println(msg);
        try {
            logFile.createNewFile();
            String before = new String(Files.readAllBytes(logFile.toPath()));
            int maxIndex = before.length() - 1;
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
