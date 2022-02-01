package prototype.xd.scheduler;

import static prototype.xd.scheduler.MainActivity.preferences;
import static prototype.xd.scheduler.utilities.Keys.SERVICE_UPDATE_SIGNAL;
import static prototype.xd.scheduler.utilities.SystemCalendarUtils.getAllCalendars;

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
import prototype.xd.scheduler.views.settings.SystemCalendarSettings;

public class SettingsFragment extends Fragment {
    
    private AdaptiveBackgroundSettingsEntry adaptiveBackgroundSettingsEntry;
    public SystemCalendarSettings calendarSettingsDialogue;
    
    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        calendarSettingsDialogue = new SystemCalendarSettings(inflater.inflate(R.layout.entry_settings, container, false), null);
        return inflater.inflate(R.layout.fragment_settings, container, false);
    }
    
    @Override
    public void onDestroyView() {
        preferences.edit().putBoolean(SERVICE_UPDATE_SIGNAL, true).apply();
        super.onDestroyView();
    }
    
    @Override
    public void onDestroy() {
        adaptiveBackgroundSettingsEntry = null;
        calendarSettingsDialogue = null;
        super.onDestroy();
    }
    
    public void notifyBgSelected() {
        adaptiveBackgroundSettingsEntry.notifyBackgroundUpdated();
    }
    
    public void onViewCreated(@NonNull final View view, final Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);
        
        ArrayList<SettingsEntry> settingsEntries = new ArrayList<>();
        
        adaptiveBackgroundSettingsEntry = new AdaptiveBackgroundSettingsEntry(this);
        
        settingsEntries.add(new TitleBarSettingsEntry(getString(R.string.category_backgrounds)));
        settingsEntries.add(adaptiveBackgroundSettingsEntry);
        settingsEntries.add(new SwitchSettingsEntry(
                Keys.ADAPTIVE_COLOR_ENABLED, Keys.SETTINGS_DEFAULT_ADAPTIVE_COLOR_ENABLED,
                getString(R.string.settings_adaptive_color)));
        settingsEntries.add(new SeekBarSettingsEntry(0, 1000, Keys.SETTINGS_DEFAULT_ADAPTIVE_COLOR_BALANCE,
                Keys.ADAPTIVE_COLOR_BALANCE, R.string.settings_adaptive_color_balance));
        settingsEntries.add(new ColorSelectSettingsEntry(Keys.BG_COLOR, Keys.SETTINGS_DEFAULT_BG_COLOR,
                getString(R.string.settings_today_bg_color)));
        settingsEntries.add(new ColorSelectSettingsEntry(Keys.EXPIRED_BG_COLOR, Keys.SETTINGS_DEFAULT_EXPIRED_BG_COLOR,
                getString(R.string.settings_expired_bg_color)));
        settingsEntries.add(new ColorSelectSettingsEntry(Keys.UPCOMING_BG_COLOR, Keys.SETTINGS_DEFAULT_UPCOMING_BG_COLOR,
                getString(R.string.settings_upcoming_bg_color)));
        
        settingsEntries.add(new TitleBarSettingsEntry(getString(R.string.category_bevels)));
        settingsEntries.add(new ColorSelectSettingsEntry(Keys.BORDER_COLOR, Keys.SETTINGS_DEFAULT_BORDER_COLOR,
                getString(R.string.settings_today_bevel_color)));
        settingsEntries.add(new DiscreteSeekBarSettingsEntry(0, 15, Keys.SETTINGS_DEFAULT_BORDER_THICKNESS,
                Keys.BORDER_THICKNESS, R.string.settings_today_bevel_thickness));
        settingsEntries.add(new ColorSelectSettingsEntry(Keys.EXPIRED_BORDER_COLOR, Keys.SETTINGS_DEFAULT_EXPIRED_BORDER_COLOR,
                getString(R.string.settings_expired_bevel_color)));
        settingsEntries.add(new DiscreteSeekBarSettingsEntry(0, 15, Keys.SETTINGS_DEFAULT_EXPIRED_BORDER_THICKNESS,
                Keys.EXPIRED_BORDER_THICKNESS, R.string.settings_expired_bevel_thickness));
        settingsEntries.add(new ColorSelectSettingsEntry(Keys.UPCOMING_BORDER_COLOR, Keys.SETTINGS_DEFAULT_UPCOMING_BORDER_COLOR,
                getString(R.string.settings_upcoming_bevel_color)));
        settingsEntries.add(new DiscreteSeekBarSettingsEntry(0, 15, Keys.SETTINGS_DEFAULT_UPCOMING_BORDER_THICKNESS,
                Keys.UPCOMING_BORDER_THICKNESS, R.string.settings_upcoming_bevel_thickness));
        
        
        settingsEntries.add(new TitleBarSettingsEntry(getString(R.string.category_fonts)));
        settingsEntries.add(new ColorSelectSettingsEntry(Keys.FONT_COLOR, Keys.SETTINGS_DEFAULT_FONT_COLOR,
                getString(R.string.settings_today_font_color)));
        settingsEntries.add(new ColorSelectSettingsEntry(Keys.EXPIRED_FONT_COLOR, Keys.SETTINGS_DEFAULT_EXPIRED_FONT_COLOR,
                getString(R.string.settings_expired_font_color)));
        settingsEntries.add(new ColorSelectSettingsEntry(Keys.UPCOMING_FONT_COLOR, Keys.SETTINGS_DEFAULT_UPCOMING_FONT_COLOR,
                getString(R.string.settings_upcoming_font_color)));
        settingsEntries.add(new DiscreteSeekBarSettingsEntry(10, 30, Keys.SETTINGS_DEFAULT_FONT_SIZE,
                Keys.FONT_SIZE, R.string.settings_font_size));
        
        
        settingsEntries.add(new TitleBarSettingsEntry(getString(R.string.category_visibility)));
        settingsEntries.add(new DiscreteSeekBarSettingsEntry(0, 14, Keys.SETTINGS_DEFAULT_UPCOMING_ITEMS_OFFSET,
                Keys.UPCOMING_ITEMS_OFFSET, R.string.settings_show_days_upcoming));
        settingsEntries.add(new DiscreteSeekBarSettingsEntry(0, 14, Keys.SETTINGS_DEFAULT_EXPIRED_ITEMS_OFFSET,
                Keys.EXPIRED_ITEMS_OFFSET, R.string.settings_show_days_expired));
        settingsEntries.add(new SwitchSettingsEntry(
                Keys.SHOW_EXPIRED_COMPLETED_ITEMS_IN_LIST, Keys.SETTINGS_DEFAULT_SHOW_EXPIRED_COMPLETED_ITEMS_IN_LIST,
                getString(R.string.settings_show_expired_done_items_list)));
        settingsEntries.add(new SwitchSettingsEntry(
                Keys.SHOW_UPCOMING_COMPLETED_ITEMS_IN_LIST, Keys.SETTINGS_DEFAULT_SHOW_UPCOMING_COMPLETED_ITEMS_IN_LIST,
                getString(R.string.settings_show_upcoming_done_items_list)));
        settingsEntries.add(new SwitchSettingsEntry(
                Keys.SHOW_GLOBAL_ITEMS_LOCK, Keys.SETTINGS_DEFAULT_SHOW_GLOBAL_ITEMS_LOCK,
                getString(R.string.settings_show_global_items_lock)));
        settingsEntries.add(new SwitchSettingsEntry(
                Keys.ITEM_FULL_WIDTH_LOCK, Keys.SETTINGS_DEFAULT_ITEM_FULL_WIDTH_LOCK,
                getString(R.string.settings_max_rWidth_lock)));
        
        settingsEntries.add(new ResetButtonsSettingsEntry(this, view, savedInstanceState));
    
        SettingsListViewAdapter settingsListViewAdapter = new SettingsListViewAdapter(settingsEntries);
        ((ListView) view.findViewById(R.id.settingsList)).setAdapter(settingsListViewAdapter);
    
        ArrayList<SettingsEntry> settingsEntries_additional = new ArrayList<>();
        new Thread(() -> {
            settingsEntries_additional.add(new TitleBarSettingsEntry(getString(R.string.category_system_calendars)));
            ArrayList<SystemCalendar> calendars = getAllCalendars(view.getContext(), true);
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
    
                settingsEntries_additional.add(new CalendarAccountSettingsEntry(SettingsFragment.this, calendar0));
    
                for (int c = 0; c < calendar_group.size(); c++) {
                    SystemCalendar current_calendar = calendar_group.get(c);
                    settingsEntries_additional.add(new CalendarSettingsEntry(SettingsFragment.this, current_calendar));
                }
            }
            requireActivity().runOnUiThread(() -> {
                settingsEntries.addAll(settingsEntries_additional);
                settingsListViewAdapter.notifyDataSetChanged();
            });
        }).start();
    }
}