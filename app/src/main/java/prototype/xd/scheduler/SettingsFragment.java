package prototype.xd.scheduler;

import static prototype.xd.scheduler.utilities.Keys.SERVICE_UPDATE_SIGNAL;
import static prototype.xd.scheduler.utilities.PreferencesStore.preferences;
import static prototype.xd.scheduler.utilities.PreferencesStore.servicePreferences;
import static prototype.xd.scheduler.utilities.SystemCalendarUtils.getAllCalendars;
import static prototype.xd.scheduler.utilities.Utilities.findFragmentInNavHost;

import android.content.DialogInterface;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.DialogFragment;
import androidx.recyclerview.widget.LinearLayoutManager;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import prototype.xd.scheduler.adapters.SettingsListViewAdapter;
import prototype.xd.scheduler.databinding.SettingsFragmentBinding;
import prototype.xd.scheduler.entities.SystemCalendar;
import prototype.xd.scheduler.entities.settings_entries.AdaptiveBackgroundSettingsEntryConfig;
import prototype.xd.scheduler.entities.settings_entries.AppThemeSelectorEntryConfig;
import prototype.xd.scheduler.entities.settings_entries.CalendarAccountSettingsEntryConfig;
import prototype.xd.scheduler.entities.settings_entries.CalendarSettingsEntryConfig;
import prototype.xd.scheduler.entities.settings_entries.CompoundCustomizationEntryConfig;
import prototype.xd.scheduler.entities.settings_entries.ResetButtonSettingsEntryConfig;
import prototype.xd.scheduler.entities.settings_entries.SeekBarSettingsEntryConfig;
import prototype.xd.scheduler.entities.settings_entries.SettingsEntryConfig;
import prototype.xd.scheduler.entities.settings_entries.SwitchSettingsEntryConfig;
import prototype.xd.scheduler.entities.settings_entries.TitleBarSettingsEntryConfig;
import prototype.xd.scheduler.utilities.Keys;
import prototype.xd.scheduler.views.settings.SystemCalendarSettings;

public class SettingsFragment extends DialogFragment {
    
    private AdaptiveBackgroundSettingsEntryConfig adaptiveBackgroundSettingsEntry;
    private SystemCalendarSettings systemCalendarSettings;
    private SettingsFragmentBinding binding;
    
    private SettingsListViewAdapter settingsListViewAdapter;
    
    private Map<String, ?> preferenceStateBefore;
    
    // initial window creation
    @Override
    public void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        systemCalendarSettings = new SystemCalendarSettings(null, requireContext(), getLifecycle());
        
        setStyle(DialogFragment.STYLE_NORMAL, R.style.FullScreenDialog);
        
        List<SettingsEntryConfig> settingsEntries = new ArrayList<>();
        
        adaptiveBackgroundSettingsEntry = new AdaptiveBackgroundSettingsEntryConfig(this);
        
        settingsEntries.add(new TitleBarSettingsEntryConfig(getString(R.string.category_appearance)));
        settingsEntries.add(new AppThemeSelectorEntryConfig());
        settingsEntries.add(adaptiveBackgroundSettingsEntry);
        settingsEntries.add(new SeekBarSettingsEntryConfig(0, 10, Keys.SETTINGS_DEFAULT_ADAPTIVE_COLOR_BALANCE, true, true,
                Keys.ADAPTIVE_COLOR_BALANCE, R.string.settings_adaptive_color_balance));
        settingsEntries.add(new CompoundCustomizationEntryConfig());
        settingsEntries.add(new SwitchSettingsEntryConfig(
                Keys.ITEM_FULL_WIDTH_LOCK, Keys.SETTINGS_DEFAULT_ITEM_FULL_WIDTH_LOCK,
                getString(R.string.settings_max_rWidth_lock)));
        
        settingsEntries.add(new TitleBarSettingsEntryConfig(getString(R.string.category_visibility)));
        settingsEntries.add(new SeekBarSettingsEntryConfig(0, 14, Keys.SETTINGS_DEFAULT_UPCOMING_ITEMS_OFFSET, true, false,
                Keys.UPCOMING_ITEMS_OFFSET, R.string.settings_show_days_upcoming));
        settingsEntries.add(new SeekBarSettingsEntryConfig(0, 14, Keys.SETTINGS_DEFAULT_EXPIRED_ITEMS_OFFSET, true, false,
                Keys.EXPIRED_ITEMS_OFFSET, R.string.settings_show_days_expired));
        settingsEntries.add(new SwitchSettingsEntryConfig(
                Keys.HIDE_EXPIRED_ENTRIES_BY_TIME, Keys.SETTINGS_DEFAULT_HIDE_EXPIRED_ENTRIES_BY_TIME,
                getString(R.string.settings_hide_expired_entries_by_time)));
        settingsEntries.add(new SwitchSettingsEntryConfig(
                Keys.SHOW_UPCOMING_EXPIRED_IN_LIST, Keys.SETTINGS_DEFAULT_SHOW_UPCOMING_EXPIRED_IN_LIST,
                getString(R.string.show_upcoming_and_expired_event_indicators)));
        settingsEntries.add(new SwitchSettingsEntryConfig(
                Keys.SHOW_GLOBAL_ITEMS_LOCK, Keys.SETTINGS_DEFAULT_SHOW_GLOBAL_ITEMS_LOCK,
                getString(R.string.settings_show_global_items_lock)));
        
        settingsEntries.add(new ResetButtonSettingsEntryConfig(this, savedInstanceState));
        
        settingsListViewAdapter = new SettingsListViewAdapter(settingsEntries, getLifecycle());
        
        List<SettingsEntryConfig> additionalSettingsEntries = new ArrayList<>();
        new Thread(() -> {
            additionalSettingsEntries.add(new TitleBarSettingsEntryConfig(getString(R.string.category_system_calendars)));
            List<SystemCalendar> calendars = getAllCalendars(requireContext(), true);
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
                
                additionalSettingsEntries.add(new CalendarAccountSettingsEntryConfig(systemCalendarSettings, calendarGroup.get(0)));
                
                for (SystemCalendar currentCalendar : calendarGroup) {
                    additionalSettingsEntries.add(new CalendarSettingsEntryConfig(systemCalendarSettings, currentCalendar));
                }
            }
            requireActivity().runOnUiThread(() -> {
                int offset = settingsEntries.size();
                settingsEntries.addAll(additionalSettingsEntries);
                settingsListViewAdapter.notifyItemRangeInserted(offset, offset + additionalSettingsEntries.size());
            });
        }, "SSCFetch thread").start();
    }
    
    // view creation begin
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        binding = SettingsFragmentBinding.inflate(inflater, container, false);
        preferenceStateBefore = preferences.getAll();
        return binding.getRoot();
    }
    
    // dialog dismissed (user pressed back button)
    @Override
    public void onDismiss(@NonNull DialogInterface dialog) {
        servicePreferences.edit()
                .putBoolean(SERVICE_UPDATE_SIGNAL, true)
                .apply();
        if(!preferenceStateBefore.equals(preferences.getAll())) {
            findFragmentInNavHost(requireActivity(), HomeFragment.class).invalidateAll();
        }
        super.onDismiss(dialog);
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
        binding.recyclerView.setItemAnimator(null);
        binding.recyclerView.setLayoutManager(new LinearLayoutManager(requireContext()));
        binding.recyclerView.setAdapter(settingsListViewAdapter);
    }
}