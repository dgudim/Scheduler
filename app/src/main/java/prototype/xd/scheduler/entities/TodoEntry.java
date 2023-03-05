package prototype.xd.scheduler.entities;

import static androidx.core.math.MathUtils.clamp;
import static java.lang.Math.abs;
import static prototype.xd.scheduler.entities.Group.NULL_GROUP;
import static prototype.xd.scheduler.utilities.DateManager.currentDayUTC;
import static prototype.xd.scheduler.utilities.DateManager.currentTimestampUTC;
import static prototype.xd.scheduler.utilities.DateManager.msToDays;
import static prototype.xd.scheduler.utilities.DateManager.msUTCtoDaysLocal;
import static prototype.xd.scheduler.utilities.ColorUtilities.getExpiredUpcomingColor;
import static prototype.xd.scheduler.utilities.ColorUtilities.mixColorWithBg;
import static prototype.xd.scheduler.utilities.Static.TIME_RANGE_SEPARATOR;
import static prototype.xd.scheduler.utilities.Utilities.doRangesOverlap;
import static prototype.xd.scheduler.utilities.Utilities.getPluralString;

import android.annotation.SuppressLint;
import android.content.Context;
import android.widget.TextView;

import androidx.annotation.ColorInt;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.collection.ArrayMap;
import androidx.collection.ArraySet;

import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.io.Serializable;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Objects;
import java.util.Set;
import java.util.function.BiConsumer;
import java.util.function.Function;
import java.util.regex.Pattern;

import prototype.xd.scheduler.BuildConfig;
import prototype.xd.scheduler.R;
import prototype.xd.scheduler.utilities.DateManager;
import prototype.xd.scheduler.utilities.Static;
import prototype.xd.scheduler.utilities.Logger;
import prototype.xd.scheduler.utilities.SArrayMap;
import prototype.xd.scheduler.utilities.Utilities;
import prototype.xd.scheduler.views.CalendarView;

@SuppressLint("UnknownNullness")
public class TodoEntry extends RecycleViewEntry implements Serializable {
    
    private static final long serialVersionUID = 3578172096594611826L;
    public static final String NAME = TodoEntry.class.getSimpleName();
    private static final Pattern hideByContentSplitPattern = Pattern.compile("\\|\\|");
    
    static class CachedGetter<T> {
        
        final ParameterGetter<T> parameterGetter;
        
        T value;
        boolean valid;
        
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
    public interface ParameterGetter<T> { // NOSONAR, will be confusing if replaced by UnaryOperator<T>
        T get(T previousValue);
        
        default T get() {
            return get(null);
        }
    }
    
    public static class Parameter<T> {
        
        @NonNull
        final TodoEntry entry;
        
        @NonNull
        final CachedGetter<T> todayCachedGetter;
        @NonNull
        final Function<String, T> loadedParameterConverter;
        @Nullable
        final
        CachedGetter<T> upcomingCachedGetter;
        @Nullable
        final
        CachedGetter<T> expiredCachedGetter;
        
        Parameter(@NonNull TodoEntry entry,
                  @NonNull String parameterKey,
                  @NonNull ParameterGetter<T> todayValueGetter,
                  @NonNull Function<String, T> loadedParameterConverter,
                  @Nullable ParameterGetter<T> upcomingValueGetter,
                  @Nullable ParameterGetter<T> expiredValueGetter) {
            this.entry = entry;
            upcomingCachedGetter = new CachedGetter<>(upcomingValueGetter);
            expiredCachedGetter = new CachedGetter<>(expiredValueGetter);
            todayCachedGetter = new CachedGetter<>(previousValue -> {
                // get parameter from group if it exists, if not, get from current parameters, if not, get from specified getter
                String paramValue = entry.params.getOrDefault(parameterKey, entry.getGroup().params.get(parameterKey));
                return paramValue != null ? loadedParameterConverter.apply(paramValue) : todayValueGetter.get();
            });
            this.loadedParameterConverter = loadedParameterConverter;
        }
        
        public T get(@NonNull EntryType entryType) {
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
    
    
    public enum EntryType {TODAY, GLOBAL, UPCOMING, EXPIRED, UNKNOWN}
    
    public static class TimeRange {
        
        public static final String NAME = TimeRange.class.getSimpleName();
        
        public static final TimeRange NullRange = new TimeRange(-1, -1);
        
        private boolean inDays;
        
        private long start;
        private long end;
        
        TimeRange(long startMsUTC, long endMsUTC) {
            start = startMsUTC;
            end = endMsUTC;
        }
        
        @NonNull
        public TimeRange toDays(boolean clone, boolean local) {
            if (clone) {
                return new TimeRange(start, end).toDays(false, local);
            }
            if (inDays) {
                Logger.warning(NAME, "Trying to convert range to days but it's already in days");
                return this;
            }
            start = local ? msUTCtoDaysLocal(start) : msToDays(start);
            end = local ? msUTCtoDaysLocal(end) : msToDays(end);
            inDays = true;
            return this;
        }
        
        public long getStart() {
            return start;
        }
        
        public long getEnd() {
            return end;
        }
        
        @NonNull
        @Override
        public String toString() {
            return "Time range in " + (inDays ? "days" : "ms") + " from " + start + " to " + end;
        }
    }
    
    @NonNull
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
    @Nullable
    private transient BiConsumer<TodoEntry, Set<String>> parameterInvalidationListener;
    
    @Nullable
    private transient TodoEntryList container;
    
    // ----------- supplementary stuff for sorting START
    int typeSortingIndex;
    
    public int getTypeSortingIndex() {
        return typeSortingIndex;
    }
    
    public void cacheTypeSortingIndex(long targetDayUTC, @NonNull List<EntryType> order) {
        typeSortingIndex = order.indexOf(getEntryType(targetDayUTC));
        if (typeSortingIndex == -1) {
            // fallback to treating like today's
            typeSortingIndex = order.indexOf(EntryType.TODAY);
        }
    }
    
    long cachedNearestStartMsUTC;
    
    public long getCachedNearestStartMsUTC() {
        return cachedNearestStartMsUTC;
    }
    
    // obtain nearest start ms near a particular day for use in sorting later
    public void cacheNearestStartMsUTC(long targetDayLocal) {
        cachedNearestStartMsUTC = getNearestCalendarEventMsRangeUTC(targetDayLocal).getStart();
    }
    // ----------- supplementary stuff for sorting END
    
    @NonNull
    private static ArrayMap<String, Parameter<?>> mapParameters(@NonNull TodoEntry entry) {
        ArrayMap<String, Parameter<?>> parameterMap = new ArrayMap<>(8);
        parameterMap.put(Static.BG_COLOR.CURRENT.key, entry.bgColor);
        parameterMap.put(Static.FONT_COLOR.CURRENT.key, entry.fontColor);
        parameterMap.put(Static.BORDER_COLOR.CURRENT.key, entry.borderColor);
        parameterMap.put(Static.BORDER_THICKNESS.CURRENT.key, entry.borderThickness);
        parameterMap.put(Static.PRIORITY.key, entry.priority);
        parameterMap.put(Static.EXPIRED_ITEMS_OFFSET.key, entry.expiredDayOffset);
        parameterMap.put(Static.UPCOMING_ITEMS_OFFSET.key, entry.upcomingDayOffset);
        parameterMap.put(Static.ADAPTIVE_COLOR_BALANCE.key, entry.adaptiveColorBalance);
        return parameterMap;
    }
    
    public TodoEntry(@NonNull SystemCalendarEvent event) {
        this.event = event;
        
        initParameters();
        assignRecyclerViewId(event.hashCode());
        
        event.linkEntry(this); // NOSONAR
    }
    
    public TodoEntry(@NonNull SArrayMap<String, String> params, @NonNull Group group, long id) {
        this.params = params;
        this.group = group;
        assignRecyclerViewId(id);
        group.attachEntryInternal(this); // NOSONAR, make sure to call assignRecyclerViewId before attachEntryInternal though
        initParameters();
    }
    
    // ------------ serialization
    @SuppressWarnings("unchecked")
    private void readObject(@NonNull ObjectInputStream in)
            throws IOException, ClassNotFoundException {
        params = (SArrayMap<String, String>) in.readObject();
        tempGroupName = (String) in.readObject();
        initParameters();
    }
    
    
    private void writeObject(@NonNull ObjectOutputStream out) throws IOException {
        out.writeObject(params);
        // group is only null when we export settings
        out.writeObject(group == null ? tempGroupName : group.getRawName());
    }
    // ------------
    
    public void initGroupAndId(@NonNull List<Group> groups, long id) {
        // id should be assigned before attaching to group
        assignRecyclerViewId(id);
        group = Group.findGroupInList(groups, tempGroupName);
        group.attachEntryInternal(this);
    }
    
    public void initParameters() {
        bgColor = new Parameter<>(this, Static.BG_COLOR.CURRENT.key,
                previousValue -> {
                    if (isFromSystemCalendar()) {
                        return Static.BG_COLOR.CURRENT.getOnlyBySubKeys(event.subKeys,
                                Static.SETTINGS_DEFAULT_CALENDAR_EVENT_BG_COLOR.applyAsInt(getCalendarEventColor()));
                    }
                    return Static.BG_COLOR.CURRENT.get();
                },
                Integer::parseInt,
                todayValue -> getExpiredUpcomingColor(todayValue, Static.BG_COLOR.UPCOMING.get()),
                todayValue -> getExpiredUpcomingColor(todayValue, Static.BG_COLOR.EXPIRED.get()));
        fontColor = new Parameter<>(this, Static.FONT_COLOR.CURRENT.key,
                previousValue -> Static.FONT_COLOR.CURRENT.get(getSubKeys()),
                Integer::parseInt,
                todayValue -> getExpiredUpcomingColor(todayValue, Static.FONT_COLOR.UPCOMING.get()),
                todayValue -> getExpiredUpcomingColor(todayValue, Static.FONT_COLOR.EXPIRED.get()));
        borderColor = new Parameter<>(this, Static.BORDER_COLOR.CURRENT.key,
                previousValue -> Static.BORDER_COLOR.CURRENT.get(getSubKeys()),
                Integer::parseInt,
                todayValue -> getExpiredUpcomingColor(todayValue, Static.BORDER_COLOR.UPCOMING.get()),
                todayValue -> getExpiredUpcomingColor(todayValue, Static.BORDER_COLOR.EXPIRED.get()));
        borderThickness = new Parameter<>(this, Static.BORDER_THICKNESS.CURRENT.key,
                previousValue -> Static.BORDER_THICKNESS.CURRENT.get(getSubKeys()),
                Integer::parseInt,
                todayValue -> Static.BORDER_THICKNESS.UPCOMING.get(),
                todayValue -> Static.BORDER_THICKNESS.EXPIRED.get());
        priority = new Parameter<>(this, Static.PRIORITY.key,
                previousValue -> isFromSystemCalendar() ?
                        Static.PRIORITY.get(event.subKeys) :
                        // there is no default setting for priority
                        Static.PRIORITY.defaultValue,
                Integer::parseInt,
                null,
                null);
        expiredDayOffset = new Parameter<>(this, Static.EXPIRED_ITEMS_OFFSET.key,
                previousValue -> Static.EXPIRED_ITEMS_OFFSET.get(getSubKeys()),
                Integer::parseInt,
                null,
                null);
        upcomingDayOffset = new Parameter<>(this, Static.UPCOMING_ITEMS_OFFSET.key,
                previousValue -> Static.UPCOMING_ITEMS_OFFSET.get(getSubKeys()),
                Integer::parseInt,
                null,
                null);
        adaptiveColorBalance = new Parameter<>(this, Static.ADAPTIVE_COLOR_BALANCE.key,
                previousValue -> Static.ADAPTIVE_COLOR_BALANCE.get(getSubKeys()),
                Integer::parseInt,
                null,
                null);
        
        parameterMap = mapParameters(this);
        
        startDayLocal = previousValue ->
                isFromSystemCalendar() ? event.startDayLocal : Long.parseLong(params.getOrDefault(Static.START_DAY_UTC, Static.DAY_FLAG_GLOBAL_STR));
        endDayLocal = previousValue ->
                isFromSystemCalendar() ? event.endDayLocal : Long.parseLong(params.getOrDefault(Static.END_DAY_UTC, Static.DAY_FLAG_GLOBAL_STR));
        rawTextValue = previousValue ->
                isFromSystemCalendar() ? event.data.title : params.getOrDefault(Static.TEXT_VALUE, "");
    }
    
    // get calendar key for calendar entries and null for normal entries
    @Nullable
    private List<String> getSubKeys() {
        return isFromSystemCalendar() ? event.subKeys : null;
    }
    
    public int getCalendarEventColor() {
        return isFromSystemCalendar() ? event.data.color : 0;
    }
    
    public void listenToParameterInvalidations(@NonNull BiConsumer<TodoEntry, Set<String>> parameterInvalidationListener) {
        if (this.parameterInvalidationListener != null) {
            Logger.warning(NAME, this + " already has a parameterInvalidationListener, double assign");
        }
        this.parameterInvalidationListener = parameterInvalidationListener;
    }
    
    public void stopListeningToParameterInvalidations() {
        parameterInvalidationListener = null;
    }
    
    protected void linkToContainer(@NonNull TodoEntryList todoEntryList) {
        if (container != null) {
            Logger.warning(NAME, this + " already has a container, double linking");
        }
        container = todoEntryList;
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
            Logger.warning(NAME, this + " is not in a container, can't remove");
        } else {
            if (!container.remove(this)) {
                Logger.warning(NAME, this + " error removing from the container");
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
        // days change on timezone changes, so we update them
        event.computeEventVisibilityDays();
    }
    
    @NonNull
    public Group getGroup() {
        return group == null ? NULL_GROUP : group;
    }
    
    public boolean hasNullGroup() {
        return group == null || group.isNullGroup();
    }
    
    @NonNull
    public String getRawGroupName() {
        return getGroup().getRawName();
    }
    
    @NonNull
    public String getRawTextValue() {
        return rawTextValue.get();
    }
    
    protected void unlinkGroupInternal(boolean invalidate) {
        if (hasNullGroup()) {
            return;
        }
        Set<String> parameters = null;
        if (invalidate) {
            parameters = getGroup().getParameterKeys();
        }
        group = NULL_GROUP;
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
    @SuppressWarnings("BooleanMethodNameMustStartWithQuestion")
    public boolean changeGroup(@NonNull Group newGroup) {
        if (newGroup.equals(group)) {
            return false;
        }
    
        getGroup().detachEntryInternal(this);
        newGroup.attachEntryInternal(this);
        
        Set<String> changedKeys = Utilities.getChangedKeys(group.params, newGroup.params);
        
        Logger.debug(NAME, "Changed group of " + this + " from " + group + " to " + newGroup);
        
        group = newGroup;
        // invalidate only after group change to avoid weird settings from cache
        invalidateParameters(changedKeys);
        
        return true;
    }
    
    protected void invalidateParameter(@NonNull String parameterKey, boolean reportInvalidated) {
        Parameter<?> param = parameterMap.get(parameterKey);
        if (param != null) {
            param.invalidate();
        }
        if (parameterInvalidationListener != null && reportInvalidated) {
            parameterInvalidationListener.accept(this, Collections.singleton(parameterKey));
        }
    }
    
    protected void invalidateParameters(@NonNull Set<String> parameterKeys) {
        if (parameterKeys.isEmpty()) {
            return;
        }
        parameterKeys.forEach(parameterKey -> invalidateParameter(parameterKey, false));
        if (parameterInvalidationListener != null) {
            parameterInvalidationListener.accept(this, parameterKeys);
        }
    }
    
    public void invalidateAllParameters(boolean reportInvalidated) {
        parameterMap.forEach((key, parameter) -> parameter.invalidate());
        if (parameterInvalidationListener != null && reportInvalidated) {
            parameterInvalidationListener.accept(this, parameterMap.keySet());
        }
    }
    
    /**
     * @return all parameters except for TEXT_VALUE START_DAY_UTC, END_DAY_UTC and completion status
     */
    @NonNull
    public SArrayMap<String, String> getDisplayParams() {
        // shallow copy map
        SArrayMap<String, String> displayParams = new SArrayMap<>(params);
        // remove not display parameters
        displayParams.removeAll(Arrays.asList(Static.TEXT_VALUE, Static.START_DAY_UTC, Static.END_DAY_UTC, Static.IS_COMPLETED));
        return displayParams;
    }
    
    /**
     * Removes all display parameters and leaves just TEXT_VALUE START_DAY_UTC, END_DAY_UTC and completion status
     *
     * @return true if there were any display parameters
     */
    @SuppressWarnings("BooleanMethodNameMustStartWithQuestion")
    public boolean removeDisplayParams() {
        invalidateAllParameters(true);
        return params.retainAll(Arrays.asList(Static.TEXT_VALUE, Static.START_DAY_UTC, Static.END_DAY_UTC, Static.IS_COMPLETED));
    }
    
    /**
     * Changes one parameter
     *
     * @param key   parameter to be changed
     * @param value new parameter value
     * @return true if the parameter was changed
     */
    @SuppressWarnings("BooleanMethodNameMustStartWithQuestion")
    private boolean changeParameterInternal(@NonNull String key, @NonNull String value) {
        return !Objects.equals(params.put(key, value), value);
    }
    
    /**
     * Changes any number of parameters, should be listed as [key, value, key, value]
     *
     * @param keyValuePairs parameters to change with their bew values
     */
    public void changeParameters(@NonNull String... keyValuePairs) {
        if (keyValuePairs.length % 2 != 0) {
            throw new IllegalArgumentException("Can't call changeParameters with odd number of arguments");
        }
        // just one parameter
        if (keyValuePairs.length == 2) {
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
    
    private boolean isInRange(long targetDayUTC, @NonNull TimeRange instanceRange) {
        return isInRange(targetDayUTC, instanceRange.getStart(), instanceRange.getEnd());
    }
    
    private boolean isInRange(long targetDayUTC, long instanceStartDayLocal, long instanceEndDayLocal) {
        return instanceStartDayLocal - upcomingDayOffset.getToday() <= targetDayUTC &&
                targetDayUTC <= instanceEndDayLocal + expiredDayOffset.getToday();
    }
    
    public boolean isCompleted() {
        // everything else except "true" is false, so we are fine here
        return Boolean.parseBoolean(params.get(Static.IS_COMPLETED));
    }
    
    public boolean isVisibleOnLockscreenToday() {
        if (isFromSystemCalendar()) {
            if (isHiddenByContent() || !Static.CALENDAR_SHOW_ON_LOCK.get(event.subKeys)) {
                return false;
            }
            
            if (!event.isAllDay() && Static.HIDE_EXPIRED_ENTRIES_BY_TIME.get(event.subKeys)) {
                return isVisibleExact(currentDayUTC, currentTimestampUTC);
            }
        } else if (isGlobal()) {
            return Static.SHOW_GLOBAL_ITEMS_LOCK.get();
        }
        
        return !isCompleted() && isVisible(currentDayUTC);
    }
    
    private boolean isVisible(long targetDayUTC) {
        // global entries are visible everywhere, no need to do anything more
        if (isGlobal()) {
            return true;
        }
        
        if (container != null) {
            return container.isNotGlobalEntryVisibleOnDay(this, targetDayUTC);
        }
        
        // fallback to iterating the recurrence set
        return isInRange(targetDayUTC, getNearestLocalEventDayRange(targetDayUTC));
    }
    
    private boolean isVisibleExact(long targetDayUTC, long targetMsUTC) {
        // global entries are visible everywhere, no need to do anything more
        if (isGlobal()) {
            return true;
        }
        
        if (!isFromSystemCalendar()) {
            // local events don't have timestamps, just check one range
            return isInRange(targetDayUTC, startDayLocal.get(), endDayLocal.get());
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
            if (!container.isNotGlobalEntryVisibleOnDay(this, targetDayUTC)) {
                // well, it's not visible
                return false;
            }
        }
        
        TimeRange nearestMsRangeUTC = getNearestCalendarEventMsRangeUTC(targetDayUTC);
        
        return isInRange(targetDayUTC, nearestMsRangeUTC.toDays(true, !event.isAllDay())) &&
                nearestMsRangeUTC.getEnd() >= targetMsUTC;
    }
    
    private static void addDayRangeToSet(long dayFrom, long dayTo,
                                         final long minDay, final long maxDay,
                                         @Nullable Set<Long> daySet) {
        if (daySet != null && doRangesOverlap(dayFrom, dayTo, minDay, maxDay)) {
            dayFrom = clamp(dayFrom, minDay, maxDay);
            dayTo = clamp(dayTo, minDay, maxDay);
            for (long day = dayFrom; day <= dayTo; day++) {
                daySet.add(day);
            }
        }
    }
    
    private static void addDayRangeToSets(long startDay, long endDay,
                                          long minDay, long maxDay,
                                          long expiredDayOffset, long upcomingDayOffset,
                                          @Nullable Set<Long> coreDaySet,
                                          @Nullable Set<Long> currentUpcomingExpiredSet) {
        
        addDayRangeToSet(
                startDay, endDay,
                minDay, maxDay,
                coreDaySet);
        
        addDayRangeToSet(
                startDay - upcomingDayOffset, endDay + expiredDayOffset,
                minDay, maxDay,
                currentUpcomingExpiredSet);
    }
    
    // get on what days from min to max an entry is visible (and was before invalidation)
    private void getVisibleDays(long minDay, long maxDay,
                                @Nullable Set<Long> coreDaySet,
                                @Nullable Set<Long> upcomingExpiredSet) {
        
        // we don't care about global entries, they are handled differently
        if (isGlobal()) {
            return;
        }
        
        long currentUpcomingDayOffset = upcomingDayOffset.getToday();
        long currentExpiredDayOffset = expiredDayOffset.getToday();
        
        if (isRecurring()) {
            event.iterateRecurrenceSet(minDay, (instanceStartMsUTC, instanceEndMsUTC, instanceStartDayLocal, instanceEndDayLocal) -> {
                
                // overshot
                if (instanceStartDayLocal + currentUpcomingDayOffset > maxDay) {
                    return Boolean.FALSE;
                }
                
                addDayRangeToSets(instanceStartDayLocal, instanceEndDayLocal,
                        minDay, maxDay,
                        // offsets
                        currentExpiredDayOffset, currentUpcomingDayOffset,
                        // sets
                        coreDaySet,
                        upcomingExpiredSet);
                
                return null;
            }, Boolean.FALSE);
        } else {
            addDayRangeToSets(startDayLocal.get(), endDayLocal.get(),
                    minDay, maxDay,
                    currentExpiredDayOffset, currentUpcomingDayOffset,
                    coreDaySet, upcomingExpiredSet);
        }
    }
    
    public enum RangeType {
        CORE, EXPIRED_UPCOMING
    }
    
    public void getVisibleDaysOnCalendar(@NonNull final CalendarView calendarView,
                                         @NonNull final Set<Long> daySet,
                                         @NonNull RangeType rangeType) {
        getVisibleDays(calendarView.getFirstLoadedDayUTC(), calendarView.getLastLoadedDayUTC(),
                rangeType == RangeType.CORE ? daySet : null,
                rangeType == RangeType.EXPIRED_UPCOMING ? daySet : null);
    }
    
    // return on what days from min to max an entry is visible (and was before invalidation)
    @NonNull
    public Set<Long> getVisibleDaysOnCalendar(@NonNull final CalendarView calendarView,
                                              @NonNull final RangeType rangeType) {
        // we don't care about global entries, they are handled differently
        if (isGlobal()) {
            return Collections.emptySet();
        }
        Set<Long> daySet = new ArraySet<>();
        getVisibleDaysOnCalendar(calendarView, daySet, rangeType);
        return daySet;
    }
    
    @NonNull
    public FullDaySet getFullDaySet(long minDay, long maxDay) {
        return new FullDaySet(this, minDay, maxDay);
    }
    
    static class FullDaySet {
        
        private final Set<Long> upcomingExpiredDaySet = new ArraySet<>();
        private final Set<Long> coreDaySet = new ArraySet<>();
        
        FullDaySet(@NonNull TodoEntry entry, long dayStart, long dayEnd) {
            entry.getVisibleDays(dayStart, dayEnd,
                    coreDaySet,
                    upcomingExpiredDaySet);
        }
        
        @NonNull
        public Set<Long> getUpcomingExpiredDaySet() {
            return upcomingExpiredDaySet;
        }
        
        @NonNull
        public Set<Long> getCoreDaySet() {
            return coreDaySet;
        }
    }
    
    public boolean isHiddenByContent() {
        if (!isFromSystemCalendar()) {
            return false;
        }
        if (Static.HIDE_ENTRIES_BY_CONTENT.get(event.subKeys)) {
            String matchString = Static.HIDE_ENTRIES_BY_CONTENT_CONTENT.get(event.subKeys);
            if (matchString.isEmpty()) {
                return false;
            }
            String[] split = hideByContentSplitPattern.split(matchString);
            for (String str : split) {
                if (rawTextValue.get().contains(str)) {
                    return true;
                }
            }
        }
        return false;
    }
    
    private TimeRange getNearestCalendarEventMsRangeUTC(long targetDayUTC) {
        if (!isRecurring()) {
            // covers local and non-recurring calendar events
            return new TimeRange(event.startMsUTC, event.endMsUTC);
        }
        // covers recurring calendar events
        return event.iterateRecurrenceSet(targetDayUTC, (instanceStartMsUTC, instanceEndMsUTC, instanceStartDayLocal, instanceEndDayLocal) -> {
            // if in range or overshoot
            if (isInRange(targetDayUTC, instanceStartDayLocal, instanceEndDayLocal) || instanceStartDayLocal >= targetDayUTC) {
                return new TimeRange(instanceStartMsUTC, instanceEndMsUTC);
            }
            return null;
        }, TimeRange.NullRange);
    }
    
    private TimeRange getNearestLocalEventDayRange(long targetDayUTC) {
        if (!isRecurring()) {
            // covers local and non-recurring calendar events
            return new TimeRange(startDayLocal.get(), endDayLocal.get());
        }
        // covers recurring calendar events
        return getNearestCalendarEventMsRangeUTC(targetDayUTC).toDays(false, !event.isAllDay());
    }
    
    @NonNull
    public EntryType getEntryType(long targetDayUTC) {
        
        if (container != null) {
            return container.getEntryType(this, targetDayUTC);
        }
        
        if (isGlobal()) {
            return EntryType.GLOBAL;
        }
        
        // in case of regular entry this will return startDay - endDay
        TimeRange nearestDayRange = getNearestLocalEventDayRange(targetDayUTC);
        
        if (nearestDayRange.getStart() <= targetDayUTC && targetDayUTC <= nearestDayRange.getEnd()) {
            return EntryType.TODAY;
        }
        
        // extended range covers but still less than nearestStartDayLocal
        if (targetDayUTC + upcomingDayOffset.getToday() >= nearestDayRange.getStart()
                && targetDayUTC < nearestDayRange.getStart()) {
            return EntryType.UPCOMING;
        }
        
        // NOTE: for regular entries durationDays = 0
        // extended range covers but more than nearestStartDayLocal
        if (targetDayUTC - expiredDayOffset.getToday() <= nearestDayRange.getEnd()
                && targetDayUTC > nearestDayRange.getEnd()) {
            return EntryType.EXPIRED;
        }
        
        Logger.warning(NAME, "Type of " + this + " is unknown on day: " + targetDayUTC);
        return EntryType.UNKNOWN;
    }
    
    public boolean isGlobal() {
        return startDayLocal.get() == Static.DAY_FLAG_GLOBAL;
    }
    
    @NonNull
    public String getCalendarEntryTimeSpan(@NonNull Context context, long targetDayUTC) {
        if (event.isAllDay()) {
            return context.getString(R.string.calendar_event_all_day);
        }
        
        return DateManager.getTimeSpan(getNearestCalendarEventMsRangeUTC(targetDayUTC));
    }
    
    public boolean isAdaptiveColorEnabled() {
        return adaptiveColorBalance.getToday() > 0;
    }
    
    public int getAdaptiveColor(@ColorInt int inputColor) {
        return mixColorWithBg(inputColor, averageBackgroundColor, adaptiveColorBalance.getToday());
    }
    
    @NonNull
    public String getTextOnDay(long targetDayUTC, @NonNull Context context, boolean displayGlobalLabel) {
        return rawTextValue.get() + " " + getDayOffset(targetDayUTC, context, displayGlobalLabel);
    }
    
    @NonNull
    public String getDayOffset(long targetDayUTC, @NonNull Context context, boolean displayGlobalLabel) { // NOSONAR, not that complex
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
                return getPluralString(context, dayShift > 0 ? R.plurals.item_in_N_days : R.plurals.item_N_days_ago, dayShiftAbs);
            }
        } else {
            return context.getString(dayShift > 0 ? R.string.item_in_more_than_in_a_month : R.string.item_more_than_a_month_ago);
        }
    }
    
    public void setStateIconColor(@NonNull TextView icon, @NonNull String parameter) {
        boolean containedInGroupParams = getGroup().params.containsKey(parameter);
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
    
    // only callable after a call to cacheNearestStartMsUTC()
    public int getInstanceHash() {
        return Objects.hash(event.data.title, cachedNearestStartMsUTC, event.durationMs);
    }
    
    @Override
    public int getRecyclerViewType() {
        return isFromSystemCalendar() ? 1 : 0;
    }
    
    public int getLockscreenHash() {
        return Objects.hash(event, params, group);
    }
    
    @NonNull
    @Override
    public String toString() {
        String str = NAME + ": ";
        if (isFromSystemCalendar()) {
            str += "[" + event + "]";
        } else {
            str += BuildConfig.DEBUG ? rawTextValue.get() : rawTextValue.get().hashCode();
        }
        return str + " (" + startDayLocal.get() + TIME_RANGE_SEPARATOR + endDayLocal.get() + ")";
    }
}
