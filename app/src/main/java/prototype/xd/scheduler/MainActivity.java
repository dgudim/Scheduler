package prototype.xd.scheduler;

import static android.util.Log.ERROR;
import static android.util.Log.INFO;
import static prototype.xd.scheduler.utilities.BitmapUtilities.fingerPrintAndSaveBitmap;
import static prototype.xd.scheduler.utilities.DateManager.availableDays;
import static prototype.xd.scheduler.utilities.DateManager.getCurrentTimestamp;
import static prototype.xd.scheduler.utilities.Keys.ROOT_DIR;
import static prototype.xd.scheduler.utilities.Logger.log;
import static prototype.xd.scheduler.utilities.Logger.logException;
import static prototype.xd.scheduler.utilities.PreferencesStore.preferences;
import static prototype.xd.scheduler.utilities.PreferencesStore.preferences_service;
import static prototype.xd.scheduler.utilities.Utilities.getRootDir;

import android.Manifest;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.graphics.BitmapFactory;
import android.graphics.Color;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.app.AppCompatDelegate;
import androidx.cardview.widget.CardView;
import androidx.core.app.ActivityCompat;

import com.google.android.material.color.DynamicColors;
import com.google.android.material.color.MaterialColors;

import java.io.File;
import java.io.InputStream;
import java.util.Locale;
import java.util.Objects;

import prototype.xd.scheduler.utilities.Keys;
import prototype.xd.scheduler.utilities.PreferencesStore;
import prototype.xd.scheduler.utilities.services.BackgroundSetterService;

public class MainActivity extends AppCompatActivity {
    
    private static final String[] PERMISSIONS = {
            Manifest.permission.READ_EXTERNAL_STORAGE,
            Manifest.permission.READ_CALENDAR
    };
    
    public static final int REQUEST_CODE_PERMISSIONS = 13;
    
    private boolean isCrapPhone() {
        return !preferences.getBoolean(Keys.XIAOMI_MESSAGE_IGNORE, false) &&
                (Build.MANUFACTURER.toLowerCase(Locale.ROOT).contains("xiaomi") || Build.MODEL.toLowerCase(Locale.ROOT).contains("xiaomi"));
    }
    
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        
        PreferencesStore.init(this);
        
        File rootDir = getExternalFilesDir("");
        if (rootDir == null) {
            Log.e("MainActivity", "Shared storage not available wtf");
            System.exit(0);
        } else if (preferences.getString(ROOT_DIR, null) == null) {
            preferences.edit().putString(ROOT_DIR, rootDir.getAbsolutePath()).apply();
            log(INFO, "Main Activity", "root dir: " + rootDir);
            if (!rootDir.exists()) {
                log(INFO, "Main Activity", "created folder structure: " + rootDir.mkdirs());
            }
        }
        
       /* try {
            Class.forName("dalvik.system.CloseGuard")
                    .getMethod("setEnabled", boolean.class)
                    .invoke(null, true);
        } catch (ReflectiveOperationException e) {
            throw new RuntimeException(e);
        }*/
        
        // init theme
        AppCompatDelegate.setDefaultNightMode(preferences.getInt(Keys.APP_THEME, Keys.DEFAULT_APP_THEME));
        DynamicColors.applyToActivityIfAvailable(this);
        
        if (!refreshPermissionStates(false)) {
            setContentView(R.layout.permissions_request_screen);
            refreshPermissionStates(true);
            View grant_button = findViewById(R.id.grant_permissions_button);
            grant_button.setOnClickListener(v -> requestPermissions(PERMISSIONS, REQUEST_CODE_PERMISSIONS));
        } else {
            if (isServiceBeingKilled()) {
                setContentView(R.layout.service_keep_alive_screen);
                
                if (preferences_service.getInt(Keys.SERVICE_KILLED_IGNORE_BUTTON_CLICKED, 0) > 1) {
                    View dontBotherButton = findViewById(R.id.never_ask_again_button);
                    dontBotherButton.setVisibility(View.VISIBLE);
                    dontBotherButton.setOnClickListener(v -> {
                        preferences_service.edit().putBoolean(Keys.SERVICE_KILLED_DONT_BOTHER, true).apply();
                        launchMainActivity();
                    });
                }
                
                findViewById(R.id.learn_howTo_button).setOnClickListener(v ->
                        startActivity(new Intent(Intent.ACTION_VIEW).setData(Uri.parse("https://dontkillmyapp.com/"))));
                
                findViewById(R.id.ignore_button).setOnClickListener(v -> {
                    preferences_service.edit().putInt(Keys.SERVICE_KILL_THRESHOLD_REACHED, 0).apply();
                    preferences_service.edit().putInt(Keys.SERVICE_KILLED_IGNORE_BUTTON_CLICKED,
                            preferences_service.getInt(Keys.SERVICE_KILLED_IGNORE_BUTTON_CLICKED, 0) + 1).apply();
                    launchMainActivity();
                });
            } else if (isCrapPhone()) {
                setContentView(R.layout.you_have_a_xiaomi_phone_screen);
                findViewById(R.id.ignore_button).setOnClickListener(v -> {
                    preferences_service.edit().putBoolean(Keys.XIAOMI_MESSAGE_IGNORE, true).apply();
                    launchMainActivity();
                });
                
            } else {
                launchMainActivity();
            }
        }
    }
    
    private boolean isServiceBeingKilled() {
        if (preferences_service.getBoolean(Keys.SERVICE_KILLED_DONT_BOTHER, false)) {
            return false;
        }
        long prevTime = preferences_service.getLong(Keys.LAST_UPDATE_TIME, getCurrentTimestamp());
        if (getCurrentTimestamp() - prevTime > 4 * 60 * 60 * 1000) {
            preferences_service.edit().putInt(Keys.SERVICE_KILL_THRESHOLD_REACHED,
                    preferences_service.getInt(Keys.SERVICE_KILL_THRESHOLD_REACHED, 0) + 1).apply();
        }
        return preferences_service.getInt(Keys.SERVICE_KILL_THRESHOLD_REACHED, 0) > 15;
    }
    
    private void launchMainActivity() {
        BackgroundSetterService.ping(getApplicationContext());
        setContentView(R.layout.activity_main);
    }
    
    private boolean refreshPermissionStates(boolean display) {
        boolean storage_granted = ActivityCompat.checkSelfPermission(this, Manifest.permission.READ_EXTERNAL_STORAGE) == PackageManager.PERMISSION_GRANTED;
        boolean calendar_granted = ActivityCompat.checkSelfPermission(this, Manifest.permission.READ_CALENDAR) == PackageManager.PERMISSION_GRANTED;
        
        if (display) {
            setPermissionChipColor(calendar_granted,
                    findViewById(R.id.calendar_permission_granted),
                    findViewById(R.id.calendar_permission_granted_bg));
    
            setPermissionChipColor(storage_granted,
                    findViewById(R.id.storage_permission_granted),
                    findViewById(R.id.storage_permission_granted_bg));
        }
        
        return storage_granted && calendar_granted;
    }
    
    private void setPermissionChipColor(boolean permission_granted, TextView permission_text, CardView permission_text_bg) {
        permission_text.setText(permission_granted ? R.string.permissions_granted : R.string.permissions_not_granted);
        permission_text.setTextColor(MaterialColors.getColor(permission_text, permission_granted ? R.attr.colorOnTertiaryContainer : R.attr.colorOnErrorContainer,
                permission_granted ? Color.GREEN : Color.RED));
        permission_text_bg.setCardBackgroundColor(MaterialColors.getColor(permission_text_bg, permission_granted ? R.attr.colorTertiaryContainer : R.attr.colorErrorContainer,
                Color.LTGRAY));
    }
    
    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        if (refreshPermissionStates(true)) {
            launchMainActivity();
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
                            fingerPrintAndSaveBitmap(BitmapFactory.decodeStream(stream),
                                    new File(getRootDir(), availableDays[requestCode] + ".png"));
                            stream.close();
                        } else {
                            log(ERROR, "MainActivity", "stream null for uri: " + uri.getPath());
                        }
                        
                        
                        runOnUiThread(() -> ((SettingsFragment) Objects.requireNonNull(getSupportFragmentManager().findFragmentById(R.id.nav_host_fragment))
                                .getChildFragmentManager().getFragments().get(0)).notifyBgSelected());
                    } catch (Exception e) {
                        logException("LBCP thread", e);
                    }
                }, "LBCP thread").start();
            }
        }
    }
}