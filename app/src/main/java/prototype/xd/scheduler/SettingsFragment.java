package prototype.xd.scheduler;

import static prototype.xd.scheduler.MainActivity.preferences_service;
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
import prototype.xd.scheduler.entities.settingsEntries.AppThemeSelectorEntry;
import prototype.xd.scheduler.entities.settingsEntries.CalendarAccountSettingsEntry;
import prototype.xd.scheduler.entities.settingsEntries.CalendarSettingsEntry;
import prototype.xd.scheduler.entities.settingsEntries.CompoundCustomizationEntry;
import prototype.xd.scheduler.entities.settingsEntries.DiscreteSeekBarSettingsEntry;
import prototype.xd.scheduler.entities.settingsEntries.ResetButtonSettingsEntry;
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
        calendarSettingsDialogue = new SystemCalendarSettings(null, inflater.inflate(R.layout.entry_settings, container, false));
        return inflater.inflate(R.layout.fragment_settings, container, false);
    }
    
    @Override
    public void onDestroyView() {
        preferences_service.edit().putBoolean(SERVICE_UPDATE_SIGNAL, true).apply();
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
        
        settingsEntries.add(new TitleBarSettingsEntry(getString(R.string.category_appearance)));
        settingsEntries.add(new AppThemeSelectorEntry());
        settingsEntries.add(adaptiveBackgroundSettingsEntry);
        settingsEntries.add(new SwitchSettingsEntry(
                Keys.ADAPTIVE_COLOR_ENABLED, Keys.SETTINGS_DEFAULT_ADAPTIVE_COLOR_ENABLED,
                getString(R.string.settings_adaptive_color)));
        settingsEntries.add(new SeekBarSettingsEntry(0, 1000, Keys.SETTINGS_DEFAULT_ADAPTIVE_COLOR_BALANCE,
                Keys.ADAPTIVE_COLOR_BALANCE, R.string.settings_adaptive_color_balance));
        settingsEntries.add(new CompoundCustomizationEntry());
        settingsEntries.add(new DiscreteSeekBarSettingsEntry(10, 30, Keys.SETTINGS_DEFAULT_FONT_SIZE,
                Keys.FONT_SIZE, R.string.settings_font_size));
        
        
        settingsEntries.add(new TitleBarSettingsEntry(getString(R.string.category_visibility)));
        settingsEntries.add(new DiscreteSeekBarSettingsEntry(0, 14, Keys.SETTINGS_DEFAULT_UPCOMING_ITEMS_OFFSET,
                Keys.UPCOMING_ITEMS_OFFSET, R.string.settings_show_days_upcoming));
        settingsEntries.add(new DiscreteSeekBarSettingsEntry(0, 14, Keys.SETTINGS_DEFAULT_EXPIRED_ITEMS_OFFSET,
                Keys.EXPIRED_ITEMS_OFFSET, R.string.settings_show_days_expired));
        settingsEntries.add(new SwitchSettingsEntry(
                Keys.SHOW_GLOBAL_ITEMS_LOCK, Keys.SETTINGS_DEFAULT_SHOW_GLOBAL_ITEMS_LOCK,
                getString(R.string.settings_show_global_items_lock)));
        settingsEntries.add(new SwitchSettingsEntry(
                Keys.ITEM_FULL_WIDTH_LOCK, Keys.SETTINGS_DEFAULT_ITEM_FULL_WIDTH_LOCK,
                getString(R.string.settings_max_rWidth_lock)));
        
        settingsEntries.add(new ResetButtonSettingsEntry(this, savedInstanceState));
    
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
        }, "SSCfetch thread").start();
    }
}