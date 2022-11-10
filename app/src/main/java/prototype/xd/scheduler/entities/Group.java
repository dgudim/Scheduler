package prototype.xd.scheduler.entities;

import android.content.Context;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import java.util.Arrays;
import java.util.List;
import java.util.Objects;

import prototype.xd.scheduler.R;

public class Group {
    
    private static final String NAME = "Entry group";
    
    private boolean isNullGroup = false;
    private String groupName;
    
    protected String[] params = new String[]{};
    
    public Group(Context context) {
        groupName = context.getString(R.string.blank_group_name);
        isNullGroup = true;
    }
    
    public String[] getParams() {
        return params;
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
