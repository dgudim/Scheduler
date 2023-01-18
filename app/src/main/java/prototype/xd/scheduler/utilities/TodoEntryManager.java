package prototype.xd.scheduler.utilities;

import static java.lang.Math.min;
import static prototype.xd.scheduler.utilities.DateManager.currentDayUTC;
import static prototype.xd.scheduler.utilities.Keys.BG_COLOR;
import static prototype.xd.scheduler.utilities.Keys.END_DAY_UTC;
import static prototype.xd.scheduler.utilities.Keys.EXPIRED_ITEMS_OFFSET;
import static prototype.xd.scheduler.utilities.Keys.IS_COMPLETED;
import static prototype.xd.scheduler.utilities.Keys.START_DAY_UTC;
import static prototype.xd.scheduler.utilities.Keys.UPCOMING_ITEMS_OFFSET;
import static prototype.xd.scheduler.utilities.Keys.setBitmapUpdateFlag;
import static prototype.xd.scheduler.utilities.Logger.error;
import static prototype.xd.scheduler.utilities.SystemCalendarUtils.getAllCalendars;
import static prototype.xd.scheduler.utilities.Utilities.loadGroups;
import static prototype.xd.scheduler.utilities.Utilities.loadTodoEntries;
import static prototype.xd.scheduler.views.CalendarView.DAYS_ON_ONE_PANEL;

import androidx.annotation.ColorInt;
import androidx.annotation.MainThread;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.collection.ArrayMap;
import androidx.collection.ArraySet;
import androidx.lifecycle.DefaultLifecycleObserver;
import androidx.lifecycle.LifecycleOwner;

import java.util.Collections;
import java.util.List;
import java.util.Set;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.function.BiConsumer;

import prototype.xd.scheduler.adapters.TodoListViewAdapter;
import prototype.xd.scheduler.entities.Group;
import prototype.xd.scheduler.entities.GroupList;
import prototype.xd.scheduler.entities.SystemCalendar;
import prototype.xd.scheduler.entities.TodoEntry;
import prototype.xd.scheduler.entities.TodoEntry.RangeType;
import prototype.xd.scheduler.entities.TodoEntryList;
import prototype.xd.scheduler.views.CalendarView;

public final class TodoEntryManager implements DefaultLifecycleObserver {
    
    public static final String NAME = TodoEntryManager.class.getSimpleName();
    
    private enum SaveType {
        ENTRIES, GROUPS
    }
    
    private long firstLoadedDay;
    private long lastLoadedDay;
    
    @Nullable
    private CalendarView calendarView;
    @NonNull
    private final TodoListViewAdapter todoListViewAdapter;
    
    private List<SystemCalendar> calendars;
    @NonNull
    private final TodoEntryList todoEntries;
    @NonNull
    private final GroupList groups;
    
    @NonNull
    private final Thread asyncSaver;
    @NonNull
    private final BlockingQueue<SaveType> saveQueue;
    
    private volatile boolean initFinished;
    @Nullable
    private Runnable onInitFinishedRunnable;
    
    private boolean displayUpcomingExpired;
    @NonNull
    private final ArrayMap<Long, Boolean> calendarVisibilityMap;
    private final Set<Long> daysToRebind = new ArraySet<>();
    private boolean shouldSaveEntries;
    
    /**
     * Listener that is called when parameters change on any TodoEntry
     */
    private final BiConsumer<TodoEntry, Set<String>> parameterInvalidationListener = new BiConsumer<>() { // NOSONAR, nah
        @Override
        public void accept(@NonNull TodoEntry entry, @NonNull Set<String> parameters) {
            
            Logger.debug(NAME, entry + " parameters changed: " + parameters);
            
            // entry moved to a new day
            boolean coreDaysChanged = parameters.contains(START_DAY_UTC) ||
                    parameters.contains(END_DAY_UTC);
            
            boolean extendedDaysChanged = (parameters.contains(UPCOMING_ITEMS_OFFSET.key) || parameters.contains(EXPIRED_ITEMS_OFFSET.key))
                    && displayUpcomingExpired;
            
            // parameters that change event range
            if (extendedDaysChanged || coreDaysChanged) {
                
                todoEntries.notifyEntryVisibilityChanged(
                        entry,
                        coreDaysChanged,
                        daysToRebind,
                        // include all days if BG_COLOR changed, else include just the difference
                        !parameters.contains(BG_COLOR.CURRENT.key));
                
            } else if (parameters.contains(BG_COLOR.CURRENT.key) || parameters.contains(IS_COMPLETED)) {
                // entry didn't move but BG_COLOR changed
                entry.getVisibleDaysOnCalendar(
                        calendarView, daysToRebind,
                        displayUpcomingExpired ? RangeType.EXPIRED_UPCOMING : RangeType.CORE);
            }
            
            // we should save the entries but now now because sometimes we change parameters frequently
            // and we don't want to call save function 10 time when we use a slider
            shouldSaveEntries = true;
            setBitmapUpdateFlag();
        }
    };
    
    // we don't know the size yet
    @SuppressWarnings("CollectionWithoutInitialCapacity")
    @MainThread
    public TodoEntryManager(@NonNull final ContextWrapper wrapper) {
        todoListViewAdapter = new TodoListViewAdapter(wrapper, this);
        calendarVisibilityMap = new ArrayMap<>();
        saveQueue = new LinkedBlockingQueue<>();
        groups = loadGroups();
        
        todoEntries = new TodoEntryList(Keys.TODO_LIST_INITIAL_CAPACITY);
        updateStaticVarsAndCalendarVisibility();
        
        wrapper.addLifecycleObserver(this); // NOSONAR, this is fine, just adding an observer
        
        initAsync(wrapper);
        
        asyncSaver = new Thread("Async writer") {
            @Override
            public void run() {
                do {
                    try {
                        // hangs the thread until there is an element in saveQueue
                        switch (saveQueue.take()) {
                            case GROUPS:
                                Utilities.saveGroups(groups);
                                break;
                            case ENTRIES:
                                Utilities.saveEntries(todoEntries);
                                break;
                        }
                    } catch (InterruptedException e) {
                        interrupt();
                        String threadName = Thread.currentThread().getName();
                        Logger.info(threadName, threadName + " stopped");
                    }
                } while (!isInterrupted());
            }
        };
        asyncSaver.start();
    }
    
    /**
     * Loads entries from calendar in a separate thread
     *
     * @param wrapper context / lifecycle
     */
    private void initAsync(@NonNull final ContextWrapper wrapper) {
        // load calendars and static entries in a separate thread (~300ms)
        new Thread(() -> {
            long start = System.currentTimeMillis();
            calendars = getAllCalendars(wrapper.context, false);
            
            calendarVisibilityMap.ensureCapacity(calendars.size());
            for (SystemCalendar calendar : calendars) {
                calendarVisibilityMap.put(calendar.id, calendar.isVisible());
            }
            
            // load one panel to the right and to the left
            firstLoadedDay = currentDayUTC - DAYS_ON_ONE_PANEL;
            lastLoadedDay = currentDayUTC + DAYS_ON_ONE_PANEL;
            
            todoEntries.initLoadingRange(firstLoadedDay, lastLoadedDay);
            todoEntries.addAll(loadTodoEntries(
                    firstLoadedDay,
                    lastLoadedDay,
                    groups, calendars,
                    true), parameterInvalidationListener);
            
            initFinished = true;
            
            Logger.info(Thread.currentThread().getName(), NAME + " cold start complete in " +
                    (System.currentTimeMillis() - start) + "ms, loaded " + todoEntries.size() + " entries");
            if (onInitFinishedRunnable != null) {
                onInitFinishedRunnable.run();
                // this runnable is one-shot, cleanup to avoid memory leaks
                onInitFinishedRunnable = null;
            }
        }, "CFetch thread").start();
    }
    
    @Override
    public void onDestroy(@NonNull LifecycleOwner owner) {
        Logger.info(NAME, "Cleaning up " + NAME + " (" + todoEntries.size() + " entries, " + groups.size() + " groups)");
        //stop IO thread
        asyncSaver.interrupt();
        todoEntries.clear();
        groups.clear();
        // remove all entries (unlink all groups and events)
        Logger.info(NAME, NAME + " destroyed");
    }
    
    /**
     * Runs the Runnable specified on the main thread after TodoEntryManager has finished initializing
     *
     * @param onInitFinishedRunnable Runnable to run
     */
    public void onInitFinished(@NonNull Runnable onInitFinishedRunnable) {
        if (initFinished) {
            // init thread already finished, run from ui
            onInitFinishedRunnable.run();
        } else {
            this.onInitFinishedRunnable = onInitFinishedRunnable;
        }
    }
    
    private void updateStaticVarsAndCalendarVisibility() {
        displayUpcomingExpired = Keys.SHOW_UPCOMING_EXPIRED_IN_LIST.get();
        
        todoEntries.setUpcomingExpiredVisibility(displayUpcomingExpired);
        
        if (!calendarVisibilityMap.isEmpty()) {
            for (SystemCalendar calendar : calendars) {
                boolean visibilityBefore = Boolean.TRUE.equals(calendarVisibilityMap.get(calendar.id));
                boolean visibilityNow = calendar.isVisible();
                calendarVisibilityMap.put(calendar.id, visibilityNow);
                
                if (visibilityNow && !visibilityBefore) {
                    // new calendar is visible now
                    Logger.debug(NAME, calendar + " is now visible");
                    addEventsFromCalendar(calendar, firstLoadedDay, lastLoadedDay);
                } else if (visibilityBefore && !visibilityNow) {
                    // calendar became invisible
                    Logger.debug(NAME, calendar + " is now invisible");
                    calendar.unlinkAllTodoEntries();
                }
            }
        }
    }
    
    /**
     * Perform all deferred tasks (saving entries, notifying calendar of the changes)
     */
    public void performDeferredTasks() {
        Logger.debug(NAME, "Performing deferred tasks...");
        if (shouldSaveEntries) {
            saveEntriesAsync();
            shouldSaveEntries = false;
            notifyEntryListChanged();
        }
        notifyDaysChanged(daysToRebind);
        daysToRebind.clear();
    }
    
    /**
     * Notifies the calendar that days have changes
     *
     * @param days days that changed
     */
    private void notifyDaysChanged(@NonNull Set<Long> days) {
        Logger.debug(NAME, days.size() + " days changed");
        if (calendarView != null) {
            calendarView.notifyDaysChanged(days);
        }
    }
    
    /**
     * Tells entry list that all entries have changed
     */
    public void notifyEntryListChanged() {
        Logger.debug(NAME, "NotifyEntryListChanged called");
        todoListViewAdapter.notifyEntryListChanged();
    }
    
    /**
     * Tells calendar that current month entries have changed
     */
    public void notifyCurrentMonthChanged() {
        Logger.debug(NAME, "NotifyCurrentMonthChanged called");
        if (calendarView != null) {
            calendarView.notifyCurrentMonthChanged();
        }
    }
    
    /**
     * Tells all entries that their parameters have changed and refreshes all ui stuff
     *
     * @param timezoneChanged did the system timezone change
     */
    public void notifyDatasetChanged(boolean timezoneChanged) {
        Logger.debug(NAME, "Dataset changed! (timezone changed: " + timezoneChanged + " )");
        for (TodoEntry todoEntry : todoEntries) {
            todoEntry.invalidateAllParameters(false);
            if (timezoneChanged && todoEntry.isFromSystemCalendar()) {
                todoEntry.notifyTimeZoneChanged();
                // entry moved because timezone changed
                todoEntries.notifyEntryVisibilityChanged(
                        todoEntry,
                        true,
                        daysToRebind,
                        true);
            }
        }
        updateStaticVarsAndCalendarVisibility();
        if (calendarView != null) {
            // tells calendar list that all months have changed
            calendarView.notifyCalendarChanged();
        }
        notifyEntryListChanged();
        setBitmapUpdateFlag();
    }
    
    public void attachCalendarView(@NonNull CalendarView calendarView) {
        this.calendarView = calendarView;
    }
    
    public void detachCalendarView() {
        calendarView = null;
    }
    
    @NonNull
    public TodoListViewAdapter getTodoListViewAdapter() {
        return todoListViewAdapter;
    }
    
    public int getCurrentlyVisibleEntriesCount() {
        return todoListViewAdapter.getItemCount();
    }
    
    @NonNull
    public List<TodoEntry> getVisibleTodoEntries(long day) {
        return todoEntries.getOnDay(day, (entry, entryType) -> {
            if (entryType == TodoEntry.EntryType.UPCOMING ||
                    entryType == TodoEntry.EntryType.EXPIRED) {
                return !entry.isCompleted();
            } else {
                return true;
            }
        });
    }
    
    @FunctionalInterface
    public interface EventIndicatorConsumer {
        void submit(@ColorInt int color, int index, boolean visiblePosition);
    }
    
    public void processEventIndicators(long day, int maxIndicators, @NonNull EventIndicatorConsumer eventIndicatorConsumer) {
        
        List<TodoEntry> todoEntriesOnDay = todoEntries.getOnDay(day, (entry, entryType) ->
                entryType != TodoEntry.EntryType.GLOBAL && !entry.isCompleted());
        
        int index = 0;
        
        // show visible indicators
        for (int ind = 0; ind < min(todoEntriesOnDay.size(), maxIndicators); ind++) {
            if (displayUpcomingExpired) {
                eventIndicatorConsumer.submit(todoEntriesOnDay.get(ind).bgColor.get(day), index, true);
            } else {
                // we already know that our entry lies on current day
                eventIndicatorConsumer.submit(todoEntriesOnDay.get(ind).bgColor.getToday(), index, true);
            }
            index++;
        }
        
        // hide all the rest
        for (int i = index; i < maxIndicators; i++) {
            eventIndicatorConsumer.submit(0, i, false);
        }
    }
    
    @NonNull
    public List<Group> getGroups() {
        return Collections.unmodifiableList(groups);
    }
    
    public void loadCalendarEntries(long toLoadDayStart, long toLoadDayEnd) {
        Logger.debug(NAME, "Loading entries from " + toLoadDayStart + " to " + toLoadDayEnd);
        if (initFinished) {
            long dayStart = 0;
            long dayEnd = 0;
            if (toLoadDayEnd > lastLoadedDay) {
                dayStart = lastLoadedDay + 1;
                dayEnd = toLoadDayEnd;
                lastLoadedDay = toLoadDayEnd;
                todoEntries.extendLoadingRangeEndDay(lastLoadedDay);
            } else if (toLoadDayStart < firstLoadedDay) {
                dayStart = toLoadDayStart;
                dayEnd = firstLoadedDay - 1;
                firstLoadedDay = toLoadDayStart;
                todoEntries.extendLoadingRangeStartDay(firstLoadedDay);
            }
            if (dayStart != 0) {
                for (SystemCalendar calendar : calendars) {
                    addEventsFromCalendar(calendar, dayStart, dayEnd);
                }
            }
        }
    }
    
    /**
     * Adds event from system calendar to this container if not already
     */
    private void addEventsFromCalendar(@NonNull SystemCalendar calendar, long firstDayUTC, long lastDayUTC) {
        calendar.getVisibleEvents(firstDayUTC, lastDayUTC,
                // if the event hasn't been associated with an entry, add it
                event -> !event.isAssociatedWithEntry(),
                calendarEvent -> todoEntries.add(new TodoEntry(calendarEvent), parameterInvalidationListener));
    }
    
    private void saveAllAsync() {
        saveQueue.add(SaveType.ENTRIES);
        saveQueue.add(SaveType.GROUPS);
    }
    
    private void saveEntriesAsync() {
        saveQueue.add(SaveType.ENTRIES);
    }
    
    private void saveGroupsAsync() {
        saveQueue.add(SaveType.GROUPS);
    }
    
    // entry added / removed
    private void notifyEntryRemovedAdded(@NonNull final TodoEntry entry) {
        if (calendarView != null) {
            notifyDaysChanged(
                    entry.getVisibleDaysOnCalendar(calendarView,
                            displayUpcomingExpired ? RangeType.EXPIRED_UPCOMING : RangeType.CORE));
        }
        notifyEntryListChanged();
        if (entry.isVisibleOnLockscreenToday()) {
            setBitmapUpdateFlag();
        }
        saveEntriesAsync();
    }
    
    /**
     * Add a new entry to this container
     *
     * @param entry entry to add
     */
    public void addEntry(@NonNull final TodoEntry entry) {
        todoEntries.add(entry, parameterInvalidationListener);
        notifyEntryRemovedAdded(entry);
    }
    
    /**
     * Remove an entry from this container
     *
     * @param entry entry to remove
     */
    public void removeEntry(@NonNull final TodoEntry entry) {
        todoEntries.remove(entry);
        notifyEntryRemovedAdded(entry);
    }
    
    @SuppressWarnings("BooleanMethodNameMustStartWithQuestion")
    public boolean resetEntrySettings(@NonNull TodoEntry entry) {
        if (!todoEntries.contains(entry)) {
            error(NAME, "Resetting settings of an entry not managed by current container " + entry);
        }
        boolean paramsChanged = entry.removeDisplayParams();
        boolean groupChanged = entry.changeGroup(Group.NULL_GROUP);
        if (paramsChanged || groupChanged) {
            saveEntriesAsync();
            return true;
        }
        return false;
    }
    
    /**
     * Changes a group of an entry
     *
     * @param entry    target entry
     * @param newGroup new group
     * @return true if group was changed
     */
    @SuppressWarnings("BooleanMethodNameMustStartWithQuestion")
    public boolean changeEntryGroup(@NonNull TodoEntry entry, @NonNull Group newGroup) {
        if (!groups.contains(newGroup) || !todoEntries.contains(entry)) {
            error(NAME, "Changing group of " + entry + " to " + newGroup + " but entry or group is not managed by current container");
        }
        if (entry.changeGroup(newGroup)) {
            saveEntriesAsync();
            return true;
        }
        return false;
    }
    
    /**
     * Sets new name for a group
     *
     * @param group   target group
     * @param newName new name
     */
    public void setNewGroupName(@NonNull Group group, @NonNull String newName) {
        if (!groups.contains(group)) {
            error(NAME, "Changing name of " + group + " not managed by current container");
        }
        if (group.changeName(newName)) {
            saveAllAsync();
        }
    }
    
    /**
     * Sets new parameters for a group
     *
     * @param group     target group
     * @param newParams new parameters
     * @return true if parameters were changed
     */
    @SuppressWarnings("BooleanMethodNameMustStartWithQuestion")
    public boolean setNewGroupParams(@NonNull Group group, @NonNull SArrayMap<String, String> newParams) {
        if (!groups.contains(group)) {
            error(NAME, "Settings new parameters of " + group + " not managed by current container");
        }
        if (group.setParameters(newParams)) {
            saveGroupsAsync();
            return true;
        }
        return false;
    }
    
    /**
     * Adds a new group to be managed by this container
     *
     * @param newGroup group to add
     */
    public void addGroup(@NonNull Group newGroup) {
        groups.add(newGroup);
        saveGroupsAsync();
    }
    
    /**
     * Removes a group from the list by index, handles all entry unlinking
     *
     * @param index index of the group to remove
     */
    public void removeGroup(int index) {
        groups.remove(index);
        saveGroupsAsync();
    }
}
