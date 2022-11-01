package prototype.xd.scheduler.utilities;

import static prototype.xd.scheduler.entities.Group.readGroupFile;
import static prototype.xd.scheduler.entities.Group.saveGroupsFile;
import static prototype.xd.scheduler.utilities.BitmapUtilities.mixTwoColors;
import static prototype.xd.scheduler.utilities.Keys.SERVICE_UPDATE_SIGNAL;
import static prototype.xd.scheduler.utilities.PreferencesStore.preferences_service;
import static prototype.xd.scheduler.utilities.SystemCalendarUtils.getAllCalendars;
import static prototype.xd.scheduler.utilities.Utilities.loadTodoEntries;
import static prototype.xd.scheduler.utilities.Utilities.sortEntries;

import android.content.Context;
import android.content.res.ColorStateList;
import android.graphics.Color;
import android.view.ViewGroup;

import com.google.android.material.color.MaterialColors;

import java.util.ArrayList;

import prototype.xd.scheduler.R;
import prototype.xd.scheduler.adapters.TodoListViewAdapter;
import prototype.xd.scheduler.entities.Group;
import prototype.xd.scheduler.entities.TodoListEntry;
import prototype.xd.scheduler.entities.calendars.SystemCalendar;

public class TodoListEntryStorage {
    
    private ArrayList<SystemCalendar> calendars;
    
    private long loadedDay_start;
    private long loadedDay_end;
    
    private final TodoListViewAdapter todoListViewAdapter;
    private final ArrayList<TodoListEntry> todoListEntries;
    
    private final ArrayList<Group> groups;
    
    public TodoListEntryStorage(final ViewGroup parent) {
        this.todoListViewAdapter = new TodoListViewAdapter(this, parent);
        this.todoListEntries = new ArrayList<>();
        this.groups = readGroupFile(parent.getContext());
        new Thread(() -> calendars = getAllCalendars(parent.getContext(), false), "Cfetch thread").start();
    }
    
    public TodoListViewAdapter getTodoListViewAdapter() {
        return todoListViewAdapter;
    }
    
    public int getCurrentlyVisibleEntriesCount() {
        return todoListViewAdapter.getCount();
    }
    
    public ArrayList<TodoListEntry> getVisibleTodoListEntries(long day) {
        ArrayList<TodoListEntry> filteredTodoListEntries = new ArrayList<>();
        // get all entries visible on a particular day
        for (TodoListEntry todoEntry: todoListEntries) {
            if (todoEntry.visibleInList(day)) {
                filteredTodoListEntries.add(todoEntry);
            }
        }
        // fancy sort
        return sortEntries(filteredTodoListEntries, day);
    }
    
    public ArrayList<ColorStateList> getEventIndicators(long day, boolean offTheCalendar, Context context) {
        ArrayList<ColorStateList> entryIndicators = new ArrayList<>();
        ArrayList<TodoListEntry> filteredTodoListEntries = new ArrayList<>();
        // get all relevant entries
        for (TodoListEntry todoEntry: todoListEntries) {
            if (!todoEntry.isGlobal() && !todoEntry.completed && todoEntry.visibleInList(day)) {
                filteredTodoListEntries.add(todoEntry);
            }
        }
        // fancy sort
        filteredTodoListEntries = sortEntries(filteredTodoListEntries, day);
        for (TodoListEntry todoEntry: filteredTodoListEntries) {
            int color = todoEntry.bgColor;
            if (offTheCalendar) {
                color = mixTwoColors(color, MaterialColors.getColor(context, R.attr.colorSurface, Color.GRAY), 0.8);
            }
            entryIndicators.add(ColorStateList.valueOf(color));
        }
        return entryIndicators;
    }
    
    public void updateTodoListAdapter(boolean updateBitmap, boolean updateCurrentCalendarIndicators) {
        if (updateBitmap) {
            preferences_service.edit().putBoolean(SERVICE_UPDATE_SIGNAL, true).apply();
        }
        todoListViewAdapter.notifyVisibleEntriesUpdated(updateCurrentCalendarIndicators);
    }
    
    public ArrayList<TodoListEntry> getTodoListEntries() {
        return todoListEntries;
    }
    
    public ArrayList<Group> getGroups() {
        return groups;
    }
    
    public void lazyLoadEntries(Context context, long toLoad_day_start, long toLoadDay_end) {
        if (loadedDay_start == 0) {
            loadedDay_start = toLoad_day_start;
            loadedDay_end = toLoadDay_end;
            todoListEntries.addAll(loadTodoEntries(context, loadedDay_start, loadedDay_end, groups));
        } else {
            long day_start = 0;
            long day_end = 0;
            if (toLoadDay_end > loadedDay_end) {
                day_start = loadedDay_end + 1;
                day_end = toLoadDay_end;
                loadedDay_end = toLoadDay_end;
            } else if (toLoad_day_start < loadedDay_start) {
                day_start = toLoad_day_start;
                day_end = loadedDay_start - 1;
                loadedDay_start = toLoad_day_start;
            }
            if (day_start != 0 && calendars != null) {
                for (int i = 0; i < calendars.size(); i++) {
                    addDistinct(calendars.get(i).getVisibleTodoListEntries(day_start, day_end));
                }
            }
        }
        updateTodoListAdapter(false, false);
    }
    
    private void addDistinct(ArrayList<TodoListEntry> entriesToAdd) {
        for (TodoListEntry entry : entriesToAdd) {
            if (!todoListEntries.contains(entry)) {
                todoListEntries.add(entry);
            }
        }
    }
    
    public void saveEntries() {
        Utilities.saveEntries(todoListEntries);
    }
    
    public void saveGroups() {
        saveGroupsFile(groups);
    }
    
    public void addEntry(TodoListEntry entry) {
        todoListEntries.add(entry);
    }
    
    public void removeEntry(TodoListEntry entry) {
        todoListEntries.remove(entry);
    }
}
