package prototype.xd.scheduler.utilities;

import static prototype.xd.scheduler.MainActivity.preferences;
import static prototype.xd.scheduler.utilities.DateManager.currentlySelectedDay;
import static prototype.xd.scheduler.utilities.Keys.SERVICE_UPDATE_SIGNAL;
import static prototype.xd.scheduler.utilities.SystemCalendarUtils.getAllCalendars;
import static prototype.xd.scheduler.utilities.Utilities.loadTodoEntries;

import android.content.Context;
import android.view.ViewGroup;

import java.util.ArrayList;

import prototype.xd.scheduler.adapters.TodoListViewAdapter;
import prototype.xd.scheduler.entities.TodoListEntry;
import prototype.xd.scheduler.entities.calendars.SystemCalendar;

public class TodoListEntryStorage {
    
    private ArrayList<SystemCalendar> calendars;
    
    private long loadedDay_start;
    private long loadedDay_end;
    
    private final TodoListViewAdapter todoListViewAdapter;
    private ArrayList<TodoListEntry> todoListEntries;
    
    public TodoListEntryStorage(final ViewGroup parent) {
        this.todoListViewAdapter = new TodoListViewAdapter(this, parent);
        this.todoListEntries = new ArrayList<>();
        new Thread(() -> calendars = getAllCalendars(parent.getContext(), false)).start();
    }
    
    public TodoListViewAdapter getTodoListViewAdapter() {
        return todoListViewAdapter;
    }
    
    public void updateTodoListAdapter(boolean updateBitmap) {
        if (updateBitmap) {
            preferences.edit().putBoolean(SERVICE_UPDATE_SIGNAL, true).apply();
        }
        todoListEntries = Utilities.sortEntries(todoListEntries, currentlySelectedDay);
        todoListViewAdapter.updateCurrentEntries();
        todoListViewAdapter.notifyDataSetChanged();
    }
    
    public ArrayList<TodoListEntry> getTodoListEntries() {
        return todoListEntries;
    }
    
    public void lazyLoadEntries(Context context) {
        if (loadedDay_start == 0) {
            loadedDay_start = currentlySelectedDay - 14;
            loadedDay_end = currentlySelectedDay + 14;
            todoListEntries.addAll(loadTodoEntries(context, loadedDay_start, loadedDay_end));
        } else {
            long old_loadedDay_start = loadedDay_start;
            long old_loadedDay_end = loadedDay_end;
            loadedDay_start = currentlySelectedDay - 14;
            loadedDay_end = currentlySelectedDay + 14;
            long day_start = 0;
            long day_end = 0;
            if (loadedDay_end > old_loadedDay_end) {
                day_start = old_loadedDay_end + 1;
                day_end = loadedDay_end;
            } else if (loadedDay_start < old_loadedDay_start) {
                day_start = loadedDay_start;
                day_end = old_loadedDay_start - 1;
            }
            if (day_start != 0) {
                for (int i = 0; i < calendars.size(); i++) {
                    addDistinct(calendars.get(i).getVisibleTodoListEntries(day_start, day_end));
                }
            }
        }
        updateTodoListAdapter(false);
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
    
    public void addEntry(TodoListEntry entry) {
        todoListEntries.add(entry);
    }
    
    public void removeEntry(int i) {
        todoListEntries.remove(i);
    }
}
