package prototype.xd.scheduler.entities;

import static android.util.Log.ERROR;
import static android.util.Log.INFO;
import static android.util.Log.WARN;
import static prototype.xd.scheduler.utilities.Logger.log;
import static prototype.xd.scheduler.utilities.Logger.logException;
import static prototype.xd.scheduler.utilities.Utilities.loadObject;
import static prototype.xd.scheduler.utilities.Utilities.saveObject;

import android.content.Context;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Objects;

import prototype.xd.scheduler.R;

public class Group {
    
    private static final String NAME = "Entry group";
    
    private boolean isNullGroup = false;
    private String groupName;
    
    String[] params = new String[]{};
    
    public Group(Context context) {
        groupName = context.getString(R.string.blank_group_name);
        isNullGroup = true;
    }
    
    public boolean isNullGroup() {
        return isNullGroup;
    }
    
    public Group(Context context, String groupName, List<Group> groups) {
        boolean foundGroup = false;
        for (int i = 0; i < groups.size(); i++) {
            if (groups.get(i).groupName.equals(groupName)) {
                params = groups.get(i).params;
                foundGroup = true;
                break;
            }
        }
        if (foundGroup) {
            this.groupName = groupName;
        } else {
            this.groupName = context.getString(R.string.blank_group_name);
            isNullGroup = true;
        }
    }
    
    public Group(String groupName, String[] params) {
        this.groupName = groupName;
        this.params = params;
    }
    
    public String getName() {
        if (isNullGroup) {
            return "";
        }
        return groupName;
    }
    
    public void setName(String newName) {
        groupName = newName;
    }
    
    public static int groupIndexInList(List<Group> groupList, String groupName) {
        int groupIndex = -1;
        for (int i = 0; i < groupList.size(); i++) {
            if (groupList.get(i).getName().equals(groupName)) {
                groupIndex = i;
                break;
            }
        }
        return groupIndex;
    }
    
    public static List<Group> readGroupFile(Context context) {
        List<Group> groups = new ArrayList<>();
        groups.add(new Group(context)); // add "null" group
        try {
    
            List<String[]> groupParams = loadObject("groups");
            List<String> groupNames = loadObject("groupNames");
            
            if (groupParams.size() != groupNames.size()) {
                log(WARN, NAME, "groupParams length: " + groupParams.size() + " groupNames length: " + groupNames.size());
            }
            
            for (int i = 0; i < groupParams.size(); i++) {
                groups.add(new Group(groupNames.get(i), groupParams.get(i)));
            }
            
            return groups;
        } catch (Exception e) {
            logException(NAME, e);
            log(INFO, NAME, "no groups file, creating one");
            saveGroupsFile(groups);
            return groups;
        }
    }
    
    public static void saveGroupsFile(List<Group> groups) {
        try {
    
            List<String[]> groupParams = new ArrayList<>();
            List<String> groupNames = new ArrayList<>();
            for (int i = 0; i < groups.size(); i++) {
                if (!groups.get(i).isNullGroup) {
                    groupParams.add(groups.get(i).params);
                    groupNames.add(groups.get(i).groupName);
                }
            }
            
            saveObject("groups", groupParams);
            saveObject("groupNames", groupNames);
            
            log(INFO, NAME, "saving groups file");
            
        } catch (Exception e) {
            log(ERROR, NAME, "failed to save groups file: " + e.getMessage());
        }
    }
    
    @NonNull
    @Override
    public String toString() {
        return groupName;
    }
    
    @Override
    public int hashCode() {
        return Objects.hash(Arrays.hashCode(params), groupName);
    }
    
    @Override
    public boolean equals(@Nullable Object obj) {
        if (obj == null) {
            return isNullGroup;
        } else if (obj == this) {
            return true;
        } else if (obj instanceof Group) {
            Group group = (Group) obj;
            if (isNullGroup && group.isNullGroup) {
                return true;
            }
            return Arrays.equals(params, group.params) && groupName.equals(group.groupName);
        }
        return super.equals(obj);
    }
}
