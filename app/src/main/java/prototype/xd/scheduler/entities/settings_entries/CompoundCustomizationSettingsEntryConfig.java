package prototype.xd.scheduler.entities.settings_entries;

import static android.widget.LinearLayout.LayoutParams.MATCH_PARENT;
import static android.widget.LinearLayout.LayoutParams.WRAP_CONTENT;
import static prototype.xd.scheduler.entities.settings_entries.SettingsEntryType.COMPOUND_CUSTOMIZATION;

import android.graphics.Color;
import android.os.Build;
import android.widget.LinearLayout;

import androidx.annotation.ColorInt;
import androidx.annotation.IntRange;
import androidx.annotation.NonNull;
import androidx.appcompat.app.AlertDialog;

import com.google.android.material.color.MaterialColors;
import com.google.android.material.dialog.MaterialAlertDialogBuilder;

import java.util.function.ObjIntConsumer;

import prototype.xd.scheduler.R;
import prototype.xd.scheduler.databinding.CompoundCustomizationSettingsEntryBinding;
import prototype.xd.scheduler.databinding.EntryEffectsDialogBinding;
import prototype.xd.scheduler.utilities.DialogUtilities;
import prototype.xd.scheduler.utilities.Static;
import prototype.xd.scheduler.utilities.Utilities;
import prototype.xd.scheduler.utilities.misc.ContextWrapper;
import prototype.xd.scheduler.views.lockscreen.LockScreenTodoItemView;
import prototype.xd.scheduler.views.lockscreen.LockScreenTodoItemView.TodoItemViewType;

public class CompoundCustomizationSettingsEntryConfig extends SettingsEntryConfig {
    
    @Override
    public int getRecyclerViewType() {
        return COMPOUND_CUSTOMIZATION.ordinal();
    }
    
    static class ViewHolder
            extends SettingsEntryConfig.SettingsViewHolder<CompoundCustomizationSettingsEntryBinding, CompoundCustomizationSettingsEntryConfig> {
        
        @NonNull
        private final EntryPreviewContainer entryPreviewContainer;
        @NonNull
        private final AlertDialog viewSelectionDialog;
        
        @NonNull
        private final AlertDialog effectSettingsDialog;
        
        ViewHolder(@NonNull final ContextWrapper wrapper,
                   @NonNull final CompoundCustomizationSettingsEntryBinding viewBinding) {
            super(wrapper, viewBinding);
            
            int padding = wrapper.getDimensionPixelSize(R.dimen.dialog_menu_padding_left_right);
            int itemPadding = wrapper.getDimensionPixelSize(R.dimen.lockscreen_item_vertical_padding);
            
            LinearLayout viewSelectionDialogView = new LinearLayout(wrapper.context);
            viewSelectionDialogView.setLayoutParams(new LinearLayout.LayoutParams(MATCH_PARENT, WRAP_CONTENT));
            viewSelectionDialogView.setOrientation(LinearLayout.VERTICAL);
            viewSelectionDialogView.setPadding(padding, padding, padding, padding - itemPadding);
            
            EntryEffectsDialogBinding effectsDialogBinding = EntryEffectsDialogBinding.inflate(wrapper.getLayoutInflater());
            
            Utilities.setSliderChangeListener(
                    effectsDialogBinding.effectTransparencyDescription,
                    effectsDialogBinding.effectTransparencySlider,
                    null,
                    R.string.settings_effects_transparency,
                    Static.EFFECT_TRANSPARENCY,
                    false);
            
            Utilities.setSliderChangeListener(
                    effectsDialogBinding.effectBlurRadiusDescription,
                    effectsDialogBinding.effectBlurRadiusSlider,
                    null,
                    R.string.settings_effects_blur_radius,
                    Static.EFFECT_BLUR_RADIUS,
                    false);
            
            Utilities.setSliderChangeListener(
                    effectsDialogBinding.effectBlurGrainDescription,
                    effectsDialogBinding.effectBlurGrainSlider,
                    null,
                    R.string.settings_effects_blur_grain,
                    Static.EFFECT_BLUR_GRAIN,
                    false);
            
            if (Build.VERSION.SDK_INT >= 31) {
                Utilities.setSwitchChangeListener(effectsDialogBinding.glowSwitch, Static.EFFECT_GLOW, null);
                Utilities.setSwitchChangeListener(effectsDialogBinding.highlightEdgesSwitch, Static.EFFECT_HIGHLIGHT_EDGE, null);
            } else {
                effectsDialogBinding.glowSwitch.freezeState(false);
                effectsDialogBinding.highlightEdgesSwitch.freezeState(false);
            }
            
            
            viewSelectionDialog = wrapper.attachDialogToLifecycle(
                    new MaterialAlertDialogBuilder(wrapper.context, R.style.DefaultAlertDialogTheme)
                            .setIcon(R.drawable.ic_view_carousel_24)
                            .setTitle(R.string.select_view)
                            .setMessage(R.string.select_view_description)
                            .setView(viewSelectionDialogView)
                            .create(), null);
            
            effectSettingsDialog = wrapper.attachDialogToLifecycle(
                    new MaterialAlertDialogBuilder(wrapper.context, R.style.DefaultAlertDialogTheme)
                            .setIcon(R.drawable.ic_effects_45)
                            .setTitle(R.string.settings_effects_title)
                            .setMessage(R.string.settings_effects_description)
                            .setView(effectsDialogBinding.getRoot())
                            .create(), null);
            
            entryPreviewContainer = new EntryPreviewContainer(wrapper, binding.previewContainer, true) {
                @ColorInt
                @Override
                protected int currentFontColorGetter() {
                    return Static.FONT_COLOR.CURRENT.get();
                }
                
                @ColorInt
                @Override
                protected int currentBgColorGetter() {
                    return Static.BG_COLOR.CURRENT.get();
                }
                
                @ColorInt
                @Override
                protected int currentBorderColorGetter() {
                    return Static.BORDER_COLOR.CURRENT.get();
                }
                
                @Override
                protected int currentBorderThicknessGetter() {
                    return Static.BORDER_THICKNESS.CURRENT.get();
                }
                
                @IntRange(from = 0, to = 10)
                @Override
                protected int adaptiveColorBalanceGetter() {
                    return Static.ADAPTIVE_COLOR_BALANCE.get();
                }
            };
            
            entryPreviewContainer.attachUpcomingSelectors(
                    binding.upcomingFontColorSelector,
                    binding.upcomingBorderColorSelector,
                    binding.upcomingBackgroundColorSelector);
            
            entryPreviewContainer.attachCurrentSelectors(
                    binding.currentFontColorSelector,
                    binding.currentBorderColorSelector,
                    binding.currentBackgroundColorSelector);
            
            entryPreviewContainer.attachExpiredSelectors(
                    binding.expiredFontColorSelector,
                    binding.expiredBorderColorSelector,
                    binding.expiredBackgroundColorSelector);
            
            // no need to reinflate, view type is already set
            entryPreviewContainer.refreshAll(false);
            
            int bgColor = MaterialColors.getColor(wrapper.context, R.attr.colorSurfaceContainerHighest, Color.WHITE);
            int borderColor = MaterialColors.getColor(wrapper.context, R.attr.colorAccent, Color.GRAY);
            int fontColor = MaterialColors.getColor(wrapper.context, R.attr.colorOnSurfaceVariant, Color.BLACK);
            
            for (TodoItemViewType viewType : TodoItemViewType.values()) {
                LockScreenTodoItemView.inflateViewByType(viewType, viewSelectionDialogView, wrapper.getLayoutInflater())
                        .mixAndSetBgAndTextColors(fontColor, bgColor)
                        .setBorderColor(borderColor)
                        .setOnClickListener(v -> {
                            entryPreviewContainer.setTodoItemViewType(viewType);
                            viewSelectionDialog.dismiss();
                        }).addToContainer(viewSelectionDialogView);
            }
            
            binding.previewContainer.setOnClickListener(v ->
                    viewSelectionDialog.show());
            
            binding.openEffectsButton.setOnClickListener(v ->
                    effectSettingsDialog.show());
            
            ObjIntConsumer<Static.DefaultedInteger> colorPickerColorSelectedListener = (value, selectedColor) -> {
                value.put(selectedColor);
                entryPreviewContainer.notifyColorChanged(value, selectedColor);
            };
            
            binding.currentBackgroundColorSelector.setOnClickListener(v ->
                    DialogUtilities.displayColorPicker(wrapper,
                            colorPickerColorSelectedListener,
                            Static.BG_COLOR.CURRENT));
            binding.upcomingBackgroundColorSelector.setOnClickListener(v ->
                    DialogUtilities.displayColorPicker(wrapper,
                            colorPickerColorSelectedListener,
                            Static.BG_COLOR.UPCOMING));
            binding.expiredBackgroundColorSelector.setOnClickListener(v ->
                    DialogUtilities.displayColorPicker(wrapper,
                            colorPickerColorSelectedListener,
                            Static.BG_COLOR.EXPIRED));
            
            binding.currentFontColorSelector.setOnClickListener(v ->
                    DialogUtilities.displayColorPicker(wrapper,
                            colorPickerColorSelectedListener,
                            Static.FONT_COLOR.CURRENT));
            binding.upcomingFontColorSelector.setOnClickListener(v ->
                    DialogUtilities.displayColorPicker(wrapper,
                            colorPickerColorSelectedListener,
                            Static.FONT_COLOR.UPCOMING));
            binding.expiredFontColorSelector.setOnClickListener(v ->
                    DialogUtilities.displayColorPicker(wrapper,
                            colorPickerColorSelectedListener,
                            Static.FONT_COLOR.EXPIRED));
            
            binding.currentBorderColorSelector.setOnClickListener(v ->
                    DialogUtilities.displayColorPicker(wrapper,
                            colorPickerColorSelectedListener,
                            Static.BORDER_COLOR.CURRENT));
            binding.upcomingBorderColorSelector.setOnClickListener(v ->
                    DialogUtilities.displayColorPicker(wrapper,
                            colorPickerColorSelectedListener,
                            Static.BORDER_COLOR.UPCOMING));
            binding.expiredBorderColorSelector.setOnClickListener(v ->
                    DialogUtilities.displayColorPicker(wrapper,
                            colorPickerColorSelectedListener,
                            Static.BORDER_COLOR.EXPIRED));
            
            Utilities.setSliderChangeListener(
                    binding.adaptiveColorBalanceDescription,
                    binding.adaptiveColorBalanceSlider,
                    (slider, sliderValue, fromUser, value) -> entryPreviewContainer.setPreviewAdaptiveColorBalance(sliderValue),
                    R.string.settings_adaptive_color_balance,
                    Static.ADAPTIVE_COLOR_BALANCE, true);
            
            Utilities.setSliderChangeListener(
                    binding.upcomingBorderThicknessDescription,
                    binding.upcomingBorderThicknessSlider,
                    (slider, sliderValue, fromUser, value) -> entryPreviewContainer.setUpcomingPreviewBorderThickness(sliderValue),
                    R.string.settings_upcoming_border_thickness,
                    Static.BORDER_THICKNESS.UPCOMING, true);
            
            Utilities.setSliderChangeListener(
                    binding.currentBorderThicknessDescription,
                    binding.currentBorderThicknessSlider,
                    (slider, sliderValue, fromUser, value) -> entryPreviewContainer.setCurrentPreviewBorderThickness(sliderValue),
                    R.string.settings_current_border_thickness,
                    Static.BORDER_THICKNESS.CURRENT, true);
            
            Utilities.setSliderChangeListener(
                    binding.expiredBorderThicknessDescription,
                    binding.expiredBorderThicknessSlider,
                    (slider, sliderValue, fromUser, value) -> entryPreviewContainer.setExpiredPreviewBorderThickness(sliderValue),
                    R.string.settings_expired_border_thickness,
                    Static.BORDER_THICKNESS.EXPIRED, true);
            
            Utilities.setSliderChangeListener(
                    binding.fontSizeDescription,
                    binding.fontSizeSlider,
                    (slider, sliderValue, fromUser, value) -> entryPreviewContainer.setPreviewFontSize(sliderValue), R.string.settings_font_size,
                    Static.FONT_SIZE, false);
            
        }
        
        @Override
        void bind(CompoundCustomizationSettingsEntryConfig config) {
            // nothing special required, this entry should be the only one of it's kind
        }
    }
}

