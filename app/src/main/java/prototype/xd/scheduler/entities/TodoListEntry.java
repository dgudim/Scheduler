package prototype.xd.scheduler.entities;

import static android.util.Log.WARN;
import static androidx.core.math.MathUtils.clamp;
import static prototype.xd.scheduler.utilities.BitmapUtilities.mixTwoColors;
import static prototype.xd.scheduler.utilities.DateManager.currentDayUTC;
import static prototype.xd.scheduler.utilities.DateManager.currentTimestampUTC;
import static prototype.xd.scheduler.utilities.DateManager.datetimeStringFromMsUTC;
import static prototype.xd.scheduler.utilities.DateManager.daysUTCFromMsUTC;
import static prototype.xd.scheduler.utilities.DateManager.msUTCFromDaysUTC;
import static prototype.xd.scheduler.utilities.Logger.log;
import static prototype.xd.scheduler.utilities.PreferencesStore.preferences;
import static prototype.xd.scheduler.utilities.SystemCalendarUtils.getFirstValidKey;
import static prototype.xd.scheduler.utilities.Utilities.rangesOverlap;

import android.content.Context;
import android.util.ArrayMap;
import android.util.ArraySet;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import com.google.android.material.color.MaterialColors;

import org.dmfs.rfc5545.recurrenceset.RecurrenceSet;
import org.dmfs.rfc5545.recurrenceset.RecurrenceSetIterator;

import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.io.Serializable;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Objects;
import java.util.Set;
import java.util.function.Function;

import prototype.xd.scheduler.R;
import prototype.xd.scheduler.utilities.DateManager;
import prototype.xd.scheduler.utilities.Keys;
import prototype.xd.scheduler.utilities.SArrayMap;
import prototype.xd.scheduler.utilities.Utilities;
import prototype.xd.scheduler.views.CalendarView;

public class TodoListEntry extends RecycleViewEntry implements Serializable {
    
    static class CachedGetter<T> {
        
        ParameterGetter<T> parameterGetter;
        
        T value;
        boolean valid = false;
        
        CachedGetter(ParameterGetter<T> parameterGetter) {
            this.parameterGetter = parameterGetter;
        }
        
        void invalidate() {
            valid = false;
        }
        
        T get(T previousValue) {
            if (!valid) {
                value = parameterGetter.get(previousValue);
                valid = true;
            }
            return value;
        }
        
        T get() {
            return get(null);
        }
        
    }
    
    @FunctionalInterface
    public interface ParameterGetter<T> {
        T get(T previousValue);
        
        default T get() {
            return get(null);
        }
    }
    
    public static class Parameter<T> {
        
        TodoListEntry entry;
        
        CachedGetter<T> todayCachedGetter;
        Function<String, T> loadedParameterConverter;
        @Nullable
        CachedGetter<T> upcomingCachedGetter;
        @Nullable
        CachedGetter<T> expiredCachedGetter;
        
        Parameter(TodoListEntry entry,
                  String parameterKey,
                  ParameterGetter<T> todayValueGetter,
                  Function<String, T> loadedParameterConverter,
                  @Nullable ParameterGetter<T> upcomingValueGetter,
                  @Nullable ParameterGetter<T> expiredValueGetter) {
            this.entry = entry;
            upcomingCachedGetter = new CachedGetter<>(upcomingValueGetter);
            expiredCachedGetter = new CachedGetter<>(expiredValueGetter);
            todayCachedGetter = new CachedGetter<>(previousValue -> {
                // get parameter from group if it exists, if not, get from current parameters, if not, get from specified getter
                String paramValue = entry.group != null ?
                        entry.params.getOrDefault(parameterKey, entry.group.params.get(parameterKey)) :
                        entry.params.get(parameterKey);
                return paramValue != null ? loadedParameterConverter.apply(paramValue) : todayValueGetter.get(null);
            });
            this.loadedParameterConverter = loadedParameterConverter;
        }
        
        public T get(EntryType entryType) {
            T todayValue = todayCachedGetter.get();
            switch (entryType) {
                case EXPIRED:
                    return expiredCachedGetter == null ? todayValue : expiredCachedGetter.get(todayValue);
                case UPCOMING:
                    return upcomingCachedGetter == null ? todayValue : upcomingCachedGetter.get(todayValue);
                case TODAY:
                case GLOBAL:
                default:
                    return todayValue;
            }
        }
        
        public T get(long day) {
            return get(entry.getEntryType(day));
        }
        
        public T getToday() {
            return todayCachedGetter.get();
        }
        
        public void invalidate() {
            todayCachedGetter.invalidate();
            if (upcomingCachedGetter != null) {
                upcomingCachedGetter.invalidate();
            }
            if (expiredCachedGetter != null) {
                expiredCachedGetter.invalidate();
            }
        }
        
    }
    
    private static final transient String NAME = "Todo list entry";
    
    // don't rearrange, entry sorting is based on this
    public enum EntryType {TODAY, GLOBAL, UPCOMING, EXPIRED, UNKNOWN}
    
    @FunctionalInterface
    public interface ParameterInvalidationListener {
        void parametersInvalidated(TodoListEntry entry, Set<String> parameters);
    }
    
    protected SArrayMap<String, String> params = new SArrayMap<>();
    
    // calendar event values
    public transient SystemCalendarEvent event;
    private transient long startMsUTC = 0;
    private transient long endMsUTC = 0;
    private transient long durationMsUTC = 0;
    private transient boolean isAllDay = true;
    private transient RecurrenceSet recurrenceSet;
    
    public transient ParameterGetter<Long> startDayUTC;
    public transient ParameterGetter<Long> endDayUTC;
    private transient ParameterGetter<Long> durationDays;
    
    private ParameterGetter<String> rawTextValue;
    
    @Nullable
    private transient Group group;
    // for initializing group after deserialization
    private transient String tempGroupName;
    
    public transient Parameter<Integer> bgColor;
    public transient Parameter<Integer> fontColor;
    public transient Parameter<Integer> borderColor;
    public transient Parameter<Integer> borderThickness;
    
    public transient Parameter<Integer> priority;
    
    public transient Parameter<Integer> expiredDayOffset;
    public transient Parameter<Integer> upcomingDayOffset;
    
    public transient Parameter<Integer> adaptiveColorBalance;
    
    private transient int averageBackgroundColor = 0xff_FFFFFF;
    
    private transient ArrayMap<String, Parameter<?>> parameterMap;
    // for listening to parameter changes
    private transient ParameterInvalidationListener parameterInvalidationListener;
    
    private transient TodoListEntryList container;
    
    // ----------- supplementary stuff for sorting START
    int sortingIndex = 0;
    
    public int getSortingIndex() {
        return sortingIndex;
    }
    
    public void setSortingIndex(int sortingIndex) {
        this.sortingIndex = sortingIndex;
    }
    
    long cachedNearestStartMsUTC = 0;
    
    public long getCachedNearestStartMsUTC() {
        return cachedNearestStartMsUTC;
    }
    
    // obtain nearest start ms near a particular day for use in sorting later
    public void cacheNearestStartMsUTC(long targetDayLocal) {
        cachedNearestStartMsUTC = getNearestCalendarEventMsUTC(targetDayLocal);
    }
    // ----------- supplementary stuff for sorting END
    
    private static ArrayMap<String, Parameter<?>> mapParameters(TodoListEntry entry) {
        ArrayMap<String, Parameter<?>> parameterMap = new ArrayMap<>(8);
        parameterMap.put(Keys.BG_COLOR, entry.bgColor);
        parameterMap.put(Keys.FONT_COLOR, entry.fontColor);
        parameterMap.put(Keys.BORDER_COLOR, entry.borderColor);
        parameterMap.put(Keys.BORDER_THICKNESS, entry.borderThickness);
        parameterMap.put(Keys.PRIORITY, entry.priority);
        parameterMap.put(Keys.EXPIRED_ITEMS_OFFSET, entry.expiredDayOffset);
        parameterMap.put(Keys.UPCOMING_ITEMS_OFFSET, entry.upcomingDayOffset);
        parameterMap.put(Keys.ADAPTIVE_COLOR_BALANCE, entry.adaptiveColorBalance);
        return parameterMap;
    }
    
    public TodoListEntry(SystemCalendarEvent event) {
        this.event = event;
        event.computeDurationInDays();
        
        startMsUTC = event.startMsUTC;
        endMsUTC = event.endMsUTC;
        durationMsUTC = event.durationMs;
        isAllDay = event.isAllDay;
        recurrenceSet = event.rSet;
        
        initParameters();
        assignId(event.hashCode());
        
        event.linkEntry(this);
    }
    
    public TodoListEntry(SArrayMap<String, String> params, String groupName, List<Group> groups, long id) {
        tempGroupName = groupName;
        initGroupAndId(groups, id, true);
        this.params = params;
        initParameters();
    }
    
    // ------------ serialization
    @SuppressWarnings("unchecked")
    private void readObject(ObjectInputStream in)
            throws IOException, ClassNotFoundException {
        params = (SArrayMap<String, String>) in.readObject();
        tempGroupName = (String) in.readObject();
        initParameters();
    }
    
    
    private void writeObject(ObjectOutputStream out) throws IOException {
        out.writeObject(params);
        if (group == null) {
            out.writeObject("");
        } else {
            out.writeObject(group.getRawName());
        }
    }
    // ------------
    
    // attaching group to entry is unnecessary if called from bitmap drawer as we don't need to propagate parameter invalidations
    public void initGroupAndId(List<Group> groups, long id, boolean attachGroupToEntry) {
        // id should be assigned before attaching to group
        assignId(id);
        if (!tempGroupName.isEmpty()) {
            group = Group.findGroupInList(groups, tempGroupName);
            if (group == null) {
                log(WARN, NAME, "Unknown group: " + tempGroupName);
            } else if (attachGroupToEntry) {
                group.attachEntryInternal(this);
            }
        }
    }
    
    public void initParameters() {
        bgColor = new Parameter<>(this, Keys.BG_COLOR,
                previousValue -> {
                    int defaultColor = Keys.SETTINGS_DEFAULT_REGULAR_EVENT_BG_COLOR;
                    if (isFromSystemCalendar()) {
                        defaultColor = Keys.SETTINGS_DEFAULT_CALENDAR_EVENT_BG_COLOR.apply(event.color);
                    }
                    return preferences.getInt(getAppropriateKey(Keys.BG_COLOR), defaultColor);
                },
                Integer::parseInt,
                todayValue -> mixTwoColors(todayValue,
                        preferences.getInt(Keys.UPCOMING_BG_COLOR, Keys.SETTINGS_DEFAULT_UPCOMING_BG_COLOR), Keys.DEFAULT_TIME_OFFSET_COLOR_MIX_FACTOR),
                todayValue -> mixTwoColors(todayValue,
                        preferences.getInt(Keys.EXPIRED_BG_COLOR, Keys.SETTINGS_DEFAULT_EXPIRED_BG_COLOR), Keys.DEFAULT_TIME_OFFSET_COLOR_MIX_FACTOR));
        fontColor = new Parameter<>(this, Keys.FONT_COLOR,
                previousValue -> preferences.getInt(getAppropriateKey(Keys.FONT_COLOR), Keys.SETTINGS_DEFAULT_FONT_COLOR),
                Integer::parseInt,
                todayValue -> mixTwoColors(todayValue,
                        preferences.getInt(Keys.UPCOMING_FONT_COLOR, Keys.SETTINGS_DEFAULT_UPCOMING_FONT_COLOR), Keys.DEFAULT_TIME_OFFSET_COLOR_MIX_FACTOR),
                todayValue -> mixTwoColors(todayValue,
                        preferences.getInt(Keys.EXPIRED_FONT_COLOR, Keys.SETTINGS_DEFAULT_EXPIRED_FONT_COLOR), Keys.DEFAULT_TIME_OFFSET_COLOR_MIX_FACTOR));
        borderColor = new Parameter<>(this, Keys.BORDER_COLOR,
                previousValue -> preferences.getInt(getAppropriateKey(Keys.BORDER_COLOR), Keys.SETTINGS_DEFAULT_BORDER_COLOR),
                Integer::parseInt,
                todayValue -> mixTwoColors(todayValue,
                        preferences.getInt(Keys.UPCOMING_BORDER_COLOR, Keys.SETTINGS_DEFAULT_UPCOMING_BORDER_COLOR), Keys.DEFAULT_TIME_OFFSET_COLOR_MIX_FACTOR),
                todayValue -> mixTwoColors(todayValue,
                        preferences.getInt(Keys.EXPIRED_BORDER_COLOR, Keys.SETTINGS_DEFAULT_EXPIRED_BORDER_COLOR), Keys.DEFAULT_TIME_OFFSET_COLOR_MIX_FACTOR));
        borderThickness = new Parameter<>(this, Keys.BORDER_THICKNESS,
                previousValue -> preferences.getInt(getAppropriateKey(Keys.BORDER_THICKNESS), Keys.SETTINGS_DEFAULT_BORDER_THICKNESS),
                Integer::parseInt,
                todayValue -> preferences.getInt(Keys.UPCOMING_BORDER_THICKNESS, Keys.SETTINGS_DEFAULT_UPCOMING_BORDER_THICKNESS),
                todayValue -> preferences.getInt(Keys.EXPIRED_BORDER_THICKNESS, Keys.SETTINGS_DEFAULT_EXPIRED_BORDER_THICKNESS));
        priority = new Parameter<>(this, Keys.PRIORITY,
                previousValue -> isFromSystemCalendar() ?
                        preferences.getInt(getFirstValidKey(event.subKeys, Keys.PRIORITY), Keys.ENTRY_SETTINGS_DEFAULT_PRIORITY) :
                        Keys.ENTRY_SETTINGS_DEFAULT_PRIORITY,
                Integer::parseInt,
                null,
                null);
        expiredDayOffset = new Parameter<>(this, Keys.EXPIRED_ITEMS_OFFSET,
                previousValue -> preferences.getInt(getAppropriateKey(Keys.EXPIRED_ITEMS_OFFSET), Keys.SETTINGS_DEFAULT_EXPIRED_ITEMS_OFFSET),
                Integer::parseInt,
                null,
                null);
        upcomingDayOffset = new Parameter<>(this, Keys.UPCOMING_ITEMS_OFFSET,
                previousValue -> preferences.getInt(getAppropriateKey(Keys.UPCOMING_ITEMS_OFFSET), Keys.SETTINGS_DEFAULT_UPCOMING_ITEMS_OFFSET),
                Integer::parseInt,
                null,
                null);
        adaptiveColorBalance = new Parameter<>(this, Keys.ADAPTIVE_COLOR_BALANCE,
                previousValue -> preferences.getInt(getAppropriateKey(Keys.ADAPTIVE_COLOR_BALANCE), Keys.SETTINGS_DEFAULT_ADAPTIVE_COLOR_BALANCE),
                Integer::parseInt,
                null,
                null);
        
        parameterMap = mapParameters(this);
        
        startDayUTC = previousValue ->
                isFromSystemCalendar() ? event.startDayUTC : Long.parseLong(params.getOrDefault(Keys.START_DAY_UTC, Keys.DAY_FLAG_GLOBAL_STR));
        endDayUTC = previousValue ->
                isFromSystemCalendar() ? event.endDayUTC : Long.parseLong(params.getOrDefault(Keys.END_DAY_UTC, Keys.DAY_FLAG_GLOBAL_STR));
        durationDays = previousValue ->
                isFromSystemCalendar() ? event.durationDays : (endDayUTC.get() - startDayUTC.get());
        rawTextValue = previousValue ->
                isFromSystemCalendar() ? event.title : params.getOrDefault(Keys.TEXT_VALUE, "");
    }
    
    public void listenToParameterInvalidations(ParameterInvalidationListener parameterInvalidationListener) {
        if (this.parameterInvalidationListener != null) {
            log(WARN, NAME, rawTextValue.get() + " already has a parameterInvalidationListener, double assign");
        }
        this.parameterInvalidationListener = parameterInvalidationListener;
    }
    
    public void stopListeningToParameterInvalidations() {
        parameterInvalidationListener = null;
    }
    
    protected void linkToContainer(TodoListEntryList todoListEntryList) {
        if (container != null) {
            log(WARN, NAME, rawTextValue.get() + " already has a container, double linking");
        }
        container = todoListEntryList;
    }
    
    protected void unlinkFromContainer() {
        container = null;
    }
    
    protected void unlinkFromCalendarEvent() {
        if (isFromSystemCalendar()) {
            event.unlinkEntry();
        }
    }
    
    protected void removeFromContainer() {
        if (container == null) {
            log(WARN, NAME, rawTextValue.get() + " is not in a container, can't remove");
        } else {
            if (!container.remove(this)) {
                log(WARN, NAME, rawTextValue.get() + " error removing from the container");
            }
        }
    }
    
    public boolean isFromSystemCalendar() {
        return event != null;
    }
    
    @Nullable
    public Group getGroup() {
        return group;
    }
    
    public String getRawGroupName() {
        if (group != null) {
            return group.getRawName();
        } else {
            return "";
        }
    }
    
    public String getRawTextValue() {
        return rawTextValue.get();
    }
    
    protected void unlinkGroupInternal(boolean invalidate) {
        Set<String> parameters = null;
        invalidate = invalidate && group != null;
        if (invalidate) {
            parameters = group.params.keySet();
        }
        group = null;
        // invalidate only after group change to avoid weird settings from cache
        if (invalidate) {
            invalidateParameters(parameters);
        }
    }
    
    /**
     * Change associated group unlinking / relinking if necessary
     *
     * @param newGroup new group
     * @return true if the group was changed
     */
    public boolean changeGroup(@Nullable Group newGroup) {
        if (Objects.equals(newGroup, group)) {
            return false;
        }
        
        Set<String> changedKeys = null;
        
        if (group != null) {
            group.detachEntryInternal(this);
            if (newGroup == null) {
                changedKeys = group.params.keySet();
            }
        }
        
        if (newGroup != null) {
            newGroup.attachEntryInternal(this);
            if (group == null) {
                changedKeys = newGroup.params.keySet();
            }
        }
        
        if (group != null && newGroup != null) {
            changedKeys = Utilities.symmetricDifference(group.params, newGroup.params);
        }
        
        group = newGroup;
        // invalidate only after group change to avoid weird settings from cache
        invalidateParameters(changedKeys);
        
        return true;
    }
    
    protected void invalidateParameter(String parameterKey, boolean reportInvalidated) {
        Parameter<?> param = parameterMap.get(parameterKey);
        if (param != null) {
            param.invalidate();
        }
        if (parameterInvalidationListener != null && reportInvalidated) {
            parameterInvalidationListener.parametersInvalidated(this, Collections.singleton(parameterKey));
        }
    }
    
    protected void invalidateParameters(Set<String> parameterKeys) {
        parameterKeys.forEach(parameterKey -> invalidateParameter(parameterKey, false));
        parameterInvalidationListener.parametersInvalidated(this, parameterKeys);
    }
    
    public void invalidateAllParameters(boolean reportInvalidated) {
        parameterMap.forEach((key, parameter) -> parameter.invalidate());
        if (reportInvalidated) {
            parameterInvalidationListener.parametersInvalidated(this, parameterMap.keySet());
        }
    }
    
    public SArrayMap<String, String> getDisplayParams() {
        // shallow copy map
        SArrayMap<String, String> displayParams = new SArrayMap<>(params);
        // remove not display parameters
        displayParams.removeAll(Arrays.asList(Keys.TEXT_VALUE, Keys.START_DAY_UTC, Keys.END_DAY_UTC, Keys.IS_COMPLETED));
        return displayParams;
    }
    
    public void removeDisplayParams() {
        invalidateAllParameters(true);
        params.retainAll(Arrays.asList(Keys.TEXT_VALUE, Keys.START_DAY_UTC, Keys.END_DAY_UTC, Keys.IS_COMPLETED));
    }
    
    // changes parameter and returns true if it was changed
    private boolean changeParameterInternal(String key, String value) {
        return !Objects.equals(params.put(key, value), key);
    }
    
    // change any number of parameters
    public void changeParameters(String... keyValuePairs) {
        if (keyValuePairs.length % 2 != 0) {
            throw new IllegalArgumentException("Can't call changeParameters with event number of arguments");
        }
        if (keyValuePairs.length == 2) { // just one parameter
            if (changeParameterInternal(keyValuePairs[0], keyValuePairs[1])) {
                invalidateParameter(keyValuePairs[0], true);
            }
            return;
        }
        Set<String> parameterKeys = new ArraySet<>();
        for (int i = 0; i < keyValuePairs.length; i += 2) {
            if (changeParameterInternal(keyValuePairs[i], keyValuePairs[i + 1])) {
                parameterKeys.add(keyValuePairs[i]);
            }
        }
        invalidateParameters(parameterKeys);
    }
    
    public void setAverageBackgroundColor(int averageBackgroundColor) {
        this.averageBackgroundColor = averageBackgroundColor;
    }
    
    private boolean inRange(long day, long instanceStartDay) {
        return isGlobal() ||
                (instanceStartDay - upcomingDayOffset.getToday() <= day &&
                        day <= instanceStartDay + durationDays.get() + expiredDayOffset.getToday());
    }
    
    public boolean isCompleted() {
        // everything else except "true" is false, so we are fine here
        return Boolean.parseBoolean(params.get(Keys.IS_COMPLETED));
    }
    
    public boolean isVisibleOnLockscreenToday() {
        if (isFromSystemCalendar()) {
            if (!hideByContent()) {
                boolean showOnLock;
                if (!isAllDay && preferences.getBoolean(Keys.HIDE_EXPIRED_ENTRIES_BY_TIME, Keys.SETTINGS_DEFAULT_HIDE_EXPIRED_ENTRIES_BY_TIME)) {
                    showOnLock = isVisibleExact(currentTimestampUTC);
                } else {
                    showOnLock = isVisible(currentDayUTC);
                }
                return showOnLock && preferences.getBoolean(getFirstValidKey(event.subKeys, Keys.SHOW_ON_LOCK), Keys.CALENDAR_SETTINGS_DEFAULT_SHOW_ON_LOCK);
            }
            return false;
        } else {
            if (isGlobal()) {
                return preferences.getBoolean(Keys.SHOW_GLOBAL_ITEMS_LOCK, Keys.SETTINGS_DEFAULT_SHOW_GLOBAL_ITEMS_LOCK);
            }
            return !isCompleted() && isVisible(currentDayUTC);
        }
    }
    
    @FunctionalInterface
    interface RecurrenceSetConsumer<T> {
        /**
         * A function to process each entry in a recurrence set
         *
         * @param instanceStartMsUTC ms since epoch of the instance
         * @param instanceStartDay   days since the epoch of the instance
         * @return return value or null if should iterate forward
         */
        @Nullable
        T processInstance(long instanceStartMsUTC, long instanceStartDay);
    }
    
    private <T> T iterateRecurrenceSet(long startMs, long firstDayUTC, RecurrenceSetConsumer<T> recurrenceSetConsumer, T defaultValue) {
        RecurrenceSetIterator it = recurrenceSet.iterator(event.timeZone, startMs);
        it.fastForward(msUTCFromDaysUTC(firstDayUTC - 1));
        long instanceMsUTC;
        long instanceDay;
        T val;
        while (it.hasNext()) {
            instanceMsUTC = it.next();
            instanceDay = daysUTCFromMsUTC(instanceMsUTC);
            val = recurrenceSetConsumer.processInstance(instanceMsUTC, instanceDay);
            if (val != null) {
                return val;
            }
        }
        return defaultValue;
    }
    
    private boolean isVisible(long targetDay) {
        // global entries are visible everywhere, no need to do anything more
        if (isGlobal()) {
            return true;
        }
        if (container != null) {
            return container.notGlobalEntryVisibleOnDay(this, targetDay);
        }
        // fallback to iterating the recurrence set
        return inRange(targetDay, getNearestEventDay(targetDay));
    }
    
    private boolean isVisibleExact(long targetMsUTC) {
        // global entries are visible everywhere, no need to do anything more
        if (isGlobal()) {
            return true;
        }
        long targetDayUTC = daysUTCFromMsUTC(targetMsUTC);
        
        if (container != null) {
            EntryType entryType = container.getEntryType(this, targetDayUTC);
            if (entryType == EntryType.EXPIRED) {
                // expired are always hidden
                return false;
            }
            if (entryType == EntryType.UPCOMING) {
                // upcoming are always visible
                return true;
            }
        }
        
        if (!isFromSystemCalendar()) {
            // local events don't have timestamps, just check one range
            return inRange(targetDayUTC, startDayUTC.get()) && startMsUTC + durationMsUTC >= targetMsUTC;
        }
        
        long nearestStartMsUTC = getNearestCalendarEventMsUTC(targetDayUTC);
        long nearestDayUTC = daysUTCFromMsUTC(nearestStartMsUTC);
        
        return inRange(targetDayUTC, nearestDayUTC) && nearestStartMsUTC + durationMsUTC >= targetMsUTC;
    }
    
    private void addDayRangeToSet(long dayFrom, long dayTo,
                                  final long minDay, final long maxDay,
                                  @Nullable Set<Long> daySet) {
        if (daySet != null && rangesOverlap(dayFrom, dayTo, minDay, maxDay)) {
            dayFrom = clamp(dayFrom, minDay, maxDay);
            dayTo = clamp(dayTo, minDay, maxDay);
            for (long day = dayFrom; day <= dayTo; day++) {
                daySet.add(day);
            }
        }
    }
    
    private void addDayRangeToSets(long startDay, long endDay,
                                   long minDay, long maxDay,
                                   long currentExpiredDayOffset, long currentUpcomingDayOffset,
                                   @Nullable Set<Long> coreDaySet,                   // without any expired/upcoming days
                                   @Nullable Set<Long> currentUpcomingExpiredSet) {  // without old expired/upcoming days
        
        addDayRangeToSet(
                startDay, endDay,
                minDay, maxDay,
                coreDaySet);
        
        addDayRangeToSet(
                startDay - currentUpcomingDayOffset, endDay + currentExpiredDayOffset,
                minDay, maxDay,
                currentUpcomingExpiredSet);
    }
    
    // get on what days from min to max an entry is visible (and was before invalidation)
    private void getVisibleDays(long minDay, long maxDay,
                                @Nullable Set<Long> coreDaySet,                  // without any expired/upcoming days
                                @Nullable Set<Long> currentUpcomingExpiredSet) { // with old expired/upcoming days
        
        // we don't care about global entries, they are handled differently
        if (isGlobal()) {
            return;
        }
        
        long currentUpcomingDayOffset = this.upcomingDayOffset.getToday();
        long currentExpiredDayOffset = this.expiredDayOffset.getToday();
        
        if (recurrenceSet != null) {
            iterateRecurrenceSet(startMsUTC, minDay, (instanceStartMsUTC, instanceStartDayUTC) -> {
                
                // overshot
                if (instanceStartDayUTC + currentUpcomingDayOffset > maxDay) {
                    return false;
                }
                
                addDayRangeToSets(instanceStartDayUTC, instanceStartDayUTC + durationDays.get(),
                        minDay, maxDay,
                        // offsets
                        currentExpiredDayOffset, currentUpcomingDayOffset,
                        // sets
                        coreDaySet,
                        currentUpcomingExpiredSet);
                
                return null;
            }, false);
        } else {
            addDayRangeToSets(startDayUTC.get(), endDayUTC.get(),
                    minDay, maxDay,
                    currentExpiredDayOffset, currentUpcomingDayOffset,
                    coreDaySet, currentUpcomingExpiredSet);
        }
    }
    
    public enum RangeType {
        CORE, EXPIRED_UPCOMING
    }
    
    public void getVisibleDaysOnCalendar(@NonNull final CalendarView calendarView,
                                         @NonNull final Set<Long> daySet, RangeType rangeType) {
        getVisibleDays(calendarView.getFirstLoadedDayUTC(), calendarView.getLastLoadedDayUTC(),
                rangeType == RangeType.CORE ? daySet : null,
                rangeType == RangeType.EXPIRED_UPCOMING ? daySet : null);
    }
    
    // return on what days from min to max an entry is visible (and was before invalidation)
    public Set<Long> getVisibleDaysOnCalendar(@NonNull final CalendarView calendarView,
                                              RangeType rangeType) {
        // we don't care about global entries, they are handled differently
        if (isGlobal()) {
            return Collections.emptySet();
        }
        Set<Long> daySet = new ArraySet<>();
        getVisibleDaysOnCalendar(calendarView, daySet, rangeType);
        return daySet;
    }
    
    public FullDaySet getFullDaySet(long minDay, long maxDay) {
        return new FullDaySet(this, minDay, maxDay);
    }
    
    static class FullDaySet {
        
        private final Set<Long> upcomingExpiredDaySet = new ArraySet<>();
        private final Set<Long> coreDaySet = new ArraySet<>();
        
        FullDaySet(TodoListEntry entry, long dayStart, long dayEnd) {
            entry.getVisibleDays(dayStart, dayEnd,
                    coreDaySet,             // core
                    upcomingExpiredDaySet); // basic expired / upcoming
        }
        
        public Set<Long> getUpcomingExpiredDaySet() {
            return upcomingExpiredDaySet;
        }
        
        public Set<Long> getCoreDaySet() {
            return coreDaySet;
        }
    }
    
    public boolean hideByContent() {
        if (!isFromSystemCalendar()) return false;
        boolean hideByContent = false;
        if (preferences.getBoolean(getFirstValidKey(event.subKeys, Keys.HIDE_ENTRIES_BY_CONTENT), Keys.SETTINGS_DEFAULT_HIDE_ENTRIES_BY_CONTENT)) {
            String matchString = preferences.getString(getFirstValidKey(event.subKeys, Keys.HIDE_ENTRIES_BY_CONTENT_CONTENT), "");
            String[] split = !matchString.isEmpty() ? matchString.split("\\|\\|") : new String[0];
            for (String str : split) {
                hideByContent = rawTextValue.get().contains(str);
                if (hideByContent) break;
            }
        }
        return hideByContent;
    }
    
    public long getNearestCalendarEventMsUTC(long targetDayUTC) {
        if (targetDayUTC >= endDayUTC.get()) {
            // no need to iterate, we already overshot
            return endMsUTC;
        }
        if (recurrenceSet != null) {
            return iterateRecurrenceSet(startMsUTC, targetDayUTC, (instanceStartMsUTC, instanceStartDayUTC) -> {
                // if in range or overshoot
                if (inRange(targetDayUTC, instanceStartDayUTC) || instanceStartDayUTC >= targetDayUTC) {
                    return instanceStartMsUTC;
                }
                return null;
            }, 0L);
        }
        return startMsUTC;
    }
    
    public long getNearestEventDay(long day) {
        if (isFromSystemCalendar()) {
            return daysUTCFromMsUTC(getNearestCalendarEventMsUTC(day));
        }
        return startDayUTC.get();
    }
    
    public EntryType getEntryType(long targetDayUTC) {
        
        if (isGlobal()) {
            return EntryType.GLOBAL;
        }
        
        // in case of regular entry this will return startDay
        long nearestDayLocal = getNearestEventDay(targetDayUTC);
        
        if (nearestDayLocal == targetDayUTC) {
            return EntryType.TODAY;
        }
        
        // extended range covers but still less that nearestDayLocal
        if (targetDayUTC + upcomingDayOffset.getToday() >= nearestDayLocal
                && targetDayUTC < nearestDayLocal) {
            return EntryType.UPCOMING;
        }
        
        // NOTE: for regular entries durationDays = 0
        // extended range covers but more that nearestDayLocal
        if (targetDayUTC - expiredDayOffset.getToday() <= nearestDayLocal + durationDays.get()
                && targetDayUTC > nearestDayLocal + durationDays.get()) {
            return EntryType.EXPIRED;
        }
        
        return EntryType.UNKNOWN;
    }
    
    public boolean isGlobal() {
        return startDayUTC.get() == Keys.DAY_FLAG_GLOBAL;
    }
    
    public String getTimeSpan(Context context, long targetDayUTC) {
        
        if (!isFromSystemCalendar()) {
            return "";
        }
        
        if (isAllDay) {
            return context.getString(R.string.calendar_event_all_day);
        }
        
        if (recurrenceSet != null) {
            long nearestMsUTC = getNearestCalendarEventMsUTC(targetDayUTC);
            return DateManager.getTimeSpan(nearestMsUTC, nearestMsUTC + durationMsUTC);
        }
        
        if (startMsUTC == endMsUTC) {
            return datetimeStringFromMsUTC(startMsUTC);
        } else {
            return DateManager.getTimeSpan(startMsUTC, endMsUTC);
        }
    }
    
    // get calendar key for calendar entries and regular key for normal entries
    private String getAppropriateKey(String key) {
        if (isFromSystemCalendar()) {
            return getFirstValidKey(event.subKeys, key);
        }
        return key;
    }
    
    public boolean isAdaptiveColorEnabled() {
        return adaptiveColorBalance.getToday() > 0;
    }
    
    public int getAdaptiveColor(int inputColor) {
        if (isAdaptiveColorEnabled()) {
            return mixTwoColors(MaterialColors.harmonize(inputColor, averageBackgroundColor),
                    averageBackgroundColor, (adaptiveColorBalance.getToday() - 1) / 9d);
            //active adaptiveColorBalance is from 1 to 10, so we make it from 0 to 9
        }
        return inputColor;
    }
    
    public String getTextOnDay(long day, Context context, boolean displayGlobalLabel) {
        return rawTextValue.get() + getDayOffset(day, context, displayGlobalLabel);
    }
    
    public String getDayOffset(long day, Context context, boolean displayGlobalLabel) {
        String dayOffset = "";
        if (!isGlobal()) {
            
            int dayShift = 0;
            
            long nearestDay = getNearestEventDay(day);
            if (day < nearestDay) {
                dayShift = (int) (nearestDay - day);
            } else if (day > nearestDay + durationDays.get()) {
                dayShift = (int) (nearestDay + durationDays.get() - day);
            }
            
            if (dayShift < 31 && dayShift > -31) {
                if (dayShift > 0) {
                    switch (dayShift) {
                        case (1):
                            dayOffset = context.getString(R.string.item_tomorrow);
                            break;
                        case (2):
                        case (3):
                        case (4):
                        case (22):
                        case (23):
                        case (24):
                            dayOffset = context.getString(R.string.item_in_N_days_single, dayShift);
                            break;
                        default:
                            dayOffset = context.getString(R.string.item_in_N_days_double, dayShift);
                            break;
                    }
                } else if (dayShift < 0) {
                    switch (dayShift) {
                        case (-1):
                            dayOffset = context.getString(R.string.item_yesterday);
                            break;
                        case (-2):
                        case (-3):
                        case (-4):
                        case (-22):
                        case (-23):
                        case (-24):
                            dayOffset = context.getString(R.string.item_N_days_ago_single, -dayShift);
                            break;
                        default:
                            dayOffset = context.getString(R.string.item_N_days_ago_double, -dayShift);
                            break;
                    }
                }
            } else {
                if (dayShift < 0) {
                    dayOffset = context.getString(R.string.item_more_than_a_month_ago);
                } else {
                    dayOffset = context.getString(R.string.item_in_more_than_in_a_month);
                }
            }
        } else if (displayGlobalLabel) {
            dayOffset = context.getString(R.string.item_global);
        }
        if (!dayOffset.equals("")) {
            return " " + dayOffset;
        }
        return dayOffset;
    }
    
    public void setStateIconColor(TextView icon, String parameter) {
        boolean containedInGroupParams = group != null && group.params.containsKey(parameter);
        boolean containedInPersonalParams = params.containsKey(parameter);
        
        if (containedInGroupParams && containedInPersonalParams) {
            icon.setTextColor(icon.getContext().getColor(R.color.entry_settings_parameter_group_and_personal));
        } else if (containedInGroupParams) {
            icon.setTextColor(icon.getContext().getColor(R.color.entry_settings_parameter_group));
        } else if (containedInPersonalParams) {
            icon.setTextColor(icon.getContext().getColor(R.color.entry_settings_parameter_personal));
        } else {
            icon.setTextColor(icon.getContext().getColor(R.color.entry_settings_parameter_default));
        }
    }
    
    // for recyclerview
    @Override
    public int getType() {
        return isFromSystemCalendar() ? 1 : 0;
    }
    
    @NonNull
    @Override
    public String toString() {
        if (isFromSystemCalendar()) {
            return "TodoListEntry: " + rawTextValue.get() + " [" + event + "] (" + startDayUTC.get() + " - " + endDayUTC.get() + ")";
        }
        return "TodoListEntry: " + rawTextValue.get() + " " + "(" + startDayUTC.get() + " - " + endDayUTC.get() + ")";
    }
}