package prototype.xd.scheduler.utilities;

import android.os.Environment;

import org.apache.commons.lang.WordUtils;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
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

            ArrayList<String[]> entryParams = (ArrayList<String[]>) loadObject("list");
            ArrayList<String> entryGroupNames = (ArrayList<String>) loadObject("list_groupData");

            if (!(entryParams.size() == entryGroupNames.size())) {
                log(WARNING, "entryParams length: " + entryParams.size() + " entryGroupNames length: " + entryGroupNames.size());
            }

            ArrayList<TodoListEntry> readEntries = new ArrayList<>();
            for (int i = 0; i < entryParams.size(); i++) {
                readEntries.add(new TodoListEntry(entryParams.get(i), entryGroupNames.get(i)));
            }

            return sortEntries(readEntries);
        } catch (Exception e) {
            log(INFO, "no todo list");
            return new ArrayList<>();
        }
    }

    public static void saveEntries(ArrayList<TodoListEntry> entries) {
        try {
            ArrayList<String[]> entryParams = new ArrayList<>();
            ArrayList<String> entryGroupNames = new ArrayList<>();

            for (int i = 0; i < entries.size(); i++) {
                entryParams.add(entries.get(i).params);
                entryGroupNames.add(entries.get(i).group.name);
            }

            saveObject("list", entryParams);
            saveObject("list_groupData", entryGroupNames);
            log(INFO, "saving todo list");
        } catch (Exception e) {
            log(ERROR, "missing permission, failed to save todo list");
        }
    }

    public static Object loadObject(String fileName) throws IOException, ClassNotFoundException {
        File file = new File(rootDir, fileName);
        FileInputStream f = new FileInputStream(file);
        ObjectInputStream s = new ObjectInputStream(f);
        Object object = s.readObject();
        s.close();
        return object;
    }

    public static void saveObject(String fileName, Object object) throws IOException {
        File file = new File(rootDir, fileName);
        FileOutputStream f = new FileOutputStream(file);
        ObjectOutputStream s = new ObjectOutputStream(f);
        s.writeObject(object);
        s.close();
    }

    private static ArrayList<TodoListEntry> sortEntries(ArrayList<TodoListEntry> entries) {
        ArrayList<TodoListEntry> yesterdayEntries = new ArrayList<>();
        ArrayList<TodoListEntry> todayEntries = new ArrayList<>();
        ArrayList<TodoListEntry> globalEntries = new ArrayList<>();
        ArrayList<TodoListEntry> otherEntries = new ArrayList<>();
        for (int i = 0; i < entries.size(); i++) {
            if (entries.get(i).isTodayEntry) {
                todayEntries.add(entries.get(i));
            } else if (entries.get(i).isYesterdayEntry) {
                yesterdayEntries.add(entries.get(i));
            } else if (entries.get(i).isGlobalEntry) {
                globalEntries.add(entries.get(i));
            } else {
                otherEntries.add(entries.get(i));
            }
        }
        ArrayList<TodoListEntry> merged = new ArrayList<>();
        merged.addAll(todayEntries);
        merged.addAll(globalEntries);
        merged.addAll(yesterdayEntries);
        merged.addAll(otherEntries);
        return merged;
    }

    public static String[] makeNewLines(String input, int maxChars) {
        return WordUtils.wrap(input, maxChars, "\n", true).split("\n");
    }
}
