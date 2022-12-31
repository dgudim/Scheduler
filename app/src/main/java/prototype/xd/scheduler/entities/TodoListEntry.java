package prototype.xd.scheduler.entities;

import static androidx.core.math.MathUtils.clamp;
import static java.lang.Math.abs;
import static prototype.xd.scheduler.utilities.BitmapUtilities.mixTwoColors;
import static prototype.xd.scheduler.utilities.DateManager.currentDayUTC;
import static prototype.xd.scheduler.utilities.DateManager.currentTimestampUTC;
import static prototype.xd.scheduler.utilities.DateManager.msUTCtoDaysLocal;
import static prototype.xd.scheduler.utilities.Utilities.getPluralString;
import static prototype.xd.scheduler.utilities.Utilities.rangesOverlap;

import android.content.Context;
import android.util.ArrayMap;
import android.util.ArraySet;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import com.google.android.material.color.MaterialColors;

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
import prototype.xd.scheduler.utilities.Logger;
import prototype.xd.scheduler.utilities.SArrayMap;
import prototype.xd.scheduler.utilities.Utilities;
import prototype.xd.scheduler.views.CalendarView;

public class TodoListEntry extends RecycleViewEntry implements Serializable {
    
    static class CachedGetter<T> {
        
        final ParameterGetter<T> parameterGetter;
        
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
        
        final TodoListEntry entry;
        
        final CachedGetter<T> todayCachedGetter;
        final Function<String, T> loadedParameterConverter;
        @Nullable
        final
        CachedGetter<T> upcomingCachedGetter;
        @Nullable
        final
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
    
    public static class TimeRange {
        
        public static final TimeRange NullRange = new TimeRange(-1, -1);
        
        private boolean inDays = false;
        
        private long start;
        private long end;
        
        TimeRange(long startMsUTC, long endMsUTC) {
            this.start = startMsUTC;
            this.end = endMsUTC;
        }
        
        public TimeRange toLocalDays(boolean clone) {
            if (clone) {
                return new TimeRange(start, end).toLocalDays(false);
            }
            if (inDays) {
                Logger.warning("TimeRange", "Trying to convert range to days but it's already in days");
                return this;
            }
            start = msUTCtoDaysLocal(start);
            end = msUTCtoDaysLocal(end);
            inDays = true;
            return this;
        }
        
        public long getStart() {
            return start;
        }
        
        public long getEnd() {
            return end;
        }
    }
    
    protected SArrayMap<String, String> params = new SArrayMap<>();
    
    // calendar event values
    public transient SystemCalendarEvent event;
    
    public transient ParameterGetter<Long> startDayLocal;
    public transient ParameterGetter<Long> endDayLocal;
    
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
    
    public void cacheSortingIndex(long targetDayUTC) {
        sortingIndex = getEntryType(targetDayUTC).ordinal();
    }
    
    long cachedNearestStartMsUTC = 0;
    
    public long getCachedNearestStartMsUTC() {
        return cachedNearestStartMsUTC;
    }
    
    // obtain nearest start ms near a particular day for use in sorting later
    public void cacheNearestStartMsUTC(long targetDayLocal) {
        cachedNearestStartMsUTC = getNearestCalendarEventMsRangeUTC(targetDayLocal).getStart();
    }
    // ----------- supplementary stuff for sorting END
    
    private static ArrayMap<String, Parameter<?>> mapParameters(TodoListEntry entry) {
        ArrayMap<String, Parameter<?>> parameterMap = new ArrayMap<>(8);
        parameterMap.put(Keys.BG_COLOR.key, entry.bgColor);
        parameterMap.put(Keys.FONT_COLOR.key, entry.fontColor);
        parameterMap.put(Keys.BORDER_COLOR.key, entry.borderColor);
        parameterMap.put(Keys.BORDER_THICKNESS.key, entry.borderThickness);
        parameterMap.put(Keys.PRIORITY.key, entry.priority);
        parameterMap.put(Keys.EXPIRED_ITEMS_OFFSET.key, entry.expiredDayOffset);
        parameterMap.put(Keys.UPCOMING_ITEMS_OFFSET.key, entry.upcomingDayOffset);
        parameterMap.put(Keys.ADAPTIVE_COLOR_BALANCE.key, entry.adaptiveColorBalance);
        return parameterMap;
    }
    
    public TodoListEntry(SystemCalendarEvent event) {
        this.event = event;
        
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
                Logger.warning(NAME, "Unknown group: " + tempGroupName);
            } else if (attachGroupToEntry) {
                group.attachEntryInternal(this);
            }
        }
    }
    
    public void initParameters() {
        bgColor = new Parameter<>(this, Keys.BG_COLOR.key,
                previousValue -> {
                    if (isFromSystemCalendar()) {
                        return Keys.BG_COLOR.get(event.subKeys, Keys.SETTINGS_DEFAULT_CALENDAR_EVENT_BG_COLOR.apply(event.color));
                    }
                    return Keys.BG_COLOR.get();
                },
                Integer::parseInt,
                todayValue -> mixTwoColors(todayValue, Keys.UPCOMING_BG_COLOR.get(), Keys.DEFAULT_TIME_OFFSET_COLOR_MIX_FACTOR),
                todayValue -> mixTwoColors(todayValue, Keys.EXPIRED_BG_COLOR.get(), Keys.DEFAULT_TIME_OFFSET_COLOR_MIX_FACTOR));
        fontColor = new Parameter<>(this, Keys.FONT_COLOR.key,
                previousValue -> Keys.FONT_COLOR.get(getSubKeys()),
                Integer::parseInt,
                todayValue -> mixTwoColors(todayValue, Keys.UPCOMING_FONT_COLOR.get(), Keys.DEFAULT_TIME_OFFSET_COLOR_MIX_FACTOR),
                todayValue -> mixTwoColors(todayValue, Keys.EXPIRED_FONT_COLOR.get(), Keys.DEFAULT_TIME_OFFSET_COLOR_MIX_FACTOR));
        borderColor = new Parameter<>(this, Keys.BORDER_COLOR.key,
                previousValue -> Keys.BORDER_COLOR.get(getSubKeys()),
                Integer::parseInt,
                todayValue -> mixTwoColors(todayValue, Keys.UPCOMING_BORDER_COLOR.get(), Keys.DEFAULT_TIME_OFFSET_COLOR_MIX_FACTOR),
                todayValue -> mixTwoColors(todayValue, Keys.EXPIRED_BORDER_COLOR.get(), Keys.DEFAULT_TIME_OFFSET_COLOR_MIX_FACTOR));
        borderThickness = new Parameter<>(this, Keys.BORDER_THICKNESS.key,
                previousValue -> Keys.BORDER_THICKNESS.get(getSubKeys()),
                Integer::parseInt,
                todayValue -> Keys.UPCOMING_BORDER_THICKNESS.get(),
                todayValue -> Keys.EXPIRED_BORDER_THICKNESS.get());
        priority = new Parameter<>(this, Keys.PRIORITY.key,
                previousValue -> isFromSystemCalendar() ?
                        Keys.PRIORITY.get(event.subKeys) :
                        Keys.PRIORITY.defaultValue, // there is no default setting for priority
                Integer::parseInt,
                null,
                null);
        expiredDayOffset = new Parameter<>(this, Keys.EXPIRED_ITEMS_OFFSET.key,
                previousValue -> Keys.EXPIRED_ITEMS_OFFSET.get(getSubKeys()),
                Integer::parseInt,
                null,
                null);
        upcomingDayOffset = new Parameter<>(this, Keys.UPCOMING_ITEMS_OFFSET.key,
                previousValue -> Keys.UPCOMING_ITEMS_OFFSET.get(getSubKeys()),
                Integer::parseInt,
                null,
                null);
        adaptiveColorBalance = new Parameter<>(this, Keys.ADAPTIVE_COLOR_BALANCE.key,
                previousValue -> Keys.ADAPTIVE_COLOR_BALANCE.get(getSubKeys()),
                Integer::parseInt,
                null,
                null);
        
        parameterMap = mapParameters(this);
        
        startDayLocal = previousValue ->
                isFromSystemCalendar() ? event.startDayLocal : Long.parseLong(params.getOrDefault(Keys.START_DAY_UTC, Keys.DAY_FLAG_GLOBAL_STR));
        endDayLocal = previousValue ->
                isFromSystemCalendar() ? event.endDayLocal : Long.parseLong(params.getOrDefault(Keys.END_DAY_UTC, Keys.DAY_FLAG_GLOBAL_STR));
        rawTextValue = previousValue ->
                isFromSystemCalendar() ? event.title : params.getOrDefault(Keys.TEXT_VALUE, "");
    }
    
    // get calendar key for calendar entries and null for normal entries
    @Nullable
    private List<String> getSubKeys() {
        if (isFromSystemCalendar()) {
            return event.subKeys;
        }
        return null;
    }
    
    public void listenToParameterInvalidations(ParameterInvalidationListener parameterInvalidationListener) {
        if (this.parameterInvalidationListener != null) {
            Logger.warning(NAME, rawTextValue.get() + " already has a parameterInvalidationListener, double assign");
        }
        this.parameterInvalidationListener = parameterInvalidationListener;
    }
    
    public void stopListeningToParameterInvalidations() {
        parameterInvalidationListener = null;
    }
    
    protected void linkToContainer(TodoListEntryList todoListEntryList) {
        if (container != null) {
            Logger.warning(NAME, rawTextValue.get() + " already has a container, double linking");
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
            Logger.warning(NAME, rawTextValue.get() + " is not in a container, can't remove");
        } else {
            if (!container.remove(this)) {
                Logger.warning(NAME, rawTextValue.get() + " error removing from the container");
            }
        }
    }
    
    public boolean isFromSystemCalendar() {
        return event != null;
    }
    
    public boolean isRecurring() {
        return isFromSystemCalendar() && event.isRecurring();
    }
    
    
    // only call on calendar events
    public void notifyTimeZoneChanged() {
        // days can change on timezone changes, so we update them
        event.computeEventVisibilityDays();
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
    
    private boolean inRange(long targetDayUTC, TimeRange instanceRange) {
        return inRange(targetDayUTC, instanceRange.getStart(), instanceRange.getEnd());
    }
    
    private boolean inRange(long targetDayUTC, long instanceStartDayLocal, long instanceEndDayLocal) {
        return instanceStartDayLocal - upcomingDayOffset.getToday() <= targetDayUTC &&
                targetDayUTC <= instanceEndDayLocal + expiredDayOffset.getToday();
    }
    
    public boolean isCompleted() {
        // everything else except "true" is false, so we are fine here
        return Boolean.parseBoolean(params.get(Keys.IS_COMPLETED));
    }
    
    public boolean isVisibleOnLockscreenToday() {
        if (isFromSystemCalendar()) {
            if (hideByContent()) {
                return true;
            }
            
            if (!Keys.CALENDAR_SHOW_ON_LOCK.get(event.subKeys)) {
                return false;
            }
            
            if (!event.isAllDay && Keys.HIDE_EXPIRED_ENTRIES_BY_TIME.get(event.subKeys)) {
                return isVisibleExact(currentDayUTC, currentTimestampUTC);
            }
        } else if (isGlobal()) {
            return Keys.SHOW_GLOBAL_ITEMS_LOCK.get();
        }
        
        return !isCompleted() && isVisible(currentDayUTC);
    }
    
    private boolean isVisible(long targetDayUTC) {
        // global entries are visible everywhere, no need to do anything more
        if (isGlobal()) {
            return true;
        }
        if (container != null) {
            return container.notGlobalEntryVisibleOnDay(this, targetDayUTC);
        }
        // fallback to iterating the recurrence set
        return inRange(targetDayUTC, getNearestLocalEventDayRange(targetDayUTC));
    }
    
    private boolean isVisibleExact(long targetDayUTC, long targetMsUTC) {
        // global entries are visible everywhere, no need to do anything more
        if (isGlobal()) {
            return true;
        }
        
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
            return inRange(targetDayUTC, startDayLocal.get(), endDayLocal.get());
        }
        
        TimeRange nearestMsRangeUTC = getNearestCalendarEventMsRangeUTC(targetDayUTC);
        
        return inRange(targetDayUTC, nearestMsRangeUTC.toLocalDays(true)) &&
                nearestMsRangeUTC.getEnd() >= targetMsUTC;
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
        
        if (isRecurring()) {
            event.iterateRecurrenceSet(minDay, (instanceStartMsUTC, instanceEndMsUTC, instanceStartDayLocal, instanceEndDayLocal) -> {
                
                // overshot
                if (instanceStartDayLocal + currentUpcomingDayOffset > maxDay) {
                    return false;
                }
                
                addDayRangeToSets(instanceStartDayLocal, instanceEndDayLocal,
                        minDay, maxDay,
                        // offsets
                        currentExpiredDayOffset, currentUpcomingDayOffset,
                        // sets
                        coreDaySet,
                        currentUpcomingExpiredSet);
                
                return null;
            }, false);
        } else {
            addDayRangeToSets(startDayLocal.get(), endDayLocal.get(),
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
        if (Keys.HIDE_ENTRIES_BY_CONTENT.get(event.subKeys)) {
            String matchString = Keys.HIDE_ENTRIES_BY_CONTENT_CONTENT.get(event.subKeys);
            if (matchString.isEmpty()) {
                return false;
            }
            String[] split = matchString.split("\\|\\|");
            for (String str : split) {
                if (rawTextValue.get().contains(str)) {
                    return true;
                }
            }
        }
        return false;
    }
    
    private TimeRange getNearestCalendarEventMsRangeUTC(long targetDayUTC) {
        if (isRecurring()) {
            return event.iterateRecurrenceSet(targetDayUTC, (instanceStartMsUTC, instanceEndMsUTC, instanceStartDayLocal, instanceEndDayLocal) -> {
                // if in range or overshoot
                if (inRange(targetDayUTC, instanceStartDayLocal, instanceEndDayLocal) || instanceStartDayLocal >= targetDayUTC) {
                    return new TimeRange(instanceStartMsUTC, instanceEndMsUTC);
                }
                return null;
            }, TimeRange.NullRange);
        }
        return new TimeRange(event.startMsUTC, event.endMsUTC);
    }
    
    private TimeRange getNearestLocalEventDayRange(long targetDayUTC) {
        if (isFromSystemCalendar()) {
            return getNearestCalendarEventMsRangeUTC(targetDayUTC).toLocalDays(false);
        }
        return new TimeRange(startDayLocal.get(), endDayLocal.get());
    }
    
    public EntryType getEntryType(long targetDayUTC) {
        
        if (isGlobal()) {
            return EntryType.GLOBAL;
        }
        
        // in case of regular entry this will return startDay - endDay
        TimeRange nearestDayRange = getNearestLocalEventDayRange(targetDayUTC);
        
        if (nearestDayRange.getStart() <= targetDayUTC && targetDayUTC <= nearestDayRange.getEnd()) {
            return EntryType.TODAY;
        }
        
        // extended range covers but still less that nearestStartDayLocal
        if (targetDayUTC + upcomingDayOffset.getToday() >= nearestDayRange.getStart()
                && targetDayUTC < nearestDayRange.getStart()) {
            return EntryType.UPCOMING;
        }
        
        // NOTE: for regular entries durationDays = 0
        // extended range covers but more that nearestStartDayLocal
        if (targetDayUTC - expiredDayOffset.getToday() <= nearestDayRange.getEnd()
                && targetDayUTC > nearestDayRange.getEnd()) {
            return EntryType.EXPIRED;
        }
        
        return EntryType.UNKNOWN;
    }
    
    public boolean isGlobal() {
        return startDayLocal.get() == Keys.DAY_FLAG_GLOBAL;
    }
    
    public String getCalendarEntryTimeSpan(Context context, long targetDayUTC) {
        if (event.isAllDay) {
            return context.getString(R.string.calendar_event_all_day);
        }
        
        return DateManager.getTimeSpan(getNearestCalendarEventMsRangeUTC(targetDayUTC));
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
    
    public String getTextOnDay(long targetDayUTC, Context context, boolean displayGlobalLabel) {
        return rawTextValue.get() + getDayOffset(targetDayUTC, context, displayGlobalLabel);
    }
    
    public String getDayOffset(long targetDayUTC, Context context, boolean displayGlobalLabel) {
        if (isGlobal()) {
            if (displayGlobalLabel) {
                return context.getString(R.string.item_global);
            }
            return "";
        }
        
        int dayShift = 0;
        
        TimeRange nearestDayRangeLocal = getNearestLocalEventDayRange(targetDayUTC);
        
        if (targetDayUTC < nearestDayRangeLocal.getStart()) {
            dayShift = (int) (nearestDayRangeLocal.getStart() - targetDayUTC);
        } else if (targetDayUTC > nearestDayRangeLocal.getEnd()) {
            dayShift = (int) (nearestDayRangeLocal.getEnd() - targetDayUTC);
        }
        
        if (dayShift == 0) {
            return "";
        }
        
        int dayShiftAbs = abs(dayShift);
        
        if (dayShiftAbs < 31) {
            if (dayShiftAbs == 1) {
                return context.getString(dayShift > 0 ? R.string.item_tomorrow : R.string.item_yesterday);
            } else {
                return getPluralString(context, dayShift > 0 ? R.plurals.item_in_N_days : R.plurals.item_N_days_ago, dayShiftAbs - 1);
            }
        } else {
            return context.getString(dayShift > 0 ? R.string.item_in_more_than_in_a_month : R.string.item_more_than_a_month_ago);
        }
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
            return "TodoListEntry: " + rawTextValue.get() + " [" + event + "] (" + startDayLocal.get() + " - " + endDayLocal.get() + ")";
        }
        return "TodoListEntry: " + rawTextValue.get() + " " + "(" + startDayLocal.get() + " - " + endDayLocal.get() + ")";
    }
}