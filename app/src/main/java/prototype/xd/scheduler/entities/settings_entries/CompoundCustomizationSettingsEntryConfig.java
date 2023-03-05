package prototype.xd.scheduler.entities.settings_entries;

import static android.widget.LinearLayout.LayoutParams.MATCH_PARENT;
import static android.widget.LinearLayout.LayoutParams.WRAP_CONTENT;
import static prototype.xd.scheduler.entities.settings_entries.SettingsEntryType.COMPOUND_CUSTOMIZATION;

import android.graphics.Color;
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
import prototype.xd.scheduler.utilities.DialogUtilities;
import prototype.xd.scheduler.utilities.Static;
import prototype.xd.scheduler.utilities.Utilities;
import prototype.xd.scheduler.utilities.misc.ContextWrapper;
import prototype.xd.scheduler.views.lockscreen.LockScreenTodoItemView;
import prototype.xd.scheduler.views.lockscreen.LockScreenTodoItemView.TodoItemViewType;
import prototype.xd.scheduler.views.settings.EntryPreviewContainer;

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
        
        ViewHolder(@NonNull final ContextWrapper wrapper,
                   @NonNull final CompoundCustomizationSettingsEntryBinding viewBinding) {
            super(wrapper, viewBinding);
            
            int padding = wrapper.getResources().getDimensionPixelSize(R.dimen.dialog_menu_padding_left_right);
            int itemPadding = wrapper.getResources().getDimensionPixelSize(R.dimen.lockscreen_item_vertical_padding);
            
            LinearLayout viewSelectionDialogView = new LinearLayout(wrapper.context);
            viewSelectionDialogView.setLayoutParams(new LinearLayout.LayoutParams(MATCH_PARENT, WRAP_CONTENT));
            viewSelectionDialogView.setOrientation(LinearLayout.VERTICAL);
            viewSelectionDialogView.setPadding(padding, padding, padding, padding - itemPadding);
            
            viewSelectionDialog = wrapper.attachDialogToLifecycle(
                    new MaterialAlertDialogBuilder(wrapper.context, R.style.DefaultAlertDialogTheme)
                            .setIcon(R.drawable.ic_view_carousel_24)
                            .setTitle(R.string.select_view)
                            .setMessage(R.string.select_view_description)
                            .setView(viewSelectionDialogView)
                            .create(), null);
            
            entryPreviewContainer = new EntryPreviewContainer(wrapper, viewBinding.previewContainer, true) {
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
            
            int bgColor = MaterialColors.getColor(wrapper.context, R.attr.colorSurfaceVariant, Color.WHITE);
            int borderColor = MaterialColors.getColor(wrapper.context, R.attr.colorAccent, Color.GRAY);
            int fontColor = MaterialColors.getColor(wrapper.context, R.attr.colorOnSurfaceVariant, Color.BLACK);
            
            for (TodoItemViewType viewType : TodoItemViewType.values()) {
                LockScreenTodoItemView.inflateViewByType(viewType, viewSelectionDialogView, wrapper.getLayoutInflater())
                        .mixAndSetBgAndTextColors(true, fontColor, bgColor)
                        .setBorderColor(borderColor)
                        .setOnClickListener(v -> {
                            entryPreviewContainer.setTodoItemViewType(viewType);
                            viewSelectionDialog.dismiss();
                        }).addToContainer(viewSelectionDialogView);
            }
            
            viewBinding.previewContainer.setOnClickListener(v ->
                    viewSelectionDialog.show());
            
            ObjIntConsumer<Static.DefaultedInteger> colorPickerColorSelectedListener = (value, selectedColor) -> {
                value.put(selectedColor);
                entryPreviewContainer.notifyColorChanged(value, selectedColor);
            };
            
            viewBinding.currentBackgroundColorSelector.setOnClickListener(v ->
                    DialogUtilities.displayColorPicker(wrapper,
                            colorPickerColorSelectedListener,
                            Static.BG_COLOR.CURRENT));
            viewBinding.upcomingBackgroundColorSelector.setOnClickListener(v ->
                    DialogUtilities.displayColorPicker(wrapper,
                            colorPickerColorSelectedListener,
                            Static.BG_COLOR.UPCOMING));
            viewBinding.expiredBackgroundColorSelector.setOnClickListener(v ->
                    DialogUtilities.displayColorPicker(wrapper,
                            colorPickerColorSelectedListener,
                            Static.BG_COLOR.EXPIRED));
            
            viewBinding.currentFontColorSelector.setOnClickListener(v ->
                    DialogUtilities.displayColorPicker(wrapper,
                            colorPickerColorSelectedListener,
                            Static.FONT_COLOR.CURRENT));
            viewBinding.upcomingFontColorSelector.setOnClickListener(v ->
                    DialogUtilities.displayColorPicker(wrapper,
                            colorPickerColorSelectedListener,
                            Static.FONT_COLOR.UPCOMING));
            viewBinding.expiredFontColorSelector.setOnClickListener(v ->
                    DialogUtilities.displayColorPicker(wrapper,
                            colorPickerColorSelectedListener,
                            Static.FONT_COLOR.EXPIRED));
            
            viewBinding.currentBorderColorSelector.setOnClickListener(v ->
                    DialogUtilities.displayColorPicker(wrapper,
                            colorPickerColorSelectedListener,
                            Static.BORDER_COLOR.CURRENT));
            viewBinding.upcomingBorderColorSelector.setOnClickListener(v ->
                    DialogUtilities.displayColorPicker(wrapper,
                            colorPickerColorSelectedListener,
                            Static.BORDER_COLOR.UPCOMING));
            viewBinding.expiredBorderColorSelector.setOnClickListener(v ->
                    DialogUtilities.displayColorPicker(wrapper,
                            colorPickerColorSelectedListener,
                            Static.BORDER_COLOR.EXPIRED));
            
            Utilities.setSliderChangeListener(
                    viewBinding.adaptiveColorBalanceDescription,
                    viewBinding.adaptiveColorBalanceSlider,
                    (slider, sliderValue, fromUser, value) -> entryPreviewContainer.setPreviewAdaptiveColorBalance(sliderValue),
                    R.string.settings_adaptive_color_balance,
                    Static.ADAPTIVE_COLOR_BALANCE, true);
            
            Utilities.setSliderChangeListener(
                    viewBinding.upcomingBorderThicknessDescription,
                    viewBinding.upcomingBorderThicknessSlider,
                    (slider, sliderValue, fromUser, value) -> entryPreviewContainer.setUpcomingPreviewBorderThickness(sliderValue),
                    R.string.settings_upcoming_border_thickness,
                    Static.BORDER_THICKNESS.UPCOMING, true);
            
            Utilities.setSliderChangeListener(
                    viewBinding.currentBorderThicknessDescription,
                    viewBinding.currentBorderThicknessSlider,
                    (slider, sliderValue, fromUser, value) -> entryPreviewContainer.setCurrentPreviewBorderThickness(sliderValue),
                    R.string.settings_current_border_thickness,
                    Static.BORDER_THICKNESS.CURRENT, true);
            
            Utilities.setSliderChangeListener(
                    viewBinding.expiredBorderThicknessDescription,
                    viewBinding.expiredBorderThicknessSlider,
                    (slider, sliderValue, fromUser, value) -> entryPreviewContainer.setExpiredPreviewBorderThickness(sliderValue),
                    R.string.settings_expired_border_thickness,
                    Static.BORDER_THICKNESS.EXPIRED, true);
            
            Utilities.setSliderChangeListener(
                    viewBinding.fontSizeDescription,
                    viewBinding.fontSizeSlider,
                    (slider, sliderValue, fromUser, value) -> entryPreviewContainer.setPreviewFontSize(sliderValue), R.string.settings_font_size,
                    Static.FONT_SIZE, false);
            
        }
        
        @Override
        void bind(CompoundCustomizationSettingsEntryConfig config) {
            // nothing special required, this entry should be the only one of it's kind
        }
    }
}

