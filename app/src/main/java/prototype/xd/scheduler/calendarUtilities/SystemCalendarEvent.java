package prototype.xd.scheduler.calendarUtilities;

import static android.provider.CalendarContract.Events;
import static prototype.xd.scheduler.calendarUtilities.SystemCalendarUtils.calendarEventsColumns;
import static prototype.xd.scheduler.utilities.QueryUtilities.getInt;
import static prototype.xd.scheduler.utilities.QueryUtilities.getLong;
import static prototype.xd.scheduler.utilities.QueryUtilities.getString;

import android.database.Cursor;

public class SystemCalendarEvent {
    
    final SystemCalendar associatedCalendar;
    
    final long id;
    final String title;
    final String description;
    final String location;
    final int color;
    final long start;
    final long end;
    
    SystemCalendarEvent(Cursor cursor, SystemCalendar associatedCalendar){
        this.associatedCalendar = associatedCalendar;
        
        id = getLong(cursor, calendarEventsColumns, Events._ID);
        title = getString(cursor, calendarEventsColumns, Events.TITLE);
        description = getString(cursor, calendarEventsColumns, Events.DESCRIPTION);
        location = getString(cursor, calendarEventsColumns, Events.EVENT_LOCATION);
        color = getInt(cursor, calendarEventsColumns, Events.DISPLAY_COLOR);
        start = getLong(cursor, calendarEventsColumns, Events.DTSTART);
        end = getLong(cursor, calendarEventsColumns, Events.DTEND);
    }
}
