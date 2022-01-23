package prototype.xd.scheduler.entities.settingsEntries;

import static prototype.xd.scheduler.entities.settingsEntries.SettingsEntryType.SEEK_BAR_DISCRETE;

import androidx.fragment.app.Fragment;

import prototype.xd.scheduler.R;

public class DiscreteSeekBarSettingsEntry extends SeekBarSettingsEntry{
    public DiscreteSeekBarSettingsEntry(int seek_min, int seek_max, int defaultValue, String key, int stringResource, Fragment fragment) {
        super(seek_min, seek_max, defaultValue, key, stringResource, fragment);
        layoutId = R.layout.settings_discrete_seekbar_entry;
        entryType = SEEK_BAR_DISCRETE;
    }
}
