package prototype.xd.scheduler.entities;

import static prototype.xd.scheduler.utilities.Utilities.sortEntries;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.function.Consumer;

import prototype.xd.scheduler.entities.TodoEntry.ParameterInvalidationListener;
import prototype.xd.scheduler.utilities.Keys;
import prototype.xd.scheduler.utilities.Logger;
import prototype.xd.scheduler.utilities.Utilities;

// a list specifically for storing TodoListEntries, automatically unlinks groups on remove to avoid memory leaks
public class TodoEntryList extends BaseCleanupList<TodoEntry> {
    
    private static final String NAME = "TodoListEntryList";
    
    // 4 mappings for normal events
    final Map<Long, Set<TodoEntry>> entriesPerDayCore;
    final Map<TodoEntry, Set<Long>> daysPerEntryCore;
    
    final Map<Long, Set<TodoEntry>> entriesPerDayUpcomingExpired;
    final Map<TodoEntry, Set<Long>> daysPerEntryUpcomingExpired;
    
    // container for global entries
    final Set<TodoEntry> globalEntries;
    
    private boolean displayUpcomingExpired;
    private long firstLoadedDay;
    private long lastLoadedDay;
    
    public TodoEntryList(int initialCapacity) {
        super(initialCapacity);
        entriesPerDayUpcomingExpired = new HashMap<>(initialCapacity);
        daysPerEntryUpcomingExpired = new HashMap<>(initialCapacity);
        entriesPerDayCore = new HashMap<>(initialCapacity);
        daysPerEntryCore = new HashMap<>(initialCapacity);
        globalEntries = new HashSet<>();
    }
    
    public void initLoadingRange(long dayStart, long dayEnd) {
        firstLoadedDay = dayStart;
        lastLoadedDay = dayEnd;
    }
    
    // extend to the left
    public void extendLoadingRangeStartDay(long newDayStart) {
        for (TodoEntry entry : this) {
            // we know that newDayStart is smaller than previous loadedDay_start
            linkEntryToLookupContainers(entry, newDayStart, firstLoadedDay - 1);
        }
        firstLoadedDay = newDayStart;
    }
    
    // extend to the right
    public void extendLoadingRangeEndDay(long newDayEnd) {
        for (TodoEntry entry : this) {
            // we know that newDayStart is bigger than previous loadedDay_start
            linkEntryToLookupContainers(entry, lastLoadedDay + 1, newDayEnd);
        }
        lastLoadedDay = newDayEnd;
    }
    
    // handle unlinking
    protected @Nullable
    @Override
    TodoEntry handleOldEntry(@Nullable TodoEntry oldEntry) {
        if (oldEntry != null) {
            oldEntry.stopListeningToParameterInvalidations();
            oldEntry.unlinkGroupInternal(false);
            oldEntry.unlinkFromCalendarEvent();
            oldEntry.unlinkFromContainer();
            
            // if the entry is global, only unlink from global entry list
            if (globalEntries.remove(oldEntry)) {
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
    
    private Set<Long> unlinkEntryFromLookupContainers(TodoEntry entry,
                                                      Map<TodoEntry, Set<Long>> daysPerEntry,
                                                      Map<Long, Set<TodoEntry>> entriesPerDay) {
        Set<Long> daysForEntry = daysPerEntry.remove(entry);
        if (daysForEntry == null) {
            Logger.error(NAME, "Can't remove associations for '" + entry + "', entry not managed by current container");
            return Collections.emptySet();
        } else {
            for (Long day : daysForEntry) {
                Set<TodoEntry> entriesOnDay = entriesPerDay.get(day);
                if (entriesOnDay == null) {
                    Logger.error(NAME, "Can't remove associations for '" + entry + "' on day " + day);
                    continue;
                }
                entriesOnDay.remove(entry);
            }
            return daysForEntry;
        }
    }
    
    private void linkEntryToLookupContainers(TodoEntry entry, long minDay, long maxDay) {
        if (entry.isGlobal()) {
            // don't link global entries
            return;
        }
        TodoEntry.FullDaySet fullDaySet = entry.getFullDaySet(minDay, maxDay);
        linkEntryToLookupContainers(entry, fullDaySet);
    }
    
    // link entry to 4 maps (core and extended)
    private void linkEntryToLookupContainers(TodoEntry entry,
                                             TodoEntry.FullDaySet fullDaySet) {
        // link to core days
        linkEntryToLookupContainers(entry, fullDaySet.getCoreDaySet(), daysPerEntryCore, entriesPerDayCore);
        // link to extended days
        linkEntryToLookupContainers(entry, fullDaySet.getUpcomingExpiredDaySet(), daysPerEntryUpcomingExpired, entriesPerDayUpcomingExpired);
    }
    
    // link entry to both maps
    private void linkEntryToLookupContainers(TodoEntry entry,
                                             Set<Long> eventDays,
                                             Map<TodoEntry, Set<Long>> daysPerEntry,
                                             Map<Long, Set<TodoEntry>> entriesPerDay) {
        daysPerEntry
                .computeIfAbsent(entry, k -> new HashSet<>())
                .addAll(eventDays);
        for (Long day : eventDays) {
            entriesPerDay
                    .computeIfAbsent(day, k -> new HashSet<>())
                    .add(entry);
        }
    }
    
    // handle linking to container and assigning an invalidation listener
    protected void handleNewEntry(@Nullable TodoEntry newEntry,
                                  ParameterInvalidationListener parameterInvalidationListener) {
        if (newEntry != null) {
            // link to current container
            newEntry.linkToContainer(this);
            // setup listener
            newEntry.listenToParameterInvalidations(parameterInvalidationListener);
            
            // if the entry is global only link to global entries list
            if (newEntry.isGlobal()) {
                globalEntries.add(newEntry);
                return;
            }
            
            // add all the associations to entriesPerDay and daysPerEntry
            linkEntryToLookupContainers(newEntry, firstLoadedDay, lastLoadedDay);
        }
    }
    
    // add, assign invalidation listener and handle linking to container
    public boolean add(TodoEntry todoEntry,
                       ParameterInvalidationListener parameterInvalidationListener) {
        handleNewEntry(todoEntry, parameterInvalidationListener);
        return super.add(todoEntry);
    }
    
    // add, assign invalidation listener and handle linking to container
    public boolean addAll(@NonNull Collection<? extends TodoEntry> collection,
                          ParameterInvalidationListener parameterInvalidationListener) {
        for (TodoEntry todoEntry : collection) {
            handleNewEntry(todoEntry, parameterInvalidationListener);
        }
        return super.addAll(collection);
    }
    
    @FunctionalInterface
    public
    interface TodoListEntryFilter {
        boolean filter(TodoEntry entry, TodoEntry.EntryType entryType);
    }
    
    public TodoEntry.EntryType getEntryType(TodoEntry entry, long day) {
        if (entry.isGlobal()) {
            return TodoEntry.EntryType.GLOBAL;
        }
        Set<Long> extendedDays = daysPerEntryUpcomingExpired.get(entry);
        Set<Long> coreDays = daysPerEntryCore.get(entry);
        if (extendedDays == null || coreDays == null) {
            Logger.error(NAME, "Can't determine if '" + entry + "' is expired or upcoming on day " + day + ". Entry not managed by current container");
            return TodoEntry.EntryType.UNKNOWN;
        }
        
        if (coreDays.contains(day)) {
            return TodoEntry.EntryType.TODAY;
        }
        
        if (!extendedDays.contains(day)) {
            // entry is 100% not expired or upcoming
            Logger.debug(NAME, entry + " is not visible on " + day);
            return TodoEntry.EntryType.UNKNOWN;
        }
        
        // our event is not today's, we need to check SETTINGS_MAX_EXPIRED_UPCOMING_ITEMS_OFFSET to the left and to the right
        for (long day_offset = 1; day_offset < Keys.SETTINGS_MAX_EXPIRED_UPCOMING_ITEMS_OFFSET; day_offset++) {
            // + + + event + + +
            //       event     we are here
            if (coreDays.contains(day - day_offset)) {
                return TodoEntry.EntryType.EXPIRED;
                //           + + + event + + +
                // we are here     event
            } else if (coreDays.contains(day + day_offset)) {
                return TodoEntry.EntryType.UPCOMING;
            }
        }
        Logger.error(NAME, "Can't determine if '" + entry + "' is expired or upcoming on day " + day + ". Cause: unknown");
        return TodoEntry.EntryType.UNKNOWN;
    }
    
    // get all entries visible on a particular day
    public List<TodoEntry> getOnDay(long day, TodoListEntryFilter filter) {
        List<TodoEntry> filtered = new ArrayList<>();
        Set<TodoEntry> notFiltered = displayUpcomingExpired ? entriesPerDayUpcomingExpired.get(day) : entriesPerDayCore.get(day);
        Consumer<TodoEntry> consumer = entry -> {
            if (filter.filter(entry, getEntryType(entry, day))) {
                filtered.add(entry);
            }
        };
        if (notFiltered != null) {
            notFiltered.forEach(consumer);
        }
        globalEntries.forEach(consumer);
        return sortEntries(filtered, day);
    }
    
    // fast lookup instead of iterating the recurrence set (does not check for global entries)
    public boolean notGlobalEntryVisibleOnDay(TodoEntry entry, long targetDayLocal) {
        Set<Long> extendedDays = daysPerEntryUpcomingExpired.get(entry);
        return extendedDays != null && extendedDays.contains(targetDayLocal);
    }
    
    public void notifyEntryVisibilityChanged(TodoEntry entry,
                                             boolean coreDaysChanged,
                                             @Nullable Set<Long> invalidatedDaySet) {
        // if the entry is currently marked as global in the list and is global now
        if (globalEntries.contains(entry) && entry.isGlobal() && !coreDaysChanged) {
            Logger.warning(NAME, "Trying to change visibility range of a global entry");
            return;
        }
        
        // entry was global and now it's normal we unlink it from global container and link to normal container
        if (globalEntries.remove(entry)) {
            TodoEntry.FullDaySet newDaySet = entry.getFullDaySet(firstLoadedDay, lastLoadedDay);
            linkEntryToLookupContainers(entry, newDaySet);
            if (invalidatedDaySet != null) {
                invalidatedDaySet.addAll(displayUpcomingExpired ? newDaySet.getUpcomingExpiredDaySet() : newDaySet.getCoreDaySet());
            }
            return;
        }
        
        Set<Long> prevCoreDays = unlinkEntryFromLookupContainers(entry, daysPerEntryCore, entriesPerDayCore);
        Set<Long> prevExpiredUpcomingDays = unlinkEntryFromLookupContainers(entry, daysPerEntryUpcomingExpired, entriesPerDayUpcomingExpired);
        
        // entry became global
        if (entry.isGlobal()) {
            globalEntries.add(entry);
            if (invalidatedDaySet != null) {
                invalidatedDaySet.addAll(displayUpcomingExpired ? prevExpiredUpcomingDays : prevCoreDays);
            }
            return;
        }
        
        // all the other cases (not global entries)
        TodoEntry.FullDaySet newDaySet = entry.getFullDaySet(firstLoadedDay, lastLoadedDay);
        
        if (invalidatedDaySet != null) {
            invalidatedDaySet.addAll(displayUpcomingExpired ?
                    // update changed days
                    Utilities.symmetricDifference(
                            prevExpiredUpcomingDays, newDaySet.getUpcomingExpiredDaySet()) :
                    Utilities.symmetricDifference(
                            prevCoreDays, newDaySet.getCoreDaySet()));
        }
        
        linkEntryToLookupContainers(entry, newDaySet);
    }
    
    public void setUpcomingExpiredVisibility(boolean displayUpcomingExpired) {
        this.displayUpcomingExpired = displayUpcomingExpired;
    }
    
    // unsupported, will cause inconsistent state
    @Override
    public boolean add(TodoEntry todoEntry) {
        throw new UnsupportedOperationException();
    }
    
    @Override
    public void add(int index, TodoEntry todoEntry) {
        throw new UnsupportedOperationException();
    }
    
    @Override
    public boolean addAll(@NonNull Collection<? extends TodoEntry> collection) {
        throw new UnsupportedOperationException();
    }
    
    @Override
    public boolean addAll(int index, @NonNull Collection<? extends TodoEntry> collection) {
        throw new UnsupportedOperationException();
    }
}
