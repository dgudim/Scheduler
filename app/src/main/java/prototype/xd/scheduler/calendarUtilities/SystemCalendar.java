package prototype.xd.scheduler.calendarUtilities;

import static prototype.xd.scheduler.QueryUtilities.getInt;
import static prototype.xd.scheduler.QueryUtilities.getLong;
import static prototype.xd.scheduler.QueryUtilities.getString;
import static prototype.xd.scheduler.QueryUtilities.query;
import static prototype.xd.scheduler.calendarUtilities.SystemCalendarUtils.calendarColumns;
import static prototype.xd.scheduler.calendarUtilities.SystemCalendarUtils.calendarEventsColumns;

import android.content.ContentResolver;
import android.database.Cursor;
import android.provider.CalendarContract;

import java.util.ArrayList;

public class SystemCalendar {
    
    String account_type;
    String account_name;
    
    String name;
    
    long id;
    
    int accessLevel;
    
    ArrayList<SystemCalendarEvent> systemCalendarEvents;
    
    SystemCalendar(Cursor cursor) {
        account_type = getString(cursor, calendarColumns, CalendarContract.Calendars.ACCOUNT_TYPE);
        account_name = getString(cursor, calendarColumns, CalendarContract.Calendars.ACCOUNT_NAME);
        name = getString(cursor, calendarColumns, CalendarContract.Calendars.CALENDAR_DISPLAY_NAME);
        id = getLong(cursor, calendarColumns, CalendarContract.Calendars._ID);
        accessLevel = getInt(cursor, calendarColumns, CalendarContract.Calendars.CALENDAR_ACCESS_LEVEL);
    }
    
    void loadCalendarEvents(ContentResolver contentResolver) {
        systemCalendarEvents = new ArrayList<>();
        Cursor cursor = query(contentResolver, CalendarContract.Events.CONTENT_URI, (String[]) calendarEventsColumns.toArray(),
                CalendarContract.Events.CALENDAR_ID + " = " + id);
        int events = cursor.getCount();
        for(int i = 0; i < events; i++){
            systemCalendarEvents.add(new SystemCalendarEvent(cursor));
            cursor.moveToNext();
        }
        cursor.close();
    }
    
}
