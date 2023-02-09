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
    public final SystemCalendarData data;
    
    @NonNull
    public final String prefKey;
    public final List<String> subKeys;
    public final String visibilityKey;
    
    @NonNull
    public final List<SystemCalendarEvent> systemCalendarEvents;
    @NonNull
    public final ArrayMap<Integer, Integer> eventColorCountMap;
    
    public SystemCalendar(@NonNull SystemCalendarData data) {
        
        this.data = data;
        
        prefKey = data.accountName + KEY_SEPARATOR + data.displayName;
        subKeys = List.of(data.accountName, prefKey);
        visibilityKey = prefKey + KEY_SEPARATOR + Static.VISIBLE;
        
        //System.out.println("CALENDAR-------------------------------------------------" + name);
        //Cursor cursor_all = query(contentResolver, Events.CONTENT_EXCEPTION_URI, null,
        //        Events.CALENDAR_ID + " = " + id);
        //printTable(cursor_all);
        //cursor_all.close();
        
        systemCalendarEvents = data.makeEvents();
        eventColorCountMap = loadAvailableEventColors();
    }
    
    // default capacity is fine
    @SuppressWarnings("CollectionWithoutInitialCapacity")
    private ArrayMap<Integer, Integer> loadAvailableEventColors() {
        ArrayMap<Integer, Integer> colors = new ArrayMap<>();
        if (data.accessLevel >= Calendars.CAL_ACCESS_CONTRIBUTOR) {
            for (SystemCalendarEvent event : systemCalendarEvents) {
                colors.put(event.data.color, colors.getOrDefault(event.data.color, 0) + 1);
            }
        } else {
            colors.put(data.color, systemCalendarEvents.size());
        }
        return colors;
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
            if (event.data.color != color) {
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
        return data.toString();
    }
    
    @Override
    public int hashCode() {
        return data.hashCode();
    }
    
    @Override
    public boolean equals(@Nullable Object o) {
        if (this == o) {
            return true;
        }
        if (o == null || getClass() != o.getClass()) {
            return false;
        }
        return data.equals(((SystemCalendar) o).data);
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
