package prototype.xd.scheduler.fragments.dialogs;

import static androidx.recyclerview.widget.ConcatAdapter.Config.StableIdMode.NO_STABLE_IDS;
import static prototype.xd.scheduler.utilities.DialogUtilities.displayAttentionDialog;

import android.view.LayoutInflater;
import android.view.ViewGroup;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.recyclerview.widget.ConcatAdapter;
import androidx.recyclerview.widget.DividerItemDecoration;
import androidx.recyclerview.widget.LinearLayoutManager;

import com.google.android.material.divider.MaterialDividerItemDecoration;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;

import prototype.xd.scheduler.R;
import prototype.xd.scheduler.adapters.SettingsListViewAdapter;
import prototype.xd.scheduler.databinding.ListViewBinding;
import prototype.xd.scheduler.entities.SystemCalendar;
import prototype.xd.scheduler.entities.settings_entries.CalendarAccountSettingsEntryConfig;
import prototype.xd.scheduler.entities.settings_entries.CalendarSettingsEntryConfig;
import prototype.xd.scheduler.entities.settings_entries.GenericCalendarSettingsEntryConfig;
import prototype.xd.scheduler.entities.settings_entries.SettingsEntryConfig;
import prototype.xd.scheduler.entities.settings_entries.SwitchSettingsEntryConfig;
import prototype.xd.scheduler.entities.settings_entries.TitleBarSettingsEntryConfig;
import prototype.xd.scheduler.utilities.Static;
import prototype.xd.scheduler.utilities.TodoEntryManager;

public class AllCalendarSettingsDialogFragment extends FullScreenSettingsDialogFragment<ListViewBinding> { // NOSONAR
    
    private ConcatAdapter listViewAdapter; // NOSONAR
    
    @NonNull
    @Override
    protected ListViewBinding inflate(@NonNull LayoutInflater inflater, @Nullable ViewGroup container) {
        return ListViewBinding.inflate(inflater, container, false);
    }
    
    @Override
    @SuppressWarnings("CollectionWithoutInitialCapacity")
    protected void buildDialogBody(@NonNull ListViewBinding binding) {
        
        List<SettingsEntryConfig> staticEntries = new ArrayList<>(2);
        ArrayList<GenericCalendarSettingsEntryConfig> calendarConfigEntries = new ArrayList<>();
        
        staticEntries.add(new TitleBarSettingsEntryConfig(R.string.category_system_calendars));
        staticEntries.add(new SwitchSettingsEntryConfig(
                Static.ALLOW_GLOBAL_CALENDAR_ACCOUNT_SETTINGS,
                R.string.settings_allow_global_calendar_account_settings, (buttonView, isChecked) -> {
            if (isChecked) {
                displayAttentionDialog(wrapper, R.string.whole_calendar_settings_on_warning, R.string.i_understand);
            } else {
                displayAttentionDialog(wrapper, R.string.whole_calendar_settings_off_warning, R.string.i_understand);
            }
            for (GenericCalendarSettingsEntryConfig calendarEntryConfig : calendarConfigEntries) {
                calendarEntryConfig.setShowSettings(isChecked);
            }
            // almost all the list is updated (except first entry)
            listViewAdapter.notifyItemRangeChanged(staticEntries.size(), calendarConfigEntries.size());
        }, false));
        
        binding.recyclerView.addItemDecoration(new MaterialDividerItemDecoration(wrapper.context, DividerItemDecoration.VERTICAL));
        binding.recyclerView.setLayoutManager(new LinearLayoutManager(wrapper.context));
        
        listViewAdapter = new ConcatAdapter(new ConcatAdapter.Config.Builder()
                .setIsolateViewTypes(false)
                .setStableIdMode(NO_STABLE_IDS).build(),
                new SettingsListViewAdapter(wrapper, staticEntries));
        binding.recyclerView.setAdapter(listViewAdapter);
        
        new Thread(() -> {
            Collection<SystemCalendar> calendars = TodoEntryManager.getInstance(wrapper.context).getCalendars();
            Map<String, List<SystemCalendar>> calendarGroups = new TreeMap<>();
            
            for (SystemCalendar calendar : calendars) {
                calendarGroups.computeIfAbsent(calendar.data.accountName,
                                e -> new ArrayList<>())
                        .add(calendar);
            }
            
            // set the capacity to the number of calendars + the number of groups
            calendarConfigEntries.ensureCapacity(calendars.size() + calendarGroups.size());
            
            boolean showSettings = Static.ALLOW_GLOBAL_CALENDAR_ACCOUNT_SETTINGS.get();
            
            for (List<SystemCalendar> calendarGroup : calendarGroups.values()) {
                
                ArrayList<GenericCalendarSettingsEntryConfig> calendarEntryList = new ArrayList<>(calendarGroup.size() + 1);
                SettingsListViewAdapter calendarEntryListAdapter = new SettingsListViewAdapter(wrapper, calendarEntryList, true);
                
                calendarEntryList.add(new CalendarAccountSettingsEntryConfig(
                        calendarGroup.get(0),
                        calendarEntryListAdapter,
                        showSettings));
                
                for (SystemCalendar currentCalendar : calendarGroup) {
                    calendarEntryList.add(new CalendarSettingsEntryConfig(
                            currentCalendar,
                            showSettings));
                }
                
                calendarConfigEntries.addAll(calendarEntryList);
                
                wrapper.runOnUiThread(() -> listViewAdapter.addAdapter(calendarEntryListAdapter));
            }
        }, "SSCFetch thread").start();
    }
}
