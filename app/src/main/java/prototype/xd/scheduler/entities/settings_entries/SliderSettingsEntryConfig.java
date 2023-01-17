package prototype.xd.scheduler.entities.settings_entries;

import static prototype.xd.scheduler.entities.settings_entries.SettingsEntryType.SLIDER;

import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.annotation.PluralsRes;
import androidx.annotation.StringRes;

import com.google.android.material.slider.Slider;

import java.util.function.IntFunction;

import prototype.xd.scheduler.databinding.SliderSettingsEntryBinding;
import prototype.xd.scheduler.utilities.ContextWrapper;
import prototype.xd.scheduler.utilities.Keys;
import prototype.xd.scheduler.utilities.Utilities;

public class SliderSettingsEntryConfig extends SettingsEntryConfig {
    
    private final int seekMin;
    private final int seekMax;
    private final int stepSize;
    private boolean zeroIsOff;
    private final Keys.DefaultedInteger value;
    @PluralsRes
    @StringRes
    private int stringResource;
    
    @Nullable
    IntFunction<String> textFormatter;
    
    public SliderSettingsEntryConfig(Keys.DefaultedInteger value, int seekMin, int seekMax, int stepSize, boolean zeroIfOff,
                                     @StringRes @PluralsRes int stringResource) {
        this.seekMin = seekMin;
        this.seekMax = seekMax;
        this.stepSize = stepSize;
        this.zeroIsOff = zeroIfOff;
        this.value = value;
        this.stringResource = stringResource;
    }
    
    public SliderSettingsEntryConfig(Keys.DefaultedInteger value, int seekMin, int seekMax, int stepSize,
                                     @NonNull IntFunction<String> textFormatter) {
        this.seekMin = seekMin;
        this.seekMax = seekMax;
        this.stepSize = stepSize;
        this.value = value;
        this.textFormatter = textFormatter;
    }
    
    @Override
    public int getRecyclerViewType() {
        return SLIDER.ordinal();
    }
    
    static class SeekBarViewHolder extends SettingsEntryConfig.SettingsViewHolder<SliderSettingsEntryBinding, SliderSettingsEntryConfig> {
        
        SeekBarViewHolder(@NonNull ContextWrapper wrapper, @NonNull SliderSettingsEntryBinding viewBinding) {
            super(wrapper, viewBinding);
        }
        
        // bind from anywhere
        static void bindExternal(SliderSettingsEntryConfig config, Slider slider, TextView sliderDescription) {
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
        void bind(SliderSettingsEntryConfig config) {
            bindExternal(config, viewBinding.slider, viewBinding.sliderDescription);
        }
    }
}


