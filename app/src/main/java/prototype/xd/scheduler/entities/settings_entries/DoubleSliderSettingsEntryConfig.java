package prototype.xd.scheduler.entities.settings_entries;

import static prototype.xd.scheduler.entities.settings_entries.SettingsEntryType.DOUBLE_SLIDER;

import android.content.Context;

import androidx.annotation.ColorInt;
import androidx.annotation.NonNull;
import androidx.annotation.StringRes;

import prototype.xd.scheduler.databinding.DoubleSliderSettingsEntryBinding;
import prototype.xd.scheduler.utilities.ImageUtilities;
import prototype.xd.scheduler.utilities.misc.ContextWrapper;

public class DoubleSliderSettingsEntryConfig extends SettingsEntryConfig {
    
    @StringRes
    private final int titleId;
    
    @NonNull
    protected final SliderSettingsEntryConfig leftSliderConfig;
    @NonNull
    protected final ImageUtilities.SliderTinter leftSliderTinter;
    
    @NonNull
    protected final SliderSettingsEntryConfig rightSliderConfig;
    @NonNull
    protected final ImageUtilities.SliderTinter rightSliderTinter;
    
    public DoubleSliderSettingsEntryConfig(@NonNull Context context,
                                           @StringRes int titleId,
                                           @NonNull final SliderSettingsEntryConfig leftSliderConfig,
                                           @ColorInt int leftSliderAccentColor,
                                           @NonNull final SliderSettingsEntryConfig rightSliderConfig,
                                           @ColorInt int rightSliderAccentColor) {
        this.titleId = titleId;
        this.leftSliderConfig = leftSliderConfig;
        this.rightSliderConfig = rightSliderConfig;
        
        leftSliderTinter = new ImageUtilities.SliderTinter(context, leftSliderAccentColor);
        rightSliderTinter = new ImageUtilities.SliderTinter(context, rightSliderAccentColor);
    }
    
    @Override
    public int getRecyclerViewType() {
        return DOUBLE_SLIDER.ordinal();
    }
    
    static class ViewHolder extends SettingsEntryConfig.SettingsViewHolder<DoubleSliderSettingsEntryBinding, DoubleSliderSettingsEntryConfig> {
        
        ViewHolder(@NonNull ContextWrapper wrapper, @NonNull DoubleSliderSettingsEntryBinding viewBinding) {
            super(wrapper, viewBinding);
        }
        
        @Override
        void bind(@NonNull DoubleSliderSettingsEntryConfig config) {
            
            binding.title.setText(config.titleId);
            
            config.leftSliderTinter.tintSlider(binding.leftSlider);
            config.rightSliderTinter.tintSlider(binding.rightSlider);
            
            SliderSettingsEntryConfig.ViewHolder.bindExternal(config.leftSliderConfig, binding.leftSlider, binding.leftSubTitle);
            SliderSettingsEntryConfig.ViewHolder.bindExternal(config.rightSliderConfig, binding.rightSlider, binding.rightSubTitle);
        }
    }
    
}
