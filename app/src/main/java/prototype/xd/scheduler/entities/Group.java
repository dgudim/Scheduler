package prototype.xd.scheduler.entities;

import java.io.File;
import java.io.FileInputStream;
import java.io.ObjectInputStream;
import java.util.ArrayList;

import static prototype.xd.scheduler.utilities.Logger.log;
import static prototype.xd.scheduler.utilities.Utilities.rootDir;

public class Group {

    String name = "default";

    String[] params = new String[]{"name", "default"};

    Group(String groupName) {
        File groupFile = new File(rootDir, "groups");
        ArrayList<Group> groups = new ArrayList<>();
        try {
            FileInputStream f = new FileInputStream(groupFile);
            ObjectInputStream s = new ObjectInputStream(f);
            groups = (ArrayList<Group>) s.readObject();
            s.close();
        } catch (Exception e) {
            log("no groups file");
        }
        for (int i = 0; i < groups.size(); i++) {
            if (groups.get(i).name.equals(groupName)) {
                params = groups.get(i).params;
                break;
            }
        }
    }
}
