package prototype.xd.scheduler;

import static android.os.Process.killProcess;
import static android.os.Process.myPid;

import android.content.Intent;
import android.os.Bundle;
import android.util.Log;

import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.app.AppCompatDelegate;

import com.google.android.material.color.DynamicColors;
import com.google.android.material.color.HarmonizedColors;
import com.google.android.material.color.HarmonizedColorsOptions;

import java.io.File;

import prototype.xd.scheduler.utilities.Static;
import prototype.xd.scheduler.utilities.Logger;
import prototype.xd.scheduler.utilities.PermissionUtilities;
import prototype.xd.scheduler.utilities.services.BackgroundSetterService;

public class MainActivity extends AppCompatActivity {
    
    public static final String NAME = MainActivity.class.getSimpleName();
    
    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
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
        
        Static.init(this);
        Logger.setDebugEnabled(Static.DEBUG_LOGGING.get() || BuildConfig.DEBUG);
        
        File rootDir = getExternalFilesDir("");
        if (rootDir == null) {
            Log.e(NAME, "Shared storage not available wtf");
            killProcess(myPid());
        } else {
            Static.ROOT_DIR.set(rootDir.getAbsolutePath());
            Logger.info(NAME, "Root dir: " + rootDir);
            if (!rootDir.exists()) {
                Logger.info(NAME, "Created folder structure: " + rootDir.mkdirs());
            }
        }
        
        // init theme
        AppCompatDelegate.setDefaultNightMode(Static.APP_THEME.get());
        
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
        if (PermissionUtilities.areEssentialPermissionsGranted(this)) {
            BackgroundSetterService.ping(getApplicationContext());
            setContentView(R.layout.activity_main);
        } else {
            // switch intro activity and close current one
            startActivity(new Intent(this, IntroActivity.class));
            finish();
        }
    }
}
