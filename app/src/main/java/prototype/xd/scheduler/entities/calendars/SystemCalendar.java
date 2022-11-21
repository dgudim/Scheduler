package prototype.xd.scheduler.entities.calendars;

import static android.provider.CalendarContract.Calendars;
import static android.util.Log.WARN;
import static prototype.xd.scheduler.utilities.DateManager.timeZone_SYSTEM;
import static prototype.xd.scheduler.utilities.Logger.log;
import static prototype.xd.scheduler.utilities.PreferencesStore.preferences;
import static prototype.xd.scheduler.utilities.QueryUtilities.getInt;
import static prototype.xd.scheduler.utilities.QueryUtilities.getLong;
import static prototype.xd.scheduler.utilities.QueryUtilities.getString;
import static prototype.xd.scheduler.utilities.QueryUtilities.query;
import static prototype.xd.scheduler.utilities.SystemCalendarUtils.calendarColumns;
import static prototype.xd.scheduler.utilities.SystemCalendarUtils.calendarEventsColumns;
import static prototype.xd.scheduler.utilities.SystemCalendarUtils.makeKey;

import android.content.ContentResolver;
import android.database.Cursor;
import android.provider.CalendarContract.Events;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.collection.ArrayMap;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.TimeZone;

import prototype.xd.scheduler.entities.TodoListEntry;
import prototype.xd.scheduler.entities.TodoListEntryList;
import prototype.xd.scheduler.utilities.Keys;

public class SystemCalendar {
    
    public final String account_type;
    public final String account_name;
    
    public final String name;
    
    public final TimeZone timeZone;
    
    public final long id;
    
    public final int accessLevel;
    
    public final int color;
    
    public final List<SystemCalendarEvent> systemCalendarEvents;
    public final ArrayMap<Integer, Integer> eventColorCountMap;
    
    public SystemCalendar(Cursor cursor, ContentResolver contentResolver, boolean loadMinimal) {
        account_type = getString(cursor, calendarColumns, Calendars.ACCOUNT_TYPE);
        account_name = getString(cursor, calendarColumns, Calendars.ACCOUNT_NAME);
        name = getString(cursor, calendarColumns, Calendars.CALENDAR_DISPLAY_NAME);
        id = getLong(cursor, calendarColumns, Calendars._ID);
        accessLevel = getInt(cursor, calendarColumns, Calendars.CALENDAR_ACCESS_LEVEL);
        color = getInt(cursor, calendarColumns, Calendars.CALENDAR_COLOR);
        
        String timeZoneId = getString(cursor, calendarColumns, Calendars.CALENDAR_TIME_ZONE);
        
        timeZone = TimeZone.getTimeZone(timeZoneId.isEmpty() ? timeZone_SYSTEM.getID() : timeZoneId);
        
        systemCalendarEvents = new ArrayList<>();
        eventColorCountMap = new ArrayMap<>();
        
        //System.out.println("CALENDAR-------------------------------------------------" + name);
        //Cursor cursor_all = query(contentResolver, Events.CONTENT_EXCEPTION_URI, null,
        //        Events.CALENDAR_ID + " = " + id);
        //printTable(cursor_all);
        //cursor_all.close();
        
        loadCalendarEvents(contentResolver, loadMinimal);
        if (accessLevel >= Calendars.CAL_ACCESS_CONTRIBUTOR) {
            loadAvailableEventColors();
        }
    }
    
    
    void loadAvailableEventColors() {
        eventColorCountMap.clear();
        
        for (SystemCalendarEvent event: systemCalendarEvents) {
            eventColorCountMap.computeIfAbsent(event.color, key -> getEventCountWithColor(event.color));
        }
    }
    
    private int getEventCountWithColor(int color) {
        int count = 0;
        for (SystemCalendarEvent event: systemCalendarEvents) {
            if (event.color == color) {
                count++;
            }
        }
        return count;
    }
    
    void loadCalendarEvents(ContentResolver contentResolver, boolean loadMinimal) {
        systemCalendarEvents.clear();
        Cursor cursor = query(contentResolver, Events.CONTENT_URI, calendarEventsColumns.toArray(new String[0]),
                Events.CALENDAR_ID + " = " + id + " AND " + Events.DELETED + " = 0");
        int eventCount = cursor.getCount();
        cursor.moveToFirst();
        
        Map<Long, List<Long>> exceptionLists = new HashMap<>();
        
        for (int i = 0; i < eventCount; i++) {
            
            long originalInstanceTime = getLong(cursor, calendarEventsColumns, Events.ORIGINAL_INSTANCE_TIME);
            if (originalInstanceTime != 0) {
                // if original id is set this event is an exception to some other event
                exceptionLists.computeIfAbsent(getLong(cursor, calendarEventsColumns, Events.ORIGINAL_ID), k -> new ArrayList<>())
                        .add(originalInstanceTime);
            } else {
                systemCalendarEvents.add(new SystemCalendarEvent(cursor, this, loadMinimal));
            }
            
            cursor.moveToNext();
        }
        
        for (Map.Entry<Long, List<Long>> exceptionList : exceptionLists.entrySet()) {
            boolean applied = false;
            for (SystemCalendarEvent event : systemCalendarEvents) {
                if (event.id == exceptionList.getKey()) {
                    event.addExceptions(exceptionList.getValue().toArray(new Long[0]));
                    applied = true;
                    break;
                }
            }
            if (!applied) {
                log(WARN, "System calendar", "Couldn't find calendar event to apply exceptions to, dangling id: " + exceptionList.getKey());
            }
        }
        
        cursor.close();
    }
    
    public TodoListEntryList getVisibleTodoListEntries(long dayStart, long dayEnd) {
        TodoListEntryList todoListEntries = new TodoListEntryList();
        if (preferences.getBoolean(makeKey(this) + "_" + Keys.VISIBLE, Keys.CALENDAR_SETTINGS_DEFAULT_VISIBLE)) {
            for (SystemCalendarEvent event : systemCalendarEvents) {
                if (event.fallsInRange(dayStart, dayEnd)) {
                    todoListEntries.add(new TodoListEntry(event));
                }
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
            SystemCalendar systemCalendar = (SystemCalendar) obj;
            return Objects.equals(account_type, systemCalendar.account_type) &&
                    Objects.equals(account_name, systemCalendar.account_name) &&
                    Objects.equals(name, systemCalendar.name) &&
                    Objects.equals(id, systemCalendar.id) &&
                    Objects.equals(accessLevel, systemCalendar.accessLevel) &&
                    Objects.equals(color, systemCalendar.color);
        }
        return super.equals(obj);
    }
}
