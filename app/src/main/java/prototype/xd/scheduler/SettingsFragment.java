package prototype.xd.scheduler;

import static prototype.xd.scheduler.utilities.SystemCalendarUtils.getAllCalendars;

import android.content.Context;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ListView;

import androidx.annotation.NonNull;
import androidx.fragment.app.Fragment;

import java.util.ArrayList;

import prototype.xd.scheduler.adapters.SettingsListViewAdapter;
import prototype.xd.scheduler.entities.calendars.SystemCalendar;
import prototype.xd.scheduler.entities.settingsEntries.AdaptiveBackgroundSettingsEntry;
import prototype.xd.scheduler.entities.settingsEntries.CalendarAccountSettingsEntry;
import prototype.xd.scheduler.entities.settingsEntries.CalendarSettingsEntry;
import prototype.xd.scheduler.entities.settingsEntries.ColorSelectSettingsEntry;
import prototype.xd.scheduler.entities.settingsEntries.DiscreteSeekBarSettingsEntry;
import prototype.xd.scheduler.entities.settingsEntries.ResetButtonsSettingsEntry;
import prototype.xd.scheduler.entities.settingsEntries.SeekBarSettingsEntry;
import prototype.xd.scheduler.entities.settingsEntries.SettingsEntry;
import prototype.xd.scheduler.entities.settingsEntries.SwitchSettingsEntry;
import prototype.xd.scheduler.entities.settingsEntries.TitleBarSettingsEntry;
import prototype.xd.scheduler.utilities.Keys;

public class SettingsFragment extends Fragment {
    
    public Context context;
    public ViewGroup rootViewGroup;
    private AdaptiveBackgroundSettingsEntry adaptiveBackgroundSettingsEntry;
    
    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        rootViewGroup = container;
        return inflater.inflate(R.layout.fragment_settings, container, false);
    }
    
    public void notifyBgSelected() {
        adaptiveBackgroundSettingsEntry.notifyBackgroundUpdated();
    }
    
    public void onViewCreated(@NonNull final View view, final Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);
        context = requireContext();
        
        ArrayList<SettingsEntry> settingsEntries = new ArrayList<>();
        
        adaptiveBackgroundSettingsEntry = new AdaptiveBackgroundSettingsEntry(this);
        
        settingsEntries.add(new TitleBarSettingsEntry(context.getString(R.string.category_backgrounds)));
        settingsEntries.add(adaptiveBackgroundSettingsEntry);
        settingsEntries.add(new SwitchSettingsEntry(
                Keys.ADAPTIVE_COLOR_ENABLED, Keys.SETTINGS_DEFAULT_ADAPTIVE_COLOR_ENABLED,
                context.getString(R.string.settings_adaptive_color)));
        settingsEntries.add(new SeekBarSettingsEntry(0, 1000, Keys.SETTINGS_DEFAULT_ADAPTIVE_COLOR_BALANCE,
                Keys.ADAPTIVE_COLOR_BALANCE, R.string.settings_adaptive_color_balance, this));
        settingsEntries.add(new ColorSelectSettingsEntry(Keys.TODAY_BG_COLOR, Keys.SETTINGS_DEFAULT_TODAY_BG_COLOR,
                context.getString(R.string.settings_today_bg_color), context));
        settingsEntries.add(new ColorSelectSettingsEntry(Keys.OLD_BG_COLOR, Keys.SETTINGS_DEFAULT_OLD_BG_COLOR,
                context.getString(R.string.settings_old_bg_color), context));
        settingsEntries.add(new ColorSelectSettingsEntry(Keys.NEW_BG_COLOR, Keys.SETTINGS_DEFAULT_NEW_BG_COLOR,
                context.getString(R.string.settings_new_bg_color), context));
        
        settingsEntries.add(new TitleBarSettingsEntry(context.getString(R.string.category_bevels)));
        settingsEntries.add(new ColorSelectSettingsEntry(Keys.TODAY_BEVEL_COLOR, Keys.SETTINGS_DEFAULT_TODAY_BEVEL_COLOR,
                context.getString(R.string.settings_today_bevel_color), context));
        settingsEntries.add(new DiscreteSeekBarSettingsEntry(0, 15, Keys.SETTINGS_DEFAULT_TODAY_BEVEL_THICKNESS,
                Keys.TODAY_BEVEL_THICKNESS, R.string.settings_today_bevel_thickness, this));
        settingsEntries.add(new ColorSelectSettingsEntry(Keys.OLD_BEVEL_COLOR, Keys.SETTINGS_DEFAULT_OLD_BEVEL_COLOR,
                context.getString(R.string.settings_old_bevel_color), context));
        settingsEntries.add(new DiscreteSeekBarSettingsEntry(0, 15, Keys.SETTINGS_DEFAULT_OLD_BEVEL_THICKNESS,
                Keys.OLD_BEVEL_THICKNESS, R.string.settings_old_bevel_thickness, this));
        settingsEntries.add(new ColorSelectSettingsEntry(Keys.NEW_BEVEL_COLOR, Keys.SETTINGS_DEFAULT_NEW_BEVEL_COLOR,
                context.getString(R.string.settings_new_bevel_color), context));
        settingsEntries.add(new DiscreteSeekBarSettingsEntry(0, 15, Keys.SETTINGS_DEFAULT_NEW_BEVEL_THICKNESS,
                Keys.NEW_BEVEL_THICKNESS, R.string.settings_new_bevel_thickness, this));
        
        
        settingsEntries.add(new TitleBarSettingsEntry(context.getString(R.string.category_fonts)));
        settingsEntries.add(new ColorSelectSettingsEntry(Keys.TODAY_FONT_COLOR, Keys.SETTINGS_DEFAULT_TODAY_FONT_COLOR,
                context.getString(R.string.settings_today_font_color), context));
        settingsEntries.add(new ColorSelectSettingsEntry(Keys.OLD_FONT_COLOR, Keys.SETTINGS_DEFAULT_OLD_FONT_COLOR,
                context.getString(R.string.settings_old_font_color), context));
        settingsEntries.add(new ColorSelectSettingsEntry(Keys.NEW_FONT_COLOR, Keys.SETTINGS_DEFAULT_NEW_FONT_COLOR,
                context.getString(R.string.settings_new_font_color), context));
        settingsEntries.add(new DiscreteSeekBarSettingsEntry(10, 30, Keys.SETTINGS_DEFAULT_FONT_SIZE,
                Keys.FONT_SIZE, R.string.settings_font_size, this));
        
        
        settingsEntries.add(new TitleBarSettingsEntry(context.getString(R.string.category_visibility)));
        settingsEntries.add(new DiscreteSeekBarSettingsEntry(0, 14, Keys.SETTINGS_DEFAULT_BEFOREHAND_ITEMS_OFFSET,
                Keys.BEFOREHAND_ITEMS_OFFSET, R.string.settings_show_days_beforehand, this));
        settingsEntries.add(new DiscreteSeekBarSettingsEntry(0, 14, Keys.SETTINGS_DEFAULT_AFTER_ITEMS_OFFSET,
                Keys.AFTER_ITEMS_OFFSET, R.string.settings_show_days_after, this));
        settingsEntries.add(new SwitchSettingsEntry(
                Keys.SHOW_OLD_COMPLETED_ITEMS_IN_LIST, Keys.SETTINGS_DEFAULT_SHOW_OLD_COMPLETED_ITEMS_IN_LIST,
                context.getString(R.string.settings_show_old_done_items_list)));
        settingsEntries.add(new SwitchSettingsEntry(
                Keys.SHOW_NEW_COMPLETED_ITEMS_IN_LIST, Keys.SETTINGS_DEFAULT_SHOW_NEW_COMPLETED_ITEMS_IN_LIST,
                context.getString(R.string.settings_show_new_done_items_list)));
        settingsEntries.add(new SwitchSettingsEntry(
                Keys.SHOW_GLOBAL_ITEMS_LOCK, Keys.SETTINGS_DEFAULT_SHOW_GLOBAL_ITEMS_LOCK,
                context.getString(R.string.settings_show_global_items_lock)));
        settingsEntries.add(new SwitchSettingsEntry(
                Keys.ITEM_FULL_WIDTH_LOCK, Keys.SETTINGS_DEFAULT_ITEM_FULL_WIDTH_LOCK,
                context.getString(R.string.settings_max_rWidth_lock)));
        
        settingsEntries.add(new ResetButtonsSettingsEntry(this, view, savedInstanceState));
    
        SettingsListViewAdapter settingsListViewAdapter = new SettingsListViewAdapter(settingsEntries, context);
        ((ListView) view.findViewById(R.id.settingsList)).setAdapter(settingsListViewAdapter);
        
        new Thread(() -> {
            settingsEntries.add(new TitleBarSettingsEntry(context.getString(R.string.category_system_calendars)));
            ArrayList<SystemCalendar> calendars = getAllCalendars(context);
            ArrayList<ArrayList<SystemCalendar>> calendars_sorted = new ArrayList<>();
            ArrayList<String> calendars_sorted_names = new ArrayList<>();

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

            for (int g = 0; g < calendars_sorted.size(); g++) {
                ArrayList<SystemCalendar> calendar_group = calendars_sorted.get(g);
                SystemCalendar calendar0 = calendar_group.get(0);
    
                settingsEntries.add(new CalendarAccountSettingsEntry(SettingsFragment.this, calendar0));
    
                for (int c = 0; c < calendar_group.size(); c++) {
                    SystemCalendar current_calendar = calendar_group.get(c);
                    settingsEntries.add(new CalendarSettingsEntry(SettingsFragment.this, current_calendar));
                }
            }
            requireActivity().runOnUiThread(settingsListViewAdapter::notifyDataSetChanged);
        }).start();
    }
}