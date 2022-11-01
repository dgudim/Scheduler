package prototype.xd.scheduler.entities.settings_entries;

import static prototype.xd.scheduler.entities.settings_entries.SettingsEntryType.SEEK_BAR_DISCRETE;

import prototype.xd.scheduler.R;

public class DiscreteSeekBarSettingsEntry extends SeekBarSettingsEntry{
    public DiscreteSeekBarSettingsEntry(int seekMin, int seekMax, int defaultValue, String key, int stringResource) {
        super(seekMin, seekMax, defaultValue, key, stringResource);
        layoutId = R.layout.settings_discrete_seekbar_entry;
        entryType = SEEK_BAR_DISCRETE;
    }
}
