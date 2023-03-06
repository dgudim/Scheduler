package prototype.xd.scheduler.entities.settings_entries;

import static prototype.xd.scheduler.entities.settings_entries.SettingsEntryType.IMPORT_EXPORT_SETTINGS;
import static prototype.xd.scheduler.utilities.Utilities.getFile;

import android.annotation.SuppressLint;
import android.content.ClipDescription;
import android.net.Uri;

import androidx.activity.result.ActivityResultLauncher;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import java.io.File;
import java.util.ArrayList;
import java.util.List;

import prototype.xd.scheduler.databinding.ImportExportSettingsEntryBinding;
import prototype.xd.scheduler.utilities.DateManager;
import prototype.xd.scheduler.utilities.Utilities;
import prototype.xd.scheduler.utilities.misc.ContextWrapper;
import prototype.xd.scheduler.utilities.misc.SettingsExporter;
import prototype.xd.scheduler.utilities.services.BackgroundSetterService;

public class ImportExportSettingsEntryConfig extends SettingsEntryConfig {
    
    private final ActivityResultLauncher<String[]> importLauncher;
    
    @SuppressLint("InlinedApi")
    private final String[] mimetypes = {
            ClipDescription.MIMETYPE_UNKNOWN
    };
    
    public ImportExportSettingsEntryConfig(@NonNull final ActivityResultLauncher<String[]> importLauncher) {
        this.importLauncher = importLauncher;
    }
    
    private void launchPicker() {
        importLauncher.launch(mimetypes);
    }
    
    @Override
    public int getRecyclerViewType() {
        return IMPORT_EXPORT_SETTINGS.ordinal();
    }
    
    @SuppressWarnings({"StaticMethodOnlyUsedInOneClass", "CallToSystemExit"})
    public static void notifyFileChosen(@NonNull ContextWrapper wrapper, @Nullable Uri uri) {
        wrapper.processUri(uri, stream -> {
            if (SettingsExporter.tryImportSettings(wrapper.context, stream)) {
                BackgroundSetterService.exit(wrapper.context);
                wrapper.activity.finishAndRemoveTask();
                System.exit(0);
            }
        });
    }
    
    static class ViewHolder extends SettingsEntryConfig.SingleBindSettingsViewHolder<ImportExportSettingsEntryBinding, ImportExportSettingsEntryConfig> {
        
        
        ViewHolder(@NonNull final ContextWrapper wrapper,
                   @NonNull final ImportExportSettingsEntryBinding viewBinding) {
            super(wrapper, viewBinding);
        }
        
        @SuppressLint("InlinedApi")
        @Override
        void bind(ImportExportSettingsEntryConfig config) {
            viewBinding.exportSettingsButton.setOnClickListener(v -> {
                File export = SettingsExporter.tryExportSettings(wrapper.context);
                if (export != null) {
    
                    List<File> allFiles = new ArrayList<>(DateManager.BG_NAMES_ROOT.size() + 1);
                    allFiles.add(export);
                    
                    for(String bgName: DateManager.BG_NAMES_ROOT) {
                        File bgFile = getFile(bgName);
                        if(bgFile.exists()) {
                            allFiles.add(bgFile);
                        }
                    }
                    Utilities.shareFiles(wrapper.context, ClipDescription.MIMETYPE_UNKNOWN, allFiles);
                }
            });
            viewBinding.importSettingsButton.setOnClickListener(v -> config.launchPicker());
        }
    }
}
