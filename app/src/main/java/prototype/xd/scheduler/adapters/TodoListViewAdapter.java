package prototype.xd.scheduler.adapters;

import static java.lang.Math.max;
import static prototype.xd.scheduler.entities.Group.groupIndexInList;
import static prototype.xd.scheduler.utilities.DateManager.currentDay;
import static prototype.xd.scheduler.utilities.DateManager.currentlySelectedDay;
import static prototype.xd.scheduler.utilities.DialogueUtilities.displayConfirmationDialogue;
import static prototype.xd.scheduler.utilities.DialogueUtilities.displayEditTextSpinnerDialogue;
import static prototype.xd.scheduler.utilities.Keys.ASSOCIATED_DAY;
import static prototype.xd.scheduler.utilities.Keys.DAY_FLAG_GLOBAL_STR;
import static prototype.xd.scheduler.utilities.Keys.IS_COMPLETED;
import static prototype.xd.scheduler.utilities.Keys.TEXT_VALUE;

import android.annotation.SuppressLint;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.BaseAdapter;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.cardview.widget.CardView;

import com.google.android.material.card.MaterialCardView;

import java.util.ArrayList;

import prototype.xd.scheduler.R;
import prototype.xd.scheduler.entities.Group;
import prototype.xd.scheduler.entities.TodoListEntry;
import prototype.xd.scheduler.utilities.TodoListEntryStorage;
import prototype.xd.scheduler.views.CheckBox;
import prototype.xd.scheduler.views.settings.EntrySettings;
import prototype.xd.scheduler.views.settings.SystemCalendarSettings;

public class TodoListViewAdapter extends BaseAdapter {
    
    private final TodoListEntryStorage todoListEntryStorage;
    
    private final ArrayList<TodoListEntry> currentTodoListEntries;
    private final ArrayList<Integer> currentTodoListEntries_indexMap;
    
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
            
            view.findViewById(R.id.deletionButton).setOnClickListener(view1 ->
                    displayConfirmationDialogue(view1.getContext(),
                            R.string.delete, R.string.are_you_sure,
                            R.string.no, R.string.yes,
                            (view2) -> {
                                todoListEntryStorage.removeEntry(currentTodoListEntries_indexMap.get(i));
                                todoListEntryStorage.saveEntries();
                                todoListEntryStorage.updateTodoListAdapter(currentEntry.getLockViewState());
                            }));
            
            CheckBox isDone = view.findViewById(R.id.isDone);
            
            isDone.setCheckedSilent(currentEntry.completed);
            
            isDone.setOnClickListener(view12 -> {
                if (!currentEntry.isGlobalEntry) {
                    currentEntry.changeParameter(IS_COMPLETED, String.valueOf(isDone.isChecked()));
                } else {
                    currentEntry.changeParameter(ASSOCIATED_DAY, String.valueOf(currentlySelectedDay));
                }
                todoListEntryStorage.saveEntries();
                todoListEntryStorage.updateTodoListAdapter(true);
            });
            
            view.setOnLongClickListener(view1 -> {
                
                final ArrayList<Group> groupList = todoListEntryStorage.getGroups();
                int currentIndex = max(groupIndexInList(groupList, currentEntry.getGroupName()), 0);
                displayEditTextSpinnerDialogue(view1.getContext(), R.string.edit_event, -1, R.string.event_name_input_hint,
                        R.string.cancel, R.string.save, R.string.move_to_global_list, currentEntry.textValue, groupList,
                        currentIndex, (view2, text, selectedIndex) -> {
                            if (selectedIndex != currentIndex) {
                                currentEntry.changeGroup(groupList.get(selectedIndex));
                            }
                            currentEntry.changeParameter(TEXT_VALUE, text);
                            todoListEntryStorage.saveEntries();
                            todoListEntryStorage.updateTodoListAdapter(currentEntry.getLockViewState());
                            return true;
                        },
                        !currentEntry.isGlobalEntry ? (view2, text, selectedIndex) -> {
                            if (selectedIndex != currentIndex) {
                                currentEntry.changeGroup(groupList.get(selectedIndex));
                            }
                            currentEntry.changeParameter(ASSOCIATED_DAY, DAY_FLAG_GLOBAL_STR);
                            todoListEntryStorage.saveEntries();
                            todoListEntryStorage.updateTodoListAdapter(currentEntry.getLockViewState());
                            return true;
                        } : null);
                return true;
            });
            settings.setOnClickListener(v -> entrySettings.show(currentEntry, v.getContext()));
        } else {
            ((CardView) view.findViewById(R.id.event_color)).setCardBackgroundColor(currentEntry.event.color);
            TextView time = view.findViewById(R.id.time_text);
            time.setText(currentEntry.getTimeSpan(view.getContext()));
            time.setTextColor(currentEntry.fontColor);
            settings.setOnClickListener(v -> systemCalendarSettings.show(currentEntry));
        }
        
        MaterialCardView backgroundLayer = view.findViewById(R.id.backgroundLayer);
        backgroundLayer.setCardBackgroundColor(currentEntry.bgColor);
        backgroundLayer.setStrokeColor(currentEntry.borderColor);
        
        if (currentEntry.completed) {
            todoText.setTextColor(currentEntry.fontColor_completed);
        } else {
            todoText.setTextColor(currentEntry.fontColor);
        }
        
        todoText.setText(currentEntry.textValue + currentEntry.getDayOffset(currentlySelectedDay, view.getContext()));
        
        return view;
    }
}
