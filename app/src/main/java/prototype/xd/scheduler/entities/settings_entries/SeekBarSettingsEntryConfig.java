package prototype.xd.scheduler.entities.settings_entries;

import static prototype.xd.scheduler.entities.settings_entries.SettingsEntryType.SEEK_BAR;

import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.annotation.PluralsRes;
import androidx.annotation.StringRes;

import com.google.android.material.slider.Slider;

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
        
        // bind from anywhere
        static void bindExternal(SeekBarSettingsEntryConfig config, Slider slider, TextView sliderDescription) {
            slider.setStepSize(config.stepSize);
            slider.setValueFrom(config.seekMin);
            slider.setValueTo(config.seekMax);
            if (config.textFormatter != null) {
                Utilities.setSliderChangeListener(sliderDescription, slider,
                        null,
                        config.value,
                        config.textFormatter);
            } else {
                Utilities.setSliderChangeListener(
                        sliderDescription, slider,
                        null,
                        config.stringResource,
                        config.value,
                        config.zeroIsOff);
            }
        }
        
        @Override
        void bind(SeekBarSettingsEntryConfig config) {
            bindExternal(config, viewBinding.slider, viewBinding.sliderDescription);
        }
    }
}


