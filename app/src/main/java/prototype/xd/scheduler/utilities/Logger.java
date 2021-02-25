package prototype.xd.scheduler.utilities;

import java.io.File;
import java.io.PrintWriter;
import java.io.StringWriter;
import java.nio.file.Files;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;

import static prototype.xd.scheduler.utilities.Utilities.rootDir;

public class Logger {

    private static DateTimeFormatter dtf = DateTimeFormatter.ofPattern("y/M/d/h/m");
    private static File logFile = new File(rootDir, "log.txt");

    public static void log(String message) {
        try {
            String before = new String(Files.readAllBytes(logFile.toPath()));
            PrintWriter out = new PrintWriter(logFile);
            out.println(before + dtf.format(LocalDateTime.now()) + " || " + message);
            out.close();
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    public static void logException(Exception e) {
        StringWriter sw = new StringWriter();
        e.printStackTrace(new PrintWriter(sw));
        String fullStackTrace = sw.toString();
        log(fullStackTrace);
    }

}
