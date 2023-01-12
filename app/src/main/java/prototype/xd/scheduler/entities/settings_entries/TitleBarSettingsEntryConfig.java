package prototype.xd.scheduler.entities.settings_entries;

import static prototype.xd.scheduler.entities.settings_entries.SettingsEntryType.TITLE_BAR;

import androidx.annotation.NonNull;

import prototype.xd.scheduler.databinding.TitleSettingsEntryBinding;
import prototype.xd.scheduler.utilities.ContextWrapper;

public class TitleBarSettingsEntryConfig extends SettingsEntryConfig {
    
    private final String text;
    
    public TitleBarSettingsEntryConfig(String text) {
        this.text = text;
    }
    
    @Override
    public int getType() {
        return TITLE_BAR.ordinal();
    }
    
    static class TitleBarViewHolder extends SettingsEntryConfig.SettingsViewHolder<TitleSettingsEntryBinding, TitleBarSettingsEntryConfig> {
        
        TitleBarViewHolder(@NonNull ContextWrapper wrapper, @NonNull TitleSettingsEntryBinding viewBinding) {
            super(wrapper, viewBinding);
        }
        
        @Override
        void bind(TitleBarSettingsEntryConfig config) {
            viewBinding.textView.setText(config.text);
        }
    }
}
