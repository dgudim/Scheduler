package prototype.xd.scheduler.entities;

import android.graphics.Paint;
import android.util.TypedValue;

import androidx.core.math.MathUtils;

import java.util.ArrayList;

import static org.apache.commons.lang.ArrayUtils.addAll;
import static prototype.xd.scheduler.utilities.DateManager.currentDate;
import static prototype.xd.scheduler.utilities.DateManager.yesterdayDate;
import static prototype.xd.scheduler.utilities.LockScreenBitmapDrawer.currentBitmapLongestText;
import static prototype.xd.scheduler.utilities.LockScreenBitmapDrawer.displayMetrics_static;
import static prototype.xd.scheduler.utilities.LockScreenBitmapDrawer.displayWidth;
import static prototype.xd.scheduler.utilities.LockScreenBitmapDrawer.preferences_static;
import static prototype.xd.scheduler.utilities.Logger.INFO;
import static prototype.xd.scheduler.utilities.Logger.WARNING;
import static prototype.xd.scheduler.utilities.Logger.log;
import static prototype.xd.scheduler.utilities.ScalingUtilities.createNewPaint;
import static prototype.xd.scheduler.utilities.Utilities.makeNewLines;

public class TodoListEntry {

    public String associatedDate;
    public boolean completed;
    public Group group;

    public int bgColor_lock;
    public int bgColor_list;
    public int fontColor_list;
    public int fontColor_list_completed;

    public int padColor;

    public int fontSize = 0;
    public float h = 0;
    public float kM = 0;
    public int maxChars = 0;
    public float rWidth = 0;

    public int bevelSize = 0;

    public int priority = 0;

    public int fontColor_lock;

    public boolean showOnLock;
    public boolean showOnLock_ifCompleted;
    public boolean showInList;
    public boolean showInList_ifCompleted;

    public String textValue = "_BLANK_";
    public String[] textValueSplit;

    public Paint textPaint;
    public Paint bgPaint;
    public Paint padPaint;

    public boolean isTodayEntry = false;
    public boolean isYesterdayEntry = false;
    public boolean isGlobalEntry = false;

    public String[] params;

    public static final String TEXT_VALUE = "value";
    public static final String IS_COMPLETED = "completed";
    public static final String SHOW_ON_LOCK = "lock";
    public static final String SHOW_ON_LOCK_COMPLETED = "lock_completed";
    public static final String FONT_SIZE = "fontSize";
    public static final String BEVEL_SIZE = "padSize";
    public static final String FONT_COLOR_LOCK = "fontColor_lock";
    public static final String FONT_COLOR_LIST = "fontColor_list";
    public static final String BACKGROUND_COLOR_LOCK = "bgColor_lock";
    public static final String BACKGROUND_COLOR_LIST = "bgColor_list";
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
        changeGroup("Ничего");
    }

    public void changeGroup(Group group) {
        this.group = group;
        reloadParams();
    }

    public boolean getLockViewState() {
        return (showOnLock && !completed) || showOnLock_ifCompleted;
    }

    public String[] getDisplayParams() {
        ArrayList<String> displayParams = new ArrayList<>();
        for (int i = 0; i < params.length; i += 2) {

            if (params[i].equals(FONT_SIZE)
                    || params[i].equals(SHOW_ON_LOCK)
                    || params[i].equals(BEVEL_SIZE)
                    || params[i].equals(FONT_COLOR_LOCK)
                    || params[i].equals(FONT_COLOR_LIST)
                    || params[i].equals(BACKGROUND_COLOR_LOCK)
                    || params[i].equals(BACKGROUND_COLOR_LIST)
                    || params[i].equals(BEVEL_COLOR)
                    || params[i].equals(PRIORITY)) {
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
            if (params[i].equals("associatedDate")) {
                if (params[i + 1].equals(currentDate)) {

                    bgColor_lock = preferences_static.getInt("todayBgColor", 0xFFFFFFFF);
                    padColor = preferences_static.getInt("todayBevelColor", 0xFF888888);
                    bevelSize = preferences_static.getInt("defaultBevelThickness", 5);

                    fontColor_list = preferences_static.getInt("todayFontColor_list", 0xFF000000);
                    fontColor_list_completed = preferences_static.getInt("todayFontColor_list_completed", 0xFFCCCCCC);

                    showInList = true;
                    showInList_ifCompleted = true;
                    showOnLock = true;
                    showOnLock_ifCompleted = preferences_static.getBoolean("completedTasks", false);

                    isTodayEntry = true;
                    isYesterdayEntry = false;
                    isGlobalEntry = false;

                } else if (params[i + 1].equals(yesterdayDate)) {

                    bgColor_lock = preferences_static.getInt("yesterdayBgColor", 0xFFFFCCCC);
                    padColor = preferences_static.getInt("yesterdayBevelColor", 0xFFFF8888);
                    bevelSize = preferences_static.getInt("yesterdayBevelThickness", 5);

                    fontColor_list = preferences_static.getInt("yesterdayFontColor_list", 0xFFCC0000);
                    fontColor_list_completed = preferences_static.getInt("yesterdayFontColor_list_completed", 0xFFFFCCCC);

                    showOnLock_ifCompleted = preferences_static.getBoolean("yesterdayItemsLock", false);
                    showInList_ifCompleted = preferences_static.getBoolean("yesterdayItemsList", false);
                    showInList = preferences_static.getBoolean("yesterdayTasks", true);
                    showOnLock = preferences_static.getBoolean("yesterdayTasksLock", true);

                    isYesterdayEntry = true;
                    isGlobalEntry = false;
                    isTodayEntry = false;

                } else if (params[i + 1].equals("GLOBAL")) {

                    bgColor_lock = preferences_static.getInt("globalBgColor", 0xFFCCFFCC);
                    padColor = preferences_static.getInt("globalBevelColor", 0xFF88FF88);
                    bevelSize = preferences_static.getInt("globalBevelThickness", 5);

                    fontColor_list = preferences_static.getInt("globalFontColor_list", 0xFF00CC00);
                    fontColor_list_completed = fontColor_list;

                    showOnLock_ifCompleted = true;
                    showInList_ifCompleted = true;
                    showInList = true;
                    showOnLock = preferences_static.getBoolean("globalTasksLock", true);

                    isGlobalEntry = true;
                    isYesterdayEntry = false;
                    isTodayEntry = false;
                } else {
                    fontColor_list = 0xFF000000;
                    fontColor_list_completed = 0xFFCCCCCC;
                    bgColor_lock = 0xFFFFFFFF;
                    padColor = 0xFF888888;
                    bevelSize = 5;

                    showInList = true;
                    showInList_ifCompleted = true;
                    showOnLock = false;
                    showOnLock_ifCompleted = false;

                    isYesterdayEntry = false;
                    isGlobalEntry = false;
                    isTodayEntry = false;
                }
            }
        }
        fontSize = preferences_static.getInt("fontSize", 21);
        fontColor_lock = 0xFF000000;
        bgColor_list = 0xFFFFFFFF;
        setParams((String[]) addAll(group.params, params));
    }

    public void initialiseDisplayData() {
        h = TypedValue.applyDimension(TypedValue.COMPLEX_UNIT_SP, fontSize, displayMetrics_static);
        textPaint = createNewPaint(fontColor_lock);
        textPaint.setTextSize(h);
        textPaint.setTextAlign(Paint.Align.CENTER);
        kM = h * 1.1f;
        maxChars = (int) ((displayWidth - bevelSize * 2) / (textPaint.measureText("qwerty_") / 5f)) - 2;
        rWidth = MathUtils.clamp(textPaint.measureText(currentBitmapLongestText) / 2 + 10, 1, displayWidth / 2f - bevelSize);

        bgPaint = createNewPaint(bgColor_lock);
        padPaint = createNewPaint(padColor);

        log(INFO, "loaded display data for " + textValue);
    }

    public void splitText() {
        textValueSplit = makeNewLines(textValue, maxChars);
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
                case (SHOW_ON_LOCK_COMPLETED):
                    showOnLock_ifCompleted = Boolean.parseBoolean(params[i + 1]);
                    break;
                case (BEVEL_SIZE):
                    bevelSize = Integer.parseInt(params[i + 1]);
                    break;
                case (FONT_COLOR_LOCK):
                    fontColor_lock = Integer.parseInt(params[i + 1]);
                    break;
                case (FONT_COLOR_LIST):
                    fontColor_list = Integer.parseInt(params[i + 1]);
                    break;
                case (BACKGROUND_COLOR_LOCK):
                    bgColor_lock = Integer.parseInt(params[i + 1]);
                    break;
                case (BACKGROUND_COLOR_LIST):
                    bgColor_list = Integer.parseInt(params[i + 1]);
                    break;
                case (BEVEL_COLOR):
                    padColor = Integer.parseInt(params[i + 1]);
                    break;
                case (PRIORITY):
                    priority = Integer.parseInt(params[i + 1]);
                    break;
                case (ASSOCIATED_DATE):
                    associatedDate = params[i + 1];
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
}
