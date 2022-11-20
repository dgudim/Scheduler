package prototype.xd.scheduler.entities;

import android.content.Context;

import androidx.annotation.Nullable;

import java.io.Serializable;
import java.util.List;
import java.util.Objects;

import prototype.xd.scheduler.R;
import prototype.xd.scheduler.utilities.SSMap;

public class Group implements Serializable {
    
    public static final transient Group NULL_GROUP = new Group();
    
    private String groupName = "";
    
    protected SSMap params = new SSMap();
    
    public Group() {
    }
    
    public Group(String groupName, SSMap params) {
        this.groupName = groupName;
        this.params = params;
    }
    
    public boolean isNullGroup() {
        return groupName.isEmpty();
    }
    
    public static int groupIndexInList(List<Group> groups, String groupName) {
        for (int i = 0; i < groups.size(); i++) {
            if (groups.get(i).groupName.equals(groupName)) {
                return i;
            }
        }
        return -1;
    }
    
    public static @Nullable
    Group findGroupInList(List<Group> groups, String groupName) {
        int index = groupIndexInList(groups, groupName);
        return index == -1 ? null : groups.get(index);
    }
    
    public static String[] groupListToNames(List<Group> groups, Context context) {
        String[] names = new String[groups.size()];
        for(int i = 0; i < groups.size(); i++) {
            names[i] = groups.get(i).getLocalizedName(context);
        }
        return names;
    }
    
    public String getLocalizedName(Context context) {
        return isNullGroup() ? context.getString(R.string.blank_group_name) : groupName;
    }
    
    public String getRawName() {
        return groupName;
    }
    
    public void setName(String newName) {
        groupName = newName;
    }
    
    public void setParams(SSMap newParams) {
        params = newParams;
    }
    
    @Override
    public int hashCode() {
        return Objects.hash(params, groupName);
    }
    
    @Override
    public boolean equals(@Nullable Object obj) {
        if (obj == null) {
            return false;
        } else if (obj == this) {
            return true;
        } else if (obj instanceof Group) {
            Group group = (Group) obj;
            return params.equals(group.params) && groupName.equals(group.groupName);
        }
        return super.equals(obj);
    }
}
