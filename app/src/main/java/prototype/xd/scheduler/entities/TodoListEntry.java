package prototype.xd.scheduler.entities;

import static android.util.Log.INFO;
import static android.util.Log.WARN;
import static org.apache.commons.lang.ArrayUtils.addAll;
import static prototype.xd.scheduler.utilities.BitmapUtilities.createNewPaint;
import static prototype.xd.scheduler.utilities.BitmapUtilities.mixTwoColors;
import static prototype.xd.scheduler.utilities.DateManager.currentDay;
import static prototype.xd.scheduler.utilities.DateManager.currentTimestamp;
import static prototype.xd.scheduler.utilities.DateManager.datetimeFromEpoch;
import static prototype.xd.scheduler.utilities.DateManager.daysFromEpoch;
import static prototype.xd.scheduler.utilities.Keys.ADAPTIVE_COLOR_BALANCE;
import static prototype.xd.scheduler.utilities.Keys.ASSOCIATED_DAY;
import static prototype.xd.scheduler.utilities.Keys.BG_COLOR;
import static prototype.xd.scheduler.utilities.Keys.BLANK_TEXT;
import static prototype.xd.scheduler.utilities.Keys.BORDER_COLOR;
import static prototype.xd.scheduler.utilities.Keys.BORDER_THICKNESS;
import static prototype.xd.scheduler.utilities.Keys.DAY_FLAG_GLOBAL;
import static prototype.xd.scheduler.utilities.Keys.DEFAULT_COLOR_MIX_FACTOR;
import static prototype.xd.scheduler.utilities.Keys.ENTITY_SETTINGS_DEFAULT_PRIORITY;
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
import android.graphics.Paint;
import android.widget.TextView;

import androidx.annotation.Nullable;
import androidx.core.math.MathUtils;

import com.google.android.material.color.MaterialColors;

import org.apache.commons.lang.WordUtils;
import org.dmfs.rfc5545.recurrenceset.RecurrenceSet;
import org.dmfs.rfc5545.recurrenceset.RecurrenceSetIterator;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Objects;

import prototype.xd.scheduler.R;
import prototype.xd.scheduler.entities.calendars.SystemCalendarEvent;
import prototype.xd.scheduler.utilities.DateManager;
import prototype.xd.scheduler.utilities.Keys;
import prototype.xd.scheduler.utilities.LockScreenBitmapDrawer;

public class TodoListEntry {
    
    private static final String NAME = "Todo list entry";
    
    enum EntryType {GLOBAL, TODAY, EXPIRED, UPCOMING, NEUTRAL}
    
    public boolean fromSystemCalendar = false;
    public SystemCalendarEvent event;
    
    public long timestamp_start = 0;
    public long timestamp_end = 0;
    public long timestamp_duration = 0;
    public boolean allDay = false;
    public RecurrenceSet recurrenceSet;
    
    public long day_start = DAY_FLAG_GLOBAL;
    public long day_end = DAY_FLAG_GLOBAL;
    public long duration_in_days = 0;
    public int dayOffset_expired = 0;
    public int dayOffset_upcoming = 0;
    public boolean completed = false;
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
    public int adaptiveColor;
    
    public int maxChars = 0;
    public float rWidth = 0;
    
    public boolean showOnLock = false;
    public boolean showInList_ifCompleted = false;
    
    public String textValue = BLANK_TEXT;
    public String[] textValueSplit;
    
    public Paint textPaint;
    public Paint bgPaint;
    public Paint padPaint;
    public Paint indicatorPaint;
    
    EntryType entryType;
    
    public String[] params;
    
    public TodoListEntry() {
    }
    
    public TodoListEntry(SystemCalendarEvent event) {
        fromSystemCalendar = true;
        this.event = event;
        params = new String[]{};
        reloadParams();
    }
    
    public TodoListEntry(Context context, String[] params, String groupName, List<Group> groups) {
        if (!groupName.isEmpty()) {
            group = new Group(context, groupName, groups);
            if (group.isNullGroup()) {
                log(WARN, NAME, "Unknown group: " + groupName);
                group = null;
            }
        }
        this.params = params;
        reloadParams();
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
    
    public boolean getLockViewState() {
        return (showOnLock && !completed);
    }
    
    private boolean inRange(long day, long eventStartDay) {
        return isGlobal() || (day >= eventStartDay - dayOffset_upcoming && day <= eventStartDay + duration_in_days + dayOffset_expired);
        // | days after the event ended ------ event start |event| event end ------ days before the event starts |
        // | ++++++++++++++++++++++++++       -------------|-----|----------        +++++++++++++++++++++++++++++|
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
        if (!fromSystemCalendar) return false;
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
    
    public long getNearestEventDay(long day) {
        if (fromSystemCalendar) {
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
        if (!fromSystemCalendar) {
            for (int i = 0; i < params.length; i += 2) {
                if (params[i].equals(ASSOCIATED_DAY)) {
                    
                    dayOffset_expired = preferences.getInt(EXPIRED_ITEMS_OFFSET, Keys.SETTINGS_DEFAULT_EXPIRED_ITEMS_OFFSET);
                    dayOffset_upcoming = preferences.getInt(UPCOMING_ITEMS_OFFSET, Keys.SETTINGS_DEFAULT_UPCOMING_ITEMS_OFFSET);
                    
                    setParamsWithGroup();
                    
                    long daysAssociated = currentDay;
                    long daysFromParam = Long.parseLong(params[i + 1]);
                    if (daysFromParam != DAY_FLAG_GLOBAL) {
                        daysAssociated = daysFromParam;
                    }
                    
                    if (daysFromParam == currentDay || daysFromParam == DAY_FLAG_GLOBAL) {
                        
                        bgColor = preferences.getInt(Keys.BG_COLOR, Keys.SETTINGS_DEFAULT_BG_COLOR);
                        borderColor = preferences.getInt(Keys.BORDER_COLOR, Keys.SETTINGS_DEFAULT_BORDER_COLOR);
                        borderThickness = preferences.getInt(Keys.BORDER_THICKNESS, Keys.SETTINGS_DEFAULT_BORDER_THICKNESS);
                        
                        setFontColor(Keys.FONT_COLOR, Keys.SETTINGS_DEFAULT_FONT_COLOR);
                        
                        showOnLock = true;
                        showInList_ifCompleted = true;
                        
                        if (daysFromParam == DAY_FLAG_GLOBAL) {
                            showOnLock = preferences.getBoolean(Keys.SHOW_GLOBAL_ITEMS_LOCK, Keys.SETTINGS_DEFAULT_SHOW_GLOBAL_ITEMS_LOCK);
                            setEntryType(EntryType.GLOBAL);
                        } else {
                            setEntryType(EntryType.TODAY);
                        }
                        
                    } else if (daysAssociated < currentDay && currentDay - daysAssociated <= dayOffset_expired) {
                        
                        bgColor = preferences.getInt(Keys.EXPIRED_BG_COLOR, Keys.SETTINGS_DEFAULT_EXPIRED_BG_COLOR);
                        borderColor = preferences.getInt(Keys.EXPIRED_BORDER_COLOR, Keys.SETTINGS_DEFAULT_EXPIRED_BORDER_COLOR);
                        borderThickness = preferences.getInt(Keys.EXPIRED_BORDER_THICKNESS, Keys.SETTINGS_DEFAULT_EXPIRED_BORDER_THICKNESS);
                        
                        setFontColor(Keys.EXPIRED_FONT_COLOR, Keys.SETTINGS_DEFAULT_EXPIRED_FONT_COLOR);
                        
                        showInList_ifCompleted = false;
                        showOnLock = true;
                        
                        setEntryType(EntryType.EXPIRED);
                    } else if (daysAssociated > currentDay && daysAssociated - currentDay <= dayOffset_upcoming) {
                        
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
                        
                        setEntryType(EntryType.NEUTRAL);
                    }
                    break;
                }
            }
            
            adaptiveColorBalance = preferences.getInt(Keys.ADAPTIVE_COLOR_BALANCE, Keys.SETTINGS_DEFAULT_ADAPTIVE_COLOR_BALANCE);
            adaptiveColor = 0xff_FFFFFF;
            priority = ENTITY_SETTINGS_DEFAULT_PRIORITY;
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
            
            priority = preferences.getInt(getFirstValidKey(calendarSubKeys, Keys.PRIORITY), Keys.ENTITY_SETTINGS_DEFAULT_PRIORITY);
            
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
            
            adaptiveColor = 0xff_FFFFFF;
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
    
    public void loadDisplayData(LockScreenBitmapDrawer lockScreenBitmapDrawer) {
        textPaint = createNewPaint(fontColor);
        textPaint.setTextSize(lockScreenBitmapDrawer.getDensityIndependentTextSize());
        textPaint.setTextAlign(Paint.Align.CENTER);
        rWidth = MathUtils.clamp(textPaint.measureText(lockScreenBitmapDrawer.getLongestText()), 1, lockScreenBitmapDrawer.displayWidth / 2f - borderThickness);
        maxChars = (int) ((lockScreenBitmapDrawer.displayWidth - borderThickness) / (textPaint.measureText("qwerty_") / 5f)) - 2;
        
        log(INFO, NAME, "Loaded display data for " + textValue);
    }
    
    private int getAdaptiveColor(int inputColor) {
        if (adaptiveColorBalance > 0) {
            return mixTwoColors(MaterialColors.harmonize(inputColor, adaptiveColor),
                    adaptiveColor, (adaptiveColorBalance - 1) / 9d);
            //active adaptiveColorBalance is from 1 to 10, so we make it from 0 to 9
        }
        return inputColor;
    }
    
    public void initializeBgAndPadPaints() {
        bgPaint = createNewPaint(getAdaptiveColor(bgColor));
        padPaint = createNewPaint(getAdaptiveColor(borderColor));
        if (fromSystemCalendar) {
            indicatorPaint = createNewPaint(event.color);
        }
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
    
    public void splitText(Context context) {
        textValueSplit = (WordUtils.wrap(textValue + getDayOffset(currentDay, context), maxChars, "\n", true) + "\n" + getTimeSpan(context)).split("\n");
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
                case (BG_COLOR):
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
    public int hashCode() {
        return Objects.hash(event, Arrays.hashCode(params), showOnLock, group);
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
                    Arrays.equals(entry.params, params) && showOnLock == entry.showOnLock
                    && Objects.equals(group, entry.group);
        }
        return super.equals(obj);
    }
}
