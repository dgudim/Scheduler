package prototype.xd.scheduler.views.settings;

import static prototype.xd.scheduler.utilities.DialogUtilities.displayMessageDialog;
import static prototype.xd.scheduler.utilities.Static.KEY_SEPARATOR;
import static prototype.xd.scheduler.utilities.Static.VISIBLE;
import static prototype.xd.scheduler.utilities.Static.getFirstValidKey;
import static prototype.xd.scheduler.utilities.SystemCalendarUtils.calendarKeyToReadable;
import static prototype.xd.scheduler.utilities.Utilities.setSliderChangeListener;
import static prototype.xd.scheduler.utilities.Utilities.setSwitchChangeListener;

import android.content.SharedPreferences;
import android.text.Editable;
import android.text.TextWatcher;
import android.view.View;
import android.widget.TextView;

import androidx.activity.ComponentDialog;
import androidx.annotation.ColorInt;
import androidx.annotation.IntRange;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import java.util.List;
import java.util.Set;

import prototype.xd.scheduler.R;
import prototype.xd.scheduler.databinding.EntrySettingsBinding;
import prototype.xd.scheduler.entities.SystemCalendarEvent;
import prototype.xd.scheduler.utilities.DialogUtilities;
import prototype.xd.scheduler.utilities.Static;
import prototype.xd.scheduler.utilities.TodoEntryManager;
import prototype.xd.scheduler.utilities.misc.ContextWrapper;

public class SystemCalendarSettings extends PopupSettingsView {
    
    // if called from regular settings
    private List<String> subKeys;
    private String prefKey;
    @ColorInt
    private int color;
    
    // if called from main screen
    @Nullable
    private SystemCalendarEvent event;
    
    private TextWatcher currentListener;
    
    public SystemCalendarSettings(@Nullable final TodoEntryManager todoEntryManager) {
        super(todoEntryManager);
    }
    
    @NonNull
    @Override
    public EntryPreviewContainer getEntryPreviewContainer(@NonNull EntrySettingsBinding bnd) {
        return new EntryPreviewContainer(wrapper, bnd.previewContainer, true) {
            @ColorInt
            @Override
            protected int currentFontColorGetter() {
                return Static.FONT_COLOR.CURRENT.get(subKeys);
            }
            
            @ColorInt
            @Override
            protected int currentBgColorGetter() {
                return Static.BG_COLOR.CURRENT.getOnlyBySubKeys(subKeys, Static.SETTINGS_DEFAULT_CALENDAR_EVENT_BG_COLOR.applyAsInt(color));
            }
            
            @ColorInt
            @Override
            protected int currentBorderColorGetter() {
                return Static.BORDER_COLOR.CURRENT.get(subKeys);
            }
            
            @Override
            protected int currentBorderThicknessGetter() {
                return Static.BORDER_THICKNESS.CURRENT.get(subKeys);
            }
            
            @IntRange(from = 0, to = 10)
            @Override
            protected int adaptiveColorBalanceGetter() {
                return Static.ADAPTIVE_COLOR_BALANCE.get(subKeys);
            }
        };
    }
    
    public void show(@NonNull final String prefKey,
                     @NonNull final List<String> subKeys,
                     @ColorInt int color,
                     @NonNull ContextWrapper wrapper) {
        event = null;
        this.prefKey = prefKey;
        this.color = color;
        this.subKeys = subKeys;
        show(wrapper.fragmentManager, "system_calendar_settings: " + prefKey);
    }
    
    public void show(@NonNull final SystemCalendarEvent event,
                     @NonNull ContextWrapper wrapper) {
        this.event = event;
        prefKey = event.prefKey;
        color = event.data.color;
        subKeys = event.subKeys;
        show(wrapper.fragmentManager, "system_calendar_settings: " + event);
    }
    
    @Override
    public void buildDialogStatic(@NonNull EntrySettingsBinding bnd, @NonNull ComponentDialog dialog) {
        super.buildDialogStatic(bnd, dialog);
        bnd.groupSelector.setVisibility(View.GONE);
    }
    
    @Override
    protected void buildDialogDynamic(@NonNull EntrySettingsBinding bnd, @NonNull ComponentDialog dialog) {
        bnd.entrySettingsTitle.setText(calendarKeyToReadable(wrapper.context, prefKey));
    
        updateAllIndicators(bnd);
        entryPreviewContainer.refreshAll(true);
    
        bnd.settingsResetButton.setOnClickListener(v ->
                displayMessageDialog(wrapper, builder -> {
                    builder.setTitle(R.string.reset_settings_prompt);
                    builder.setMessage(R.string.reset_calendar_settings_description);
                    builder.setIcon(R.drawable.ic_clear_all_24);
                    builder.setNegativeButton(R.string.cancel, null);
                
                    builder.setPositiveButton(R.string.reset, (dialogInterface, whichButton) -> {
                        Set<String> preferenceKeys = Static.getAll().keySet();
                        SharedPreferences.Editor editor = Static.edit();
                        for (String preferenceKey : preferenceKeys) {
                            // reset all except for visibility
                            if (preferenceKey.startsWith(prefKey) && !preferenceKey.endsWith(VISIBLE)) {
                                editor.remove(preferenceKey);
                            }
                        }
                        editor.apply();
                        if (event != null) {
                            event.invalidateAllParametersOfConnectedEntries();
                        }
                        rebuild();
                    });
                }));
    
        bnd.currentFontColorSelector.setOnClickListener(view -> DialogUtilities.displayColorPicker(
                wrapper,
                bnd.fontColorState, this,
                Static.FONT_COLOR.CURRENT,
                value -> value.get(subKeys)));
    
        bnd.currentBackgroundColorSelector.setOnClickListener(view -> DialogUtilities.displayColorPicker(
                wrapper,
                bnd.backgroundColorState, this,
                Static.BG_COLOR.CURRENT,
                value -> value.getOnlyBySubKeys(subKeys, Static.SETTINGS_DEFAULT_CALENDAR_EVENT_BG_COLOR.applyAsInt(color))));
    
        bnd.currentBorderColorSelector.setOnClickListener(view -> DialogUtilities.displayColorPicker(
                wrapper,
                bnd.borderColorState, this,
                Static.BORDER_COLOR.CURRENT,
                value -> value.get(subKeys)));
    
        setSliderChangeListener(
                bnd.borderThicknessDescription,
                bnd.borderThicknessSlider, bnd.borderThicknessState,
                this, R.string.settings_border_thickness,
                Static.BORDER_THICKNESS.CURRENT,
                value -> value.get(subKeys),
                (slider, value, fromUser) -> entryPreviewContainer.setCurrentPreviewBorderThickness((int) value));
    
        setSliderChangeListener(
                bnd.priorityDescription,
                bnd.prioritySlider, bnd.priorityState,
                this, R.string.settings_priority,
                Static.PRIORITY,
                value -> value.get(subKeys), null);
    
        setSliderChangeListener(
                bnd.adaptiveColorBalanceDescription,
                bnd.adaptiveColorBalanceSlider, bnd.adaptiveColorBalanceState,
                this, R.string.settings_adaptive_color_balance,
                Static.ADAPTIVE_COLOR_BALANCE,
                value -> value.get(subKeys),
                (slider, value, fromUser) -> entryPreviewContainer.setPreviewAdaptiveColorBalance((int) value));
    
        setSliderChangeListener(
                bnd.showDaysUpcomingDescription,
                bnd.showDaysUpcomingSlider, bnd.showDaysUpcomingState,
                this, R.plurals.settings_in_n_days,
                Static.UPCOMING_ITEMS_OFFSET,
                value -> value.get(subKeys), null);
    
        setSliderChangeListener(
                bnd.showDaysExpiredDescription,
                bnd.showDaysExpiredSlider, bnd.showDaysExpiredState,
                this, R.plurals.settings_after_n_days,
                Static.EXPIRED_ITEMS_OFFSET,
                value -> value.get(subKeys),
                (slider, value, fromUser) ->
                        bnd.hideExpiredItemsByTimeSwitch.setTextColor(value == 0 ?
                                defaultTextColor :
                                wrapper.getColor(R.color.entry_settings_parameter_group_and_personal)), null);
    
        setSwitchChangeListener(
                bnd.hideExpiredItemsByTimeSwitch,
                bnd.hideExpiredItemsByTimeState, this,
                Static.HIDE_EXPIRED_ENTRIES_BY_TIME,
                value -> value.get(subKeys));
    
        setSwitchChangeListener(
                bnd.showOnLockSwitch,
                bnd.showOnLockState, this,
                Static.CALENDAR_SHOW_ON_LOCK,
                value -> value.get(subKeys));
    
        setSwitchChangeListener(
                bnd.hideByContentSwitch,
                bnd.hideByContentSwitchState, this,
                Static.HIDE_ENTRIES_BY_CONTENT,
                value -> value.get(subKeys));
    
        bnd.hideByContentField.setText(Static.HIDE_ENTRIES_BY_CONTENT_CONTENT.get(subKeys));
        if (currentListener != null) {
            bnd.hideByContentField.removeTextChangedListener(currentListener);
        }
        currentListener = new TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence s, int start, int count, int after) {
                //ignore
            }
        
            @Override
            public void onTextChanged(@NonNull CharSequence s, int start, int before, int count) {
                // sometimes this listener fires just on text field getting focus with count = 0
                if (count != 0) {
                    notifyParameterChanged(bnd.hideByContentFieldState, Static.HIDE_ENTRIES_BY_CONTENT_CONTENT.key, s.toString());
                }
            }
        
            @Override
            public void afterTextChanged(Editable s) {
                //ignore
            }
        };
        bnd.hideByContentField.addTextChangedListener(currentListener);
    }
    
    @Override
    public <T> void notifyParameterChanged(@NonNull TextView displayTo, @NonNull String parameterKey, @NonNull T value) {
        Static.putAny(prefKey + KEY_SEPARATOR + parameterKey, value);
        setStateIconColor(displayTo, parameterKey);
        // invalidate parameters on entries in the same calendar category / color
        if (event != null) {
            event.invalidateParameterOfConnectedEntries(parameterKey);
        }
    }
    
    @Override
    public void setStateIconColor(@NonNull TextView icon, @NonNull String parameterKey) {
        Integer keyIndex = getFirstValidKey(subKeys, parameterKey, (key, index) -> index);
        if (keyIndex == subKeys.size() - 1) {
            icon.setTextColor(wrapper.context.getColor(R.color.entry_settings_parameter_personal));
        } else if (keyIndex >= 0) {
            icon.setTextColor(wrapper.context.getColor(R.color.entry_settings_parameter_group));
        } else {
            icon.setTextColor(wrapper.context.getColor(R.color.entry_settings_parameter_default));
        }
    }
}
