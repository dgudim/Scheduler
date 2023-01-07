package prototype.xd.scheduler.entities.settings_entries;

import static prototype.xd.scheduler.entities.settings_entries.SettingsEntryType.COMPOUND_CUSTOMIZATION;
import static prototype.xd.scheduler.utilities.GraphicsUtilities.getExpiredUpcomingColor;
import static prototype.xd.scheduler.utilities.Keys.BORDER_THICKNESS;
import static prototype.xd.scheduler.utilities.Keys.EXPIRED_BORDER_THICKNESS;
import static prototype.xd.scheduler.utilities.Keys.TODO_ITEM_VIEW_TYPE;
import static prototype.xd.scheduler.utilities.Keys.UPCOMING_BORDER_THICKNESS;
import static prototype.xd.scheduler.utilities.Keys.setBitmapUpdateFlag;

import android.view.LayoutInflater;
import android.view.View;
import android.widget.LinearLayout;

import androidx.annotation.Nullable;
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

public class CompoundCustomizationEntryConfig extends SettingsEntryConfig {
    
    @Override
    public int getType() {
        return COMPOUND_CUSTOMIZATION.ordinal();
    }
    
    static class CompoundCustomizationViewHolder extends SettingsViewHolder<CompoundCustomizationSettingsEntryBinding, CompoundCustomizationEntryConfig> {
        
        LockScreenTodoItemView<?> todayEntryPreview;
        LockScreenTodoItemView<?> upcomingEntryPreview;
        LockScreenTodoItemView<?> expiredEntryPreview;
        
        @Nullable
        TodoItemViewType prevTodoItemViewType;
        TodoItemViewType todoItemViewType;
        AlertDialog viewSelectionDialog;
        
        CompoundCustomizationViewHolder(CompoundCustomizationSettingsEntryBinding viewBinding, Lifecycle lifecycle) {
            super(viewBinding);
            
            todoItemViewType = TodoItemViewType.valueOf(TODO_ITEM_VIEW_TYPE.get());
            
            LayoutInflater layoutInflater = LayoutInflater.from(context);
            LinearLayout viewSelectionDialogView = TodoItemViewSelectionDialogBinding.inflate(layoutInflater).getRoot();
            
            viewSelectionDialog = new MaterialAlertDialogBuilder(context)
                    .setView(viewSelectionDialogView)
                    .create();
            
            for (TodoItemViewType viewType : TodoItemViewType.values()) {
                View view = LockScreenTodoItemView.inflateViewByType(viewType, viewSelectionDialogView, layoutInflater).getRoot();
                view.setOnClickListener(v -> {
                    todoItemViewType = viewType;
                    TODO_ITEM_VIEW_TYPE.put(todoItemViewType.name());
                    updatePreviews();
                    viewSelectionDialog.dismiss();
                });
                viewSelectionDialogView.addView(view);
            }
            
            viewBinding.previewContainer.setOnClickListener(v ->
                    viewSelectionDialog.show());
            
            updatePreviews();
            
            DialogUtilities.ColorPickerColorSelectionListener colorPickerColorSelectedListener = (value, selectedColor) -> {
                value.put(selectedColor);
                if (value.equals(Keys.UPCOMING_BG_COLOR) ||
                        value.equals(Keys.BG_COLOR) ||
                        value.equals(Keys.EXPIRED_BG_COLOR) ||
                        value.equals(Keys.UPCOMING_FONT_COLOR) ||
                        value.equals(Keys.FONT_COLOR) ||
                        value.equals(Keys.EXPIRED_FONT_COLOR)) {
                    updatePreviewFontsAndBgs();
                } else if (value.equals(Keys.UPCOMING_BORDER_COLOR) ||
                        value.equals(Keys.BORDER_COLOR) ||
                        value.equals(Keys.EXPIRED_BORDER_COLOR)) {
                    updatePreviewBorders();
                }
                setBitmapUpdateFlag();
            };
            
            viewBinding.backgroundColorSelector.setOnClickListener(v ->
                    DialogUtilities.invokeColorDialog(context, lifecycle,
                            colorPickerColorSelectedListener,
                            Keys.BG_COLOR));
            viewBinding.backgroundColorUpcomingSelector.setOnClickListener(v ->
                    DialogUtilities.invokeColorDialog(context, lifecycle,
                            colorPickerColorSelectedListener,
                            Keys.UPCOMING_BG_COLOR));
            viewBinding.backgroundColorExpiredSelector.setOnClickListener(v ->
                    DialogUtilities.invokeColorDialog(context, lifecycle,
                            colorPickerColorSelectedListener,
                            Keys.EXPIRED_BG_COLOR));
            
            viewBinding.fontColorSelector.setOnClickListener(v ->
                    DialogUtilities.invokeColorDialog(context, lifecycle,
                            colorPickerColorSelectedListener,
                            Keys.FONT_COLOR));
            viewBinding.fontColorUpcomingSelector.setOnClickListener(v ->
                    DialogUtilities.invokeColorDialog(context, lifecycle,
                            colorPickerColorSelectedListener,
                            Keys.UPCOMING_FONT_COLOR));
            viewBinding.fontColorExpiredSelector.setOnClickListener(v ->
                    DialogUtilities.invokeColorDialog(context, lifecycle,
                            colorPickerColorSelectedListener,
                            Keys.EXPIRED_FONT_COLOR));
            
            viewBinding.borderColorSelector.setOnClickListener(v ->
                    DialogUtilities.invokeColorDialog(context, lifecycle,
                            colorPickerColorSelectedListener,
                            Keys.BORDER_COLOR));
            viewBinding.borderColorUpcomingSelector.setOnClickListener(v ->
                    DialogUtilities.invokeColorDialog(context, lifecycle,
                            colorPickerColorSelectedListener,
                            Keys.UPCOMING_BORDER_COLOR));
            viewBinding.borderColorExpiredSelector.setOnClickListener(v ->
                    DialogUtilities.invokeColorDialog(context, lifecycle,
                            colorPickerColorSelectedListener,
                            Keys.EXPIRED_BORDER_COLOR));
            
            Utilities.SliderOnChangeKeyedListener onChangeListener = (slider, sliderValue, fromUser, value) -> {
                if (value.equals(UPCOMING_BORDER_THICKNESS)) {
                    updateUpcomingPreviewBorderThickness(sliderValue);
                    return;
                }
                if (value.equals(EXPIRED_BORDER_THICKNESS)) {
                    updateExpiredPreviewBorderThickness(sliderValue);
                    return;
                }
                if (value.equals(BORDER_THICKNESS)) {
                    updateCurrentPreviewBorderThickness(sliderValue);
                }
            };
            
            Utilities.setSliderChangeListener(
                    viewBinding.upcomingBorderThicknessText,
                    viewBinding.upcomingBorderThicknessSeekBar,
                    onChangeListener, R.string.settings_upcoming_border_thickness,
                    Keys.UPCOMING_BORDER_THICKNESS, true);
            
            Utilities.setSliderChangeListener(
                    viewBinding.currentBorderThicknessText,
                    viewBinding.currentBorderThicknessSeekBar,
                    onChangeListener, R.string.settings_current_border_thickness,
                    Keys.BORDER_THICKNESS, true);
            
            Utilities.setSliderChangeListener(
                    viewBinding.expiredBorderThicknessText,
                    viewBinding.expiredBorderThicknessSeekBar,
                    onChangeListener, R.string.settings_expired_border_thickness,
                    Keys.EXPIRED_BORDER_THICKNESS, true);
            
            Utilities.setSliderChangeListener(
                    viewBinding.fontSizeText,
                    viewBinding.fontSizeSeekBar,
                    (slider, sliderValue, fromUser, value) -> updatePreviewFontSize(sliderValue), R.string.settings_font_size,
                    Keys.FONT_SIZE, false);
            
        }
        
        @Override
        void bind(CompoundCustomizationEntryConfig config) {
            // nothing special required, this entry should be the only one of it's kind
        }
        
        protected void inflatePreviews() {
            if (prevTodoItemViewType != todoItemViewType) {
                prevTodoItemViewType = todoItemViewType;
                
                LayoutInflater layoutInflater = LayoutInflater.from(context);
                
                todayEntryPreview = LockScreenTodoItemView.inflateViewByType(todoItemViewType, viewBinding.previewContainer, layoutInflater);
                upcomingEntryPreview = LockScreenTodoItemView.inflateViewByType(todoItemViewType, viewBinding.previewContainer, layoutInflater);
                expiredEntryPreview = LockScreenTodoItemView.inflateViewByType(todoItemViewType, viewBinding.previewContainer, layoutInflater);
                
                viewBinding.previewContainer.removeAllViews();
                viewBinding.previewContainer.addView(upcomingEntryPreview.getRoot());
                viewBinding.previewContainer.addView(todayEntryPreview.getRoot());
                viewBinding.previewContainer.addView(expiredEntryPreview.getRoot());
            }
        }
        
        protected void updatePreviews() {
            
            inflatePreviews();
            updatePreviewFontsAndBgs();
            updatePreviewBorders();
            
            updateUpcomingPreviewBorderThickness(Keys.UPCOMING_BORDER_THICKNESS.get());
            updateCurrentPreviewBorderThickness(Keys.BORDER_THICKNESS.get());
            updateExpiredPreviewBorderThickness(Keys.EXPIRED_BORDER_THICKNESS.get());
            
            updatePreviewFontSize(Keys.FONT_SIZE.get());
        }
        
        
        public void updateUpcomingPreviewBorderThickness(int borderThickness) {
            upcomingEntryPreview.setBorderSizeDP(borderThickness);
        }
        
        public void updateCurrentPreviewBorderThickness(int borderThickness) {
            todayEntryPreview.setBorderSizeDP(borderThickness);
        }
        
        public void updateExpiredPreviewBorderThickness(int borderThickness) {
            expiredEntryPreview.setBorderSizeDP(borderThickness);
        }
        
        
        public void updatePreviewFontSize(int fontSizeSP) {
            todayEntryPreview.setCombinedTextSize(fontSizeSP);
            upcomingEntryPreview.setCombinedTextSize(fontSizeSP);
            expiredEntryPreview.setCombinedTextSize(fontSizeSP);
        }
        
        public void updatePreviewFontsAndBgs() {
            
            int fontColor = Keys.FONT_COLOR.get();
            int fontColorUpcoming = Keys.UPCOMING_FONT_COLOR.get();
            int fontColorExpired = Keys.EXPIRED_FONT_COLOR.get();
            
            viewBinding.fontColorUpcomingSelector.setCardBackgroundColor(fontColorUpcoming);
            viewBinding.fontColorSelector.setCardBackgroundColor(fontColor);
            viewBinding.fontColorExpiredSelector.setCardBackgroundColor(fontColorExpired);
            
            
            int bgColor = Keys.BG_COLOR.get();
            int bgColorUpcoming = Keys.UPCOMING_BG_COLOR.get();
            int bgColorExpired = Keys.EXPIRED_BG_COLOR.get();
            
            viewBinding.backgroundColorUpcomingSelector.setCardBackgroundColor(bgColorUpcoming);
            viewBinding.backgroundColorSelector.setCardBackgroundColor(bgColor);
            viewBinding.backgroundColorExpiredSelector.setCardBackgroundColor(bgColorExpired);
            
            
            upcomingEntryPreview.mixAndSetBgAndTextColors(true,
                    getExpiredUpcomingColor(fontColor, fontColorUpcoming),
                    getExpiredUpcomingColor(bgColor, bgColorUpcoming));
            todayEntryPreview.mixAndSetBgAndTextColors(true, fontColor, bgColor);
            expiredEntryPreview.mixAndSetBgAndTextColors(true,
                    getExpiredUpcomingColor(fontColor, fontColorExpired),
                    getExpiredUpcomingColor(bgColor, bgColorExpired));
        }
        
        public void updatePreviewBorders() {
            
            int borderColor = Keys.BORDER_COLOR.get();
            int borderColorUpcoming = Keys.UPCOMING_BORDER_COLOR.get();
            int borderColorExpired = Keys.EXPIRED_BORDER_COLOR.get();
            
            viewBinding.borderColorUpcomingSelector.setCardBackgroundColor(borderColorUpcoming);
            viewBinding.borderColorSelector.setCardBackgroundColor(borderColor);
            viewBinding.borderColorExpiredSelector.setCardBackgroundColor(borderColorExpired);
            
            upcomingEntryPreview.setBorderColor(getExpiredUpcomingColor(borderColor, borderColorUpcoming));
            todayEntryPreview.setBorderColor(borderColor);
            expiredEntryPreview.setBorderColor(getExpiredUpcomingColor(borderColor, borderColorExpired));
        }
    }
}

