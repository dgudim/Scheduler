package prototype.xd.scheduler.entities.settings_entries;

import static prototype.xd.scheduler.entities.settings_entries.SettingsEntryType.SWITCH;
import static prototype.xd.scheduler.utilities.Utilities.setSwitchChangeListener;

import prototype.xd.scheduler.databinding.SwitchSettingsEntryBinding;
import prototype.xd.scheduler.utilities.Utilities;

public class SwitchSettingsEntryConfig implements SettingsEntryConfig {
    
    private final String key;
    private final String text;
    private final boolean defaultValue;
    
    public SwitchSettingsEntryConfig(String key, boolean defaultValue, String text) {
        this.key = key;
        this.text = text;
        this.defaultValue = defaultValue;
    }
    
    @Override
    public int getType() {
        return SWITCH.ordinal();
    }
    
    static class SwitchViewHolder extends SettingsEntryConfig.SettingsViewHolder<SwitchSettingsEntryBinding, SwitchSettingsEntryConfig> {
        
        SwitchViewHolder(SwitchSettingsEntryBinding viewBinding) {
            super(viewBinding);
        }
        
        @Override
        void bind(SwitchSettingsEntryConfig config) {
            viewBinding.mainSwitch.setText(config.text);
            Utilities.setSwitchChangeListener(viewBinding.mainSwitch, config.key, config.defaultValue);
        }
    }
}


