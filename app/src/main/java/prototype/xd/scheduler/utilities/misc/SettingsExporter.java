package prototype.xd.scheduler.utilities.misc;

import static prototype.xd.scheduler.utilities.Logger.logException;
import static prototype.xd.scheduler.utilities.Static.SETTINGS_FILE;
import static prototype.xd.scheduler.utilities.Static.SETTINGS_FILE_BACKUP;
import static prototype.xd.scheduler.utilities.Utilities.displayToast;

import android.content.Context;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.io.ObjectInputStream;
import java.io.Serializable;
import java.util.Collection;
import java.util.Map;

import prototype.xd.scheduler.R;
import prototype.xd.scheduler.entities.Group;
import prototype.xd.scheduler.entities.TodoEntry;
import prototype.xd.scheduler.utilities.Static;
import prototype.xd.scheduler.utilities.Utilities;

public final class SettingsExporter implements Serializable {
    
    private static final long serialVersionUID = 123123123L;
    
    public static final String NAME = SettingsExporter.class.getSimpleName();
    
    private final Collection<TodoEntry> entries;
    private final Collection<Group> groups;
    private final Map<String, ?> prefs; // NOSONAR prefs return a HashMap
    
    private SettingsExporter() throws IOException, ClassNotFoundException {
        entries = Utilities.loadFromEntriesFile();
        groups = Utilities.loadFromGroupsFile();
        prefs = Static.getAll();
    }
    
    @Nullable
    public static File tryExportSettings(@NonNull Context context) {
        try {
            SettingsExporter exporter = new SettingsExporter();
            File file = Utilities.saveObjectWithBackup(SETTINGS_FILE, SETTINGS_FILE_BACKUP, exporter);
            displayToast(context, R.string.export_settings_successful);
            return file;
        } catch (IOException | ClassNotFoundException e) {
            logException(NAME, e);
            displayToast(context, R.string.export_settings_failed);
            return null;
        }
    }
    
    public static boolean tryImportSettings(@NonNull Context context, @NonNull InputStream stream) {
        try {
            try (ObjectInputStream objectStream = new ObjectInputStream(stream)) {
                SettingsExporter exporter = (SettingsExporter) objectStream.readObject();
                Utilities.saveToEntriesFile(exporter.entries);
                Utilities.saveToGroupsFile(exporter.groups);
                Static.clearAll();
                for (Map.Entry<String, ?> entry : exporter.prefs.entrySet()) {
                    Static.putAny(entry.getKey(), entry.getValue());
                }
                displayToast(context, R.string.import_settings_successful);
                return true;
            }
        } catch (IOException | ClassNotFoundException e) {
            logException(NAME, e);
            displayToast(context, R.string.import_settings_failed);
            return false;
        }
    }
    
}
