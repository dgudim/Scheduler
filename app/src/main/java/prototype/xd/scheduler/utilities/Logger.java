package prototype.xd.scheduler.utilities;

import static prototype.xd.scheduler.utilities.Logger.ContentType.ERROR;

import java.io.PrintStream;
import java.io.PrintWriter;
import java.io.StringWriter;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;

public class Logger {

    private static final DateTimeFormatter dtf = DateTimeFormatter.ofPattern("y.M.d  h:m");
    
    public enum ContentType {INFO, WARNING, ERROR}
   
    public static void log(ContentType contentType, String message) {
        PrintStream stream = System.out;
        if(contentType == ERROR){
            stream = System.err;
        }
        stream.println(dtf.format(LocalDateTime.now()) + "  [" + contentType + "]: " + message);
    }

    public static void logException(Exception e) {
        StringWriter sw = new StringWriter();
        e.printStackTrace(new PrintWriter(sw));
        String fullStackTrace = sw.toString();
        log(ERROR, fullStackTrace);
    }

}
