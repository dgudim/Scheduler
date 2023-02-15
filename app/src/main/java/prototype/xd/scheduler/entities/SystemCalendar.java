package prototype.xd.scheduler.entities;

import static android.provider.CalendarContract.Calendars;
import static prototype.xd.scheduler.utilities.Static.KEY_SEPARATOR;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.collection.ArrayMap;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.function.Consumer;
import java.util.function.Predicate;

import prototype.xd.scheduler.utilities.Logger;
import prototype.xd.scheduler.utilities.Static;
import prototype.xd.scheduler.utilities.Utilities;

/**
 * Class for storing system calendars and their events
 */
public class SystemCalendar {
    
    public static final String NAME = SystemCalendar.class.getSimpleName();
    
    @NonNull
    public SystemCalendarData data;
    
    @NonNull
    public final String prefKey;
    public final List<String> subKeys;
    public final String visibilityKey;
    
    @NonNull
    public final Map<Long, SystemCalendarEvent> systemCalendarEventMap;
    @NonNull
    public final Map<Integer, Integer> eventColorCountMap;
    
    @SuppressWarnings("CollectionWithoutInitialCapacity")
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
        
        systemCalendarEventMap = data.makeEvents(this);
        eventColorCountMap = new ArrayMap<>();
        loadAvailableEventColors();
    }
    
    // default capacity is fine
    protected void loadAvailableEventColors() {
        eventColorCountMap.clear();
        if (data.accessLevel >= Calendars.CAL_ACCESS_CONTRIBUTOR) {
            for (SystemCalendarEvent event : systemCalendarEventMap.values()) {
                eventColorCountMap.merge(event.data.color, 1, Integer::sum); // increment by 1
            }
        } else {
            eventColorCountMap.put(data.color, systemCalendarEventMap.size());
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
            for (SystemCalendarEvent event : systemCalendarEventMap.values()) {
                if ((filter == null || filter.test(event)) &&
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
        systemCalendarEventMap.values().forEach(event -> {
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
        systemCalendarEventMap.values().forEach(SystemCalendarEvent::removeFromContainer);
    }
    
    public SystemCalendarEvent addEvent(@NonNull Long id, @NonNull SystemCalendarEventData data) {
        if (id != data.id) {
            Logger.error(NAME, "Something has gone really wrong, id != data.id (" + id + "/" + data.id + "), " + data);
            return null;
        }SystemCalendarEvent event = new SystemCalendarEvent(data, this);
        systemCalendarEventMap.put(id, event);
        return event;
    }
    
    public SystemCalendarEvent removeEvent(@NonNull Long id) {
        return Objects.requireNonNull(systemCalendarEventMap.remove(id)).removeFromContainer();
    }
    
    public boolean setNewData(@NonNull SystemCalendarData newData) {
        if (data.equals(newData)) {
            return false;
        }
        SystemCalendarData oldData = data;
        
        boolean changed = Utilities.processDifference(oldData.systemCalendarEventsData, newData.systemCalendarEventsData, (entry, elementState) -> {
            SystemCalendarEvent event;
            switch (elementState) {
                case MODIFIED:
                    event = removeEvent(entry.first);
                    addEvent(entry.first, entry.second);
                    Logger.info(NAME,  "Changed " + event + " in " + this);
                    break;
                case NEW:
                    event = addEvent(entry.first, entry.second);
                    Logger.info(NAME,  "Added " + event + " in " + this);
                    break;
                case DELETED:
                    event = removeEvent(entry.first);
                    Logger.info(NAME,  "Removed " + event + " in " + this);
                    break;
            }
        });
        
        if (changed || newData.accessLevel != oldData.accessLevel) {
            loadAvailableEventColors();
        }
        
        data = newData;
        return true;
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
