package prototype.xd.scheduler.utilities;

import static prototype.xd.scheduler.utilities.Keys.PREFERENCES;
import static prototype.xd.scheduler.utilities.Keys.PREFERENCES_SERVICE;

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
    
}
