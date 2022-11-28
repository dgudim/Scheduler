package prototype.xd.scheduler;

import static android.util.Log.ERROR;
import static android.util.Log.INFO;
import static prototype.xd.scheduler.utilities.Keys.ROOT_DIR;
import static prototype.xd.scheduler.utilities.Logger.log;
import static prototype.xd.scheduler.utilities.Logger.logException;
import static prototype.xd.scheduler.utilities.PreferencesStore.preferences;
import static prototype.xd.scheduler.utilities.Utilities.findFragmentInNavHost;
import static prototype.xd.scheduler.utilities.Utilities.getRootDir;

import android.content.Intent;
import android.graphics.BitmapFactory;
import android.net.Uri;
import android.os.Bundle;
import android.util.Log;

import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.app.AppCompatDelegate;

import com.google.android.material.color.DynamicColors;
import com.google.android.material.color.HarmonizedColors;
import com.google.android.material.color.HarmonizedColorsOptions;

import java.io.File;
import java.io.InputStream;

import prototype.xd.scheduler.utilities.BitmapUtilities;
import prototype.xd.scheduler.utilities.Keys;
import prototype.xd.scheduler.utilities.PreferencesStore;
import prototype.xd.scheduler.utilities.services.BackgroundSetterService;

public class MainActivity extends AppCompatActivity {
    
    private static final String NAME = "MainActivity";
    public static final String PACKAGE_NAME = BuildConfig.APPLICATION_ID;
    
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        // enable warnings in debug mode
        //if (BuildConfig.DEBUG) {
            //StrictMode.enableDefaults();
            /* try {
            Class.forName("dalvik.system.CloseGuard")
                    .getMethod("setEnabled", boolean.class)
                    .invoke(null, true);
            } catch (ReflectiveOperationException e) {
                throw new RuntimeException(e);
            }*/
        //}
        
        PreferencesStore.init(this);
        
        File rootDir = getExternalFilesDir("");
        if (rootDir == null) {
            Log.e(NAME, "Shared storage not available wtf");
            System.exit(0);
        } else if (preferences.getString(ROOT_DIR, null) == null) {
            preferences.edit().putString(ROOT_DIR, rootDir.getAbsolutePath()).apply();
            log(INFO, NAME, "root dir: " + rootDir);
            if (!rootDir.exists()) {
                log(INFO, NAME, "created folder structure: " + rootDir.mkdirs());
            }
        }
        
        // init theme
        AppCompatDelegate.setDefaultNightMode(preferences.getInt(Keys.APP_THEME, Keys.DEFAULT_APP_THEME));
        
        HarmonizedColors.applyToContextIfAvailable(this,
                new HarmonizedColorsOptions.Builder()
                        .setColorResourceIds(new int[]{
                                R.color.gray_harmonized,
                                R.color.entry_settings_parameter_default,
                                R.color.entry_settings_parameter_group,
                                R.color.entry_settings_parameter_group_and_personal,
                                R.color.entry_settings_parameter_personal})
                        .build());
        
        DynamicColors.applyToActivityIfAvailable(this);
        
        // switch intro activity and close current one
        if (!preferences.getBoolean(Keys.INTRO_SHOWN, false)) {
            // switch intro activity and close current one
            MainActivity.this.startActivity(new Intent(MainActivity.this, IntroActivity.class));
            finish();
        } else {
            BackgroundSetterService.ping(getApplicationContext());
            setContentView(R.layout.activity_main);
        }
    }
    
    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent intent) {
        super.onActivityResult(requestCode, resultCode, intent);
        if (resultCode != RESULT_OK) return;
        if (requestCode >= 0 && requestCode <= 7) {
            Uri uri = intent.getData();
            if (uri != null) {
                new Thread(() -> {
                    try {
                        
                        InputStream stream = getContentResolver().openInputStream(uri);
                        if (stream != null) {
                            BitmapUtilities.fingerPrintAndSaveBitmap(BitmapFactory.decodeStream(stream),
                                    new File(getRootDir(), Keys.WEEK_DAYS.get(requestCode) + ".png"));
                            stream.close();
                        } else {
                            log(ERROR, NAME, "stream null for uri: " + uri.getPath());
                        }
                        
                        runOnUiThread(() -> findFragmentInNavHost(this, SettingsFragment.class).notifyBgSelected());
                    } catch (Exception e) {
                        logException("LBCP thread", e);
                    }
                }, "LBCP thread").start();
            }
        }
    }
}