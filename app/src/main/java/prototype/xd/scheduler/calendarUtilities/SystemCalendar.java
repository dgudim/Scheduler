package prototype.xd.scheduler.calendarUtilities;

import static prototype.xd.scheduler.MainActivity.preferences;
import static prototype.xd.scheduler.calendarUtilities.SystemCalendarUtils.calendarColumns;
import static prototype.xd.scheduler.calendarUtilities.SystemCalendarUtils.calendarEventsColumns;
import static prototype.xd.scheduler.utilities.QueryUtilities.getInt;
import static prototype.xd.scheduler.utilities.QueryUtilities.getLong;
import static prototype.xd.scheduler.utilities.QueryUtilities.getString;
import static prototype.xd.scheduler.utilities.QueryUtilities.query;

import android.content.ContentResolver;
import android.database.Cursor;
import android.provider.CalendarContract;
import android.provider.CalendarContract.Events;

import androidx.annotation.NonNull;

import java.util.ArrayList;

import prototype.xd.scheduler.entities.TodoListEntry;
import prototype.xd.scheduler.utilities.Keys;

public class SystemCalendar {
    
    public final String account_type;
    public final String account_name;
    
    public final String name;
    
    public final long id;
    
    public final int accessLevel;
    
    public final int color;
    
    public ArrayList<SystemCalendarEvent> systemCalendarEvents;
    public ArrayList<Integer> availableEventColors;
    public ArrayList<Integer> eventCountsForColors;
    
    SystemCalendar(Cursor cursor, ContentResolver contentResolver) {
        account_type = getString(cursor, calendarColumns, CalendarContract.Calendars.ACCOUNT_TYPE);
        account_name = getString(cursor, calendarColumns, CalendarContract.Calendars.ACCOUNT_NAME);
        name = getString(cursor, calendarColumns, CalendarContract.Calendars.CALENDAR_DISPLAY_NAME);
        id = getLong(cursor, calendarColumns, CalendarContract.Calendars._ID);
        accessLevel = getInt(cursor, calendarColumns, CalendarContract.Calendars.CALENDAR_ACCESS_LEVEL);
        color = getInt(cursor, calendarColumns, CalendarContract.Calendars.CALENDAR_COLOR);
        
        systemCalendarEvents = new ArrayList<>();
        availableEventColors = new ArrayList<>();
        eventCountsForColors = new ArrayList<>();
        
        loadCalendarEvents(contentResolver);
        if (accessLevel >= CalendarContract.Calendars.CAL_ACCESS_CONTRIBUTOR) {
            loadAvailableEventColors();
        }
    }
    
    void loadAvailableEventColors() {
        availableEventColors.clear();
        eventCountsForColors.clear();
        
        for (int i = 0; i < systemCalendarEvents.size(); i++) {
            int color = systemCalendarEvents.get(i).color;
            if (!availableEventColors.contains(color)) {
                availableEventColors.add(color);
                eventCountsForColors.add(getEventCountWithColor(color));
            }
        }
    }
    
    private int getEventCountWithColor(int color) {
        int count = 0;
        for (int i = 0; i < systemCalendarEvents.size(); i++) {
            if (systemCalendarEvents.get(i).color == color) {
                count++;
            }
        }
        return count;
    }
    
    void loadCalendarEvents(ContentResolver contentResolver) {
        systemCalendarEvents.clear();
        Cursor cursor = query(contentResolver, Events.CONTENT_URI, calendarEventsColumns.toArray(new String[0]),
                Events.CALENDAR_ID + " = " + id + " AND " + Events.DELETED + " = 0");
        int events = cursor.getCount();
        cursor.moveToFirst();
        for (int i = 0; i < events; i++) {
            systemCalendarEvents.add(new SystemCalendarEvent(cursor, this));
            cursor.moveToNext();
        }
        cursor.close();
    }
    
    ArrayList<TodoListEntry> getVisibleTodoListEntries() {
        ArrayList <TodoListEntry> todoListEntries = new ArrayList<>();
        if (preferences.getBoolean(account_name + "_" + name + "_" + Keys.VISIBLE, Keys.CALENDAR_SETTINGS_DEFAULT_VISIBLE)) {
            for (int i = 0; i < systemCalendarEvents.size(); i++) {
                todoListEntries.add(new TodoListEntry(systemCalendarEvents.get(i)));
            }
        }
        return todoListEntries;
    }
    
    @NonNull
    @Override
    public String toString() {
        return account_name + ": " + name;
    }
}
