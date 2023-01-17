package prototype.xd.scheduler.entities;


import androidx.annotation.Nullable;

/**
 * A list specifically for storing TodoEntry groups, automatically unlinks entries on remove to avoid memory leaks
 */
public class GroupList extends BaseCleanupList<Group> { // NOSONAR, shouldn't be serialized
    
    @Nullable
    @Override
    protected Group handleOldEntry(@Nullable Group oldGroup) {
        if (oldGroup != null) {
            // detach all entries from the group
            oldGroup.detachAllEntries();
        }
        return oldGroup;
    }
}
