package prototype.xd.scheduler;

import static prototype.xd.scheduler.MainActivity.preferences;
import static prototype.xd.scheduler.calendarUtilities.SystemCalendarUtils.getAllCalendars;
import static prototype.xd.scheduler.utilities.BackgroundChooser.defaultBackgroundName;
import static prototype.xd.scheduler.utilities.BitmapUtilities.createSolidColorCircle;
import static prototype.xd.scheduler.utilities.DateManager.availableDays;
import static prototype.xd.scheduler.utilities.Utilities.addSeekBarChangeListener;
import static prototype.xd.scheduler.utilities.Utilities.addSwitchChangeListener;
import static prototype.xd.scheduler.utilities.Utilities.invokeColorDialogue;
import static prototype.xd.scheduler.utilities.Utilities.rootDir;

import android.app.AlertDialog;
import android.content.Context;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.fragment.app.Fragment;

import java.io.File;
import java.util.ArrayList;

import prototype.xd.scheduler.calendarUtilities.SystemCalendar;
import prototype.xd.scheduler.utilities.Keys;

@SuppressWarnings({"ResultOfMethodCallIgnored", "ApplySharedPref"})
public class SettingsFragment extends Fragment {
    
    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        return inflater.inflate(R.layout.fragment_settings, container, false);
    }
    
    public void onViewCreated(@NonNull final View view, final Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);
        Context context = requireContext();
        
        view.findViewById(R.id.resetBgButton).setOnClickListener(view1 -> {
            AlertDialog.Builder builder = new AlertDialog.Builder(context);
            builder.setTitle("Удалить все сохраненные фоны?");
            
            builder.setPositiveButton("Да", (dialog, which) -> {
                new File(rootDir, defaultBackgroundName).delete();
                for (int i = 0; i < 7; i++) {
                    new File(rootDir, availableDays[i] + ".png").delete();
                }
            });
            builder.setNegativeButton("Нет", (dialog, which) -> dialog.dismiss());
            
            builder.show();
        });
        
        ImageView todayBgColor = view.findViewById(R.id.defaultColor);
        ImageView todayBevelColor = view.findViewById(R.id.defaultBevelColor);
        ImageView todayFontColor = view.findViewById(R.id.defaultFontColor);
        
        ImageView oldBgColor = view.findViewById(R.id.defaultOldColor);
        ImageView oldBevelColor = view.findViewById(R.id.defaultOldBevelColor);
        ImageView oldFontColor = view.findViewById(R.id.oldFontColor);
        
        ImageView newBgColor = view.findViewById(R.id.newColor);
        ImageView newBevelColor = view.findViewById(R.id.newBevelColor);
        ImageView newFontColor = view.findViewById(R.id.newFontColor);
        
        todayBgColor.setImageBitmap(createSolidColorCircle(preferences.getInt(Keys.TODAY_BG_COLOR, Keys.SETTINGS_DEFAULT_TODAY_BG_COLOR)));
        oldBgColor.setImageBitmap(createSolidColorCircle(preferences.getInt(Keys.OLD_BG_COLOR, Keys.SETTINGS_DEFAULT_OLD_BG_COLOR)));
        newBgColor.setImageBitmap(createSolidColorCircle(preferences.getInt(Keys.NEW_BG_COLOR, Keys.SETTINGS_DEFAULT_NEW_BG_COLOR)));
        
        todayBevelColor.setImageBitmap(createSolidColorCircle(preferences.getInt(Keys.TODAY_BEVEL_COLOR, Keys.SETTINGS_DEFAULT_TODAY_BEVEL_COLOR)));
        oldBevelColor.setImageBitmap(createSolidColorCircle(preferences.getInt(Keys.OLD_BEVEL_COLOR, Keys.SETTINGS_DEFAULT_OLD_BEVEL_COLOR)));
        newBevelColor.setImageBitmap(createSolidColorCircle(preferences.getInt(Keys.NEW_BEVEL_COLOR, Keys.SETTINGS_DEFAULT_NEW_BEVEL_COLOR)));
        
        todayFontColor.setImageBitmap(createSolidColorCircle(preferences.getInt(Keys.TODAY_FONT_COLOR, Keys.SETTINGS_DEFAULT_TODAY_FONT_COLOR)));
        oldFontColor.setImageBitmap(createSolidColorCircle(preferences.getInt(Keys.OLD_FONT_COLOR, Keys.SETTINGS_DEFAULT_OLD_FONT_COLOR)));
        newFontColor.setImageBitmap(createSolidColorCircle(preferences.getInt(Keys.NEW_FONT_COLOR, Keys.SETTINGS_DEFAULT_NEW_FONT_COLOR)));
        
        todayBgColor.setOnClickListener(v -> invokeColorDialogue(Keys.TODAY_BG_COLOR, (ImageView) v, Keys.SETTINGS_DEFAULT_TODAY_BG_COLOR, context));
        oldBgColor.setOnClickListener(v -> invokeColorDialogue(Keys.OLD_BG_COLOR, (ImageView) v, Keys.SETTINGS_DEFAULT_OLD_BG_COLOR, context));
        newBgColor.setOnClickListener(v -> invokeColorDialogue(Keys.NEW_BG_COLOR, (ImageView) v, Keys.SETTINGS_DEFAULT_NEW_BG_COLOR, context));
        
        todayBevelColor.setOnClickListener(v -> invokeColorDialogue(Keys.TODAY_BEVEL_COLOR, (ImageView) v, Keys.SETTINGS_DEFAULT_TODAY_BEVEL_COLOR, context));
        oldBevelColor.setOnClickListener(v -> invokeColorDialogue(Keys.OLD_BEVEL_COLOR, (ImageView) v, Keys.SETTINGS_DEFAULT_OLD_BEVEL_COLOR, context));
        newBevelColor.setOnClickListener(v -> invokeColorDialogue(Keys.NEW_BEVEL_COLOR, (ImageView) v, Keys.SETTINGS_DEFAULT_NEW_BEVEL_COLOR, context));
        
        todayFontColor.setOnClickListener(v -> invokeColorDialogue(Keys.TODAY_FONT_COLOR, (ImageView) v, Keys.SETTINGS_DEFAULT_TODAY_FONT_COLOR, context));
        oldFontColor.setOnClickListener(v -> invokeColorDialogue(Keys.OLD_FONT_COLOR, (ImageView) v, Keys.SETTINGS_DEFAULT_OLD_FONT_COLOR, context));
        newFontColor.setOnClickListener(v -> invokeColorDialogue(Keys.NEW_FONT_COLOR, (ImageView) v, Keys.SETTINGS_DEFAULT_NEW_FONT_COLOR, context));
        
        addSeekBarChangeListener(
                view.findViewById(R.id.textSizeDescription),
                view.findViewById(R.id.fontSizeBar),
                Keys.FONT_SIZE, Keys.SETTINGS_DEFAULT_FONT_SIZE, R.string.settings_font_size, this);
        
        addSeekBarChangeListener(
                view.findViewById(R.id.defaultBevelThickness),
                view.findViewById(R.id.defaultBevelThicknessBar),
                Keys.TODAY_BEVEL_THICKNESS, Keys.SETTINGS_DEFAULT_TODAY_BEVEL_THICKNESS, R.string.settings_default_bevel_thickness, this);
        
        addSeekBarChangeListener(
                view.findViewById(R.id.oldBevelThickness),
                view.findViewById(R.id.oldBevelThicknessBar),
                Keys.OLD_BEVEL_THICKNESS, Keys.SETTINGS_DEFAULT_OLD_BEVEL_THICKNESS, R.string.settings_old_bevel_thickness, this);
        
        addSeekBarChangeListener(
                view.findViewById(R.id.newBevelThickness),
                view.findViewById(R.id.newBevelThicknessBar),
                Keys.NEW_BEVEL_THICKNESS, Keys.SETTINGS_DEFAULT_NEW_BEVEL_THICKNESS, R.string.settings_new_bevel_thickness, this);
        
        addSeekBarChangeListener(
                view.findViewById(R.id.adaptive_color_balance_description),
                view.findViewById(R.id.adaptive_color_balance_bar),
                Keys.ADAPTIVE_COLOR_BALANCE, Keys.SETTINGS_DEFAULT_ADAPTIVE_COLOR_BALANCE, R.string.settings_adaptive_color_balance, this);
        
        addSeekBarChangeListener(
                view.findViewById(R.id.show_days_beforehand_description),
                view.findViewById(R.id.show_days_beforehand_bar),
                Keys.NEW_ITEMS_OFFSET, Keys.SETTINGS_DEFAULT_NEW_ITEMS_OFFSET, R.string.settings_show_days_beforehand, this);
        
        addSeekBarChangeListener(
                view.findViewById(R.id.show_days_after_description),
                view.findViewById(R.id.show_days_after_bar),
                Keys.OLD_ITEMS_OFFSET, Keys.SETTINGS_DEFAULT_OLD_ITEMS_OFFSET, R.string.settings_show_days_after, this);
        
        addSwitchChangeListener(view.findViewById(R.id.show_old_done_tasks_switch), Keys.SHOW_OLD_COMPLETED_ITEMS_IN_LIST, Keys.SETTINGS_DEFAULT_SHOW_OLD_COMPLETED_ITEMS_IN_LIST, null);
        addSwitchChangeListener(view.findViewById(R.id.show_new_done_tasks_switch), Keys.SHOW_NEW_COMPLETED_ITEMS_IN_LIST, Keys.SETTINGS_DEFAULT_SHOW_NEW_COMPLETED_ITEMS_IN_LIST, null);
        addSwitchChangeListener(view.findViewById(R.id.show_global_Items_lock_switch), Keys.SHOW_GLOBAL_ITEMS_LOCK, Keys.SETTINGS_DEFAULT_SHOW_OLD_COMPLETED_ITEMS_IN_LIST, null);
        addSwitchChangeListener(view.findViewById(R.id.item_full_width_switch), Keys.ITEM_FULL_WIDTH_LOCK, Keys.SETTINGS_DEFAULT_ITEM_FULL_WIDTH_LOCK, null);
        addSwitchChangeListener(view.findViewById(R.id.adaptive_color_switch), Keys.ADAPTIVE_COLOR_ENABLED, Keys.SETTINGS_DEFAULT_ADAPTIVE_COLOR_ENABLED, null);
        addSwitchChangeListener(view.findViewById(R.id.adaptive_background_switch), Keys.ADAPTIVE_BACKGROUND_ENABLED, Keys.SETTINGS_DEFAULT_ADAPTIVE_BACKGROUND_ENABLED, null);
        
        view.findViewById(R.id.resetSettingsButton).setOnClickListener(v -> {
            AlertDialog.Builder builder = new AlertDialog.Builder(context);
            builder.setTitle("Сбросить настройки?");
            
            builder.setPositiveButton("Да", (dialog, which) -> {
                preferences.edit().clear().commit();
                SettingsFragment.this.onViewCreated(view, savedInstanceState);
            });
            builder.setNegativeButton("Нет", (dialog, which) -> dialog.dismiss());
            
            builder.show();
        });
        
        LinearLayout calendar_settings_container = view.findViewById(R.id.calendar_settings_container);
        LayoutInflater inflater = LayoutInflater.from(context);
        
        ArrayList<SystemCalendar> calendars = getAllCalendars(context.getContentResolver());
        
        ArrayList<ArrayList<SystemCalendar>> calendars_sorted = new ArrayList<>();
        ArrayList<String> calendars_sorted_names = new ArrayList<>();
        
        ArrayList<View> calendar_views = new ArrayList<>();
        
        for (int i = 0; i < calendars.size(); i++) {
            SystemCalendar calendar = calendars.get(i);
            if (calendars_sorted_names.contains(calendar.account_name)) {
                calendars_sorted.get(calendars_sorted_names.indexOf(calendar.account_name)).add(calendar);
            } else {
                ArrayList<SystemCalendar> calendar_group = new ArrayList<>();
                calendar_group.add(calendar);
                calendars_sorted.add(calendar_group);
                calendars_sorted_names.add(calendar.account_name);
            }
        }
        
        new Thread(() -> {
            for (int g = 0; g < calendars_sorted.size(); g++) {
                ArrayList<SystemCalendar> calendar_group = calendars_sorted.get(g);
                SystemCalendar calendar0 = calendar_group.get(0);
                
                View acc_view = inflater.inflate(R.layout.account_entry, calendar_settings_container, false);
                ((TextView) acc_view.findViewById(R.id.calendar_name)).setText(calendars_sorted_names.get(g));
                ((TextView) acc_view.findViewById(R.id.account_type)).setText(calendar0.account_type);
                ((ImageView) acc_view.findViewById(R.id.calendar_color)).setImageBitmap(createSolidColorCircle(calendar0.color));
                calendar_views.add(acc_view);
                
                for (int c = 0; c < calendar_group.size(); c++) {
                    SystemCalendar current_calendar = calendar_group.get(c);
                    
                    View c_view = inflater.inflate(R.layout.calendar_entry, calendar_settings_container, false);
                    ((TextView) c_view.findViewById(R.id.calendar_name)).setText(current_calendar.name);
                    ((ImageView) c_view.findViewById(R.id.calendar_icon)).setImageBitmap(createSolidColorCircle(current_calendar.color));
                    calendar_views.add(c_view);
                    
                    for(int c_col = 0; c_col < current_calendar.availableEventColors.size(); c_col++){
                        View c_col_view = inflater.inflate(R.layout.calendar_entry, calendar_settings_container, false);
                        ((ImageView) c_col_view.findViewById(R.id.calendar_icon)).setImageBitmap(createSolidColorCircle(current_calendar.availableEventColors.get(c_col)));
                        calendar_views.add(c_col_view);
                    }
                }
            }
            requireActivity().runOnUiThread(() -> {
                for (View c_view : calendar_views) {
                    calendar_settings_container.addView(c_view);
                }
                calendar_views.clear();
            });
        }).start();
    }
}