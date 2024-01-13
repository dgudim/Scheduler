package prototype.xd.scheduler.entities.settings_entries;

import static prototype.xd.scheduler.entities.settings_entries.SettingsEntryType.TITLE_BAR;

import androidx.annotation.NonNull;
import androidx.annotation.StringRes;

import prototype.xd.scheduler.databinding.TitleSettingsEntryBinding;
import prototype.xd.scheduler.utilities.misc.ContextWrapper;

public class TitleBarSettingsEntryConfig extends SettingsEntryConfig {
    
    @StringRes
    private final int textId;
    
    public TitleBarSettingsEntryConfig(@StringRes int textId) {
        this.textId = textId;
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
            binding.textView.setText(config.textId);
        }
    }
}
