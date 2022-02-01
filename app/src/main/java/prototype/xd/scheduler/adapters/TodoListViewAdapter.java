package prototype.xd.scheduler.adapters;

import static prototype.xd.scheduler.utilities.DateManager.currentDay;
import static prototype.xd.scheduler.utilities.DateManager.currentlySelectedDay;
import static prototype.xd.scheduler.utilities.Keys.ASSOCIATED_DAY;
import static prototype.xd.scheduler.utilities.Keys.DAY_FLAG_GLOBAL_STR;
import static prototype.xd.scheduler.utilities.Keys.IS_COMPLETED;
import static prototype.xd.scheduler.utilities.Keys.TEXT_VALUE;

import android.annotation.SuppressLint;
import android.app.AlertDialog;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.BaseAdapter;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.cardview.widget.CardView;

import java.util.ArrayList;

import prototype.xd.scheduler.R;
import prototype.xd.scheduler.entities.TodoListEntry;
import prototype.xd.scheduler.utilities.TodoListEntryStorage;
import prototype.xd.scheduler.views.CheckBox;
import prototype.xd.scheduler.views.settings.EntrySettings;
import prototype.xd.scheduler.views.settings.SystemCalendarSettings;

public class TodoListViewAdapter extends BaseAdapter {
    
    private final TodoListEntryStorage todoListEntryStorage;
    
    public final ArrayList<TodoListEntry> currentTodoListEntries;
    public final ArrayList<Integer> currentTodoListEntries_indexMap;
    
    private final EntrySettings entrySettings;
    private final SystemCalendarSettings systemCalendarSettings;
    
    public TodoListViewAdapter(final TodoListEntryStorage todoListEntryStorage, final ViewGroup parent) {
        this.todoListEntryStorage = todoListEntryStorage;
        currentTodoListEntries = new ArrayList<>();
        currentTodoListEntries_indexMap = new ArrayList<>();
        entrySettings = new EntrySettings(todoListEntryStorage, LayoutInflater.from(parent.getContext()).inflate(R.layout.entry_settings, parent, false));
        systemCalendarSettings = new SystemCalendarSettings(todoListEntryStorage,
                LayoutInflater.from(parent.getContext()).inflate(R.layout.entry_settings, parent, false));
    }
    
    @Override
    public int getCount() {
        return currentTodoListEntries.size();
    }
    
    @Override
    public Object getItem(int i) {
        return currentTodoListEntries.get(i);
    }
    
    @Override
    public long getItemId(int i) {
        return i;
    }
    
    public void updateCurrentEntries() {
        currentTodoListEntries.clear();
        currentTodoListEntries_indexMap.clear();
        ArrayList<TodoListEntry> entries = todoListEntryStorage.getTodoListEntries();
        for (int i = 0; i < entries.size(); i++) {
            TodoListEntry currentEntry = entries.get(i);
            
            boolean visibilityFlag = !currentEntry.completed || currentEntry.showInList_ifCompleted;
            boolean show;
            if (currentlySelectedDay == currentDay) {
                show = currentEntry.isUpcomingEntry || currentEntry.isExpiredEntry || currentEntry.isVisible(currentlySelectedDay);
                show = show && visibilityFlag;
            } else {
                show = currentEntry.isVisible(currentlySelectedDay);
            }
            
            if (show) {
                currentTodoListEntries.add(currentEntry);
                currentTodoListEntries_indexMap.add(i);
            }
        }
    }
    
    @Override
    public int getViewTypeCount() {
        return 2;
    }
    
    @Override
    public int getItemViewType(int i) {
        return currentTodoListEntries.get(i).fromSystemCalendar ? 1 : 0;
    }
    
    @SuppressLint("SetTextI18n")
    @Override
    public View getView(final int i, View view, ViewGroup parent) {
        
        final TodoListEntry currentEntry = currentTodoListEntries.get(i);
        
        if (view == null) {
            if (currentEntry.fromSystemCalendar) {
                view = LayoutInflater.from(parent.getContext()).inflate(R.layout.list_selection_calendar, parent, false);
            } else {
                view = LayoutInflater.from(parent.getContext()).inflate(R.layout.list_selection, parent, false);
            }
        }
        
        TextView todoText = view.findViewById(R.id.todoText);
        
        ImageView settings = view.findViewById(R.id.settings);
        
        if (!currentEntry.fromSystemCalendar) {
            
            view.findViewById(R.id.deletionButton).setOnClickListener(view1 -> {
                
                AlertDialog.Builder alert = new AlertDialog.Builder(view1.getContext());
                alert.setTitle("Удалить");
                alert.setMessage("Вы уверены?");
                alert.setPositiveButton("Да", (dialog, which) -> {
                    todoListEntryStorage.removeEntry(currentTodoListEntries_indexMap.get(i));
                    todoListEntryStorage.saveEntries();
                    todoListEntryStorage.updateTodoListAdapter(currentEntry.getLockViewState());
                });
                
                alert.setNegativeButton("Нет", (dialog, which) -> dialog.dismiss());
                
                alert.show();
            });
            
            CheckBox isDone = view.findViewById(R.id.isDone);
            
            isDone.setChecked(currentEntry.completed, false);
            isDone.setOnClickListener(view12 -> {
                if (!currentEntry.isGlobalEntry) {
                    currentEntry.changeParameter(IS_COMPLETED, String.valueOf(isDone.isChecked()));
                } else {
                    currentEntry.changeParameter(ASSOCIATED_DAY, String.valueOf(currentlySelectedDay));
                }
                todoListEntryStorage.saveEntries();
                todoListEntryStorage.updateTodoListAdapter(true);
            });
            
            todoText.setOnLongClickListener(view1 -> {
                AlertDialog.Builder alertBuilder = new AlertDialog.Builder(view1.getContext());
                alertBuilder.setTitle(R.string.edit);
                View addView = LayoutInflater.from(view1.getContext()).inflate(R.layout.edit_entry_dialogue, parent, false);
                alertBuilder.setView(addView);
                AlertDialog dialog = alertBuilder.create();
                
                final EditText input = addView.findViewById(R.id.entryNameEditText);
                input.setText(currentEntry.textValue);
                
                addView.findViewById(R.id.save_button).setOnClickListener(v1 -> {
                    currentEntry.changeParameter(TEXT_VALUE, input.getText().toString());
                    todoListEntryStorage.saveEntries();
                    todoListEntryStorage.updateTodoListAdapter(currentEntry.getLockViewState());
                });
                
                View move_to_global_button = addView.findViewById(R.id.move_to_global_button);
                
                if (!currentEntry.isGlobalEntry) {
                    move_to_global_button.setOnClickListener(v1 -> {
                        currentEntry.changeParameter(ASSOCIATED_DAY, DAY_FLAG_GLOBAL_STR);
                        todoListEntryStorage.saveEntries();
                        todoListEntryStorage.updateTodoListAdapter(currentEntry.getLockViewState());
                    });
                }else{
                    move_to_global_button.setVisibility(View.GONE);
                }
    
                addView.findViewById(R.id.cancel_button).setOnClickListener(v1 -> dialog.dismiss());
                
                dialog.show();
                return false;
            });
            settings.setOnClickListener(v -> entrySettings.show(currentEntry, v.getContext()));
        } else {
            ((CardView) view.findViewById(R.id.event_color)).setCardBackgroundColor(currentEntry.event.color);
            ((TextView) view.findViewById(R.id.time_text)).setText(currentEntry.getTimeSpan(view.getContext()));
            settings.setOnClickListener(v -> systemCalendarSettings.show(currentEntry));
        }
        
        view.findViewById(R.id.backgroundSecondLayer).setBackgroundColor(currentEntry.bgColor);
        
        if (currentEntry.completed) {
            todoText.setTextColor(currentEntry.fontColor_completed);
        } else {
            todoText.setTextColor(currentEntry.fontColor);
        }
        
        todoText.setText(currentEntry.textValue + currentEntry.getDayOffset(currentlySelectedDay, view.getContext()));
        
        return view;
    }
}
