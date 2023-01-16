package prototype.xd.scheduler.entities;

import android.content.Context;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.collection.ArrayMap;

import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.Serializable;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;

import prototype.xd.scheduler.BuildConfig;
import prototype.xd.scheduler.R;
import prototype.xd.scheduler.utilities.ContextWrapper;
import prototype.xd.scheduler.utilities.Logger;
import prototype.xd.scheduler.utilities.SArrayMap;
import prototype.xd.scheduler.utilities.Utilities;

/**
 * Class for storing a bunch of parameters
 */
public class Group implements Serializable {
    
    static final long serialVersionUID = -5159688717810769428L;
    
    public static final String NAME = Group.class.getSimpleName();
    
    public static final Group NULL_GROUP = new Group();
    
    private String groupName;
    private transient Map<Long, TodoEntry> associatedEntries;
    protected SArrayMap<String, String> params;
    
    public Group() {
        groupName = "";
        params = new SArrayMap<>();
        associatedEntries = Collections.emptyMap();
    }
    
    public Group(String groupName, SArrayMap<String, String> params) {
        this.groupName = groupName;
        this.params = params;
        associatedEntries = new ArrayMap<>();
    }
    
    private void readObject(ObjectInputStream in)
            throws IOException, ClassNotFoundException {
        in.defaultReadObject();
        if (isNullGroup()) {
            associatedEntries = Collections.emptyMap();
        } else {
            associatedEntries = new ArrayMap<>();
        }
    }
    
    // to be called only by TodoEntry
    protected void attachEntryInternal(TodoEntry todoEntry) {
        if (isNullGroup()) {
            // don't attach to empty group
            return;
        }
        if (associatedEntries.put(todoEntry.getRecyclerViewId(), todoEntry) != null) {
            Logger.warning(NAME, "attachEntryInternal called with " + todoEntry + " but it's already attached");
        }
    }
    
    // to be called only by TodoEntry
    protected void detachEntryInternal(TodoEntry todoEntry) {
        if (isNullGroup()) {
            // don't detach from empty group
            return;
        }
        if (associatedEntries.remove(todoEntry.getRecyclerViewId()) == null) {
            Logger.warning(NAME, "detachEntryInternal called with " + todoEntry + " but it's not attached");
        }
    }
    
    public void detachAllEntries() {
        if (isNullGroup()) {
            // don't detach from empty group
            return;
        }
        associatedEntries.forEach((aLong, todoEntry) -> todoEntry.unlinkGroupInternal(true));
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
    
    @NonNull
    public static Group findGroupInList(List<Group> groups, String groupName) {
        if (groupName.isEmpty()) {
            return NULL_GROUP;
        }
        int index = groupIndexInList(groups, groupName);
        return index == -1 ? NULL_GROUP : groups.get(index);
    }
    
    @NonNull
    public static String[] groupListToNames(List<Group> groups, ContextWrapper wrapper) {
        String[] names = new String[groups.size()];
        for (int i = 0; i < groups.size(); i++) {
            names[i] = groups.get(i).getLocalizedName(wrapper.context);
        }
        return names;
    }
    
    /**
     * Get localized group name
     *
     * @param context any context, will be used to get string resource
     * @return localized group name
     */
    @NonNull
    public String getLocalizedName(@NonNull Context context) {
        return isNullGroup() ? context.getString(R.string.blank_group_name) : groupName;
    }
    
    @NonNull
    public String getRawName() {
        return groupName;
    }
    
    public Set<String> getParameterKeys() {
        if (isNullGroup()) {
            return Collections.emptySet();
        } else {
            return params.keySet();
        }
    }
    
    /**
     * Set new group name
     *
     * @param newName new group name
     * @return true if name changed
     */
    public boolean setName(@NonNull String newName) {
        if (groupName.isEmpty()) {
            Logger.warning(NAME, "Trying to set name of NULL_GROUP");
            return false;
        }
        if (!groupName.equals(newName)) {
            groupName = newName;
            return true;
        }
        return false;
    }
    
    /**
     * Set new group parameters
     *
     * @param newParams new group parameters
     * @return true if parameters were changed
     */
    public boolean setParams(@NonNull SArrayMap<String, String> newParams) {
        if (groupName.isEmpty()) {
            Logger.warning(NAME, "Trying to set parameters of NULL_GROUP");
            return false;
        }
        Set<String> changedKeys = Utilities.getChangedKeys(params, newParams);
        // notify all connected entries of the change
        associatedEntries.forEach((aLong, todoEntry) -> todoEntry.invalidateParameters(changedKeys));
        params = newParams;
        return !changedKeys.isEmpty();
    }
    
    @NonNull
    @Override
    public String toString() {
        return NAME + ": " + (groupName.isEmpty() ? "NULL" : (BuildConfig.DEBUG ? groupName : groupName.hashCode()));
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
        return false;
    }
}
