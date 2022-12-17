package prototype.xd.scheduler.entities;

import static android.util.Log.ERROR;
import static android.util.Log.WARN;
import static prototype.xd.scheduler.utilities.Logger.log;
import static prototype.xd.scheduler.utilities.Utilities.sortEntries;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.function.Consumer;

import prototype.xd.scheduler.entities.TodoListEntry.ParameterInvalidationListener;

// a list specifically for storing TodoListEntries, automatically unlinks groups on remove to avoid memory leaks
public class TodoListEntryList extends BaseCleanupList<TodoListEntry> {
    
    private static final String NAME = "TodoListEntryList";
    
    Map<Long, Set<TodoListEntry>> entriesPerDayCore;
    Map<TodoListEntry, Set<Long>> daysPerEntryCore;
    
    Map<Long, Set<TodoListEntry>> entriesPerDayUpcomingExpired;
    Map<TodoListEntry, Set<Long>> daysPerEntryUpcomingExpired;
    
    Set<TodoListEntry> globalEntries;
    
    private boolean displayUpcomingExpired;
    private long loadedDay_start;
    private long loadedDay_end;
    
    public TodoListEntryList(int initialCapacity) {
        super(initialCapacity);
        entriesPerDayUpcomingExpired = new HashMap<>(initialCapacity);
        daysPerEntryUpcomingExpired = new HashMap<>(initialCapacity);
        entriesPerDayCore = new HashMap<>(initialCapacity);
        daysPerEntryCore = new HashMap<>(initialCapacity);
        globalEntries = new HashSet<>();
    }
    
    public void initLoadingRange(long dayStart, long dayEnd) {
        loadedDay_start = dayStart;
        loadedDay_end = dayEnd;
    }
    
    // extend to the left
    public void extendLoadingRangeStartDay(long newDayStart) {
        for (TodoListEntry entry : this) {
            // we know that newDayStart is smaller than previous loadedDay_start
            linkEntryToLookupContainers(entry, newDayStart, loadedDay_start - 1);
        }
        loadedDay_start = newDayStart;
    }
    
    // extend to the right
    public void extendLoadingRangeEndDay(long newDayEnd) {
        for (TodoListEntry entry : this) {
            // we know that newDayStart is bigger than previous loadedDay_start
            linkEntryToLookupContainers(entry, loadedDay_end + 1, newDayEnd);
        }
        loadedDay_end = newDayEnd;
    }
    
    // handle unlinking
    protected @Nullable
    @Override
    TodoListEntry handleOldEntry(@Nullable TodoListEntry oldEntry) {
        if (oldEntry != null) {
            oldEntry.stopListeningToParameterInvalidations();
            oldEntry.unlinkGroupInternal(false);
            oldEntry.unlinkFromCalendarEvent();
            oldEntry.unlinkFromContainer();
            
            // if the entry is global, only unlink from global entry list
            if(globalEntries.remove(oldEntry)) {
                return oldEntry;
            }
            
            // remove all the associations from entriesPerDay and daysPerEntry
            
            // unlink from core days
            unlinkEntryFromLookupContainers(oldEntry, daysPerEntryCore, entriesPerDayCore);
            // unlink from extended days
            unlinkEntryFromLookupContainers(oldEntry, daysPerEntryUpcomingExpired, entriesPerDayUpcomingExpired);
            return oldEntry;
        }
        return null;
    }
    
    private void unlinkEntryFromLookupContainers(TodoListEntry entry,
                                                 Map<TodoListEntry, Set<Long>> daysPerEntry,
                                                 Map<Long, Set<TodoListEntry>> entriesPerDay) {
        Set<Long> daysForEntry = daysPerEntry.remove(entry);
        if (daysForEntry == null) {
            log(ERROR, NAME, "Can't remove associations for '" + entry + "', entry not managed by current container");
        } else {
            for (Long day : daysForEntry) {
                Set<TodoListEntry> entriesOnDay = entriesPerDay.get(day);
                if (entriesOnDay == null) {
                    log(ERROR, NAME, "Can't remove associations for '" + entry + "' on day " + day);
                    continue;
                }
                entriesOnDay.remove(entry);
            }
        }
    }
    
    private void linkEntryToLookupContainers(TodoListEntry entry, long minDay, long maxDay) {
        if(entry.isGlobal()) {
            return;
        }
        TodoListEntry.FullDaySet fullDaySet = entry.getFullDaySet(minDay, maxDay);
        // link to core days
        linkEntryToLookupContainers(entry, fullDaySet.getCoreDaySet(), daysPerEntryCore, entriesPerDayCore);
        // link to extended days
        linkEntryToLookupContainers(entry, fullDaySet.getUpcomingExpiredDaySet(), daysPerEntryUpcomingExpired, entriesPerDayUpcomingExpired);
    }
    
    private void linkEntryToLookupContainers(TodoListEntry entry,
                                             Set<Long> eventDays,
                                             Map<TodoListEntry, Set<Long>> daysPerEntry,
                                             Map<Long, Set<TodoListEntry>> entriesPerDay) {
        daysPerEntry.put(entry, eventDays);
        for (Long day : eventDays) {
            entriesPerDay
                    .computeIfAbsent(day, k -> new HashSet<>())
                    .add(entry);
        }
    }
    
    // handle linking to container and assigning an invalidation listener
    protected void handleNewEntry(@Nullable TodoListEntry newEntry,
                                  ParameterInvalidationListener parameterInvalidationListener) {
        if (newEntry != null) {
            // link to current container
            newEntry.linkToContainer(this);
            // setup listener
            newEntry.listenToParameterInvalidations(parameterInvalidationListener);
            
            // if the entry is global only link to global entries list
            if(newEntry.isGlobal()) {
                globalEntries.add(newEntry);
                return;
            }
            
            // add all the associations to entriesPerDay and daysPerEntry
            linkEntryToLookupContainers(newEntry, loadedDay_start, loadedDay_end);
        }
    }
    
    // add, assign invalidation listener and handle linking to container
    public boolean add(TodoListEntry todoListEntry,
                       ParameterInvalidationListener parameterInvalidationListener) {
        handleNewEntry(todoListEntry, parameterInvalidationListener);
        return super.add(todoListEntry);
    }
    
    // add, assign invalidation listener and handle linking to container
    public boolean addAll(@NonNull Collection<? extends TodoListEntry> collection,
                          ParameterInvalidationListener parameterInvalidationListener) {
        for (TodoListEntry todoListEntry : collection) {
            handleNewEntry(todoListEntry, parameterInvalidationListener);
        }
        return super.addAll(collection);
    }
    
    @FunctionalInterface
    public
    interface TodoListEntryFilter {
        boolean filter(TodoListEntry entry, boolean upcomingExpired);
    }
    
    boolean isEntryExpiredOrUpcoming(TodoListEntry entry, long day) {
        Set<TodoListEntry> extendedEntries = entriesPerDayUpcomingExpired.get(day);
        Set<TodoListEntry> coreEntries = entriesPerDayCore.get(day);
        if (extendedEntries == null || coreEntries == null) {
            log(ERROR, NAME, "Can't determine if '" + entry + "' is expired or upcoming on day " + day + ". Day not loaded?");
            return false;
        }
        // in extended set but not in core set
        return extendedEntries.contains(entry) && !coreEntries.contains(entry);
    }
    
    // get all entries visible on a particular day
    public List<TodoListEntry> getOnDay(long day, TodoListEntryFilter filter) {
        List<TodoListEntry> filtered = new ArrayList<>();
        Set<TodoListEntry> notFiltered = displayUpcomingExpired ? entriesPerDayUpcomingExpired.get(day) : entriesPerDayCore.get(day);
        if(notFiltered == null) {
            return new ArrayList<>();
        }
        Consumer<TodoListEntry> consumer = entry -> {
            if (filter.filter(entry, isEntryExpiredOrUpcoming(entry, day))) {
                filtered.add(entry);
            }
        };
        notFiltered.forEach(consumer);
        globalEntries.forEach(consumer);
        return sortEntries(filtered, day);
    }
    
    public void notifyEntryVisibilityRangeChanged(TodoListEntry entry,
                                                  long minDay, long maxDay,
                                                  Set<Long> invalidatedDaySet) {
        if(entry.isGlobal()) {
            log(WARN, NAME, "Trying to change visibility range of a global entry");
            return;
        }
        Set<Long> upcomingExpiredDaySet = new HashSet<>();
        
        entry.getVisibleDays(minDay, maxDay,
                displayUpcomingExpired ? null : invalidatedDaySet,  // core
                upcomingExpiredDaySet,                              // basic expired / upcoming
                displayUpcomingExpired ? invalidatedDaySet : null); // extended expired / upcoming
        
        // refresh associations
        unlinkEntryFromLookupContainers(entry, daysPerEntryUpcomingExpired, entriesPerDayUpcomingExpired);
        linkEntryToLookupContainers(entry, upcomingExpiredDaySet, daysPerEntryUpcomingExpired, entriesPerDayUpcomingExpired);
    }
    
    public void setUpcomingExpiredVisibility(boolean displayUpcomingExpired) {
        this.displayUpcomingExpired = displayUpcomingExpired;
    }
    
    // unsupported, will cause inconsistent state
    @Override
    public boolean add(TodoListEntry todoListEntry) {
        throw new UnsupportedOperationException();
    }
    
    @Override
    public void add(int index, TodoListEntry todoListEntry) {
        throw new UnsupportedOperationException();
    }
    
    @Override
    public boolean addAll(@NonNull Collection<? extends TodoListEntry> collection) {
        throw new UnsupportedOperationException();
    }
    
    @Override
    public boolean addAll(int index, @NonNull Collection<? extends TodoListEntry> collection) {
        throw new UnsupportedOperationException();
    }
}
