package prototype.xd.scheduler.utilities.misc;

import static prototype.xd.scheduler.utilities.Static.SETTINGS_FILE;
import static prototype.xd.scheduler.utilities.Static.SETTINGS_FILE_BACKUP;
import static prototype.xd.scheduler.utilities.Utilities.loadObjectWithBackup;

import android.content.SharedPreferences;

import androidx.annotation.NonNull;

import java.io.File;
import java.io.IOException;
import java.io.Serializable;
import java.util.Collection;
import java.util.Map;

import prototype.xd.scheduler.entities.Group;
import prototype.xd.scheduler.entities.TodoEntry;
import prototype.xd.scheduler.utilities.Logger;
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
        Logger.info(NAME, ">>>>>>>>> Settings export created with " + entries.size() + " entries, " + groups.size() + " groups and " + prefs.size() + " prefs");
    }
    
    @NonNull
    public static File exportSettings() throws IOException, ClassNotFoundException {
        return Utilities.saveObjectWithBackup(SETTINGS_FILE, SETTINGS_FILE_BACKUP, new SettingsExporter());
    }
    
    public static void importSettings() throws IOException, ClassNotFoundException {
        SettingsExporter exporter = loadObjectWithBackup(SETTINGS_FILE, SETTINGS_FILE_BACKUP);
        Utilities.saveToEntriesFile(exporter.entries);
        Utilities.saveToGroupsFile(exporter.groups);
        SharedPreferences.Editor editor = Static.clearAll();
        exporter.prefs.forEach((key, value) -> Static.putAnyEditor(editor, key, value));
        // Overwrite immediately
        editor.commit();
        Logger.info(NAME, ">>>>>>>>> Settings imported: "
                + exporter.entries.size() + " entries, "
                + exporter.groups.size() + " groups and "
                + exporter.prefs.size() + " prefs");
    }
    
}
