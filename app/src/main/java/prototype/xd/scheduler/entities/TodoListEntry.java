package prototype.xd.scheduler.entities;

import static org.apache.commons.lang.ArrayUtils.addAll;
import static prototype.xd.scheduler.MainActivity.preferences;
import static prototype.xd.scheduler.utilities.BitmapUtilities.createNewPaint;
import static prototype.xd.scheduler.utilities.BitmapUtilities.mixTwoColors;
import static prototype.xd.scheduler.utilities.DateManager.currentDay;
import static prototype.xd.scheduler.utilities.DateManager.dateFromEpoch;
import static prototype.xd.scheduler.utilities.DateManager.daysFromEpoch;
import static prototype.xd.scheduler.utilities.DateManager.timeZone_UTC;
import static prototype.xd.scheduler.utilities.Keys.ADAPTIVE_COLOR_BALANCE;
import static prototype.xd.scheduler.utilities.Keys.ADAPTIVE_COLOR_ENABLED;
import static prototype.xd.scheduler.utilities.Keys.ASSOCIATED_DAY;
import static prototype.xd.scheduler.utilities.Keys.BEVEL_COLOR;
import static prototype.xd.scheduler.utilities.Keys.BEVEL_THICKNESS;
import static prototype.xd.scheduler.utilities.Keys.BG_COLOR;
import static prototype.xd.scheduler.utilities.Keys.BLANK_GROUP_NAME;
import static prototype.xd.scheduler.utilities.Keys.BLANK_TEXT;
import static prototype.xd.scheduler.utilities.Keys.DAY_FLAG_GLOBAL;
import static prototype.xd.scheduler.utilities.Keys.ENTITY_SETTINGS_DEFAULT_PRIORITY;
import static prototype.xd.scheduler.utilities.Keys.EXPIRED_ITEMS_OFFSET;
import static prototype.xd.scheduler.utilities.Keys.FONT_COLOR;
import static prototype.xd.scheduler.utilities.Keys.IS_COMPLETED;
import static prototype.xd.scheduler.utilities.Keys.PRIORITY;
import static prototype.xd.scheduler.utilities.Keys.SHOW_ON_LOCK;
import static prototype.xd.scheduler.utilities.Keys.TEXT_VALUE;
import static prototype.xd.scheduler.utilities.Keys.UPCOMING_ITEMS_OFFSET;
import static prototype.xd.scheduler.utilities.Logger.ContentType.INFO;
import static prototype.xd.scheduler.utilities.Logger.ContentType.WARNING;
import static prototype.xd.scheduler.utilities.Logger.log;
import static prototype.xd.scheduler.utilities.SystemCalendarUtils.getFirstValidKey;

import android.content.Context;
import android.graphics.Color;
import android.graphics.Paint;
import android.widget.TextView;

import androidx.annotation.Nullable;
import androidx.core.math.MathUtils;

import org.apache.commons.lang.WordUtils;
import org.dmfs.rfc5545.recurrenceset.RecurrenceSet;
import org.dmfs.rfc5545.recurrenceset.RecurrenceSetIterator;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Objects;

import prototype.xd.scheduler.R;
import prototype.xd.scheduler.entities.calendars.SystemCalendarEvent;
import prototype.xd.scheduler.utilities.DateManager;
import prototype.xd.scheduler.utilities.Keys;
import prototype.xd.scheduler.utilities.LockScreenBitmapDrawer;

public class TodoListEntry {
    
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
    public Group group;
    
    public int bgColor;
    public int fontColor;
    public int fontColor_completed;
    public int bevelColor;
    
    public boolean adaptiveColorEnabled;
    public int adaptiveColorBalance;
    public int adaptiveColor;
    
    public int maxChars = 0;
    public float rWidth = 0;
    
    public int bevelThickness = 0;
    
    public int priority = 0;
    
    public boolean showOnLock = false;
    public boolean showInList_ifCompleted = false;
    
    public String textValue = BLANK_TEXT;
    public String[] textValueSplit;
    
    public Paint textPaint;
    public Paint bgPaint;
    public Paint padPaint;
    
    public boolean isTodayEntry = false;
    public boolean isGlobalEntry = false;
    public boolean isOldEntry = false;
    public boolean isNewEntry = false;
    
    public String[] params;
    
    public TodoListEntry() {
    }
    
    private boolean inRange(long day, long event_start_day) {
        return isGlobal() || (day >= event_start_day - dayOffset_upcoming && day <= event_start_day + duration_in_days + dayOffset_expired);
        // | days after the event ended ------ event start |event| event end ------ days before the event starts |
        // | ++++++++++++++++++++++++++       -------------|-----|----------        +++++++++++++++++++++++++++++|
    }
    
    public boolean isGlobal() {
        return day_start == DAY_FLAG_GLOBAL || day_end == DAY_FLAG_GLOBAL;
    }
    
    public boolean isVisible(long day) {
        if (recurrenceSet != null) {
            if (day > day_end) {
                return false;
            }
            RecurrenceSetIterator it = recurrenceSet.iterator(timeZone_UTC, timestamp_start);
            long instance = 0;
            while (it.hasNext() && daysFromEpoch(instance) <= day) {
                instance = it.next();
                if (inRange(day, daysFromEpoch(instance))) {
                    return true;
                }
            }
            return false;
        }
        return inRange(day, day_start);
    }
    
    public long genNearestEventDay(long day) {
        if (recurrenceSet != null) {
            if (day > day_end) {
                return day_end;
            }
            RecurrenceSetIterator it = recurrenceSet.iterator(timeZone_UTC, timestamp_start);
            while (it.hasNext()) {
                long epoch = it.next();
                if (inRange(day, daysFromEpoch(epoch))) {
                    return daysFromEpoch(epoch);
                }
                if (daysFromEpoch(epoch) >= day) {
                    return daysFromEpoch(epoch);
                }
            }
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
            return dateFromEpoch(timestamp_start);
        } else {
            return DateManager.getTimeSpan(timestamp_start, timestamp_end);
        }
    }
    
    public TodoListEntry(SystemCalendarEvent event) {
        fromSystemCalendar = true;
        this.event = event;
        params = new String[]{};
        reloadParams();
    }
    
    public TodoListEntry(String[] params, String groupName) {
        group = new Group(groupName);
        this.params = params;
        reloadParams();
    }
    
    public void changeGroup(String groupName) {
        group = new Group(groupName);
        reloadParams();
    }
    
    public void resetGroup() {
        changeGroup(BLANK_GROUP_NAME);
    }
    
    public void changeGroup(Group group) {
        this.group = group;
        reloadParams();
    }
    
    public boolean getLockViewState() {
        return (showOnLock && !completed);
    }
    
    public String[] getDisplayParams() {
        ArrayList<String> displayParams = new ArrayList<>();
        for (int i = 0; i < params.length; i += 2) {
            
            if (!(params[i].equals(TEXT_VALUE)
                    || params[i].equals(ASSOCIATED_DAY)
                    || params[i].equals(IS_COMPLETED))) {
                displayParams.add(params[i]);
                displayParams.add(params[i + 1]);
            }
        }
        String[] displayParams_new = new String[displayParams.size()];
        for (int i = 0; i < displayParams.size(); i++) {
            displayParams_new[i] = displayParams.get(i);
        }
        return displayParams_new;
    }
    
    public void removeDisplayParams() {
        ArrayList<String> displayParams = new ArrayList<>();
        for (int i = 0; i < params.length; i += 2) {
            
            if (params[i].equals(TEXT_VALUE)
                    || params[i].equals(ASSOCIATED_DAY)
                    || params[i].equals(IS_COMPLETED)) {
                displayParams.add(params[i]);
                displayParams.add(params[i + 1]);
            }
        }
        String[] params_new = new String[displayParams.size()];
        for (int i = 0; i < displayParams.size(); i++) {
            params_new[i] = displayParams.get(i);
        }
        params = params_new;
        reloadParams();
    }
    
    public void reloadParams() {
        if (!fromSystemCalendar) {
            for (int i = 0; i < params.length; i += 2) {
                if (params[i].equals(ASSOCIATED_DAY)) {
                    
                    dayOffset_expired = preferences.getInt(EXPIRED_ITEMS_OFFSET, Keys.SETTINGS_DEFAULT_EXPIRED_ITEMS_OFFSET);
                    dayOffset_upcoming = preferences.getInt(UPCOMING_ITEMS_OFFSET, Keys.SETTINGS_DEFAULT_UPCOMING_ITEMS_OFFSET);
                    
                    setParams((String[]) addAll(group.params, params));
                    
                    long days_associated = currentDay;
                    long days_from_param = Long.parseLong(params[i + 1]);
                    if (days_from_param != DAY_FLAG_GLOBAL) {
                        days_associated = days_from_param;
                    }
                    
                    if (days_from_param == currentDay || days_from_param == DAY_FLAG_GLOBAL) {
                        
                        bgColor = preferences.getInt(Keys.TODAY_BG_COLOR, Keys.SETTINGS_DEFAULT_TODAY_BG_COLOR);
                        bevelColor = preferences.getInt(Keys.TODAY_BEVEL_COLOR, Keys.SETTINGS_DEFAULT_TODAY_BEVEL_COLOR);
                        bevelThickness = preferences.getInt(Keys.TODAY_BEVEL_THICKNESS, Keys.SETTINGS_DEFAULT_TODAY_BEVEL_THICKNESS);
                        
                        setFontColor(Keys.TODAY_FONT_COLOR, Keys.SETTINGS_DEFAULT_TODAY_FONT_COLOR);
                        
                        showOnLock = true;
                        showInList_ifCompleted = true;
                        
                        if (days_from_param == DAY_FLAG_GLOBAL) {
                            showOnLock = preferences.getBoolean(Keys.SHOW_GLOBAL_ITEMS_LOCK, Keys.SETTINGS_DEFAULT_SHOW_GLOBAL_ITEMS_LOCK);
                            setEntryType(EntryType.GLOBAL);
                        } else {
                            setEntryType(EntryType.TODAY);
                        }
                        
                    } else if (days_associated < currentDay && currentDay - days_associated <= dayOffset_expired) {
                        
                        bgColor = preferences.getInt(Keys.EXPIRED_BG_COLOR, Keys.SETTINGS_DEFAULT_EXPIRED_BG_COLOR);
                        bevelColor = preferences.getInt(Keys.EXPIRED_BEVEL_COLOR, Keys.SETTINGS_DEFAULT_EXPIRED_BEVEL_COLOR);
                        bevelThickness = preferences.getInt(Keys.EXPIRED_BEVEL_THICKNESS, Keys.SETTINGS_DEFAULT_EXPIRED_BEVEL_THICKNESS);
                        
                        setFontColor(Keys.EXPIRED_FONT_COLOR, Keys.SETTINGS_DEFAULT_EXPIRED_FONT_COLOR);
                        
                        showInList_ifCompleted = preferences.getBoolean(Keys.SHOW_EXPIRED_COMPLETED_ITEMS_IN_LIST, Keys.SETTINGS_DEFAULT_SHOW_EXPIRED_COMPLETED_ITEMS_IN_LIST);
                        showOnLock = true;
                        
                        setEntryType(EntryType.EXPIRED);
                    } else if (days_associated > currentDay && days_associated - currentDay <= dayOffset_upcoming) {
                        
                        bgColor = preferences.getInt(Keys.UPCOMING_BG_COLOR, Keys.SETTINGS_DEFAULT_UPCOMING_BG_COLOR);
                        bevelColor = preferences.getInt(Keys.UPCOMING_BEVEL_COLOR, Keys.SETTINGS_DEFAULT_UPCOMING_BEVEL_COLOR);
                        bevelThickness = preferences.getInt(Keys.UPCOMING_BEVEL_THICKNESS, Keys.SETTINGS_DEFAULT_UPCOMING_BEVEL_THICKNESS);
                        
                        setFontColor(Keys.UPCOMING_FONT_COLOR, Keys.SETTINGS_DEFAULT_UPCOMING_FONT_COLOR);
                        
                        showInList_ifCompleted = preferences.getBoolean(Keys.SHOW_UPCOMING_COMPLETED_ITEMS_IN_LIST, Keys.SETTINGS_DEFAULT_SHOW_UPCOMING_COMPLETED_ITEMS_IN_LIST);
                        showOnLock = true;
                        
                        setEntryType(EntryType.UPCOMING);
                    } else {
                        setFontColor(Keys.SETTINGS_DEFAULT_TODAY_FONT_COLOR);
                        
                        bgColor = Keys.SETTINGS_DEFAULT_TODAY_BG_COLOR;
                        bevelColor = Keys.SETTINGS_DEFAULT_TODAY_BEVEL_COLOR;
                        bevelThickness = Keys.SETTINGS_DEFAULT_TODAY_BEVEL_THICKNESS;
                        
                        showInList_ifCompleted = true;
                        showOnLock = false;
                        
                        setEntryType(EntryType.NEUTRAL);
                    }
                    break;
                }
            }
            
            adaptiveColorEnabled = preferences.getBoolean(Keys.ADAPTIVE_COLOR_ENABLED, Keys.SETTINGS_DEFAULT_ADAPTIVE_COLOR_ENABLED);
            adaptiveColorBalance = preferences.getInt(Keys.ADAPTIVE_COLOR_BALANCE, Keys.SETTINGS_DEFAULT_ADAPTIVE_COLOR_BALANCE);
            adaptiveColor = 0xff_FFFFFF;
            priority = ENTITY_SETTINGS_DEFAULT_PRIORITY;
            setParams((String[]) addAll(group.params, params));
            
        } else {
            
            timestamp_start = event.start;
            timestamp_end = event.end;
            timestamp_duration = event.duration;
            allDay = event.allDay;
            recurrenceSet = event.rSet;
            
            day_start = daysFromEpoch(timestamp_start);
            day_end = daysFromEpoch(timestamp_end);
            duration_in_days = daysFromEpoch(timestamp_duration);
            
            textValue = event.title.trim();
            
            ArrayList<String> calendarSubKeys = event.subKeys;
            
            fontColor = preferences.getInt(getFirstValidKey(calendarSubKeys, Keys.FONT_COLOR), Keys.SETTINGS_DEFAULT_TODAY_FONT_COLOR);
            bgColor = preferences.getInt(getFirstValidKey(calendarSubKeys, Keys.BG_COLOR), Keys.SETTINGS_DEFAULT_TODAY_BG_COLOR);
            bevelColor = preferences.getInt(getFirstValidKey(calendarSubKeys, Keys.BEVEL_COLOR), Keys.SETTINGS_DEFAULT_TODAY_BEVEL_COLOR);
            bevelThickness = preferences.getInt(getFirstValidKey(calendarSubKeys, Keys.BEVEL_THICKNESS), Keys.SETTINGS_DEFAULT_TODAY_BEVEL_THICKNESS);
            
            adaptiveColorBalance = preferences.getInt(getFirstValidKey(calendarSubKeys, Keys.ADAPTIVE_COLOR_BALANCE), Keys.SETTINGS_DEFAULT_ADAPTIVE_COLOR_BALANCE);
            adaptiveColorEnabled = preferences.getBoolean(getFirstValidKey(calendarSubKeys, Keys.ADAPTIVE_COLOR_ENABLED), Keys.SETTINGS_DEFAULT_ADAPTIVE_COLOR_ENABLED);
            
            priority = preferences.getInt(getFirstValidKey(calendarSubKeys, Keys.PRIORITY), Keys.ENTITY_SETTINGS_DEFAULT_PRIORITY);
            
            dayOffset_upcoming = preferences.getInt(getFirstValidKey(calendarSubKeys, UPCOMING_ITEMS_OFFSET), Keys.SETTINGS_DEFAULT_UPCOMING_ITEMS_OFFSET);
            dayOffset_expired = preferences.getInt(getFirstValidKey(calendarSubKeys, EXPIRED_ITEMS_OFFSET), Keys.SETTINGS_DEFAULT_EXPIRED_ITEMS_OFFSET);
            
            long nearestDay = genNearestEventDay(currentDay);
            isNewEntry = currentDay + dayOffset_upcoming >= nearestDay && currentDay < nearestDay;
            isOldEntry = !isNewEntry && (currentDay - dayOffset_expired <= day_end && currentDay > day_end);
            
            if (isOldEntry) {
                bgColor = preferences.getInt(Keys.EXPIRED_BG_COLOR, Keys.SETTINGS_DEFAULT_EXPIRED_BG_COLOR);
                bevelColor = preferences.getInt(Keys.EXPIRED_BEVEL_COLOR, Keys.SETTINGS_DEFAULT_EXPIRED_BEVEL_COLOR);
                setFontColor(Keys.EXPIRED_FONT_COLOR, Keys.SETTINGS_DEFAULT_EXPIRED_FONT_COLOR);
            } else if (isNewEntry) {
                bgColor = preferences.getInt(Keys.UPCOMING_BG_COLOR, Keys.SETTINGS_DEFAULT_UPCOMING_BG_COLOR);
                bevelColor = preferences.getInt(Keys.UPCOMING_BEVEL_COLOR, Keys.SETTINGS_DEFAULT_UPCOMING_BEVEL_COLOR);
                setFontColor(Keys.UPCOMING_FONT_COLOR, Keys.SETTINGS_DEFAULT_UPCOMING_FONT_COLOR);
            }
            
            showOnLock = isVisible(currentDay) && preferences.getBoolean(getFirstValidKey(calendarSubKeys, Keys.SHOW_ON_LOCK), Keys.SETTINGS_DEFAULT_SHOW_ON_LOCK);
            showOnLock = showOnLock || isOldEntry || isNewEntry;
            
            adaptiveColor = 0xff_FFFFFF;
            setParams(params);
        }
    }
    
    private void setEntryType(EntryType entryType) {
        isTodayEntry = entryType == EntryType.TODAY;
        isGlobalEntry = entryType == EntryType.GLOBAL;
        isOldEntry = entryType == EntryType.EXPIRED;
        isNewEntry = entryType == EntryType.UPCOMING;
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
        textPaint.setTextSize(lockScreenBitmapDrawer.fontSize_h);
        textPaint.setTextAlign(Paint.Align.CENTER);
        rWidth = MathUtils.clamp(textPaint.measureText(lockScreenBitmapDrawer.currentBitmapLongestText), 1, lockScreenBitmapDrawer.displayWidth / 2f - bevelThickness);
        maxChars = (int) ((lockScreenBitmapDrawer.displayWidth - bevelThickness * 2) / (textPaint.measureText("qwerty_") / 5f)) - 2;
        
        log(INFO, "loaded display data for " + textValue);
    }
    
    public void initializeBgAndPadPaints() {
        if (adaptiveColorEnabled) {
            bgPaint = createNewPaint(mixTwoColors(bgColor, adaptiveColor, adaptiveColorBalance / 1000d));
            padPaint = createNewPaint(mixTwoColors(bevelColor, adaptiveColor, adaptiveColorBalance / 1000d));
        } else {
            bgPaint = createNewPaint(bgColor);
            padPaint = createNewPaint(bevelColor);
        }
    }
    
    public String getDayOffset(long day, Context context) {
        String dayOffset = "";
        if (day_start != DAY_FLAG_GLOBAL && day_end != DAY_FLAG_GLOBAL) {
            
            int dayShift = 0;
            
            long nearestDay = genNearestEventDay(day);
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
                case (BEVEL_THICKNESS):
                    bevelThickness = Integer.parseInt(params[i + 1]);
                    break;
                case (FONT_COLOR):
                    setFontColor(Integer.parseInt(params[i + 1]));
                    break;
                case (BG_COLOR):
                    bgColor = Integer.parseInt(params[i + 1]);
                    break;
                case (BEVEL_COLOR):
                    bevelColor = Integer.parseInt(params[i + 1]);
                    break;
                case (PRIORITY):
                    priority = Integer.parseInt(params[i + 1]);
                    break;
                case (ASSOCIATED_DAY):
                    day_start = Long.parseLong(params[i + 1]);
                    day_end = day_start;
                    break;
                case (ADAPTIVE_COLOR_ENABLED):
                    adaptiveColorEnabled = Boolean.parseBoolean(params[i + 1]);
                    break;
                case (ADAPTIVE_COLOR_BALANCE):
                    adaptiveColorBalance = Integer.parseInt(params[i + 1]);
                    break;
                default:
                    log(WARNING, "unknown parameter: " + params[i] + " entry textValue: " + textValue);
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
        for (int i = 0; i < group.params.length; i += 2) {
            if (group.params[i].equals(parameter)) {
                containedInGroupParams = true;
                break;
            }
        }
        for (int i = 0; i < params.length; i += 2) {
            if (params[i].equals(parameter)) {
                containedInPersonalParams = true;
                break;
            }
        }
        if (containedInGroupParams && containedInPersonalParams) {
            icon.setTextColor(Color.BLUE);
        } else if (containedInGroupParams) {
            icon.setTextColor(Color.YELLOW);
        } else if (containedInPersonalParams) {
            icon.setTextColor(Color.GREEN);
        } else {
            icon.setTextColor(Color.GRAY);
        }
    }
    
    @Override
    public int hashCode() {
        return Objects.hash(event, Arrays.hashCode(params));
    }
    
    @Override
    public boolean equals(@Nullable Object obj) {
        if (obj == null) {
            return false;
        } else if (obj instanceof TodoListEntry) {
            return Objects.equals(event, ((TodoListEntry) obj).event) &&
                    Arrays.equals(((TodoListEntry) obj).params, params);
        }
        return super.equals(obj);
    }
}
