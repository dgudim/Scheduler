package prototype.xd.scheduler.utilities;

import static android.util.Log.INFO;
import static prototype.xd.scheduler.utilities.BitmapUtilities.mixTwoColors;
import static prototype.xd.scheduler.utilities.DateManager.currentDay;
import static prototype.xd.scheduler.utilities.Keys.ASSOCIATED_DAY;
import static prototype.xd.scheduler.utilities.Keys.BG_COLOR;
import static prototype.xd.scheduler.utilities.Keys.EXPIRED_ITEMS_OFFSET;
import static prototype.xd.scheduler.utilities.Keys.INDICATORS_FILE;
import static prototype.xd.scheduler.utilities.Keys.IS_COMPLETED;
import static prototype.xd.scheduler.utilities.Keys.SERVICE_UPDATE_SIGNAL;
import static prototype.xd.scheduler.utilities.Keys.UPCOMING_ITEMS_OFFSET;
import static prototype.xd.scheduler.utilities.Logger.log;
import static prototype.xd.scheduler.utilities.PreferencesStore.servicePreferences;
import static prototype.xd.scheduler.utilities.SystemCalendarUtils.getAllCalendars;
import static prototype.xd.scheduler.utilities.Utilities.loadGroups;
import static prototype.xd.scheduler.utilities.Utilities.loadObject;
import static prototype.xd.scheduler.utilities.Utilities.loadTodoEntries;
import static prototype.xd.scheduler.utilities.Utilities.saveObject;
import static prototype.xd.scheduler.utilities.Utilities.sortEntries;

import android.content.Context;
import android.content.res.ColorStateList;
import android.graphics.Color;
import android.util.ArraySet;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.lifecycle.DefaultLifecycleObserver;
import androidx.lifecycle.Lifecycle;
import androidx.lifecycle.LifecycleOwner;

import com.google.android.material.color.MaterialColors;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Set;

import prototype.xd.scheduler.R;
import prototype.xd.scheduler.adapters.TodoListViewAdapter;
import prototype.xd.scheduler.entities.Group;
import prototype.xd.scheduler.entities.GroupList;
import prototype.xd.scheduler.entities.SystemCalendar;
import prototype.xd.scheduler.entities.SystemCalendarEvent;
import prototype.xd.scheduler.entities.TodoListEntry;
import prototype.xd.scheduler.entities.TodoListEntryList;
import prototype.xd.scheduler.views.CalendarView;

public class TodoListEntryManager implements DefaultLifecycleObserver {
    
    public enum SaveType {
        ENTRIES, GROUPS, NONE
    }
    
    private static final String NAME = "TodoListEntryManager";
    
    private long loadedDay_start;
    private long loadedDay_end;
    
    private @Nullable
    CalendarView calendarView;
    private final TodoListViewAdapter todoListViewAdapter;
    
    private List<SystemCalendar> calendars;
    private final TodoListEntryList todoListEntries;
    private final GroupList groups;
    
    private SArrayMap<Long, ArrayList<Integer>> cachedIndicators;
    
    private final Thread asyncSaver;
    private final Object saveSyncObject = new Object();
    private volatile SaveType saveType = SaveType.NONE;
    
    private volatile boolean initFinished = false;
    private @Nullable
    Runnable onInitFinishedRunnable;
    
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
            
            // parameters that change calendar indicators
            if (parameters.contains(BG_COLOR) ||
                    parameters.contains(UPCOMING_ITEMS_OFFSET) ||
                    parameters.contains(EXPIRED_ITEMS_OFFSET)) {
                entry.addVisibleDays(calendarView.getFirstVisibleDay(), calendarView.getLastVisibleDay(), daysToRebind);
            }
            
            // we should save the entries but now now because sometimes we change parameters frequently
            // and we don't want to call save function 10 time when we use a slider
            shouldSaveEntries = true;
        }
    };
    
    public TodoListEntryManager(@NonNull final Context context,
                                @NonNull final Lifecycle lifecycle) {
        this.todoListViewAdapter = new TodoListViewAdapter(this, context, lifecycle);
        this.todoListEntries = new TodoListEntryList();
        this.groups = loadGroups();
        
        lifecycle.addObserver(this);
        
        // load cached indicators immediately (~10ms)
        try {
            cachedIndicators = loadObject(INDICATORS_FILE);
        } catch (IOException | ClassNotFoundException e) {
            cachedIndicators = new SArrayMap<>();
            log(INFO, NAME, "No cached indicators file");
        }
        
        // load calendars and static entries in a separate thread (~300ms)
        new Thread(() -> {
            long start = System.currentTimeMillis();
            calendars = getAllCalendars(context, false);
            todoListEntries.addAll(loadTodoEntries(context, currentDay - 30, currentDay + 30, groups, calendars, true));
            for (TodoListEntry entry : todoListEntries) {
                // attach to all entries
                entry.listenToParameterInvalidations(parameterInvalidationListener);
            }
            initFinished = true;
            // clear cached indicators, fetch current ones later
            cachedIndicators.clear();
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
                            log(INFO, "Async writer", "Async writer stopped");
                        }
                        
                    } while (!isInterrupted());
                }
            }
        };
        asyncSaver.start();
    }
    
    @Override
    public void onDestroy(@NonNull LifecycleOwner owner) {
        // save indicators
        try {
            saveObject(INDICATORS_FILE, cachedIndicators);
        } catch (IOException e) {
            e.printStackTrace();
        }
        //stop lingering thread
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
    
    // perform all deferred tasks
    public void ensureUpToDate() {
        if (shouldSaveEntries) {
            saveEntriesAsync();
            shouldSaveEntries = false;
            invalidateArrayAdapter();
        }
        notifyDaysChanged(daysToRebind);
        daysToRebind.clear();
    }
    
    private void notifyDaysChanged(Set<Long> days) {
        cachedIndicators.removeAll(days);
        if (calendarView != null) {
            calendarView.notifyDaysChanged(days);
        }
    }
    
    // tells entry list that all entries have changed
    public void invalidateArrayAdapter() {
        todoListViewAdapter.notifyVisibleEntriesUpdated();
    }
    
    // tells calendar list that all entries have changed
    public void invalidateCalendar() {
        if (calendarView != null) {
            calendarView.notifyVisibleDaysChanged();
        }
    }
    
    // tells all entries that their parameters have changed and refresh all ui stuff
    public void invalidateAll() {
        cachedIndicators.clear();
        for (TodoListEntry todoListEntry : todoListEntries) {
            todoListEntry.invalidateAllParameters(false);
        }
        invalidateCalendar();
        invalidateArrayAdapter();
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
    
    public TodoListEntryList getVisibleTodoListEntries(long day) {
        TodoListEntryList filteredTodoListEntries = new TodoListEntryList();
        // get all entries visible on a particular day
        for (TodoListEntry todoEntry : todoListEntries) {
            if (todoEntry.visibleInList(day)) {
                filteredTodoListEntries.add(todoEntry);
            }
        }
        // fancy sort
        return sortEntries(filteredTodoListEntries, day);
    }
    
    public List<ColorStateList> getEventIndicators(long day, boolean offTheCalendar, Context context) {
        
        List<Integer> colors = getIndicatorRawColors(day);
        List<ColorStateList> entryIndicators = new ArrayList<>(colors.size());
        
        for (int color : colors) {
            if (offTheCalendar) {
                color = mixTwoColors(color, MaterialColors.getColor(context, R.attr.colorSurface, Color.GRAY), 0.8);
            }
            entryIndicators.add(ColorStateList.valueOf(color));
        }
        
        return entryIndicators;
    }
    
    private List<Integer> getIndicatorRawColors(long day) {
        // try to get from cache
        ArrayList<Integer> colors = cachedIndicators.get(day);
        if (colors != null) {
            return colors;
        }
        
        TodoListEntryList filteredTodoListEntries = new TodoListEntryList();
        // get all relevant entries
        for (TodoListEntry todoEntry : todoListEntries) {
            if (!todoEntry.isGlobal() && !todoEntry.isCompleted() && todoEntry.visibleInList(day)) {
                filteredTodoListEntries.add(todoEntry);
            }
        }
        // fancy sort
        filteredTodoListEntries = sortEntries(filteredTodoListEntries, day);
        
        colors = new ArrayList<>(filteredTodoListEntries.size());
        for (TodoListEntry todoEntry : filteredTodoListEntries) {
            colors.add(todoEntry.bgColor.get(day));
        }
        cachedIndicators.put(day, colors);
        
        return colors;
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
            } else if (toLoadDayStart < loadedDay_start) {
                dayStart = toLoadDayStart;
                dayEnd = loadedDay_start - 1;
                loadedDay_start = toLoadDayStart;
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
                TodoListEntry newEntry = new TodoListEntry(event);
                newEntry.listenToParameterInvalidations(parameterInvalidationListener);
                todoListEntries.add(newEntry);
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
            notifyDaysChanged(entry.getVisibleDays(calendarView.getFirstVisibleDay(), calendarView.getLastVisibleDay()));
        }
        invalidateArrayAdapter();
        if (entry.isVisibleOnLockscreenToday()) {
            setBitmapUpdateFlag();
        }
        saveEntriesAsync();
    }
    
    public void addEntry(TodoListEntry entry) {
        entry.listenToParameterInvalidations(parameterInvalidationListener);
        todoListEntries.add(entry);
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
