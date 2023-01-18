package prototype.xd.scheduler.entities;

import static android.provider.CalendarContract.Calendars;
import static java.lang.Math.max;
import static prototype.xd.scheduler.utilities.QueryUtilities.getInt;
import static prototype.xd.scheduler.utilities.QueryUtilities.getLong;
import static prototype.xd.scheduler.utilities.QueryUtilities.getString;
import static prototype.xd.scheduler.utilities.QueryUtilities.query;
import static prototype.xd.scheduler.utilities.SystemCalendarUtils.CALENDAR_COLUMNS;
import static prototype.xd.scheduler.utilities.SystemCalendarUtils.CALENDAR_EVENT_COLUMNS;

import android.content.ContentResolver;
import android.database.Cursor;
import android.provider.CalendarContract.Events;

import androidx.annotation.ColorInt;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.collection.ArrayMap;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.function.Consumer;
import java.util.function.Function;
import java.util.function.Predicate;

import prototype.xd.scheduler.BuildConfig;
import prototype.xd.scheduler.utilities.Keys;
import prototype.xd.scheduler.utilities.Logger;

/**
 * Class for storing system calendars and their events
 */
public class SystemCalendar {
    
    public static final String NAME = SystemCalendar.class.getSimpleName();
    
    @NonNull
    public final String accountType;
    @NonNull
    public final String accountName;
    
    @NonNull
    private final String prefKey;
    @NonNull
    private final String visibilityKey;
    public final long id;
    public final int accessLevel;
    
    @NonNull
    public final String timeZoneId;
    @NonNull
    public final String displayName;
    @ColorInt
    public final int color;
    
    @NonNull
    public final List<SystemCalendarEvent> systemCalendarEvents;
    @NonNull
    public final ArrayMap<Integer, Integer> eventColorCountMap;
    
    public SystemCalendar(@NonNull Cursor cursor, @NonNull ContentResolver contentResolver, boolean loadMinimal) {
        accountType = getString(cursor, CALENDAR_COLUMNS, Calendars.ACCOUNT_TYPE);
        accountName = getString(cursor, CALENDAR_COLUMNS, Calendars.ACCOUNT_NAME);
        displayName = getString(cursor, CALENDAR_COLUMNS, Calendars.CALENDAR_DISPLAY_NAME);
        id = getLong(cursor, CALENDAR_COLUMNS, Calendars._ID);
        accessLevel = getInt(cursor, CALENDAR_COLUMNS, Calendars.CALENDAR_ACCESS_LEVEL);
        color = getInt(cursor, CALENDAR_COLUMNS, Calendars.CALENDAR_COLOR);
        
        prefKey = accountName + "_" + displayName;
        visibilityKey = prefKey + "_" + Keys.VISIBLE;
        
        String calTimeZoneId = getString(cursor, CALENDAR_COLUMNS, Calendars.CALENDAR_TIME_ZONE);
        
        if (calTimeZoneId.isEmpty()) {
            Logger.warning(NAME, this + " has no timezone, defaulting to UTC");
            timeZoneId = "UTC";
        } else {
            timeZoneId = calTimeZoneId;
        }
        
        //System.out.println("CALENDAR-------------------------------------------------" + name);
        //Cursor cursor_all = query(contentResolver, Events.CONTENT_EXCEPTION_URI, null,
        //        Events.CALENDAR_ID + " = " + id);
        //printTable(cursor_all);
        //cursor_all.close();
        
        systemCalendarEvents = loadCalendarEvents(contentResolver, loadMinimal);
        eventColorCountMap = loadAvailableEventColors();
    }
    
    // default capacity is fine
    @SuppressWarnings("CollectionWithoutInitialCapacity")
    private ArrayMap<Integer, Integer> loadAvailableEventColors() {
        ArrayMap<Integer, Integer> colors = new ArrayMap<>();
        if (accessLevel >= Calendars.CAL_ACCESS_CONTRIBUTOR) {
            for (SystemCalendarEvent event : systemCalendarEvents) {
                colors.put(event.color, colors.getOrDefault(event.color, 0) + 1);
            }
        } else {
            colors.put(color, systemCalendarEvents.size());
        }
        return colors;
    }
    
    @SuppressWarnings("CollectionWithoutInitialCapacity")
    private List<SystemCalendarEvent> loadCalendarEvents(@NonNull ContentResolver contentResolver, boolean loadMinimal) {
        
        Map<Long, List<Long>> exceptionLists;
        List<SystemCalendarEvent> events;
        
        final int MIN_EVENTS = 10;
        final int MIN_EXCEPTIONS = 16;
        
        try (Cursor cursor = query(contentResolver, Events.CONTENT_URI, CALENDAR_EVENT_COLUMNS.toArray(new String[0]),
                Events.CALENDAR_ID + " = " + id + " AND " + Events.DELETED + " = 0")) {
            
            int allEvents = cursor.getCount();
            events = new ArrayList<>(max(allEvents - MIN_EXCEPTIONS, MIN_EVENTS));
            exceptionLists = new HashMap<>(MIN_EXCEPTIONS);
            
            while (cursor.moveToNext()) {
                long originalInstanceTime = getLong(cursor, CALENDAR_EVENT_COLUMNS, Events.ORIGINAL_INSTANCE_TIME);
                if (originalInstanceTime == 0) {
                    events.add(new SystemCalendarEvent(cursor, this, loadMinimal));
                } else {
                    // if original id is set this event is an exception to some other event
                    if (!loadMinimal) {
                        exceptionLists.computeIfAbsent(getLong(cursor, CALENDAR_EVENT_COLUMNS, Events.ORIGINAL_ID),
                                        e -> new ArrayList<>())
                                .add(originalInstanceTime);
                    }
                }
            }
        }
        
        if (!loadMinimal) {
            addExceptions(exceptionLists, events);
        }
        
        return events;
    }
    
    /**
     * Add exceptions to recurrence rules
     *
     * @param exceptionsMap map of event ids to exception days
     */
    void addExceptions(@NonNull final Map<Long, List<Long>> exceptionsMap, @NonNull final List<SystemCalendarEvent> events) {
        for (Map.Entry<Long, List<Long>> exceptionList : exceptionsMap.entrySet()) {
            boolean applied = false;
            for (SystemCalendarEvent event : events) {
                if (event.id == exceptionList.getKey()) {
                    event.addExceptions(exceptionList.getValue());
                    applied = true;
                    break;
                }
            }
            if (!applied) {
                Logger.warning(accountName, "Couldn't find calendar event to apply exceptions to, dangling id: " + exceptionList.getKey());
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
     * Get visible events between first and last days, add them to the list
     *
     * @param firstDayUTC        start of range
     * @param lastDayUTC         end of range
     * @param list               list to add converted events to
     * @param conversionFunction function to convert to necessary type
     */
    public <T> void addVisibleEventsToList(long firstDayUTC, long lastDayUTC,
                                           @NonNull List<T> list,
                                           @NonNull Function<SystemCalendarEvent, T> conversionFunction) {
        if (isVisible()) {
            for (SystemCalendarEvent event : systemCalendarEvents) {
                if (event.isVisibleOnRange(firstDayUTC, lastDayUTC)) {
                    list.add(conversionFunction.apply(event));
                }
            }
        }
    }
    
    /**
     * Get visible events between first and last days, push them to the consumer
     *
     * @param firstDayUTC start of range
     * @param lastDayUTC  end of range
     * @param filter      function to filter out some events
     * @param consumer    consumer that processes all events
     */
    public void getVisibleEvents(long firstDayUTC, long lastDayUTC,
                                 @NonNull Predicate<SystemCalendarEvent> filter,
                                 @NonNull Consumer<SystemCalendarEvent> consumer) {
        if (isVisible()) {
            for (SystemCalendarEvent event : systemCalendarEvents) {
                if (filter.test(event) && event.isVisibleOnRange(firstDayUTC, lastDayUTC)) {
                    consumer.accept(event);
                }
            }
        }
    }
    
    /**
     * Notifies all events with a specific color (in the same group) about parameter changes
     *
     * @param parameterKey parameter to invalidate
     * @param color        target events color
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
     *
     * @param color target events color
     */
    public void invalidateAllParametersOnEvents(int color) {
        systemCalendarEvents.forEach(event -> {
            if (event.color == color) {
                event.invalidateAllParameters();
            }
        });
    }
    
    public void unlinkAllTodoEntries() {
        systemCalendarEvents.forEach(event -> {
            if (event.associatedEntry != null) {
                event.associatedEntry.removeFromContainer();
            }
        });
    }
    
    @NonNull
    @Override
    public String toString() {
        return NAME + ": " + (BuildConfig.DEBUG ? prefKey : id);
    }
    
    @Override
    public int hashCode() {
        return Objects.hashCode(id);
    }
    
    @Override
    public boolean equals(@Nullable Object obj) {
        if (obj == null) {
            return false;
        } else if (obj == this) {
            return true;
        } else if (obj instanceof SystemCalendar) {
            // id is unique
            return id == ((SystemCalendar) obj).id;
        }
        return false;
    }
    
    @NonNull
    public String getPrefKey() {
        return prefKey;
    }
    
    @NonNull
    public String makePrefKey(int possibleEventColor) {
        return prefKey + "_" + possibleEventColor;
    }
}
