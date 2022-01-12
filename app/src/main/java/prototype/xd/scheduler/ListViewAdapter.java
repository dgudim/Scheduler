package prototype.xd.scheduler;

import static prototype.xd.scheduler.utilities.DateManager.currentDate;
import static prototype.xd.scheduler.utilities.DateManager.currentlySelectedDate;
import static prototype.xd.scheduler.utilities.DateManager.yesterdayDate;
import static prototype.xd.scheduler.utilities.Utilities.saveEntries;
import static prototype.xd.scheduler.utilities.Utilities.sortEntries;

import android.app.AlertDialog;
import android.content.Context;
import android.text.InputType;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.BaseAdapter;
import android.widget.CheckBox;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.TextView;

import java.util.ArrayList;

import prototype.xd.scheduler.entities.TodoListEntry;
import prototype.xd.scheduler.utilities.EntrySettings;
import prototype.xd.scheduler.utilities.LockScreenBitmapDrawer;

public class ListViewAdapter extends BaseAdapter {
    
    private final Context context;
    
    private final LayoutInflater inflater;
    private final HomeFragment home;
    private final LockScreenBitmapDrawer lockScreenBitmapDrawer;
    
    public ArrayList<TodoListEntry> currentTodoListEntries;
    public ArrayList<Integer> currentTodoListEntries_indexMap;
    
    public ListViewAdapter(HomeFragment fragment, LockScreenBitmapDrawer lockScreenBitmapDrawer) {
        this.home = fragment;
        this.context = fragment.getContext();
        this.lockScreenBitmapDrawer = lockScreenBitmapDrawer;
        inflater = LayoutInflater.from(context);
        currentTodoListEntries = new ArrayList<>();
        currentTodoListEntries_indexMap = new ArrayList<>();
        updateCurrentEntries();
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
        for (int i = 0; i < home.todoListEntries.size(); i++) {
            TodoListEntry currentEntry = home.todoListEntries.get(i);
            boolean show = currentEntry.showInList_ifCompleted || (!currentEntry.completed && currentEntry.showInList);
            if (currentEntry.isYesterdayEntry || !currentEntry.isGlobalEntry) {
                show = show && (currentEntry.associatedDate.equals(currentlySelectedDate) || currentEntry.associatedDate.equals(yesterdayDate));
                if (currentEntry.isYesterdayEntry) {
                    show = show && currentlySelectedDate.equals(currentDate) || currentlySelectedDate.equals(yesterdayDate);
                }
            }
            if (show) {
                currentTodoListEntries.add(currentEntry);
                currentTodoListEntries_indexMap.add(i);
            }
        }
    }
    
    public void updateData(boolean reconstructBitmap) {
        if (reconstructBitmap) {
            lockScreenBitmapDrawer.constructBitmap();
        }
        home.todoListEntries = sortEntries(home.todoListEntries);
        updateCurrentEntries();
        home.rootActivity.runOnUiThread(this::notifyDataSetChanged);
    }
    
    @Override
    public View getView(final int i, View view, ViewGroup parent) {
        
        if (view == null) {
            final TodoListEntry currentEntry = currentTodoListEntries.get(i);
            
            view = inflater.inflate(R.layout.list_selection, parent);
            
            view.findViewById(R.id.backgroudSecondLayer).setBackgroundColor(currentEntry.bgColor_list);
            
            final CheckBox isDone = view.findViewById(R.id.isDone);
            
            final TextView todoText = view.findViewById(R.id.todoText);
            
            if (currentEntry.completed) {
                todoText.setTextColor(currentEntry.fontColor_list_completed);
            } else {
                todoText.setTextColor(currentEntry.fontColor_list);
            }
            
            todoText.setText(currentEntry.textValue);
            ImageView delete = view.findViewById(R.id.deletionButton);
            ImageView settings = view.findViewById(R.id.settings);
            
            delete.setOnClickListener(view1 -> {
                
                AlertDialog.Builder alert = new AlertDialog.Builder(context);
                alert.setTitle("Удалить");
                alert.setMessage("Вы уверены?");
                alert.setPositiveButton("Да", (dialog, which) -> {
                    home.todoListEntries.remove(currentTodoListEntries_indexMap.get(i).intValue());
                    saveEntries(home.todoListEntries);
                    updateData(currentEntry.getLockViewState());
                });
                
                alert.setNegativeButton("Нет", (dialog, which) -> dialog.dismiss());
                
                alert.show();
            });
            isDone.setChecked(currentEntry.completed);
            isDone.setOnClickListener(view12 -> {
                if (!currentEntry.isGlobalEntry) {
                    currentEntry.changeParameter(TodoListEntry.IS_COMPLETED, String.valueOf(isDone.isChecked()));
                } else {
                    currentEntry.changeParameter(TodoListEntry.ASSOCIATED_DATE, currentlySelectedDate);
                }
                saveEntries(home.todoListEntries);
                updateData(true);
            });
            
            todoText.setOnLongClickListener(v -> {
                AlertDialog.Builder alert = new AlertDialog.Builder(context);
                
                final EditText input = new EditText(context);
                input.setInputType(InputType.TYPE_CLASS_TEXT);
                input.setText(currentEntry.textValue);
                alert.setView(input);
                
                alert.setTitle("Изменить");
                alert.setPositiveButton("Сохранить", (dialog, which) -> {
                    currentEntry.changeParameter(TodoListEntry.TEXT_VALUE, input.getText().toString());
                    saveEntries(home.todoListEntries);
                    updateData(currentEntry.getLockViewState());
                });
                
                if (!currentEntry.isGlobalEntry) {
                    alert.setNeutralButton("Переместить в общий список", (dialog, which) -> {
                        currentEntry.changeParameter(TodoListEntry.ASSOCIATED_DATE, "GLOBAL");
                        saveEntries(home.todoListEntries);
                        updateData(currentEntry.getLockViewState());
                    });
                }
                
                alert.setNegativeButton("Отмена", (dialog, which) -> dialog.dismiss());
                
                alert.show();
                return false;
            });
            settings.setOnClickListener(v -> new EntrySettings(inflater, currentEntry, context, home, home.todoListEntries));
        }
        return view;
    }
}
