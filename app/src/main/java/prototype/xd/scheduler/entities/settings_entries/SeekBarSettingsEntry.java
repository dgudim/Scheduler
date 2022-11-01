package prototype.xd.scheduler.entities.settings_entries;

import static prototype.xd.scheduler.entities.settings_entries.SettingsEntryType.SEEK_BAR;

import android.view.View;
import android.view.ViewGroup;

import com.google.android.material.slider.Slider;

import prototype.xd.scheduler.R;
import prototype.xd.scheduler.utilities.Utilities;

public class SeekBarSettingsEntry extends SettingsEntry{
    
    private final int seekMin;
    private final int seekMax;
    private final int defaultValue;
    private final String key;
    private final int stringResource;
    
    public SeekBarSettingsEntry(int seekMin, int seekMax, int defaultValue, String key, int stringResource) {
        super(R.layout.settings_seekbar_entry);
        this.seekMin = seekMin;
        this.seekMax = seekMax;
        this.defaultValue = defaultValue;
        this.key = key;
        this.stringResource = stringResource;
        entryType = SEEK_BAR;
    }
    
    @Override
    protected View initInnerViews(View convertView, ViewGroup viewGroup) {
        Slider slider = convertView.findViewById(R.id.slider);
        slider.setValueFrom(seekMin);
        slider.setValueTo(seekMax);
        Utilities.addSliderChangeListener(
                convertView.findViewById(R.id.seek_bar_description), slider,
                null,
                stringResource,
                key, defaultValue);
        return super.initInnerViews(convertView, viewGroup);
    }
}
