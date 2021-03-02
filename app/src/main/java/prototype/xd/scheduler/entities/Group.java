package prototype.xd.scheduler.entities;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.util.ArrayList;

import static prototype.xd.scheduler.utilities.Logger.ERROR;
import static prototype.xd.scheduler.utilities.Logger.INFO;
import static prototype.xd.scheduler.utilities.Logger.WARNING;
import static prototype.xd.scheduler.utilities.Logger.log;
import static prototype.xd.scheduler.utilities.Utilities.rootDir;

public class Group {

    public String name = "default";

    String[] params = new String[]{};
    static File groupFile = new File(rootDir, "groups");
    static File groupFile_names = new File(rootDir, "groupNames");

    public Group(String groupName) {
        ArrayList<Group> groups = readGroupFile();
        boolean foundGroup = false;
        for (int i = 0; i < groups.size(); i++) {
            if (groups.get(i).name.equals(groupName)) {
                params = groups.get(i).params;
                foundGroup = true;
                break;
            }
        }
        if (foundGroup) {
            name = groupName;
        }
    }

    private Group(String groupName, String[] params) {
        name = groupName;
        this.params = params;
    }

    private static ArrayList<Group> readGroupFile() {
        try {
            FileInputStream f = new FileInputStream(groupFile);
            ObjectInputStream s = new ObjectInputStream(f);

            FileInputStream f_g = new FileInputStream(groupFile_names);
            ObjectInputStream s_g = new ObjectInputStream(f_g);

            ArrayList<String[]> groupParams = (ArrayList<String[]>) s.readObject();
            ArrayList<String> groupNames = (ArrayList<String>) s_g.readObject();
            s.close();
            s_g.close();

            if (!(groupParams.size() == groupNames.size())) {
                log(WARNING, "groupParams length: " + groupParams.size() + " groupNames length: " + groupNames.size());
            }

            ArrayList<Group> groups = new ArrayList<>();
            for (int i = 0; i < groupParams.size(); i++) {
                groups.add(new Group(groupNames.get(i), groupParams.get(i)));
            }

            return groups;
        } catch (Exception e) {
            log(INFO, "no groups file, creating one");
            return createDefaultGroupFile();
        }
    }

    private static void saveGroupsFile(ArrayList<Group> groups) {
        try {
            FileOutputStream fos = new FileOutputStream(groupFile);
            ObjectOutputStream s = new ObjectOutputStream(fos);

            FileOutputStream fos_g = new FileOutputStream(groupFile_names);
            ObjectOutputStream s_g = new ObjectOutputStream(fos_g);

            ArrayList<String[]> groupParams = new ArrayList<>();
            ArrayList<String> groupNames = new ArrayList<>();
            for (int i = 0; i < groups.size(); i++) {
                groupParams.add(groups.get(i).params);
                groupNames.add(groups.get(i).name);
            }

            s.writeObject(groupParams);
            s.close();
            s_g.writeObject(groupNames);
            s_g.close();
        } catch (Exception e) {
            log(ERROR, "missing permission, failed to save groups file");
        }
    }

    public static void createGroup(String name, String[] params) {
        ArrayList<Group> groups = readGroupFile();
        groups.add(new Group(name, params));
        saveGroupsFile(groups);
    }

    private static ArrayList<Group> createDefaultGroupFile() {
        ArrayList<Group> groups = new ArrayList<>();
        groups.add(new Group("default", new String[]{}));
        saveGroupsFile(groups);
        return groups;
    }
}
