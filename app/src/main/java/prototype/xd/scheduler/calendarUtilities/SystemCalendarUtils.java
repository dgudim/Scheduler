package prototype.xd.scheduler.calendarUtilities;

import static prototype.xd.scheduler.QueryUtilities.query;

import android.content.ContentResolver;
import android.database.Cursor;
import android.provider.CalendarContract;

import java.util.ArrayList;
import java.util.Arrays;

public class SystemCalendarUtils {
    
    public static final ArrayList<String> calendarColumns =  new ArrayList<>(Arrays.asList(
            CalendarContract.Calendars.CALENDAR_DISPLAY_NAME,
            CalendarContract.Calendars.ACCOUNT_NAME,
            CalendarContract.Calendars.ACCOUNT_TYPE,
            CalendarContract.Calendars._ID,
            CalendarContract.Calendars.CALENDAR_ACCESS_LEVEL,
            CalendarContract.Calendars.CALENDAR_COLOR));
    
    public static final ArrayList<String> calendarEventsColumns = new ArrayList<>(Arrays.asList(
            CalendarContract.Events.MUTATORS,
            CalendarContract.Events.TITLE,
            CalendarContract.Events.DESCRIPTION,
            CalendarContract.Events.EVENT_LOCATION,
            CalendarContract.Events.DISPLAY_COLOR,
            CalendarContract.Events.DTSTART,
            CalendarContract.Events.DTEND));
    
    public static ArrayList<SystemCalendar> getAllCalendars(ContentResolver contentResolver){
        ArrayList<SystemCalendar> systemCalendars = new ArrayList<>();
        Cursor cursor = query(contentResolver, CalendarContract.Calendars.CONTENT_URI, calendarColumns.toArray(new String[0]), null);
        int calendars = cursor.getCount();
        cursor.moveToFirst();
        for(int i = 0; i < calendars; i++){
            systemCalendars.add(new SystemCalendar(cursor));
            cursor.moveToNext();
        }
        cursor.close();
        return systemCalendars;
    }
    
}
