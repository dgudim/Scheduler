package prototype.xd.scheduler.entities;

import static android.util.Log.WARN;
import static prototype.xd.scheduler.utilities.Logger.log;

import android.content.Context;

import androidx.annotation.Nullable;
import androidx.collection.ArrayMap;

import java.io.Serializable;
import java.util.List;
import java.util.Objects;
import java.util.Set;

import prototype.xd.scheduler.R;
import prototype.xd.scheduler.utilities.SArrayMap;

public class Group implements Serializable {
    
    public static final transient Group NULL_GROUP = new Group();
    
    private String groupName;
    private final ArrayMap<Long, TodoListEntry> associatedEntries = new ArrayMap<>();
    protected SArrayMap<String, String> params;
    
    public Group() {
        groupName = "";
        params = new SArrayMap<>();
    }
    
    public Group(String groupName, SArrayMap<String, String> params) {
        this.groupName = groupName;
        this.params = params;
    }
    
    // to be called only by TodoListEntry
    protected void attachEntryInternal(TodoListEntry todoListEntry) {
        if (associatedEntries.put(todoListEntry.getId(), todoListEntry) != null) {
            log(WARN, "Group", "attachEntryInternal called with " + todoListEntry.rawTextValue.get() + " but it's already attached");
        }
    }
    
    // to be called only by TodoListEntry
    protected void detachEntryInternal(TodoListEntry todoListEntry) {
        if (associatedEntries.remove(todoListEntry.getId()) == null) {
            log(WARN, "Group", "detachEntryInternal called with " + todoListEntry.rawTextValue.get() + " but it's not attached");
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
        Set<String> keys = params.keySet();
        keys.addAll(newParams.keySet());
        // remove parameters that didn't change
        keys.removeIf(key -> Objects.equals(params.get(key), newParams.get(key)));
        associatedEntries.forEach((aLong, todoListEntry) -> todoListEntry.invalidateParameters(keys));
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
