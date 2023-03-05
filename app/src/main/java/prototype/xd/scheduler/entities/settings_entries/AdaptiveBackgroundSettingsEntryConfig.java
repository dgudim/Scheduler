package prototype.xd.scheduler.entities.settings_entries;

import static prototype.xd.scheduler.entities.settings_entries.SettingsEntryType.ADAPTIVE_BACKGROUND_SETTINGS;
import static prototype.xd.scheduler.utilities.DialogUtilities.displayAttentionDialog;
import static prototype.xd.scheduler.utilities.DialogUtilities.displayMessageDialog;
import static prototype.xd.scheduler.utilities.Logger.error;
import static prototype.xd.scheduler.utilities.Logger.logException;
import static prototype.xd.scheduler.utilities.Static.ADAPTIVE_BACKGROUND_ENABLED;
import static prototype.xd.scheduler.utilities.Static.DISPLAY_METRICS_HEIGHT;
import static prototype.xd.scheduler.utilities.Static.DISPLAY_METRICS_WIDTH;
import static prototype.xd.scheduler.utilities.Utilities.getFile;

import android.app.AlertDialog;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Color;
import android.net.Uri;
import android.widget.GridView;

import androidx.activity.result.ActivityResultLauncher;
import androidx.annotation.NonNull;

import com.canhub.cropper.CropImageContractOptions;
import com.canhub.cropper.CropImageOptions;
import com.canhub.cropper.CropImageView;
import com.google.android.material.color.MaterialColors;

import java.io.InputStream;

import prototype.xd.scheduler.R;
import prototype.xd.scheduler.adapters.PerDayBgGridViewAdapter;
import prototype.xd.scheduler.databinding.AdaptiveBackgroundSettingsEntryBinding;
import prototype.xd.scheduler.databinding.BgGridSelectionViewBinding;
import prototype.xd.scheduler.utilities.ColorUtilities;
import prototype.xd.scheduler.utilities.DateManager;
import prototype.xd.scheduler.utilities.Utilities;
import prototype.xd.scheduler.utilities.misc.ContextWrapper;

public class AdaptiveBackgroundSettingsEntryConfig extends SettingsEntryConfig {
    
    public static final String NAME = AdaptiveBackgroundSettingsEntryConfig.class.getSimpleName();
    
    @NonNull
    private final PerDayBgGridViewAdapter gridViewAdapter;
    private Integer lastClickedBgIndex;
    
    public AdaptiveBackgroundSettingsEntryConfig(@NonNull final ContextWrapper wrapper,
                                                 @NonNull final ActivityResultLauncher<CropImageContractOptions> bgSelectionLauncher) {
        gridViewAdapter = new PerDayBgGridViewAdapter(wrapper.context, bgIndex -> {
            lastClickedBgIndex = bgIndex;
            
            if (!ADAPTIVE_BACKGROUND_ENABLED.get()) {
                displayAttentionDialog(wrapper, R.string.dynamic_wallpaper_off_warning, R.string.close);
            }
    
            CropImageOptions options = new CropImageOptions();
    
            options.imageSourceIncludeCamera = false;
            options.outputCompressFormat = Bitmap.CompressFormat.PNG;
            options.aspectRatioX = DISPLAY_METRICS_WIDTH.get();
            options.aspectRatioY = DISPLAY_METRICS_HEIGHT.get();
            options.fixAspectRatio = true;
            options.progressBarColor = MaterialColors.getColor(wrapper.context, R.attr.colorPrimary, Color.GRAY);
            options.activityBackgroundColor = Color.TRANSPARENT;
            options.activityMenuIconColor = MaterialColors.getColor(wrapper.context, R.attr.colorControlNormal, Color.GRAY);
    
            bgSelectionLauncher.launch(new CropImageContractOptions(null, options));
        });
    }
    
    public void notifyBackgroundSelected(@NonNull ContextWrapper wrapper, @NonNull CropImageView.CropResult result) {
        if (result.isSuccessful()) {
            Uri uri = result.getUriContent();
            new Thread(() -> {
                try (InputStream stream = wrapper.activity.getContentResolver().openInputStream(uri)) {
                
                    if (stream != null) {
                        ColorUtilities.fingerPrintAndSaveBitmap(BitmapFactory.decodeStream(stream),
                                getFile(DateManager.BG_NAMES_ROOT.get(lastClickedBgIndex)));
                    } else {
                        error(NAME, "Stream null for uri: " + uri);
                    }
                
                } catch (Exception e) {
                    logException(Thread.currentThread().getName(), e);
                }
            }, "LBCP thread").start();
            wrapper.runOnUiThread(gridViewAdapter::notifyDataSetChanged);
        }
    }
    
    @Override
    public int getRecyclerViewType() {
        return ADAPTIVE_BACKGROUND_SETTINGS.ordinal();
    }
    
    static class ViewHolder extends SettingsEntryConfig.SettingsViewHolder<AdaptiveBackgroundSettingsEntryBinding, AdaptiveBackgroundSettingsEntryConfig> {
        
        ViewHolder(@NonNull ContextWrapper wrapper, @NonNull AdaptiveBackgroundSettingsEntryBinding viewBinding) {
            super(wrapper, viewBinding);
        }
        
        @SuppressWarnings("ResultOfMethodCallIgnored")
        @Override
        void bind(@NonNull AdaptiveBackgroundSettingsEntryConfig config) {
            viewBinding.adaptiveBgSettings.setOnClickListener(v -> {
                
                BgGridSelectionViewBinding gridSelection = BgGridSelectionViewBinding.inflate(wrapper.getLayoutInflater());
                
                Utilities.setSwitchChangeListener(gridSelection.adaptiveBgSwitch,
                        ADAPTIVE_BACKGROUND_ENABLED,
                        null);
                
                gridSelection.resetBgButton.setOnClickListener(view1 ->
                        displayMessageDialog(wrapper, builder -> {
                            builder.setTitle(R.string.delete_all_saved_backgrounds_prompt);
                            builder.setMessage(R.string.delete_all_saved_backgrounds_description);
                            builder.setIcon(R.drawable.ic_delete_50);
                            builder.setNegativeButton(R.string.cancel, null);
                            
                            builder.setPositiveButton(R.string.delete, (dialogInterface, whichButton) -> {
                                for (int dayIndex = 0; dayIndex < DateManager.BG_NAMES_ROOT.size() - 1; dayIndex++) {
                                    String bgName = DateManager.BG_NAMES_ROOT.get(dayIndex);
                                    getFile(bgName).delete();
                                    getFile(bgName + "_min.png").delete();
                                }
                                config.gridViewAdapter.notifyDataSetChanged();
                            });
                        }));
                
                GridView gridView = gridSelection.gridViewInclude.gridView;
                gridView.setNumColumns(2);
                gridView.setHorizontalSpacing(30);
                gridView.setVerticalSpacing(30);
                gridView.setAdapter(config.gridViewAdapter);
                
                wrapper.attachDialogToLifecycle(
                        new AlertDialog.Builder(v.getContext(), R.style.FullScreenDialog)
                                .setView(gridSelection.getRoot()).show(), null);
            });
        }
    }
}



