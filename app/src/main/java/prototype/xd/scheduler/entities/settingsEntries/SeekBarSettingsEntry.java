package prototype.xd.scheduler.entities.settingsEntries;

import static prototype.xd.scheduler.entities.settingsEntries.SettingsEntryType.SEEK_BAR;
import static prototype.xd.scheduler.utilities.Utilities.addSeekBarChangeListener;

import android.view.View;
import android.widget.SeekBar;

import androidx.fragment.app.Fragment;

import prototype.xd.scheduler.R;

public class SeekBarSettingsEntry extends SettingsEntry{
    
    private final int seek_min;
    private final int seek_max;
    private final int defaultValue;
    private final String key;
    private final int stringResource;
    private final Fragment fragment;
    
    public SeekBarSettingsEntry(int seek_min, int seek_max, int defaultValue, String key, int stringResource, Fragment fragment) {
        super(R.layout.settings_seekbar_entry);
        this.seek_min = seek_min;
        this.seek_max = seek_max;
        this.defaultValue = defaultValue;
        this.key = key;
        this.stringResource = stringResource;
        this.fragment = fragment;
        entryType = SEEK_BAR;
    }
    
    @Override
    protected View InitInnerViews(View rootView) {
        SeekBar seekBar = rootView.findViewById(R.id.seek_bar);
        seekBar.setMin(seek_min);
        seekBar.setMax(seek_max);
        addSeekBarChangeListener(
                rootView.findViewById(R.id.seek_bar_description), seekBar,
                fragment, stringResource,
                key, defaultValue);
        return super.InitInnerViews(rootView);
    }
}
