package prototype.xd.scheduler.entities.settings_entries;

import static prototype.xd.scheduler.entities.settings_entries.SettingsEntryType.APP_THEME_SELECTOR;

import androidx.annotation.NonNull;

import prototype.xd.scheduler.databinding.AppThemeSelectorSettingsEntryBinding;
import prototype.xd.scheduler.utilities.misc.ContextWrapper;

public class AppThemeSelectorEntryConfig extends SettingsEntryConfig {
    @Override
    public int getRecyclerViewType() {
        return APP_THEME_SELECTOR.ordinal();
    }
    
    static class AppThemeSelectorViewHolder extends SettingsEntryConfig.SettingsViewHolder<AppThemeSelectorSettingsEntryBinding, AppThemeSelectorEntryConfig> {
        
        AppThemeSelectorViewHolder(@NonNull ContextWrapper wrapper, @NonNull AppThemeSelectorSettingsEntryBinding viewBinding) {
            super(wrapper, viewBinding);
        }
        
        @Override
        void bind(@NonNull AppThemeSelectorEntryConfig config) {
            // nothing should be done, this entry is self-configured
        }
    }
}


