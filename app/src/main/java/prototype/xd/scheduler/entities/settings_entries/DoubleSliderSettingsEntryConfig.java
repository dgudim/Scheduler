package prototype.xd.scheduler.entities.settings_entries;

import static prototype.xd.scheduler.entities.settings_entries.SettingsEntryType.DOUBLE_SLIDER;

import android.content.Context;

import androidx.annotation.ColorInt;
import androidx.annotation.NonNull;
import androidx.annotation.StringRes;

import prototype.xd.scheduler.databinding.DoubleSliderSettingsEntryBinding;
import prototype.xd.scheduler.utilities.misc.ContextWrapper;
import prototype.xd.scheduler.utilities.ColorUtilities;

public class DoubleSliderSettingsEntryConfig extends SettingsEntryConfig {
    
    @StringRes
    final int titleId;
    
    @NonNull
    protected final SliderSettingsEntryConfig leftSliderConfig;
    @NonNull
    protected final ColorUtilities.SliderTinter leftSliderTinter;
    
    @NonNull
    protected final SliderSettingsEntryConfig rightSliderConfig;
    @NonNull
    protected final ColorUtilities.SliderTinter rightSliderTinter;
    
    public DoubleSliderSettingsEntryConfig(@NonNull Context context,
                                           @StringRes int titleId,
                                           @NonNull final SliderSettingsEntryConfig leftSliderConfig,
                                           @ColorInt int leftSliderAccentColor,
                                           @NonNull final SliderSettingsEntryConfig rightSliderConfig,
                                           @ColorInt int rightSliderAccentColor) {
        this.titleId = titleId;
        this.leftSliderConfig = leftSliderConfig;
        this.rightSliderConfig = rightSliderConfig;
        
        leftSliderTinter = new ColorUtilities.SliderTinter(context, leftSliderAccentColor);
        rightSliderTinter = new ColorUtilities.SliderTinter(context, rightSliderAccentColor);
    }
    
    @Override
    public int getRecyclerViewType() {
        return DOUBLE_SLIDER.ordinal();
    }
    
    static class DoubleSeekBarViewHolder extends SettingsEntryConfig.SettingsViewHolder<DoubleSliderSettingsEntryBinding, DoubleSliderSettingsEntryConfig> {
        
        DoubleSeekBarViewHolder(@NonNull ContextWrapper wrapper, @NonNull DoubleSliderSettingsEntryBinding viewBinding) {
            super(wrapper, viewBinding);
        }
        
        @Override
        void bind(@NonNull DoubleSliderSettingsEntryConfig config) {
            
            viewBinding.title.setText(config.titleId);
            
            config.leftSliderTinter.tintSlider(viewBinding.leftSlider);
            config.rightSliderTinter.tintSlider(viewBinding.rightSlider);
            
            SliderSettingsEntryConfig.SeekBarViewHolder.bindExternal(config.leftSliderConfig, viewBinding.leftSlider, viewBinding.leftSubTitle);
            SliderSettingsEntryConfig.SeekBarViewHolder.bindExternal(config.rightSliderConfig, viewBinding.rightSlider, viewBinding.rightSubTitle);
        }
    }
    
}
