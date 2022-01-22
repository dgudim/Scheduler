package prototype.xd.scheduler.utilities;

import static prototype.xd.scheduler.MainActivity.preferences;
import static prototype.xd.scheduler.entities.TodoListEntry.ADAPTIVE_COLOR;
import static prototype.xd.scheduler.entities.TodoListEntry.ADAPTIVE_COLOR_BALANCE;
import static prototype.xd.scheduler.entities.TodoListEntry.BACKGROUND_COLOR;
import static prototype.xd.scheduler.entities.TodoListEntry.BEVEL_COLOR;
import static prototype.xd.scheduler.entities.TodoListEntry.BEVEL_SIZE;
import static prototype.xd.scheduler.entities.TodoListEntry.FONT_COLOR;
import static prototype.xd.scheduler.entities.TodoListEntry.PRIORITY;
import static prototype.xd.scheduler.entities.TodoListEntry.SHOW_DAYS_AFTER;
import static prototype.xd.scheduler.entities.TodoListEntry.SHOW_DAYS_BEFOREHAND;
import static prototype.xd.scheduler.entities.TodoListEntry.SHOW_ON_LOCK;
import static prototype.xd.scheduler.utilities.BitmapUtilities.createSolidColorCircle;
import static prototype.xd.scheduler.utilities.Keys.NEED_TO_RECONSTRUCT_BITMAP;
import static prototype.xd.scheduler.utilities.Utilities.addSeekBarChangeListener;
import static prototype.xd.scheduler.utilities.Utilities.addSwitchChangeListener;
import static prototype.xd.scheduler.utilities.Utilities.invokeColorDialogue;

import android.app.AlertDialog;
import android.content.Context;
import android.graphics.Color;
import android.view.View;
import android.widget.ImageView;
import android.widget.TextView;

import java.util.ArrayList;

import prototype.xd.scheduler.R;
import prototype.xd.scheduler.SettingsFragment;

public class SystemCalendarSettings {
    
    ArrayList<String> calendarSubKeys;
    
    TextView fontColor_view_state;
    TextView bgColor_view_state;
    TextView padColor_view_state;
    TextView adaptiveColor_state;
    TextView priority_state;
    TextView padSize_state;
    TextView show_on_lock_state;
    TextView adaptiveColor_bar_state;
    TextView showDaysBeforehand_bar_state;
    TextView showDaysAfter_bar_state;
    
    public SystemCalendarSettings(final Context context, final SettingsFragment fragment, View settingsView, final String calendar_key) {
        
        settingsView.findViewById(R.id.group_selector).setVisibility(View.GONE);
        
        calendarSubKeys = new ArrayList<>();
        String[] key_split = calendar_key.split("_");
        StringBuilder buffer = new StringBuilder();
        for (int i = 0; i < key_split.length; i++) {
            if (i != 0) {
                buffer.append('_');
            }
            buffer.append(key_split[i]);
            calendarSubKeys.add(buffer.toString());
        }
        
        initialise(calendar_key, context, fragment, settingsView);
        
        new AlertDialog.Builder(context).setView(settingsView).show();
    }
    
    private int getFirstValidKeyIndex(String parameter) {
        for (int i = calendarSubKeys.size() - 1; i >= 0; i--) {
            try {
                if (preferences.getString(calendarSubKeys.get(i) + "_" + parameter, null) != null) {
                    return i;
                }
            } catch (ClassCastException e) {
                return i;
            }
        }
        return -1;
    }
    
    private String getFirstValidKey(String parameter) {
        int index = getFirstValidKeyIndex(parameter);
        return index == -1 ? parameter : calendarSubKeys.get(index);
    }
    
    private void initialise(String calendarKey, final Context context, final SettingsFragment fragment, final View settingsView) {
        
        int fontColor = preferences.getInt(getFirstValidKey(Keys.TODAY_FONT_COLOR), Keys.SETTINGS_DEFAULT_TODAY_FONT_COLOR);
        int bgColor = preferences.getInt(getFirstValidKey(Keys.TODAY_BG_COLOR), Keys.SETTINGS_DEFAULT_TODAY_BG_COLOR);
        int bevelColor = preferences.getInt(getFirstValidKey(Keys.TODAY_BEVEL_COLOR), Keys.SETTINGS_DEFAULT_TODAY_BEVEL_COLOR);
        int bevelThickness = preferences.getInt(getFirstValidKey(Keys.TODAY_BEVEL_THICKNESS), Keys.SETTINGS_DEFAULT_TODAY_BEVEL_THICKNESS);
        
        int adaptiveColorBalance = preferences.getInt(getFirstValidKey(Keys.ADAPTIVE_COLOR_BALANCE), Keys.SETTINGS_DEFAULT_ADAPTIVE_COLOR_BALANCE);
        boolean adaptiveColorEnabled = preferences.getBoolean(getFirstValidKey(Keys.ADAPTIVE_COLOR_ENABLED), Keys.SETTINGS_DEFAULT_ADAPTIVE_COLOR_ENABLED);
        
        int dayOffset_right = preferences.getInt(getFirstValidKey(Keys.NEW_ITEMS_OFFSET), Keys.SETTINGS_DEFAULT_NEW_ITEMS_OFFSET);
        int dayOffset_left = preferences.getInt(getFirstValidKey(Keys.OLD_BEVEL_COLOR), Keys.SETTINGS_DEFAULT_OLD_ITEMS_OFFSET);
        
        ImageView fontColor_view = settingsView.findViewById(R.id.textColor);
        ImageView bgColor_view = settingsView.findViewById(R.id.backgroundColor);
        ImageView padColor_view = settingsView.findViewById(R.id.padColor);
        fontColor_view.setImageBitmap(createSolidColorCircle(fontColor));
        bgColor_view.setImageBitmap(createSolidColorCircle(bgColor));
        padColor_view.setImageBitmap(createSolidColorCircle(bevelColor));
        
        fontColor_view_state = settingsView.findViewById(R.id.font_color_state);
        bgColor_view_state = settingsView.findViewById(R.id.background_color_state);
        padColor_view_state = settingsView.findViewById(R.id.bevel_color_state);
        priority_state = settingsView.findViewById(R.id.priority_state);
        padSize_state = settingsView.findViewById(R.id.bevel_size_state);
        show_on_lock_state = settingsView.findViewById(R.id.show_on_lock_state);
        adaptiveColor_state = settingsView.findViewById(R.id.adaptive_color_state);
        adaptiveColor_bar_state = settingsView.findViewById(R.id.adaptive_color_balance_state);
        showDaysBeforehand_bar_state = settingsView.findViewById(R.id.days_beforehand_state);
        showDaysAfter_bar_state = settingsView.findViewById(R.id.days_after_state);
        
        updateAllIndicators();
        
        settingsView.findViewById(R.id.settingsResetButton).setOnClickListener(v -> {
            AlertDialog.Builder builder = new AlertDialog.Builder(context);
            builder.setTitle("Сбросить настройки?");
            
            builder.setPositiveButton("Да", (dialog, which) -> {
                // TODO: 22/1/2022 reset settings
                preferences.edit().putBoolean(NEED_TO_RECONSTRUCT_BITMAP, true).apply();
                initialise(calendarKey, context, fragment, settingsView);
            });
            builder.setNegativeButton("Нет", (dialog, which) -> dialog.dismiss());
            
            builder.show();
        });
        
        fontColor_view.setOnClickListener(v -> invokeColorDialogue(context,
                (ImageView) v, fontColor_view_state,
                this,
                calendarKey, FONT_COLOR, fontColor, true));
        
        bgColor_view.setOnClickListener(v -> invokeColorDialogue(context,
                (ImageView) v, bgColor_view_state,
                this,
                calendarKey, BACKGROUND_COLOR, bgColor, true));
        
        padColor_view.setOnClickListener(v -> invokeColorDialogue(context,
                (ImageView) v, padColor_view_state,
                this,
                calendarKey, BEVEL_COLOR, bevelColor, true));
        
        addSeekBarChangeListener(
                settingsView.findViewById(R.id.bevelThicknessDescription),
                settingsView.findViewById(R.id.bevelThicknessBar),
                padSize_state, this, fragment, R.string.settings_bevel_thickness,
                calendarKey, BEVEL_SIZE, bevelThickness);
        
        addSeekBarChangeListener(
                settingsView.findViewById(R.id.priorityDescription),
                settingsView.findViewById(R.id.priorityBar),
                priority_state, this, fragment, R.string.settings_priority,
                calendarKey, PRIORITY, 0);
        
        addSeekBarChangeListener(
                settingsView.findViewById(R.id.adaptive_color_balance_description),
                settingsView.findViewById(R.id.adaptive_color_balance_bar),
                adaptiveColor_bar_state, this, fragment, R.string.settings_adaptive_color_balance,
                calendarKey, ADAPTIVE_COLOR_BALANCE, adaptiveColorBalance);
        
        addSeekBarChangeListener(
                settingsView.findViewById(R.id.show_days_beforehand_description),
                settingsView.findViewById(R.id.show_days_beforehand_bar),
                showDaysBeforehand_bar_state, this, fragment, R.string.settings_show_days_beforehand,
                calendarKey, SHOW_DAYS_BEFOREHAND, dayOffset_right);
        
        addSeekBarChangeListener(
                settingsView.findViewById(R.id.show_days_after_description),
                settingsView.findViewById(R.id.show_days_after_bar),
                showDaysAfter_bar_state, this, fragment, R.string.settings_show_days_after,
                calendarKey, SHOW_DAYS_AFTER, dayOffset_left);
        
        addSwitchChangeListener(
                settingsView.findViewById(R.id.showOnLockSwitch),
                show_on_lock_state, this,
                calendarKey, SHOW_ON_LOCK, true);
        
        addSwitchChangeListener(
                settingsView.findViewById(R.id.adaptive_color_switch),
                adaptiveColor_state, this,
                calendarKey, ADAPTIVE_COLOR, adaptiveColorEnabled);
    }
    
    void setStateIconColor(TextView display, String parameter) {
        int keyIndex = getFirstValidKeyIndex(parameter);
        if (keyIndex == calendarSubKeys.size() - 1) {
            display.setTextColor(Color.GREEN);
        } else if (keyIndex > 0) {
            display.setTextColor(Color.YELLOW);
        }
    }
    
    void updateAllIndicators() {
        setStateIconColor(fontColor_view_state, FONT_COLOR);
        setStateIconColor(bgColor_view_state, BACKGROUND_COLOR);
        setStateIconColor(padColor_view_state, BEVEL_COLOR);
        setStateIconColor(padSize_state, BEVEL_SIZE);
        setStateIconColor(priority_state, PRIORITY);
        setStateIconColor(show_on_lock_state, SHOW_ON_LOCK);
        setStateIconColor(adaptiveColor_state, ADAPTIVE_COLOR);
        setStateIconColor(adaptiveColor_bar_state, ADAPTIVE_COLOR_BALANCE);
        setStateIconColor(showDaysBeforehand_bar_state, SHOW_DAYS_BEFOREHAND);
        setStateIconColor(showDaysAfter_bar_state, SHOW_DAYS_AFTER);
    }
}
