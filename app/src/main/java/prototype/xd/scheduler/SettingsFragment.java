package prototype.xd.scheduler;

import static prototype.xd.scheduler.utilities.Keys.SERVICE_UPDATE_SIGNAL;
import static prototype.xd.scheduler.utilities.PreferencesStore.servicePreferences;
import static prototype.xd.scheduler.utilities.SystemCalendarUtils.getAllCalendars;

import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ListView;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.DialogFragment;

import java.util.ArrayList;
import java.util.List;

import prototype.xd.scheduler.adapters.SettingsListViewAdapter;
import prototype.xd.scheduler.entities.calendars.SystemCalendar;
import prototype.xd.scheduler.entities.settings_entries.AdaptiveBackgroundSettingsEntry;
import prototype.xd.scheduler.entities.settings_entries.AppThemeSelectorEntry;
import prototype.xd.scheduler.entities.settings_entries.CalendarAccountSettingsEntry;
import prototype.xd.scheduler.entities.settings_entries.CalendarSettingsEntry;
import prototype.xd.scheduler.entities.settings_entries.CompoundCustomizationEntry;
import prototype.xd.scheduler.entities.settings_entries.DiscreteSeekBarSettingsEntry;
import prototype.xd.scheduler.entities.settings_entries.ResetButtonSettingsEntry;
import prototype.xd.scheduler.entities.settings_entries.SettingsEntry;
import prototype.xd.scheduler.entities.settings_entries.SwitchSettingsEntry;
import prototype.xd.scheduler.entities.settings_entries.TitleBarSettingsEntry;
import prototype.xd.scheduler.utilities.Keys;
import prototype.xd.scheduler.views.settings.SystemCalendarSettings;

public class SettingsFragment extends DialogFragment {
    
    private AdaptiveBackgroundSettingsEntry adaptiveBackgroundSettingsEntry;
    private SystemCalendarSettings systemCalendarSettings;
    
    // initial window creation
    @Override
    public void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setStyle(DialogFragment.STYLE_NORMAL,
                R.style.FullScreenDialog);
    }
    
    // view creation begin
    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        systemCalendarSettings = new SystemCalendarSettings(null, inflater.inflate(R.layout.entry_settings, container, false));
        return inflater.inflate(R.layout.fragment_settings, container, false);
    }
    
    // fragment becomes not visible
    @Override
    public void onDestroyView() {
        servicePreferences.edit().putBoolean(SERVICE_UPDATE_SIGNAL, true).apply();
        super.onDestroyView();
    }
    
    // full destroy
    @Override
    public void onDestroy() {
        adaptiveBackgroundSettingsEntry = null;
        systemCalendarSettings = null;
        super.onDestroy();
    }
    
    public void notifyBgSelected() {
        adaptiveBackgroundSettingsEntry.notifyBackgroundUpdated();
    }
    
    // view creation end (fragment visible)
    @Override
    public void onViewCreated(@NonNull final View view, final Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);
        
        List<SettingsEntry> settingsEntries = new ArrayList<>();
        
        adaptiveBackgroundSettingsEntry = new AdaptiveBackgroundSettingsEntry(this);
        
        settingsEntries.add(new TitleBarSettingsEntry(getString(R.string.category_appearance)));
        settingsEntries.add(new AppThemeSelectorEntry());
        settingsEntries.add(adaptiveBackgroundSettingsEntry);
        settingsEntries.add(new DiscreteSeekBarSettingsEntry(0, 10, Keys.SETTINGS_DEFAULT_ADAPTIVE_COLOR_BALANCE, true,
                Keys.ADAPTIVE_COLOR_BALANCE, R.string.settings_adaptive_color_balance));
        settingsEntries.add(new CompoundCustomizationEntry());
        settingsEntries.add(new DiscreteSeekBarSettingsEntry(10, 30, Keys.SETTINGS_DEFAULT_FONT_SIZE, false,
                Keys.FONT_SIZE, R.string.settings_font_size));
        
        
        settingsEntries.add(new TitleBarSettingsEntry(getString(R.string.category_visibility)));
        settingsEntries.add(new DiscreteSeekBarSettingsEntry(0, 14, Keys.SETTINGS_DEFAULT_UPCOMING_ITEMS_OFFSET, false,
                Keys.UPCOMING_ITEMS_OFFSET, R.string.settings_show_days_upcoming));
        settingsEntries.add(new DiscreteSeekBarSettingsEntry(0, 14, Keys.SETTINGS_DEFAULT_EXPIRED_ITEMS_OFFSET, false,
                Keys.EXPIRED_ITEMS_OFFSET, R.string.settings_show_days_expired));
        settingsEntries.add(new SwitchSettingsEntry(
                Keys.HIDE_EXPIRED_ENTRIES_BY_TIME, Keys.SETTINGS_DEFAULT_HIDE_EXPIRED_ENTRIES_BY_TIME,
                getString(R.string.settings_hide_expired_entries_by_time)));
        settingsEntries.add(new SwitchSettingsEntry(
                Keys.SHOW_GLOBAL_ITEMS_LOCK, Keys.SETTINGS_DEFAULT_SHOW_GLOBAL_ITEMS_LOCK,
                getString(R.string.settings_show_global_items_lock)));
        settingsEntries.add(new SwitchSettingsEntry(
                Keys.ITEM_FULL_WIDTH_LOCK, Keys.SETTINGS_DEFAULT_ITEM_FULL_WIDTH_LOCK,
                getString(R.string.settings_max_rWidth_lock)));
        
        settingsEntries.add(new ResetButtonSettingsEntry(this, savedInstanceState));
        
        SettingsListViewAdapter settingsListViewAdapter = new SettingsListViewAdapter(settingsEntries);
        ((ListView) view.findViewById(R.id.settingsList)).setAdapter(settingsListViewAdapter);
        
        List<SettingsEntry> additionalSettingsEntries = new ArrayList<>();
        new Thread(() -> {
            additionalSettingsEntries.add(new TitleBarSettingsEntry(getString(R.string.category_system_calendars)));
            List<SystemCalendar> calendars = getAllCalendars(view.getContext(), true);
            List<List<SystemCalendar>> sortedCalendars = new ArrayList<>();
            List<String> sortedCalendarNames = new ArrayList<>();
            
            for (SystemCalendar calendar : calendars) {
                if (sortedCalendarNames.contains(calendar.account_name)) {
                    sortedCalendars.get(sortedCalendarNames.indexOf(calendar.account_name)).add(calendar);
                } else {
                    List<SystemCalendar> calendarGroup = new ArrayList<>();
                    calendarGroup.add(calendar);
                    sortedCalendars.add(calendarGroup);
                    sortedCalendarNames.add(calendar.account_name);
                }
            }
            
            for (List<SystemCalendar> calendarGroup : sortedCalendars) {
                SystemCalendar calendar0 = calendarGroup.get(0);
                
                additionalSettingsEntries.add(new CalendarAccountSettingsEntry(systemCalendarSettings, calendar0));
                
                for (SystemCalendar currentCalendar : calendarGroup) {
                    additionalSettingsEntries.add(new CalendarSettingsEntry(systemCalendarSettings, currentCalendar));
                }
            }
            requireActivity().runOnUiThread(() -> {
                settingsEntries.addAll(additionalSettingsEntries);
                settingsListViewAdapter.notifyDataSetChanged();
            });
        }, "SSCFetch thread").start();
    }
}