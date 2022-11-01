package prototype.xd.scheduler.entities.settings_entries;

import static prototype.xd.scheduler.entities.settings_entries.SettingsEntryType.SWITCH;
import static prototype.xd.scheduler.utilities.Utilities.addSwitchChangeListener;

import android.view.View;
import android.view.ViewGroup;

import prototype.xd.scheduler.R;
import prototype.xd.scheduler.views.Switch;

public class SwitchSettingsEntry extends SettingsEntry{
    
    private final String key;
    private final String text;
    private final boolean defaultValue;
    
    public SwitchSettingsEntry(String key, boolean defaultValue, String text) {
        super(R.layout.settings_switch_entry);
        this.key = key;
        this.text = text;
        this.defaultValue = defaultValue;
        entryType = SWITCH;
    }
    
    @Override
    protected View initInnerViews(View convertView, ViewGroup viewGroup) {
        Switch switchView = convertView.findViewById(R.id.Switch);
        switchView.setText(text);
        addSwitchChangeListener(switchView, key, defaultValue);
        return super.initInnerViews(convertView, viewGroup);
    }
}
