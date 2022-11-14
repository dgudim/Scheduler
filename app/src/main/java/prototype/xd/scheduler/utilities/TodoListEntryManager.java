package prototype.xd.scheduler.utilities;

import static android.util.Log.INFO;
import static prototype.xd.scheduler.utilities.BitmapUtilities.mixTwoColors;
import static prototype.xd.scheduler.utilities.DateManager.currentDay;
import static prototype.xd.scheduler.utilities.Keys.SERVICE_UPDATE_SIGNAL;
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

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.lifecycle.DefaultLifecycleObserver;
import androidx.lifecycle.Lifecycle;
import androidx.lifecycle.LifecycleOwner;

import com.google.android.material.color.MaterialColors;

import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import prototype.xd.scheduler.R;
import prototype.xd.scheduler.adapters.TodoListViewAdapter;
import prototype.xd.scheduler.entities.Group;
import prototype.xd.scheduler.entities.TodoListEntry;
import prototype.xd.scheduler.entities.calendars.SystemCalendar;
import prototype.xd.scheduler.views.CalendarView;

public class TodoListEntryManager implements DefaultLifecycleObserver {
    
    public enum SaveType {
        ENTRIES, GROUPS, BOTH, NONE
    }
    
    private static final String NAME = "TodoListEntryStorage";
    
    private long loadedDay_start;
    private long loadedDay_end;
    
    private @Nullable
    CalendarView calendarView;
    private final TodoListViewAdapter todoListViewAdapter;
    
    private List<SystemCalendar> calendars;
    private final List<TodoListEntry> todoListEntries;
    private final List<Group> groups;
    
    private Map<Long, List<Integer>> cachedIndicators;
    
    private final Thread asyncSaver;
    private final Object saveSyncObject = new Object();
    private volatile SaveType saveType = SaveType.NONE;
    
    private volatile boolean initFinished = false;
    private @Nullable
    Runnable onInitFinishedRunnable;
    
    public TodoListEntryManager(final Context context, final Lifecycle lifecycle) {
        this.todoListViewAdapter = new TodoListViewAdapter(this, context);
        this.todoListEntries = new ArrayList<>();
        this.groups = loadGroups(context);
        
        lifecycle.addObserver(this);
        
        // load cached indicators immediately (~10ms)
        try {
            cachedIndicators = loadObject("cached_indicators");
        } catch (IOException | ClassNotFoundException e) {
            cachedIndicators = new HashMap<>();
            log(INFO, NAME, "No cached indicators file");
        }
        
        // load calendars and static entries in a separate thread (~300ms)
        new Thread(() -> {
            long start = System.currentTimeMillis();
            calendars = getAllCalendars(context, false);
            todoListEntries.addAll(loadTodoEntries(context, currentDay - 30, currentDay + 30, groups, calendars));
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
                                case BOTH:
                                    Utilities.saveEntries(todoListEntries);
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
        //stop lingering thread
        asyncSaver.interrupt();
    }
    
    public void onInitFinished(@NonNull Runnable onInitFinishedRunnable) {
        if (initFinished) {
            // init thread already finished, run from ui
            onInitFinishedRunnable.run();
        } else {
            this.onInitFinishedRunnable = onInitFinishedRunnable;
        }
    }
    
    public void bindToCalendarView(@NonNull CalendarView calendarView) {
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
        List<TodoListEntry> filteredTodoListEntries = new ArrayList<>();
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
        //// try to get from cache
        //List<Integer> colors = cachedIndicators.get(day);
        //if (colors != null) {
        //    return colors;
        //}
        
        // 5 entries on average
        List<TodoListEntry> filteredTodoListEntries = new ArrayList<>(5);
        // get all relevant entries
        for (TodoListEntry todoEntry : todoListEntries) {
            if (!todoEntry.isGlobal() && !todoEntry.isCompleted() && todoEntry.visibleInList(day)) {
                filteredTodoListEntries.add(todoEntry);
            }
        }
        // fancy sort
        filteredTodoListEntries = sortEntries(filteredTodoListEntries, day);
        
        List<Integer> colors = new ArrayList<>(filteredTodoListEntries.size());
        for (TodoListEntry todoEntry : filteredTodoListEntries) {
            colors.add(todoEntry.bgColor);
        }
        cachedIndicators.put(day, colors);
        
        return colors;
    }
    
    // remove indicator cache on day
    public void invalidateIndicatorCache(long day) {
        cachedIndicators.remove(day);
    }
    
    public void updateTodoListAdapter(boolean updateBitmap, boolean updateCurrentDayIndicators) {
        if (updateBitmap) {
            servicePreferences.edit().putBoolean(SERVICE_UPDATE_SIGNAL, true).apply();
        }
        todoListViewAdapter.notifyVisibleEntriesUpdated();
        if (updateCurrentDayIndicators && calendarView != null) {
            calendarView.notifyCurrentDayChanged();
        }
    }
    
    public List<TodoListEntry> getTodoListEntries() {
        return todoListEntries;
    }
    
    public List<Group> getGroups() {
        return groups;
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
                    addDistinct(calendar.getVisibleTodoListEntries(dayStart, dayEnd));
                }
            }
            updateTodoListAdapter(false, false);
        }
    }
    
    private void addDistinct(List<TodoListEntry> entriesToAdd) {
        for (TodoListEntry entry : entriesToAdd) {
            if (!todoListEntries.contains(entry)) {
                todoListEntries.add(entry);
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
    
    public void saveGroupsAndEntriesAsync() {
        wakeUpAndSave(SaveType.BOTH);
    }
    
    public void saveIndicators() {
        try {
            saveObject("cached_indicators", cachedIndicators);
        } catch (IOException e) {
            e.printStackTrace();
        }
    }
    
    public void addEntry(TodoListEntry entry) {
        todoListEntries.add(entry);
    }
    
    public void removeEntry(TodoListEntry entry) {
        todoListEntries.remove(entry);
    }
}
