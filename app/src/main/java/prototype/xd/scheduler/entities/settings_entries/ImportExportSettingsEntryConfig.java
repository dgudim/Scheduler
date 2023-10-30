package prototype.xd.scheduler.entities.settings_entries;

import static prototype.xd.scheduler.entities.settings_entries.SettingsEntryType.IMPORT_EXPORT_SETTINGS;
import static prototype.xd.scheduler.utilities.Logger.logException;
import static prototype.xd.scheduler.utilities.Utilities.displayToast;
import static prototype.xd.scheduler.utilities.Utilities.getFile;

import android.annotation.SuppressLint;
import android.net.Uri;

import androidx.activity.result.ActivityResultLauncher;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import net.lingala.zip4j.ZipFile;

import java.io.File;
import java.io.InputStream;
import java.io.OutputStream;
import java.nio.file.Files;
import java.nio.file.StandardCopyOption;

import prototype.xd.scheduler.R;
import prototype.xd.scheduler.databinding.ImportExportSettingsEntryBinding;
import prototype.xd.scheduler.utilities.DateManager;
import prototype.xd.scheduler.utilities.Static;
import prototype.xd.scheduler.utilities.misc.ContextWrapper;
import prototype.xd.scheduler.utilities.misc.SettingsExporter;
import prototype.xd.scheduler.utilities.services.BackgroundSetterService;

public class ImportExportSettingsEntryConfig extends SettingsEntryConfig {
    
    public static final String NAME = ImportExportSettingsEntryConfig.class.getSimpleName();
    
    private final ActivityResultLauncher<String[]> importLauncher;
    private final ActivityResultLauncher<String> exportLauncher;
    
    public static final String MIMETYPE = "application/zip";
    
    public ImportExportSettingsEntryConfig(@NonNull final ActivityResultLauncher<String[]> importLauncher,
                                           @NonNull final ActivityResultLauncher<String> exportLauncher) {
        this.importLauncher = importLauncher;
        this.exportLauncher = exportLauncher;
    }
    
    private void launchImportPicker() {
        importLauncher.launch(new String[]{MIMETYPE});
    }
    
    private void launchExportPicker() {
        exportLauncher.launch(Static.EXPORT_FILE);
    }
    
    @Override
    public int getRecyclerViewType() {
        return IMPORT_EXPORT_SETTINGS.ordinal();
    }
    
    @SuppressWarnings({"StaticMethodOnlyUsedInOneClass", "CallToSystemExit"})
    public static void notifyFileChosen(@NonNull ContextWrapper wrapper, @Nullable Uri uri) {
        wrapper.uriToStream(uri, InputStream.class, stream -> {
            File tempFile = getFile(Static.EXPORT_FILE);
            Files.copy(stream, tempFile.toPath(), StandardCopyOption.REPLACE_EXISTING);
            
            try (var zip = new ZipFile(tempFile)) {
                zip.extractAll(wrapper.context.getExternalFilesDir(null).getPath());
            }
            
            SettingsExporter.importSettings();
            
            displayToast(wrapper.context, R.string.import_settings_successful);
            try {
                // Wait for toast
                Thread.sleep(500);
            } catch (Exception e) { // NOSONAR
                logException(NAME, e);
                // ignore
            }
    
            BackgroundSetterService.exit(wrapper.context);
            wrapper.activity.finishAndRemoveTask();
            System.exit(0);
        }, fail -> displayToast(wrapper.context,
                fail ? R.string.import_settings_failed : R.string.import_settings_canceled));
    }
    
    @SuppressWarnings("StaticMethodOnlyUsedInOneClass")
    public static void notifySaveLocationChosen(@NonNull ContextWrapper wrapper, @Nullable Uri uri) {
        wrapper.uriToStream(uri, OutputStream.class, stream -> {
            File tempFile = getFile(Static.EXPORT_FILE);
            Files.deleteIfExists(tempFile.toPath());
            try (var zip = new ZipFile(tempFile)) {
                zip.addFile(SettingsExporter.exportSettings());
                
                for (String bgName : DateManager.BG_NAMES_ROOT) {
                    File bgFile = getFile(bgName);
                    if (bgFile.exists()) {
                        zip.addFile(bgFile);
                    }
                }
                
                Files.copy(tempFile.toPath(), stream);
                
                displayToast(wrapper.context, R.string.export_settings_successful);
            }
        }, fail -> displayToast(wrapper.context,
                fail ? R.string.export_settings_failed : R.string.export_settings_canceled));
    }
    
    static class ViewHolder extends SettingsEntryConfig.SingleBindSettingsViewHolder<ImportExportSettingsEntryBinding, ImportExportSettingsEntryConfig> {
        ViewHolder(@NonNull final ContextWrapper wrapper,
                   @NonNull final ImportExportSettingsEntryBinding viewBinding) {
            super(wrapper, viewBinding);
        }
        
        @SuppressLint("InlinedApi")
        @Override
        void bind(ImportExportSettingsEntryConfig config) {
            viewBinding.exportSettingsButton.setOnClickListener(v -> config.launchExportPicker());
            viewBinding.importSettingsButton.setOnClickListener(v -> config.launchImportPicker());
        }
    }
}
