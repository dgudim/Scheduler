package prototype.xd.scheduler.utilities;

import static prototype.xd.scheduler.entities.Group.readGroupFile;
import static prototype.xd.scheduler.entities.Group.saveGroupsFile;
import static prototype.xd.scheduler.utilities.BitmapUtilities.mixTwoColors;
import static prototype.xd.scheduler.utilities.Keys.SERVICE_UPDATE_SIGNAL;
import static prototype.xd.scheduler.utilities.PreferencesStore.servicePreferences;
import static prototype.xd.scheduler.utilities.SystemCalendarUtils.getAllCalendars;
import static prototype.xd.scheduler.utilities.Utilities.loadTodoEntries;
import static prototype.xd.scheduler.utilities.Utilities.sortEntries;

import android.content.Context;
import android.content.res.ColorStateList;
import android.graphics.Color;
import android.view.ViewGroup;

import com.google.android.material.color.MaterialColors;

import java.util.ArrayList;
import java.util.List;

import prototype.xd.scheduler.R;
import prototype.xd.scheduler.adapters.TodoListViewAdapter;
import prototype.xd.scheduler.entities.Group;
import prototype.xd.scheduler.entities.TodoListEntry;
import prototype.xd.scheduler.entities.calendars.SystemCalendar;

public class TodoListEntryStorage {
    
    private List<SystemCalendar> calendars;
    
    private long loadedDay_start;
    private long loadedDay_end;
    
    private final TodoListViewAdapter todoListViewAdapter;
    private final List<TodoListEntry> todoListEntries;
    
    private final List<Group> groups;
    
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
    
    public List<TodoListEntry> getVisibleTodoListEntries(long day) {
        List<TodoListEntry> filteredTodoListEntries = new ArrayList<>();
        // get all entries visible on a particular day
        for (TodoListEntry todoEntry: todoListEntries) {
            if (todoEntry.visibleInList(day)) {
                filteredTodoListEntries.add(todoEntry);
            }
        }
        // fancy sort
        return sortEntries(filteredTodoListEntries, day);
    }
    
    public List<ColorStateList> getEventIndicators(long day, boolean offTheCalendar, Context context) {
        List<ColorStateList> entryIndicators = new ArrayList<>();
        List<TodoListEntry> filteredTodoListEntries = new ArrayList<>();
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
            servicePreferences.edit().putBoolean(SERVICE_UPDATE_SIGNAL, true).apply();
        }
        todoListViewAdapter.notifyVisibleEntriesUpdated(updateCurrentCalendarIndicators);
    }
    
    public List<TodoListEntry> getTodoListEntries() {
        return todoListEntries;
    }
    
    public List<Group> getGroups() {
        return groups;
    }
    
    public void lazyLoadEntries(Context context, long toLoadDayStart, long toLoadDayEnd) {
        if (loadedDay_start == 0) {
            loadedDay_start = toLoadDayStart;
            loadedDay_end = toLoadDayEnd;
            todoListEntries.addAll(loadTodoEntries(context, loadedDay_start, loadedDay_end, groups));
        } else {
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
            if (dayStart != 0 && calendars != null) {
                for (SystemCalendar calendar: calendars) {
                    addDistinct(calendar.getVisibleTodoListEntries(dayStart, dayEnd));
                }
            }
        }
        updateTodoListAdapter(false, false);
    }
    
    private void addDistinct(List<TodoListEntry> entriesToAdd) {
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
