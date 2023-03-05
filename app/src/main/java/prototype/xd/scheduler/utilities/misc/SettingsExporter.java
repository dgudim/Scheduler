package prototype.xd.scheduler.utilities.misc;

import static prototype.xd.scheduler.utilities.Logger.logException;
import static prototype.xd.scheduler.utilities.Static.ENTRIES_FILE;
import static prototype.xd.scheduler.utilities.Static.ENTRIES_FILE_BACKUP;
import static prototype.xd.scheduler.utilities.Static.GROUPS_FILE;
import static prototype.xd.scheduler.utilities.Static.GROUPS_FILE_BACKUP;
import static prototype.xd.scheduler.utilities.Static.SETTINGS_FILE;
import static prototype.xd.scheduler.utilities.Static.SETTINGS_FILE_BACKUP;
import static prototype.xd.scheduler.utilities.Utilities.displayToast;

import android.content.Context;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import java.io.File;
import java.io.IOException;
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
    private final Map<String, ?> prefs; // NOSONAR
    
    private SettingsExporter() throws IOException, ClassNotFoundException {
        entries = Utilities.loadObjectWithBackup(ENTRIES_FILE, ENTRIES_FILE_BACKUP);
        groups = Utilities.loadObjectWithBackup(GROUPS_FILE, GROUPS_FILE_BACKUP);
        prefs = Static.getAll();
    }
    
    @Nullable
    public static File exportSettings(@NonNull Context context) {
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
    
    public static void importSettings(@NonNull Context context) {
        try {
            SettingsExporter exporter = Utilities.loadObjectWithBackup(SETTINGS_FILE, SETTINGS_FILE_BACKUP);
            Utilities.saveToEntriesFile(exporter.entries);
            Utilities.saveToGroupsFile(exporter.groups);
            Static.clearAll();
            for (Map.Entry<String, ?> entry : exporter.prefs.entrySet()) {
                Static.putAny(entry.getKey(), entry.getValue());
            }
        } catch (IOException | ClassNotFoundException e) {
            logException(NAME, e);
            displayToast(context, R.string.import_settings_failed);
            return;
        }
        displayToast(context, R.string.import_settings_successful);
    }
    
}
