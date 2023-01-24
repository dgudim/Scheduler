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
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.function.IntUnaryOperator;

import kotlin.jvm.functions.Function2;
import prototype.xd.scheduler.BuildConfig;
import prototype.xd.scheduler.entities.TodoEntry;
import prototype.xd.scheduler.utilities.Triplet.DefaultedValueTriplet;
import prototype.xd.scheduler.views.lockscreen.LockScreenTodoItemView.TodoItemViewType;

public final class Keys {
    
    public static final String NAME = Keys.class.getSimpleName();
    
    private Keys() throws InstantiationException {
        throw new InstantiationException(NAME);
    }
    
    public abstract static class DefaultedValue<T> {
        
        @NonNull
        protected final Triplet.Type type;
        
        @NonNull
        public final String key;
        @NonNull
        public final T defaultValue;
        
        DefaultedValue(@NonNull String key, @NonNull T defaultValue, @NonNull Triplet.Type type) {
            this.key = key;
            this.defaultValue = defaultValue;
            this.type = type;
        }
        
        DefaultedValue(@NonNull String key, @NonNull T defaultValue) {
            this(key, defaultValue, Triplet.Type.CURRENT);
        }
        
        protected abstract T getInternal(@NonNull String actualKey, @NonNull T actualDefaultValue);
        
        @NonNull
        public Triplet.Type getType() {
            return type;
        }
        
        @NonNull
        public T get(@Nullable List<String> subKeys, @NonNull T actualDefaultValue, boolean ignoreBaseKey) {
            return getFirstValidKey(subKeys, key, (validKey, index) -> {
                if (index == -1 && ignoreBaseKey) {
                    return actualDefaultValue;
                }
                return getInternal(validKey, actualDefaultValue);
            });
        }
        
        @NonNull
        public T get(@Nullable List<String> subKeys) {
            return get(subKeys, defaultValue, false);
        }
        
        // ignore the "base" key, only use sub-keys
        @NonNull
        public T getOnlyBySubKeys(@NonNull List<String> subKeys, @NonNull T defaultValueOverride) {
            return get(subKeys, defaultValueOverride, true);
        }
        
        @NonNull
        public T get() {
            return get(null, defaultValue, false);
        }
        
        public abstract void put(@NonNull T value);
        
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
            if (obj instanceof DefaultedValue<?>) {
                DefaultedValue<?> val = (DefaultedValue<?>) obj;
                return Objects.equals(val.defaultValue, defaultValue) &&
                        val.key.equals(key) &&
                        val.type == type;
            }
            return false;
        }
        
        @NonNull
        @Override
        public String toString() {
            return "Defaulted value: " + key + " (" + defaultValue + ")";
        }
    }
    
    public static class DefaultedBoolean extends DefaultedValue<Boolean> {
        
        DefaultedBoolean(String key, Boolean defaultValue) {
            super(key, defaultValue);
        }
        
        @NonNull
        @Override
        protected Boolean getInternal(@NonNull String actualKey, @NonNull Boolean actualDefaultValue) {
            return preferences.getBoolean(actualKey, actualDefaultValue);
        }
        
        @Override
        public void put(@NonNull Boolean value) {
            preferences.edit().putBoolean(key, value).apply();
        }
    }
    
    public static class DefaultedInteger extends DefaultedValue<Integer> {
        
        DefaultedInteger(String key, Integer defaultValue, Triplet.Type type) {
            super(key, defaultValue, type);
        }
        
        DefaultedInteger(String key, Integer defaultValue) {
            super(key, defaultValue);
        }
        
        @NonNull
        @Override
        protected Integer getInternal(@NonNull String actualKey, @NonNull Integer actualDefaultValue) {
            return preferences.getInt(actualKey, actualDefaultValue);
        }
        
        @Override
        public void put(@NonNull Integer value) {
            preferences.edit().putInt(key, value).apply();
        }
    }
    
    public static class DefaultedFloat extends DefaultedValue<Float> {
        DefaultedFloat(String key, Float defaultValue) {
            super(key, defaultValue);
        }
        
        @NonNull
        @Override
        protected Float getInternal(@NonNull String actualKey, @NonNull Float actualDefaultValue) {
            return preferences.getFloat(actualKey, actualDefaultValue);
        }
        
        @Override
        public void put(@NonNull Float value) {
            preferences.edit().putFloat(key, value).apply();
        }
    }
    
    public static class DefaultedString extends DefaultedValue<String> {
        DefaultedString(String key, String defaultValue) {
            super(key, defaultValue);
        }
        
        @Override
        @Nullable
        protected String getInternal(@NonNull String actualKey, @NonNull String actualDefaultValue) {
            return preferences.getString(actualKey, actualDefaultValue);
        }
        
        @Override
        public void put(@NonNull String value) {
            preferences.edit().putString(key, value).apply();
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
        @NonNull
        protected List<T> getInternal(@NonNull String actualKey, @NonNull List<T> actualDefaultValue) {
            String stringValue = preferences.getString(actualKey, null);
            if (stringValue == null) {
                return actualDefaultValue;
            }
            String[] stringList = stringValue.split(delimiter);
            List<T> list = new ArrayList<>(stringList.length);
            for (String value : stringList) {
                list.add(Enum.valueOf(enumClass, value));
            }
            return list;
        }
        
        @Override
        public void put(@NonNull List<T> enumList) {
            StringBuilder stringValue = new StringBuilder(enumList.get(0).name());
            for (int i = 1; i < enumList.size(); i++) {
                stringValue
                        .append(delimiter)
                        .append(enumList.get(i).name());
            }
            preferences.edit().putString(key, stringValue.toString()).apply();
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
        protected T getInternal(@NonNull String actualKey, @NonNull T actualDefaultValue) {
            String valName = preferences.getString(actualKey, null);
            return valName == null ? actualDefaultValue : Enum.valueOf(enumClass, valName);
        }
        
        @Override
        public void put(@NonNull T value) {
            preferences.edit().putString(key, value.name()).apply();
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
    
    public static synchronized void init(@NonNull Context context) {
        if (preferences == null) {
            preferences = context.getSharedPreferences(PREFERENCES_MAIN, Context.MODE_PRIVATE);
            servicePreferences = context.getSharedPreferences(PREFERENCES_SERVICE, Context.MODE_PRIVATE);
        }
    }
    
    public static <T> void putAny(@NonNull String key, @NonNull T value) {
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
    
    public static boolean getBoolean(@NonNull String key, boolean defaultValue) {
        return preferences.getBoolean(key, defaultValue);
    }
    
    @NonNull
    public static Map<String, ?> getAll() {
        return preferences.getAll();
    }
    
    @NonNull
    public static SharedPreferences.Editor edit() {
        return preferences.edit();
    }
    
    public static void clearAll() {
        preferences.edit().clear().apply();
    }
    
    public static <T> T getFirstValidKey(@Nullable List<String> subKeys, @NonNull String parameter, @NonNull Function2<String, Integer, T> converter) {
        if (subKeys != null) {
            for (int i = subKeys.size() - 1; i >= 0; i--) {
                String key = subKeys.get(i) + KEY_SEPARATOR + parameter;
                if (preferences.contains(key)) {
                    return converter.invoke(key, i);
                }
            }
        }
        return converter.invoke(parameter, -1);
    }
    
    public static boolean getBitmapUpdateFlag() {
        return servicePreferences.getBoolean(SERVICE_UPDATE_SIGNAL.key, SERVICE_UPDATE_SIGNAL.defaultValue);
    }
    
    public static void setBitmapUpdateFlag() {
        servicePreferences.edit().putBoolean(SERVICE_UPDATE_SIGNAL.key, true).apply();
    }
    
    public static void clearBitmapUpdateFlag() {
        servicePreferences.edit().putBoolean(SERVICE_UPDATE_SIGNAL.key, false).apply();
    }
    
    private static volatile SharedPreferences preferences; // NOSONAR, SharedPreferences are thread safe
    private static volatile SharedPreferences servicePreferences; // NOSONAR
    
    public static final float DEFAULT_DIM_FACTOR = 0.5F;
    public static final float CALENDAR_SETTINGS_DIM_FACTOR = 0.8F;
    public static final float DEFAULT_TIME_OFFSET_COLOR_MIX_FACTOR = 0.75F;
    public static final float DEFAULT_CALENDAR_EVENT_BG_COLOR_MIX_FACTOR = 0.85F;
    public static final float DEFAULT_SECONDARY_TEXT_COLOR_MIX_FACTOR = 0.25F;
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
    
    public static final IntUnaryOperator SETTINGS_DEFAULT_CALENDAR_EVENT_BG_COLOR = eventColor ->
            mixTwoColors(Color.WHITE, eventColor, DEFAULT_CALENDAR_EVENT_BG_COLOR_MIX_FACTOR);
    
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
    public static final List<Integer> APP_THEMES = List.of(APP_THEME_DARK, APP_THEME_SYSTEM, APP_THEME_LIGHT);
    public static final DefaultedInteger APP_THEME = new DefaultedInteger("app_theme", DEFAULT_APP_THEME);
    
    public static final String PREFERENCES_MAIN = "prefs";
    public static final String PREFERENCES_SERVICE = "prefs_service";
    
    public static final DefaultedBoolean INTRO_SHOWN = new DefaultedBoolean("app_intro", false);
    
    private static final DefaultedBoolean SERVICE_UPDATE_SIGNAL = new DefaultedBoolean("update_lockscreen", false);
    public static final DefaultedBoolean DEBUG_LOGGING = new DefaultedBoolean("debug_logging", false);
    public static final String SERVICE_KEEP_ALIVE_SIGNAL = "keep_alive";
    public static final DefaultedBoolean SERVICE_FAILED = new DefaultedBoolean("service_failed", false);
    public static final DefaultedBoolean WALLPAPER_OBTAIN_FAILED = new DefaultedBoolean("wallpaper_obtain_failed", false);
    
    public static final DefaultedInteger DISPLAY_METRICS_HEIGHT = new DefaultedInteger("metrics_H", 100);
    public static final DefaultedInteger DISPLAY_METRICS_WIDTH = new DefaultedInteger("metrics_W", 100);
    public static final DefaultedFloat DISPLAY_METRICS_DENSITY = new DefaultedFloat("metrics_D", -1F);
    
    public static final MutableObject<String> ROOT_DIR = new MutableObject<>();
    public static final String ENTRIES_FILE = "entries";
    public static final String ENTRIES_FILE_BACKUP = ENTRIES_FILE + ".old";
    public static final String GROUPS_FILE = "groupData";
    public static final String GROUPS_FILE_BACKUP = GROUPS_FILE + ".old";
    
    public static final String LOG_FILE = "log.txt";
    public static final String LOGCAT_FILE = "logcat.txt";
    public static final String LOG_FILE_OLD = LOG_FILE + ".old";
    
    public static final String PACKAGE_NAME = BuildConfig.APPLICATION_ID;
    public static final String PACKAGE_PROVIDER_NAME = PACKAGE_NAME + ".provider";
    
    public static final String GITHUB_ISSUES = "https://github.com/dgudim/Scheduler/issues";
    public static final String GITHUB_REPO = "https://github.com/dgudim/Scheduler";
    public static final String GITHUB_RELEASES = "https://github.com/dgudim/Scheduler/releases";
    
    public static final int TODO_LIST_INITIAL_CAPACITY = 75;
    
    public static final String TIME_RANGE_SEPARATOR = " - ";
    public static final String KEY_SEPARATOR = "_";
}
