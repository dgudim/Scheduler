package prototype.xd.scheduler.entities;


import androidx.annotation.Nullable;

// a list specifically for storing TodoListEntry groups, automatically unlinks entries on remove to avoid memory leaks
public class GroupList extends BaseCleanupList<Group> {
    
    @Nullable
    @Override
    protected Group handleOldEntry(@Nullable Group oldEntry) {
        if(oldEntry != null) {
            oldEntry.detachAllEntries();
        }
        return oldEntry;
    }
}
