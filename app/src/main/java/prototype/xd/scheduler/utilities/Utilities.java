package prototype.xd.scheduler.utilities;

import android.os.Environment;

import org.apache.commons.lang.WordUtils;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.util.ArrayList;

import prototype.xd.scheduler.entities.TodoListEntry;

import static prototype.xd.scheduler.utilities.Logger.ERROR;
import static prototype.xd.scheduler.utilities.Logger.INFO;
import static prototype.xd.scheduler.utilities.Logger.WARNING;
import static prototype.xd.scheduler.utilities.Logger.log;

public class Utilities {

    public static File rootDir = new File(Environment.getExternalStorageDirectory().toString() + "/.Scheduler");

    public static void createRootIfNeeded() {
        if (!rootDir.exists()) {
            rootDir.mkdirs();
        }
    }

    @SuppressWarnings("unchecked")
    public static ArrayList<TodoListEntry> loadEntries() {
        try {
            File file = new File(rootDir, "list");
            File file_g = new File(rootDir, "list_groupData");

            FileInputStream f = new FileInputStream(file);
            ObjectInputStream s = new ObjectInputStream(f);
            FileInputStream f_g = new FileInputStream(file_g);
            ObjectInputStream s_g = new ObjectInputStream(f_g);
            // TODO: 01.03.2021 add function to save(load) an object

            ArrayList<String[]> entryParams = (ArrayList<String[]>) s.readObject();
            ArrayList<String> entryGroupNames = (ArrayList<String>) s_g.readObject();
            s.close();
            s_g.close();

            if (!(entryParams.size() == entryGroupNames.size())) {
                log(WARNING, "entryParams length: " + entryParams.size() + " entryGroupNames length: " + entryGroupNames.size());
                throw new IllegalAccessException();
            }

            ArrayList<TodoListEntry> readEntries = new ArrayList<>();
            for (int i = 0; i < entryParams.size(); i++) {
                readEntries.add(new TodoListEntry(entryParams.get(i), entryGroupNames.get(i)));
            }

            return readEntries;
        } catch (Exception e) {
            log(INFO, "no todo list");
            return new ArrayList<>();
        }
    }

    public static void saveEntries(ArrayList<TodoListEntry> entries) {
        try {
            File file = new File(rootDir, "list");
            File file_g = new File(rootDir, "list_groupData");
            ArrayList<String[]> entryParams = new ArrayList<>();
            ArrayList<String> entryGroupNames = new ArrayList<>();

            for (int i = 0; i < entries.size(); i++) {
                entryParams.add(entries.get(i).params);
                entryGroupNames.add(entries.get(i).group.name);
            }

            FileOutputStream fos = new FileOutputStream(file);
            ObjectOutputStream s = new ObjectOutputStream(fos);
            s.writeObject(entryParams);
            s.close();

            FileOutputStream fos_g = new FileOutputStream(file_g);
            ObjectOutputStream s_g = new ObjectOutputStream(fos_g);
            s_g.writeObject(entryGroupNames);
            s_g.close();
        } catch (Exception e) {
            log(ERROR, "missing permission, failed to save todo list");
        }
    }

    public static String[] makeNewLines(String input, int maxChars) {
        return WordUtils.wrap(input, maxChars, "\n", true).split("\n");
    }
}
