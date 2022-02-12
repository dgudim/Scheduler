package prototype.xd.scheduler.entities;

import static prototype.xd.scheduler.utilities.Logger.ContentType.ERROR;
import static prototype.xd.scheduler.utilities.Logger.ContentType.INFO;
import static prototype.xd.scheduler.utilities.Logger.ContentType.WARNING;
import static prototype.xd.scheduler.utilities.Logger.log;
import static prototype.xd.scheduler.utilities.Logger.logException;
import static prototype.xd.scheduler.utilities.Utilities.loadObject;
import static prototype.xd.scheduler.utilities.Utilities.saveObject;

import android.content.Context;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Objects;

import prototype.xd.scheduler.R;

public class Group {
    
    private boolean isNullGroup = false;
    private String name;
    
    String[] params = new String[]{};
    
    public Group(Context context) {
        name = context.getString(R.string.blank_group_name);
        isNullGroup = true;
    }
    
    public boolean isNullGroup() {
        return isNullGroup;
    }
    
    public Group(Context context, String groupName, ArrayList<Group> groups) {
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
        } else {
            name = context.getString(R.string.blank_group_name);
            isNullGroup = true;
        }
    }
    
    public Group(String groupName, String[] params) {
        name = groupName;
        this.params = params;
    }
    
    public String getName() {
        if (isNullGroup) {
            return "";
        }
        return name;
    }
    
    public void setName(String newName) {
        name = newName;
    }
    
    public static int groupIndexInList(ArrayList<Group> groupList, String groupName) {
        int groupIndex = -1;
        for (int i = 0; i < groupList.size(); i++) {
            if (groupList.get(i).getName().equals(groupName)) {
                groupIndex = i;
                break;
            }
        }
        return groupIndex;
    }
    
    public static ArrayList<Group> readGroupFile(Context context) {
        ArrayList<Group> groups = new ArrayList<>();
        groups.add(new Group(context)); // add "null" group
        try {
            
            ArrayList<String[]> groupParams = loadObject("groups");
            ArrayList<String> groupNames = loadObject("groupNames");
            
            if (!(groupParams.size() == groupNames.size())) {
                log(WARNING, "groupParams length: " + groupParams.size() + " groupNames length: " + groupNames.size());
            }
            
            for (int i = 0; i < groupParams.size(); i++) {
                groups.add(new Group(groupNames.get(i), groupParams.get(i)));
            }
            
            return groups;
        } catch (Exception e) {
            logException(e);
            log(INFO, "no groups file, creating one");
            saveGroupsFile(groups);
            return groups;
        }
    }
    
    public static void saveGroupsFile(ArrayList<Group> groups) {
        try {
            
            ArrayList<String[]> groupParams = new ArrayList<>();
            ArrayList<String> groupNames = new ArrayList<>();
            for (int i = 0; i < groups.size(); i++) {
                if (!groups.get(i).isNullGroup) {
                    groupParams.add(groups.get(i).params);
                    groupNames.add(groups.get(i).name);
                }
            }
            
            saveObject("groups", groupParams);
            saveObject("groupNames", groupNames);
            
            log(INFO, "saving groups file");
            
        } catch (Exception e) {
            log(ERROR, "failed to save groups file: " + e.getMessage());
        }
    }
    
    @NonNull
    @Override
    public String toString() {
        return name;
    }
    
    @Override
    public int hashCode() {
        return Objects.hash(Arrays.hashCode(params), name);
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
            return Arrays.equals(params, group.params) && name.equals(group.name);
        }
        return super.equals(obj);
    }
}
