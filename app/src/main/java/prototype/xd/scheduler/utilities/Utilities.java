package prototype.xd.scheduler.utilities;

import android.os.Environment;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.util.ArrayList;

import prototype.xd.scheduler.entities.TodoListEntry;

import static prototype.xd.scheduler.utilities.Logger.log;

public class Utilities {

    public static File rootDir = new File(Environment.getExternalStorageDirectory().toString() + "/Scheduler");

    public static void createRootIfNeeded() {
        if (!rootDir.exists()) {
            rootDir.mkdirs();
        }
    }

    @SuppressWarnings("unchecked")
    public static ArrayList<TodoListEntry> loadEntries(String name) {
        try {
            File file = new File(rootDir, name);
            FileInputStream f = new FileInputStream(file);
            ObjectInputStream s = new ObjectInputStream(f);
            ArrayList<TodoListEntry> read = (ArrayList<TodoListEntry>) s.readObject();
            s.close();
            return read;
        } catch (Exception e) {
            log("no file for name " + name);
            return new ArrayList<>();
        }
    }

    public static void saveEntries(String name, ArrayList<TodoListEntry> entries) {
        try {
            File file = new File(rootDir, name);
            FileOutputStream fos = new FileOutputStream(file);
            ObjectOutputStream s = new ObjectOutputStream(fos);
            s.writeObject(entries);
            s.close();
        } catch (Exception e) {
            log("missing permission, failed to save " + name);
        }
    }



}
