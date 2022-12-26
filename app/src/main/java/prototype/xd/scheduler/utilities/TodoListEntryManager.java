package prototype.xd.scheduler.utilities;

import static android.util.Log.INFO;
import static java.lang.Math.min;
import static prototype.xd.scheduler.utilities.DateManager.currentDayUTC;
import static prototype.xd.scheduler.utilities.Keys.BG_COLOR;
import static prototype.xd.scheduler.utilities.Keys.END_DAY_UTC;
import static prototype.xd.scheduler.utilities.Keys.EXPIRED_ITEMS_OFFSET;
import static prototype.xd.scheduler.utilities.Keys.IS_COMPLETED;
import static prototype.xd.scheduler.utilities.Keys.START_DAY_UTC;
import static prototype.xd.scheduler.utilities.Keys.UPCOMING_ITEMS_OFFSET;
import static prototype.xd.scheduler.utilities.Keys.setBitmapUpdateFlag;
import static prototype.xd.scheduler.utilities.Logger.log;
import static prototype.xd.scheduler.utilities.SystemCalendarUtils.getAllCalendars;
import static prototype.xd.scheduler.utilities.Utilities.loadGroups;
import static prototype.xd.scheduler.utilities.Utilities.loadTodoEntries;
import static prototype.xd.scheduler.views.CalendarView.DAYS_ON_ONE_PANEL;

import android.content.Context;
import android.util.ArraySet;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.collection.ArrayMap;
import androidx.fragment.app.FragmentManager;
import androidx.lifecycle.DefaultLifecycleObserver;
import androidx.lifecycle.Lifecycle;
import androidx.lifecycle.LifecycleOwner;

import java.util.Collections;
import java.util.List;
import java.util.Set;

import prototype.xd.scheduler.adapters.TodoListViewAdapter;
import prototype.xd.scheduler.entities.Group;
import prototype.xd.scheduler.entities.GroupList;
import prototype.xd.scheduler.entities.SystemCalendar;
import prototype.xd.scheduler.entities.SystemCalendarEvent;
import prototype.xd.scheduler.entities.TodoListEntry;
import prototype.xd.scheduler.entities.TodoListEntry.RangeType;
import prototype.xd.scheduler.entities.TodoListEntryList;
import prototype.xd.scheduler.views.CalendarView;

public class TodoListEntryManager implements DefaultLifecycleObserver {
    
    public enum SaveType {
        ENTRIES, GROUPS, NONE
    }
    
    private long loadedDay_start;
    private long loadedDay_end;
    
    private @Nullable
    CalendarView calendarView;
    private final TodoListViewAdapter todoListViewAdapter;
    
    private List<SystemCalendar> calendars;
    private final TodoListEntryList todoListEntries;
    private final GroupList groups;
    
    private final Thread asyncSaver;
    private final Object saveSyncObject = new Object();
    private volatile SaveType saveType = SaveType.NONE;
    
    private volatile boolean initFinished = false;
    private @Nullable
    Runnable onInitFinishedRunnable;
    
    private boolean displayUpcomingExpired;
    private final ArrayMap<SystemCalendar, Boolean> calendarVisibilityMap;
    private final Set<Long> daysToRebind = new ArraySet<>();
    private boolean shouldSaveEntries;
    
    final TodoListEntry.ParameterInvalidationListener parameterInvalidationListener = new TodoListEntry.ParameterInvalidationListener() {
        @Override
        public void parametersInvalidated(TodoListEntry entry, Set<String> parameters) {
            
            boolean coreDaysChanged = parameters.contains(START_DAY_UTC) ||
                    parameters.contains(END_DAY_UTC);
            
            // parameters that change event range
            if (parameters.contains(UPCOMING_ITEMS_OFFSET.key) ||
                    parameters.contains(EXPIRED_ITEMS_OFFSET.key) ||
                    coreDaysChanged) {
                
                todoListEntries.notifyEntryVisibilityChanged(
                        entry,
                        calendarView,
                        coreDaysChanged,
                        daysToRebind);
                
                // changes indicators
            } else if (parameters.contains(BG_COLOR.key) || parameters.contains(IS_COMPLETED)) {
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
    
    public TodoListEntryManager(@NonNull final Context context,
                                @NonNull final Lifecycle lifecycle,
                                @NonNull final FragmentManager fragmentManager) {
        todoListViewAdapter = new TodoListViewAdapter(this, context, lifecycle, fragmentManager);
        calendarVisibilityMap = new ArrayMap<>();
        groups = loadGroups();
        
        todoListEntries = new TodoListEntryList(Keys.TODO_LIST_INITIAL_CAPACITY);
        updateStaticVarsAndCalendarVisibility();
        
        lifecycle.addObserver(this);
        
        // load calendars and static entries in a separate thread (~300ms)
        new Thread(() -> {
            long start = System.currentTimeMillis();
            calendars = getAllCalendars(context, false);
            
            for (SystemCalendar calendar : calendars) {
                calendarVisibilityMap.put(calendar, calendar.isVisible());
            }
            
            // load one panel to the right and to the left
            loadedDay_start = currentDayUTC - DAYS_ON_ONE_PANEL;
            loadedDay_end = currentDayUTC + DAYS_ON_ONE_PANEL;
            
            todoListEntries.initLoadingRange(loadedDay_start, loadedDay_end);
            todoListEntries.addAll(loadTodoEntries(context,
                    loadedDay_start,
                    loadedDay_end,
                    groups, calendars,
                    true), parameterInvalidationListener);
            
            initFinished = true;
            
            log(INFO, Thread.currentThread().getName(), "TodoListEntryStorage cold start complete in " +
                    (System.currentTimeMillis() - start) + "ms, loaded " + todoListEntries.size() + " entries");
            if (onInitFinishedRunnable != null) {
                onInitFinishedRunnable.run();
                // this runnable is one-shot, cleanup to avoid memory leaks
                onInitFinishedRunnable = null;
            }
        }, "CFetch thread").start();
        
        asyncSaver = new Thread("Async writer") {
            @Override
            public void run() {
                synchronized (saveSyncObject) {
                    do {
                        try {
                            // Calling wait() will block this thread until another thread
                            // calls notify() on the object.
                            saveSyncObject.wait();
                            switch (saveType) {
                                case ENTRIES:
                                    Utilities.saveEntries(todoListEntries);
                                    break;
                                case GROUPS:
                                    Utilities.saveGroups(groups);
                                    break;
                                default:
                            }
                            saveType = SaveType.NONE;
                        } catch (InterruptedException e) {
                            interrupt();
                            log(INFO, Thread.currentThread().getName(), "Stopped");
                        }
                        
                    } while (!isInterrupted());
                }
            }
        };
        asyncSaver.start();
    }
    
    @Override
    public void onDestroy(@NonNull LifecycleOwner owner) {
        //stop IO thread
        asyncSaver.interrupt();
        // remove all entries (unlink all groups and events)
        todoListEntries.clear();
        groups.clear();
    }
    
    public void onInitFinished(@NonNull Runnable onInitFinishedRunnable) {
        if (initFinished) {
            // init thread already finished, run from ui
            onInitFinishedRunnable.run();
        } else {
            this.onInitFinishedRunnable = onInitFinishedRunnable;
        }
    }
    
    public void updateStaticVarsAndCalendarVisibility() {
        displayUpcomingExpired = Keys.SHOW_UPCOMING_EXPIRED_IN_LIST.get();
        
        todoListEntries.setUpcomingExpiredVisibility(displayUpcomingExpired);
        
        if (!calendarVisibilityMap.isEmpty()) {
            for (SystemCalendar calendar : calendars) {
                boolean visibilityBefore = Boolean.TRUE.equals(calendarVisibilityMap.get(calendar));
                boolean visibilityNow = calendar.isVisible();
                calendarVisibilityMap.put(calendar, visibilityNow);
                
                if (visibilityNow && !visibilityBefore) {
                    // new calendar is visible now
                    addEvents(calendar.getVisibleTodoListEvents(loadedDay_start, loadedDay_end));
                } else if (visibilityBefore && !visibilityNow) {
                    // calendar became invisible
                    calendar.unlinkAllTodoListEntries();
                }
            }
        }
    }
    
    // perform all deferred tasks
    public void performDeferredTasks() {
        if (shouldSaveEntries) {
            saveEntriesAsync();
            shouldSaveEntries = false;
            notifyEntryListChanged();
        }
        notifyDaysChanged(daysToRebind);
        daysToRebind.clear();
    }
    
    private void notifyDaysChanged(Set<Long> days) {
        if (calendarView != null) {
            calendarView.notifyDaysChanged(days);
        }
    }
    
    // tells entry list that all entries have changed
    public void notifyEntryListChanged() {
        todoListViewAdapter.notifyVisibleEntriesUpdated();
    }
    
    // tells calendar list that all visible entries have changed
    public void notifyVisibleDaysChanged() {
        if (calendarView != null) {
            calendarView.notifyVisibleDaysChanged();
        }
    }
    
    // tells calendar list that all visible entries have changed
    public void notifyCalendarChanged() {
        if (calendarView != null) {
            calendarView.notifyCalendarChanged();
        }
    }
    
    // tells all entries that their parameters have changed and refreshes all ui stuff
    public void notifyDatasetChanged() {
        for (TodoListEntry todoListEntry : todoListEntries) {
            todoListEntry.invalidateAllParameters(false);
        }
        updateStaticVarsAndCalendarVisibility();
        notifyCalendarChanged();
        notifyEntryListChanged();
    }
    
    public void attachCalendarView(@NonNull CalendarView calendarView) {
        this.calendarView = calendarView;
    }
    
    public void detachCalendarView() {
        calendarView = null;
    }
    
    public TodoListViewAdapter getTodoListViewAdapter() {
        return todoListViewAdapter;
    }
    
    public int getCurrentlyVisibleEntriesCount() {
        return todoListViewAdapter.getItemCount();
    }
    
    public List<TodoListEntry> getVisibleTodoListEntries(long day) {
        return todoListEntries.getOnDay(day, (entry, entryType) -> {
            if (entryType == TodoListEntry.EntryType.UPCOMING ||
                    entryType == TodoListEntry.EntryType.EXPIRED) {
                return !entry.isCompleted();
            } else {
                return true;
            }
        });
    }
    
    @FunctionalInterface
    public interface EventIndicatorConsumer {
        void submit(int color, int index, boolean visiblePosition);
    }
    
    public void processEventIndicators(long day, int maxIndicators, EventIndicatorConsumer eventIndicatorConsumer) {
        
        List<TodoListEntry> todoListEntriesOnDay = todoListEntries.getOnDay(day, (entry, entryType) ->
                entryType != TodoListEntry.EntryType.GLOBAL && !entry.isCompleted());
        
        int index = 0;
        
        // show visible indicators
        for (int ind = 0; ind < min(todoListEntriesOnDay.size(), maxIndicators); ind++) {
            if (displayUpcomingExpired) {
                eventIndicatorConsumer.submit(todoListEntriesOnDay.get(ind).bgColor.get(day), index, true);
            } else {
                // we already know that our entry lies on current day
                eventIndicatorConsumer.submit(todoListEntriesOnDay.get(ind).bgColor.getToday(), index, true);
            }
            index++;
        }
        
        // hide all the rest
        for (int i = index; i < maxIndicators; i++) {
            eventIndicatorConsumer.submit(0, i, false);
        }
    }
    
    public List<Group> getGroups() {
        return Collections.unmodifiableList(groups);
    }
    
    public void loadEntries(long toLoadDayStart, long toLoadDayEnd) {
        if (initFinished) {
            long dayStart = 0;
            long dayEnd = 0;
            if (toLoadDayEnd > loadedDay_end) {
                dayStart = loadedDay_end + 1;
                dayEnd = toLoadDayEnd;
                loadedDay_end = toLoadDayEnd;
                todoListEntries.extendLoadingRangeEndDay(loadedDay_end);
            } else if (toLoadDayStart < loadedDay_start) {
                dayStart = toLoadDayStart;
                dayEnd = loadedDay_start - 1;
                loadedDay_start = toLoadDayStart;
                todoListEntries.extendLoadingRangeStartDay(loadedDay_start);
            }
            if (dayStart != 0) {
                for (SystemCalendar calendar : calendars) {
                    addEvents(calendar.getVisibleTodoListEvents(dayStart, dayEnd));
                }
            }
        }
    }
    
    private void addEvents(List<SystemCalendarEvent> eventsToAdd) {
        for (SystemCalendarEvent event : eventsToAdd) {
            // if the event hasn't been associated with an entry, add it
            if (!event.isAssociatedWithEntry()) {
                todoListEntries.add(new TodoListEntry(event), parameterInvalidationListener);
            }
        }
    }
    
    private void wakeUpAndSave(SaveType saveType) {
        this.saveType = saveType;
        synchronized (saveSyncObject) {
            saveSyncObject.notifyAll();
        }
    }
    
    public void saveEntriesAsync() {
        wakeUpAndSave(SaveType.ENTRIES);
    }
    
    public void saveGroupsAsync() {
        wakeUpAndSave(SaveType.GROUPS);
    }
    
    // entry added / removed
    private void notifyEntryRemovedAdded(@NonNull final TodoListEntry entry) {
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
    
    public void addEntry(@NonNull final TodoListEntry entry) {
        todoListEntries.add(entry, parameterInvalidationListener);
        notifyEntryRemovedAdded(entry);
    }
    
    public void removeEntry(@NonNull final TodoListEntry entry) {
        todoListEntries.remove(entry);
        notifyEntryRemovedAdded(entry);
    }
    
    public void addGroup(Group newGroup) {
        groups.add(newGroup);
        saveGroupsAsync();
    }
    
    public void removeGroup(int index) {
        groups.remove(index);
        saveGroupsAsync();
    }
}
