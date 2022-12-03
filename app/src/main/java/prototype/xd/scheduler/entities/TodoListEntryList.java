package prototype.xd.scheduler.entities;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import java.util.Collection;

import prototype.xd.scheduler.entities.TodoListEntry.ParameterInvalidationListener;

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
            oldEntry.stopListeningToParameterInvalidations();
            oldEntry.unlinkGroupInternal(false);
            oldEntry.unlinkFromCalendarEvent();
            oldEntry.unlinkFromContainer();
        }
        return oldEntry;
    }
    
    protected void handleNewEntry(@Nullable TodoListEntry newEntry) {
        if (newEntry != null) {
            newEntry.linkToContainer(this);
        }
    }
    
    protected void handleNewEntry(@Nullable TodoListEntry newEntry, ParameterInvalidationListener parameterInvalidationListener) {
        if (newEntry != null) {
            newEntry.linkToContainer(this);
            newEntry.listenToParameterInvalidations(parameterInvalidationListener);
        }
    }
    
    public boolean add(TodoListEntry todoListEntry, ParameterInvalidationListener parameterInvalidationListener) {
        handleNewEntry(todoListEntry, parameterInvalidationListener);
        return super.add(todoListEntry);
    }
    
    @Override
    public boolean add(TodoListEntry todoListEntry) {
        handleNewEntry(todoListEntry);
        return super.add(todoListEntry);
    }
    
    @Override
    public void add(int index, TodoListEntry todoListEntry) {
        handleNewEntry(todoListEntry);
        super.add(index, todoListEntry);
    }
    
    @Override
    public boolean addAll(@NonNull Collection<? extends TodoListEntry> collection) {
        for (TodoListEntry todoListEntry : collection) {
            handleNewEntry(todoListEntry);
        }
        return super.addAll(collection);
    }
    
    @Override
    public boolean addAll(int index, @NonNull Collection<? extends TodoListEntry> collection) {
        for (TodoListEntry todoListEntry : collection) {
            handleNewEntry(todoListEntry);
        }
        return super.addAll(index, collection);
    }
}
