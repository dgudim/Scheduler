package prototype.xd.scheduler.views.settings;

import static prototype.xd.scheduler.utilities.DialogUtilities.displayConfirmationDialogue;
import static prototype.xd.scheduler.utilities.Keys.KEY_SEPARATOR;
import static prototype.xd.scheduler.utilities.Keys.VISIBLE;
import static prototype.xd.scheduler.utilities.Keys.getFirstValidKey;
import static prototype.xd.scheduler.utilities.SystemCalendarUtils.calendarKeyToReadable;
import static prototype.xd.scheduler.utilities.Utilities.setSliderChangeListener;
import static prototype.xd.scheduler.utilities.Utilities.setSwitchChangeListener;

import android.content.SharedPreferences;
import android.text.Editable;
import android.text.TextWatcher;
import android.view.View;
import android.widget.TextView;

import androidx.annotation.ColorInt;
import androidx.annotation.IntRange;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import java.util.List;
import java.util.Set;

import prototype.xd.scheduler.R;
import prototype.xd.scheduler.entities.SystemCalendarEvent;
import prototype.xd.scheduler.utilities.ContextWrapper;
import prototype.xd.scheduler.utilities.DialogUtilities;
import prototype.xd.scheduler.utilities.Keys;
import prototype.xd.scheduler.utilities.TodoEntryManager;

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
    
    public SystemCalendarSettings(@NonNull final ContextWrapper wrapper,
                                  @Nullable final TodoEntryManager todoEntryManager) {
        super(wrapper, todoEntryManager);
        bnd.groupSelector.setVisibility(View.GONE);
    }
    
    @NonNull
    @Override
    public EntryPreviewContainer getEntryPreviewContainer() {
        return new EntryPreviewContainer(wrapper, bnd.previewContainer, true) {
            @ColorInt
            @Override
            protected int currentFontColorGetter() {
                return Keys.FONT_COLOR.CURRENT.get(subKeys);
            }
            
            @ColorInt
            @Override
            protected int currentBgColorGetter() {
                return Keys.BG_COLOR.CURRENT.getOnlyBySubKeys(subKeys, Keys.SETTINGS_DEFAULT_CALENDAR_EVENT_BG_COLOR.applyAsInt(color));
            }
            
            @ColorInt
            @Override
            protected int currentBorderColorGetter() {
                return Keys.BORDER_COLOR.CURRENT.get(subKeys);
            }
            
            @Override
            protected int currentBorderThicknessGetter() {
                return Keys.BORDER_THICKNESS.CURRENT.get(subKeys);
            }
            
            @IntRange(from = 0, to = 10)
            @Override
            protected int adaptiveColorBalanceGetter() {
                return Keys.ADAPTIVE_COLOR_BALANCE.get(subKeys);
            }
        };
    }
    
    public void show(@NonNull final String prefKey, @NonNull final List<String> subKeys, @ColorInt int color) {
        event = null;
        initialize(prefKey, subKeys, color);
        dialog.show();
    }
    
    public void show(@NonNull final SystemCalendarEvent event) {
        this.event = event;
        initialize(event.getPrefKey(), event.getSubKeys(), event.color);
        dialog.show();
    }
    
    private void initialize(@NonNull final String prefKey, @NonNull final List<String> subKeys, @ColorInt int color) {
        
        bnd.entrySettingsTitle.setText(calendarKeyToReadable(wrapper.context, prefKey));
        
        this.prefKey = prefKey;
        this.color = color;
        this.subKeys = subKeys;
        
        updateAllIndicators();
        entryPreviewContainer.refreshAll(true);
        
        bnd.settingsResetButton.setOnClickListener(v ->
                displayConfirmationDialogue(wrapper,
                        R.string.reset_settings_prompt, R.string.reset_calendar_settings_description,
                        R.string.cancel, R.string.reset, v1 -> {
                            Set<String> preferenceKeys = Keys.getAll().keySet();
                            SharedPreferences.Editor editor = Keys.edit();
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
                            initialize(prefKey, subKeys, color);
                        }));
        
        bnd.currentFontColorSelector.setOnClickListener(view -> DialogUtilities.displayColorPicker(
                wrapper,
                bnd.fontColorState, this,
                Keys.FONT_COLOR.CURRENT,
                value -> value.get(this.subKeys)));
        
        bnd.currentBackgroundColorSelector.setOnClickListener(view -> DialogUtilities.displayColorPicker(
                wrapper,
                bnd.backgroundColorState, this,
                Keys.BG_COLOR.CURRENT,
                value -> value.getOnlyBySubKeys(this.subKeys, Keys.SETTINGS_DEFAULT_CALENDAR_EVENT_BG_COLOR.applyAsInt(color))));
        
        bnd.currentBorderColorSelector.setOnClickListener(view -> DialogUtilities.displayColorPicker(
                wrapper,
                bnd.borderColorState, this,
                Keys.BORDER_COLOR.CURRENT,
                value -> value.get(this.subKeys)));
        
        setSliderChangeListener(
                bnd.borderThicknessDescription,
                bnd.borderThicknessSlider, bnd.borderThicknessState,
                this, R.string.settings_border_thickness,
                Keys.BORDER_THICKNESS.CURRENT,
                value -> value.get(this.subKeys),
                (slider, value, fromUser) -> entryPreviewContainer.setCurrentPreviewBorderThickness((int) value));
        
        setSliderChangeListener(
                bnd.priorityDescription,
                bnd.prioritySlider, bnd.priorityState,
                this, R.string.settings_priority,
                Keys.PRIORITY,
                value -> value.get(this.subKeys), null);
        
        setSliderChangeListener(
                bnd.adaptiveColorBalanceDescription,
                bnd.adaptiveColorBalanceSlider, bnd.adaptiveColorBalanceState,
                this, R.string.settings_adaptive_color_balance,
                Keys.ADAPTIVE_COLOR_BALANCE,
                value -> value.get(this.subKeys),
                (slider, value, fromUser) -> entryPreviewContainer.setPreviewAdaptiveColorBalance((int) value));
        
        setSliderChangeListener(
                bnd.showDaysUpcomingDescription,
                bnd.showDaysUpcomingSlider, bnd.showDaysUpcomingState,
                this, R.plurals.settings_in_n_days,
                Keys.UPCOMING_ITEMS_OFFSET,
                value -> value.get(this.subKeys), null);
        
        setSliderChangeListener(
                bnd.showDaysExpiredDescription,
                bnd.showDaysExpiredSlider, bnd.showDaysExpiredState,
                this, R.plurals.settings_after_n_days,
                Keys.EXPIRED_ITEMS_OFFSET,
                value -> value.get(this.subKeys),
                (slider, value, fromUser) ->
                        bnd.hideExpiredItemsByTimeSwitch.setTextColor(value == 0 ?
                                defaultTextColor :
                                wrapper.getColor(R.color.entry_settings_parameter_group_and_personal)), null);
        
        setSwitchChangeListener(
                bnd.hideExpiredItemsByTimeSwitch,
                bnd.hideExpiredItemsByTimeState, this,
                Keys.HIDE_EXPIRED_ENTRIES_BY_TIME,
                value -> value.get(this.subKeys));
        
        setSwitchChangeListener(
                bnd.showOnLockSwitch,
                bnd.showOnLockState, this,
                Keys.CALENDAR_SHOW_ON_LOCK,
                value -> value.get(this.subKeys));
        
        setSwitchChangeListener(
                bnd.hideByContentSwitch,
                bnd.hideByContentSwitchState, this,
                Keys.HIDE_ENTRIES_BY_CONTENT,
                value -> value.get(this.subKeys));
        
        bnd.hideByContentField.setText(Keys.HIDE_ENTRIES_BY_CONTENT_CONTENT.get(this.subKeys));
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
                    notifyParameterChanged(bnd.hideByContentFieldState, Keys.HIDE_ENTRIES_BY_CONTENT_CONTENT.key, s.toString());
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
        Keys.putAny(prefKey + KEY_SEPARATOR + parameterKey, value);
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
