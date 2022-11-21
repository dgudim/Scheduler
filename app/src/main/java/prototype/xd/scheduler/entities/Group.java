package prototype.xd.scheduler.entities;

import android.content.Context;

import androidx.annotation.Nullable;
import androidx.collection.ArrayMap;

import java.io.Serializable;
import java.util.Objects;

import prototype.xd.scheduler.R;
import prototype.xd.scheduler.utilities.SSMap;

public class Group implements Serializable {
    
    public static final transient Group NULL_GROUP = new Group();
    
    private String groupName;
    private final ArrayMap<Long, TodoListEntry> associatedEntries = new ArrayMap<>();
    protected SSMap params;
    
    public Group() {
        groupName = "";
        params = new SSMap();
    }
    
    public Group(String groupName, SSMap params) {
        this.groupName = groupName;
        this.params = params;
    }
    
    // to be called only by TodoListEntry
    protected void attachEntryInternal(TodoListEntry todoListEntry) {
        associatedEntries.put(todoListEntry.getId(), todoListEntry);
    }
    
    // to be called only by TodoListEntry
    protected void detachEntryInternal(TodoListEntry todoListEntry) {
        associatedEntries.remove(todoListEntry.getId());
    }
    
    public void detachEntry(TodoListEntry todoListEntry) {
        todoListEntry.unlinkGroupInternal();
        associatedEntries.remove(todoListEntry.getId());
    }
    
    public void detachAllEntries() {
        for(TodoListEntry entry: associatedEntries.values()) {
            entry.unlinkGroupInternal();
        }
        associatedEntries.clear();
    }
    
    public boolean isNullGroup() {
        return groupName.isEmpty();
    }
    
    public static int groupIndexInList(GroupList groups, String groupName) {
        for (int i = 0; i < groups.size(); i++) {
            if (groups.get(i).groupName.equals(groupName)) {
                return i;
            }
        }
        return -1;
    }
    
    public static @Nullable
    Group findGroupInList(GroupList groups, String groupName) {
        int index = groupIndexInList(groups, groupName);
        return index == -1 ? null : groups.get(index);
    }
    
    public static String[] groupListToNames(GroupList groups, Context context) {
        String[] names = new String[groups.size()];
        for (int i = 0; i < groups.size(); i++) {
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
