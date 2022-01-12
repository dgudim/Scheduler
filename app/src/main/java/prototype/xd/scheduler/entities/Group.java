package prototype.xd.scheduler.entities;

import static prototype.xd.scheduler.utilities.Logger.ContentType.ERROR;
import static prototype.xd.scheduler.utilities.Logger.ContentType.INFO;
import static prototype.xd.scheduler.utilities.Logger.ContentType.WARNING;
import static prototype.xd.scheduler.utilities.Logger.log;
import static prototype.xd.scheduler.utilities.Logger.logException;
import static prototype.xd.scheduler.utilities.Utilities.loadObject;
import static prototype.xd.scheduler.utilities.Utilities.saveObject;

import androidx.annotation.NonNull;

import java.util.ArrayList;

public class Group {

    public static final String BLANK_NAME = "Ничего";
    public String name = BLANK_NAME;

    String[] params = new String[]{};

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

    public static ArrayList<Group> readGroupFile() {
        try {

            ArrayList<String[]> groupParams = loadObject("groups");
            ArrayList<String> groupNames = loadObject("groupNames");

            if (!(groupParams.size() == groupNames.size())) {
                log(WARNING, "groupParams length: " + groupParams.size() + " groupNames length: " + groupNames.size());
            }

            ArrayList<Group> groups = new ArrayList<>();
            for (int i = 0; i < groupParams.size(); i++) {
                groups.add(new Group(groupNames.get(i), groupParams.get(i)));
            }

            return groups;
        } catch (Exception e) {
            logException(e);
            log(INFO, "no groups file, creating one");
            return createDefaultGroupFile();
        }
    }

    public static void saveGroupsFile(ArrayList<Group> groups) {
        try {

            ArrayList<String[]> groupParams = new ArrayList<>();
            ArrayList<String> groupNames = new ArrayList<>();
            for (int i = 0; i < groups.size(); i++) {
                groupParams.add(groups.get(i).params);
                groupNames.add(groups.get(i).name);
            }

            saveObject("groups", groupParams);
            saveObject("groupNames", groupNames);

            log(INFO, "saving groups file");

        } catch (Exception e) {
            log(ERROR, "failed to save groups file: " + e.getMessage());
        }
    }

    public static Group createGroup(String name, String[] params) {
        ArrayList<Group> groups = readGroupFile();
        Group createdGroup = new Group(name, params);
        groups.add(createdGroup);
        saveGroupsFile(groups);
        return createdGroup;
    }

    private static ArrayList<Group> createDefaultGroupFile() {
        ArrayList<Group> groups = new ArrayList<>();
        groups.add(new Group(BLANK_NAME, new String[]{}));
        saveGroupsFile(groups);
        return groups;
    }

    @NonNull
    @Override
    public String toString() {
        return name;
    }
}
