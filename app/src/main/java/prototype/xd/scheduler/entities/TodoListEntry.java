package prototype.xd.scheduler.entities;

import static android.util.Log.WARN;
import static prototype.xd.scheduler.utilities.BitmapUtilities.mixTwoColors;
import static prototype.xd.scheduler.utilities.DateManager.currentDay;
import static prototype.xd.scheduler.utilities.DateManager.currentTimestamp;
import static prototype.xd.scheduler.utilities.DateManager.datetimeFromEpoch;
import static prototype.xd.scheduler.utilities.DateManager.daysFromEpoch;
import static prototype.xd.scheduler.utilities.Logger.log;
import static prototype.xd.scheduler.utilities.PreferencesStore.preferences;
import static prototype.xd.scheduler.utilities.SystemCalendarUtils.getFirstValidKey;

import android.content.Context;
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
import java.util.List;
import java.util.Objects;
import java.util.function.Consumer;
import java.util.function.Function;

import prototype.xd.scheduler.R;
import prototype.xd.scheduler.entities.calendars.SystemCalendarEvent;
import prototype.xd.scheduler.utilities.DateManager;
import prototype.xd.scheduler.utilities.Keys;
import prototype.xd.scheduler.utilities.SSMap;

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
    }
    
    @FunctionalInterface
    interface ParameterGetter<T> {
        T get(T previousValue);
    }
    
    public static class Parameter<T> {
        
        TodoListEntry entry;
        
        String parameterKey;
        
        boolean invalidateNext;
        
        CachedGetter<T> todayValueGetter;
        Function<String, T> loadedParameterConverter;
        @Nullable
        CachedGetter<T> upcomingValueGetter;
        @Nullable
        CachedGetter<T> expiredValueGetter;
        
        Parameter(TodoListEntry entry,
                  String parameterKey,
                  ParameterGetter<T> todayValueGetter,
                  Function<String, T> loadedParameterConverter,
                  @Nullable ParameterGetter<T> upcomingValueGetter,
                  @Nullable ParameterGetter<T> expiredValueGetter,
                  boolean invalidateNext) {
            this.entry = entry;
            this.parameterKey = parameterKey;
            this.upcomingValueGetter = new CachedGetter<>(upcomingValueGetter);
            this.todayValueGetter = new CachedGetter<>(todayValueGetter);
            this.loadedParameterConverter = loadedParameterConverter;
            this.expiredValueGetter = new CachedGetter<>(expiredValueGetter);
            this.invalidateNext = invalidateNext;
        }
        
        // get parameter from group if it exists, if not, get from current parameters
        // TODO: 19.11.2022 also cache map accesses
        private T getParamFromEntryIfPossible() {
            String paramValue = entry.group != null ?
                    entry.group.params.getOrDefault(parameterKey, entry.params.getWithNull(parameterKey)) :
                    entry.params.getWithNull(parameterKey);
            return paramValue != null ? loadedParameterConverter.apply(paramValue) : todayValueGetter.get(null);
        }
        
        public T get(EntryType entryType) {
            T todayValue = getParamFromEntryIfPossible();
            switch (entryType) {
                case EXPIRED:
                    return expiredValueGetter == null ? todayValue : expiredValueGetter.get(todayValue);
                case UPCOMING:
                    return upcomingValueGetter == null ? todayValue : upcomingValueGetter.get(todayValue);
                case TODAY:
                default:
                    return todayValue;
            }
        }
        
        public T get() {
            return getParamFromEntryIfPossible();
        }
        
        public void invalidate(EntryType entryType) {
            switch (entryType) {
                case EXPIRED:
                    if (expiredValueGetter != null) {
                        expiredValueGetter.invalidate();
                    }
                    break;
                case UPCOMING:
                    if (upcomingValueGetter != null) {
                        upcomingValueGetter.invalidate();
                    }
                    break;
                case TODAY:
                default:
                    todayValueGetter.invalidate();
                    // if upcoming and expired depend on current and we should also invalidate them
                    if (invalidateNext && upcomingValueGetter != null) {
                        upcomingValueGetter.invalidate();
                    }
                    if (invalidateNext && expiredValueGetter != null) {
                        expiredValueGetter.invalidate();
                    }
            }
        }
        
    }
    
    private static final transient String NAME = "Todo list entry";
    
    public enum EntryType {TODAY, EXPIRED, UPCOMING, UNKNOWN}
    
    public transient SystemCalendarEvent event;
    
    private transient long startMsUTC = 0;
    private transient long endMsUTC = 0;
    private transient long durationMsUTC = 0;
    private transient boolean isAllDay = true;
    private transient RecurrenceSet recurrenceSet;
    
    private transient long startDay = Keys.DAY_FLAG_GLOBAL;
    private transient long endDay = Keys.DAY_FLAG_GLOBAL;
    private transient long durationDays = 0;
    
    @Nullable
    private transient Group group;
    // for initializing group after deserialization
    private transient String tempGroupName;
    
    public transient Parameter<Integer> bgColor = new Parameter<>(this, Keys.BG_COLOR,
            previousValue -> preferences.getInt(getAppropriateKey(Keys.BG_COLOR), Keys.SETTINGS_DEFAULT_BG_COLOR),
            Integer::parseInt,
            todayValue -> mixTwoColors(todayValue,
                    preferences.getInt(Keys.UPCOMING_BG_COLOR, Keys.SETTINGS_DEFAULT_UPCOMING_BG_COLOR), Keys.DEFAULT_COLOR_MIX_FACTOR),
            todayValue -> mixTwoColors(todayValue,
                    preferences.getInt(Keys.EXPIRED_BG_COLOR, Keys.SETTINGS_DEFAULT_EXPIRED_BG_COLOR), Keys.DEFAULT_COLOR_MIX_FACTOR),
            true);
    
    public transient Parameter<Integer> fontColor = new Parameter<>(this, Keys.FONT_COLOR,
            previousValue -> preferences.getInt(getAppropriateKey(Keys.FONT_COLOR), Keys.SETTINGS_DEFAULT_FONT_COLOR),
            Integer::parseInt,
            todayValue -> mixTwoColors(todayValue,
                    preferences.getInt(Keys.UPCOMING_FONT_COLOR, Keys.SETTINGS_DEFAULT_UPCOMING_FONT_COLOR), Keys.DEFAULT_COLOR_MIX_FACTOR),
            todayValue -> mixTwoColors(todayValue,
                    preferences.getInt(Keys.EXPIRED_FONT_COLOR, Keys.SETTINGS_DEFAULT_EXPIRED_FONT_COLOR), Keys.DEFAULT_COLOR_MIX_FACTOR),
            true);
    
    public transient Parameter<Integer> borderColor = new Parameter<>(this, Keys.BORDER_COLOR,
            previousValue -> preferences.getInt(getAppropriateKey(Keys.BORDER_COLOR), Keys.SETTINGS_DEFAULT_BORDER_COLOR),
            Integer::parseInt,
            todayValue -> mixTwoColors(todayValue,
                    preferences.getInt(Keys.UPCOMING_BORDER_COLOR, Keys.SETTINGS_DEFAULT_UPCOMING_BORDER_COLOR), Keys.DEFAULT_COLOR_MIX_FACTOR),
            todayValue -> mixTwoColors(todayValue,
                    preferences.getInt(Keys.EXPIRED_BORDER_COLOR, Keys.SETTINGS_DEFAULT_EXPIRED_BORDER_COLOR), Keys.DEFAULT_COLOR_MIX_FACTOR),
            true);
    
    public transient Parameter<Integer> borderThickness = new Parameter<>(this, Keys.BORDER_THICKNESS,
            previousValue -> preferences.getInt(getAppropriateKey(Keys.BORDER_THICKNESS), Keys.SETTINGS_DEFAULT_BORDER_THICKNESS),
            Integer::parseInt,
            todayValue -> preferences.getInt(Keys.UPCOMING_BORDER_THICKNESS, Keys.SETTINGS_DEFAULT_UPCOMING_BORDER_THICKNESS),
            todayValue -> preferences.getInt(Keys.EXPIRED_BORDER_THICKNESS, Keys.SETTINGS_DEFAULT_EXPIRED_BORDER_THICKNESS),
            false);
    
    public transient Parameter<Integer> priority = new Parameter<>(this, Keys.PRIORITY,
            previousValue -> isFromSystemCalendar() ?
                    preferences.getInt(getFirstValidKey(event.subKeys, Keys.PRIORITY), Keys.ENTRY_SETTINGS_DEFAULT_PRIORITY) :
                    Keys.ENTRY_SETTINGS_DEFAULT_PRIORITY,
            Integer::parseInt,
            null,
            null,
            false);
    
    public transient Parameter<Integer> expiredDayOffset = new Parameter<>(this, Keys.EXPIRED_ITEMS_OFFSET,
            previousValue -> preferences.getInt(getAppropriateKey(Keys.EXPIRED_ITEMS_OFFSET), Keys.SETTINGS_DEFAULT_EXPIRED_ITEMS_OFFSET),
            Integer::parseInt,
            null,
            null,
            false);
    public transient Parameter<Integer> upcomingDayOffset = new Parameter<>(this, Keys.UPCOMING_ITEMS_OFFSET,
            previousValue -> preferences.getInt(getAppropriateKey(Keys.UPCOMING_ITEMS_OFFSET), Keys.SETTINGS_DEFAULT_UPCOMING_ITEMS_OFFSET),
            Integer::parseInt,
            null,
            null,
            false);
    
    public transient Parameter<Integer> adaptiveColorBalance = new Parameter<>(this, Keys.ADAPTIVE_COLOR_BALANCE,
            previousValue -> preferences.getInt(getAppropriateKey(Keys.ADAPTIVE_COLOR_BALANCE), Keys.SETTINGS_DEFAULT_ADAPTIVE_COLOR_BALANCE),
            Integer::parseInt,
            null,
            null,
            false);
    
    private transient int averageBackgroundColor = 0xff_FFFFFF;
    
    protected SSMap params = new SSMap();
    
    public TodoListEntry(SystemCalendarEvent event) {
        this.event = event;
        
        startMsUTC = event.start;
        endMsUTC = event.end;
        durationMsUTC = event.duration;
        isAllDay = event.allDay;
        recurrenceSet = event.rSet;
        
        startDay = daysFromEpoch(startMsUTC, event.timeZone);
        endDay = daysFromEpoch(endMsUTC, event.timeZone);
        durationDays = daysFromEpoch(startMsUTC + durationMsUTC, event.timeZone) - startDay;
        
        assignId(event.hashCode());
    }
    
    // ------------ serialization
    public TodoListEntry(SSMap params, String groupName, List<Group> groups, long id) {
        tempGroupName = groupName;
        initGroup(groups);
        this.params = params;
        assignId(id);
    }
    
    private void readObject(ObjectInputStream in)
            throws IOException, ClassNotFoundException {
        params = (SSMap) in.readObject();
        tempGroupName = (String) in.readObject();
    }
    // ------------
    
    private void writeObject(ObjectOutputStream out) throws IOException {
        out.writeObject(params);
        if (group == null) {
            out.writeObject("");
        } else {
            out.writeObject(group.getName());
        }
    }
    
    public void initGroup(List<Group> groups) {
        if (!tempGroupName.isEmpty()) {
            group = Group.findGroup(groups, tempGroupName);
            if (group == null) {
                log(WARN, NAME, "Unknown group: " + tempGroupName);
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
            return group.getName();
        } else {
            return "";
        }
    }
    
    public String getLocalizedGroupName(Context context) {
        if (group != null) {
            return group.getLocalizedName(context);
        } else {
            return "";
        }
    }
    
    public void setGroupName(String name) {
        if (group != null) {
            group.setName(name);
        }
    }
    
    public void resetGroup() {
        group = null;
        // TODO: 19.11.2022 invalidate
    }
    
    public void changeGroup(Group group) {
        if (group.isNullGroup()) {
            resetGroup();
        } else {
            this.group = group;
            // TODO: 19.11.2022 invalidate
        }
    }
    
    public int getAverageBackgroundColor() {
        return averageBackgroundColor;
    }
    
    public void setAverageBackgroundColor(int averageBackgroundColor) {
        this.averageBackgroundColor = averageBackgroundColor;
    }
    
    public boolean isVisibleOnLockscreenToday() {
        String visibleOnLockscreen = params.getWithNull(Keys.SHOW_ON_LOCK);
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
            return !isCompleted();
        }
    }
    
    private boolean inRange(long day, long instanceStartDay) {
        return isGlobal() || (day >= instanceStartDay - upcomingDayOffset.get() && day <= instanceStartDay + durationDays + expiredDayOffset.get());
        // | days after the event ended ------ event start |event| event end ------ days before the event starts |
        // | ++++++++++++++++++++++++++       -------------|-----|----------        +++++++++++++++++++++++++++++|
    }
    
    public boolean isCompleted() {
        // everything else except "true" is false, so we are fine here
        return Boolean.parseBoolean(params.getWithNull(Keys.IS_COMPLETED));
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
    
    public boolean isVisible(long day) {
        if (recurrenceSet != null) {
            if (day > endDay) {
                return false;
            }
            RecurrenceSetIterator it = recurrenceSet.iterator(event.timeZone, startMsUTC);
            long instanceDay = 0;
            while (it.hasNext() && instanceDay <= day) {
                instanceDay = daysFromEpoch(it.next(), event.timeZone);
                if (inRange(day, instanceDay)) {
                    return true;
                }
            }
            return false;
        }
        return inRange(day, startDay);
    }
    
    public boolean isVisibleExact(long targetTimestamp) {
        long targetDay = daysFromEpoch(targetTimestamp, event.timeZone);
        if (recurrenceSet != null) {
            if (targetTimestamp > endMsUTC) {
                return false;
            }
            RecurrenceSetIterator it = recurrenceSet.iterator(event.timeZone, startMsUTC);
            long instanceMsUTC = 0, instanceDay;
            while (it.hasNext() && instanceMsUTC <= targetTimestamp) {
                instanceMsUTC = it.next();
                instanceDay = daysFromEpoch(instanceMsUTC, event.timeZone);
                if (inRange(targetDay, instanceDay)) {
                    return isUpcoming(instanceDay) || (instanceMsUTC + durationMsUTC >= targetTimestamp && instanceDay <= currentDay);
                }
            }
            return false;
        }
        return inRange(targetDay, startDay) && (isUpcoming(targetDay) || (startMsUTC + durationMsUTC >= targetTimestamp && targetDay <= currentDay));
    }
    
    public boolean hideByContent() {
        if (!isFromSystemCalendar()) return false;
        boolean hideByContent = false;
        if (preferences.getBoolean(getFirstValidKey(event.subKeys, Keys.HIDE_ENTRIES_BY_CONTENT), Keys.SETTINGS_DEFAULT_HIDE_ENTRIES_BY_CONTENT)) {
            String matchString = preferences.getString(getFirstValidKey(event.subKeys, Keys.HIDE_ENTRIES_BY_CONTENT_CONTENT), "");
            String[] split = !matchString.isEmpty() ? matchString.split("\\|\\|") : new String[0];
            for (String str : split) {
                hideByContent = getRawTextValue().contains(str);
                if (hideByContent) break;
            }
        }
        return hideByContent;
    }
    
    public long getNearestEventTimestamp(long day) {
        if (recurrenceSet != null) {
            if (day >= endDay) {
                return endMsUTC;
            }
            RecurrenceSetIterator it = recurrenceSet.iterator(event.timeZone, startMsUTC);
            long instanceTimestamp, instanceDay;
            while (it.hasNext()) {
                instanceDay = daysFromEpoch(instanceTimestamp = it.next(), event.timeZone);
                if (inRange(day, instanceDay) || instanceDay >= day) {
                    return instanceTimestamp;
                }
            }
        }
        return startMsUTC;
    }
    
    public long getNearestEventDay(long day) {
        if (isFromSystemCalendar()) {
            return daysFromEpoch(getNearestEventTimestamp(day), event.timeZone);
        }
        return startDay;
    }
    
    public EntryType getEntryType(long targetDay) {
        // in case of regular entry this will return startDay
        long nearestDay = getNearestEventDay(targetDay);
        
        if (nearestDay == targetDay) {
            return EntryType.TODAY;
        }
        
        if (targetDay + upcomingDayOffset.get() >= nearestDay
                && targetDay < nearestDay) {
            return EntryType.UPCOMING;
        }
        
        // for regular entries durationDay = 0
        if (targetDay - expiredDayOffset.get() <= nearestDay + durationDays
                && targetDay > nearestDay + durationDays) {
            return EntryType.EXPIRED;
        }
        
        return EntryType.UNKNOWN;
    }
    
    public boolean isGlobal() {
        // startDay = endDay for not calendar entries, so we can use any
        return startDay == Keys.DAY_FLAG_GLOBAL;
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
    
    public SSMap getDisplayParams() {
        // shallow copy map
        SSMap displayParams = new SSMap(params);
        // remove not display parameters
        displayParams.removeAll(Arrays.asList(Keys.TEXT_VALUE, Keys.ASSOCIATED_DAY, Keys.IS_COMPLETED));
        return displayParams;
    }
    
    public void removeDisplayParams() {
        params.retainAll(Arrays.asList(Keys.TEXT_VALUE, Keys.ASSOCIATED_DAY, Keys.IS_COMPLETED));
        // TODO: 19.11.2022 invalidate
    }
    
    // get calendar key for calendar entries and regular key for normal entries
    private String getAppropriateKey(String key) {
        if (isFromSystemCalendar()) {
            return getFirstValidKey(event.subKeys, key);
        }
        return key;
    }
    
    public boolean isAdaptiveColorEnabled() {
        return adaptiveColorBalance.get() > 0;
    }
    
    public int getAdaptiveColor(int inputColor) {
        if (isAdaptiveColorEnabled()) {
            return mixTwoColors(MaterialColors.harmonize(inputColor, averageBackgroundColor),
                    averageBackgroundColor, (adaptiveColorBalance.get() - 1) / 9d);
            //active adaptiveColorBalance is from 1 to 10, so we make it from 0 to 9
        }
        return inputColor;
    }
    
    public String getRawTextValue() {
        return isFromSystemCalendar() ? event.title : params.get(Keys.TEXT_VALUE);
    }
    
    public String getTextOnDay(long day, Context context) {
        return getRawTextValue() + getDayOffset(day, context);
    }
    
    public String getDayOffset(long day, Context context) {
        String dayOffset = "";
        if (!isGlobal()) {
            
            int dayShift = 0;
            
            long nearestDay = getNearestEventDay(day);
            if (day < nearestDay) {
                dayShift = (int) (nearestDay - day);
            } else if (day > nearestDay + durationDays) {
                dayShift = (int) (nearestDay + durationDays - day);
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
    
    // get parameter from group if it exists, if not, get from current parameters
    private void setParam(String parameter, Consumer<String> actionIfNotNull) {
        String paramValue = group != null ? group.params.getOrDefault(parameter, params.getWithNull(parameter)) : params.getWithNull(parameter);
        if (paramValue != null) {
            actionIfNotNull.accept(paramValue);
        }
    }
    
    private void setParams() {
        // TODO: 19.11.2022 deal with this
        setParam(Keys.ASSOCIATED_DAY, param -> {
            startDay = Long.parseLong(param);
            endDay = startDay;
        });
        
    }
    
    public void changeParameter(String name, String value) {
        params.put(name, value);
        // TODO: 19.11.2022 invalidate
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