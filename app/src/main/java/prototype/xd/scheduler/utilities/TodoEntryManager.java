package prototype.xd.scheduler.utilities;

import static java.lang.Math.min;
import static prototype.xd.scheduler.utilities.DateManager.currentDayUTC;
import static prototype.xd.scheduler.utilities.Static.BG_COLOR;
import static prototype.xd.scheduler.utilities.Static.END_DAY_UTC;
import static prototype.xd.scheduler.utilities.Static.EXPIRED_ITEMS_OFFSET;
import static prototype.xd.scheduler.utilities.Static.IS_COMPLETED;
import static prototype.xd.scheduler.utilities.Static.START_DAY_UTC;
import static prototype.xd.scheduler.utilities.Static.UPCOMING_ITEMS_OFFSET;
import static prototype.xd.scheduler.utilities.Static.setBitmapUpdateFlag;
import static prototype.xd.scheduler.utilities.SystemCalendarUtils.addMissingCalendars;
import static prototype.xd.scheduler.utilities.SystemCalendarUtils.loadCalendars;
import static prototype.xd.scheduler.utilities.Utilities.loadGroups;
import static prototype.xd.scheduler.views.CalendarView.DAYS_ON_ONE_PANEL;

import android.content.Context;

import androidx.annotation.ColorInt;
import androidx.annotation.MainThread;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.collection.ArrayMap;
import androidx.collection.ArraySet;
import androidx.lifecycle.DefaultLifecycleObserver;
import androidx.lifecycle.Lifecycle;
import androidx.lifecycle.LifecycleOwner;

import java.util.Collections;
import java.util.List;
import java.util.Set;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.function.BiConsumer;
import java.util.function.BiPredicate;

import prototype.xd.scheduler.entities.Group;
import prototype.xd.scheduler.entities.GroupList;
import prototype.xd.scheduler.entities.SystemCalendar;
import prototype.xd.scheduler.entities.TodoEntry;
import prototype.xd.scheduler.entities.TodoEntry.RangeType;
import prototype.xd.scheduler.entities.TodoEntryList;
import prototype.xd.scheduler.utilities.misc.DefaultedMutableLiveData;
import prototype.xd.scheduler.utilities.misc.SArrayMap;
import prototype.xd.scheduler.views.CalendarView;

public final class TodoEntryManager implements DefaultLifecycleObserver {
    
    public static final String NAME = TodoEntryManager.class.getSimpleName();
    
    private enum SaveType {
        ENTRIES, GROUPS
    }
    
    @Nullable
    private CalendarView calendarView;
    
    private List<SystemCalendar> calendars;
    @Nullable
    private TodoEntryList todoEntries;
    private GroupList groups;
    
    @NonNull
    private final BlockingQueue<SaveType> saveQueue = new LinkedBlockingQueue<>();
    
    public final DefaultedMutableLiveData<Boolean> initFinished = new DefaultedMutableLiveData<>(Boolean.FALSE);
    
    
    @SuppressWarnings("CollectionWithoutInitialCapacity")
    private final ArrayMap<Long, Boolean> calendarVisibilityMap = new ArrayMap<>();
    private final Set<Long> daysToRebind = new ArraySet<>();
    private boolean shouldSaveEntries;
    
    public final DefaultedMutableLiveData<Boolean> listChangedSignal = new DefaultedMutableLiveData<>(Boolean.FALSE);
    
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
                    && todoEntries.displayUpcomingExpired;
            
            // parameters that change event range
            if (extendedDaysChanged || coreDaysChanged) {
                
                todoEntries.notifyEntryVisibilityChanged(
                        entry,
                        coreDaysChanged,
                        daysToRebind,
                        // include all days if BG_COLOR changed, else include just the difference
                        !parameters.contains(BG_COLOR.CURRENT.key));
                
            } else if ((parameters.contains(BG_COLOR.CURRENT.key) || parameters.contains(IS_COMPLETED)) && calendarView != null) {
                // entry didn't move but BG_COLOR changed
                entry.getVisibleDaysOnCalendar(
                        calendarView, daysToRebind,
                        todoEntries.displayUpcomingExpired ? RangeType.EXPIRED_UPCOMING : RangeType.CORE);
            }
            
            // we should save the entries but now now because sometimes we change parameters frequently
            // and we don't want to call save function 10 time when we use a slider
            shouldSaveEntries = true;
            setBitmapUpdateFlag();
        }
    };
    
    @Nullable
    private static TodoEntryManager instance;
    
    @NonNull
    public static synchronized TodoEntryManager getInstance(@NonNull Context context) {
        if (instance == null) {
            instance = new TodoEntryManager(context);
        }
        return instance;
    }
    
    @MainThread
    private TodoEntryManager(@NonNull final Context context) {
        DateManager.systemTimeZone.observeForever(timeZone -> notifyTimezoneChanged());
        initAsync(context);
    }
    
    /**
     * Loads entries from calendar in a separate thread
     *
     * @param context - any context
     */
    private void initAsync(@NonNull final Context context) {
        // load calendars and static entries in a separate thread (~300ms)
        new Thread(() -> {
            long start = System.currentTimeMillis();
            
            groups = loadGroups();
            calendars = loadCalendars(context);
            
            calendarVisibilityMap.ensureCapacity(calendars.size());
            for (SystemCalendar calendar : calendars) {
                calendarVisibilityMap.put(calendar.id, calendar.isVisible());
            }
            
            // load one panel to the right and to the left
            todoEntries = new TodoEntryList(
                    currentDayUTC - DAYS_ON_ONE_PANEL,
                    currentDayUTC + DAYS_ON_ONE_PANEL,
                    groups, calendars, parameterInvalidationListener);
            
            updateStaticVarsAndCalendarVisibility();
            
            Logger.info(Thread.currentThread().getName(), NAME + " cold start complete in " +
                    (System.currentTimeMillis() - start) + "ms, loaded " + todoEntries.size() + " entries");
            
            initFinished.postValue(Boolean.TRUE);
            
        }, "CFetch thread").start();
        
        new Thread("Async writer") {
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
                        String threadName = getName();
                        Logger.info(threadName, threadName + " stopped");
                    }
                } while (!isInterrupted());
            }
        }.start();
        
    }
    
    private void updateStaticVarsAndCalendarVisibility() {
        
        if (todoEntries == null) {
            Logger.error(NAME, "updateStaticVarsAndCalendarVisibility called, but manager is still initializing");
            return;
        }
        
        todoEntries.updateUpcomingExpiredVisibility();
        
        if (!calendarVisibilityMap.isEmpty()) {
            for (SystemCalendar calendar : calendars) {
                boolean visibilityBefore = Boolean.TRUE.equals(calendarVisibilityMap.get(calendar.id));
                boolean visibilityNow = calendar.isVisible();
                calendarVisibilityMap.put(calendar.id, visibilityNow);
                
                if (visibilityNow && !visibilityBefore) {
                    // new calendar is visible now
                    Logger.debug(NAME, calendar + " is now visible");
                    addEventsFromCalendar(calendar, todoEntries.firstLoadedDay, todoEntries.lastLoadedDay);
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
        listChangedSignal.setValue(Boolean.TRUE);
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
     * tells calendar list that all months have changed
     */
    public void notifyCalendarChanged() {
        Logger.debug(NAME, "NotifyCalendarChanged called");
        if (calendarView != null) {
            calendarView.notifyCalendarChanged();
        }
    }
    
    public void notifyCalendarProviderChanged(@NonNull Context context) {
        Logger.debug(NAME, "notifyCalendarProviderChanged called");
        addMissingCalendars(context, calendars);
        // TODO: 05.02.2023 implement
    }
    
    public void notifyDatasetChanged() {
        Logger.debug(NAME, "Dataset changed!");
        notifyDatasetChanged(false);
    }
    
    private void notifyTimezoneChanged() {
        Logger.debug(NAME, "Dataset timezone changed!");
        notifyDatasetChanged(true);
    }
    
    /**
     * Tells all entries that their parameters have changed and refreshes all ui stuff
     *
     * @param timezoneChanged did the system timezone change
     */
    private void notifyDatasetChanged(boolean timezoneChanged) {
        if (todoEntries == null) {
            Logger.error(NAME, "notifyDatasetChanged called, but manager is still initializing");
            return;
        }
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
        notifyCalendarChanged();
        notifyEntryListChanged();
        setBitmapUpdateFlag();
    }
    
    public void attachCalendarView(@NonNull CalendarView calendarView, @NonNull Lifecycle lifecycle) {
        this.calendarView = calendarView;
        // observe lifecycle to clear ui references later
        lifecycle.addObserver(this);
    }
    
    @Override
    public void onDestroy(@NonNull LifecycleOwner owner) {
        // remove references to ui
        calendarView = null;
    }
    
    @NonNull
    public List<TodoEntry> getVisibleTodoEntries(long day) {
        return getVisibleTodoEntries(day, (entry, entryType) -> {
            if (entryType == TodoEntry.EntryType.UPCOMING ||
                    entryType == TodoEntry.EntryType.EXPIRED) {
                return !entry.isCompleted();
            } else {
                return true;
            }
        });
    }
    
    @NonNull
    public List<TodoEntry> getVisibleTodoEntries(long day, @NonNull BiPredicate<TodoEntry, TodoEntry.EntryType> filter) {
        if (todoEntries == null) {
            Logger.error(NAME, "getVisibleTodoEntries called, but manager is still initializing");
            return Collections.emptyList();
        }
        return todoEntries.getOnDay(day, filter);
    }
    
    @FunctionalInterface
    public interface EventIndicatorConsumer {
        void submit(@ColorInt int color, int index, boolean visiblePosition);
    }
    
    public void processEventIndicators(long day, int maxIndicators, @NonNull EventIndicatorConsumer eventIndicatorConsumer) {
        
        if (todoEntries == null) {
            Logger.error(NAME, "processEventIndicators called, but manager is still initializing");
            return;
        }
        
        List<TodoEntry> todoEntriesOnDay = todoEntries.getOnDay(day, (entry, entryType) ->
                entryType != TodoEntry.EntryType.GLOBAL && !entry.isCompleted());
        
        int index = 0;
        
        // show visible indicators
        for (int ind = 0; ind < min(todoEntriesOnDay.size(), maxIndicators); ind++) {
            if (todoEntries.displayUpcomingExpired) {
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
    
    @NonNull
    public List<SystemCalendar> getCalendars() {
        return Collections.unmodifiableList(calendars);
    }
    
    public void loadCalendarEntries(long toLoadDayStart, long toLoadDayEnd) {
        
        if (todoEntries == null) {
            Logger.error(NAME, "loadCalendarEntries called, but manager is still initializing");
            return;
        }
        
        Logger.debug(NAME, "Loading entries from " + toLoadDayStart + " to " + toLoadDayEnd);
        
        if (todoEntries.tryExtendLoadingRange(toLoadDayStart, toLoadDayEnd)) {
            for (SystemCalendar calendar : calendars) {
                addEventsFromCalendar(calendar, toLoadDayStart, toLoadDayEnd);
            }
        }
    }
    
    /**
     * Adds event from system calendar to this container if not already
     */
    private void addEventsFromCalendar(@NonNull SystemCalendar calendar, long firstDayUTC, long lastDayUTC) {
        
        if (todoEntries == null) {
            Logger.error(NAME, "addEventsFromCalendar called, but manager is still initializing");
            return;
        }
        
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
        
        if (todoEntries == null) {
            Logger.error(NAME, "notifyEntryRemovedAdded called, but manager is still initializing");
            return;
        }
        
        if (calendarView != null) {
            notifyDaysChanged(entry.getVisibleDaysOnCalendar(calendarView,
                    todoEntries.displayUpcomingExpired ? RangeType.EXPIRED_UPCOMING : RangeType.CORE));
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
        
        if (todoEntries == null) {
            Logger.error(NAME, "addEntry called, but manager is still initializing");
            return;
        }
        
        todoEntries.add(entry, parameterInvalidationListener);
        Logger.debug(NAME, "Added entry: " + entry);
        notifyEntryRemovedAdded(entry);
    }
    
    /**
     * Remove an entry from this container
     *
     * @param entry entry to remove
     */
    public void removeEntry(@NonNull final TodoEntry entry) {
        
        if (todoEntries == null) {
            Logger.error(NAME, "removeEntry called, but manager is still initializing");
            return;
        }
        
        todoEntries.remove(entry);
        Logger.debug(NAME, "Removed " + entry);
        notifyEntryRemovedAdded(entry);
    }
    
    @SuppressWarnings("BooleanMethodNameMustStartWithQuestion")
    public boolean resetEntrySettings(@NonNull TodoEntry entry) {
        
        if (todoEntries == null) {
            Logger.error(NAME, "resetEntrySettings called, but manager is still initializing");
            return false;
        }
        
        if (!todoEntries.contains(entry)) {
            Logger.error(NAME, "Resetting settings of an entry not managed by current container " + entry);
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
        
        if (todoEntries == null) {
            Logger.error(NAME, "changeEntryGroup called, but manager is still initializing");
            return false;
        }
        
        if (!groups.contains(newGroup) || !todoEntries.contains(entry)) {
            Logger.error(NAME, "Changing group of " + entry + " to " + newGroup + " but entry or group is not managed by current container");
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
            Logger.error(NAME, "Changing name of " + group + " not managed by current container");
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
            Logger.error(NAME, "Settings new parameters of " + group + " not managed by current container");
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
        Logger.debug(NAME, "Added " + newGroup);
        saveGroupsAsync();
    }
    
    /**
     * Removes a group from the list by index, handles all entry unlinking
     *
     * @param index index of the group to remove
     */
    public void removeGroup(int index) {
        Group old = groups.remove(index);
        Logger.debug(NAME, "Removed " + old);
        saveGroupsAsync();
    }
}
