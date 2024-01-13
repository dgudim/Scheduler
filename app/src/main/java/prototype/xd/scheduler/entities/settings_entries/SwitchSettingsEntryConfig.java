package prototype.xd.scheduler.entities.settings_entries;

import static prototype.xd.scheduler.entities.settings_entries.SettingsEntryType.SWITCH;

import android.widget.CompoundButton;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.annotation.StringRes;

import prototype.xd.scheduler.databinding.SwitchSettingsEntryBinding;
import prototype.xd.scheduler.utilities.Static;
import prototype.xd.scheduler.utilities.Utilities;
import prototype.xd.scheduler.utilities.misc.ContextWrapper;

public class SwitchSettingsEntryConfig extends SettingsEntryConfig {
    
    @StringRes
    private final int textId;
    @NonNull
    private final Static.DefaultedBoolean value;
    @Nullable
    private final CompoundButton.OnCheckedChangeListener onCheckedChangeListener;
    
    public SwitchSettingsEntryConfig(@NonNull Static.DefaultedBoolean value,
                                     @StringRes int textId,
                                     @Nullable CompoundButton.OnCheckedChangeListener onCheckedChangeListener,
                                     boolean instantlyTriggerListener) {
        this.textId = textId;
        this.value = value;
        this.onCheckedChangeListener = onCheckedChangeListener;
        if (onCheckedChangeListener != null && instantlyTriggerListener) {
            onCheckedChangeListener.onCheckedChanged(null, value.get());
        }
    }
    
    public SwitchSettingsEntryConfig(@NonNull Static.DefaultedBoolean value, @StringRes int textId) {
        this(value, textId, null, false);
    }
    
    @Override
    public int getRecyclerViewType() {
        return SWITCH.ordinal();
    }
    
    static class ViewHolder extends SettingsEntryConfig.SettingsViewHolder<SwitchSettingsEntryBinding, SwitchSettingsEntryConfig> {
        
        ViewHolder(@NonNull ContextWrapper wrapper, @NonNull SwitchSettingsEntryBinding viewBinding) {
            super(wrapper, viewBinding);
        }
        
        @Override
        void bind(@NonNull SwitchSettingsEntryConfig config) {
            binding.mainSwitch.setText(config.textId);
            Utilities.setSwitchChangeListener(
                    binding.mainSwitch,
                    config.value,
                    config.onCheckedChangeListener);
        }
    }
}


