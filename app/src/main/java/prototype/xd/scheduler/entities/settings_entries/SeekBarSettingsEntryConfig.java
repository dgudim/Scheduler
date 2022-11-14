package prototype.xd.scheduler.entities.settings_entries;

import static prototype.xd.scheduler.entities.settings_entries.SettingsEntryType.SEEK_BAR;

import prototype.xd.scheduler.databinding.SeekbarSettingsEntryBinding;
import prototype.xd.scheduler.utilities.Utilities;

public class SeekBarSettingsEntryConfig extends SettingsEntryConfig {
    
    private final int seekMin;
    private final int seekMax;
    private final int defaultValue;
    private final boolean zeroIsOff;
    private final String key;
    private final int stringResource;
    private final boolean discrete;
    
    public SeekBarSettingsEntryConfig(int seekMin, int seekMax, int defaultValue, boolean discrete, boolean zeroIfOff, String key, int stringResource) {
        this.seekMin = seekMin;
        this.seekMax = seekMax;
        this.defaultValue = defaultValue;
        this.zeroIsOff = zeroIfOff;
        this.key = key;
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
                    config.stringResource, config.key,
                    config.defaultValue, config.zeroIsOff);
        }
    }
}


