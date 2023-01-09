package prototype.xd.scheduler.entities.settings_entries;

import static prototype.xd.scheduler.entities.settings_entries.SettingsEntryType.DOUBLE_SLIDER;

import android.content.Context;

import androidx.annotation.ColorInt;
import androidx.annotation.NonNull;
import androidx.annotation.StringRes;

import prototype.xd.scheduler.databinding.DoubleSliderSettingsEntryBinding;
import prototype.xd.scheduler.utilities.GraphicsUtilities;

public class DoubleSliderSettingsEntryConfig extends SettingsEntryConfig {
    
    @StringRes
    int titleId;
    
    SliderSettingsEntryConfig leftSliderConfig;
    GraphicsUtilities.SliderTinter leftSliderTinter;
    
    SliderSettingsEntryConfig rightSliderConfig;
    GraphicsUtilities.SliderTinter rightSliderTinter;
    
    public DoubleSliderSettingsEntryConfig(@NonNull Context context,
                                           @StringRes int titleId,
                                           SliderSettingsEntryConfig leftSliderConfig,
                                           @ColorInt int leftSliderAccentColor,
                                           SliderSettingsEntryConfig rightSliderConfig,
                                           @ColorInt int rightSliderAccentColor) {
        this.titleId = titleId;
        this.leftSliderConfig = leftSliderConfig;
        this.rightSliderConfig = rightSliderConfig;
    
        leftSliderTinter = new GraphicsUtilities.SliderTinter(context, leftSliderAccentColor);
        rightSliderTinter = new GraphicsUtilities.SliderTinter(context, rightSliderAccentColor);
    }
    
    @Override
    public int getType() {
        return DOUBLE_SLIDER.ordinal();
    }
    
    static class DoubleSeekBarViewHolder extends SettingsEntryConfig.SettingsViewHolder<DoubleSliderSettingsEntryBinding, DoubleSliderSettingsEntryConfig> {
        
        DoubleSeekBarViewHolder(DoubleSliderSettingsEntryBinding viewBinding) {
            super(viewBinding);
        }
        
        @Override
        void bind(DoubleSliderSettingsEntryConfig config) {
            
            viewBinding.title.setText(config.titleId);
    
            config.leftSliderTinter.tintSlider(viewBinding.leftSlider);
            config.rightSliderTinter.tintSlider(viewBinding.rightSlider);
            
            SliderSettingsEntryConfig.SeekBarViewHolder.bindExternal(config.leftSliderConfig, viewBinding.leftSlider, viewBinding.leftSubTitle);
            SliderSettingsEntryConfig.SeekBarViewHolder.bindExternal(config.rightSliderConfig, viewBinding.rightSlider, viewBinding.rightSubTitle);
        }
    }
    
}
