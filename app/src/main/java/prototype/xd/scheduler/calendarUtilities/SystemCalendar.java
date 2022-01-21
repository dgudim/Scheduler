package prototype.xd.scheduler.calendarUtilities;

import static prototype.xd.scheduler.QueryUtilities.getInt;
import static prototype.xd.scheduler.QueryUtilities.getLong;
import static prototype.xd.scheduler.QueryUtilities.getString;
import static prototype.xd.scheduler.QueryUtilities.query;
import static prototype.xd.scheduler.calendarUtilities.SystemCalendarUtils.calendarColumns;
import static prototype.xd.scheduler.calendarUtilities.SystemCalendarUtils.calendarEventsColumns;
import static prototype.xd.scheduler.calendarUtilities.SystemCalendarUtils.colorColumns;

import android.content.ContentResolver;
import android.database.Cursor;
import android.provider.CalendarContract;

import androidx.annotation.NonNull;

import java.util.ArrayList;

public class SystemCalendar {
    
    public final String account_type;
    public final String account_name;
    
    public final String name;
    
    public final long id;
    
    public final int accessLevel;
    
    public final int color;
    
    public ArrayList<SystemCalendarEvent> systemCalendarEvents;
    public ArrayList<Integer> availableEventColors;
    
    SystemCalendar(Cursor cursor, ContentResolver contentResolver) {
        account_type = getString(cursor, calendarColumns, CalendarContract.Calendars.ACCOUNT_TYPE);
        account_name = getString(cursor, calendarColumns, CalendarContract.Calendars.ACCOUNT_NAME);
        name = getString(cursor, calendarColumns, CalendarContract.Calendars.CALENDAR_DISPLAY_NAME);
        id = getLong(cursor, calendarColumns, CalendarContract.Calendars._ID);
        accessLevel = getInt(cursor, calendarColumns, CalendarContract.Calendars.CALENDAR_ACCESS_LEVEL);
        color = getInt(cursor, calendarColumns, CalendarContract.Calendars.CALENDAR_COLOR);
        
        systemCalendarEvents = new ArrayList<>();
        availableEventColors = new ArrayList<>();
        
        if (accessLevel >= CalendarContract.Calendars.CAL_ACCESS_CONTRIBUTOR) {
            loadAvailableEventColors(contentResolver);
        }
    }
    
    void loadAvailableEventColors(ContentResolver contentResolver) {
        availableEventColors.clear();
        Cursor cursor = query(contentResolver, CalendarContract.Colors.CONTENT_URI, colorColumns.toArray(new String[0]),
                CalendarContract.Colors.ACCOUNT_NAME + "=?", new String[]{account_name});
        
        int colors = cursor.getCount();
        
        cursor.moveToFirst();
        for (int i = 0; i < colors; i++) {
            int color = getInt(cursor, colorColumns, CalendarContract.Colors.COLOR);
            if (!availableEventColors.contains(color)) {
                availableEventColors.add(color);
            }
            cursor.moveToNext();
        }
        cursor.close();
    }
    
    void loadCalendarEvents(ContentResolver contentResolver) {
        systemCalendarEvents.clear();
        Cursor cursor = query(contentResolver, CalendarContract.Events.CONTENT_URI, calendarEventsColumns.toArray(new String[0]),
                CalendarContract.Events.CALENDAR_ID + " = " + id);
        int events = cursor.getCount();
        cursor.moveToFirst();
        for (int i = 0; i < events; i++) {
            systemCalendarEvents.add(new SystemCalendarEvent(cursor));
            cursor.moveToNext();
        }
        cursor.close();
    }
    
    @NonNull
    @Override
    public String toString() {
        return account_name + ": " + name;
    }
}
