package prototype.xd.scheduler.entities.settings_entries;

import static prototype.xd.scheduler.entities.settings_entries.SettingsEntryType.COMPOUND_CUSTOMIZATION;
import static prototype.xd.scheduler.utilities.BitmapUtilities.mixTwoColors;
import static prototype.xd.scheduler.utilities.DialogUtilities.invokeColorDialogue;
import static prototype.xd.scheduler.utilities.Keys.BORDER_THICKNESS;
import static prototype.xd.scheduler.utilities.Keys.DEFAULT_TIME_OFFSET_COLOR_MIX_FACTOR;
import static prototype.xd.scheduler.utilities.Keys.EXPIRED_BORDER_THICKNESS;
import static prototype.xd.scheduler.utilities.Keys.SERVICE_UPDATE_SIGNAL;
import static prototype.xd.scheduler.utilities.Keys.SETTINGS_DEFAULT_TODO_ITEM_VIEW_TYPE;
import static prototype.xd.scheduler.utilities.Keys.TODO_ITEM_VIEW_TYPE;
import static prototype.xd.scheduler.utilities.Keys.UPCOMING_BORDER_THICKNESS;
import static prototype.xd.scheduler.utilities.PreferencesStore.preferences;
import static prototype.xd.scheduler.utilities.PreferencesStore.servicePreferences;

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
            
            todoItemViewType = TodoItemViewType.valueOf(preferences.getString(TODO_ITEM_VIEW_TYPE, SETTINGS_DEFAULT_TODO_ITEM_VIEW_TYPE));
            
            LayoutInflater layoutInflater = LayoutInflater.from(context);
            LinearLayout viewSelectionDialogView = TodoItemViewSelectionDialogBinding.inflate(layoutInflater).getRoot();
            
            viewSelectionDialog = new MaterialAlertDialogBuilder(context)
                    .setView(viewSelectionDialogView)
                    .create();
            
            for (TodoItemViewType viewType : TodoItemViewType.values()) {
                View view = LockScreenTodoItemView.inflateViewByType(viewType, viewSelectionDialogView, layoutInflater).getRoot();
                view.setOnClickListener(v -> {
                    todoItemViewType = viewType;
                    preferences.edit().putString(TODO_ITEM_VIEW_TYPE, todoItemViewType.name()).apply();
                    updatePreviews();
                    viewSelectionDialog.dismiss();
                });
                viewSelectionDialogView.addView(view);
            }
            
            viewBinding.previewContainer.setOnClickListener(v ->
                    viewSelectionDialog.show());
            
            updatePreviews();
            
            DialogUtilities.ColorPickerKeyedClickListener colorPickerClickListener = (dialog, selectedColor, key, allColors) -> {
                preferences.edit().putInt(key, selectedColor).apply();
                switch (key) {
                    case Keys.UPCOMING_BG_COLOR:
                    case Keys.BG_COLOR:
                    case Keys.EXPIRED_BG_COLOR:
                    
                    case Keys.UPCOMING_FONT_COLOR:
                    case Keys.FONT_COLOR:
                    case Keys.EXPIRED_FONT_COLOR:
                        updatePreviewFontsAndBgs();
                        break;
                    case Keys.UPCOMING_BORDER_COLOR:
                    case Keys.BORDER_COLOR:
                    case Keys.EXPIRED_BORDER_COLOR:
                        updatePreviewBorders();
                        break;
                }
                servicePreferences.edit().putBoolean(SERVICE_UPDATE_SIGNAL, true).apply();
            };
            
            viewBinding.backgroundColorSelector.setOnClickListener(v ->
                    invokeColorDialogue(context, lifecycle,
                            colorPickerClickListener,
                            Keys.BG_COLOR, Keys.SETTINGS_DEFAULT_BG_COLOR));
            viewBinding.backgroundColorUpcomingSelector.setOnClickListener(v ->
                    invokeColorDialogue(context, lifecycle,
                            colorPickerClickListener,
                            Keys.UPCOMING_BG_COLOR, Keys.SETTINGS_DEFAULT_UPCOMING_BG_COLOR));
            viewBinding.backgroundColorExpiredSelector.setOnClickListener(v ->
                    invokeColorDialogue(context, lifecycle,
                            colorPickerClickListener,
                            Keys.EXPIRED_BG_COLOR, Keys.SETTINGS_DEFAULT_EXPIRED_BG_COLOR));
            
            viewBinding.fontColorSelector.setOnClickListener(v ->
                    invokeColorDialogue(context, lifecycle,
                            colorPickerClickListener,
                            Keys.FONT_COLOR, Keys.SETTINGS_DEFAULT_FONT_COLOR));
            viewBinding.fontColorUpcomingSelector.setOnClickListener(v ->
                    invokeColorDialogue(context, lifecycle,
                            colorPickerClickListener,
                            Keys.UPCOMING_FONT_COLOR, Keys.SETTINGS_DEFAULT_UPCOMING_FONT_COLOR));
            viewBinding.fontColorExpiredSelector.setOnClickListener(v ->
                    invokeColorDialogue(context, lifecycle,
                            colorPickerClickListener,
                            Keys.EXPIRED_FONT_COLOR, Keys.SETTINGS_DEFAULT_EXPIRED_FONT_COLOR));
            
            viewBinding.borderColorSelector.setOnClickListener(v ->
                    invokeColorDialogue(context, lifecycle,
                            colorPickerClickListener,
                            Keys.BORDER_COLOR, Keys.SETTINGS_DEFAULT_BORDER_COLOR));
            viewBinding.borderColorUpcomingSelector.setOnClickListener(v ->
                    invokeColorDialogue(context, lifecycle,
                            colorPickerClickListener,
                            Keys.UPCOMING_BORDER_COLOR, Keys.SETTINGS_DEFAULT_UPCOMING_BORDER_COLOR));
            viewBinding.borderColorExpiredSelector.setOnClickListener(v ->
                    invokeColorDialogue(context, lifecycle,
                            colorPickerClickListener,
                            Keys.EXPIRED_BORDER_COLOR, Keys.SETTINGS_DEFAULT_EXPIRED_BORDER_COLOR));
            
            Utilities.SliderOnChangeKeyedListener onChangeListener = (slider, progress, fromUser, key) -> {
                switch (key) {
                    case UPCOMING_BORDER_THICKNESS:
                        updateUpcomingPreviewBorderThickness((int) progress);
                        break;
                    case EXPIRED_BORDER_THICKNESS:
                        updateExpiredPreviewBorderThickness((int) progress);
                        break;
                    case BORDER_THICKNESS:
                    default:
                        updateCurrentPreviewBorderThickness((int) progress);
                        break;
                }
            };
            
            Utilities.setSliderChangeListener(
                    viewBinding.upcomingBorderThicknessText,
                    viewBinding.upcomingBorderThicknessSeekBar,
                    onChangeListener, R.string.settings_upcoming_border_thickness,
                    Keys.UPCOMING_BORDER_THICKNESS, Keys.SETTINGS_DEFAULT_UPCOMING_BORDER_THICKNESS, true);
            
            Utilities.setSliderChangeListener(
                    viewBinding.currentBorderThicknessText,
                    viewBinding.currentBorderThicknessSeekBar,
                    onChangeListener, R.string.settings_current_border_thickness,
                    Keys.BORDER_THICKNESS, Keys.SETTINGS_DEFAULT_BORDER_THICKNESS, true);
            
            Utilities.setSliderChangeListener(
                    viewBinding.expiredBorderThicknessText,
                    viewBinding.expiredBorderThicknessSeekBar,
                    onChangeListener, R.string.settings_expired_border_thickness,
                    Keys.EXPIRED_BORDER_THICKNESS, Keys.SETTINGS_DEFAULT_EXPIRED_BORDER_THICKNESS, true);
            
            Utilities.setSliderChangeListener(
                    viewBinding.fontSizeText,
                    viewBinding.fontSizeSeekBar,
                    (slider, value, fromUser, key) -> updatePreviewFontSize((int) value), R.string.settings_font_size,
                    Keys.FONT_SIZE, Keys.SETTINGS_DEFAULT_FONT_SIZE, false);
            
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
            
            updateUpcomingPreviewBorderThickness(preferences.getInt(Keys.UPCOMING_BORDER_THICKNESS, Keys.SETTINGS_DEFAULT_UPCOMING_BORDER_THICKNESS));
            updateCurrentPreviewBorderThickness(preferences.getInt(Keys.BORDER_THICKNESS, Keys.SETTINGS_DEFAULT_BORDER_THICKNESS));
            updateExpiredPreviewBorderThickness(preferences.getInt(Keys.EXPIRED_BORDER_THICKNESS, Keys.SETTINGS_DEFAULT_EXPIRED_BORDER_THICKNESS));
            
            updatePreviewFontSize(preferences.getInt(Keys.FONT_SIZE, Keys.SETTINGS_DEFAULT_FONT_SIZE));
        }
        
        
        public void updateUpcomingPreviewBorderThickness(int borderThickness) {
            upcomingEntryPreview.setBorderSizeDP(borderThickness, preferences);
        }
        
        public void updateCurrentPreviewBorderThickness(int borderThickness) {
            todayEntryPreview.setBorderSizeDP(borderThickness, preferences);
        }
        
        public void updateExpiredPreviewBorderThickness(int borderThickness) {
            expiredEntryPreview.setBorderSizeDP(borderThickness, preferences);
        }
        
        
        public void updatePreviewFontSize(int fontSizeSP) {
            todayEntryPreview.setCombinedTextSize(fontSizeSP);
            upcomingEntryPreview.setCombinedTextSize(fontSizeSP);
            expiredEntryPreview.setCombinedTextSize(fontSizeSP);
        }
        
        public void updatePreviewFontsAndBgs() {
            
            int fontColor = preferences.getInt(Keys.FONT_COLOR, Keys.SETTINGS_DEFAULT_FONT_COLOR);
            int fontColorUpcoming = preferences.getInt(Keys.UPCOMING_FONT_COLOR, Keys.SETTINGS_DEFAULT_UPCOMING_FONT_COLOR);
            int fontColorExpired = preferences.getInt(Keys.EXPIRED_FONT_COLOR, Keys.SETTINGS_DEFAULT_EXPIRED_FONT_COLOR);
            
            viewBinding.fontColorUpcomingSelector.setCardBackgroundColor(fontColorUpcoming);
            viewBinding.fontColorSelector.setCardBackgroundColor(fontColor);
            viewBinding.fontColorExpiredSelector.setCardBackgroundColor(fontColorExpired);
            
            
            int bgColor = preferences.getInt(Keys.BG_COLOR, Keys.SETTINGS_DEFAULT_BG_COLOR);
            int bgColorUpcoming = preferences.getInt(Keys.UPCOMING_BG_COLOR, Keys.SETTINGS_DEFAULT_UPCOMING_BG_COLOR);
            int bgColorExpired = preferences.getInt(Keys.EXPIRED_BG_COLOR, Keys.SETTINGS_DEFAULT_EXPIRED_BG_COLOR);
            
            viewBinding.backgroundColorUpcomingSelector.setCardBackgroundColor(bgColorUpcoming);
            viewBinding.backgroundColorSelector.setCardBackgroundColor(bgColor);
            viewBinding.backgroundColorExpiredSelector.setCardBackgroundColor(bgColorExpired);
            
            
            upcomingEntryPreview.mixAndSetBgAndTextColors(
                    mixTwoColors(fontColor, fontColorUpcoming, DEFAULT_TIME_OFFSET_COLOR_MIX_FACTOR),
                    mixTwoColors(bgColor, bgColorUpcoming, DEFAULT_TIME_OFFSET_COLOR_MIX_FACTOR));
            todayEntryPreview.mixAndSetBgAndTextColors(fontColor, bgColor);
            expiredEntryPreview.mixAndSetBgAndTextColors(
                    mixTwoColors(fontColor, fontColorExpired, DEFAULT_TIME_OFFSET_COLOR_MIX_FACTOR),
                    mixTwoColors(bgColor, bgColorExpired, DEFAULT_TIME_OFFSET_COLOR_MIX_FACTOR));
        }
        
        public void updatePreviewBorders() {
            
            int borderColor = preferences.getInt(Keys.BORDER_COLOR, Keys.SETTINGS_DEFAULT_BORDER_COLOR);
            int borderColorUpcoming = preferences.getInt(Keys.UPCOMING_BORDER_COLOR, Keys.SETTINGS_DEFAULT_UPCOMING_BORDER_COLOR);
            int borderColorExpired = preferences.getInt(Keys.EXPIRED_BORDER_COLOR, Keys.SETTINGS_DEFAULT_EXPIRED_BORDER_COLOR);
            
            viewBinding.borderColorUpcomingSelector.setCardBackgroundColor(borderColorUpcoming);
            viewBinding.borderColorSelector.setCardBackgroundColor(borderColor);
            viewBinding.borderColorExpiredSelector.setCardBackgroundColor(borderColorExpired);
            
            upcomingEntryPreview.setBorderColor(mixTwoColors(borderColor, borderColorUpcoming, DEFAULT_TIME_OFFSET_COLOR_MIX_FACTOR));
            todayEntryPreview.setBorderColor(borderColor);
            expiredEntryPreview.setBorderColor(mixTwoColors(borderColor, borderColorExpired, DEFAULT_TIME_OFFSET_COLOR_MIX_FACTOR));
        }
    }
}

