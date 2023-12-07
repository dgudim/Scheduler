package prototype.xd.scheduler;

import android.os.Bundle;

import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.app.AppCompatDelegate;

import prototype.xd.scheduler.utilities.ImageUtilities;
import prototype.xd.scheduler.utilities.PermissionUtilities;
import prototype.xd.scheduler.utilities.Static;
import prototype.xd.scheduler.utilities.Utilities;
import prototype.xd.scheduler.utilities.services.BackgroundSetterService;

public class MainActivity extends AppCompatActivity {
    
    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        // for enabling all warnings
        //StrictMode.enableDefaults();
        
        // init static stuff
        Static.init(this);
        
        // init theme
        AppCompatDelegate.setDefaultNightMode(Static.APP_THEME.get());
        
        ImageUtilities.harmonizeColorsForActivity(this,
                R.color.gray_harmonized,
                R.color.entry_settings_parameter_default,
                R.color.entry_settings_parameter_group,
                R.color.entry_settings_parameter_group_and_personal,
                R.color.entry_settings_parameter_personal);
        
        
        if (PermissionUtilities.areEssentialPermissionsGranted(this)) {
            BackgroundSetterService.ping(this, true);
            setContentView(R.layout.activity_main);
        } else {
            // switch intro activity and close current one
            Utilities.switchActivity(this, IntroActivity.class);
        }
    }
}
