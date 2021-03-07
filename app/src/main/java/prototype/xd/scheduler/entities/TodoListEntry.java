package prototype.xd.scheduler.entities;

import android.graphics.Paint;
import android.util.TypedValue;

import androidx.core.math.MathUtils;

import static org.apache.commons.lang.ArrayUtils.addAll;
import static prototype.xd.scheduler.utilities.DateManager.currentDate;
import static prototype.xd.scheduler.utilities.DateManager.yesterdayDate;
import static prototype.xd.scheduler.utilities.LockScreenBitmapDrawer.currentBitmapLongestText;
import static prototype.xd.scheduler.utilities.LockScreenBitmapDrawer.displayMetrics_static;
import static prototype.xd.scheduler.utilities.LockScreenBitmapDrawer.displayWidth;
import static prototype.xd.scheduler.utilities.LockScreenBitmapDrawer.preferences_static;
import static prototype.xd.scheduler.utilities.Logger.WARNING;
import static prototype.xd.scheduler.utilities.Logger.log;
import static prototype.xd.scheduler.utilities.ScalingUtilities.createNewPaint;
import static prototype.xd.scheduler.utilities.Utilities.makeNewLines;

public class TodoListEntry {

    public String associatedDate;
    public boolean completed;
    public Group group;

    public int bgColor_lock;
    public int fontColor_list;
    public int fontColor_list_completed;

    int padColor;

    public int fontSize;
    public float h;
    public float kM;
    int maxChars;
    public float rWidth;

    public int padSize;

    public int fontColor;

    public boolean showOnLock;
    public boolean showOnLock_ifCompleted;
    public boolean showInList;
    public boolean showInList_ifCompleted;

    public String textValue;
    public String[] textValueSplit;

    public Paint textPaint;
    public Paint bgPaint;
    public Paint padPaint;
    private boolean resourcesValid;

    public boolean isTodayEntry;
    public boolean isYesterdayEntry;
    public boolean isGlobalEntry;

    public String[] params;

    public static final String TEXT_VALUE = "value";
    public static final String IS_COMPLETED = "completed";
    public static final String SHOW_ON_LOCK = "lock";
    public static final String FONT_SIZE = "fontSize";
    public static final String PAD_SIZE = "padSize";
    public static final String FONT_COLOR = "fontColor";
    public static final String BACKGROUND_COLOR = "bgColor";
    public static final String PAD_COLOR = "padColor";
    public static final String ASSOCIATED_DATE = "associatedDate";

    public TodoListEntry(String[] params, String groupName) {
        group = new Group(groupName);
        this.params = params;
        reloadParams();
    }

    public void changeGroup(String groupName) {
        group = new Group(groupName);
        reloadParams();
    }

    public void reloadParams() {
        for (int i = 0; i < params.length; i++) {
            if (params[i].equals("associatedDate")) {
                if (params[i + 1].equals(currentDate)) {

                    bgColor_lock = preferences_static.getInt("todayBgColor", 0xFFFFFFFF);
                    padColor = preferences_static.getInt("todayBevelColor", 0xFF888888);
                    padSize = preferences_static.getInt("defaultBevelThickness", 5);

                    fontColor_list = preferences_static.getInt("todayFontColor_list", 0xFF000000);
                    fontColor_list_completed = preferences_static.getInt("todayFontColor_list_completed", 0xFFCCCCCC);

                    showInList = true;
                    showInList_ifCompleted = true;
                    showOnLock = true;
                    showOnLock_ifCompleted = preferences_static.getBoolean("completedTasks", false);

                    isTodayEntry = true;

                } else if (params[i + 1].equals(yesterdayDate)) {

                    bgColor_lock = preferences_static.getInt("yesterdayBgColor", 0xFFFFCCCC);
                    padColor = preferences_static.getInt("yesterdayBevelColor", 0xFFFF8888);
                    padSize = preferences_static.getInt("yesterdayBevelThickness", 5);

                    fontColor_list = preferences_static.getInt("yesterdayFontColor_list", 0xFFCC0000);
                    fontColor_list_completed = preferences_static.getInt("yesterdayFontColor_list_completed", 0xFFFFCCCC);

                    showOnLock_ifCompleted = preferences_static.getBoolean("yesterdayItemsLock", false);
                    showInList_ifCompleted = preferences_static.getBoolean("yesterdayItemsList", false);
                    showInList = preferences_static.getBoolean("yesterdayTasks", true);
                    showOnLock = preferences_static.getBoolean("yesterdayTasksLock", true);

                    isYesterdayEntry = true;

                } else if (params[i + 1].equals("GLOBAL")) {

                    bgColor_lock = preferences_static.getInt("globalBgColor", 0xFFCCFFCC);
                    padColor = preferences_static.getInt("globalBevelColor", 0xFF88FF88);
                    padSize = preferences_static.getInt("globalBevelThickness", 5);

                    fontColor_list = preferences_static.getInt("globalFontColor_list", 0xFF00CC00);
                    fontColor_list_completed = fontColor_list;

                    showOnLock_ifCompleted = false;
                    showInList_ifCompleted = false;
                    showInList = true;
                    showOnLock = preferences_static.getBoolean("globalTasksLock", true);

                    isGlobalEntry = true;
                } else {
                    fontColor_list = 0xFF000000;
                    fontColor_list_completed = 0xFFCCCCCC;

                    showInList = true;
                    showInList_ifCompleted = true;
                    showOnLock = false;
                    showOnLock_ifCompleted = false;

                    isTodayEntry = true;
                }
            }
        }
        fontSize = preferences_static.getInt("fontSize", 21);
        fontColor = 0xFF000000;
        setParams((String[]) addAll(group.params, params));
        if (!resourcesValid) {
            initialiseDisplayData();
        }
    }

    private void initialiseDisplayData() {
        h = TypedValue.applyDimension(TypedValue.COMPLEX_UNIT_SP, fontSize, displayMetrics_static);
        textPaint = createNewPaint(fontColor);
        textPaint.setTextSize(h);
        textPaint.setTextAlign(Paint.Align.CENTER);
        kM = h * 1.1f;
        maxChars = (int) ((displayWidth - padSize * 2) / (textPaint.measureText("qwerty_") / 5f)) - 2;
        rWidth = MathUtils.clamp(textPaint.measureText(currentBitmapLongestText) / 2 + 10, 1, displayWidth / 2f - padSize);

        bgPaint = createNewPaint(bgColor_lock);
        padPaint = createNewPaint(padColor);

        splitText("");

        resourcesValid = true;
    }

    public void splitText(String addition) {
        textValueSplit = makeNewLines(textValue + addition, maxChars);
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
                case (FONT_SIZE):
                    fontSize = Integer.parseInt(params[i + 1]);
                    break;
                case (PAD_SIZE):
                    padSize = Integer.parseInt(params[i + 1]);
                    break;
                case (FONT_COLOR):
                    fontColor = Integer.parseInt(params[i + 1]);
                    break;
                case (BACKGROUND_COLOR):
                    bgColor_lock = Integer.parseInt(params[i + 1]);
                    break;
                case (PAD_COLOR):
                    padColor = Integer.parseInt(params[i + 1]);
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

    public void changeParameter(String name, String value, boolean addIfNotPresent, boolean reloadAfter, boolean invalidateResources) {
        boolean changed = false;
        for (int i = 0; i < params.length; i++) {
            if (params[i].equals(name)) {
                params[i + 1] = value;
                changed = true;
                break;
            }
        }
        if (!changed && addIfNotPresent) {
            String[] newParams = new String[params.length + 2];
            System.arraycopy(params, 0, newParams, 0, params.length);
            newParams[newParams.length - 1] = value;
            newParams[newParams.length - 2] = name;
            params = newParams;
        }
        resourcesValid = !invalidateResources;
        if (reloadAfter) {
            reloadParams();
        }
    }
}
