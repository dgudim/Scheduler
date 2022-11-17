package prototype.xd.scheduler.entities;

import static android.util.Log.WARN;
import static org.apache.commons.lang.ArrayUtils.addAll;
import static prototype.xd.scheduler.utilities.BitmapUtilities.mixTwoColors;
import static prototype.xd.scheduler.utilities.DateManager.currentDay;
import static prototype.xd.scheduler.utilities.DateManager.currentTimestamp;
import static prototype.xd.scheduler.utilities.DateManager.datetimeFromEpoch;
import static prototype.xd.scheduler.utilities.DateManager.daysFromEpoch;
import static prototype.xd.scheduler.utilities.Keys.ADAPTIVE_COLOR_BALANCE;
import static prototype.xd.scheduler.utilities.Keys.ASSOCIATED_DAY;
import static prototype.xd.scheduler.utilities.Keys.BORDER_COLOR;
import static prototype.xd.scheduler.utilities.Keys.BORDER_THICKNESS;
import static prototype.xd.scheduler.utilities.Keys.DAY_FLAG_GLOBAL;
import static prototype.xd.scheduler.utilities.Keys.DEFAULT_COLOR_MIX_FACTOR;
import static prototype.xd.scheduler.utilities.Keys.ENTRY_SETTINGS_DEFAULT_PRIORITY;
import static prototype.xd.scheduler.utilities.Keys.EXPIRED_BORDER_THICKNESS;
import static prototype.xd.scheduler.utilities.Keys.EXPIRED_ITEMS_OFFSET;
import static prototype.xd.scheduler.utilities.Keys.FONT_COLOR;
import static prototype.xd.scheduler.utilities.Keys.IS_COMPLETED;
import static prototype.xd.scheduler.utilities.Keys.PRIORITY;
import static prototype.xd.scheduler.utilities.Keys.SHOW_ON_LOCK;
import static prototype.xd.scheduler.utilities.Keys.TEXT_VALUE;
import static prototype.xd.scheduler.utilities.Keys.UPCOMING_ITEMS_OFFSET;
import static prototype.xd.scheduler.utilities.Logger.log;
import static prototype.xd.scheduler.utilities.PreferencesStore.preferences;
import static prototype.xd.scheduler.utilities.SystemCalendarUtils.getFirstValidKey;

import android.content.Context;
import android.widget.TextView;

import androidx.annotation.Nullable;

import com.google.android.material.color.MaterialColors;

import org.dmfs.rfc5545.recurrenceset.RecurrenceSetIterator;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.EnumMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;

import prototype.xd.scheduler.R;
import prototype.xd.scheduler.entities.TodoListEntry.EntryType;
import prototype.xd.scheduler.entities.TodoListEntry.Parameter;
import prototype.xd.scheduler.entities.calendars.SystemCalendarEvent;
import prototype.xd.scheduler.utilities.DateManager;
import prototype.xd.scheduler.utilities.Keys;

class TemporalStorage<T extends Serializable> implements Serializable {
    
    T upcoming;
    T today;
    T expired;
    T global;
    
    T get(EntryType entryType) {
        switch (entryType) {
            case TODAY:
                return today;
            case EXPIRED:
                return expired;
            case UPCOMING:
                return upcoming;
            case GLOBAL:
            default:
                return global;
        }
    }
    
    void set(T newValue, EntryType entryType) {
        switch (entryType) {
            case TODAY:
                today = newValue;
                break;
            case EXPIRED:
                expired = newValue;
                break;
            case UPCOMING:
                upcoming = newValue;
                break;
            case GLOBAL:
            default:
                global = newValue;
        }
    }
}

@FunctionalInterface
interface TodoListEntryParameterTemporalAccessor<T extends Serializable> {
    T get(long day, long timestamp, Parameter<T> parameter, @Nullable Context context);
}

class ForwardTemporalAccessor<T extends Serializable> implements TodoListEntryParameterTemporalAccessor<T> {
    // TODO: implement
    @Override
    public T get(long day, long timestamp, Parameter<T> parameter, @Nullable Context context) {
        return null;
    }
}

@FunctionalInterface
interface TemporalGetter<T extends Serializable> {
    T get(EntryType entryType);
}

public class TodoListEntry extends RecycleViewEntry implements Serializable {
    
    public static class Parameter<T extends Serializable> implements Serializable {
        
        transient Map<EntryType, Boolean> validityMap = new EnumMap<>(EntryType.class);
        
        transient TemporalStorage<T> cachedValue;
        transient TemporalGetter<T> getter;
        TemporalStorage<T> persistentValue;
        
        @Nullable
        transient TodoListEntryParameterTemporalAccessor<T> temporalAccessor;
        
        @SuppressWarnings("unchecked")
        protected Parameter(TemporalGetter<T> getter,
                            @Nullable TodoListEntryParameterTemporalAccessor<T> temporalAccessor,
                            @Nullable Parameter<?> copyFrom) {
            this.getter = getter;
            this.temporalAccessor = temporalAccessor;
            if (copyFrom != null) {
                persistentValue = (TemporalStorage<T>) copyFrom.persistentValue;
            }
        }
        
        public T getToday() {
            return get(EntryType.TODAY);
        }
        
        public T getExpired() {
            return get(EntryType.EXPIRED);
        }
        
        public T getUpcoming() {
            return get(EntryType.UPCOMING);
        }
        
        public T get(long day, long timestamp, @Nullable Context context) {
            if (temporalAccessor != null) {
                return temporalAccessor.get(day, timestamp, this, context);
            }
            return getToday();
        }
        
        public T get(long day, long timestamp) {
            return get(day, timestamp, null);
        }
        
        public void setPersistent(T newValue, EntryType entryType) {
            persistentValue.set(newValue, entryType);
        }
        
        public void setCached(T newValue, EntryType entryType) {
            cachedValue.set(newValue, entryType);
        }
        
        public T get(EntryType entryType) {
            T pValue = persistentValue.get(entryType);
            if (pValue != null) {
                return pValue;
            }
            Boolean valid = validityMap.get(entryType);
            if (valid == null || Boolean.FALSE.equals(valid)) {
                cachedValue.set(getter.get(entryType), entryType);
                validityMap.put(entryType, true);
            }
            return cachedValue.get(entryType);
        }
        
        public void invalidate(EntryType entryType) {
            validityMap.put(entryType, false);
        }
    }
    
    private static final String NAME = "Todo list entry";
    
    enum EntryType {GLOBAL, TODAY, EXPIRED, UPCOMING}
    
    public SystemCalendarEvent event;
    
    private long timestamp_start = 0;
    private long timestamp_end = 0;
    private long timestamp_duration = 0;
    private boolean allDay = false;
    
    private long day_start = DAY_FLAG_GLOBAL;
    private long day_end = DAY_FLAG_GLOBAL;
    private long duration_in_days = 0;
    private Group group;
    
    public Parameter<String> textValue;
    public Parameter<Boolean> isCompleted;
    public Parameter<Boolean> showOnLock;
    
    public Parameter<Integer> upcomingOffset;
    public Parameter<Integer> expiredOffset;
    
    public Parameter<Integer> bgColor;
    public Parameter<Integer> fontColor;
    public Parameter<Integer> fontColor_completed;
    public Parameter<Integer> borderColor;
    public Parameter<Integer> borderThickness;
    
    public Parameter<Integer> priority;
    public Parameter<Integer> associatedDay;
    
    public int averageBackgroundColor = 0;
    public Parameter<Integer> adaptiveColorBalance;
    
    private boolean showInList_ifCompleted = false;
    
    public TodoListEntry(SystemCalendarEvent event) {
        this.event = event;
        assignId(event.hashCode());
        initParams();
    }
    
    public TodoListEntry(Context context, String groupName, List<Group> groups, long id) {
        if (!groupName.isEmpty()) {
            group = new Group(context, groupName, groups);
            if (group.isNullGroup()) {
                log(WARN, NAME, "Unknown group: " + groupName);
                group = null;
            }
        }
        assignId(id);
        reloadParams();
    }
    
    private String getProperKey(String key) {
        if (isFromSystemCalendar()) {
            key = getFirstValidKey(event.subKeys, key);
        }
        return key;
    }
    
    public void postDeserialize(long id, List<Group> groups) {
        // after deserialization the group only stores it's name so we get the real group
        if (group != null) {
            int index = Group.groupIndexInList(groups, group.getName());
            if (index != -1) {
                group = groups.get(index);
            } else {
                log(WARN, NAME, "Unknown group: " + group.getName());
            }
        }
        assignId(id);
        initParams();
    }
    
    public void initParams() {
        
        textValue = new Parameter<>(
                entryType -> "",
                (day, timestamp, parameter, context) -> parameter.getToday() + getDayOffset(day, context),
                textValue);
        
        isCompleted = new Parameter<>(
                entryType -> false,
                null,
                isCompleted);
        
        showOnLock = new Parameter<>(entryType -> {
            if (isFromSystemCalendar()) {
                if (!hideByContent()) {
                    boolean showOnLock;
                    if (!allDay && preferences.getBoolean(Keys.HIDE_EXPIRED_ENTRIES_BY_TIME, Keys.SETTINGS_DEFAULT_HIDE_EXPIRED_ENTRIES_BY_TIME)) {
                        showOnLock = isVisibleExact(currentTimestamp);
                    } else {
                        showOnLock = isVisible(currentDay);
                    }
                    return showOnLock && preferences.getBoolean(getFirstValidKey(event.subKeys, Keys.SHOW_ON_LOCK), Keys.CALENDAR_SETTINGS_DEFAULT_SHOW_ON_LOCK);
                }
                return false;
            }
            return true;
        }, null, showOnLock);
        
        upcomingOffset = new Parameter<>(entryType ->
                preferences.getInt(getProperKey(UPCOMING_ITEMS_OFFSET), Keys.SETTINGS_DEFAULT_UPCOMING_ITEMS_OFFSET), null,
                upcomingOffset);
        
        expiredOffset = new Parameter<>(entryType ->
                preferences.getInt(getProperKey(EXPIRED_ITEMS_OFFSET), Keys.SETTINGS_DEFAULT_EXPIRED_ITEMS_OFFSET), null,
                expiredOffset);
        
        borderThickness = new Parameter<>(entryType -> {
            switch (entryType) {
                case EXPIRED:
                    return preferences.getInt(EXPIRED_BORDER_THICKNESS, Keys.SETTINGS_DEFAULT_EXPIRED_BORDER_THICKNESS);
                case UPCOMING:
                    return preferences.getInt(Keys.UPCOMING_BORDER_THICKNESS, Keys.SETTINGS_DEFAULT_UPCOMING_BORDER_THICKNESS);
                case TODAY:
                case GLOBAL:
                default:
                    return preferences.getInt(getProperKey(Keys.BORDER_THICKNESS), Keys.SETTINGS_DEFAULT_BORDER_THICKNESS);
            }
        }, new ForwardTemporalAccessor<>(), borderThickness);
        
        borderColor = new Parameter<>(entryType -> {
            int borderColor = preferences.getInt(getProperKey(Keys.BORDER_COLOR), Keys.SETTINGS_DEFAULT_BORDER_COLOR);
            switch (entryType) {
                case EXPIRED:
                    return mixTwoColors(
                            borderColor,
                            preferences.getInt(Keys.EXPIRED_BORDER_COLOR, Keys.SETTINGS_DEFAULT_EXPIRED_BORDER_COLOR),
                            DEFAULT_COLOR_MIX_FACTOR);
                case UPCOMING:
                    return mixTwoColors(
                            borderColor,
                            preferences.getInt(Keys.UPCOMING_BORDER_COLOR, Keys.SETTINGS_DEFAULT_UPCOMING_BORDER_COLOR),
                            DEFAULT_COLOR_MIX_FACTOR);
                case TODAY:
                case GLOBAL:
                default:
                    return borderColor;
            }
        }, new ForwardTemporalAccessor<>(), borderColor);
        
        fontColor = new Parameter<>(entryType -> {
            int fontColor = preferences.getInt(getProperKey(Keys.FONT_COLOR), Keys.SETTINGS_DEFAULT_FONT_COLOR);
            switch (entryType) {
                case EXPIRED:
                    return mixTwoColors(
                            fontColor,
                            preferences.getInt(Keys.EXPIRED_FONT_COLOR, Keys.SETTINGS_DEFAULT_EXPIRED_FONT_COLOR),
                            DEFAULT_COLOR_MIX_FACTOR);
                case UPCOMING:
                    return mixTwoColors(
                            fontColor,
                            preferences.getInt(Keys.UPCOMING_FONT_COLOR, Keys.SETTINGS_DEFAULT_UPCOMING_FONT_COLOR),
                            DEFAULT_COLOR_MIX_FACTOR);
                case TODAY:
                case GLOBAL:
                default:
                    return fontColor;
            }
        }, new ForwardTemporalAccessor<>(), fontColor);
        
        bgColor = new Parameter<>(entryType -> {
            int bgColor = preferences.getInt(getProperKey(Keys.BG_COLOR), Keys.SETTINGS_DEFAULT_BG_COLOR);
            switch (entryType) {
                case EXPIRED:
                    return mixTwoColors(
                            bgColor,
                            preferences.getInt(Keys.EXPIRED_BG_COLOR, Keys.SETTINGS_DEFAULT_EXPIRED_BG_COLOR),
                            DEFAULT_COLOR_MIX_FACTOR);
                case UPCOMING:
                    return mixTwoColors(
                            bgColor,
                            preferences.getInt(Keys.UPCOMING_BG_COLOR, Keys.SETTINGS_DEFAULT_UPCOMING_BG_COLOR),
                            DEFAULT_COLOR_MIX_FACTOR);
                case TODAY:
                case GLOBAL:
                default:
                    return bgColor;
            }
        }, new ForwardTemporalAccessor<>(), bgColor);
        
        priority = new Parameter<>(entryType ->
                preferences.getInt(getProperKey(PRIORITY), Keys.ENTRY_SETTINGS_DEFAULT_PRIORITY), null,
                priority);
        
        associatedDay = new Parameter<>(entryType ->
                DAY_FLAG_GLOBAL, null,
                associatedDay);
        
        adaptiveColorBalance = new Parameter<>((entryType) ->
                preferences.getInt(getProperKey(ADAPTIVE_COLOR_BALANCE), Keys.SETTINGS_DEFAULT_ADAPTIVE_COLOR_BALANCE), null,
                adaptiveColorBalance);
    }
    
    public Group getGroup() {
        return group;
    }
    
    public String getGroupName() {
        if (group != null) {
            return group.getName();
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
        reloadParams();
    }
    
    public void changeGroup(Group group) {
        if (group.isNullGroup()) {
            resetGroup();
        } else {
            this.group = group;
            reloadParams();
        }
    }
    
    public boolean isFromSystemCalendar() {
        return event != null;
    }
    
    public boolean isVisibleOnLockscreen() {
        return (showOnLock && !completed);
    }
    
    private boolean inRange(long day, long eventStartDay) {
        return isGlobal() || (day >= eventStartDay - dayOffset_upcoming && day <= eventStartDay + duration_in_days + dayOffset_expired);
        // | days after the event ended ------ event start |event| event end ------ days before the event starts |
        // | ++++++++++++++++++++++++++       -------------|-----|----------        +++++++++++++++++++++++++++++|
    }
    
    public boolean isCompleted() {
        return completed;
    }
    
    public boolean isGlobal() {
        return day_start == DAY_FLAG_GLOBAL || day_end == DAY_FLAG_GLOBAL || entryType == EntryType.GLOBAL;
    }
    
    public boolean isExpired() {
        return entryType == EntryType.EXPIRED;
    }
    
    public boolean isUpcoming() {
        return entryType == EntryType.UPCOMING;
    }
    
    public boolean isToday() {
        return entryType == EntryType.TODAY;
    }
    
    public boolean visibleInList(long day) {
        boolean visibilityFlag = !completed || showInList_ifCompleted;
        boolean show;
        if (day == currentDay) {
            show = isUpcoming() || isExpired() || isVisible(day);
            show = show && visibilityFlag;
        } else {
            show = isVisible(day);
        }
        return show;
    }
    
    public boolean isVisible(long day) {
        if (recurrenceSet != null) {
            if (day > day_end) {
                return false;
            }
            RecurrenceSetIterator it = recurrenceSet.iterator(event.timeZone, timestamp_start);
            long instance = 0;
            while (it.hasNext() && instance <= day) {
                instance = daysFromEpoch(it.next(), event.timeZone);
                if (inRange(day, instance)) {
                    return true;
                }
            }
            return false;
        }
        return inRange(day, day_start);
    }
    
    public boolean isVisibleExact(long timestamp) {
        if (recurrenceSet != null) {
            if (timestamp > timestamp_end) {
                return false;
            }
            RecurrenceSetIterator it = recurrenceSet.iterator(event.timeZone, timestamp_start);
            long instance = 0;
            long day = daysFromEpoch(timestamp, event.timeZone);
            while (it.hasNext() && instance <= timestamp) {
                instance = it.next();
                if (inRange(day, daysFromEpoch(instance, event.timeZone))) {
                    return isUpcoming() || (instance + timestamp_duration >= timestamp && daysFromEpoch(instance, event.timeZone) <= currentDay);
                }
            }
            return false;
        }
        return inRange(daysFromEpoch(timestamp, event.timeZone), day_start)
                && (isUpcoming() || (timestamp_start + timestamp_duration >= timestamp && daysFromEpoch(timestamp, event.timeZone) <= currentDay));
    }
    
    public boolean hideByContent() {
        if (!isFromSystemCalendar()) return false;
        if (preferences.getBoolean(getFirstValidKey(event.subKeys, Keys.HIDE_ENTRIES_BY_CONTENT), Keys.SETTINGS_DEFAULT_HIDE_ENTRIES_BY_CONTENT)) {
            String matchString = preferences.getString(getFirstValidKey(event.subKeys, Keys.HIDE_ENTRIES_BY_CONTENT_CONTENT), "");
            String[] split = !matchString.isEmpty() ? matchString.split("\\|\\|") : new String[0];
            for (String str : split) {
                if (textValue.contains(str)) return true;
            }
        }
        return false;
    }
    
    public long getNearestEventTimestamp(long day) {
        if (recurrenceSet != null) {
            if (day >= day_end) {
                return timestamp_end;
            }
            RecurrenceSetIterator it = recurrenceSet.iterator(event.timeZone, timestamp_start);
            while (it.hasNext()) {
                long epoch = it.next();
                if (inRange(day, daysFromEpoch(epoch, event.timeZone)) || daysFromEpoch(epoch, event.timeZone) >= day) {
                    return epoch;
                }
            }
        }
        return timestamp_start;
    }
    
    public long getNearestEventDay(long day) {
        if (isFromSystemCalendar()) {
            return daysFromEpoch(getNearestEventTimestamp(day), event.timeZone);
        }
        return day_start;
    }
    
    public String getTimeSpan(Context context) {
        
        if (event == null) {
            return "";
        }
        
        if (allDay) {
            return context.getString(R.string.calendar_event_all_day);
        }
        
        if (recurrenceSet != null) {
            return DateManager.getTimeSpan(timestamp_start, timestamp_start + timestamp_duration);
        }
        
        if (timestamp_start == timestamp_end) {
            return datetimeFromEpoch(timestamp_start);
        } else {
            return DateManager.getTimeSpan(timestamp_start, timestamp_end);
        }
    }
    
    public String[] getDisplayParams() {
        List<String> displayParams = new ArrayList<>();
        for (int i = 0; i < params.length; i += 2) {
            
            if (!(params[i].equals(TEXT_VALUE)
                    || params[i].equals(ASSOCIATED_DAY)
                    || params[i].equals(IS_COMPLETED))) {
                displayParams.add(params[i]);
                displayParams.add(params[i + 1]);
            }
        }
        
        return displayParams.toArray(new String[0]);
    }
    
    public void removeDisplayParams() {
        List<String> displayParams = new ArrayList<>();
        for (int i = 0; i < params.length; i += 2) {
            
            if (params[i].equals(TEXT_VALUE)
                    || params[i].equals(ASSOCIATED_DAY)
                    || params[i].equals(IS_COMPLETED)) {
                displayParams.add(params[i]);
                displayParams.add(params[i + 1]);
            }
        }
        params = displayParams.toArray(new String[0]);
        reloadParams();
    }
    
    public void reloadParams() {
        if (!isFromSystemCalendar()) {
            int paramIndex = -1;
            for (int i = 0; i < params.length; i += 2) {
                if (params[i].equals(ASSOCIATED_DAY)) {
                    paramIndex = i;
                    break;
                }
            }
            if (paramIndex != -1) {
                long associatedDayLoaded = Long.parseLong(params[paramIndex + 1]);
                
                setParamsWithGroup();
                
                long associatedDay = currentDay;
                if (associatedDayLoaded != DAY_FLAG_GLOBAL) {
                    associatedDay = associatedDayLoaded;
                }
                
                if (associatedDayLoaded == currentDay || associatedDayLoaded == DAY_FLAG_GLOBAL) {
                    
                    showOnLock = true;
                    showInList_ifCompleted = true;
                    
                    if (associatedDayLoaded == DAY_FLAG_GLOBAL) {
                        showOnLock = preferences.getBoolean(Keys.SHOW_GLOBAL_ITEMS_LOCK, Keys.SETTINGS_DEFAULT_SHOW_GLOBAL_ITEMS_LOCK);
                        setEntryType(EntryType.GLOBAL);
                    } else {
                        setEntryType(EntryType.TODAY);
                    }
                    
                } else if (associatedDay < currentDay && currentDay - associatedDay <= dayOffset_expired) {
                    
                    showInList_ifCompleted = false;
                    showOnLock = true;
                    
                    setEntryType(EntryType.EXPIRED);
                } else if (associatedDay > currentDay && associatedDay - currentDay <= dayOffset_upcoming) {
                    
                    bgColor = preferences.getInt(Keys.UPCOMING_BG_COLOR, Keys.SETTINGS_DEFAULT_UPCOMING_BG_COLOR);
                    borderColor = preferences.getInt(Keys.UPCOMING_BORDER_COLOR, Keys.SETTINGS_DEFAULT_UPCOMING_BORDER_COLOR);
                    borderThickness = preferences.getInt(Keys.UPCOMING_BORDER_THICKNESS, Keys.SETTINGS_DEFAULT_UPCOMING_BORDER_THICKNESS);
                    
                    setFontColor(Keys.UPCOMING_FONT_COLOR, Keys.SETTINGS_DEFAULT_UPCOMING_FONT_COLOR);
                    
                    showInList_ifCompleted = false;
                    showOnLock = true;
                    
                    setEntryType(EntryType.UPCOMING);
                } else {
                    setFontColor(Keys.SETTINGS_DEFAULT_FONT_COLOR);
                    
                    bgColor = Keys.SETTINGS_DEFAULT_BG_COLOR;
                    borderColor = Keys.SETTINGS_DEFAULT_BORDER_COLOR;
                    borderThickness = Keys.SETTINGS_DEFAULT_BORDER_THICKNESS;
                    
                    showInList_ifCompleted = true;
                    showOnLock = false;
                    
                    setEntryType(EntryType.UNKNOWN);
                }
            }
            
            adaptiveColorBalance = preferences.getInt(Keys.ADAPTIVE_COLOR_BALANCE, Keys.SETTINGS_DEFAULT_ADAPTIVE_COLOR_BALANCE);
            priority = ENTRY_SETTINGS_DEFAULT_PRIORITY;
            setParamsWithGroup();
            
        } else {
            
            timestamp_start = event.start;
            timestamp_end = event.end;
            timestamp_duration = event.duration;
            allDay = event.allDay;
            recurrenceSet = event.rSet;
            
            day_start = daysFromEpoch(timestamp_start, event.timeZone);
            day_end = daysFromEpoch(timestamp_end, event.timeZone);
            duration_in_days = daysFromEpoch(timestamp_start + timestamp_duration, event.timeZone) - day_start;
            
            textValue = event.title;
            
            List<String> calendarSubKeys = event.subKeys;
            
            adaptiveColorBalance = preferences.getInt(getFirstValidKey(calendarSubKeys, Keys.ADAPTIVE_COLOR_BALANCE), Keys.SETTINGS_DEFAULT_ADAPTIVE_COLOR_BALANCE);
            
            priority = preferences.getInt(getFirstValidKey(calendarSubKeys, Keys.PRIORITY), Keys.ENTRY_SETTINGS_DEFAULT_PRIORITY);
            
            dayOffset_upcoming = preferences.getInt(getFirstValidKey(calendarSubKeys, UPCOMING_ITEMS_OFFSET), Keys.SETTINGS_DEFAULT_UPCOMING_ITEMS_OFFSET);
            dayOffset_expired = preferences.getInt(getFirstValidKey(calendarSubKeys, EXPIRED_ITEMS_OFFSET), Keys.SETTINGS_DEFAULT_EXPIRED_ITEMS_OFFSET);
            
            long nearestDay = getNearestEventDay(currentDay);
            if (currentDay + dayOffset_upcoming >= nearestDay && currentDay < nearestDay) {
                setEntryType(EntryType.UPCOMING);
            } else if (currentDay - dayOffset_expired <= nearestDay + duration_in_days && currentDay > nearestDay + duration_in_days) {
                setEntryType(EntryType.EXPIRED);
            }
            
            setFontColor(getFirstValidKey(calendarSubKeys, Keys.FONT_COLOR), Keys.SETTINGS_DEFAULT_FONT_COLOR);
            bgColor = preferences.getInt(getFirstValidKey(calendarSubKeys, Keys.BG_COLOR), Keys.SETTINGS_DEFAULT_BG_COLOR);
            borderColor = preferences.getInt(getFirstValidKey(calendarSubKeys, Keys.BORDER_COLOR), Keys.SETTINGS_DEFAULT_BORDER_COLOR);
            borderThickness = preferences.getInt(getFirstValidKey(calendarSubKeys, Keys.BORDER_THICKNESS), Keys.SETTINGS_DEFAULT_BORDER_THICKNESS);
            
            if (!hideByContent()) {
                if (!allDay && preferences.getBoolean(Keys.HIDE_EXPIRED_ENTRIES_BY_TIME, Keys.SETTINGS_DEFAULT_HIDE_EXPIRED_ENTRIES_BY_TIME)) {
                    showOnLock = isVisibleExact(currentTimestamp);
                } else {
                    showOnLock = isVisible(currentDay);
                }
                showOnLock = showOnLock && preferences.getBoolean(getFirstValidKey(calendarSubKeys, Keys.SHOW_ON_LOCK), Keys.CALENDAR_SETTINGS_DEFAULT_SHOW_ON_LOCK);
            }
            
            setParams(params);
        }
        
        //colors and thickness post update
        
        fontColor_original = fontColor;
        bgColor_original = bgColor;
        borderColor_original = borderColor;
        border_thickness_original = borderThickness;
        
        if (isUpcoming()) {
            setFontColor(mixTwoColors(fontColor, preferences.getInt(Keys.UPCOMING_FONT_COLOR, Keys.SETTINGS_DEFAULT_UPCOMING_FONT_COLOR), DEFAULT_COLOR_MIX_FACTOR));
            bgColor = mixTwoColors(bgColor, preferences.getInt(Keys.UPCOMING_BG_COLOR, Keys.SETTINGS_DEFAULT_UPCOMING_BG_COLOR), DEFAULT_COLOR_MIX_FACTOR);
            borderColor = mixTwoColors(borderColor, preferences.getInt(Keys.UPCOMING_BORDER_COLOR, Keys.SETTINGS_DEFAULT_UPCOMING_BORDER_COLOR), DEFAULT_COLOR_MIX_FACTOR);
            borderThickness = preferences.getInt(Keys.UPCOMING_BORDER_THICKNESS, Keys.SETTINGS_DEFAULT_UPCOMING_BORDER_THICKNESS);
        } else if (isExpired()) {
            setFontColor(mixTwoColors(fontColor, preferences.getInt(Keys.EXPIRED_FONT_COLOR, Keys.SETTINGS_DEFAULT_EXPIRED_FONT_COLOR), DEFAULT_COLOR_MIX_FACTOR));
            bgColor = mixTwoColors(bgColor, preferences.getInt(Keys.EXPIRED_BG_COLOR, Keys.SETTINGS_DEFAULT_EXPIRED_BG_COLOR), DEFAULT_COLOR_MIX_FACTOR);
            borderColor = mixTwoColors(borderColor, preferences.getInt(Keys.EXPIRED_BORDER_COLOR, Keys.SETTINGS_DEFAULT_EXPIRED_BORDER_COLOR), DEFAULT_COLOR_MIX_FACTOR);
            borderThickness = preferences.getInt(Keys.EXPIRED_BORDER_THICKNESS, Keys.SETTINGS_DEFAULT_EXPIRED_BORDER_THICKNESS);
        }
    }
    
    private void setEntryType(EntryType entryType) {
        this.entryType = entryType;
    }
    
    private void setFontColor(int color) {
        fontColor = color;
        fontColor_completed = mixTwoColors(fontColor, 0xff_FFFFFF, 0.5);
    }
    
    private void setFontColor(String key, int defaultColor) {
        setFontColor(preferences.getInt(key, defaultColor));
    }
    
    public boolean isAdaptiveColorEnabled() {
        return adaptiveColorBalance > 0;
    }
    
    public int getAdaptiveColor(int inputColor) {
        if (adaptiveColorBalance > 0) {
            return mixTwoColors(MaterialColors.harmonize(inputColor, averageBackgroundColor),
                    averageBackgroundColor, (adaptiveColorBalance - 1) / 9d);
            //active adaptiveColorBalance is from 1 to 10, so we make it from 0 to 9
        }
        return inputColor;
    }
    
    public String getRawTextValue() {
        return textValue;
    }
    
    public String getTextOnDay(long day, Context context) {
        return textValue + getDayOffset(day, context);
    }
    
    public String getDayOffset(long day, Context context) {
        String dayOffset = "";
        if (!isGlobal()) {
            
            int dayShift = 0;
            
            long nearestDay = getNearestEventDay(day);
            if (day < nearestDay) {
                dayShift = (int) (nearestDay - day);
            } else if (day > nearestDay + duration_in_days) {
                dayShift = (int) (nearestDay + duration_in_days - day);
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
    
    private void setParamsWithGroup() {
        if (group != null) {
            setParams((String[]) addAll(group.params, params));
        } else {
            setParams(params);
        }
    }
    
    private void setParams(String[] params) {
        for (int i = 0; i < params.length; i += 2) {
            switch (params[i]) {
                case (TEXT_VALUE):
                    textValue = params[i + 1];
                    break;
                case (IS_COMPLETED):
                    completed = Boolean.parseBoolean(params[i + 1]);
                    break;
                case (SHOW_ON_LOCK):
                    showOnLock = Boolean.parseBoolean(params[i + 1]);
                    break;
                case (UPCOMING_ITEMS_OFFSET):
                    dayOffset_upcoming = Integer.parseInt(params[i + 1]);
                    break;
                case (EXPIRED_ITEMS_OFFSET):
                    dayOffset_expired = Integer.parseInt(params[i + 1]);
                    break;
                case (BORDER_THICKNESS):
                    borderThickness = Integer.parseInt(params[i + 1]);
                    break;
                case (FONT_COLOR):
                    setFontColor(Integer.parseInt(params[i + 1]));
                    break;
                case (Keys.BG_COLOR):
                    bgColor = Integer.parseInt(params[i + 1]);
                    break;
                case (BORDER_COLOR):
                    borderColor = Integer.parseInt(params[i + 1]);
                    break;
                case (PRIORITY):
                    priority = Integer.parseInt(params[i + 1]);
                    break;
                case (ASSOCIATED_DAY):
                    day_start = Long.parseLong(params[i + 1]);
                    day_end = day_start;
                    break;
                case (ADAPTIVE_COLOR_BALANCE):
                    adaptiveColorBalance = Integer.parseInt(params[i + 1]);
                    break;
                default:
                    log(WARN, NAME, "unknown parameter: " + params[i] + " entry textValue: " + textValue);
                    break;
            }
        }
    }
    
    public <T extends Serializable> void setParameterPersistent(ParameterType parameterType, T newValue, EntryType entryType) {
        Objects.requireNonNull(parameters.get(parameterType)).setPersistent(newValue, entryType);
    }
    
    public void changeParameter(String name, String value) {
        boolean changed = false;
        for (int i = 0; i < params.length; i++) {
            if (params[i].equals(name)) {
                params[i + 1] = value;
                changed = true;
                break;
            }
        }
        if (!changed) {
            String[] newParams = new String[params.length + 2];
            System.arraycopy(params, 0, newParams, 0, params.length);
            newParams[newParams.length - 1] = value;
            newParams[newParams.length - 2] = name;
            params = newParams;
        }
        reloadParams();
    }
    
    public void setStateIconColor(TextView icon, String parameter) {
        boolean containedInGroupParams = false;
        boolean containedInPersonalParams = false;
        if (group != null) {
            for (int i = 0; i < group.params.length; i += 2) {
                if (group.params[i].equals(parameter)) {
                    containedInGroupParams = true;
                    break;
                }
            }
        }
        for (int i = 0; i < params.length; i += 2) {
            if (params[i].equals(parameter)) {
                containedInPersonalParams = true;
                break;
            }
        }
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
        return Objects.hash(event, parameters, group);
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
                    parameters.equals(entry.parameters)
                    && Objects.equals(group, entry.group);
        }
        return super.equals(obj);
    }
}
