package prototype.xd.scheduler.entities.settings_entries;

import static prototype.xd.scheduler.entities.settings_entries.SettingsEntryType.APP_THEME_SELECTOR;

import prototype.xd.scheduler.databinding.AppThemeSelectorSettingsEntryBinding;

public class AppThemeSelectorEntryConfig extends SettingsEntryConfig {
    @Override
    public int getType() {
        return APP_THEME_SELECTOR.ordinal();
    }
    
    static class AppThemeSelectorViewHolder extends SettingsEntryConfig.SettingsViewHolder<AppThemeSelectorSettingsEntryBinding, AppThemeSelectorEntryConfig> {
        
        AppThemeSelectorViewHolder(AppThemeSelectorSettingsEntryBinding viewBinding) {
            super(viewBinding);
        }
        
        @Override
        void bind(AppThemeSelectorEntryConfig config) {
            // nothing should be done, this entry is self-configured
        }
    }
}


