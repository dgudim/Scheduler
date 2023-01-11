package prototype.xd.scheduler.utilities;

import static androidx.appcompat.app.AppCompatDelegate.MODE_NIGHT_FOLLOW_SYSTEM;
import static androidx.appcompat.app.AppCompatDelegate.MODE_NIGHT_NO;
import static androidx.appcompat.app.AppCompatDelegate.MODE_NIGHT_YES;
import static prototype.xd.scheduler.utilities.GraphicsUtilities.mixTwoColors;

import android.content.Context;
import android.content.SharedPreferences;
import android.graphics.Color;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.function.Function;

import prototype.xd.scheduler.entities.TodoEntry;
import prototype.xd.scheduler.utilities.Triplet.DefaultedValueTriplet;
import prototype.xd.scheduler.views.lockscreen.LockScreenTodoItemView.TodoItemViewType;

public class Keys {
    
    public static final String NAME = Keys.class.getSimpleName();
    
    private Keys() {
        throw new IllegalStateException(NAME + " can't be instantiated");
    }
    
    public abstract static class DefaultedValue<T> {
        
        protected Triplet.Type type = Triplet.Type.CURRENT;
        
        public final String key;
        public final T defaultValue;
        @Nullable
        private final SharedPreferences internalPrefs;
        
        DefaultedValue(String key, T defaultValue, @Nullable SharedPreferences preferences) {
            this.key = key;
            this.defaultValue = defaultValue;
            internalPrefs = preferences;
        }
        
        DefaultedValue(String key, T defaultValue, Triplet.Type type) {
            this.key = key;
            this.defaultValue = defaultValue;
            internalPrefs = null;
            this.type = type;
        }
        
        DefaultedValue(String key, T defaultValue) {
            this.key = key;
            this.defaultValue = defaultValue;
            internalPrefs = null;
        }
        
        protected SharedPreferences getInternalPrefs() {
            return internalPrefs == null ? Keys.preferences : internalPrefs;
        }
        
        protected abstract T getInternal(String actualKey, T actualDefaultValue);
        
        public Triplet.Type getType() {
            return type;
        }
        
        public T get(@Nullable List<String> subKeys, T actualDefaultValue, boolean ignoreBaseKey) {
            if (subKeys != null) {
                String targetKey = getFirstValidKey(subKeys, key);
                if (targetKey.equals(key) && ignoreBaseKey) {
                    return actualDefaultValue;
                }
                return getInternal(targetKey, actualDefaultValue);
            }
            return getInternal(key, actualDefaultValue);
        }
        
        public T get(@Nullable List<String> subKeys) {
            return get(subKeys, defaultValue, false);
        }
        
        // ignore the "base" key, only use sub-keys
        public T getOnlyBySubKeys(@NonNull List<String> subKeys, T defaultValueOverride) {
            return get(subKeys, defaultValueOverride, true);
        }
        
        public T get() {
            return get(null, defaultValue, false);
        }
        
        public abstract void put(T value);
        
        @Override
        public int hashCode() {
            return Objects.hash(key, defaultValue);
        }
        
        @Override
        public boolean equals(@Nullable Object obj) {
            if (obj == null) {
                return false;
            }
            if (obj == this) {
                return true;
            }
            if (!(obj instanceof DefaultedValue<?>))
                return false;
            DefaultedValue<?> val = (DefaultedValue<?>) obj;
            return Objects.equals(val.defaultValue, defaultValue) &&
                    val.key.equals(key) &&
                    val.type == type &&
                    Objects.equals(val.internalPrefs, internalPrefs);
        }
        
        @NonNull
        @Override
        public String toString() {
            return "Defaulted value: " + key + " (" + defaultValue + ")";
        }
    }
    
    public static class DefaultedBoolean extends DefaultedValue<Boolean> {
        DefaultedBoolean(String key, Boolean defaultValue, SharedPreferences preferences) {
            super(key, defaultValue, preferences);
        }
        
        DefaultedBoolean(String key, Boolean defaultValue) {
            super(key, defaultValue);
        }
        
        @Override
        protected Boolean getInternal(String actualKey, Boolean actualDefaultValue) {
            return getInternalPrefs().getBoolean(actualKey, actualDefaultValue);
        }
        
        @Override
        public void put(Boolean value) {
            getInternalPrefs().edit().putBoolean(key, value).apply();
        }
    }
    
    public static class DefaultedInteger extends DefaultedValue<Integer> {
        
        DefaultedInteger(String key, Integer defaultValue, Triplet.Type type) {
            super(key, defaultValue, type);
        }
        
        DefaultedInteger(String key, Integer defaultValue) {
            super(key, defaultValue);
        }
        
        @Override
        protected Integer getInternal(String actualKey, Integer actualDefaultValue) {
            return getInternalPrefs().getInt(actualKey, actualDefaultValue);
        }
        
        @Override
        public void put(Integer value) {
            getInternalPrefs().edit().putInt(key, value).apply();
        }
    }
    
    public static class DefaultedFloat extends DefaultedValue<Float> {
        DefaultedFloat(String key, Float defaultValue) {
            super(key, defaultValue);
        }
        
        @Override
        protected Float getInternal(String actualKey, Float actualDefaultValue) {
            return getInternalPrefs().getFloat(actualKey, actualDefaultValue);
        }
        
        @Override
        public void put(Float value) {
            getInternalPrefs().edit().putFloat(key, value).apply();
        }
    }
    
    public static class DefaultedString extends DefaultedValue<String> {
        DefaultedString(String key, String defaultValue) {
            super(key, defaultValue);
        }
        
        @Override
        protected String getInternal(String actualKey, String actualDefaultValue) {
            return getInternalPrefs().getString(actualKey, actualDefaultValue);
        }
        
        @Override
        public void put(String value) {
            getInternalPrefs().edit().putString(key, value).apply();
        }
    }
    
    public static class DefaultedEnumList<T extends Enum<T>> extends DefaultedValue<List<T>> {
        
        private final String delimiter;
        private final Class<T> enumClass;
        
        DefaultedEnumList(String key, List<T> defaultValue, String delimiter, Class<T> enumClass) {
            super(key, defaultValue);
            this.delimiter = delimiter;
            this.enumClass = enumClass;
        }
        
        @Override
        protected List<T> getInternal(String actualKey, List<T> actualDefaultValue) {
            String stringValue = getInternalPrefs().getString(actualKey, null);
            if (stringValue == null) {
                return actualDefaultValue;
            }
            String[] stringList = stringValue.split(delimiter);
            List<T> list = new ArrayList<>(stringList.length);
            for (String value : stringList) {
                list.add(T.valueOf(enumClass, value));
            }
            return list;
        }
        
        @Override
        public void put(List<T> enumList) {
            StringBuilder stringValue = new StringBuilder(enumList.get(0).name());
            for (int i = 1; i < enumList.size(); i++) {
                stringValue
                        .append(delimiter)
                        .append(enumList.get(i).name());
            }
            getInternalPrefs().edit().putString(key, stringValue.toString()).apply();
        }
        
        @Override
        public int hashCode() {
            return Objects.hash(key, defaultValue, enumClass);
        }
        
        @Override
        public boolean equals(@Nullable Object obj) {
            return super.equals(obj) && ((DefaultedEnumList<?>) obj).enumClass.equals(enumClass);
        }
    }
    
    public static class DefaultedEnum<T extends Enum<T>> extends DefaultedValue<T> {
        
        private final Class<T> enumClass;
        
        DefaultedEnum(String key, T defaultValue, Class<T> enumClass) {
            super(key, defaultValue);
            this.enumClass = enumClass;
        }
        
        @Override
        protected T getInternal(String actualKey, T actualDefaultValue) {
            String valName = getInternalPrefs().getString(actualKey, null);
            return valName == null ? actualDefaultValue : T.valueOf(enumClass, valName);
        }
        
        @Override
        public void put(T value) {
            getInternalPrefs().edit().putString(key, value.name()).apply();
        }
        
        @Override
        public int hashCode() {
            return Objects.hash(key, defaultValue, enumClass);
        }
        
        @Override
        public boolean equals(@Nullable Object obj) {
            return super.equals(obj) && ((DefaultedEnum<?>) obj).enumClass.equals(enumClass);
        }
    }
    
    public static void initPrefs(Context context) {
        if (preferences == null) {
            preferences = context.getSharedPreferences(PREFERENCES_MAIN, Context.MODE_PRIVATE);
            servicePreferences = context.getSharedPreferences(PREFERENCES_SERVICE, Context.MODE_PRIVATE);
        }
    }
    
    public static <T> void putAny(String key, T value) {
        if (value.getClass() == Integer.class) {
            preferences.edit().putInt(key, (Integer) value).apply();
        } else if (value.getClass() == String.class) {
            preferences.edit().putString(key, (String) value).apply();
        } else if (value.getClass() == Boolean.class) {
            preferences.edit().putBoolean(key, (Boolean) value).apply();
        } else if (value.getClass() == Long.class) {
            preferences.edit().putLong(key, (Long) value).apply();
        } else if (value.getClass() == Float.class) {
            preferences.edit().putFloat(key, (Float) value).apply();
        } else {
            Logger.error(NAME, "Can't put key: " + key + " with value " + value);
        }
    }
    
    public static boolean getBoolean(String key, boolean defaultValue) {
        return preferences.getBoolean(key, defaultValue);
    }
    
    public static Map<String, ?> getAll() {
        return preferences.getAll();
    }
    
    public static SharedPreferences.Editor edit() {
        return preferences.edit();
    }
    
    public static void clearAll() {
        preferences.edit().clear().apply();
    }
    
    public static int getFirstValidKeyIndex(List<String> calendarSubKeys, String parameter) {
        for (int i = calendarSubKeys.size() - 1; i >= 0; i--) {
            try {
                if (preferences.getString(calendarSubKeys.get(i) + "_" + parameter, null) != null) {
                    return i;
                }
            } catch (ClassCastException e) {
                return i;
            }
        }
        return -1;
    }
    
    public static String getFirstValidKey(List<String> calendarSubKeys, String parameter) {
        int index = getFirstValidKeyIndex(calendarSubKeys, parameter);
        return index == -1 ? parameter : calendarSubKeys.get(index) + "_" + parameter;
    }
    
    public static void setBitmapUpdateFlag() {
        SERVICE_UPDATE_SIGNAL.put(Boolean.TRUE);
        servicePreferences.edit().putBoolean(SERVICE_UPDATE_SIGNAL.key, true).apply();
    }
    
    public static void clearBitmapUpdateFlag() {
        SERVICE_UPDATE_SIGNAL.put(Boolean.FALSE);
    }
    
    private static volatile SharedPreferences preferences;
    private static volatile SharedPreferences servicePreferences;
    
    public static final float DEFAULT_TIME_OFFSET_COLOR_MIX_FACTOR = 0.75f;
    public static final float DEFAULT_CALENDAR_EVENT_BG_COLOR_MIX_FACTOR = 0.85f;
    public static final float DEFAULT_CALENDAR_EVENT_TIME_COLOR_MIX_FACTOR = 0.25f;
    public static final float DEFAULT_TITLE_FONT_SIZE_MULTIPLIER = 1.1F;
    
    public static final int DAY_FLAG_GLOBAL = -1;
    public static final String DAY_FLAG_GLOBAL_STR = "-1";
    
    public static final String VISIBLE = "visible";
    public static final boolean CALENDAR_SETTINGS_DEFAULT_VISIBLE = true;
    public static final String TEXT_VALUE = "value";
    public static final String IS_COMPLETED = "completed";
    public static final DefaultedBoolean CALENDAR_SHOW_ON_LOCK = new DefaultedBoolean("lock", true);
    public static final String START_DAY_UTC = "startDay";
    public static final String END_DAY_UTC = "endDay";
    public static final DefaultedInteger PRIORITY = new DefaultedInteger("priority", 0);
    
    public static final DefaultedValueTriplet<Integer, DefaultedInteger> BG_COLOR = new DefaultedValueTriplet<>(
            DefaultedInteger::new,
            "upcomingBgColor", 0xff_CCFFCC,
            "bgColor", 0xff_999999,
            "expiredBgColor", 0xff_FFCCCC);
    
    public static final Function<Integer, Integer> SETTINGS_DEFAULT_CALENDAR_EVENT_BG_COLOR = eventColor ->
            mixTwoColors(Color.WHITE, eventColor, Keys.DEFAULT_CALENDAR_EVENT_BG_COLOR_MIX_FACTOR);
    
    public static final DefaultedValueTriplet<Integer, DefaultedInteger> BORDER_COLOR = new DefaultedValueTriplet<>(
            DefaultedInteger::new,
            "upcomingBevelColor", 0xff_88FF88,
            "bevelColor", 0xff_777777,
            "expiredBevelColor", 0xff_FF8888);
    
    public static final DefaultedValueTriplet<Integer, DefaultedInteger> BORDER_THICKNESS = new DefaultedValueTriplet<>(
            DefaultedInteger::new,
            "upcomingBevelThickness", 3,
            "bevelThickness", 2,
            "expiredBevelThickness", 3);
    
    public static final DefaultedValueTriplet<Integer, DefaultedInteger> FONT_COLOR = new DefaultedValueTriplet<>(
            DefaultedInteger::new,
            "upcomingFontColor", 0xff_005500,
            "fontColor", 0xff_000000,
            "expiredFontColor", 0xff_990000);
    
    public static final DefaultedInteger FONT_SIZE = new DefaultedInteger("fontSize", 15);
    public static final DefaultedBoolean ADAPTIVE_BACKGROUND_ENABLED = new DefaultedBoolean("adaptive_background_enabled", false);
    public static final DefaultedInteger ADAPTIVE_COLOR_BALANCE = new DefaultedInteger("adaptive_color_balance", 3);
    
    public static final DefaultedInteger LOCKSCREEN_VIEW_VERTICAL_BIAS = new DefaultedInteger("lockscreen_view_vertical_bias", 50);
    
    public static final DefaultedBoolean HIDE_ENTRIES_BY_CONTENT = new DefaultedBoolean("hide_entries_by_content", false);
    public static final DefaultedString HIDE_ENTRIES_BY_CONTENT_CONTENT = new DefaultedString("hide_entries_by_content_content", "");
    
    public static final DefaultedInteger UPCOMING_ITEMS_OFFSET = new DefaultedInteger("dayOffset_upcoming", 0);
    public static final DefaultedInteger EXPIRED_ITEMS_OFFSET = new DefaultedInteger("dayOffset_expired", 0);
    public static final int SETTINGS_MAX_EXPIRED_UPCOMING_ITEMS_OFFSET = 14;
    
    public static final DefaultedBoolean SHOW_UPCOMING_EXPIRED_IN_LIST = new DefaultedBoolean("upcomingExpiredVisibleInList", true);
    public static final DefaultedBoolean HIDE_EXPIRED_ENTRIES_BY_TIME = new DefaultedBoolean("hide_entries_strict", false);
    public static final DefaultedBoolean ITEM_FULL_WIDTH_LOCK = new DefaultedBoolean("force_max_RWidth_lock", true);
    
    public static final DefaultedBoolean MERGE_ENTRIES = new DefaultedBoolean("merge_events", true);
    
    public static final DefaultedBoolean SHOW_GLOBAL_ITEMS_LOCK = new DefaultedBoolean("show_global_tasks_lock", true);
    public static final DefaultedBoolean SHOW_GLOBAL_ITEMS_LABEL_LOCK = new DefaultedBoolean("show_global_tasks_label_lock", true);
    
    public static final DefaultedBoolean ALLOW_GLOBAL_CALENDAR_ACCOUNT_SETTINGS = new DefaultedBoolean("allow_global_calendar_settings", false);
    
    public static final DefaultedEnum<TodoItemViewType> TODO_ITEM_VIEW_TYPE =
            new DefaultedEnum<>("lockScreenTodoItemViewType", TodoItemViewType.BASIC, TodoItemViewType.class);
    
    public static final DefaultedEnumList<TodoEntry.EntryType> TODO_ITEM_SORTING_ORDER = new DefaultedEnumList<>(
            "sort_order", Arrays.asList(
            TodoEntry.EntryType.GLOBAL,
            TodoEntry.EntryType.UPCOMING,
            TodoEntry.EntryType.TODAY,
            TodoEntry.EntryType.EXPIRED),
            "_", TodoEntry.EntryType.class);
    public static final DefaultedBoolean TREAT_GLOBAL_ITEMS_AS_TODAYS = new DefaultedBoolean("treat_global_as_todays", false);
    
    public static final int APP_THEME_LIGHT = MODE_NIGHT_NO;
    public static final int APP_THEME_DARK = MODE_NIGHT_YES;
    public static final int APP_THEME_SYSTEM = MODE_NIGHT_FOLLOW_SYSTEM;
    public static final int DEFAULT_APP_THEME = APP_THEME_SYSTEM;
    public static final List<Integer> appThemes = Collections.unmodifiableList(Arrays.asList(APP_THEME_DARK, APP_THEME_SYSTEM, APP_THEME_LIGHT));
    public static final DefaultedInteger APP_THEME = new DefaultedInteger("app_theme", DEFAULT_APP_THEME);
    
    public static final String PREFERENCES_MAIN = "prefs";
    public static final String PREFERENCES_SERVICE = "prefs_service";
    
    public static final DefaultedBoolean INTRO_SHOWN = new DefaultedBoolean("app_intro", false);
    
    public static final DefaultedBoolean SERVICE_UPDATE_SIGNAL = new DefaultedBoolean("update_lockscreen", false, servicePreferences);
    public static final String SERVICE_KEEP_ALIVE_SIGNAL = "keep_alive";
    public static final DefaultedBoolean SERVICE_FAILED = new DefaultedBoolean("service_failed", false);
    public static final DefaultedBoolean WALLPAPER_OBTAIN_FAILED = new DefaultedBoolean("wallpaper_obtain_failed", false);
    
    public static final DefaultedInteger DISPLAY_METRICS_HEIGHT = new DefaultedInteger("metrics_H", 100);
    public static final DefaultedInteger DISPLAY_METRICS_WIDTH = new DefaultedInteger("metrics_W", 100);
    public static final DefaultedFloat DISPLAY_METRICS_DENSITY = new DefaultedFloat("metrics_D", -1f);
    
    public static final DefaultedString ROOT_DIR = new DefaultedString("root_directory", "");
    public static final String ENTRIES_FILE = "entries";
    public static final String GROUPS_FILE = "groupData";
    
    public static final String GITHUB_ISSUES = "https://github.com/dgudim/Scheduler/issues";
    public static final String GITHUB_REPO = "https://github.com/dgudim/Scheduler";
    public static final String GITHUB_RELEASES = "https://github.com/dgudim/Scheduler/releases";
    
    public static final int TODO_LIST_INITIAL_CAPACITY = 75;
}
