package prototype.xd.scheduler.entities;

import android.content.Context;
import android.util.ArrayMap;

import androidx.annotation.Nullable;

import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.Serializable;
import java.util.List;
import java.util.Objects;
import java.util.Set;

import prototype.xd.scheduler.R;
import prototype.xd.scheduler.utilities.Logger;
import prototype.xd.scheduler.utilities.SArrayMap;
import prototype.xd.scheduler.utilities.Utilities;

public class Group implements Serializable {
    
    private static final String NAME = "Group";
    
    public static final transient Group NULL_GROUP = new Group();
    
    private String groupName;
    private transient ArrayMap<Long, TodoEntry> associatedEntries;
    protected SArrayMap<String, String> params;
    
    public Group() {
        groupName = "";
        params = new SArrayMap<>();
        associatedEntries = new ArrayMap<>();
    }
    
    public Group(String groupName, SArrayMap<String, String> params) {
        this.groupName = groupName;
        this.params = params;
        associatedEntries = new ArrayMap<>();
    }
    
    private void readObject(ObjectInputStream in)
            throws IOException, ClassNotFoundException {
        in.defaultReadObject();
        associatedEntries = new ArrayMap<>();
    }
    
    // to be called only by TodoListEntry
    protected void attachEntryInternal(TodoEntry todoEntry) {
        if (associatedEntries.put(todoEntry.getId(), todoEntry) != null) {
            Logger.warning(NAME, "attachEntryInternal called with " + todoEntry + " but it's already attached");
        }
    }
    
    // to be called only by TodoListEntry
    protected void detachEntryInternal(TodoEntry todoEntry) {
        if (associatedEntries.remove(todoEntry.getId()) == null) {
            Logger.warning(NAME, "detachEntryInternal called with " + todoEntry + " but it's not attached");
        }
    }
    
    public void detachAllEntries() {
        associatedEntries.forEach((aLong, todoListEntry) -> todoListEntry.unlinkGroupInternal(true));
        associatedEntries.clear();
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
    
    public void setParams(SArrayMap<String, String> newParams) {
        Set<String> changedKeys = Utilities.symmetricDifference(params, newParams);
        associatedEntries.forEach((aLong, todoListEntry) -> todoListEntry.invalidateParameters(changedKeys));
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
