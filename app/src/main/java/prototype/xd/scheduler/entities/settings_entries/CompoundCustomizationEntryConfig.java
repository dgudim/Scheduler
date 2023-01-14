package prototype.xd.scheduler.entities.settings_entries;

import static prototype.xd.scheduler.entities.settings_entries.SettingsEntryType.COMPOUND_CUSTOMIZATION;

import android.view.LayoutInflater;
import android.widget.LinearLayout;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AlertDialog;

import com.google.android.material.dialog.MaterialAlertDialogBuilder;

import prototype.xd.scheduler.R;
import prototype.xd.scheduler.databinding.CompoundCustomizationSettingsEntryBinding;
import prototype.xd.scheduler.databinding.TodoItemViewSelectionDialogBinding;
import prototype.xd.scheduler.utilities.ContextWrapper;
import prototype.xd.scheduler.utilities.DialogUtilities;
import prototype.xd.scheduler.utilities.Keys;
import prototype.xd.scheduler.utilities.Utilities;
import prototype.xd.scheduler.views.lockscreen.LockScreenTodoItemView;
import prototype.xd.scheduler.views.lockscreen.LockScreenTodoItemView.TodoItemViewType;
import prototype.xd.scheduler.views.settings.EntryPreviewContainer;

public class CompoundCustomizationEntryConfig extends SettingsEntryConfig {
    
    @Override
    public int getType() {
        return COMPOUND_CUSTOMIZATION.ordinal();
    }
    
    static class CompoundCustomizationViewHolder extends SettingsViewHolder<CompoundCustomizationSettingsEntryBinding, CompoundCustomizationEntryConfig> {
        
        private final EntryPreviewContainer entryPreviewContainer;
        private final AlertDialog viewSelectionDialog;
        
        CompoundCustomizationViewHolder(@NonNull final ContextWrapper wrapper,
                                        @NonNull final CompoundCustomizationSettingsEntryBinding viewBinding) {
            super(wrapper, viewBinding);
            
            LayoutInflater layoutInflater = wrapper.getLayoutInflater();
            LinearLayout viewSelectionDialogView = TodoItemViewSelectionDialogBinding.inflate(layoutInflater).getRoot();
            
            viewSelectionDialog = wrapper.attachDialogToLifecycle(
                    new MaterialAlertDialogBuilder(wrapper.context)
                            .setView(viewSelectionDialogView)
                            .create(), null);
            
            entryPreviewContainer = new EntryPreviewContainer(wrapper, viewBinding.previewContainer, true) {
                @Override
                protected int currentFontColorGetter() {
                    return Keys.FONT_COLOR.CURRENT.get();
                }
                
                @Override
                protected int currentBgColorGetter() {
                    return Keys.BG_COLOR.CURRENT.get();
                }
                
                @Override
                protected int currentBorderColorGetter() {
                    return Keys.BORDER_COLOR.CURRENT.get();
                }
                
                @Override
                protected int currentBorderThicknessGetter() {
                    return Keys.BORDER_THICKNESS.CURRENT.get();
                }
                
                @Override
                protected int adaptiveColorBalanceGetter() {
                    return Keys.ADAPTIVE_COLOR_BALANCE.get();
                }
            };
            
            entryPreviewContainer.attachUpcomingSelectors(
                    viewBinding.upcomingFontColorSelector,
                    viewBinding.upcomingBorderColorSelector,
                    viewBinding.upcomingBackgroundColorSelector);
            
            entryPreviewContainer.attachCurrentSelectors(
                    viewBinding.currentFontColorSelector,
                    viewBinding.currentBorderColorSelector,
                    viewBinding.currentBackgroundColorSelector);
            
            entryPreviewContainer.attachExpiredSelectors(
                    viewBinding.expiredFontColorSelector,
                    viewBinding.expiredBorderColorSelector,
                    viewBinding.expiredBackgroundColorSelector);
            
            // no need to reinflate, view type is already set
            entryPreviewContainer.refreshAll(false);
            
            for (TodoItemViewType viewType : TodoItemViewType.values()) {
                LockScreenTodoItemView.inflateViewByType(viewType, viewSelectionDialogView, layoutInflater)
                        .setOnClickListener(v -> {
                            entryPreviewContainer.setTodoItemViewType(viewType);
                            viewSelectionDialog.dismiss();
                        }).addToContainer(viewSelectionDialogView);
            }
            
            viewBinding.previewContainer.setOnClickListener(v ->
                    viewSelectionDialog.show());
            
            DialogUtilities.ColorPickerColorSelectionListener colorPickerColorSelectedListener = (value, selectedColor) -> {
                value.put(selectedColor);
                entryPreviewContainer.notifyColorChanged(value, selectedColor);
            };
            
            viewBinding.currentBackgroundColorSelector.setOnClickListener(v ->
                    DialogUtilities.displayColorPicker(wrapper,
                            colorPickerColorSelectedListener,
                            Keys.BG_COLOR.CURRENT));
            viewBinding.upcomingBackgroundColorSelector.setOnClickListener(v ->
                    DialogUtilities.displayColorPicker(wrapper,
                            colorPickerColorSelectedListener,
                            Keys.BG_COLOR.UPCOMING));
            viewBinding.expiredBackgroundColorSelector.setOnClickListener(v ->
                    DialogUtilities.displayColorPicker(wrapper,
                            colorPickerColorSelectedListener,
                            Keys.BG_COLOR.EXPIRED));
            
            viewBinding.currentFontColorSelector.setOnClickListener(v ->
                    DialogUtilities.displayColorPicker(wrapper,
                            colorPickerColorSelectedListener,
                            Keys.FONT_COLOR.CURRENT));
            viewBinding.upcomingFontColorSelector.setOnClickListener(v ->
                    DialogUtilities.displayColorPicker(wrapper,
                            colorPickerColorSelectedListener,
                            Keys.FONT_COLOR.UPCOMING));
            viewBinding.expiredFontColorSelector.setOnClickListener(v ->
                    DialogUtilities.displayColorPicker(wrapper,
                            colorPickerColorSelectedListener,
                            Keys.FONT_COLOR.EXPIRED));
            
            viewBinding.currentBorderColorSelector.setOnClickListener(v ->
                    DialogUtilities.displayColorPicker(wrapper,
                            colorPickerColorSelectedListener,
                            Keys.BORDER_COLOR.CURRENT));
            viewBinding.upcomingBorderColorSelector.setOnClickListener(v ->
                    DialogUtilities.displayColorPicker(wrapper,
                            colorPickerColorSelectedListener,
                            Keys.BORDER_COLOR.UPCOMING));
            viewBinding.expiredBorderColorSelector.setOnClickListener(v ->
                    DialogUtilities.displayColorPicker(wrapper,
                            colorPickerColorSelectedListener,
                            Keys.BORDER_COLOR.EXPIRED));
            
            Utilities.setSliderChangeListener(
                    viewBinding.adaptiveColorBalanceDescription,
                    viewBinding.adaptiveColorBalanceSlider,
                    (slider, sliderValue, fromUser, value) -> entryPreviewContainer.setPreviewAdaptiveColorBalance(sliderValue),
                    R.string.settings_adaptive_color_balance,
                    Keys.ADAPTIVE_COLOR_BALANCE, true);
            
            Utilities.setSliderChangeListener(
                    viewBinding.upcomingBorderThicknessDescription,
                    viewBinding.upcomingBorderThicknessSlider,
                    (slider, sliderValue, fromUser, value) -> entryPreviewContainer.setUpcomingPreviewBorderThickness(sliderValue),
                    R.string.settings_upcoming_border_thickness,
                    Keys.BORDER_THICKNESS.UPCOMING, true);
            
            Utilities.setSliderChangeListener(
                    viewBinding.currentBorderThicknessDescription,
                    viewBinding.currentBorderThicknessSlider,
                    (slider, sliderValue, fromUser, value) -> entryPreviewContainer.setCurrentPreviewBorderThickness(sliderValue),
                    R.string.settings_current_border_thickness,
                    Keys.BORDER_THICKNESS.CURRENT, true);
            
            Utilities.setSliderChangeListener(
                    viewBinding.expiredBorderThicknessDescription,
                    viewBinding.expiredBorderThicknessSlider,
                    (slider, sliderValue, fromUser, value) -> entryPreviewContainer.setExpiredPreviewBorderThickness(sliderValue),
                    R.string.settings_expired_border_thickness,
                    Keys.BORDER_THICKNESS.EXPIRED, true);
            
            Utilities.setSliderChangeListener(
                    viewBinding.fontSizeDescription,
                    viewBinding.fontSizeSlider,
                    (slider, sliderValue, fromUser, value) -> entryPreviewContainer.setPreviewFontSize(sliderValue), R.string.settings_font_size,
                    Keys.FONT_SIZE, false);
            
        }
        
        @Override
        void bind(CompoundCustomizationEntryConfig config) {
            // nothing special required, this entry should be the only one of it's kind
        }
    }
}

