package prototype.xd.scheduler.entities.settings_entries;

import static prototype.xd.scheduler.entities.settings_entries.SettingsEntryType.SEEK_BAR;

import androidx.annotation.PluralsRes;
import androidx.annotation.StringRes;

import prototype.xd.scheduler.databinding.SeekbarSettingsEntryBinding;
import prototype.xd.scheduler.utilities.Keys;
import prototype.xd.scheduler.utilities.Utilities;

public class SeekBarSettingsEntryConfig extends SettingsEntryConfig {
    
    private final int seekMin;
    private final int seekMax;
    private final boolean zeroIsOff;
    private final Keys.DefaultedInteger value;
    @PluralsRes @StringRes
    private final int stringResource;
    private final boolean discrete;
    
    public SeekBarSettingsEntryConfig(Keys.DefaultedInteger value, int seekMin, int seekMax,
                                      boolean discrete, boolean zeroIfOff,
                                      @StringRes @PluralsRes int stringResource) {
        this.seekMin = seekMin;
        this.seekMax = seekMax;
        this.zeroIsOff = zeroIfOff;
        this.value = value;
        this.stringResource = stringResource;
        this.discrete = discrete;
    }
    
    @Override
    public int getType() {
        return SEEK_BAR.ordinal();
    }
    
    static class SeekBarViewHolder extends SettingsEntryConfig.SettingsViewHolder<SeekbarSettingsEntryBinding, SeekBarSettingsEntryConfig> {
        
        SeekBarViewHolder(SeekbarSettingsEntryBinding viewBinding) {
            super(viewBinding);
        }
        
        @Override
        void bind(SeekBarSettingsEntryConfig config) {
            viewBinding.slider.setStepSize(config.discrete ? 1 : 0);
            viewBinding.slider.setValueFrom(config.seekMin);
            viewBinding.slider.setValueTo(config.seekMax);
            Utilities.setSliderChangeListener(
                    viewBinding.seekBarDescription, viewBinding.slider,
                    null,
                    config.stringResource,
                    config.value,
                    config.zeroIsOff);
        }
    }
}


