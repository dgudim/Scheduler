package prototype.xd.scheduler.entities.settings_entries;

import static prototype.xd.scheduler.entities.settings_entries.SettingsEntryType.SWITCH;

import android.widget.CompoundButton;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import prototype.xd.scheduler.databinding.SwitchSettingsEntryBinding;
import prototype.xd.scheduler.utilities.ContextWrapper;
import prototype.xd.scheduler.utilities.Keys;
import prototype.xd.scheduler.utilities.Utilities;

public class SwitchSettingsEntryConfig extends SettingsEntryConfig {
    
    private final String text;
    private final Keys.DefaultedBoolean value;
    @Nullable
    private final CompoundButton.OnCheckedChangeListener onCheckedChangeListener;
    
    public SwitchSettingsEntryConfig(@NonNull Keys.DefaultedBoolean value,
                                     @NonNull String text,
                                     @Nullable CompoundButton.OnCheckedChangeListener onCheckedChangeListener,
                                     boolean instantlyTriggerListener) {
        this.text = text;
        this.value = value;
        this.onCheckedChangeListener = onCheckedChangeListener;
        if (onCheckedChangeListener != null && instantlyTriggerListener) {
            onCheckedChangeListener.onCheckedChanged(null, value.get());
        }
    }
    
    public SwitchSettingsEntryConfig(Keys.DefaultedBoolean value, String text) {
        this(value, text, null, false);
    }
    
    @Override
    public int getType() {
        return SWITCH.ordinal();
    }
    
    static class SwitchViewHolder extends SettingsEntryConfig.SettingsViewHolder<SwitchSettingsEntryBinding, SwitchSettingsEntryConfig> {
        
        SwitchViewHolder(@NonNull ContextWrapper wrapper, @NonNull SwitchSettingsEntryBinding viewBinding) {
            super(wrapper, viewBinding);
        }
        
        @Override
        void bind(SwitchSettingsEntryConfig config) {
            viewBinding.mainSwitch.setText(config.text);
            Utilities.setSwitchChangeListener(
                    viewBinding.mainSwitch,
                    config.value,
                    config.onCheckedChangeListener);
        }
    }
}


