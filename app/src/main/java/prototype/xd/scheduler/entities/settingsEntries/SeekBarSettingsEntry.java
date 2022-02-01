package prototype.xd.scheduler.entities.settingsEntries;

import static prototype.xd.scheduler.entities.settingsEntries.SettingsEntryType.SEEK_BAR;
import static prototype.xd.scheduler.utilities.Utilities.addSeekBarChangeListener;

import android.view.View;
import android.view.ViewGroup;
import android.widget.SeekBar;

import prototype.xd.scheduler.R;

public class SeekBarSettingsEntry extends SettingsEntry{
    
    private final int seek_min;
    private final int seek_max;
    private final int defaultValue;
    private final String key;
    private final int stringResource;
    
    public SeekBarSettingsEntry(int seek_min, int seek_max, int defaultValue, String key, int stringResource) {
        super(R.layout.settings_seekbar_entry);
        this.seek_min = seek_min;
        this.seek_max = seek_max;
        this.defaultValue = defaultValue;
        this.key = key;
        this.stringResource = stringResource;
        entryType = SEEK_BAR;
    }
    
    @Override
    protected View InitInnerViews(View convertView, ViewGroup viewGroup) {
        SeekBar seekBar = convertView.findViewById(R.id.seek_bar);
        seekBar.setMin(seek_min);
        seekBar.setMax(seek_max);
        addSeekBarChangeListener(
                convertView.findViewById(R.id.seek_bar_description), seekBar,
                stringResource,
                key, defaultValue);
        return super.InitInnerViews(convertView, viewGroup);
    }
}
