package prototype.xd.scheduler.entities;

import static android.provider.CalendarContract.Calendars;
import static android.util.Log.WARN;
import static prototype.xd.scheduler.utilities.Logger.log;
import static prototype.xd.scheduler.utilities.QueryUtilities.getInt;
import static prototype.xd.scheduler.utilities.QueryUtilities.getLong;
import static prototype.xd.scheduler.utilities.QueryUtilities.getString;
import static prototype.xd.scheduler.utilities.QueryUtilities.query;
import static prototype.xd.scheduler.utilities.SystemCalendarUtils.calendarColumns;
import static prototype.xd.scheduler.utilities.SystemCalendarUtils.calendarEventsColumns;

import android.content.ContentResolver;
import android.database.Cursor;
import android.provider.CalendarContract.Events;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.collection.ArrayMap;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;

import prototype.xd.scheduler.utilities.Keys;

public class SystemCalendar {
    
    private static final String NAME = "SystemCalendar";
    
    public final String account_type;
    public final String account_name;
    
    private final String prefKey;
    private final String visibilityKey;
    public final long id;
    public final int accessLevel;
    
    public final String timeZoneId;
    public final String displayName;
    public final int color;
    
    public final List<SystemCalendarEvent> systemCalendarEvents;
    public final ArrayMap<Integer, Integer> eventColorCountMap;
    
    public SystemCalendar(Cursor cursor, ContentResolver contentResolver, boolean loadMinimal) {
        account_type = getString(cursor, calendarColumns, Calendars.ACCOUNT_TYPE);
        account_name = getString(cursor, calendarColumns, Calendars.ACCOUNT_NAME);
        displayName = getString(cursor, calendarColumns, Calendars.CALENDAR_DISPLAY_NAME);
        id = getLong(cursor, calendarColumns, Calendars._ID);
        accessLevel = getInt(cursor, calendarColumns, Calendars.CALENDAR_ACCESS_LEVEL);
        color = getInt(cursor, calendarColumns, Calendars.CALENDAR_COLOR);
        
        prefKey = account_name + "_" + displayName;
        visibilityKey = prefKey + "_" + Keys.VISIBLE;
        
        String calTimeZoneId = getString(cursor, calendarColumns, Calendars.CALENDAR_TIME_ZONE);
        
        if(calTimeZoneId.isEmpty()) {
            log(WARN, NAME, "Calendar " + prefKey + " has no timezone, defaulting to UTC");
            this.timeZoneId = "UTC";
        } else {
            this.timeZoneId = calTimeZoneId;
        }
        
        systemCalendarEvents = new ArrayList<>();
        eventColorCountMap = new ArrayMap<>();
        
        //System.out.println("CALENDAR-------------------------------------------------" + name);
        //Cursor cursor_all = query(contentResolver, Events.CONTENT_EXCEPTION_URI, null,
        //        Events.CALENDAR_ID + " = " + id);
        //printTable(cursor_all);
        //cursor_all.close();
        
        loadCalendarEvents(contentResolver, loadMinimal);
        loadAvailableEventColors();
    }
    
    
    void loadAvailableEventColors() {
        eventColorCountMap.clear();
        if(accessLevel >= Calendars.CAL_ACCESS_CONTRIBUTOR) {
            for (SystemCalendarEvent event : systemCalendarEvents) {
                eventColorCountMap.computeIfAbsent(event.color, key -> getEventCountWithColor(event.color));
            }
        } else {
            eventColorCountMap.put(color, systemCalendarEvents.size());
        }
        
    }
    
    private int getEventCountWithColor(int color) {
        int count = 0;
        for (SystemCalendarEvent event : systemCalendarEvents) {
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
                if(!loadMinimal) {
                    exceptionLists.computeIfAbsent(getLong(cursor, calendarEventsColumns, Events.ORIGINAL_ID), k -> new ArrayList<>())
                            .add(originalInstanceTime);
                }
            } else {
                systemCalendarEvents.add(new SystemCalendarEvent(cursor, this, loadMinimal));
            }
            
            cursor.moveToNext();
        }
        
        if (!loadMinimal) {
            addExceptions(exceptionLists);
        }
        
        cursor.close();
    }
    
    void addExceptions(final Map<Long, List<Long>> exceptionLists) {
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
                log(WARN, account_name, "Couldn't find calendar event to apply exceptions to, dangling id: " + exceptionList.getKey());
            }
        }
    }
    
    public boolean isVisible() {
        return Keys.getBoolean(visibilityKey, Keys.CALENDAR_SETTINGS_DEFAULT_VISIBLE);
    }
    
    public List<SystemCalendarEvent> getVisibleTodoListEvents(long firstDayUTC, long lastDayUTC) {
        if(isVisible()) {
            List<SystemCalendarEvent> visibleEvents = new ArrayList<>();
            for (SystemCalendarEvent event : systemCalendarEvents) {
                if (event.visibleOnRange(firstDayUTC, lastDayUTC)) {
                    visibleEvents.add(event);
                }
            }
            return visibleEvents;
        }
        return Collections.emptyList();
    }
    
    protected void invalidateParameterOnEvents(String parameterKey, int color) {
        systemCalendarEvents.forEach(event -> {
            if (event.color == color) {
                event.invalidateParameter(parameterKey);
            }
        });
    }
    
    public void invalidateAllParametersOnEvents(int color) {
        systemCalendarEvents.forEach(event -> {
            if (event.color == color) {
                event.invalidateAllParameters();
            }
        });
    }
    
    public void unlinkAllTodoListEntries() {
        systemCalendarEvents.forEach(event -> {
            if (event.associatedEntry != null) {
                event.associatedEntry.removeFromContainer();
            }
        });
    }
    
    @NonNull
    @Override
    public String toString() {
        return account_name + ": " + displayName;
    }
    
    @Override
    public int hashCode() {
        return Objects.hash(account_type, account_name, displayName, id, accessLevel, color);
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
                    Objects.equals(displayName, systemCalendar.displayName) &&
                    Objects.equals(id, systemCalendar.id) &&
                    Objects.equals(accessLevel, systemCalendar.accessLevel) &&
                    Objects.equals(color, systemCalendar.color);
        }
        return super.equals(obj);
    }
    
    public String getKey() {
        return prefKey;
    }
    
    public String makeKey(int possibleEventColor) {
        return prefKey + "_" + possibleEventColor;
    }
}
