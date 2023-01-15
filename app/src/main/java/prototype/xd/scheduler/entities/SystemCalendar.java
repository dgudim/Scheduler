package prototype.xd.scheduler.entities;

import static android.provider.CalendarContract.Calendars;
import static prototype.xd.scheduler.utilities.QueryUtilities.getInt;
import static prototype.xd.scheduler.utilities.QueryUtilities.getLong;
import static prototype.xd.scheduler.utilities.QueryUtilities.getString;
import static prototype.xd.scheduler.utilities.QueryUtilities.query;
import static prototype.xd.scheduler.utilities.SystemCalendarUtils.CALENDAR_COLUMNS;
import static prototype.xd.scheduler.utilities.SystemCalendarUtils.CALENDAR_EVENT_COLUMNS;

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
import prototype.xd.scheduler.utilities.Logger;

/**
 * Class for storing system calendars and their events
 */
public class SystemCalendar {
    
    public static final String NAME = SystemCalendar.class.getSimpleName();
    
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
        account_type = getString(cursor, CALENDAR_COLUMNS, Calendars.ACCOUNT_TYPE);
        account_name = getString(cursor, CALENDAR_COLUMNS, Calendars.ACCOUNT_NAME);
        displayName = getString(cursor, CALENDAR_COLUMNS, Calendars.CALENDAR_DISPLAY_NAME);
        id = getLong(cursor, CALENDAR_COLUMNS, Calendars._ID);
        accessLevel = getInt(cursor, CALENDAR_COLUMNS, Calendars.CALENDAR_ACCESS_LEVEL);
        color = getInt(cursor, CALENDAR_COLUMNS, Calendars.CALENDAR_COLOR);
        
        prefKey = account_name + "_" + displayName;
        visibilityKey = prefKey + "_" + Keys.VISIBLE;
        
        String calTimeZoneId = getString(cursor, CALENDAR_COLUMNS, Calendars.CALENDAR_TIME_ZONE);
        
        if (calTimeZoneId.isEmpty()) {
            Logger.warning(NAME, "Calendar " + prefKey + " has no timezone, defaulting to UTC");
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
        if (accessLevel >= Calendars.CAL_ACCESS_CONTRIBUTOR) {
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
        Cursor cursor = query(contentResolver, Events.CONTENT_URI, CALENDAR_EVENT_COLUMNS.toArray(new String[0]),
                Events.CALENDAR_ID + " = " + id + " AND " + Events.DELETED + " = 0");
        int eventCount = cursor.getCount();
        cursor.moveToFirst();
        
        Map<Long, List<Long>> exceptionLists = new HashMap<>();
        
        for (int i = 0; i < eventCount; i++) {
            
            long originalInstanceTime = getLong(cursor, CALENDAR_EVENT_COLUMNS, Events.ORIGINAL_INSTANCE_TIME);
            if (originalInstanceTime != 0) {
                // if original id is set this event is an exception to some other event
                if (!loadMinimal) {
                    exceptionLists.computeIfAbsent(getLong(cursor, CALENDAR_EVENT_COLUMNS, Events.ORIGINAL_ID), k -> new ArrayList<>())
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
    
    /**
     * Add exceptions to recurrence rules
     *
     * @param exceptionsMap map of event ids to exception days
     */
    void addExceptions(final Map<Long, List<Long>> exceptionsMap) {
        for (Map.Entry<Long, List<Long>> exceptionList : exceptionsMap.entrySet()) {
            boolean applied = false;
            for (SystemCalendarEvent event : systemCalendarEvents) {
                if (event.id == exceptionList.getKey()) {
                    event.addExceptions(exceptionList.getValue().toArray(new Long[0]));
                    applied = true;
                    break;
                }
            }
            if (!applied) {
                Logger.warning(account_name, "Couldn't find calendar event to apply exceptions to, dangling id: " + exceptionList.getKey());
            }
        }
    }
    
    /**
     * @return true if the user selected this calendar to be visible
     */
    public boolean isVisible() {
        return Keys.getBoolean(visibilityKey, Keys.CALENDAR_SETTINGS_DEFAULT_VISIBLE);
    }
    
    /**
     * Get visible events between first and last days
     * @param firstDayUTC start of range
     * @param lastDayUTC end of range
     * @return visible events on range
     */
    public List<SystemCalendarEvent> getVisibleTodoListEvents(long firstDayUTC, long lastDayUTC) {
        if (isVisible()) {
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
    
    /**
     * Notifies all events with a specific color (in the same group) about parameter changes
     * @param parameterKey parameter to invalidate
     * @param color target events color
     */
    protected void invalidateParameterOnEvents(@NonNull String parameterKey, int color) {
        systemCalendarEvents.forEach(event -> {
            if (event.color == color) {
                event.invalidateParameter(parameterKey);
            }
        });
    }
    
    /**
     * Notifies all events with a specific color (in the same group) about all parameter changes
     * @param color target events color
     */
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
