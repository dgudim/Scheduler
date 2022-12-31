package prototype.xd.scheduler.entities.settings_entries;

import static prototype.xd.scheduler.entities.settings_entries.SettingsEntryType.SEEK_BAR;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.annotation.PluralsRes;
import androidx.annotation.StringRes;

import java.util.function.Function;

import prototype.xd.scheduler.databinding.SeekbarSettingsEntryBinding;
import prototype.xd.scheduler.utilities.Keys;
import prototype.xd.scheduler.utilities.Utilities;

public class SeekBarSettingsEntryConfig extends SettingsEntryConfig {
    
    private final int seekMin;
    private final int seekMax;
    private final int stepSize;
    private boolean zeroIsOff;
    private final Keys.DefaultedInteger value;
    @PluralsRes
    @StringRes
    private int stringResource;
    
    @Nullable
    Function<Integer, String> textFormatter;
    
    public SeekBarSettingsEntryConfig(Keys.DefaultedInteger value, int seekMin, int seekMax, int stepSize, boolean zeroIfOff,
                                      @StringRes @PluralsRes int stringResource) {
        this.seekMin = seekMin;
        this.seekMax = seekMax;
        this.stepSize = stepSize;
        this.zeroIsOff = zeroIfOff;
        this.value = value;
        this.stringResource = stringResource;
    }
    
    public SeekBarSettingsEntryConfig(Keys.DefaultedInteger value, int seekMin, int seekMax, int stepSize,
                                      @NonNull Function<Integer, String> textFormatter) {
        this.seekMin = seekMin;
        this.seekMax = seekMax;
        this.stepSize = stepSize;
        this.value = value;
        this.textFormatter = textFormatter;
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
            viewBinding.slider.setStepSize(config.stepSize);
            viewBinding.slider.setValueFrom(config.seekMin);
            viewBinding.slider.setValueTo(config.seekMax);
            if (config.textFormatter != null) {
                Utilities.setSliderChangeListener(viewBinding.seekBarDescription, viewBinding.slider,
                        null,
                        config.value,
                        config.textFormatter);
            } else {
                Utilities.setSliderChangeListener(
                        viewBinding.seekBarDescription, viewBinding.slider,
                        null,
                        config.stringResource,
                        config.value,
                        config.zeroIsOff);
            }
        }
    }
}


