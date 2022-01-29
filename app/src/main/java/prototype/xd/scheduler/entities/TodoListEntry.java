package prototype.xd.scheduler.entities;

import static prototype.xd.scheduler.MainActivity.preferences;
import static prototype.xd.scheduler.entities.Group.BLANK_GROUP_NAME;
import static prototype.xd.scheduler.utilities.BitmapUtilities.createNewPaint;
import static prototype.xd.scheduler.utilities.BitmapUtilities.mixTwoColors;
import static prototype.xd.scheduler.utilities.DateManager.currentDay;
import static prototype.xd.scheduler.utilities.DateManager.dateFromEpoch;
import static prototype.xd.scheduler.utilities.DateManager.daysFromEpoch;
import static prototype.xd.scheduler.utilities.DateManager.timeZone_UTC;
import static prototype.xd.scheduler.utilities.Keys.ADAPTIVE_COLOR_BALANCE;
import static prototype.xd.scheduler.utilities.Keys.ADAPTIVE_COLOR_ENABLED;
import static prototype.xd.scheduler.utilities.Keys.ASSOCIATED_DAY;
import static prototype.xd.scheduler.utilities.Keys.BLANK_TEXT;
import static prototype.xd.scheduler.utilities.Keys.DAY_FLAG_GLOBAL;
import static prototype.xd.scheduler.utilities.Keys.ENTITY_SETTINGS_DEFAULT_PRIORITY;
import static prototype.xd.scheduler.utilities.Keys.IS_COMPLETED;
import static prototype.xd.scheduler.utilities.Keys.PRIORITY;
import static prototype.xd.scheduler.utilities.Keys.SETTINGS_DEFAULT_ADAPTIVE_COLOR_BALANCE;
import static prototype.xd.scheduler.utilities.Keys.SETTINGS_DEFAULT_ADAPTIVE_COLOR_ENABLED;
import static prototype.xd.scheduler.utilities.Keys.TEXT_VALUE;
import static prototype.xd.scheduler.utilities.Logger.ContentType.INFO;
import static prototype.xd.scheduler.utilities.Logger.log;
import static prototype.xd.scheduler.utilities.SystemCalendarUtils.getFirstValidKey;
import static prototype.xd.scheduler.utilities.Utilities.makeNewLines;

import android.content.Context;
import android.graphics.Color;
import android.graphics.Paint;
import android.widget.TextView;

import androidx.annotation.Nullable;
import androidx.core.math.MathUtils;

import org.apache.commons.lang.SerializationUtils;
import org.dmfs.rfc5545.recurrenceset.RecurrenceSet;
import org.dmfs.rfc5545.recurrenceset.RecurrenceSetIterator;

import java.util.HashMap;
import java.util.Objects;

import prototype.xd.scheduler.R;
import prototype.xd.scheduler.entities.calendars.SystemCalendarEvent;
import prototype.xd.scheduler.utilities.DateManager;
import prototype.xd.scheduler.utilities.Keys;
import prototype.xd.scheduler.utilities.LockScreenBitmapDrawer;

public class TodoListEntry {
    
    public boolean fromSystemCalendar = false;
    public SystemCalendarEvent event;
    
    public long timestamp_start = 0;
    public long timestamp_end = 0;
    public long timestamp_duration = 0;
    
    public long day_start = DAY_FLAG_GLOBAL;
    public long day_end = DAY_FLAG_GLOBAL;
    public long duration_in_days = 0;
    
    public boolean allDay = false;
    public RecurrenceSet recurrenceSet;
    
    private String textValue;
    public HashMap<String, Integer> params;
    public Group group;
    
    public Paint textPaint;
    public Paint padPaint;
    public Paint bgPaint;
    public int adaptiveColor = 0xff_FFFFFF;
    
    public float rWidth;
    public String[] textValue_split;
    
    public TodoListEntry() {
        params = new HashMap<>(0);
        textValue = BLANK_TEXT;
    }
    
    public TodoListEntry(SystemCalendarEvent event) {
        fromSystemCalendar = true;
        this.event = event;
        
        textValue = event.title;
        timestamp_start = event.start;
        timestamp_end = event.end;
        timestamp_duration = event.duration;
        allDay = event.allDay;
        recurrenceSet = event.rSet;
        
        day_start = daysFromEpoch(timestamp_start);
        day_end = daysFromEpoch(timestamp_end);
        duration_in_days = daysFromEpoch(timestamp_duration);
        
        params = new HashMap<>(0);
        resetGroup();
    }
    
    public TodoListEntry(String textValue, HashMap<String, Integer> params, String groupName) {
        group = new Group(groupName);
        this.params = params;
        
        this.textValue = textValue;
        day_start = Objects.requireNonNull(params.get(ASSOCIATED_DAY));
        day_end = day_start;
    }
    
    public void changeGroup(String groupName) {
        group = new Group(groupName);
    }
    
    public void resetGroup() {
        changeGroup(BLANK_GROUP_NAME);
    }
    
    public void changeGroup(Group group) {
        this.group = group;
    }
    
    private boolean inRange(long day, long event_start_day) {
        return isGlobal() || (day >= event_start_day - getDayOffset_upcoming() && day <= event_start_day + duration_in_days + getDayOffset_expired());
        // | days after the event ended ------ event start |event| event end ------ days before the event starts |
        // | ++++++++++++++++++++++++++       -------------|-----|----------        +++++++++++++++++++++++++++++|
    }
    
    public boolean isGlobal() {
        return day_start == DAY_FLAG_GLOBAL || day_end == DAY_FLAG_GLOBAL;
    }
    
    private boolean isToday(long day, long event_start_day) {
        return day >= event_start_day && day <= event_start_day + timestamp_duration;
    }
    
    public boolean isExpired(long day, long event_start_day) {
        long event_end_day = event_start_day + duration_in_days;
        return day <= event_end_day + getDayOffset_expired() && day > event_end_day;
    }
    
    public boolean isUpcoming(long day, long event_start_day) {
        return day >= event_start_day - getDayOffset_upcoming() && day < event_start_day;
    }
    
    public boolean isToday(long day) {
        return isToday(day, getNearestEventDay(day));
    }
    
    public boolean isExpired(long day) {
        return isExpired(day, getNearestEventDay(day));
    }
    
    public boolean isUpcoming(long day) {
        return isUpcoming(day, getNearestEventDay(day));
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
    
    public boolean isVisibleOnLock() {
        long day = currentDay;
        if (recurrenceSet != null) {
            if (day > day_end) {
                return false;
            }
            RecurrenceSetIterator it = recurrenceSet.iterator(timeZone_UTC, timestamp_start);
            long instance = 0;
            while (it.hasNext() && daysFromEpoch(instance) <= day) {
                instance = it.next();
                boolean visible = inRange(day, daysFromEpoch(instance));
                if (!visible) continue;
                return showOnLock();
            }
            return false;
        }
        boolean visible = inRange(day, day_start);
        if (isGlobal()) {
            visible = visible && preferences.getBoolean(Keys.SHOW_GLOBAL_ITEMS_LOCK, Keys.SETTINGS_DEFAULT_SHOW_GLOBAL_ITEMS_LOCK);
        } else if (isCompleted() && isExpired(day)) {
            visible = visible && preferences.getBoolean(Keys.SHOW_EXPIRED_COMPLETED_ITEMS_IN_LIST, Keys.SETTINGS_DEFAULT_SHOW_EXPIRED_COMPLETED_ITEMS_IN_LIST);
        } else if (isCompleted() && isUpcoming(day)) {
            visible = visible && preferences.getBoolean(Keys.SHOW_UPCOMING_COMPLETED_ITEMS_IN_LIST, Keys.SETTINGS_DEFAULT_SHOW_UPCOMING_COMPLETED_ITEMS_IN_LIST);
        }
        return visible && !isCompleted() && showOnLock();
    }
    
    public boolean showOnLock() {
        return getParameter_generic(Keys.SHOW_ON_LOCK, Keys.SETTINGS_DEFAULT_SHOW_ON_LOCK ? 1 : 0) > 0;
    }
    
    private long getNearestEventDay(long relative_to) {
        if (recurrenceSet != null) {
            if (relative_to > day_end) {
                return day_end;
            }
            RecurrenceSetIterator it = recurrenceSet.iterator(timeZone_UTC, timestamp_start);
            while (it.hasNext()) {
                long epoch = it.next();
                if (daysFromEpoch(epoch) >= relative_to) {
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
    
    public int getParameter(String parameterKey, int fallbackValue) {
        Integer param = params.get(parameterKey);
        if (param == null) {
            if(group == null){
                return fallbackValue;
            }
            param = group.params.get(parameterKey);
            if (param == null) {
                return fallbackValue;
            }
        }
        return param;
    }
    
    private int tryRead(String parameterKey, int defaultValue) {
        int toReturn;
        try {
            toReturn = preferences.getInt(parameterKey, defaultValue);
        } catch (ClassCastException e) {
            toReturn = preferences.getBoolean(parameterKey, defaultValue > 0) ? 1 : 0;
        }
        return toReturn;
    }
    
    public int getParameter_generic(String parameterKey, int defaultValue) {
        if (fromSystemCalendar) {
            return getParameter(parameterKey, tryRead(getFirstValidKey(event.subKeys, parameterKey), defaultValue));
        } else {
            return getParameter(parameterKey, tryRead(parameterKey, defaultValue));
        }
    }
    
    public void setTextValue(String textValue) {
        if (fromSystemCalendar) {
            event.title = textValue;
        } else {
            this.textValue = textValue;
        }
    }
    
    public String getTextValue() {
        if (fromSystemCalendar) {
            return event.title;
        } else {
            return textValue;
        }
    }
    
    public int getFontColor(long day) {
        int defaultValue;
        if (isExpired(day)) {
            defaultValue = preferences.getInt(Keys.EXPIRED_FONT_COLOR, Keys.SETTINGS_DEFAULT_EXPIRED_FONT_COLOR);
        } else if (isUpcoming(day)) {
            defaultValue = preferences.getInt(Keys.UPCOMING_FONT_COLOR, Keys.SETTINGS_DEFAULT_UPCOMING_FONT_COLOR);
        } else {
            defaultValue = preferences.getInt(Keys.TODAY_FONT_COLOR, Keys.SETTINGS_DEFAULT_TODAY_FONT_COLOR);
        }
        if (isCompleted()) {
            return mixTwoColors(getParameter_generic(Keys.FONT_COLOR, defaultValue), 0xff_FFFFFF, 0.5);
        } else {
            return getParameter_generic(Keys.FONT_COLOR, defaultValue);
        }
    }
    
    public int getBgColor(long day) {
        int defaultValue;
        if (isExpired(day)) {
            defaultValue = preferences.getInt(Keys.EXPIRED_BG_COLOR, Keys.SETTINGS_DEFAULT_EXPIRED_BG_COLOR);
        } else if (isUpcoming(day)) {
            defaultValue = preferences.getInt(Keys.UPCOMING_BG_COLOR, Keys.SETTINGS_DEFAULT_UPCOMING_BG_COLOR);
        } else {
            defaultValue = preferences.getInt(Keys.TODAY_BG_COLOR, Keys.SETTINGS_DEFAULT_TODAY_BG_COLOR);
        }
        return getParameter_generic(Keys.BG_COLOR, defaultValue);
    }
    
    public int getBgColor_lock() {
        if (isAdaptiveColorEnabled()) {
            return mixTwoColors(getBgColor(currentDay), adaptiveColor, getAdaptiveColorBalance() / 1000d);
        } else {
            return getBgColor(currentDay);
        }
    }
    
    public int getBevelColor(long day) {
        int defaultValue;
        if (isExpired(day)) {
            defaultValue = preferences.getInt(Keys.EXPIRED_BEVEL_COLOR, Keys.SETTINGS_DEFAULT_EXPIRED_BEVEL_COLOR);
        } else if (isUpcoming(day)) {
            defaultValue = preferences.getInt(Keys.UPCOMING_BEVEL_COLOR, Keys.SETTINGS_DEFAULT_UPCOMING_BEVEL_COLOR);
        } else {
            defaultValue = preferences.getInt(Keys.TODAY_BEVEL_COLOR, Keys.SETTINGS_DEFAULT_TODAY_BEVEL_COLOR);
        }
        return getParameter_generic(Keys.BEVEL_COLOR, defaultValue);
    }
    
    public int getBevelColor_lock() {
        if (isAdaptiveColorEnabled()) {
            return mixTwoColors(getBevelColor(currentDay), adaptiveColor, getAdaptiveColorBalance() / 1000d);
        } else {
            return getBevelColor(currentDay);
        }
    }
    
    public int getBevelThickness(long day) {
        int defaultValue;
        if (isExpired(day)) {
            defaultValue = preferences.getInt(Keys.EXPIRED_BEVEL_THICKNESS, Keys.SETTINGS_DEFAULT_EXPIRED_BEVEL_THICKNESS);
        } else if (isUpcoming(day)) {
            defaultValue = preferences.getInt(Keys.UPCOMING_BEVEL_THICKNESS, Keys.SETTINGS_DEFAULT_UPCOMING_BEVEL_THICKNESS);
        } else {
            defaultValue = preferences.getInt(Keys.TODAY_BEVEL_THICKNESS, Keys.SETTINGS_DEFAULT_TODAY_BEVEL_THICKNESS);
        }
        return getParameter_generic(Keys.BEVEL_THICKNESS, defaultValue);
    }
    
    public boolean isAdaptiveColorEnabled() {
        return getParameter_generic(ADAPTIVE_COLOR_ENABLED, SETTINGS_DEFAULT_ADAPTIVE_COLOR_ENABLED ? 1 : 0) > 0;
    }
    
    public int getAdaptiveColorBalance() {
        return getParameter_generic(ADAPTIVE_COLOR_BALANCE, SETTINGS_DEFAULT_ADAPTIVE_COLOR_BALANCE);
    }
    
    public int getPriority() {
        return getParameter_generic(PRIORITY, ENTITY_SETTINGS_DEFAULT_PRIORITY);
    }
    
    public boolean isCompleted() {
        return getParameter(IS_COMPLETED, 0) > 0;
    }
    
    public int getDayOffset_expired() {
        return getParameter_generic(Keys.EXPIRED_ITEMS_OFFSET, Keys.SETTINGS_DEFAULT_EXPIRED_ITEMS_OFFSET);
    }
    
    public int getDayOffset_upcoming() {
        return getParameter_generic(Keys.UPCOMING_ITEMS_OFFSET, Keys.SETTINGS_DEFAULT_UPCOMING_ITEMS_OFFSET);
    }
    
    @SuppressWarnings("unchecked")
    public HashMap<String, Integer> getDisplayParams() {
        HashMap<String, Integer> displayParams = (HashMap<String, Integer>) SerializationUtils.clone(params);
        displayParams.remove(TEXT_VALUE);
        displayParams.remove(ASSOCIATED_DAY);
        displayParams.remove(IS_COMPLETED);
        return displayParams;
    }
    
    public void removeDisplayParams() {
        HashMap<String, Integer> newParams = new HashMap<>(3);
        newParams.put(TEXT_VALUE, params.get(TEXT_VALUE));
        newParams.put(ASSOCIATED_DAY, params.get(ASSOCIATED_DAY));
        newParams.put(IS_COMPLETED, params.get(IS_COMPLETED));
        params = newParams;
    }
    
    public void loadDisplayData(LockScreenBitmapDrawer lockScreenBitmapDrawer) {
        
        textPaint = createNewPaint(getFontColor(currentDay));
        textPaint.setTextSize(lockScreenBitmapDrawer.fontSize_scaled);
        textPaint.setTextAlign(Paint.Align.CENTER);
        rWidth = MathUtils.clamp(textPaint.measureText(lockScreenBitmapDrawer.currentBitmapLongestText), 1,
                lockScreenBitmapDrawer.displayWidth / 2f - getBevelThickness(currentDay));
        
        log(INFO, "loaded display data for " + textValue);
    }
    
    public void initializeBgAndPadPaints() {
        bgPaint = createNewPaint(getBgColor_lock());
        padPaint = createNewPaint(getBevelColor_lock());
    }
    
    public void splitText(Context context, int maxChars) {
        textValue_split = makeNewLines(getTextValue() + getDayOffset(currentDay) + "\n" + getTimeSpan(context), maxChars);
    }
    
    public String getDayOffset(long day) {
        String dayOffset = "";
        if (!isGlobal()) {
            
            int dayShift = 0;
            
            long nearestDay = getNearestEventDay(day);
            if (day < nearestDay) {
                dayShift = (int) (nearestDay - day);
            } else if (day > day_end) {
                dayShift = (int) (day_end - day);
            }
            
            if (dayShift < 31 && dayShift > -31) {
                if (dayShift > 0) {
                    switch (dayShift) {
                        case (1):
                            dayOffset = " (Завтра)";
                            break;
                        case (2):
                        case (3):
                        case (4):
                        case (22):
                        case (23):
                        case (24):
                            dayOffset = " (Через " + dayShift + " дня)";
                            break;
                        default:
                            dayOffset = " (Через " + dayShift + " дней)";
                            break;
                    }
                } else if (dayShift < 0) {
                    switch (dayShift) {
                        case (-1):
                            dayOffset = " (Вчера)";
                            break;
                        case (-2):
                        case (-3):
                        case (-4):
                        case (-22):
                        case (-23):
                        case (-24):
                            dayOffset = " (" + -dayShift + " дня назад)";
                            break;
                        default:
                            dayOffset = " (" + -dayShift + " дней назад)";
                            break;
                    }
                }
            } else {
                if (dayShift < 0) {
                    dayOffset = " (> месяца назад)";
                } else {
                    dayOffset = " (> чем через месяц)";
                }
            }
        } else {
            dayOffset = " (общее)";
        }
        return dayOffset;
    }
    
    public void changeParameter(String name, Integer value) {
        params.put(name, value);
    }
    
    public void setStateIconColor(TextView icon, String parameterKey) {
        boolean containedInGroupParams = group.params.containsKey(parameterKey);
        boolean containedInPersonalParams = params.containsKey(parameterKey);
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
        return Objects.hash(event, params.hashCode());
    }
    
    @Override
    public boolean equals(@Nullable Object obj) {
        if (obj == null) {
            return false;
        } else if (obj instanceof TodoListEntry) {
            return Objects.equals(event, ((TodoListEntry) obj).event) &&
                    Objects.equals(((TodoListEntry) obj).params, params);
        }
        return super.equals(obj);
    }
}
