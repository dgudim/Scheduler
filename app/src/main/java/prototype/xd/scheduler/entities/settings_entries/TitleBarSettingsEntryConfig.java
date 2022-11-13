package prototype.xd.scheduler.entities.settings_entries;

import static prototype.xd.scheduler.entities.settings_entries.SettingsEntryType.TITLE_BAR;

import prototype.xd.scheduler.databinding.TitleSettingsEntryBinding;

public class TitleBarSettingsEntryConfig implements SettingsEntryConfig {
    
    private final String text;
    
    public TitleBarSettingsEntryConfig(String text) {
        this.text = text;
    }
    
    @Override
    public int getType() {
        return TITLE_BAR.ordinal();
    }
    
    static class TitleBarViewHolder extends SettingsEntryConfig.SettingsViewHolder<TitleSettingsEntryBinding, TitleBarSettingsEntryConfig> {
    
        TitleBarViewHolder(TitleSettingsEntryBinding viewBinding) {
            super(viewBinding);
        }
        
        @Override
        void bind(TitleBarSettingsEntryConfig config) {
            viewBinding.textView.setText(config.text);
        }
    }
}
