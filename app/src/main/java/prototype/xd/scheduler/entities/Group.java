package prototype.xd.scheduler.entities;

import android.content.Context;
import android.util.ArraySet;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.Serializable;
import java.util.Collections;
import java.util.List;
import java.util.Objects;
import java.util.Set;

import prototype.xd.scheduler.BuildConfig;
import prototype.xd.scheduler.R;
import prototype.xd.scheduler.utilities.Logger;
import prototype.xd.scheduler.utilities.SArrayMap;
import prototype.xd.scheduler.utilities.Utilities;
import prototype.xd.scheduler.utilities.misc.ContextWrapper;

/**
 * Class for storing a bunch of parameters
 */
public class Group implements Serializable {
    
    private static final long serialVersionUID = -5159688717810769428L;
    
    public static final String NAME = Group.class.getSimpleName();
    
    public static final Group NULL_GROUP = new Group();
    
    private String groupName;
    private transient Set<TodoEntry> associatedEntries;
    @NonNull
    protected SArrayMap<String, String> params;
    
    public Group() {
        groupName = "";
        params = new SArrayMap<>();
        associatedEntries = Collections.emptySet();
    }
    
    public Group(@NonNull String groupName, @NonNull SArrayMap<String, String> params) {
        this.groupName = groupName;
        this.params = params;
        associatedEntries = new ArraySet<>();
    }
    
    private void readObject(@NonNull ObjectInputStream in)
            throws IOException, ClassNotFoundException {
        in.defaultReadObject();
        if (isNull()) {
            associatedEntries = Collections.emptySet();
        } else {
            associatedEntries = new ArraySet<>();
        }
    }
    
    // to be called only by TodoEntry
    protected void attachEntryInternal(@NonNull TodoEntry todoEntry) {
        if (isNull()) {
            // don't attach to empty group
            return;
        }
        if (!associatedEntries.add(todoEntry)) {
            Logger.warning(NAME, "attachEntryInternal called with " + todoEntry + " but it's already attached");
        }
    }
    
    // to be called only by TodoEntry
    protected void detachEntryInternal(@NonNull TodoEntry todoEntry) {
        if (isNull()) {
            // don't detach from empty group
            return;
        }
        if (!associatedEntries.remove(todoEntry)) {
            Logger.warning(NAME, "detachEntryInternal called with " + todoEntry + " but it's not attached");
        }
    }
    
    public void detachAllEntries() {
        if (isNull()) {
            // don't detach from empty group
            return;
        }
        associatedEntries.forEach(todoEntry -> todoEntry.unlinkGroupInternal(true));
        associatedEntries.clear();
    }
    
    public boolean isNull() {
        return groupName.isEmpty();
    }
    
    public static int groupIndexInList(@NonNull List<Group> groups, @NonNull String groupName) {
        for (int i = 0; i < groups.size(); i++) {
            if (Objects.equals(groups.get(i).groupName, groupName)) {
                return i;
            }
        }
        return -1;
    }
    
    @NonNull
    public static Group findGroupInList(@NonNull List<Group> groups, @NonNull String groupName) {
        if (groupName.isEmpty()) {
            return NULL_GROUP;
        }
        int index = groupIndexInList(groups, groupName);
        return index == -1 ? NULL_GROUP : groups.get(index);
    }
    
    @NonNull
    public static String[] groupListToNames(@NonNull List<Group> groups, @NonNull ContextWrapper wrapper) {
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
        return isNull() ? context.getString(R.string.blank_group_name) : groupName;
    }
    
    @NonNull
    public String getRawName() {
        return groupName;
    }
    
    @NonNull
    public Set<String> getParameterKeys() {
        if (isNull()) {
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
    @SuppressWarnings("BooleanMethodNameMustStartWithQuestion")
    public boolean changeName(@NonNull String newName) {
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
    @SuppressWarnings("BooleanMethodNameMustStartWithQuestion")
    public boolean setParameters(@NonNull SArrayMap<String, String> newParams) {
        if (groupName.isEmpty()) {
            Logger.warning(NAME, "Trying to set parameters of NULL_GROUP");
            return false;
        }
        Set<String> changedKeys = Utilities.getChangedKeys(params, newParams);
        // notify all connected entries of the change
        associatedEntries.forEach(todoEntry -> todoEntry.invalidateParameters(changedKeys));
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
        } else if (obj instanceof Group group) {
            return params.equals(group.params) && groupName.equals(group.groupName);
        }
        return false;
    }
}
