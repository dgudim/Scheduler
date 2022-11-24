package prototype.xd.scheduler.utilities;

import static androidx.appcompat.app.AppCompatDelegate.MODE_NIGHT_FOLLOW_SYSTEM;
import static androidx.appcompat.app.AppCompatDelegate.MODE_NIGHT_NO;
import static androidx.appcompat.app.AppCompatDelegate.MODE_NIGHT_YES;

import java.util.Arrays;
import java.util.Collections;
import java.util.List;

import prototype.xd.scheduler.views.lockscreen.LockScreenTodoItemView;

public class Keys {
    
    private Keys() {
        throw new IllegalStateException("Utility key storage class");
    }
    
    public static final int SETTINGS_DEFAULT_BG_COLOR = 0xff_FFFFFF;
    public static final int SETTINGS_DEFAULT_UPCOMING_BG_COLOR = 0xff_CCFFCC;
    public static final int SETTINGS_DEFAULT_EXPIRED_BG_COLOR = 0xff_FFCCCC;
    
    public static final int SETTINGS_DEFAULT_BORDER_COLOR = 0xff_888888;
    public static final int SETTINGS_DEFAULT_UPCOMING_BORDER_COLOR = 0xff_88FF88;
    public static final int SETTINGS_DEFAULT_EXPIRED_BORDER_COLOR = 0xff_FF8888;
    
    public static final int SETTINGS_DEFAULT_BORDER_THICKNESS = 2;
    public static final int SETTINGS_DEFAULT_UPCOMING_BORDER_THICKNESS = 3;
    public static final int SETTINGS_DEFAULT_EXPIRED_BORDER_THICKNESS = 3;
    
    public static final int SETTINGS_DEFAULT_FONT_COLOR = 0xff_000000;
    public static final int SETTINGS_DEFAULT_UPCOMING_FONT_COLOR = 0xff_005500;
    public static final int SETTINGS_DEFAULT_EXPIRED_FONT_COLOR = 0xff_990000;
    
    public static final float DEFAULT_TITLE_FONT_SIZE_MULTIPLIER = 1.1F;
    public static final int SETTINGS_DEFAULT_FONT_SIZE = 15;
    public static final boolean SETTINGS_DEFAULT_ADAPTIVE_BACKGROUND_ENABLED = false;
    public static final int SETTINGS_DEFAULT_ADAPTIVE_COLOR_BALANCE = 3;
    
    public static final boolean SETTINGS_DEFAULT_ITEM_FULL_WIDTH_LOCK = true;
    public static final boolean SETTINGS_DEFAULT_HIDE_EXPIRED_ENTRIES_BY_TIME = true;
    
    public static final boolean SETTINGS_DEFAULT_SHOW_GLOBAL_ITEMS_LOCK = true;
    
    public static final int SETTINGS_DEFAULT_UPCOMING_ITEMS_OFFSET = 0;
    public static final int SETTINGS_DEFAULT_EXPIRED_ITEMS_OFFSET = 0;
    
    public static final boolean SETTINGS_DEFAULT_HIDE_ENTRIES_BY_CONTENT = false;
    
    public static final float DEFAULT_COLOR_MIX_FACTOR = 0.85f;
    
    public static final int DAY_FLAG_GLOBAL = -1;
    public static final String DAY_FLAG_GLOBAL_STR = "-1";
    
    public static final boolean CALENDAR_SETTINGS_DEFAULT_SHOW_ON_LOCK = true;
    public static final boolean CALENDAR_SETTINGS_DEFAULT_VISIBLE = true;
    public static final String VISIBLE = "visible";
    
    public static final String TEXT_VALUE = "value";
    public static final String IS_COMPLETED = "completed";
    public static final String SHOW_ON_LOCK = "lock";
    public static final String ASSOCIATED_DAY = "associatedDay";
    public static final int ENTRY_SETTINGS_DEFAULT_PRIORITY = 0;
    public static final String PRIORITY = "priority";
    
    public static final String BG_COLOR = "bgColor";
    public static final String UPCOMING_BG_COLOR = "upcomingBgColor";
    public static final String EXPIRED_BG_COLOR = "expiredBgColor";
    
    public static final String BORDER_COLOR = "bevelColor";
    public static final String UPCOMING_BORDER_COLOR = "upcomingBevelColor";
    public static final String EXPIRED_BORDER_COLOR = "expiredBevelColor";
    
    public static final String BORDER_THICKNESS = "bevelThickness";
    public static final String UPCOMING_BORDER_THICKNESS = "upcomingBevelThickness";
    public static final String EXPIRED_BORDER_THICKNESS = "expiredBevelThickness";
    
    public static final String FONT_COLOR = "fontColor";
    public static final String UPCOMING_FONT_COLOR = "upcomingFontColor";
    public static final String EXPIRED_FONT_COLOR = "expiredFontColor";
    
    public static final String FONT_SIZE = "fontSize";
    public static final String ADAPTIVE_BACKGROUND_ENABLED = "adaptive_background_enabled";
    public static final String ADAPTIVE_COLOR_BALANCE = "adaptive_color_balance";
    
    public static final String HIDE_ENTRIES_BY_CONTENT = "hide_entries_by_content";
    public static final String HIDE_ENTRIES_BY_CONTENT_CONTENT = "hide_entries_by_content_content";
    
    public static final String UPCOMING_ITEMS_OFFSET = "dayOffset_upcoming";
    public static final String EXPIRED_ITEMS_OFFSET = "dayOffset_expired";
    
    public static final String SHOW_GLOBAL_ITEMS_LOCK = "show_global_tasks_lock";
    
    public static final String ITEM_FULL_WIDTH_LOCK = "force_max_RWidth_lock";
    
    public static final String HIDE_EXPIRED_ENTRIES_BY_TIME = "hide_entries_strict";
    
    public static final String TODO_ITEM_VIEW_TYPE = "lockScreenTodoItemViewType";
    public static final String SETTINGS_DEFAULT_TODO_ITEM_VIEW_TYPE = LockScreenTodoItemView.TodoItemViewType.BASIC.name();
    
    public static final byte APP_THEME_LIGHT = MODE_NIGHT_NO;
    public static final byte APP_THEME_DARK = MODE_NIGHT_YES;
    public static final byte APP_THEME_SYSTEM = MODE_NIGHT_FOLLOW_SYSTEM;
    public static final byte DEFAULT_APP_THEME = APP_THEME_SYSTEM;
    public static final List<Byte> appThemes = Collections.unmodifiableList(Arrays.asList(APP_THEME_DARK, APP_THEME_SYSTEM, APP_THEME_LIGHT));
    public static final String APP_THEME = "app_theme";
    
    public static final String PREFERENCES = "prefs";
    public static final String PREFERENCES_SERVICE = "prefs_s";
    
    public static final String INTRO_SHOWN = "app_intro";
    
    public static final String LAST_UPDATE_TIME = "last_update_time";
    public static final String SERVICE_UPDATE_SIGNAL = "update";
    public static final String SERVICE_KEEP_ALIVE_SIGNAL = "keep_alive";
    
    public static final String DISPLAY_METRICS_HEIGHT = "metrics_H";
    public static final String DISPLAY_METRICS_WIDTH = "metrics_W";
    public static final String DISPLAY_METRICS_DENSITY = "metrics_D";
    
    public static final String ROOT_DIR = "root_directory";
    public static final String ENTRIES_FILE = "entries";
    public static final String GROUPS_FILE = "groupData";
    public static final String INDICATORS_FILE = "cachedIndicators";
}
