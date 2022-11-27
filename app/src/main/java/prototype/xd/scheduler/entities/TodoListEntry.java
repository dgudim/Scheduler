package prototype.xd.scheduler.entities;

import static android.util.Log.WARN;
import static androidx.core.math.MathUtils.clamp;
import static java.lang.Math.max;
import static prototype.xd.scheduler.utilities.BitmapUtilities.mixTwoColors;
import static prototype.xd.scheduler.utilities.DateManager.currentDay;
import static prototype.xd.scheduler.utilities.DateManager.currentTimestamp;
import static prototype.xd.scheduler.utilities.DateManager.datetimeFromEpoch;
import static prototype.xd.scheduler.utilities.DateManager.daysFromEpoch;
import static prototype.xd.scheduler.utilities.Logger.log;
import static prototype.xd.scheduler.utilities.PreferencesStore.preferences;
import static prototype.xd.scheduler.utilities.SystemCalendarUtils.getFirstValidKey;
import static prototype.xd.scheduler.utilities.Utilities.addDayRangeToSet;

import android.content.Context;
import android.util.ArrayMap;
import android.util.ArraySet;
import android.widget.TextView;

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
import java.util.TimeZone;
import java.util.function.Function;

import prototype.xd.scheduler.R;
import prototype.xd.scheduler.utilities.DateManager;
import prototype.xd.scheduler.utilities.Keys;
import prototype.xd.scheduler.utilities.SArrayMap;

public class TodoListEntry extends RecycleViewEntry implements Serializable {
    
    static class CachedGetter<T> {
        
        ParameterGetter<T> parameterGetter;
        
        T discardedValue;
        T value;
        boolean valid = false;
        
        CachedGetter(ParameterGetter<T> parameterGetter) {
            this.parameterGetter = parameterGetter;
        }
        
        void invalidate() {
            valid = false;
            discardedValue = value;
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
        
        // only makes sense to call after a call to 'invalidate'
        T getDiscardedValue(T defaultValue) {
            return discardedValue == null ? defaultValue : discardedValue;
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
        
        public T getDiscarded(T defaultValue) {
            return todayCachedGetter.getDiscardedValue(defaultValue);
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
    
    public enum EntryType {TODAY, EXPIRED, UPCOMING, UNKNOWN}
    
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
    
    private transient ParameterGetter<Long> startDay;
    private transient ParameterGetter<Long> endDay;
    private transient ParameterGetter<Long> durationDays;
    
    public ParameterGetter<String> rawTextValue;
    
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
                previousValue -> preferences.getInt(getAppropriateKey(Keys.BG_COLOR), Keys.SETTINGS_DEFAULT_BG_COLOR),
                Integer::parseInt,
                todayValue -> mixTwoColors(todayValue,
                        preferences.getInt(Keys.UPCOMING_BG_COLOR, Keys.SETTINGS_DEFAULT_UPCOMING_BG_COLOR), Keys.DEFAULT_COLOR_MIX_FACTOR),
                todayValue -> mixTwoColors(todayValue,
                        preferences.getInt(Keys.EXPIRED_BG_COLOR, Keys.SETTINGS_DEFAULT_EXPIRED_BG_COLOR), Keys.DEFAULT_COLOR_MIX_FACTOR));
        fontColor = new Parameter<>(this, Keys.FONT_COLOR,
                previousValue -> preferences.getInt(getAppropriateKey(Keys.FONT_COLOR), Keys.SETTINGS_DEFAULT_FONT_COLOR),
                Integer::parseInt,
                todayValue -> mixTwoColors(todayValue,
                        preferences.getInt(Keys.UPCOMING_FONT_COLOR, Keys.SETTINGS_DEFAULT_UPCOMING_FONT_COLOR), Keys.DEFAULT_COLOR_MIX_FACTOR),
                todayValue -> mixTwoColors(todayValue,
                        preferences.getInt(Keys.EXPIRED_FONT_COLOR, Keys.SETTINGS_DEFAULT_EXPIRED_FONT_COLOR), Keys.DEFAULT_COLOR_MIX_FACTOR));
        borderColor = new Parameter<>(this, Keys.BORDER_COLOR,
                previousValue -> preferences.getInt(getAppropriateKey(Keys.BORDER_COLOR), Keys.SETTINGS_DEFAULT_BORDER_COLOR),
                Integer::parseInt,
                todayValue -> mixTwoColors(todayValue,
                        preferences.getInt(Keys.UPCOMING_BORDER_COLOR, Keys.SETTINGS_DEFAULT_UPCOMING_BORDER_COLOR), Keys.DEFAULT_COLOR_MIX_FACTOR),
                todayValue -> mixTwoColors(todayValue,
                        preferences.getInt(Keys.EXPIRED_BORDER_COLOR, Keys.SETTINGS_DEFAULT_EXPIRED_BORDER_COLOR), Keys.DEFAULT_COLOR_MIX_FACTOR));
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
        
        startDay = previousValue ->
                isFromSystemCalendar() ? event.startDay : Long.parseLong(params.getOrDefault(Keys.ASSOCIATED_DAY, Keys.DAY_FLAG_GLOBAL_STR));
        endDay = previousValue ->
                isFromSystemCalendar() ? event.endDay : Long.parseLong(params.getOrDefault(Keys.ASSOCIATED_DAY, Keys.DAY_FLAG_GLOBAL_STR));
        durationDays = previousValue ->
                isFromSystemCalendar() ? event.durationDays : 0;
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
    
    protected void unlinkFromCalendarEvent() {
        if (isFromSystemCalendar()) {
            event.unlinkEntry();
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
    
    protected void unlinkGroupInternal(boolean invalidate) {
        if (invalidate && group != null) {
            invalidateParameters(group.params.keySet());
        }
        group = null;
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
        
        Set<String> parameterKeys = new ArraySet<>();
        
        if (group != null) {
            group.detachEntryInternal(this);
            // invalidate parameters from previous group
            parameterKeys.addAll(group.params.keySet());
        }
        
        if (newGroup != null) {
            newGroup.attachEntryInternal(this);
            // invalidate parameters from new group
            parameterKeys.addAll(newGroup.params.keySet());
        }
        
        invalidateParameters(parameterKeys);
        
        group = newGroup;
        return true;
    }
    
    public void invalidateParameter(String parameterKey, boolean reportInvalidated) {
        Parameter<?> param = parameterMap.get(parameterKey);
        if (param != null) {
            param.invalidate();
        }
        if (parameterInvalidationListener != null && reportInvalidated) {
            parameterInvalidationListener.parametersInvalidated(this, Collections.singleton(parameterKey));
        }
    }
    
    public void invalidateParameters(Set<String> parameterKeys) {
        parameterKeys.forEach(parameterKey -> invalidateParameter(parameterKey, false));
        parameterInvalidationListener.parametersInvalidated(this, parameterKeys);
    }
    
    public void invalidateAllParameters(boolean reportInvalidated) {
        parameterMap.forEach((key, parameter) -> parameter.invalidate());
        if(reportInvalidated) {
            parameterInvalidationListener.parametersInvalidated(this, parameterMap.keySet());
        }
    }
    
    public SArrayMap<String, String> getDisplayParams() {
        // shallow copy map
        SArrayMap<String, String> displayParams = new SArrayMap<>(params);
        // remove not display parameters
        displayParams.removeAll(Arrays.asList(Keys.TEXT_VALUE, Keys.ASSOCIATED_DAY, Keys.IS_COMPLETED));
        return displayParams;
    }
    
    public void removeDisplayParams() {
        invalidateAllParameters(true);
        params.retainAll(Arrays.asList(Keys.TEXT_VALUE, Keys.ASSOCIATED_DAY, Keys.IS_COMPLETED));
    }
    
    public void changeParameter(String name, String value) {
        // if previous value does not equal new one
        if (!Objects.equals(params.put(name, value), value)) {
            invalidateParameter(name, true);
        }
    }
    
    public <T> T getRawParameter(String parameter, Function<String, T> converter) {
        return converter.apply(params.get(parameter));
    }
    
    public void setAverageBackgroundColor(int averageBackgroundColor) {
        this.averageBackgroundColor = averageBackgroundColor;
    }
    
    private boolean inRange(long day, long instanceStartDay) {
        return isGlobal() ||
                (day >= instanceStartDay - upcomingDayOffset.getToday() &&
                        day <= instanceStartDay + durationDays.get() + expiredDayOffset.getToday());
        // | days after the event ended ------ event start |event| event end ------ days before the event starts |
        // | ++++++++++++++++++++++++++       -------------|-----|----------        +++++++++++++++++++++++++++++|
    }
    
    public boolean isCompleted() {
        // everything else except "true" is false, so we are fine here
        return Boolean.parseBoolean(params.get(Keys.IS_COMPLETED));
    }
    
    public boolean isVisibleOnLockscreenToday() {
        String visibleOnLockscreen = params.get(Keys.SHOW_ON_LOCK);
        if (visibleOnLockscreen != null) {
            return Boolean.parseBoolean(visibleOnLockscreen) && !isCompleted();
        }
        if (isFromSystemCalendar()) {
            if (!hideByContent()) {
                boolean showOnLock;
                if (!isAllDay && preferences.getBoolean(Keys.HIDE_EXPIRED_ENTRIES_BY_TIME, Keys.SETTINGS_DEFAULT_HIDE_EXPIRED_ENTRIES_BY_TIME)) {
                    showOnLock = isVisibleExact(currentTimestamp);
                } else {
                    showOnLock = isVisible(currentDay);
                }
                return showOnLock && preferences.getBoolean(getFirstValidKey(event.subKeys, Keys.SHOW_ON_LOCK), Keys.CALENDAR_SETTINGS_DEFAULT_SHOW_ON_LOCK);
            }
            return false;
        } else {
            if (isGlobal()) {
                return preferences.getBoolean(Keys.SHOW_GLOBAL_ITEMS_LOCK, Keys.SETTINGS_DEFAULT_SHOW_GLOBAL_ITEMS_LOCK);
            }
            return !isCompleted() && isVisible(currentDay);
        }
    }
    
    public boolean visibleInList(long day) {
        boolean show = isVisible(day);
        EntryType entryType = getEntryType(day);
        if (entryType == EntryType.EXPIRED || entryType == EntryType.UPCOMING) {
            return show && !isCompleted();
        } else {
            return show;
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
    
    private <T> T iterateRecurrenceSet(long startMs, TimeZone startTimezone, RecurrenceSetConsumer<T> recurrenceSetConsumer, T defaultValue) {
        RecurrenceSetIterator it = recurrenceSet.iterator(startTimezone, startMs);
        long instanceMsUTC;
        long instanceDay;
        T val;
        while (it.hasNext()) {
            instanceMsUTC = it.next();
            instanceDay = daysFromEpoch(instanceMsUTC, event.timeZone);
            val = recurrenceSetConsumer.processInstance(instanceMsUTC, instanceDay);
            if (val != null) {
                return val;
            }
        }
        return defaultValue;
    }
    
    public boolean isVisible(long targetDay) {
        if (recurrenceSet != null) {
            // already overshot
            if (targetDay > endDay.get()) {
                return false;
            }
            return iterateRecurrenceSet(startMsUTC, event.timeZone, (instanceStartMsUTC, instanceStartDay) -> {
                // we overshot
                if (instanceStartDay + upcomingDayOffset.getToday() > targetDay) {
                    return false;
                }
                // check if we fall in range
                if (inRange(targetDay, instanceStartDay)) {
                    return true;
                }
                return null;
            }, false);
        }
        return inRange(targetDay, startDay.get());
    }
    
    public boolean isVisibleExact(long targetTimestamp) {
        long targetDay = daysFromEpoch(targetTimestamp, event.timeZone);
        if (recurrenceSet != null) {
            if (targetTimestamp > endMsUTC + durationMsUTC) {
                return false;
            }
            return iterateRecurrenceSet(startMsUTC, event.timeZone, (instanceStartMsUTC, instanceStartDay) -> {
                // we overshot
                if (instanceStartDay + upcomingDayOffset.getToday() > targetDay) {
                    return false;
                }
                if (inRange(targetDay, instanceStartDay)) {
                    return instanceStartMsUTC + durationMsUTC >= targetTimestamp;
                }
                return null;
            }, false);
        }
        return inRange(targetDay, startDay.get()) && (startMsUTC + durationMsUTC >= targetTimestamp);
    }
    
    // get on what days from min to max an entry is visible (and was before invalidation)
    public void addVisibleDays(long minDay, long maxDay, Set<Long> daySet) {
        // get max between previous range and current
        long maxUpcomingDayOffset = max(this.upcomingDayOffset.getDiscarded(0), this.upcomingDayOffset.getToday());
        long maxExpiredDayOffset = max(this.expiredDayOffset.getDiscarded(0), this.expiredDayOffset.getToday());
        
        if (recurrenceSet != null) {
            iterateRecurrenceSet(startMsUTC, event.timeZone, (instanceStartMsUTC, instanceStartDay) -> {
                long instanceEndDay = instanceStartDay + durationDays.get() + maxExpiredDayOffset;
                instanceStartDay -= maxUpcomingDayOffset;
                // if any of the event days lies between min and max days
                if ((minDay <= instanceStartDay && instanceStartDay <= maxDay) ||
                        (minDay <= instanceEndDay && instanceEndDay <= maxDay)) {
                    
                    addDayRangeToSet(clamp(instanceStartDay, minDay, maxDay), clamp(instanceEndDay, minDay, maxDay), daySet);
                } else if (instanceStartDay - maxUpcomingDayOffset >= maxDay) {
                    return false;
                }
                return null;
            }, false);
        } else {
            addDayRangeToSet(startDay.get() - maxUpcomingDayOffset, endDay.get() + maxExpiredDayOffset, daySet);
        }
    }
    
    // return on what days from min to max an entry is visible (and was before invalidation)
    public Set<Long> getVisibleDays(long minDay, long maxDay) {
        Set<Long> daySet = new ArraySet<>();
        addVisibleDays(minDay, maxDay, daySet);
        return daySet;
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
    
    public long getNearestEventTimestamp(long targetDay) {
        if (recurrenceSet != null) {
            if (targetDay >= endDay.get()) {
                return endMsUTC;
            }
            return iterateRecurrenceSet(startMsUTC, event.timeZone, (instanceStartMsUTC, instanceStartDay) -> {
                // if in range or overshoot
                if (inRange(targetDay, instanceStartDay) || instanceStartDay >= targetDay) {
                    return instanceStartMsUTC;
                }
                return null;
            }, 0L);
        }
        return startMsUTC;
    }
    
    public long getNearestEventDay(long day) {
        if (isFromSystemCalendar()) {
            return daysFromEpoch(getNearestEventTimestamp(day), event.timeZone);
        }
        return startDay.get();
    }
    
    public EntryType getEntryType(long targetDay) {
        // in case of regular entry this will return startDay
        long nearestDay = getNearestEventDay(targetDay);
        
        if (nearestDay == targetDay) {
            return EntryType.TODAY;
        }
        
        if (targetDay + upcomingDayOffset.getToday() >= nearestDay
                && targetDay < nearestDay) {
            return EntryType.UPCOMING;
        }
        
        // for regular entries durationDays = 0
        if (targetDay - expiredDayOffset.getToday() <= nearestDay + durationDays.get()
                && targetDay > nearestDay + durationDays.get()) {
            return EntryType.EXPIRED;
        }
        
        return EntryType.UNKNOWN;
    }
    
    public boolean isGlobal() {
        // startDay = endDay for not calendar entries, so we can use any
        return startDay.get() == Keys.DAY_FLAG_GLOBAL;
    }
    
    public boolean isUpcoming(long day) {
        return getEntryType(day) == EntryType.UPCOMING;
    }
    
    public boolean isExpired(long day) {
        return getEntryType(day) == EntryType.EXPIRED;
    }
    
    public boolean isToday(long day) {
        return getEntryType(day) == EntryType.TODAY;
    }
    
    public String getTimeSpan(Context context) {
        
        if (event == null) {
            return "";
        }
        
        if (isAllDay) {
            return context.getString(R.string.calendar_event_all_day);
        }
        
        if (recurrenceSet != null) {
            return DateManager.getTimeSpan(startMsUTC, startMsUTC + durationMsUTC);
        }
        
        if (startMsUTC == endMsUTC) {
            return datetimeFromEpoch(startMsUTC);
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
    
    public String getTextOnDay(long day, Context context) {
        return rawTextValue.get() + getDayOffset(day, context);
    }
    
    public String getDayOffset(long day, Context context) {
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
        } else {
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
    
    @Override
    public int hashCode() {
        return Objects.hash(event, params, group);
    }
    
    @Override
    public boolean equals(@Nullable Object obj) {
        if (obj == null) {
            return false;
        } else if (obj == this) {
            return true;
        } else if (obj instanceof TodoListEntry) {
            TodoListEntry entry = (TodoListEntry) obj;
            return Objects.equals(event, entry.event) &&
                    Objects.equals(entry.params, params)
                    && Objects.equals(group, entry.group);
        }
        return super.equals(obj);
    }
}