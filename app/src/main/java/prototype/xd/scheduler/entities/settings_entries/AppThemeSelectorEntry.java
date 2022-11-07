package prototype.xd.scheduler.entities.settings_entries;

import static prototype.xd.scheduler.entities.settings_entries.SettingsEntryType.APP_THEME_SELECTOR;

import prototype.xd.scheduler.R;

public class AppThemeSelectorEntry extends SettingsEntry {
    public AppThemeSelectorEntry() {
        super(R.layout.settings_app_theme_selector_entry);
        entryType = APP_THEME_SELECTOR;
    }
}
