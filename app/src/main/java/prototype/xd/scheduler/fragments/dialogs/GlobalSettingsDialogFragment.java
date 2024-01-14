package prototype.xd.scheduler.fragments.dialogs;

import static androidx.activity.result.contract.ActivityResultContracts.OpenDocument;
import static prototype.xd.scheduler.utilities.DateManager.FIRST_DAYS_OF_WEEK_LOCAL;
import static prototype.xd.scheduler.utilities.DateManager.FIRST_DAYS_OF_WEEK_ROOT;
import static prototype.xd.scheduler.utilities.DateManager.FIRST_DAY_OF_WEEK;

import android.annotation.SuppressLint;
import android.view.LayoutInflater;
import android.view.ViewGroup;

import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.recyclerview.widget.LinearLayoutManager;

import com.canhub.cropper.CropImageContract;
import com.canhub.cropper.CropImageContractOptions;

import java.util.List;
import java.util.Objects;

import prototype.xd.scheduler.R;
import prototype.xd.scheduler.adapters.SettingsListViewAdapter;
import prototype.xd.scheduler.databinding.ListViewBinding;
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

public class GlobalSettingsDialogFragment extends FullScreenSettingsDialogFragment<ListViewBinding> { // NOSONAR, this is a fragment
    
    
    private AdaptiveBackgroundSettingsEntryConfig adaptiveBgSettingsEntry;
    
    private final ActivityResultLauncher<CropImageContractOptions> cropBgLauncher =
            registerForActivityResult(new CropImageContract(), result -> adaptiveBgSettingsEntry.notifyBackgroundSelected(wrapper, result));
    
    private final ActivityResultLauncher<String[]> importSettingsLauncher =
            registerForActivityResult(new OpenDocument(),
                    result -> ImportExportSettingsEntryConfig.notifyFileChosen(wrapper, result));
    
    private final ActivityResultLauncher<String> exportSettingsLauncher =
            registerForActivityResult(new ActivityResultContracts.CreateDocument(ImportExportSettingsEntryConfig.MIMETYPE),
                    result -> ImportExportSettingsEntryConfig.notifySaveLocationChosen(wrapper, result));
    
    @NonNull
    @Override
    protected ListViewBinding inflate(@NonNull LayoutInflater inflater, @Nullable ViewGroup container) {
        return ListViewBinding.inflate(inflater, container, false);
    }
    
    @Override
    @SuppressLint("NotifyDataSetChanged")
    protected void buildDialogBody(@NonNull ListViewBinding binding) {
        adaptiveBgSettingsEntry = new AdaptiveBackgroundSettingsEntryConfig(wrapper, cropBgLauncher);
        
        List<SettingsEntryConfig> settingsEntries = List.of(
                
                new TitleBarSettingsEntryConfig(R.string.category_application_settings),
                new DividerSettingsEntryConfig(),
                new AppThemeSelectorEntryConfig(),
                new DividerSettingsEntryConfig(),
                new ImportExportSettingsEntryConfig(importSettingsLauncher, exportSettingsLauncher),
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
                    String baseString = wrapper.getString(R.string.settings_event_vertical_bias, value);
                    return switch (value) {
                        case 0 -> baseString + " (" + wrapper.getString(R.string.top) + ")";
                        case 25 -> baseString + " (1/4)";
                        case 50 -> baseString + " (" + wrapper.getString(R.string.middle) + ")";
                        case 75 -> baseString + " (3/4)";
                        case 100 -> baseString + " (" + wrapper.getString(R.string.bottom) + ")";
                        default -> baseString;
                    };
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
                new ResetButtonSettingsEntryConfig((dialogInterface, which) -> {
                    // Erase immediately
                    Static.clearAll().commit();
                    Objects.requireNonNull(binding.recyclerView.getAdapter()).notifyDataSetChanged();
                }));
        
        binding.recyclerView.setAdapter(new SettingsListViewAdapter(wrapper, settingsEntries));
        binding.recyclerView.setLayoutManager(new LinearLayoutManager(wrapper.context));
    }
}
