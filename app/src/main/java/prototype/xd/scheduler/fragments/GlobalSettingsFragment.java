package prototype.xd.scheduler.fragments;

import static androidx.recyclerview.widget.ConcatAdapter.Config.StableIdMode.NO_STABLE_IDS;
import static prototype.xd.scheduler.utilities.DateManager.FIRST_DAYS_OF_WEEK_LOCAL;
import static prototype.xd.scheduler.utilities.DateManager.FIRST_DAYS_OF_WEEK_ROOT;
import static prototype.xd.scheduler.utilities.DateManager.FIRST_DAY_OF_WEEK;
import static prototype.xd.scheduler.utilities.Logger.error;
import static prototype.xd.scheduler.utilities.Logger.logException;
import static prototype.xd.scheduler.utilities.Static.DISPLAY_METRICS_HEIGHT;
import static prototype.xd.scheduler.utilities.Static.DISPLAY_METRICS_WIDTH;
import static prototype.xd.scheduler.utilities.Utilities.getFile;

import android.annotation.SuppressLint;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Color;
import android.net.Uri;
import android.os.Bundle;

import androidx.activity.result.ActivityResultLauncher;
import androidx.annotation.MainThread;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.recyclerview.widget.ConcatAdapter;

import com.canhub.cropper.CropImageContract;
import com.canhub.cropper.CropImageContractOptions;
import com.canhub.cropper.CropImageOptions;
import com.canhub.cropper.CropImageView;
import com.google.android.material.color.MaterialColors;

import java.io.InputStream;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import prototype.xd.scheduler.R;
import prototype.xd.scheduler.adapters.SettingsListViewAdapter;
import prototype.xd.scheduler.entities.settings_entries.AdaptiveBackgroundSettingsEntryConfig;
import prototype.xd.scheduler.entities.settings_entries.AppThemeSelectorEntryConfig;
import prototype.xd.scheduler.entities.settings_entries.CompoundCustomizationEntryConfig;
import prototype.xd.scheduler.entities.settings_entries.DoubleSliderSettingsEntryConfig;
import prototype.xd.scheduler.entities.settings_entries.DropdownSettingsEntryConfig;
import prototype.xd.scheduler.entities.settings_entries.ResetButtonSettingsEntryConfig;
import prototype.xd.scheduler.entities.settings_entries.SettingsEntryConfig;
import prototype.xd.scheduler.entities.settings_entries.SliderSettingsEntryConfig;
import prototype.xd.scheduler.entities.settings_entries.SwitchSettingsEntryConfig;
import prototype.xd.scheduler.entities.settings_entries.TitleBarSettingsEntryConfig;
import prototype.xd.scheduler.utilities.ColorUtilities;
import prototype.xd.scheduler.utilities.DateManager;
import prototype.xd.scheduler.utilities.Static;

public class GlobalSettingsFragment extends BaseListSettingsFragment<ConcatAdapter> { // NOSONAR, this is a fragment
    
    public static final String NAME = GlobalSettingsFragment.class.getSimpleName();
    
    private AdaptiveBackgroundSettingsEntryConfig adaptiveBgSettingsEntry;
    
    final ActivityResultLauncher<CropImageContractOptions> cropBg =
            registerForActivityResult(new CropImageContract(), this::onBgCropped);
    
    // initial window creation
    @SuppressLint("NotifyDataSetChanged")
    @Override
    @MainThread
    public void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        
        adaptiveBgSettingsEntry = new AdaptiveBackgroundSettingsEntryConfig(wrapper.context,
                bgIndex -> {
                    CropImageOptions options = new CropImageOptions();
                    
                    options.imageSourceIncludeCamera = false;
                    options.outputCompressFormat = Bitmap.CompressFormat.PNG;
                    options.aspectRatioX = DISPLAY_METRICS_WIDTH.get();
                    options.aspectRatioY = DISPLAY_METRICS_HEIGHT.get();
                    options.fixAspectRatio = true;
                    options.activityBackgroundColor = Color.TRANSPARENT;
                    options.activityMenuIconColor = MaterialColors.getColor(wrapper.context, R.attr.colorControlNormal, Color.GRAY);
                    
                    cropBg.launch(new CropImageContractOptions(null, options));
                });
        
        List<SettingsEntryConfig> settingsEntries = List.of(
                
                new TitleBarSettingsEntryConfig(getString(R.string.category_application_settings)),
                new AppThemeSelectorEntryConfig(),
                new DropdownSettingsEntryConfig<>(R.string.first_weekday, FIRST_DAYS_OF_WEEK_LOCAL, FIRST_DAYS_OF_WEEK_ROOT, FIRST_DAY_OF_WEEK),
                new TitleBarSettingsEntryConfig(getString(R.string.category_lockscreen_appearance)),
                adaptiveBgSettingsEntry,
                new CompoundCustomizationEntryConfig(),
                
                new SwitchSettingsEntryConfig(
                        Static.ITEM_FULL_WIDTH_LOCK, getString(R.string.settings_max_rWidth_lock)),
                
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
                
                new TitleBarSettingsEntryConfig(getString(R.string.category_event_visibility)),
                
                new DoubleSliderSettingsEntryConfig(wrapper.context, R.string.settings_show_events,
                        
                        new SliderSettingsEntryConfig(Static.UPCOMING_ITEMS_OFFSET,
                                0, Static.SETTINGS_MAX_EXPIRED_UPCOMING_ITEMS_OFFSET, 1, false, R.plurals.settings_in_n_days),
                        Static.BG_COLOR.UPCOMING.defaultValue,
                        
                        new SliderSettingsEntryConfig(Static.EXPIRED_ITEMS_OFFSET,
                                0, Static.SETTINGS_MAX_EXPIRED_UPCOMING_ITEMS_OFFSET, 1, false, R.plurals.settings_after_n_days),
                        Static.BG_COLOR.EXPIRED.defaultValue),
                
                new SwitchSettingsEntryConfig(Static.MERGE_ENTRIES, getString(R.string.settings_merge_events)),
                new SwitchSettingsEntryConfig(Static.HIDE_EXPIRED_ENTRIES_BY_TIME, getString(R.string.settings_hide_expired_entries_by_time)),
                
                new SwitchSettingsEntryConfig(Static.SHOW_UPCOMING_EXPIRED_IN_LIST, getString(R.string.show_upcoming_and_expired_event_indicators)));
        
        //                                                                      two entries (global switches)
        List<SettingsEntryConfig> globalSwitchSettingsEntries = new ArrayList<>(2);
        SettingsListViewAdapter globalSwitchSettingsListViewAdapter = new SettingsListViewAdapter(wrapper, globalSwitchSettingsEntries,
                !Static.SHOW_GLOBAL_ITEMS_LOCK.get());
        
        globalSwitchSettingsEntries.add(new SwitchSettingsEntryConfig(
                Static.SHOW_GLOBAL_ITEMS_LOCK, getString(R.string.settings_show_global_items_lock),
                (buttonView, isChecked) -> globalSwitchSettingsListViewAdapter.setCollapsed(!isChecked), false));
        
        globalSwitchSettingsEntries.add(new SwitchSettingsEntryConfig(
                Static.SHOW_GLOBAL_ITEMS_LABEL_LOCK, getString(R.string.settings_show_global_items_label_lock)));
        
        listViewAdapter = new ConcatAdapter(new ConcatAdapter.Config.Builder()
                .setIsolateViewTypes(false)
                .setStableIdMode(NO_STABLE_IDS).build(),
                new SettingsListViewAdapter(wrapper, settingsEntries),
                globalSwitchSettingsListViewAdapter,
                new SettingsListViewAdapter(wrapper,
                        Collections.singletonList(
                                new ResetButtonSettingsEntryConfig((dialog, which) -> {
                                    Static.clearAll();
                                    listViewAdapter.notifyDataSetChanged();
                                }))));
        
    }
    
    public void onBgCropped(@NonNull CropImageView.CropResult result) {
        if(result.isSuccessful()) {
            Uri uri = result.getUriContent();
            new Thread(() -> {
                try (InputStream stream = requireActivity().getContentResolver().openInputStream(uri)) {
            
                    if (stream != null) {
                        ColorUtilities.fingerPrintAndSaveBitmap(BitmapFactory.decodeStream(stream),
                                getFile(DateManager.BG_NAMES_ROOT.get(adaptiveBgSettingsEntry.getLastClickedBgIndex())));
                        requireActivity().runOnUiThread(() -> adaptiveBgSettingsEntry.notifyBackgroundUpdated());
                    } else {
                        error(NAME, "Stream null for uri: " + uri);
                    }
            
                } catch (Exception e) {
                    logException(Thread.currentThread().getName(), e);
                }
            }, "LBCP thread").start();
        }
    }
}