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

import org.dmfs.rfc5545.recurrenceset.RecurrenceSetIterator;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.TimeZone;

import prototype.xd.scheduler.entities.TodoListEntry;
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
    public final List<Integer> availableEventColors;
    public final List<Integer> eventCountsForColors;
    
    public SystemCalendar(Cursor cursor, ContentResolver contentResolver, boolean loadMinimal) {
        account_type = getString(cursor, calendarColumns, Calendars.ACCOUNT_TYPE);
        account_name = getString(cursor, calendarColumns, Calendars.ACCOUNT_NAME);
        name = getString(cursor, calendarColumns, Calendars.CALENDAR_DISPLAY_NAME);
        id = getLong(cursor, calendarColumns, Calendars._ID);
        accessLevel = getInt(cursor, calendarColumns, Calendars.CALENDAR_ACCESS_LEVEL);
        color = getInt(cursor, calendarColumns, Calendars.CALENDAR_COLOR);
        
        String timeZoneId = getString(cursor, calendarColumns, Calendars.CALENDAR_TIME_ZONE);
        
        timeZone = TimeZone.getTimeZone(timeZoneId == null ? timeZone_SYSTEM.getID() : timeZoneId);
        
        systemCalendarEvents = new ArrayList<>();
        availableEventColors = new ArrayList<>();
        eventCountsForColors = new ArrayList<>();
        
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
        availableEventColors.clear();
        eventCountsForColors.clear();
        
        for (int i = 0; i < systemCalendarEvents.size(); i++) {
            int eventColor = systemCalendarEvents.get(i).color;
            if (!availableEventColors.contains(eventColor)) {
                availableEventColors.add(eventColor);
                eventCountsForColors.add(getEventCountWithColor(eventColor));
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
            
            long originalId = getLong(cursor, calendarEventsColumns, Events.ORIGINAL_ID);
            if (originalId != 0) {
                // if original id is set this event is an exception to some other event
                // TODO: find and add exception to appropriate event
                System.out.println(getLong(cursor, calendarEventsColumns, Events.ORIGINAL_INSTANCE_TIME));
            } else {
                systemCalendarEvents.add(new SystemCalendarEvent(cursor, this, loadMinimal));
            }
            
            cursor.moveToNext();
        }
        cursor.close();
        dropDuplicates();
    }
    
    public List<TodoListEntry> getVisibleTodoListEntries(long dayStart, long dayEnd) {
        List<TodoListEntry> todoListEntries = new ArrayList<>();
        if (preferences.getBoolean(makeKey(this) + "_" + Keys.VISIBLE, Keys.CALENDAR_SETTINGS_DEFAULT_VISIBLE)) {
            for (SystemCalendarEvent event : systemCalendarEvents) {
                if (event.fallsInRange(dayStart, dayEnd)) {
                    todoListEntries.add(new TodoListEntry(event));
                }
            }
        }
        return todoListEntries;
    }
    
    public void dropDuplicates() {
        List<SystemCalendarEvent> filteredSystemCalendarEvents = new ArrayList<>();
        
        for (int i = 0; i < systemCalendarEvents.size(); i++) {
            SystemCalendarEvent recurrentEvent = systemCalendarEvents.get(i);
            if (recurrentEvent.rSet != null) {
                
                for (int i2 = 0; i2 < systemCalendarEvents.size(); i2++) {
                    SystemCalendarEvent staticEvent = systemCalendarEvents.get(i2);
                    if (staticEvent.title.equals(recurrentEvent.title) && staticEvent.rSet == null && !staticEvent.invalidFlag) {
                        
                        RecurrenceSetIterator it = recurrentEvent.rSet.iterator(recurrentEvent.timeZone, recurrentEvent.start);
                        long instance = 0;
                        while (it.hasNext() && instance <= staticEvent.start) {
                            instance = it.next();
                            if (instance == staticEvent.start && instance + recurrentEvent.duration == staticEvent.start + staticEvent.duration) {
                                staticEvent.invalidFlag = true;
                                log(WARN, "SystemCalendar", "Overlapping duplicate events: " + staticEvent.title + ", dropping");
                                break;
                            }
                        }
                        
                    }
                }
                
            }
        }
        
        int filtered = 0;
        
        for (SystemCalendarEvent event : systemCalendarEvents) {
            if (!event.invalidFlag) {
                filteredSystemCalendarEvents.add(event);
            } else {
                filtered++;
            }
        }
        if (filtered > 0) {
            log(WARN, "SystemCalendar", "Calendar " + name + " is unstable, consider deleting duplicate events, you have " + filtered + " duplicates");
        }
        systemCalendarEvents.clear();
        systemCalendarEvents.addAll(filteredSystemCalendarEvents);
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
