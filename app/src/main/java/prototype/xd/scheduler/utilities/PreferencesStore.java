package prototype.xd.scheduler.utilities;

import static android.util.Log.ERROR;
import static prototype.xd.scheduler.utilities.Keys.PREFERENCES;
import static prototype.xd.scheduler.utilities.Keys.PREFERENCES_SERVICE;
import static prototype.xd.scheduler.utilities.Logger.log;

import android.content.Context;
import android.content.SharedPreferences;

public class PreferencesStore {
    
    private PreferencesStore() {
        throw new IllegalStateException("Preferences storage class");
    }
    
    public volatile static SharedPreferences preferences;
    public volatile static SharedPreferences servicePreferences;
    
    public static void init(Context context) {
        if (preferences == null) {
            preferences = context.getSharedPreferences(PREFERENCES, Context.MODE_PRIVATE);
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
            log(ERROR, "PreferencesStore", "Can't put key: " + key + " with value " + value);
        }
    }
    
}
