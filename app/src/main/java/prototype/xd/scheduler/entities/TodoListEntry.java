package prototype.xd.scheduler.entities;

import static android.util.Log.WARN;
import static prototype.xd.scheduler.utilities.BitmapUtilities.mixTwoColors;
import static prototype.xd.scheduler.utilities.DateManager.currentDay;
import static prototype.xd.scheduler.utilities.DateManager.datetimeFromEpoch;
import static prototype.xd.scheduler.utilities.DateManager.daysFromEpoch;
import static prototype.xd.scheduler.utilities.Keys.ADAPTIVE_COLOR_BALANCE;
import static prototype.xd.scheduler.utilities.Keys.ASSOCIATED_DAY;
import static prototype.xd.scheduler.utilities.Keys.BG_COLOR;
import static prototype.xd.scheduler.utilities.Keys.BORDER_COLOR;
import static prototype.xd.scheduler.utilities.Keys.BORDER_THICKNESS;
import static prototype.xd.scheduler.utilities.Keys.DAY_FLAG_GLOBAL;
import static prototype.xd.scheduler.utilities.Keys.ENTRY_SETTINGS_DEFAULT_PRIORITY;
import static prototype.xd.scheduler.utilities.Keys.IS_COMPLETED;
import static prototype.xd.scheduler.utilities.Keys.PRIORITY;
import static prototype.xd.scheduler.utilities.Keys.SHOW_ON_LOCK;
import static prototype.xd.scheduler.utilities.Keys.TEXT_VALUE;
import static prototype.xd.scheduler.utilities.Logger.log;
import static prototype.xd.scheduler.utilities.PreferencesStore.preferences;
import static prototype.xd.scheduler.utilities.SystemCalendarUtils.getFirstValidKey;
import static prototype.xd.scheduler.utilities.Utilities.nullWrapper;

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

import prototype.xd.scheduler.R;
import prototype.xd.scheduler.entities.calendars.SystemCalendarEvent;
import prototype.xd.scheduler.utilities.DateManager;
import prototype.xd.scheduler.utilities.Keys;
import prototype.xd.scheduler.utilities.SSMap;

public class TodoListEntry extends RecycleViewEntry implements Serializable {
    
    transient private static final String NAME = "Todo list entry";
    
    public enum EntryType {GLOBAL, TODAY, EXPIRED, UPCOMING, UNKNOWN}
    
    transient public SystemCalendarEvent event;
    
    transient private long startMsUTC = 0;
    transient private long endMsUTC = 0;
    transient private long durationMsUTC = 0;
    transient private boolean allDay = true;
    transient private RecurrenceSet recurrenceSet;
    
    transient private long startDay = DAY_FLAG_GLOBAL;
    transient private long endDay = DAY_FLAG_GLOBAL;
    transient private long durationDay = 0;
    transient public int dayOffset_expired = 0;
    transient public int dayOffset_upcoming = 0;
    transient private boolean completed = false;
    transient private Group group;
    transient private String tempGroupName;
    
    transient public int bgColor;
    transient public int fontColor;
    transient public int fontColor_completed;
    transient public int borderColor;
    transient public int borderThickness = 0;
    
    transient public int bgColor_original;
    transient public int fontColor_original;
    transient public int borderColor_original;
    transient public int border_thickness_original = 0;
    
    transient public int priority = 0;
    
    transient public int adaptiveColorBalance;
    transient public int averageBackgroundColor = 0xff_FFFFFF;
    
    transient private String textValue = "";
    
    protected SSMap params = new SSMap();
    
    public TodoListEntry(SystemCalendarEvent event) {
        this.event = event;
        
        startMsUTC = event.start;
        endMsUTC = event.end;
        durationMsUTC = event.duration;
        allDay = event.allDay;
        recurrenceSet = event.rSet;
        
        startDay = daysFromEpoch(startMsUTC, event.timeZone);
        endDay = daysFromEpoch(endMsUTC, event.timeZone);
        durationDay = daysFromEpoch(startMsUTC + durationMsUTC, event.timeZone) - startDay;
        
        textValue = event.title;
        
        assignId(event.hashCode());
        reloadParams();
    }
    
    public TodoListEntry(Context context, SSMap params, String groupName, List<Group> groups, long id) {
        tempGroupName = groupName;
        initGroup(context, groups);
        this.params = params;
        assignId(id);
        reloadParams();
    }
    
    private void readObject(ObjectInputStream in)
            throws IOException, ClassNotFoundException {
        in.defaultReadObject();
        tempGroupName = nullWrapper((String) in.readObject());
    }
    
    private void writeObject(ObjectOutputStream out) throws IOException {
        out.defaultWriteObject();
        if(group != null) {
            out.writeObject(group.getName());
        }
    }
    
    public void initGroup(Context context, List<Group> groups) {
        if (!tempGroupName.isEmpty()) {
            group = new Group(context, tempGroupName, groups);
            if (group.isNullGroup()) {
                log(WARN, NAME, "Unknown group: " + tempGroupName);
                group = null;
            }
        }
    }
    
    public boolean isFromSystemCalendar() {
        return event != null;
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
    
    public boolean isVisibleOnLockscreen(long day, long timestamp) {
        String visibleOnLockscreen = params.getWithNull(SHOW_ON_LOCK);
        if (visibleOnLockscreen != null) {
            return Boolean.parseBoolean(visibleOnLockscreen) && !completed;
        }
        if (isFromSystemCalendar()) {
            if (!hideByContent()) {
                boolean showOnLock;
                if (!allDay && preferences.getBoolean(Keys.HIDE_EXPIRED_ENTRIES_BY_TIME, Keys.SETTINGS_DEFAULT_HIDE_EXPIRED_ENTRIES_BY_TIME)) {
                    showOnLock = isVisibleExact(timestamp);
                } else {
                    showOnLock = isVisible(day);
                }
                return showOnLock && preferences.getBoolean(getFirstValidKey(event.subKeys, Keys.SHOW_ON_LOCK), Keys.CALENDAR_SETTINGS_DEFAULT_SHOW_ON_LOCK);
            }
            return false;
        } else {
            if (getEntryType(day) == EntryType.GLOBAL) {
                return preferences.getBoolean(Keys.SHOW_GLOBAL_ITEMS_LOCK, Keys.SETTINGS_DEFAULT_SHOW_GLOBAL_ITEMS_LOCK);
            }
            return !completed;
        }
    }
    
    private boolean inRange(long day, long instanceStartDay) {
        return getEntryType(day) == EntryType.GLOBAL || (day >= instanceStartDay - dayOffset_upcoming && day <= instanceStartDay + durationDay + dayOffset_expired);
        // | days after the event ended ------ event start |event| event end ------ days before the event starts |
        // | ++++++++++++++++++++++++++       -------------|-----|----------        +++++++++++++++++++++++++++++|
    }
    
    public boolean isCompleted() {
        return completed;
    }
    
    public boolean visibleInList(long day) {
        boolean show = isVisible(day);
        EntryType entryType = getEntryType(day);
        if (entryType == EntryType.EXPIRED || entryType == EntryType.UPCOMING) {
            return show && !completed;
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
                hideByContent = textValue.contains(str);
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
        if (isFromSystemCalendar()) {
            long nearestDay = getNearestEventDay(targetDay);
            if (nearestDay == targetDay) {
                return EntryType.TODAY;
            }
            if (targetDay + dayOffset_upcoming >= nearestDay && targetDay < nearestDay) {
                return EntryType.UPCOMING;
            } else if (targetDay - dayOffset_expired <= nearestDay + durationDay && targetDay > nearestDay + durationDay) {
                return EntryType.EXPIRED;
            }
        } else {
            // startDay = endDay for not calendar entries, so we can use any
            if (startDay == targetDay) {
                return EntryType.TODAY;
            }
            
            if (startDay < targetDay && targetDay - startDay <= dayOffset_expired) {
                return EntryType.EXPIRED;
            }
            
            if (startDay > targetDay && startDay - targetDay <= dayOffset_upcoming) {
                return EntryType.UPCOMING;
            }
        }
        return EntryType.UNKNOWN;
    }
    
    public boolean notGlobal() {
        // startDay = endDay for not calendar entries, so we can use any
        return startDay != DAY_FLAG_GLOBAL;
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
        
        if (allDay) {
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
        displayParams.removeAll(Arrays.asList(TEXT_VALUE, ASSOCIATED_DAY, IS_COMPLETED));
        return displayParams;
    }
    
    public void removeDisplayParams() {
        params.retainAll(Arrays.asList(TEXT_VALUE, ASSOCIATED_DAY, IS_COMPLETED));
        reloadParams();
    }
    
    // get calendar key for calendar entries and regular key for normal entries
    private String getAppropriateKey(String key) {
        if (isFromSystemCalendar()) {
            return getFirstValidKey(event.subKeys, key);
        }
        return key;
    }
    
    public void reloadParams() {
        
        // load default parameters
        dayOffset_expired = preferences.getInt(getAppropriateKey(Keys.EXPIRED_ITEMS_OFFSET), Keys.SETTINGS_DEFAULT_EXPIRED_ITEMS_OFFSET);
        dayOffset_upcoming = preferences.getInt(getAppropriateKey(Keys.UPCOMING_ITEMS_OFFSET), Keys.SETTINGS_DEFAULT_UPCOMING_ITEMS_OFFSET);
        
        bgColor = preferences.getInt(getAppropriateKey(Keys.BG_COLOR), Keys.SETTINGS_DEFAULT_BG_COLOR);
        borderColor = preferences.getInt(getAppropriateKey(Keys.BORDER_COLOR), Keys.SETTINGS_DEFAULT_BORDER_COLOR);
        borderThickness = preferences.getInt(getAppropriateKey(Keys.BORDER_THICKNESS), Keys.SETTINGS_DEFAULT_BORDER_THICKNESS);
        fontColor = preferences.getInt(getAppropriateKey(Keys.FONT_COLOR), Keys.SETTINGS_DEFAULT_FONT_COLOR);
        fontColor_completed = mixTwoColors(fontColor, 0xff_FFFFFF, 0.5);
        
        adaptiveColorBalance = preferences.getInt(getAppropriateKey(Keys.ADAPTIVE_COLOR_BALANCE), Keys.SETTINGS_DEFAULT_ADAPTIVE_COLOR_BALANCE);
        
        priority = isFromSystemCalendar() ? preferences.getInt(getFirstValidKey(event.subKeys, Keys.PRIORITY), Keys.ENTRY_SETTINGS_DEFAULT_PRIORITY) : ENTRY_SETTINGS_DEFAULT_PRIORITY;
        
        setParams();
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
        if (getEntryType(day) != EntryType.GLOBAL) {
            
            int dayShift = 0;
            
            long nearestDay = getNearestEventDay(day);
            if (day < nearestDay) {
                dayShift = (int) (nearestDay - day);
            } else if (day > nearestDay + durationDay) {
                dayShift = (int) (nearestDay + durationDay - day);
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
        setParam(TEXT_VALUE, param -> textValue = param);
        setParam(IS_COMPLETED, param -> completed = Boolean.parseBoolean(param));
        setParam(Keys.UPCOMING_ITEMS_OFFSET, param -> dayOffset_upcoming = Integer.parseInt(param));
        setParam(Keys.EXPIRED_ITEMS_OFFSET, param -> dayOffset_expired = Integer.parseInt(param));
        setParam(BORDER_THICKNESS, param -> borderThickness = Integer.parseInt(param));
        setParam(Keys.FONT_COLOR, param -> fontColor = Integer.parseInt(param));
        setParam(BG_COLOR, param -> bgColor = Integer.parseInt(param));
        setParam(BORDER_COLOR, param -> borderColor = Integer.parseInt(param));
        setParam(PRIORITY, param -> priority = Integer.parseInt(param));
        setParam(ASSOCIATED_DAY, param -> {
            startDay = Long.parseLong(param);
            endDay = startDay;
        });
        setParam(ADAPTIVE_COLOR_BALANCE, param -> adaptiveColorBalance = Integer.parseInt(param));
    }
    
    public void changeParameter(String name, String value) {
        params.put(name, value);
        reloadParams();
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