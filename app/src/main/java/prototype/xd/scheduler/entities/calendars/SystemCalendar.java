package prototype.xd.scheduler.entities.calendars;

import static prototype.xd.scheduler.MainActivity.preferences;
import static prototype.xd.scheduler.utilities.QueryUtilities.getInt;
import static prototype.xd.scheduler.utilities.QueryUtilities.getLong;
import static prototype.xd.scheduler.utilities.QueryUtilities.getString;
import static prototype.xd.scheduler.utilities.QueryUtilities.query;
import static prototype.xd.scheduler.utilities.SystemCalendarUtils.calendarColumns;
import static prototype.xd.scheduler.utilities.SystemCalendarUtils.calendarEventsColumns;
import static prototype.xd.scheduler.utilities.SystemCalendarUtils.makeKey;

import android.content.ContentResolver;
import android.database.Cursor;
import android.provider.CalendarContract;
import android.provider.CalendarContract.Events;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import java.util.ArrayList;
import java.util.Objects;

import prototype.xd.scheduler.entities.TodoListEntry;
import prototype.xd.scheduler.utilities.Keys;

public class SystemCalendar {
    
    public final String account_type;
    public final String account_name;
    
    public final String name;
    
    public final long id;
    
    public final int accessLevel;
    
    public final int color;
    
    public final ArrayList<SystemCalendarEvent> systemCalendarEvents;
    public final ArrayList<Integer> availableEventColors;
    public final ArrayList<Integer> eventCountsForColors;
    
    public SystemCalendar(Cursor cursor, ContentResolver contentResolver, boolean loadMinimal) {
        account_type = getString(cursor, calendarColumns, CalendarContract.Calendars.ACCOUNT_TYPE);
        account_name = getString(cursor, calendarColumns, CalendarContract.Calendars.ACCOUNT_NAME);
        name = getString(cursor, calendarColumns, CalendarContract.Calendars.CALENDAR_DISPLAY_NAME);
        id = getLong(cursor, calendarColumns, CalendarContract.Calendars._ID);
        accessLevel = getInt(cursor, calendarColumns, CalendarContract.Calendars.CALENDAR_ACCESS_LEVEL);
        color = getInt(cursor, calendarColumns, CalendarContract.Calendars.CALENDAR_COLOR);
        
        systemCalendarEvents = new ArrayList<>();
        availableEventColors = new ArrayList<>();
        eventCountsForColors = new ArrayList<>();
        
        loadCalendarEvents(contentResolver, loadMinimal);
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
    
    void loadCalendarEvents(ContentResolver contentResolver, boolean loadMinimal) {
        systemCalendarEvents.clear();
        Cursor cursor = query(contentResolver, Events.CONTENT_URI, calendarEventsColumns.toArray(new String[0]),
                Events.CALENDAR_ID + " = " + id + " AND " + Events.DELETED + " = 0");
        int events = cursor.getCount();
        cursor.moveToFirst();
        for (int i = 0; i < events; i++) {
            systemCalendarEvents.add(new SystemCalendarEvent(cursor, this, loadMinimal));
            cursor.moveToNext();
        }
        cursor.close();
    }
    
    public ArrayList<TodoListEntry> getVisibleTodoListEntries() {
        ArrayList<TodoListEntry> todoListEntries = new ArrayList<>();
        if (preferences.getBoolean(makeKey(this) + "_" + Keys.VISIBLE, Keys.CALENDAR_SETTINGS_DEFAULT_VISIBLE)) {
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
    
    @Override
    public int hashCode() {
        return Objects.hash(account_type, account_name, name, id, accessLevel, color);
    }
    
    @Override
    public boolean equals(@Nullable Object obj) {
        if (obj == null) {
            return false;
        } else if (obj == this) {
            return true;
        } else if (obj instanceof SystemCalendar) {
            SystemCalendar s_calendar = (SystemCalendar) obj;
            return Objects.equals(account_type, s_calendar.account_type) &&
                    Objects.equals(account_name, s_calendar.account_name) &&
                    Objects.equals(name, s_calendar.name) &&
                    Objects.equals(id, s_calendar.id) &&
                    Objects.equals(accessLevel, s_calendar.accessLevel) &&
                    Objects.equals(color, s_calendar.color);
        }
        return super.equals(obj);
    }
}
