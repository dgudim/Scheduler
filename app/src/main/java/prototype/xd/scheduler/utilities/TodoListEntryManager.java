package prototype.xd.scheduler.utilities;

import static android.util.Log.INFO;
import static java.lang.Math.min;
import static prototype.xd.scheduler.utilities.DateManager.currentDay;
import static prototype.xd.scheduler.utilities.Keys.ASSOCIATED_DAY;
import static prototype.xd.scheduler.utilities.Keys.BG_COLOR;
import static prototype.xd.scheduler.utilities.Keys.EXPIRED_ITEMS_OFFSET;
import static prototype.xd.scheduler.utilities.Keys.IS_COMPLETED;
import static prototype.xd.scheduler.utilities.Keys.SERVICE_UPDATE_SIGNAL;
import static prototype.xd.scheduler.utilities.Keys.UPCOMING_ITEMS_OFFSET;
import static prototype.xd.scheduler.utilities.Logger.log;
import static prototype.xd.scheduler.utilities.PreferencesStore.preferences;
import static prototype.xd.scheduler.utilities.PreferencesStore.servicePreferences;
import static prototype.xd.scheduler.utilities.SystemCalendarUtils.getAllCalendars;
import static prototype.xd.scheduler.utilities.Utilities.loadGroups;
import static prototype.xd.scheduler.utilities.Utilities.loadTodoEntries;

import android.content.Context;
import android.util.ArraySet;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.collection.ArrayMap;
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
    
    TodoListEntry.ParameterInvalidationListener parameterInvalidationListener = new TodoListEntry.ParameterInvalidationListener() {
        @Override
        public void parametersInvalidated(TodoListEntry entry, Set<String> parameters) {
            // in case of completed status changes or associated day changes the entry may not be visible on the lockscreen now but was before
            if (parameters.contains(IS_COMPLETED) ||
                    parameters.contains(ASSOCIATED_DAY) ||
                    entry.isVisibleOnLockscreenToday()) {
                setBitmapUpdateFlag();
            }
            
            // parameters that change event range
            if (parameters.contains(UPCOMING_ITEMS_OFFSET) ||
                    parameters.contains(EXPIRED_ITEMS_OFFSET)) {
                
                todoListEntries.notifyEntryVisibilityRangeChanged(
                        entry,
                        calendarView.getFirstVisibleDay(), calendarView.getLastVisibleDay(),
                        daysToRebind);
                
                // changes indicators
            } else if (parameters.contains(BG_COLOR)) {
                entry.getVisibleDays(
                        calendarView.getFirstVisibleDay(), calendarView.getLastVisibleDay(),
                        daysToRebind,
                        displayUpcomingExpired ? RangeType.EXTENDED_EXPIRED_UPCOMING : RangeType.CORE);
            }
            
            // we should save the entries but now now because sometimes we change parameters frequently
            // and we don't want to call save function 10 time when we use a slider
            shouldSaveEntries = true;
        }
    };
    
    public TodoListEntryManager(@NonNull final Context context,
                                @NonNull final Lifecycle lifecycle) {
        todoListViewAdapter = new TodoListViewAdapter(this, context, lifecycle);
        calendarVisibilityMap = new ArrayMap<>();
        groups = loadGroups();
        
        todoListEntries = new TodoListEntryList();
        updateStaticVarsAndCalendarVisibility();
        
        lifecycle.addObserver(this);
        
        // load calendars and static entries in a separate thread (~300ms)
        new Thread(() -> {
            long start = System.currentTimeMillis();
            calendars = getAllCalendars(context, false);
            
            for (SystemCalendar calendar : calendars) {
                calendarVisibilityMap.put(calendar, calendar.isVisible());
            }
            
            loadedDay_start = currentDay - 30;
            loadedDay_end = currentDay + 30;
            
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
        displayUpcomingExpired = preferences.getBoolean(
                Keys.SHOW_UPCOMING_EXPIRED_IN_LIST,
                Keys.SETTINGS_DEFAULT_SHOW_UPCOMING_EXPIRED_IN_LIST);
        
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
            invalidateEntryList();
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
    public void invalidateEntryList() {
        todoListViewAdapter.notifyVisibleEntriesUpdated();
    }
    
    // tells calendar list that all entries have changed
    public void invalidateCalendar() {
        if (calendarView != null) {
            calendarView.notifyVisibleDaysChanged();
        }
    }
    
    // tells all entries that their parameters have changed and refreshes all ui stuff
    public void invalidateAll() {
        for (TodoListEntry todoListEntry : todoListEntries) {
            todoListEntry.invalidateAllParameters(false);
        }
        updateStaticVarsAndCalendarVisibility();
        invalidateCalendar();
        invalidateEntryList();
    }
    
    public void bindCalendarView(@NonNull CalendarView calendarView) {
        this.calendarView = calendarView;
    }
    
    public void unbindCalendarView() {
        calendarView = null;
    }
    
    public TodoListViewAdapter getTodoListViewAdapter() {
        return todoListViewAdapter;
    }
    
    public int getCurrentlyVisibleEntriesCount() {
        return todoListViewAdapter.getItemCount();
    }
    
    public List<TodoListEntry> getVisibleTodoListEntries(long day) {
        return todoListEntries.getOnDay(day, (entry, upcomingExpired) -> {
            if (upcomingExpired) {
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
        
        List<TodoListEntry> todoListEntriesOnDay = todoListEntries.getOnDay(day, (entry, upcomingExpired) ->
                !entry.isGlobal() && !entry.isCompleted());
        
        int index = 0;
        
        // show visible indicators
        for (int ind = 0; ind < min(todoListEntriesOnDay.size(), maxIndicators); ind ++) {
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
    
    public void setBitmapUpdateFlag() {
        servicePreferences.edit().putBoolean(SERVICE_UPDATE_SIGNAL, true).apply();
    }
    
    // entry added / removed
    private void entryListChanged(TodoListEntry entry) {
        if (calendarView != null) {
            notifyDaysChanged(
                    entry.getVisibleDays(calendarView.getFirstVisibleDay(), calendarView.getLastVisibleDay(),
                            displayUpcomingExpired ? RangeType.EXPIRED_UPCOMING : RangeType.CORE));
        }
        invalidateEntryList();
        if (entry.isVisibleOnLockscreenToday()) {
            setBitmapUpdateFlag();
        }
        saveEntriesAsync();
    }
    
    public void addEntry(TodoListEntry entry) {
        todoListEntries.add(entry, parameterInvalidationListener);
        entryListChanged(entry);
    }
    
    public void removeEntry(TodoListEntry entry) {
        todoListEntries.remove(entry);
        entryListChanged(entry);
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
