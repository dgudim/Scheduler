package prototype.xd.scheduler.entities;

import static org.apache.commons.lang.ArrayUtils.addAll;
import static prototype.xd.scheduler.MainActivity.displayMetrics;
import static prototype.xd.scheduler.MainActivity.preferences;
import static prototype.xd.scheduler.entities.Group.BLANK_NAME;
import static prototype.xd.scheduler.utilities.BitmapUtilities.createNewPaint;
import static prototype.xd.scheduler.utilities.BitmapUtilities.mixTwoColors;
import static prototype.xd.scheduler.utilities.DateManager.currentDate;
import static prototype.xd.scheduler.utilities.DateManager.daysFromDate;
import static prototype.xd.scheduler.utilities.LockScreenBitmapDrawer.currentBitmapLongestText;
import static prototype.xd.scheduler.utilities.LockScreenBitmapDrawer.displayWidth;
import static prototype.xd.scheduler.utilities.Logger.ContentType.INFO;
import static prototype.xd.scheduler.utilities.Logger.ContentType.WARNING;
import static prototype.xd.scheduler.utilities.Logger.log;
import static prototype.xd.scheduler.utilities.Utilities.makeNewLines;

import android.graphics.Color;
import android.graphics.Paint;
import android.util.TypedValue;
import android.widget.TextView;

import androidx.core.math.MathUtils;

import java.util.ArrayList;
import java.util.Calendar;

import prototype.xd.scheduler.utilities.Keys;

public class TodoListEntry {
    
    public String associatedDate;
    public int dayOffset_left = 0;
    public int dayOffset_right = 0;
    public boolean completed;
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
    
    public boolean showOnLock;
    public boolean showInList;
    public boolean showInList_ifCompleted;
    
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
    
    public static final String DATE_FLAG_GLOBAL = "GLOBAL";
    
    public static final String TEXT_VALUE = "value";
    public static final String IS_COMPLETED = "completed";
    public static final String SHOW_ON_LOCK = "lock";
    public static final String SHOW_DAYS_BEFOREHAND = "show_beforehand";
    public static final String SHOW_DAYS_AFTER = "show_after";
    public static final String BEVEL_SIZE = "padSize";
    public static final String FONT_COLOR = "fontColor";
    public static final String BACKGROUND_COLOR = "bgColor";
    public static final String ADAPTIVE_COLOR = "adaptiveColor";
    public static final String ADAPTIVE_COLOR_BALANCE = "adaptiveColorBalance";
    public static final String BEVEL_COLOR = "padColor";
    public static final String ASSOCIATED_DATE = "associatedDate";
    public static final String PRIORITY = "priority";
    
    public TodoListEntry() {
    
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
        changeGroup(BLANK_NAME);
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
                    || params[i].equals(ASSOCIATED_DATE)
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
                    || params[i].equals(ASSOCIATED_DATE)
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
        for (int i = 0; i < params.length; i++) {
            if (params[i].equals(ASSOCIATED_DATE)) {
                
                dayOffset_left = preferences.getInt(Keys.OLD_ITEMS_OFFSET, Keys.SETTINGS_DEFAULT_OLD_ITEMS_OFFSET);
                dayOffset_right = preferences.getInt(Keys.NEW_ITEMS_OFFSET, Keys.SETTINGS_DEFAULT_NEW_ITEMS_OFFSET);
                
                long days_associated = daysFromDate(params[i + 1]);
                long days_current = daysFromDate(currentDate);
                if (params[i + 1].equals(currentDate) || params[i + 1].equals(DATE_FLAG_GLOBAL)) {
                    
                    bgColor = preferences.getInt(Keys.TODAY_BG_COLOR, Keys.SETTINGS_DEFAULT_TODAY_BG_COLOR);
                    bevelColor = preferences.getInt(Keys.TODAY_BEVEL_COLOR, Keys.SETTINGS_DEFAULT_TODAY_BEVEL_COLOR);
                    bevelThickness = preferences.getInt(Keys.TODAY_BEVEL_THICKNESS, Keys.SETTINGS_DEFAULT_TODAY_BEVEL_THICKNESS);
                    
                    setFontColor(Keys.TODAY_FONT_COLOR, Keys.SETTINGS_DEFAULT_TODAY_FONT_COLOR);
                    
                    showInList = true;
                    showInList_ifCompleted = true;
                    showOnLock = true;
                    
                    if (params[i + 1].equals(DATE_FLAG_GLOBAL)) {
                        showOnLock = preferences.getBoolean(Keys.SHOW_GLOBAL_ITEMS_LOCK, Keys.SETTINGS_DEFAULT_SHOW_GLOBAL_ITEMS_LOCK);
                        isGlobalEntry = true;
                    } else {
                        isTodayEntry = true;
                    }
                    
                } else if (days_associated < days_current && days_current - days_associated <= dayOffset_left) {
                    
                    bgColor = preferences.getInt(Keys.OLD_BG_COLOR, Keys.SETTINGS_DEFAULT_OLD_BG_COLOR);
                    bevelColor = preferences.getInt(Keys.OLD_BEVEL_COLOR, Keys.SETTINGS_DEFAULT_OLD_BEVEL_COLOR);
                    bevelThickness = preferences.getInt(Keys.OLD_BEVEL_THICKNESS, Keys.SETTINGS_DEFAULT_OLD_BEVEL_THICKNESS);
                    
                    setFontColor(Keys.OLD_FONT_COLOR, Keys.SETTINGS_DEFAULT_OLD_FONT_COLOR);
                    
                    showInList_ifCompleted = preferences.getBoolean(Keys.SHOW_OLD_COMPLETED_ITEMS_IN_LIST, Keys.SETTINGS_DEFAULT_SHOW_OLD_COMPLETED_ITEMS_IN_LIST);
                    showInList = true;
                    showOnLock = true;
                    
                    isOldEntry = true;
                } else if (days_associated > days_current && days_associated - days_current <= dayOffset_right) {
                    
                    bgColor = preferences.getInt(Keys.NEW_BG_COLOR, Keys.SETTINGS_DEFAULT_NEW_BG_COLOR);
                    bevelColor = preferences.getInt(Keys.NEW_BEVEL_COLOR, Keys.SETTINGS_DEFAULT_NEW_BEVEL_COLOR);
                    bevelThickness = preferences.getInt(Keys.NEW_BEVEL_THICKNESS, Keys.SETTINGS_DEFAULT_NEW_BEVEL_THICKNESS);
                    
                    setFontColor(Keys.NEW_FONT_COLOR, Keys.SETTINGS_DEFAULT_NEW_FONT_COLOR);
                    
                    showInList_ifCompleted = preferences.getBoolean(Keys.SHOW_NEW_COMPLETED_ITEMS_IN_LIST, Keys.SETTINGS_DEFAULT_SHOW_NEW_COMPLETED_ITEMS_IN_LIST);
                    showInList = true;
                    showOnLock = true;
                    
                    isNewEntry = true;
                } else {
                    fontColor = Keys.SETTINGS_DEFAULT_TODAY_FONT_COLOR;
                    fontColor_completed = mixTwoColors(fontColor, 0xff_FFFFFF, 0.5);
                    bgColor = Keys.SETTINGS_DEFAULT_TODAY_BG_COLOR;
                    bevelColor = Keys.SETTINGS_DEFAULT_TODAY_BEVEL_COLOR;
                    bevelThickness = Keys.SETTINGS_DEFAULT_TODAY_BEVEL_THICKNESS;
                    
                    showInList = true;
                    showInList_ifCompleted = true;
                    showOnLock = false;
                }
            }
        }
        
        fontSize = preferences.getInt(Keys.FONT_SIZE, Keys.SETTINGS_DEFAULT_FONT_SIZE);
        adaptiveColorEnabled = preferences.getBoolean(Keys.ADAPTIVE_COLOR_ENABLED, Keys.SETTINGS_DEFAULT_ADAPTIVE_COLOR_ENABLED);
        adaptiveColorBalance = preferences.getInt(Keys.ADAPTIVE_COLOR_BALANCE, Keys.SETTINGS_DEFAULT_ADAPTIVE_COLOR_BALANCE);
        adaptiveColor = 0xff_FFFFFF;
        priority = 0;
        setParams((String[]) addAll(group.params, params));
    }
    
    private void setFontColor(String key, int defaultColor) {
        fontColor = preferences.getInt(key, defaultColor);
        fontColor_completed = mixTwoColors(fontColor, 0xff_FFFFFF, 0.5);
    }
    
    public void initialiseDisplayData() {
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
        if (!associatedDate.equals("GLOBAL")) {
            dayOffset = "";

            String[] dateParts = associatedDate.split("_");
            int year = Integer.parseInt(dateParts[0]);
            int month = Integer.parseInt(dateParts[1]);
            int day = Integer.parseInt(dateParts[2]);
            
            String[] dateParts_current = currentDate.split("_");
            int year_current = Integer.parseInt(dateParts_current[0]);
            int month_current = Integer.parseInt(dateParts_current[1]);
            int day_current = Integer.parseInt(dateParts_current[2]);
            
            int dayShift;
            
            if (month == month_current) {
                dayShift = day - day_current;
            } else {
                long now = System.currentTimeMillis();
                
                Calendar calendar = Calendar.getInstance();
                calendar.setTimeInMillis(now);
                calendar.add(Calendar.MONTH, month - month_current);
                calendar.add(Calendar.DATE, day - day_current);
                long previous = calendar.getTimeInMillis();
                dayShift = (int) ((previous - now) / (1000 * 3600 * 24));
            }
            
            if (year == year_current) {
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
                        dayOffset += " (> месяца назад)(" + associatedDate.replace("_", "/") + ")";
                    } else {
                        dayOffset += " (> чем через месяц)(" + associatedDate.replace("_", "/") + ")";
                    }
                }
            } else {
                if (year > year_current) {
                    dayOffset += " (В следующем году)(" + associatedDate.replace("_", "/") + ")";
                } else {
                    dayOffset += " (В прошлом году)(" + associatedDate.replace("_", "/") + ")";
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
                case (SHOW_DAYS_BEFOREHAND):
                    dayOffset_right = Integer.parseInt(params[i + 1]);
                    break;
                case (SHOW_DAYS_AFTER):
                    dayOffset_left = Integer.parseInt(params[i + 1]);
                    break;
                case (BEVEL_SIZE):
                    bevelThickness = Integer.parseInt(params[i + 1]);
                    break;
                case (FONT_COLOR):
                    fontColor = Integer.parseInt(params[i + 1]);
                    break;
                case (BACKGROUND_COLOR):
                    bgColor = Integer.parseInt(params[i + 1]);
                    break;
                case (BEVEL_COLOR):
                    bevelColor = Integer.parseInt(params[i + 1]);
                    break;
                case (PRIORITY):
                    priority = Integer.parseInt(params[i + 1]);
                    break;
                case (ASSOCIATED_DATE):
                    associatedDate = params[i + 1];
                    break;
                case (ADAPTIVE_COLOR):
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
