package prototype.xd.scheduler.utilities;

import java.io.File;
import java.io.PrintWriter;
import java.io.StringWriter;
import java.nio.file.Files;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;

import static prototype.xd.scheduler.utilities.Utilities.rootDir;

public class Logger {

    private static final DateTimeFormatter dtf = DateTimeFormatter.ofPattern("y.M.d  h:m");
    private static final File logFile = new File(rootDir, "log.txt");

    public static final String INFO = "  [INFO]: ";
    public static final String WARNING = "  [WARNING]: ";
    public static final String ERROR = "  [ERROR]: ";


    public static void log(String contentType, String message) {
        try {
            if (!logFile.exists()) {
                logFile.createNewFile();
            }
            String before = new String(Files.readAllBytes(logFile.toPath()));
            PrintWriter out = new PrintWriter(logFile);
            out.println(before + dtf.format(LocalDateTime.now()) + contentType + message);
            out.close();
        } catch (Exception e) {
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
