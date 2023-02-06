package prototype.xd.scheduler.entities;

import static prototype.xd.scheduler.utilities.Utilities.loadTodoEntries;
import static prototype.xd.scheduler.utilities.Utilities.sortEntries;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import com.google.common.collect.Sets;

import java.io.IOException;
import java.io.NotSerializableException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.function.BiConsumer;
import java.util.function.BiPredicate;
import java.util.function.Consumer;

import prototype.xd.scheduler.utilities.Static;
import prototype.xd.scheduler.utilities.Logger;

// a list specifically for storing TodoEntries, automatically unlinks groups on remove to avoid memory leaks
public final class TodoEntryList extends BaseCleanupList<TodoEntry> { // NOSONAR, should only be initialized once, shouldn't be serialized
    
    public static final String NAME = TodoEntryList.class.getSimpleName();
    
    public static final int TODO_LIST_INITIAL_CAPACITY = 75;
    
    // 4 mappings for normal events
    @NonNull
    private final Map<Long, Set<TodoEntry>> entriesPerDayCore;
    @NonNull
    private final Map<TodoEntry, Set<Long>> daysPerEntryCore; // NOSONAR, TodoEntry does not override equals
    
    @NonNull
    private final Map<Long, Set<TodoEntry>> entriesPerDayUpcomingExpired;
    @NonNull
    private final Map<TodoEntry, Set<Long>> daysPerEntryUpcomingExpired; // NOSONAR
    
    // container for global entries
    @NonNull
    private final Set<TodoEntry> globalEntries;
    
    // container for non-calendar entries
    @NonNull
    private final Set<TodoEntry> regularEntries;
    
    public boolean displayUpcomingExpired;
    public long firstLoadedDay;
    public long lastLoadedDay;
    
    private void readObject(@NonNull ObjectInputStream in)
            throws IOException, ClassNotFoundException {
        throw new NotSerializableException(NAME + " should not be serialized");
    }
    
    private void writeObject(@NonNull ObjectOutputStream out) throws IOException {
        throw new NotSerializableException(NAME + " should not be serialized");
    }
    
    @SuppressWarnings("CollectionWithoutInitialCapacity")
    public TodoEntryList(long dayStart, long dayEnd,
                         @NonNull GroupList groups,
                         @NonNull List<SystemCalendar> calendars,
                         @NonNull BiConsumer<TodoEntry, Set<String>> parameterInvalidationListener) {
        super(TODO_LIST_INITIAL_CAPACITY);
        
        firstLoadedDay = dayStart;
        lastLoadedDay = dayEnd;
        
        entriesPerDayUpcomingExpired = new HashMap<>(TODO_LIST_INITIAL_CAPACITY);
        daysPerEntryUpcomingExpired = new HashMap<>(TODO_LIST_INITIAL_CAPACITY);
        entriesPerDayCore = new HashMap<>(TODO_LIST_INITIAL_CAPACITY);
        daysPerEntryCore = new HashMap<>(TODO_LIST_INITIAL_CAPACITY);
        globalEntries = new HashSet<>();
        regularEntries = new HashSet<>();
        
        addAll(loadTodoEntries(
                        firstLoadedDay,
                        lastLoadedDay,
                        groups, calendars),
                parameterInvalidationListener);
        
    }
    
    public boolean tryExtendLoadingRange(long toLoadDayStart, long toLoadDayEnd) {
        
        if (toLoadDayEnd > lastLoadedDay) {
            toLoadDayStart = lastLoadedDay + 1;
            lastLoadedDay = toLoadDayEnd;
        } else if (toLoadDayStart < firstLoadedDay) {
            toLoadDayEnd = firstLoadedDay - 1;
            firstLoadedDay = toLoadDayStart;
        } else {
            return false;
        }
        
        Logger.debug(NAME, "Actual loading range is from " + toLoadDayStart + " to " + toLoadDayEnd);
        
        for (TodoEntry entry : this) {
            linkEntryToLookupContainers(entry, toLoadDayStart, toLoadDayEnd);
        }
        
        return true;
    }
    
    // handle unlinking
    @Nullable
    @Override
    protected TodoEntry handleOldEntry(@Nullable TodoEntry oldEntry) {
        if (oldEntry != null) {
            oldEntry.stopListeningToParameterInvalidations();
            oldEntry.unlinkGroupInternal(false);
            oldEntry.unlinkFromCalendarEvent();
            oldEntry.unlinkFromContainer();
            
            if (!oldEntry.isFromSystemCalendar() && !regularEntries.remove(oldEntry)) {
                Logger.warning(NAME, "Inconsistency detected: removing an entry from " + NAME + " but it's not in the regularEntries");
            }
            
            // if the entry is global, only unlink from global entry list
            if (oldEntry.isGlobal()) {
                if (!globalEntries.remove(oldEntry)) {
                    Logger.warning(NAME, "Inconsistency detected: removing a global entry from " + NAME + " but it's not in the globalEntries");
                }
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
    
    @NonNull
    private static Set<Long> unlinkEntryFromLookupContainers(@NonNull TodoEntry entry,
                                                             @NonNull Map<TodoEntry, Set<Long>> daysPerEntry, // NOSONAR
                                                             @NonNull Map<Long, Set<TodoEntry>> entriesPerDay) {
        Set<Long> daysForEntry = daysPerEntry.remove(entry);
        if (daysForEntry == null) {
            Logger.error(NAME, "Can't remove associations for " + entry + ", entry not managed by current container");
            return Collections.emptySet();
        } else {
            for (Long day : daysForEntry) {
                Set<TodoEntry> entriesOnDay = entriesPerDay.get(day);
                if (entriesOnDay == null) {
                    Logger.error(NAME, "Can't remove associations for " + entry + " on day " + day);
                    continue;
                }
                entriesOnDay.remove(entry);
            }
            return daysForEntry;
        }
    }
    
    private void linkEntryToLookupContainers(@NonNull TodoEntry entry, long minDay, long maxDay) {
        if (entry.isGlobal()) {
            // don't link global entries
            return;
        }
        TodoEntry.FullDaySet fullDaySet = entry.getFullDaySet(minDay, maxDay);
        linkEntryToLookupContainers(entry, fullDaySet);
    }
    
    // link entry to 4 maps (core and extended)
    private void linkEntryToLookupContainers(@NonNull TodoEntry entry,
                                             @NonNull TodoEntry.FullDaySet fullDaySet) {
        // link to core days
        linkEntryToLookupContainers(entry, fullDaySet.getCoreDaySet(), daysPerEntryCore, entriesPerDayCore);
        // link to extended days
        linkEntryToLookupContainers(entry, fullDaySet.getUpcomingExpiredDaySet(), daysPerEntryUpcomingExpired, entriesPerDayUpcomingExpired);
    }
    
    // link entry to both maps
    // makes no sense to initialize with some capacity
    @SuppressWarnings("CollectionWithoutInitialCapacity")
    private static void linkEntryToLookupContainers(@NonNull TodoEntry entry,
                                                    @NonNull Set<Long> eventDays,
                                                    @NonNull Map<TodoEntry, Set<Long>> daysPerEntry,  // NOSONAR
                                                    @NonNull Map<Long, Set<TodoEntry>> entriesPerDay) {
        daysPerEntry
                .computeIfAbsent(entry, e -> new HashSet<>())
                .addAll(eventDays);
        for (Long day : eventDays) {
            entriesPerDay
                    .computeIfAbsent(day, e -> new HashSet<>())
                    .add(entry);
        }
    }
    
    // handle linking to container and assigning an invalidation listener
    private void handleNewEntry(@NonNull TodoEntry newEntry,
                                @NonNull BiConsumer<TodoEntry, Set<String>> parameterInvalidationListener) {
        // link to current container
        newEntry.linkToContainer(this);
        // setup listener
        newEntry.listenToParameterInvalidations(parameterInvalidationListener);
        
        if (!newEntry.isFromSystemCalendar() && !regularEntries.add(newEntry)) {
            Logger.warning(NAME, "Trying to add duplicate entry: " + newEntry);
        }
        
        // if the entry is global only link to global entries list
        if (newEntry.isGlobal()) {
            if (!globalEntries.add(newEntry)) {
                Logger.warning(NAME, "Trying to add duplicate global entry: " + newEntry);
            }
            return;
        }
        
        // add all the associations to entriesPerDay and daysPerEntry
        linkEntryToLookupContainers(newEntry, firstLoadedDay, lastLoadedDay);
    }
    
    // add, assign invalidation listener and handle linking to container
    public boolean add(@NonNull TodoEntry todoEntry,
                       @NonNull BiConsumer<TodoEntry, Set<String>> parameterInvalidationListener) {
        handleNewEntry(todoEntry, parameterInvalidationListener);
        return super.add(todoEntry);
    }
    
    // add, assign invalidation listener and handle linking to container
    @SuppressWarnings({"UnusedReturnValue", "BooleanMethodNameMustStartWithQuestion"})
    public boolean addAll(@NonNull Collection<? extends TodoEntry> collection,
                          @NonNull BiConsumer<TodoEntry, Set<String>> parameterInvalidationListener) {
        for (TodoEntry todoEntry : collection) {
            handleNewEntry(todoEntry, parameterInvalidationListener);
        }
        return super.addAll(collection);
    }
    
    @NonNull
    public Set<TodoEntry> getRegularEntries() {
        return regularEntries;
    }
    
    @NonNull
    public TodoEntry.EntryType getEntryType(@NonNull TodoEntry entry, long day) {
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
        for (long day_offset = 1; day_offset <= Static.SETTINGS_MAX_EXPIRED_UPCOMING_ITEMS_OFFSET; day_offset++) {
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
    @NonNull
    // we don't know how many will be left after filtering, default capacity is fine
    @SuppressWarnings("CollectionWithoutInitialCapacity")
    public List<TodoEntry> getOnDay(long day,
                                    @NonNull BiPredicate<TodoEntry, TodoEntry.EntryType> filter) {
        List<TodoEntry> filtered = new ArrayList<>();
        Set<TodoEntry> notFiltered = displayUpcomingExpired ? entriesPerDayUpcomingExpired.get(day) : entriesPerDayCore.get(day);
        Consumer<TodoEntry> consumer = entry -> {
            if (filter.test(entry, getEntryType(entry, day))) {
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
    public boolean isNotGlobalEntryVisibleOnDay(@NonNull TodoEntry entry, long targetDayLocal) {
        Set<Long> extendedDays = daysPerEntryUpcomingExpired.get(entry);
        return extendedDays != null && extendedDays.contains(targetDayLocal);
    }
    
    public void notifyEntryVisibilityChanged(@NonNull TodoEntry entry,
                                             boolean coreDaysChanged,
                                             @NonNull Set<Long> invalidatedDaySet,
                                             boolean onlyAddDifference) {
        // if the entry is currently marked as global in the list and is global now
        if (globalEntries.contains(entry) && entry.isGlobal() && !coreDaysChanged) {
            Logger.warning(NAME, "Trying to change visibility range of a global entry");
            return;
        }
        
        Logger.debug(NAME, "notifyEntryVisibilityChanged called");
        
        // entry was global and now it's normal we unlink it from global container and link to normal container
        if (globalEntries.remove(entry)) {
            Logger.debug(NAME, entry + " is now not global");
            TodoEntry.FullDaySet newDaySet = entry.getFullDaySet(firstLoadedDay, lastLoadedDay);
            linkEntryToLookupContainers(entry, newDaySet);
            invalidatedDaySet.addAll(displayUpcomingExpired ? newDaySet.getUpcomingExpiredDaySet() : newDaySet.getCoreDaySet());
            return;
        }
        
        Set<Long> prevCoreDays = unlinkEntryFromLookupContainers(entry, daysPerEntryCore, entriesPerDayCore);
        Set<Long> prevExpiredUpcomingDays = unlinkEntryFromLookupContainers(entry, daysPerEntryUpcomingExpired, entriesPerDayUpcomingExpired);
        
        // entry became global
        if (entry.isGlobal()) {
            Logger.debug(NAME, entry + " is now global");
            globalEntries.add(entry);
            invalidatedDaySet.addAll(displayUpcomingExpired ? prevExpiredUpcomingDays : prevCoreDays);
            return;
        }
        
        // all the other cases (not global entries)
        TodoEntry.FullDaySet newDaySet = entry.getFullDaySet(firstLoadedDay, lastLoadedDay);
        
        if (onlyAddDifference) {
            Logger.debug(NAME, "Added difference to invalidatedDaySet");
            invalidatedDaySet.addAll(displayUpcomingExpired ?
                    Sets.symmetricDifference(prevExpiredUpcomingDays, newDaySet.getUpcomingExpiredDaySet()) :
                    Sets.symmetricDifference(prevCoreDays, newDaySet.getCoreDaySet()));
            
        } else {
            invalidatedDaySet.addAll(displayUpcomingExpired ? prevExpiredUpcomingDays : prevCoreDays);
            invalidatedDaySet.addAll(displayUpcomingExpired ? newDaySet.getUpcomingExpiredDaySet() : newDaySet.getCoreDaySet());
            
            Logger.debug(NAME, "Added all days to invalidatedDaySet");
        }
        
        linkEntryToLookupContainers(entry, newDaySet);
    }
    
    public void updateUpcomingExpiredVisibility() {
        displayUpcomingExpired = Static.SHOW_UPCOMING_EXPIRED_IN_LIST.get();
    }
    
    // unsupported, will cause inconsistent state
    @Override
    public boolean add(TodoEntry todoEntry) {
        throw new UnsupportedOperationException("add is not supported");
    }
    
    @Override
    public void add(int index, TodoEntry todoEntry) {
        throw new UnsupportedOperationException("add is not supported");
    }
    
    @Override
    public boolean addAll(@NonNull Collection<? extends TodoEntry> collection) {
        throw new UnsupportedOperationException("addAll is not supported");
    }
    
    @Override
    public boolean addAll(int index, @NonNull Collection<? extends TodoEntry> collection) {
        throw new UnsupportedOperationException("addAll is not supported");
    }
}
