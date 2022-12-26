package prototype.xd.scheduler.views.settings;

import static prototype.xd.scheduler.utilities.DialogUtilities.displayConfirmationDialogue;
import static prototype.xd.scheduler.utilities.Keys.getFirstValidKeyIndex;
import static prototype.xd.scheduler.utilities.SystemCalendarUtils.calendarKeyToReadable;
import static prototype.xd.scheduler.utilities.SystemCalendarUtils.generateSubKeysFromCalendarKey;
import static prototype.xd.scheduler.utilities.Utilities.setSliderChangeListener;
import static prototype.xd.scheduler.utilities.Utilities.setSwitchChangeListener;

import android.content.Context;
import android.content.SharedPreferences;
import android.text.Editable;
import android.text.TextWatcher;
import android.view.View;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.lifecycle.Lifecycle;

import java.util.List;
import java.util.Set;

import prototype.xd.scheduler.R;
import prototype.xd.scheduler.entities.TodoListEntry;
import prototype.xd.scheduler.utilities.DialogUtilities;
import prototype.xd.scheduler.utilities.Keys;
import prototype.xd.scheduler.utilities.TodoListEntryManager;

public class SystemCalendarSettings extends PopupSettingsView {
    
    // if called from regular settings
    private List<String> calendarSubKeys;
    private String calendarKey;
    
    // if called from main screen
    private TodoListEntry todoListEntry;
    
    private TextWatcher currentListener;
    
    public SystemCalendarSettings(@Nullable final TodoListEntryManager todoListEntryManager,
                                  @NonNull final Context context,
                                  @NonNull final Lifecycle lifecycle) {
        super(context, todoListEntryManager, lifecycle);
        bnd.groupSelector.setVisibility(View.GONE);
    }
    
    public void show(final String calendarKey, int eventColor) {
        initialize(calendarKey, eventColor);
        dialog.show();
    }
    
    public void show(final TodoListEntry entry) {
        this.todoListEntry = entry;
        initialize(entry.event.getKey(), entry.event.color);
        dialog.show();
    }
    
    private void initialize(final String calendarKey, int eventColor) {
        
        bnd.entrySettingsTitle.setText(calendarKeyToReadable(dialog.getContext(), calendarKey));
        
        this.calendarKey = calendarKey;
        calendarSubKeys = generateSubKeysFromCalendarKey(calendarKey);
        
        updatePreviews(
                Keys.FONT_COLOR.get(calendarSubKeys),
                Keys.BG_COLOR.get(calendarSubKeys, Keys.SETTINGS_DEFAULT_CALENDAR_EVENT_BG_COLOR.apply(eventColor)),
                Keys.BORDER_COLOR.get(calendarSubKeys),
                Keys.BORDER_THICKNESS.get(calendarSubKeys));
        
        updateAllIndicators();
        
        bnd.settingsResetButton.setOnClickListener(v ->
                displayConfirmationDialogue(v.getContext(), lifecycle,
                        R.string.reset_settings_prompt, R.string.reset_calendar_settings_description,
                        R.string.cancel, R.string.reset, v1 -> {
                            Set<String> preferenceKeys = Keys.getAll().keySet();
                            SharedPreferences.Editor editor = Keys.edit();
                            for (String preferenceKey : preferenceKeys) {
                                if (preferenceKey.startsWith(calendarKey)) {
                                    editor.remove(preferenceKey);
                                }
                            }
                            editor.apply();
                            if (todoListEntry != null) {
                                todoListEntry.event.invalidateAllParametersOfConnectedEntries();
                            }
                            initialize(calendarKey, eventColor);
                        }));
        
        bnd.fontColorSelector.setOnClickListener(view -> DialogUtilities.invokeColorDialog(
                context, lifecycle,
                bnd.fontColorState, this,
                Keys.FONT_COLOR,
                value -> value.get(calendarSubKeys)));
        
        bnd.backgroundColorSelector.setOnClickListener(view -> DialogUtilities.invokeColorDialog(
                context, lifecycle,
                bnd.backgroundColorState, this,
                Keys.BG_COLOR,
                value -> value.get(calendarSubKeys)));
        
        bnd.borderColorSelector.setOnClickListener(view -> DialogUtilities.invokeColorDialog(
                context, lifecycle,
                bnd.borderColorState, this,
                Keys.BORDER_COLOR,
                value -> value.get(calendarSubKeys)));
        
        setSliderChangeListener(
                bnd.borderThicknessDescription,
                bnd.borderThicknessBar, bnd.borderThicknessState,
                this, bnd.previewBorder, R.string.settings_border_thickness,
                Keys.BORDER_THICKNESS,
                value -> value.get(calendarSubKeys));
        
        setSliderChangeListener(
                bnd.priorityDescription,
                bnd.priorityBar, bnd.priorityState,
                this, null, R.string.settings_priority,
                Keys.PRIORITY,
                value -> value.get(calendarSubKeys));
        
        setSliderChangeListener(
                bnd.adaptiveColorBalanceDescription,
                bnd.adaptiveColorBalanceBar, bnd.adaptiveColorBalanceState,
                this, null, R.string.settings_adaptive_color_balance,
                Keys.ADAPTIVE_COLOR_BALANCE,
                value -> value.get(calendarSubKeys));
        
        setSliderChangeListener(
                bnd.showDaysUpcomingDescription,
                bnd.showDaysUpcomingBar, bnd.showDaysUpcomingState,
                this, null, R.plurals.settings_show_days_upcoming,
                Keys.UPCOMING_ITEMS_OFFSET,
                value -> value.get(calendarSubKeys));
        
        setSliderChangeListener(
                bnd.showDaysExpiredDescription,
                bnd.showDaysExpiredBar, bnd.showDaysExpiredState,
                this, null, R.plurals.settings_show_days_expired,
                Keys.EXPIRED_ITEMS_OFFSET,
                value -> value.get(calendarSubKeys),
                (slider, value, fromUser) ->
                        bnd.hideExpiredItemsByTimeSwitch.setTextColor(value == 0 ?
                                defaultTextColor :
                                slider.getContext().getColor(R.color.entry_settings_parameter_group_and_personal)), null);
        
        setSwitchChangeListener(
                bnd.hideExpiredItemsByTimeSwitch,
                bnd.hideExpiredItemsByTimeState, this,
                Keys.HIDE_EXPIRED_ENTRIES_BY_TIME,
                value -> value.get(calendarSubKeys));
        
        setSwitchChangeListener(
                bnd.showOnLockSwitch,
                bnd.showOnLockState, this,
                Keys.CALENDAR_SHOW_ON_LOCK,
                value -> value.get(calendarSubKeys));
        
        setSwitchChangeListener(
                bnd.hideByContentSwitch,
                bnd.hideByContentSwitchState, this,
                Keys.HIDE_ENTRIES_BY_CONTENT,
                value -> value.get(calendarSubKeys));
        
        bnd.hideByContentField.setText(Keys.HIDE_ENTRIES_BY_CONTENT_CONTENT.get(calendarSubKeys));
        if (currentListener != null) {
            bnd.hideByContentField.removeTextChangedListener(currentListener);
        }
        currentListener = new TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence s, int start, int count, int after) {
                //ignore
            }
            
            @Override
            public void onTextChanged(CharSequence s, int start, int before, int count) {
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
    public <T> void notifyParameterChanged(TextView displayTo, String parameterKey, T value) {
        Keys.putAny(calendarKey + "_" + parameterKey, value);
        setStateIconColor(displayTo, parameterKey);
        // invalidate parameters on entries in the same calendar category / color
        if (todoListEntry != null) {
            todoListEntry.event.invalidateParameterOfConnectedEntries(parameterKey);
        }
    }
    
    @Override
    public void setStateIconColor(TextView display, String parameterKey) {
        int keyIndex = getFirstValidKeyIndex(calendarSubKeys, parameterKey);
        if (keyIndex == calendarSubKeys.size() - 1) {
            display.setTextColor(display.getContext().getColor(R.color.entry_settings_parameter_personal));
        } else if (keyIndex >= 0) {
            display.setTextColor(display.getContext().getColor(R.color.entry_settings_parameter_group));
        } else {
            display.setTextColor(display.getContext().getColor(R.color.entry_settings_parameter_default));
        }
    }
}
