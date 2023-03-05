package prototype.xd.scheduler.entities.settings_entries;

import static prototype.xd.scheduler.entities.settings_entries.SettingsEntryType.IMPORT_EXPORT_SETTINGS;

import android.annotation.SuppressLint;
import android.content.ClipDescription;

import androidx.annotation.NonNull;

import java.io.File;
import java.util.List;

import prototype.xd.scheduler.databinding.ImportExportSettingsEntryBinding;
import prototype.xd.scheduler.utilities.Utilities;
import prototype.xd.scheduler.utilities.misc.ContextWrapper;
import prototype.xd.scheduler.utilities.misc.SettingsExporter;

public class ImportExportSettingsEntryConfig extends SettingsEntryConfig {
    
    @Override
    public int getRecyclerViewType() {
        return IMPORT_EXPORT_SETTINGS.ordinal();
    }
    
    static class ViewHolder extends SettingsEntryConfig.SettingsViewHolder<ImportExportSettingsEntryBinding, ImportExportSettingsEntryConfig> {
        
        @SuppressLint("InlinedApi")
        ViewHolder(@NonNull final ContextWrapper wrapper,
                   @NonNull final ImportExportSettingsEntryBinding viewBinding) {
            super(wrapper, viewBinding);
            viewBinding.exportSettingsButton.setOnClickListener(v -> {
                File export = SettingsExporter.exportSettings(wrapper.context);
                if (export != null) {
                    Utilities.shareFiles(wrapper.context, ClipDescription.MIMETYPE_UNKNOWN, List.of(export));
                }
            });
            viewBinding.importSettingsButton.setOnClickListener(v -> {
                //SettingsExporter.importSettings(wrapper.context);
                //Utilities.switchActivity(wrapper.activity, MainActivity.class);
            });
        }
        
        @Override
        void bind(ImportExportSettingsEntryConfig config) {
            // nothing special required
        }
    }
}
