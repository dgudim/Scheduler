package prototype.xd.scheduler.entities.settings_entries;

import static prototype.xd.scheduler.entities.settings_entries.SettingsEntryType.COMPOUND_CUSTOMIZATION;

import android.view.LayoutInflater;
import android.view.View;
import android.widget.LinearLayout;

import androidx.appcompat.app.AlertDialog;
import androidx.lifecycle.Lifecycle;

import com.google.android.material.dialog.MaterialAlertDialogBuilder;

import prototype.xd.scheduler.R;
import prototype.xd.scheduler.databinding.CompoundCustomizationSettingsEntryBinding;
import prototype.xd.scheduler.databinding.TodoItemViewSelectionDialogBinding;
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
        
        CompoundCustomizationViewHolder(CompoundCustomizationSettingsEntryBinding viewBinding, Lifecycle lifecycle) {
            super(viewBinding);
            
            LayoutInflater layoutInflater = LayoutInflater.from(context);
            LinearLayout viewSelectionDialogView = TodoItemViewSelectionDialogBinding.inflate(layoutInflater).getRoot();
            
            viewSelectionDialog = new MaterialAlertDialogBuilder(context)
                    .setView(viewSelectionDialogView)
                    .create();
            
            entryPreviewContainer = new EntryPreviewContainer(context, viewBinding.previewContainer, true) {
                @Override
                protected int currentFontColorGetter() {
                    return Keys.FONT_COLOR.get();
                }
                
                @Override
                protected int currentBgColorGetter() {
                    return Keys.BG_COLOR.get();
                }
                
                @Override
                protected int currentBorderColorGetter() {
                    return Keys.BORDER_COLOR.get();
                }
                
                @Override
                protected int currentBorderThicknessGetter() {
                    return Keys.BORDER_THICKNESS.get();
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
            
            entryPreviewContainer.refreshAll();
            
            for (TodoItemViewType viewType : TodoItemViewType.values()) {
                View view = LockScreenTodoItemView.inflateViewByType(viewType, viewSelectionDialogView, layoutInflater).getRoot();
                view.setOnClickListener(v -> {
                    entryPreviewContainer.setTodoItemViewType(viewType);
                    viewSelectionDialog.dismiss();
                });
                viewSelectionDialogView.addView(view);
            }
            
            viewBinding.previewContainer.setOnClickListener(v ->
                    viewSelectionDialog.show());
            
            DialogUtilities.ColorPickerColorSelectionListener colorPickerColorSelectedListener = (value, selectedColor) -> {
                value.put(selectedColor);
                entryPreviewContainer.notifyColorChanged(value, selectedColor, true);
            };
            
            viewBinding.currentBackgroundColorSelector.setOnClickListener(v ->
                    DialogUtilities.invokeColorDialog(context, lifecycle,
                            colorPickerColorSelectedListener,
                            Keys.BG_COLOR));
            viewBinding.upcomingBackgroundColorSelector.setOnClickListener(v ->
                    DialogUtilities.invokeColorDialog(context, lifecycle,
                            colorPickerColorSelectedListener,
                            Keys.UPCOMING_BG_COLOR));
            viewBinding.expiredBackgroundColorSelector.setOnClickListener(v ->
                    DialogUtilities.invokeColorDialog(context, lifecycle,
                            colorPickerColorSelectedListener,
                            Keys.EXPIRED_BG_COLOR));
            
            viewBinding.currentFontColorSelector.setOnClickListener(v ->
                    DialogUtilities.invokeColorDialog(context, lifecycle,
                            colorPickerColorSelectedListener,
                            Keys.FONT_COLOR));
            viewBinding.upcomingFontColorSelector.setOnClickListener(v ->
                    DialogUtilities.invokeColorDialog(context, lifecycle,
                            colorPickerColorSelectedListener,
                            Keys.UPCOMING_FONT_COLOR));
            viewBinding.expiredFontColorSelector.setOnClickListener(v ->
                    DialogUtilities.invokeColorDialog(context, lifecycle,
                            colorPickerColorSelectedListener,
                            Keys.EXPIRED_FONT_COLOR));
            
            viewBinding.currentBorderColorSelector.setOnClickListener(v ->
                    DialogUtilities.invokeColorDialog(context, lifecycle,
                            colorPickerColorSelectedListener,
                            Keys.BORDER_COLOR));
            viewBinding.upcomingBorderColorSelector.setOnClickListener(v ->
                    DialogUtilities.invokeColorDialog(context, lifecycle,
                            colorPickerColorSelectedListener,
                            Keys.UPCOMING_BORDER_COLOR));
            viewBinding.expiredBorderColorSelector.setOnClickListener(v ->
                    DialogUtilities.invokeColorDialog(context, lifecycle,
                            colorPickerColorSelectedListener,
                            Keys.EXPIRED_BORDER_COLOR));
            
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
                    Keys.UPCOMING_BORDER_THICKNESS, true);
            
            Utilities.setSliderChangeListener(
                    viewBinding.currentBorderThicknessDescription,
                    viewBinding.currentBorderThicknessSlider,
                    (slider, sliderValue, fromUser, value) -> entryPreviewContainer.setCurrentPreviewBorderThickness(sliderValue),
                    R.string.settings_current_border_thickness,
                    Keys.BORDER_THICKNESS, true);
            
            Utilities.setSliderChangeListener(
                    viewBinding.expiredBorderThicknessDescription,
                    viewBinding.expiredBorderThicknessSlider,
                    (slider, sliderValue, fromUser, value) -> entryPreviewContainer.setExpiredPreviewBorderThickness(sliderValue),
                    R.string.settings_expired_border_thickness,
                    Keys.EXPIRED_BORDER_THICKNESS, true);
            
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

