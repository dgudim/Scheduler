package prototype.xd.scheduler.entities;

import static android.util.Log.WARN;
import static prototype.xd.scheduler.utilities.BitmapUtilities.mixTwoColors;
import static prototype.xd.scheduler.utilities.DateManager.currentDay;
import static prototype.xd.scheduler.utilities.DateManager.currentTimestamp;
import static prototype.xd.scheduler.utilities.DateManager.datetimeFromEpoch;
import static prototype.xd.scheduler.utilities.DateManager.daysFromEpoch;
import static prototype.xd.scheduler.utilities.Keys.ADAPTIVE_COLOR_BALANCE;
import static prototype.xd.scheduler.utilities.Keys.ASSOCIATED_DAY;
import static prototype.xd.scheduler.utilities.Keys.BG_COLOR;
import static prototype.xd.scheduler.utilities.Keys.BORDER_COLOR;
import static prototype.xd.scheduler.utilities.Keys.BORDER_THICKNESS;
import static prototype.xd.scheduler.utilities.Keys.DAY_FLAG_GLOBAL;
import static prototype.xd.scheduler.utilities.Keys.DEFAULT_COLOR_MIX_FACTOR;
import static prototype.xd.scheduler.utilities.Keys.ENTRY_SETTINGS_DEFAULT_PRIORITY;
import static prototype.xd.scheduler.utilities.Keys.IS_COMPLETED;
import static prototype.xd.scheduler.utilities.Keys.PRIORITY;
import static prototype.xd.scheduler.utilities.Keys.SHOW_ON_LOCK;
import static prototype.xd.scheduler.utilities.Keys.TEXT_VALUE;
import static prototype.xd.scheduler.utilities.Keys.VISIBLE;
import static prototype.xd.scheduler.utilities.Logger.log;
import static prototype.xd.scheduler.utilities.PreferencesStore.preferences;
import static prototype.xd.scheduler.utilities.SystemCalendarUtils.getFirstValidKey;

import android.content.Context;
import android.widget.TextView;

import androidx.annotation.Nullable;

import com.google.android.material.color.MaterialColors;

import org.dmfs.rfc5545.recurrenceset.RecurrenceSet;
import org.dmfs.rfc5545.recurrenceset.RecurrenceSetIterator;

import java.util.Arrays;
import java.util.List;
import java.util.Objects;
import java.util.function.Consumer;

import prototype.xd.scheduler.R;
import prototype.xd.scheduler.entities.calendars.SystemCalendarEvent;
import prototype.xd.scheduler.utilities.DateManager;
import prototype.xd.scheduler.utilities.Keys;
import prototype.xd.scheduler.utilities.SSMap;

public class TodoListEntry extends RecycleViewEntry {
    
    private static final String NAME = "Todo list entry";
    
    enum EntryType {GLOBAL, TODAY, EXPIRED, UPCOMING, UNKNOWN}
    
    public SystemCalendarEvent event;
    
    private long timestamp_start = 0;
    private long timestamp_end = 0;
    private long timestamp_duration = 0;
    private boolean allDay = false;
    private RecurrenceSet recurrenceSet;
    
    private long day_start = DAY_FLAG_GLOBAL;
    private long day_end = DAY_FLAG_GLOBAL;
    private long duration_in_days = 0;
    public int dayOffset_expired = 0;
    public int dayOffset_upcoming = 0;
    private boolean completed = false;
    private Group group;
    
    public int bgColor;
    public int fontColor;
    public int fontColor_completed;
    public int borderColor;
    public int borderThickness = 0;
    
    public int bgColor_original;
    public int fontColor_original;
    public int borderColor_original;
    public int border_thickness_original = 0;
    
    public int priority = 0;
    
    public int adaptiveColorBalance;
    public int averageBackgroundColor = 0xff_FFFFFF;
    
    public boolean showOnLock = true;
    private boolean showInList_ifCompleted = false;
    
    private String textValue = "";
    
    EntryType entryType;
    
    protected SSMap params = new SSMap();
    
    public TodoListEntry(SystemCalendarEvent event) {
        this.event = event;
        assignId(event.hashCode());
        reloadParams();
    }
    
    public TodoListEntry(Context context, SSMap params, String groupName, List<Group> groups, long id) {
        if (!groupName.isEmpty()) {
            group = new Group(context, groupName, groups);
            if (group.isNullGroup()) {
                log(WARN, NAME, "Unknown group: " + groupName);
                group = null;
            }
        }
        this.params = params;
        assignId(id);
        reloadParams();
    }
    
    public boolean isFromSystemCalendar() {
        return event != null;
    }
    
    public SSMap getParams() {
        return params;
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
        String visibleOnLockscreen = params.get(SHOW_ON_LOCK);
        if (visibleOnLockscreen != null) {
            return Boolean.parseBoolean(visibleOnLockscreen) && !completed;
        }
        if (isFromSystemCalendar()) {
            if (!hideByContent()) {
                if (!allDay && preferences.getBoolean(Keys.HIDE_EXPIRED_ENTRIES_BY_TIME, Keys.SETTINGS_DEFAULT_HIDE_EXPIRED_ENTRIES_BY_TIME)) {
                    showOnLock = isVisibleExact(timestamp);
                } else {
                    showOnLock = isVisible(day);
                }
                return showOnLock &&
                        preferences.getBoolean(getFirstValidKey(event.subKeys, Keys.SHOW_ON_LOCK), Keys.CALENDAR_SETTINGS_DEFAULT_SHOW_ON_LOCK);
            }
            return false;
        } else {
            if (getEntryType(day) == EntryType.GLOBAL) {
                return preferences.getBoolean(Keys.SHOW_GLOBAL_ITEMS_LOCK, Keys.SETTINGS_DEFAULT_SHOW_GLOBAL_ITEMS_LOCK);
            }
            return !completed;
        }
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
    
    public EntryType getEntryType(long targetDay) {
        if (isFromSystemCalendar()) {
            long nearestDay = getNearestEventDay(targetDay);
            if (nearestDay == targetDay) {
                return EntryType.TODAY;
            }
            if (targetDay + dayOffset_upcoming >= nearestDay && targetDay < nearestDay) {
                return EntryType.UPCOMING;
            } else if (targetDay - dayOffset_expired <= nearestDay + duration_in_days && targetDay > nearestDay + duration_in_days) {
                return EntryType.EXPIRED;
            }
        } else {
            // day_start = day_end for not calendar entries, so we can use any
            if (day_start == targetDay) {
                return EntryType.TODAY;
            }
            
            if (day_start == DAY_FLAG_GLOBAL) {
                return EntryType.GLOBAL;
            }
            
            if (day_start < targetDay && targetDay - day_start <= dayOffset_expired) {
                return EntryType.EXPIRED;
            }
            
            if (day_start > targetDay && day_start - targetDay <= dayOffset_upcoming) {
                return EntryType.UPCOMING;
            }
        }
        return EntryType.UNKNOWN;
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
        
        if (!isFromSystemCalendar()) {
            
            long associatedDayLoaded = Long.parseLong(Objects.requireNonNull(params.get(ASSOCIATED_DAY)));
            
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
                
            } else if (associatedDay > currentDay && associatedDay - currentDay <= dayOffset_upcoming) {
                
                showInList_ifCompleted = false;
                showOnLock = true;
                
            }
            
            adaptiveColorBalance = preferences.getInt(Keys.ADAPTIVE_COLOR_BALANCE, Keys.SETTINGS_DEFAULT_ADAPTIVE_COLOR_BALANCE);
            priority = ENTRY_SETTINGS_DEFAULT_PRIORITY;
            setParams();
            
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
            
            
            setParams();
        }
        
        //colors and thickness post update
        
        fontColor_original = fontColor;
        bgColor_original = bgColor;
        borderColor_original = borderColor;
        border_thickness_original = borderThickness;
        
        if (isUpcoming()) {
            fontColor = mixTwoColors(fontColor, preferences.getInt(Keys.UPCOMING_FONT_COLOR, Keys.SETTINGS_DEFAULT_UPCOMING_FONT_COLOR), DEFAULT_COLOR_MIX_FACTOR);
            bgColor = mixTwoColors(bgColor, preferences.getInt(Keys.UPCOMING_BG_COLOR, Keys.SETTINGS_DEFAULT_UPCOMING_BG_COLOR), DEFAULT_COLOR_MIX_FACTOR);
            borderColor = mixTwoColors(borderColor, preferences.getInt(Keys.UPCOMING_BORDER_COLOR, Keys.SETTINGS_DEFAULT_UPCOMING_BORDER_COLOR), DEFAULT_COLOR_MIX_FACTOR);
            borderThickness = preferences.getInt(Keys.UPCOMING_BORDER_THICKNESS, Keys.SETTINGS_DEFAULT_UPCOMING_BORDER_THICKNESS);
        } else if (isExpired()) {
            fontColor = mixTwoColors(fontColor, preferences.getInt(Keys.EXPIRED_FONT_COLOR, Keys.SETTINGS_DEFAULT_EXPIRED_FONT_COLOR), DEFAULT_COLOR_MIX_FACTOR);
            bgColor = mixTwoColors(bgColor, preferences.getInt(Keys.EXPIRED_BG_COLOR, Keys.SETTINGS_DEFAULT_EXPIRED_BG_COLOR), DEFAULT_COLOR_MIX_FACTOR);
            borderColor = mixTwoColors(borderColor, preferences.getInt(Keys.EXPIRED_BORDER_COLOR, Keys.SETTINGS_DEFAULT_EXPIRED_BORDER_COLOR), DEFAULT_COLOR_MIX_FACTOR);
            borderThickness = preferences.getInt(Keys.EXPIRED_BORDER_THICKNESS, Keys.SETTINGS_DEFAULT_EXPIRED_BORDER_THICKNESS);
        }
        fontColor_completed = mixTwoColors(fontColor, 0xff_FFFFFF, 0.5);
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
    
    // get parameter from group if it exists, if not, get from current parameters
    private void setParam(String parameter, Consumer<String> actionIfNotNull) {
        String paramValue = group != null ? group.params.getOrDefault(parameter, params.get(parameter)) : params.get(parameter);
        if (paramValue != null) {
            actionIfNotNull.accept(paramValue);
        }
    }
    
    private void setParams() {
        setParam(TEXT_VALUE, param -> textValue = param);
        setParam(IS_COMPLETED, param -> completed = Boolean.parseBoolean(param));
        setParam(SHOW_ON_LOCK, param -> showOnLock = Boolean.parseBoolean(param));
        setParam(Keys.UPCOMING_ITEMS_OFFSET, param -> dayOffset_upcoming = Integer.parseInt(param));
        setParam(Keys.EXPIRED_ITEMS_OFFSET, param -> dayOffset_expired = Integer.parseInt(param));
        setParam(BORDER_THICKNESS, param -> borderThickness = Integer.parseInt(param));
        setParam(Keys.FONT_COLOR, param -> fontColor = Integer.parseInt(param));
        setParam(BG_COLOR, param -> bgColor = Integer.parseInt(param));
        setParam(BORDER_COLOR, param -> borderColor = Integer.parseInt(param));
        setParam(PRIORITY, param -> priority = Integer.parseInt(param));
        setParam(ASSOCIATED_DAY, param -> {
            day_start = Long.parseLong(param);
            day_end = day_start;
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
        return Objects.hash(event, params, showOnLock, group);
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
                    Objects.equals(entry.params, params) && showOnLock == entry.showOnLock
                    && Objects.equals(group, entry.group);
        }
        return super.equals(obj);
    }
}