package prototype.xd.scheduler.entities;

import static android.provider.CalendarContract.Calendars;
import static prototype.xd.scheduler.utilities.QueryUtilities.getInt;
import static prototype.xd.scheduler.utilities.QueryUtilities.getLong;
import static prototype.xd.scheduler.utilities.QueryUtilities.getString;
import static prototype.xd.scheduler.utilities.Static.KEY_SEPARATOR;
import static prototype.xd.scheduler.utilities.SystemCalendarUtils.CALENDAR_COLUMNS;

import android.database.Cursor;

import androidx.annotation.ColorInt;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.collection.ArrayMap;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.function.Consumer;
import java.util.function.Predicate;

import prototype.xd.scheduler.BuildConfig;
import prototype.xd.scheduler.utilities.Logger;
import prototype.xd.scheduler.utilities.Static;

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
    protected final String prefKey;
    @NonNull
    protected final List<String> subKeys;
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
    
    public SystemCalendar(@NonNull Cursor cursor,
                          @NonNull List<SystemCalendarEvent> events,
                          @NonNull Map<Long, List<Long>> exceptionLists) {
        accountType = getString(cursor, CALENDAR_COLUMNS, Calendars.ACCOUNT_TYPE);
        accountName = getString(cursor, CALENDAR_COLUMNS, Calendars.ACCOUNT_NAME);
        displayName = getString(cursor, CALENDAR_COLUMNS, Calendars.CALENDAR_DISPLAY_NAME);
        id = getLong(cursor, CALENDAR_COLUMNS, Calendars._ID);
        accessLevel = getInt(cursor, CALENDAR_COLUMNS, Calendars.CALENDAR_ACCESS_LEVEL);
        color = getInt(cursor, CALENDAR_COLUMNS, Calendars.CALENDAR_COLOR);
        
        prefKey = accountName + KEY_SEPARATOR + displayName;
        subKeys = List.of(accountName, prefKey);
        visibilityKey = prefKey + KEY_SEPARATOR + Static.VISIBLE;
        
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
        
        systemCalendarEvents = events;
        addExceptions(exceptionLists);
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
    
    /**
     * Add exceptions to recurrence rules
     *
     * @param exceptionsMap map of event ids to exception days
     */
    void addExceptions(@NonNull final Map<Long, List<Long>> exceptionsMap) {
        for (Map.Entry<Long, List<Long>> exceptionList : exceptionsMap.entrySet()) {
            boolean applied = false;
            for (SystemCalendarEvent event : systemCalendarEvents) {
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
        return Static.getBoolean(visibilityKey, Static.CALENDAR_SETTINGS_DEFAULT_VISIBLE);
    }
    
    /**
     * Get visible events between first and last days, add them to the list
     *
     * @param firstDayUTC start of range
     * @param lastDayUTC  end of range
     * @param list        list to add todoEntries to
     */
    public void addVisibleEventsToList(long firstDayUTC, long lastDayUTC,
                                       @NonNull List<TodoEntry> list) {
        getVisibleEvents(
                firstDayUTC, lastDayUTC,
                null,
                event -> list.add(new TodoEntry(event)));
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
                                 @Nullable Predicate<SystemCalendarEvent> filter,
                                 @NonNull Consumer<SystemCalendarEvent> consumer) {
        if (isVisible()) {
            for (SystemCalendarEvent event : systemCalendarEvents) {
                if ((filter != null && filter.test(event)) &&
                        event.isVisibleOnRange(firstDayUTC, lastDayUTC)) {
                    consumer.accept(event);
                }
            }
        }
    }
    
    /**
     * Notifies all events with a specific color (in the same group) about parameter changes
     *
     * @param parameterKey parameter to invalidate, null means invalidate all parameters
     * @param color        target events color
     */
    protected void invalidateParameterOnEvents(@Nullable String parameterKey, int color) {
        systemCalendarEvents.forEach(event -> {
            if (event.color != color) {
                return;
            }
            if (parameterKey == null) {
                event.invalidateAllParameters();
            } else {
                event.invalidateParameter(parameterKey);
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
    public String getPrefKey() {
        return prefKey;
    }
    
    @NonNull
    public String getVisibilityKey() {
        return visibilityKey;
    }
    
    @NonNull
    public List<String> getSubKeys() {
        return subKeys;
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
    public String makeEventPrefKey(int possibleEventColor) {
        return prefKey + "_" + possibleEventColor;
    }
    
    @NonNull
    public List<String> makeEventSubKeys(@NonNull String eventPrefKey) {
        List<String> eventSubKeys = new ArrayList<>(subKeys);
        eventSubKeys.add(eventPrefKey);
        return eventSubKeys;
    }
}
