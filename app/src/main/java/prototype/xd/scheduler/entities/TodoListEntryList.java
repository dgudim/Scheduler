package prototype.xd.scheduler.entities;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import java.util.Collection;

// a list specifically for storing TodoListEntries, automatically unlinks groups on remove to avoid memory leaks
public class TodoListEntryList extends BaseCleanupList<TodoListEntry> {
    
    public TodoListEntryList(int initialCapacity) {
        super(initialCapacity);
    }
    
    public TodoListEntryList() {
        super();
    }
    
    public TodoListEntryList(@NonNull Collection<? extends TodoListEntry> c) {
        super(c);
    }
    
    protected @Nullable
    TodoListEntry handleOldEntry(@Nullable TodoListEntry oldEntry) {
        if (oldEntry != null) {
            oldEntry.unlinkGroupInternal();
        }
        return oldEntry;
    }
}
