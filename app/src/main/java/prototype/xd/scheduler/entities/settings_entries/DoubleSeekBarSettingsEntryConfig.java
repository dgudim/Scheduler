package prototype.xd.scheduler.entities.settings_entries;

import static prototype.xd.scheduler.entities.settings_entries.SettingsEntryType.DOUBLE_SEEK_BAR;

import android.content.Context;

import androidx.annotation.ColorInt;
import androidx.annotation.NonNull;
import androidx.annotation.StringRes;

import prototype.xd.scheduler.databinding.DoubleSeekbarSettingsEntryBinding;
import prototype.xd.scheduler.utilities.GraphicsUtilities;

public class DoubleSeekBarSettingsEntryConfig extends SettingsEntryConfig {
    
    @StringRes
    int titleId;
    
    SeekBarSettingsEntryConfig leftSliderConfig;
    GraphicsUtilities.SliderTinter leftSliderTinter;
    
    SeekBarSettingsEntryConfig rightSliderConfig;
    GraphicsUtilities.SliderTinter rightSliderTinter;
    
    public DoubleSeekBarSettingsEntryConfig(@NonNull Context context,
                                            @StringRes int titleId,
                                            SeekBarSettingsEntryConfig leftSliderConfig,
                                            @ColorInt int leftSliderAccentColor,
                                            SeekBarSettingsEntryConfig rightSliderConfig,
                                            @ColorInt int rightSliderAccentColor) {
        this.titleId = titleId;
        this.leftSliderConfig = leftSliderConfig;
        this.rightSliderConfig = rightSliderConfig;
    
        leftSliderTinter = new GraphicsUtilities.SliderTinter(context, leftSliderAccentColor);
        rightSliderTinter = new GraphicsUtilities.SliderTinter(context, rightSliderAccentColor);
    }
    
    @Override
    public int getType() {
        return DOUBLE_SEEK_BAR.ordinal();
    }
    
    static class DoubleSeekBarViewHolder extends SettingsEntryConfig.SettingsViewHolder<DoubleSeekbarSettingsEntryBinding, DoubleSeekBarSettingsEntryConfig> {
        
        DoubleSeekBarViewHolder(DoubleSeekbarSettingsEntryBinding viewBinding) {
            super(viewBinding);
        }
        
        @Override
        void bind(DoubleSeekBarSettingsEntryConfig config) {
            
            viewBinding.title.setText(config.titleId);
    
            config.leftSliderTinter.tintSlider(viewBinding.leftSlider);
            config.rightSliderTinter.tintSlider(viewBinding.rightSlider);
            
            SeekBarSettingsEntryConfig.SeekBarViewHolder.bindExternal(config.leftSliderConfig, viewBinding.leftSlider, viewBinding.leftSubTitle);
            SeekBarSettingsEntryConfig.SeekBarViewHolder.bindExternal(config.rightSliderConfig, viewBinding.rightSlider, viewBinding.rightSubTitle);
        }
    }
    
}
