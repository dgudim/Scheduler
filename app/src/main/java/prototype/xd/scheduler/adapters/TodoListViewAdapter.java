package prototype.xd.scheduler.adapters;

import static prototype.xd.scheduler.utilities.DateManager.currentlySelectedDay;
import static prototype.xd.scheduler.utilities.Keys.ASSOCIATED_DAY;
import static prototype.xd.scheduler.utilities.Keys.DAY_FLAG_GLOBAL;
import static prototype.xd.scheduler.utilities.Keys.IS_COMPLETED;
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

import androidx.navigation.fragment.NavHostFragment;

import java.util.ArrayList;

import prototype.xd.scheduler.HomeFragment;
import prototype.xd.scheduler.MainActivity;
import prototype.xd.scheduler.R;
import prototype.xd.scheduler.entities.TodoListEntry;
import prototype.xd.scheduler.views.CheckBox;
import prototype.xd.scheduler.views.settings.EntrySettings;

public class TodoListViewAdapter extends BaseAdapter {
    
    private final Context context;
    
    private final LayoutInflater inflater;
    private final HomeFragment home;
    
    public final ArrayList<TodoListEntry> currentTodoListEntries;
    public final ArrayList<Integer> currentTodoListEntries_indexMap;
    
    private final MainActivity mainActivity;
    
    public TodoListViewAdapter(HomeFragment fragment, MainActivity mainActivity) {
        this.mainActivity = mainActivity;
        this.home = fragment;
        this.context = fragment.rootActivity;
        inflater = LayoutInflater.from(context);
        currentTodoListEntries = new ArrayList<>();
        currentTodoListEntries_indexMap = new ArrayList<>();
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
            if (currentEntry.isVisible(currentlySelectedDay)) {
                currentTodoListEntries.add(currentEntry);
                currentTodoListEntries_indexMap.add(i);
            }
        }
    }
    
    public void updateData(boolean updateBitmap) {
        if (updateBitmap) {
            mainActivity.notifyService();
        }
        home.todoListEntries = sortEntries(home.todoListEntries, currentlySelectedDay);
        updateCurrentEntries();
        home.rootActivity.runOnUiThread(TodoListViewAdapter.this::notifyDataSetChanged);
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
                view = inflater.inflate(R.layout.list_selection_calendar, parent, false);
            } else {
                view = inflater.inflate(R.layout.list_selection, parent, false);
            }
        }
        
        TextView todoText = view.findViewById(R.id.todoText);
        
        ImageView settings = view.findViewById(R.id.settings);
        
        if (!currentEntry.fromSystemCalendar) {
            
            view.findViewById(R.id.deletionButton).setOnClickListener(view1 -> {
                
                AlertDialog.Builder alert = new AlertDialog.Builder(context);
                alert.setTitle("Удалить");
                alert.setMessage("Вы уверены?");
                alert.setPositiveButton("Да", (dialog, which) -> {
                    home.todoListEntries.remove(currentTodoListEntries_indexMap.get(i).intValue());
                    saveEntries(home.todoListEntries);
                    updateData(currentEntry.isVisibleOnLock());
                });
                
                alert.setNegativeButton("Нет", (dialog, which) -> dialog.dismiss());
                
                alert.show();
            });
            
            CheckBox isDone = view.findViewById(R.id.isDone);
            
            isDone.setChecked(currentEntry.isCompleted(), false);
            isDone.setOnClickListener(view12 -> {
                if (!currentEntry.isGlobal()) {
                    currentEntry.changeParameter(IS_COMPLETED, isDone.isChecked() ? 1 : 0);
                } else {
                    currentEntry.changeParameter(ASSOCIATED_DAY, (int) currentlySelectedDay);
                }
                saveEntries(home.todoListEntries);
                updateData(true);
            });
            
            todoText.setOnLongClickListener(v -> {
                AlertDialog.Builder alert = new AlertDialog.Builder(context);
                
                final EditText input = new EditText(context);
                input.setInputType(InputType.TYPE_CLASS_TEXT);
                input.setText(currentEntry.getTextValue());
                alert.setView(input);
                
                alert.setTitle("Изменить");
                alert.setPositiveButton("Сохранить", (dialog, which) -> {
                    currentEntry.setTextValue(input.getText().toString());
                    saveEntries(home.todoListEntries);
                    updateData(currentEntry.isVisibleOnLock());
                });
                
                if (!currentEntry.isGlobal()) {
                    alert.setNeutralButton("Переместить в общий список", (dialog, which) -> {
                        currentEntry.changeParameter(ASSOCIATED_DAY, DAY_FLAG_GLOBAL);
                        saveEntries(home.todoListEntries);
                        updateData(currentEntry.isVisibleOnLock());
                    });
                }
                
                alert.setNegativeButton("Отмена", (dialog, which) -> dialog.dismiss());
                
                alert.show();
                return false;
            });
            settings.setOnClickListener(v -> new EntrySettings(context, home, inflater.inflate(R.layout.entry_settings, parent, false), currentEntry, home.todoListEntries));
        } else {
            view.findViewById(R.id.event_color).setBackgroundColor(currentEntry.event.color);
            ((TextView) view.findViewById(R.id.time_text)).setText(currentEntry.getTimeSpan(context));
            settings.setOnClickListener(v -> NavHostFragment.findNavController(home).navigate(R.id.action_HomeFragment_to_SettingsFragment));
        }
        
        view.findViewById(R.id.backgroundSecondLayer).setBackgroundColor(currentEntry.getBgColor(currentlySelectedDay));
        
        todoText.setTextColor(currentEntry.getFontColor(currentlySelectedDay));
        
        todoText.setText(currentEntry.getTextValue() + currentEntry.getDayOffset(currentlySelectedDay));
        
        return view;
    }
}
