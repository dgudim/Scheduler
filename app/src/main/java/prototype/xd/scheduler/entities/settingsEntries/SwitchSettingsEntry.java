package prototype.xd.scheduler.entities.settingsEntries;

import static prototype.xd.scheduler.utilities.Utilities.addSwitchChangeListener;

import android.view.View;

import com.google.android.material.switchmaterial.SwitchMaterial;

import prototype.xd.scheduler.R;

public class SwitchSettingsEntry extends SettingsEntry{
    
    private final String key;
    private final String text;
    private final boolean defaultValue;
    
    public SwitchSettingsEntry(String key, boolean defaultValue, String text) {
        super(R.layout.settings_switch_entry);
        this.key = key;
        this.text = text;
        this.defaultValue = defaultValue;
    }
    
    @Override
    protected View InitInnerViews(View rootView) {
        SwitchMaterial switchView = rootView.findViewById(R.id.Switch);
        switchView.setText(text);
        addSwitchChangeListener(switchView, key, defaultValue);
        return super.InitInnerViews(rootView);
    }
}
