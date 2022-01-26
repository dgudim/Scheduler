package prototype.xd.scheduler.entities;

import static org.apache.commons.lang.ArrayUtils.addAll;
import static prototype.xd.scheduler.MainActivity.displayMetrics;
import static prototype.xd.scheduler.MainActivity.preferences;
import static prototype.xd.scheduler.entities.Group.BLANK_GROUP_NAME;
import static prototype.xd.scheduler.utilities.BitmapUtilities.createNewPaint;
import static prototype.xd.scheduler.utilities.BitmapUtilities.mixTwoColors;
import static prototype.xd.scheduler.utilities.DateManager.timeZone_UTC;
import static prototype.xd.scheduler.utilities.DateManager.currentDay;
import static prototype.xd.scheduler.utilities.DateManager.dateFromEpoch;
import static prototype.xd.scheduler.utilities.DateManager.daysFromEpoch;
import static prototype.xd.scheduler.utilities.Keys.ADAPTIVE_COLOR_BALANCE;
import static prototype.xd.scheduler.utilities.Keys.ADAPTIVE_COLOR_ENABLED;
import static prototype.xd.scheduler.utilities.Keys.AFTER_ITEMS_OFFSET;
import static prototype.xd.scheduler.utilities.Keys.ASSOCIATED_DAY;
import static prototype.xd.scheduler.utilities.Keys.BEFOREHAND_ITEMS_OFFSET;
import static prototype.xd.scheduler.utilities.Keys.BEVEL_COLOR;
import static prototype.xd.scheduler.utilities.Keys.BEVEL_THICKNESS;
import static prototype.xd.scheduler.utilities.Keys.BG_COLOR;
import static prototype.xd.scheduler.utilities.Keys.DATE_FLAG_GLOBAL;
import static prototype.xd.scheduler.utilities.Keys.ENTITY_SETTINGS_DEFAULT_PRIORITY;
import static prototype.xd.scheduler.utilities.Keys.FONT_COLOR;
import static prototype.xd.scheduler.utilities.Keys.IS_COMPLETED;
import static prototype.xd.scheduler.utilities.Keys.PRIORITY;
import static prototype.xd.scheduler.utilities.Keys.SHOW_ON_LOCK;
import static prototype.xd.scheduler.utilities.Keys.TEXT_VALUE;
import static prototype.xd.scheduler.utilities.LockScreenBitmapDrawer.currentBitmapLongestText;
import static prototype.xd.scheduler.utilities.LockScreenBitmapDrawer.displayWidth;
import static prototype.xd.scheduler.utilities.Logger.ContentType.INFO;
import static prototype.xd.scheduler.utilities.Logger.ContentType.WARNING;
import static prototype.xd.scheduler.utilities.Logger.log;
import static prototype.xd.scheduler.utilities.SystemCalendarUtils.generateSubKeysFromKey;
import static prototype.xd.scheduler.utilities.SystemCalendarUtils.getFirstValidKey;
import static prototype.xd.scheduler.utilities.SystemCalendarUtils.makeKey;
import static prototype.xd.scheduler.utilities.Utilities.makeNewLines;

import android.content.Context;
import android.graphics.Color;
import android.graphics.Paint;
import android.util.TypedValue;
import android.widget.TextView;

import androidx.core.math.MathUtils;

import org.dmfs.rfc5545.recurrenceset.RecurrenceSet;
import org.dmfs.rfc5545.recurrenceset.RecurrenceSetIterator;

import java.util.ArrayList;

import prototype.xd.scheduler.R;
import prototype.xd.scheduler.entities.calendars.SystemCalendarEvent;
import prototype.xd.scheduler.utilities.DateManager;
import prototype.xd.scheduler.utilities.Keys;

public class TodoListEntry {
    
    enum EntryType {GLOBAL, TODAY, OLD, NEW, NEUTRAL}
    
    public boolean fromSystemCalendar = false;
    public SystemCalendarEvent event;
    
    public long timestamp_start = 0;
    public long timestamp_end = 0;
    public long timestamp_duration = 0;
    public boolean allDay = false;
    public RecurrenceSet recurrenceSet;
    
    public long day_start = DATE_FLAG_GLOBAL;
    public long day_end = DATE_FLAG_GLOBAL;
    public int dayOffset_after = 0;
    public int dayOffset_beforehand = 0;
    public boolean completed = false;
    public Group group;
    
    public int bgColor;
    public int fontColor;
    public int fontColor_completed;
    public int bevelColor;
    
    public boolean adaptiveColorEnabled;
    public int adaptiveColorBalance;
    public int adaptiveColor;
    
    public int fontSize = 0;
    public float h = 0;
    public float kM = 0;
    public int maxChars = 0;
    public float rWidth = 0;
    
    public int bevelThickness = 0;
    
    public int priority = 0;
    
    public boolean showOnLock = false;
    public boolean showInList_ifCompleted = false;
    
    public static final String blankTextValue = "_BLANK_";
    public String textValue = blankTextValue;
    public String dayOffset = "";
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
    
    public boolean isVisible(long day) {
        if(recurrenceSet != null){
            if(day > day_end){
                return false;
            }
            RecurrenceSetIterator it = recurrenceSet.iterator(timeZone_UTC, timestamp_start);
            long instance = 0;
            while (it.hasNext() && !recurrenceSet.isInfinite() && daysFromEpoch(instance) <= day)
            {
                instance = it.next();
                if(daysFromEpoch(instance) == day){
                    return true;
                }
            }
            return false;
        }
        return day >= day_start && day <= day_end;
    }
    
    public String getTimeSpan(Context context) {
        if (allDay) {
            return context.getString(R.string.calendar_event_all_day);
        }
        if(recurrenceSet != null){
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
                    
                    dayOffset_after = preferences.getInt(Keys.AFTER_ITEMS_OFFSET, Keys.SETTINGS_DEFAULT_AFTER_ITEMS_OFFSET);
                    dayOffset_beforehand = preferences.getInt(Keys.BEFOREHAND_ITEMS_OFFSET, Keys.SETTINGS_DEFAULT_BEFOREHAND_ITEMS_OFFSET);
                    
                    setParams((String[]) addAll(group.params, params));
                    
                    long days_associated = currentDay;
                    long days_from_param = Long.parseLong(params[i + 1]);
                    if (days_from_param != DATE_FLAG_GLOBAL) {
                        days_associated = days_from_param;
                    }
                    
                    if (days_from_param == currentDay || days_from_param == DATE_FLAG_GLOBAL) {
                        
                        bgColor = preferences.getInt(Keys.TODAY_BG_COLOR, Keys.SETTINGS_DEFAULT_TODAY_BG_COLOR);
                        bevelColor = preferences.getInt(Keys.TODAY_BEVEL_COLOR, Keys.SETTINGS_DEFAULT_TODAY_BEVEL_COLOR);
                        bevelThickness = preferences.getInt(Keys.TODAY_BEVEL_THICKNESS, Keys.SETTINGS_DEFAULT_TODAY_BEVEL_THICKNESS);
                        
                        setFontColor(Keys.TODAY_FONT_COLOR, Keys.SETTINGS_DEFAULT_TODAY_FONT_COLOR);
                        
                        showOnLock = true;
                        showInList_ifCompleted = true;
                        
                        if (days_from_param == DATE_FLAG_GLOBAL) {
                            showOnLock = preferences.getBoolean(Keys.SHOW_GLOBAL_ITEMS_LOCK, Keys.SETTINGS_DEFAULT_SHOW_GLOBAL_ITEMS_LOCK);
                            setEntryType(EntryType.GLOBAL);
                        } else {
                            setEntryType(EntryType.TODAY);
                        }
                        
                    } else if (days_associated < currentDay && currentDay - days_associated < dayOffset_after) {
                        
                        bgColor = preferences.getInt(Keys.OLD_BG_COLOR, Keys.SETTINGS_DEFAULT_OLD_BG_COLOR);
                        bevelColor = preferences.getInt(Keys.OLD_BEVEL_COLOR, Keys.SETTINGS_DEFAULT_OLD_BEVEL_COLOR);
                        bevelThickness = preferences.getInt(Keys.OLD_BEVEL_THICKNESS, Keys.SETTINGS_DEFAULT_OLD_BEVEL_THICKNESS);
                        
                        setFontColor(Keys.OLD_FONT_COLOR, Keys.SETTINGS_DEFAULT_OLD_FONT_COLOR);
                        
                        showInList_ifCompleted = preferences.getBoolean(Keys.SHOW_OLD_COMPLETED_ITEMS_IN_LIST, Keys.SETTINGS_DEFAULT_SHOW_OLD_COMPLETED_ITEMS_IN_LIST);
                        showOnLock = true;
                        
                        setEntryType(EntryType.OLD);
                    } else if (days_associated > currentDay && days_associated - currentDay < dayOffset_beforehand) {
                        
                        bgColor = preferences.getInt(Keys.NEW_BG_COLOR, Keys.SETTINGS_DEFAULT_NEW_BG_COLOR);
                        bevelColor = preferences.getInt(Keys.NEW_BEVEL_COLOR, Keys.SETTINGS_DEFAULT_NEW_BEVEL_COLOR);
                        bevelThickness = preferences.getInt(Keys.NEW_BEVEL_THICKNESS, Keys.SETTINGS_DEFAULT_NEW_BEVEL_THICKNESS);
                        
                        setFontColor(Keys.NEW_FONT_COLOR, Keys.SETTINGS_DEFAULT_NEW_FONT_COLOR);
                        
                        showInList_ifCompleted = preferences.getBoolean(Keys.SHOW_NEW_COMPLETED_ITEMS_IN_LIST, Keys.SETTINGS_DEFAULT_SHOW_NEW_COMPLETED_ITEMS_IN_LIST);
                        showOnLock = true;
                        
                        setEntryType(EntryType.NEW);
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
            
            fontSize = preferences.getInt(Keys.FONT_SIZE, Keys.SETTINGS_DEFAULT_FONT_SIZE);
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
    
            textValue = event.title;
    
            ArrayList<String> calendarSubKeys = generateSubKeysFromKey(makeKey(event));
    
            fontColor = preferences.getInt(getFirstValidKey(calendarSubKeys, Keys.FONT_COLOR), Keys.SETTINGS_DEFAULT_TODAY_FONT_COLOR);
            bgColor = preferences.getInt(getFirstValidKey(calendarSubKeys, Keys.BG_COLOR), Keys.SETTINGS_DEFAULT_TODAY_BG_COLOR);
            bevelColor = preferences.getInt(getFirstValidKey(calendarSubKeys, Keys.BEVEL_COLOR), Keys.SETTINGS_DEFAULT_TODAY_BEVEL_COLOR);
            bevelThickness = preferences.getInt(getFirstValidKey(calendarSubKeys, Keys.BEVEL_THICKNESS), Keys.SETTINGS_DEFAULT_TODAY_BEVEL_THICKNESS);
    
            adaptiveColorBalance = preferences.getInt(getFirstValidKey(calendarSubKeys, Keys.ADAPTIVE_COLOR_BALANCE), Keys.SETTINGS_DEFAULT_ADAPTIVE_COLOR_BALANCE);
            adaptiveColorEnabled = preferences.getBoolean(getFirstValidKey(calendarSubKeys, Keys.ADAPTIVE_COLOR_ENABLED), Keys.SETTINGS_DEFAULT_ADAPTIVE_COLOR_ENABLED);
    
            priority = preferences.getInt(getFirstValidKey(calendarSubKeys, Keys.PRIORITY), Keys.ENTITY_SETTINGS_DEFAULT_PRIORITY);
    
            dayOffset_beforehand = preferences.getInt(getFirstValidKey(calendarSubKeys, Keys.BEFOREHAND_ITEMS_OFFSET), Keys.SETTINGS_DEFAULT_BEFOREHAND_ITEMS_OFFSET);
            dayOffset_after = preferences.getInt(getFirstValidKey(calendarSubKeys, Keys.AFTER_ITEMS_OFFSET), Keys.SETTINGS_DEFAULT_AFTER_ITEMS_OFFSET);
    
            isNewEntry = currentDay + dayOffset_beforehand >= day_start && currentDay < day_start;
            isOldEntry = currentDay - dayOffset_after <= day_end && currentDay > day_end;
    
            if (isOldEntry) {
                bgColor = preferences.getInt(Keys.OLD_BG_COLOR, Keys.SETTINGS_DEFAULT_OLD_BG_COLOR);
                bevelColor = preferences.getInt(Keys.OLD_BEVEL_COLOR, Keys.SETTINGS_DEFAULT_OLD_BEVEL_COLOR);
                setFontColor(Keys.OLD_FONT_COLOR, Keys.SETTINGS_DEFAULT_OLD_FONT_COLOR);
            } else if (isNewEntry) {
                bgColor = preferences.getInt(Keys.NEW_BG_COLOR, Keys.SETTINGS_DEFAULT_NEW_BG_COLOR);
                bevelColor = preferences.getInt(Keys.NEW_BEVEL_COLOR, Keys.SETTINGS_DEFAULT_NEW_BEVEL_COLOR);
                setFontColor(Keys.NEW_FONT_COLOR, Keys.SETTINGS_DEFAULT_NEW_FONT_COLOR);
            }
    
            showOnLock = isVisible(currentDay) && preferences.getBoolean(getFirstValidKey(calendarSubKeys, Keys.SHOW_ON_LOCK), Keys.CALENDAR_SETTINGS_DEFAULT_SHOW_ON_LOCK);
            showOnLock = showOnLock || isOldEntry || isNewEntry;
            
            fontSize = preferences.getInt(Keys.FONT_SIZE, Keys.SETTINGS_DEFAULT_FONT_SIZE);
            adaptiveColor = 0xff_FFFFFF;
            setParams(params);
        }
    }
    
    private void setEntryType(EntryType entryType) {
        isTodayEntry = entryType == EntryType.TODAY;
        isGlobalEntry = entryType == EntryType.GLOBAL;
        isOldEntry = entryType == EntryType.OLD;
        isNewEntry = entryType == EntryType.NEW;
    }
    
    private void setFontColor(int color) {
        fontColor = color;
        fontColor_completed = mixTwoColors(fontColor, 0xff_FFFFFF, 0.5);
    }
    
    private void setFontColor(String key, int defaultColor) {
        setFontColor(preferences.getInt(key, defaultColor));
    }
    
    public void loadDisplayData() {
        h = TypedValue.applyDimension(TypedValue.COMPLEX_UNIT_SP, fontSize, displayMetrics);
        kM = h * 1.1f;
        textPaint = createNewPaint(fontColor);
        textPaint.setTextSize(h);
        textPaint.setTextAlign(Paint.Align.CENTER);
        rWidth = MathUtils.clamp(textPaint.measureText(currentBitmapLongestText), 1, displayWidth / 2f - bevelThickness);
        maxChars = (int) ((displayWidth - bevelThickness * 2) / (textPaint.measureText("qwerty_") / 5f)) - 2;
        
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
    
    public String getDayOffset() {
        if (day_start != DATE_FLAG_GLOBAL && day_end != DATE_FLAG_GLOBAL) {
            dayOffset = "";
            
            int dayShift = 0;
            
            if (currentDay < day_start) {
                dayShift = (int) (day_start - currentDay);
            } else if (currentDay > day_end) {
                dayShift = (int) (day_end - currentDay);
            }
            
            if (dayShift < 31 && dayShift > -31) {
                if (dayShift > 0) {
                    switch (dayShift) {
                        case (1):
                            dayOffset += " (Завтра)";
                            break;
                        case (2):
                        case (3):
                        case (4):
                        case (22):
                        case (23):
                        case (24):
                            dayOffset += " (Через " + dayShift + " дня)";
                            break;
                        default:
                            dayOffset += " (Через " + dayShift + " дней)";
                            break;
                    }
                } else if (dayShift < 0) {
                    switch (dayShift) {
                        case (-1):
                            dayOffset += " (Вчера)";
                            break;
                        case (-2):
                        case (-3):
                        case (-4):
                        case (-22):
                        case (-23):
                        case (-24):
                            dayOffset += " (" + -dayShift + " дня назад)";
                            break;
                        default:
                            dayOffset += " (" + -dayShift + " дней назад)";
                            break;
                    }
                }
            } else {
                if (dayShift < 0) {
                    dayOffset += " (> месяца назад)";
                } else {
                    dayOffset += " (> чем через месяц)";
                }
            }
        }
        return dayOffset;
    }
    
    public void splitText() {
        textValueSplit = makeNewLines(textValue + getDayOffset(), maxChars);
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
                case (BEFOREHAND_ITEMS_OFFSET):
                    dayOffset_beforehand = Integer.parseInt(params[i + 1]);
                    break;
                case (AFTER_ITEMS_OFFSET):
                    dayOffset_after = Integer.parseInt(params[i + 1]);
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
}
