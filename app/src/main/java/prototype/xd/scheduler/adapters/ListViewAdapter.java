package prototype.xd.scheduler.adapters;

import static prototype.xd.scheduler.utilities.DateManager.currentDate;
import static prototype.xd.scheduler.utilities.DateManager.currentlySelectedDate;
import static prototype.xd.scheduler.utilities.Keys.ASSOCIATED_DATE;
import static prototype.xd.scheduler.utilities.Keys.DATE_FLAG_GLOBAL;
import static prototype.xd.scheduler.utilities.Keys.IS_COMPLETED;
import static prototype.xd.scheduler.utilities.Keys.TEXT_VALUE;
import static prototype.xd.scheduler.utilities.Utilities.saveEntries;
import static prototype.xd.scheduler.utilities.Utilities.sortEntries;

import android.annotation.SuppressLint;
import android.app.AlertDialog;
import android.content.Context;
import android.text.InputType;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.BaseAdapter;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.TextView;

import java.util.ArrayList;

import prototype.xd.scheduler.HomeFragment;
import prototype.xd.scheduler.R;
import prototype.xd.scheduler.entities.TodoListEntry;
import prototype.xd.scheduler.entities.Views.CheckBox;
import prototype.xd.scheduler.utilities.EntrySettings;
import prototype.xd.scheduler.utilities.LockScreenBitmapDrawer;

public class ListViewAdapter extends BaseAdapter {
    
    private final Context context;
    
    private final LayoutInflater inflater;
    private final HomeFragment home;
    private final LockScreenBitmapDrawer lockScreenBitmapDrawer;
    
    public final ArrayList<TodoListEntry> currentTodoListEntries;
    public final ArrayList<Integer> currentTodoListEntries_indexMap;
    
    public ListViewAdapter(HomeFragment fragment, LockScreenBitmapDrawer lockScreenBitmapDrawer) {
        this.home = fragment;
        this.context = fragment.context;
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
            boolean visibilityFlag = (currentEntry.completed && currentEntry.showInList_ifCompleted) || (!currentEntry.completed && currentEntry.showInList);
            boolean show = visibilityFlag;
            if (!currentEntry.isGlobalEntry) {
                show = show && (currentEntry.associatedDate.equals(currentlySelectedDate));
            }
            show = show || (currentEntry.isNewEntry || currentEntry.isOldEntry) && currentlySelectedDate.equals(currentDate) && visibilityFlag;
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
    
    @SuppressLint("SetTextI18n")
    @Override
    public View getView(final int i, View view, ViewGroup parent) {
        
        final TodoListEntry currentEntry = currentTodoListEntries.get(i);
        TextView todoText;
        CheckBox isDone;
        
        if (view == null) {
            view = inflater.inflate(R.layout.list_selection, parent, false);
        }
        todoText = view.findViewById(R.id.todoText);
        isDone = view.findViewById(R.id.isDone);
        
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
        
        isDone.setChecked(currentEntry.completed, false);
        isDone.setOnClickListener(view12 -> {
            if (!currentEntry.isGlobalEntry) {
                currentEntry.changeParameter(IS_COMPLETED, String.valueOf(isDone.isChecked()));
            } else {
                currentEntry.changeParameter(ASSOCIATED_DATE, currentlySelectedDate);
            }
            saveEntries(home.todoListEntries);
            updateData(true);
        });
        
        view.findViewById(R.id.backgroundSecondLayer).setBackgroundColor(currentEntry.bgColor);
        
        if (currentEntry.completed) {
            todoText.setTextColor(currentEntry.fontColor_completed);
        } else {
            todoText.setTextColor(currentEntry.fontColor);
        }
        
        todoText.setText(currentEntry.textValue);
        if (currentlySelectedDate.equals(currentDate)) {
            todoText.setText(todoText.getText() + currentEntry.getDayOffset());
        }
        
        todoText.setOnLongClickListener(v -> {
            AlertDialog.Builder alert = new AlertDialog.Builder(context);
            
            final EditText input = new EditText(context);
            input.setInputType(InputType.TYPE_CLASS_TEXT);
            input.setText(currentEntry.textValue);
            alert.setView(input);
            
            alert.setTitle("Изменить");
            alert.setPositiveButton("Сохранить", (dialog, which) -> {
                currentEntry.changeParameter(TEXT_VALUE, input.getText().toString());
                saveEntries(home.todoListEntries);
                updateData(currentEntry.getLockViewState());
            });
            
            if (!currentEntry.isGlobalEntry) {
                alert.setNeutralButton("Переместить в общий список", (dialog, which) -> {
                    currentEntry.changeParameter(ASSOCIATED_DATE, DATE_FLAG_GLOBAL);
                    saveEntries(home.todoListEntries);
                    updateData(currentEntry.getLockViewState());
                });
            }
            
            alert.setNegativeButton("Отмена", (dialog, which) -> dialog.dismiss());
            
            alert.show();
            return false;
        });
        settings.setOnClickListener(v -> new EntrySettings(context, home, inflater.inflate(R.layout.entry_settings, parent, false), currentEntry, home.todoListEntries));
        
        return view;
    }
}
