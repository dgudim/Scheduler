package prototype.xd.scheduler.entities.settingsEntries;

import static prototype.xd.scheduler.entities.settingsEntries.SettingsEntryType.SEEK_BAR;
import static prototype.xd.scheduler.utilities.Utilities.addSliderChangeListener;

import android.view.View;
import android.view.ViewGroup;

import com.google.android.material.slider.Slider;

import prototype.xd.scheduler.R;
import prototype.xd.scheduler.utilities.Utilities;

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
        Slider slider = convertView.findViewById(R.id.slider);
        slider.setValueFrom(seek_min);
        slider.setValueTo(seek_max);
        Utilities.addSliderChangeListener(
                convertView.findViewById(R.id.seek_bar_description), slider,
                null,
                stringResource,
                key, defaultValue);
        return super.InitInnerViews(convertView, viewGroup);
    }
}
