package prototype.xd.scheduler.entities.settings_entries;

import android.view.View;
import android.widget.ImageButton;

public abstract class GenericCalendarSettingsEntryConfig extends SettingsEntryConfig {
    
    private boolean showSettings;
    
    GenericCalendarSettingsEntryConfig(boolean showSettings) {
        setShowSettings(showSettings);
    }
    
    public void setShowSettings(boolean showSettings) {
        this.showSettings = showSettings;
    }
 
    protected void updateSettingsButtonVisibility(ImageButton settingsButton) {
        settingsButton.setVisibility(showSettings ? View.VISIBLE : View.GONE);
    }
    
}
