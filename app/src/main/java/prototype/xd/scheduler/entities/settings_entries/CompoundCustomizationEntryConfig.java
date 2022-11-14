package prototype.xd.scheduler.entities.settings_entries;

import static prototype.xd.scheduler.entities.settings_entries.SettingsEntryType.COMPOUND_CUSTOMIZATION;
import static prototype.xd.scheduler.utilities.BitmapUtilities.mixTwoColors;
import static prototype.xd.scheduler.utilities.Keys.BORDER_THICKNESS;
import static prototype.xd.scheduler.utilities.Keys.DEFAULT_COLOR_MIX_FACTOR;
import static prototype.xd.scheduler.utilities.Keys.DEFAULT_TODO_ITEM_VIEW_TYPE;
import static prototype.xd.scheduler.utilities.Keys.EXPIRED_BORDER_THICKNESS;
import static prototype.xd.scheduler.utilities.Keys.SERVICE_UPDATE_SIGNAL;
import static prototype.xd.scheduler.utilities.Keys.TODO_ITEM_VIEW_TYPE;
import static prototype.xd.scheduler.utilities.Keys.UPCOMING_BORDER_THICKNESS;
import static prototype.xd.scheduler.utilities.PreferencesStore.preferences;
import static prototype.xd.scheduler.utilities.PreferencesStore.servicePreferences;
import static prototype.xd.scheduler.utilities.Utilities.invokeColorDialogue;

import android.app.AlertDialog;
import android.view.LayoutInflater;
import android.widget.GridView;

import prototype.xd.scheduler.R;
import prototype.xd.scheduler.adapters.TodoItemViewListAdapter;
import prototype.xd.scheduler.databinding.CompoundCustomizationSettingsEntryBinding;
import prototype.xd.scheduler.databinding.GridSelectionViewBinding;
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
        
        TodoItemViewType todoItemViewType;
        
        CompoundCustomizationViewHolder(CompoundCustomizationSettingsEntryBinding viewBinding) {
            super(viewBinding);
            
            todoItemViewType = TodoItemViewType.valueOf(preferences.getString(TODO_ITEM_VIEW_TYPE, DEFAULT_TODO_ITEM_VIEW_TYPE));
            
            inflatePreviews();
            updatePreviews();
            
            Utilities.ColorPickerKeyedClickListener colorPickerClickListener = (dialog, selectedColor, key, allColors) -> {
                preferences.edit().putInt(key, selectedColor).apply();
                switch (key) {
                    case Keys.UPCOMING_BG_COLOR:
                    case Keys.BG_COLOR:
                    case Keys.EXPIRED_BG_COLOR:
                        updatePreviewBgs();
                        break;
                    case Keys.UPCOMING_FONT_COLOR:
                    case Keys.FONT_COLOR:
                    case Keys.EXPIRED_FONT_COLOR:
                        updatePreviewFonts();
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
                    invokeColorDialogue(viewBinding.backgroundColorSelector, colorPickerClickListener,
                            Keys.BG_COLOR, Keys.SETTINGS_DEFAULT_BG_COLOR));
            viewBinding.backgroundColorUpcomingSelector.setOnClickListener(v ->
                    invokeColorDialogue(viewBinding.backgroundColorUpcomingSelector, colorPickerClickListener,
                            Keys.UPCOMING_BG_COLOR, Keys.SETTINGS_DEFAULT_UPCOMING_BG_COLOR));
            viewBinding.backgroundColorExpiredSelector.setOnClickListener(v ->
                    invokeColorDialogue(viewBinding.backgroundColorExpiredSelector, colorPickerClickListener,
                            Keys.EXPIRED_BG_COLOR, Keys.SETTINGS_DEFAULT_EXPIRED_BG_COLOR));
            
            viewBinding.fontColorSelector.setOnClickListener(v ->
                    invokeColorDialogue(viewBinding.fontColorSelector, colorPickerClickListener,
                            Keys.FONT_COLOR, Keys.SETTINGS_DEFAULT_FONT_COLOR));
            viewBinding.fontColorUpcomingSelector.setOnClickListener(v ->
                    invokeColorDialogue(viewBinding.fontColorUpcomingSelector, colorPickerClickListener,
                            Keys.UPCOMING_FONT_COLOR, Keys.SETTINGS_DEFAULT_UPCOMING_FONT_COLOR));
            viewBinding.fontColorExpiredSelector.setOnClickListener(v ->
                    invokeColorDialogue(viewBinding.fontColorExpiredSelector, colorPickerClickListener,
                            Keys.EXPIRED_FONT_COLOR, Keys.SETTINGS_DEFAULT_EXPIRED_FONT_COLOR));
            
            viewBinding.borderColorSelector.setOnClickListener(v ->
                    invokeColorDialogue(viewBinding.borderColorSelector, colorPickerClickListener,
                            Keys.BORDER_COLOR, Keys.SETTINGS_DEFAULT_BORDER_COLOR));
            viewBinding.borderColorUpcomingSelector.setOnClickListener(v ->
                    invokeColorDialogue(viewBinding.borderColorUpcomingSelector, colorPickerClickListener,
                            Keys.UPCOMING_BORDER_COLOR, Keys.SETTINGS_DEFAULT_UPCOMING_BORDER_COLOR));
            viewBinding.borderColorExpiredSelector.setOnClickListener(v ->
                    invokeColorDialogue(viewBinding.borderColorExpiredSelector, colorPickerClickListener,
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
            
        }
        
        @Override
        void bind(CompoundCustomizationEntryConfig config) {
            // nothing special required, this entry should be the only on of it's kind
        }
        
        protected void inflatePreviews() {
            LayoutInflater layoutInflater = LayoutInflater.from(context);
            todayEntryPreview = LockScreenTodoItemView.inflateViewByType(todoItemViewType, viewBinding.previewContainer, layoutInflater);
            upcomingEntryPreview = LockScreenTodoItemView.inflateViewByType(todoItemViewType, viewBinding.previewContainer, layoutInflater);
            expiredEntryPreview = LockScreenTodoItemView.inflateViewByType(todoItemViewType, viewBinding.previewContainer, layoutInflater);
            viewBinding.previewContainer.addView(upcomingEntryPreview.getRoot());
            viewBinding.previewContainer.addView(todayEntryPreview.getRoot());
            viewBinding.previewContainer.addView(expiredEntryPreview.getRoot());
    
            todayEntryPreview.getRoot().setOnClickListener(v -> {
                final AlertDialog.Builder alert = new AlertDialog.Builder(v.getContext());
        
                GridSelectionViewBinding gridSelection = GridSelectionViewBinding.inflate(LayoutInflater.from(context));
                GridView gridView = gridSelection.gridView;
                gridView.setNumColumns(1);
                gridView.setHorizontalSpacing(5);
                gridView.setVerticalSpacing(5);
                gridView.setAdapter(new TodoItemViewListAdapter());
        
                alert.setView(gridSelection.getRoot());
                alert.show();
            });
        }
        
        protected void updatePreviews() {
            updatePreviewFonts();
            updatePreviewBgs();
            updatePreviewBorders();
            updateUpcomingPreviewBorderThickness(preferences.getInt(Keys.UPCOMING_BORDER_THICKNESS, Keys.SETTINGS_DEFAULT_UPCOMING_BORDER_THICKNESS));
            updateCurrentPreviewBorderThickness(preferences.getInt(Keys.BORDER_THICKNESS, Keys.SETTINGS_DEFAULT_BORDER_THICKNESS));
            updateExpiredPreviewBorderThickness(preferences.getInt(Keys.EXPIRED_BORDER_THICKNESS, Keys.SETTINGS_DEFAULT_EXPIRED_BORDER_THICKNESS));
        }
        
        public void updateUpcomingPreviewBorderThickness(int borderThickness) {
            upcomingEntryPreview.setBorderSize(borderThickness);
        }
        
        public void updateCurrentPreviewBorderThickness(int borderThickness) {
            todayEntryPreview.setBorderSize(borderThickness);
        }
        
        public void updateExpiredPreviewBorderThickness(int borderThickness) {
            expiredEntryPreview.setBorderSize(borderThickness);
        }
        
        public void updatePreviewFonts() {
            
            int fontColor = preferences.getInt(Keys.FONT_COLOR, Keys.SETTINGS_DEFAULT_FONT_COLOR);
            int fontColorUpcoming = preferences.getInt(Keys.UPCOMING_FONT_COLOR, Keys.SETTINGS_DEFAULT_UPCOMING_FONT_COLOR);
            int fontColorExpired = preferences.getInt(Keys.EXPIRED_FONT_COLOR, Keys.SETTINGS_DEFAULT_EXPIRED_FONT_COLOR);
            
            viewBinding.fontColorUpcomingSelector.setCardBackgroundColor(fontColorUpcoming);
            viewBinding.fontColorSelector.setCardBackgroundColor(fontColor);
            viewBinding.fontColorExpiredSelector.setCardBackgroundColor(fontColorExpired);
            
            upcomingEntryPreview.setTitleTextColor(mixTwoColors(fontColor, fontColorUpcoming, DEFAULT_COLOR_MIX_FACTOR));
            todayEntryPreview.setTitleTextColor(fontColor);
            expiredEntryPreview.setTitleTextColor(mixTwoColors(fontColor, fontColorExpired, DEFAULT_COLOR_MIX_FACTOR));
        }
        
        public void updatePreviewBgs() {
            
            int bgColor = preferences.getInt(Keys.BG_COLOR, Keys.SETTINGS_DEFAULT_BG_COLOR);
            int bgColorUpcoming = preferences.getInt(Keys.UPCOMING_BG_COLOR, Keys.SETTINGS_DEFAULT_UPCOMING_BG_COLOR);
            int bgColorExpired = preferences.getInt(Keys.EXPIRED_BG_COLOR, Keys.SETTINGS_DEFAULT_EXPIRED_BG_COLOR);
            
            viewBinding.backgroundColorUpcomingSelector.setCardBackgroundColor(bgColorUpcoming);
            viewBinding.backgroundColorSelector.setCardBackgroundColor(bgColor);
            viewBinding.backgroundColorExpiredSelector.setCardBackgroundColor(bgColorExpired);
            
            upcomingEntryPreview.setBackgroundColor(mixTwoColors(bgColor, bgColorUpcoming, DEFAULT_COLOR_MIX_FACTOR));
            todayEntryPreview.setBackgroundColor(bgColor);
            expiredEntryPreview.setBackgroundColor(mixTwoColors(bgColor, bgColorExpired, DEFAULT_COLOR_MIX_FACTOR));
        }
        
        public void updatePreviewBorders() {
            
            int borderColor = preferences.getInt(Keys.BORDER_COLOR, Keys.SETTINGS_DEFAULT_BORDER_COLOR);
            int borderColorUpcoming = preferences.getInt(Keys.UPCOMING_BORDER_COLOR, Keys.SETTINGS_DEFAULT_UPCOMING_BORDER_COLOR);
            int borderColorExpired = preferences.getInt(Keys.EXPIRED_BORDER_COLOR, Keys.SETTINGS_DEFAULT_EXPIRED_BORDER_COLOR);
            
            viewBinding.borderColorUpcomingSelector.setCardBackgroundColor(borderColorUpcoming);
            viewBinding.borderColorSelector.setCardBackgroundColor(borderColor);
            viewBinding.borderColorExpiredSelector.setCardBackgroundColor(borderColorExpired);
            
            upcomingEntryPreview.setBorderColor(mixTwoColors(borderColor, borderColorUpcoming, DEFAULT_COLOR_MIX_FACTOR));
            todayEntryPreview.setBorderColor(borderColor);
            expiredEntryPreview.setBorderColor(mixTwoColors(borderColor, borderColorExpired, DEFAULT_COLOR_MIX_FACTOR));
        }
    }
}

