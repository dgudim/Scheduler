package prototype.xd.scheduler.fragments;

import static androidx.activity.result.contract.ActivityResultContracts.OpenDocument;
import static prototype.xd.scheduler.utilities.DateManager.FIRST_DAYS_OF_WEEK_LOCAL;
import static prototype.xd.scheduler.utilities.DateManager.FIRST_DAYS_OF_WEEK_ROOT;
import static prototype.xd.scheduler.utilities.DateManager.FIRST_DAY_OF_WEEK;

import android.annotation.SuppressLint;
import android.os.Bundle;

import androidx.activity.result.ActivityResultLauncher;
import androidx.annotation.MainThread;
import androidx.annotation.Nullable;

import com.canhub.cropper.CropImageContract;
import com.canhub.cropper.CropImageContractOptions;

import java.util.List;

import prototype.xd.scheduler.R;
import prototype.xd.scheduler.adapters.SettingsListViewAdapter;
import prototype.xd.scheduler.entities.settings_entries.AdaptiveBackgroundSettingsEntryConfig;
import prototype.xd.scheduler.entities.settings_entries.AppThemeSelectorEntryConfig;
import prototype.xd.scheduler.entities.settings_entries.CompoundCustomizationSettingsEntryConfig;
import prototype.xd.scheduler.entities.settings_entries.DividerSettingsEntryConfig;
import prototype.xd.scheduler.entities.settings_entries.DoubleSliderSettingsEntryConfig;
import prototype.xd.scheduler.entities.settings_entries.DropdownSettingsEntryConfig;
import prototype.xd.scheduler.entities.settings_entries.ImportExportSettingsEntryConfig;
import prototype.xd.scheduler.entities.settings_entries.ResetButtonSettingsEntryConfig;
import prototype.xd.scheduler.entities.settings_entries.SettingsEntryConfig;
import prototype.xd.scheduler.entities.settings_entries.SliderSettingsEntryConfig;
import prototype.xd.scheduler.entities.settings_entries.SwitchSettingsEntryConfig;
import prototype.xd.scheduler.entities.settings_entries.TitleBarSettingsEntryConfig;
import prototype.xd.scheduler.utilities.Static;

public class GlobalSettingsFragment extends BaseListSettingsFragment<SettingsListViewAdapter> { // NOSONAR, this is a fragment
    
    
    private AdaptiveBackgroundSettingsEntryConfig adaptiveBgSettingsEntry;
    
    final ActivityResultLauncher<CropImageContractOptions> cropBgLauncher =
            registerForActivityResult(new CropImageContract(), result -> adaptiveBgSettingsEntry.notifyBackgroundSelected(wrapper, result));
    
    final ActivityResultLauncher<String[]> importSettingsLauncher =
            registerForActivityResult(new OpenDocument(), result -> ImportExportSettingsEntryConfig.notifyFileChosen(wrapper, result));
    
    // initial window creation
    @SuppressLint("NotifyDataSetChanged")
    @Override
    @MainThread
    public void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        
        adaptiveBgSettingsEntry = new AdaptiveBackgroundSettingsEntryConfig(wrapper, cropBgLauncher);
        
        List<SettingsEntryConfig> settingsEntries = List.of(
                
                new TitleBarSettingsEntryConfig(R.string.category_application_settings),
                new DividerSettingsEntryConfig(),
                new AppThemeSelectorEntryConfig(),
                new DividerSettingsEntryConfig(),
                new ImportExportSettingsEntryConfig(importSettingsLauncher),
                new DropdownSettingsEntryConfig<>(R.string.first_weekday, FIRST_DAYS_OF_WEEK_LOCAL, FIRST_DAYS_OF_WEEK_ROOT, FIRST_DAY_OF_WEEK),
                
                new TitleBarSettingsEntryConfig(R.string.category_lockscreen_appearance),
                new DividerSettingsEntryConfig(),
                adaptiveBgSettingsEntry,
                new CompoundCustomizationSettingsEntryConfig(),
                
                new SwitchSettingsEntryConfig(
                        Static.ITEM_FULL_WIDTH_LOCK, R.string.settings_max_rWidth_lock),
                
                new DividerSettingsEntryConfig(),
                
                new SliderSettingsEntryConfig(Static.LOCKSCREEN_VIEW_VERTICAL_BIAS,
                        0, 100, 5, value -> {
                    String baseString = wrapper.getString(R.string.settings_event_vertical_bias, value) + "%";
                    if (value == 0) {
                        return baseString + " (" + wrapper.getString(R.string.top) + ")";
                    }
                    if (value == 25) {
                        return baseString + " (1/4)";
                    }
                    if (value == 50) {
                        return baseString + " (" + wrapper.getString(R.string.middle) + ")";
                    }
                    if (value == 75) {
                        return baseString + " (3/4)";
                    }
                    if (value == 100) {
                        return baseString + " (" + wrapper.getString(R.string.bottom) + ")";
                    }
                    return baseString;
                }),
                
                new DividerSettingsEntryConfig(),
                new TitleBarSettingsEntryConfig(R.string.category_event_visibility),
                
                new DoubleSliderSettingsEntryConfig(wrapper.context, R.string.settings_show_events,
                        
                        new SliderSettingsEntryConfig(Static.UPCOMING_ITEMS_OFFSET,
                                0, Static.SETTINGS_MAX_EXPIRED_UPCOMING_ITEMS_OFFSET, 1, false, R.plurals.settings_in_n_days),
                        Static.BG_COLOR.UPCOMING.defaultValue,
                        
                        new SliderSettingsEntryConfig(Static.EXPIRED_ITEMS_OFFSET,
                                0, Static.SETTINGS_MAX_EXPIRED_UPCOMING_ITEMS_OFFSET, 1, false, R.plurals.settings_after_n_days),
                        Static.BG_COLOR.EXPIRED.defaultValue),
                
                new SwitchSettingsEntryConfig(Static.MERGE_ENTRIES, R.string.settings_merge_events),
                new DividerSettingsEntryConfig(),
                new SwitchSettingsEntryConfig(Static.HIDE_EXPIRED_ENTRIES_BY_TIME, R.string.settings_hide_expired_entries_by_time),
                new DividerSettingsEntryConfig(),
                new SwitchSettingsEntryConfig(Static.SHOW_UPCOMING_EXPIRED_IN_LIST, R.string.show_upcoming_and_expired_event_indicators),
                new DividerSettingsEntryConfig(),
                new SwitchSettingsEntryConfig(Static.SHOW_GLOBAL_ITEMS_LOCK, R.string.settings_show_global_items_lock),
                new DividerSettingsEntryConfig(),
                new DropdownSettingsEntryConfig<>(
                        R.string.global_events_show_hint,
                        List.of(wrapper.getString(R.string.global_events_show_front),
                                wrapper.getString(R.string.global_events_show_back),
                                wrapper.getString(R.string.global_events_show_hidden)),
                        List.of(Static.GlobalLabelPos.FRONT,
                                Static.GlobalLabelPos.BACK,
                                Static.GlobalLabelPos.HIDDEN), Static.GLOBAL_ITEMS_LABEL_POSITION),
                new ResetButtonSettingsEntryConfig((dialog, which) -> {
                    Static.clearAll();
                    listViewAdapter.notifyDataSetChanged();
                }));
        
        listViewAdapter = new SettingsListViewAdapter(wrapper, settingsEntries);
    }
}
