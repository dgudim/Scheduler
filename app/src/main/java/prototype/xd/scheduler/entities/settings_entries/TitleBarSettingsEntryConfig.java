package prototype.xd.scheduler.entities.settings_entries;

import static prototype.xd.scheduler.entities.settings_entries.SettingsEntryType.TITLE_BAR;

import androidx.annotation.NonNull;

import prototype.xd.scheduler.databinding.TitleSettingsEntryBinding;
import prototype.xd.scheduler.utilities.misc.ContextWrapper;

public class TitleBarSettingsEntryConfig extends SettingsEntryConfig {
    
    @NonNull
    private final String text;
    
    public TitleBarSettingsEntryConfig(@NonNull String text) {
        this.text = text;
    }
    
    @Override
    public int getRecyclerViewType() {
        return TITLE_BAR.ordinal();
    }
    
    static class ViewHolder extends SettingsEntryConfig.SettingsViewHolder<TitleSettingsEntryBinding, TitleBarSettingsEntryConfig> {
        
        ViewHolder(@NonNull ContextWrapper wrapper, @NonNull TitleSettingsEntryBinding viewBinding) {
            super(wrapper, viewBinding);
        }
        
        @Override
        void bind(@NonNull TitleBarSettingsEntryConfig config) {
            viewBinding.textView.setText(config.text);
        }
    }
}
