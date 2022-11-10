package prototype.xd.scheduler.adapters;

import static java.lang.Math.max;
import static prototype.xd.scheduler.entities.Group.groupIndexInList;
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
import java.util.List;

import prototype.xd.scheduler.R;
import prototype.xd.scheduler.entities.Group;
import prototype.xd.scheduler.entities.TodoListEntry;
import prototype.xd.scheduler.utilities.TodoListEntryManager;
import prototype.xd.scheduler.views.CheckBox;
import prototype.xd.scheduler.views.settings.EntrySettings;
import prototype.xd.scheduler.views.settings.SystemCalendarSettings;

public class TodoListViewAdapter extends BaseAdapter {
    
    private final TodoListEntryManager todoListEntryManager;
    
    private List<TodoListEntry> currentTodoListEntries;
    
    private final EntrySettings entrySettings;
    private final SystemCalendarSettings systemCalendarSettings;
    
    public TodoListViewAdapter(final TodoListEntryManager todoListEntryManager, final ViewGroup parent) {
        this.todoListEntryManager = todoListEntryManager;
        currentTodoListEntries = new ArrayList<>();
        entrySettings = new EntrySettings(todoListEntryManager,
                LayoutInflater.from(parent.getContext()).inflate(R.layout.entry_settings, parent, false));
        systemCalendarSettings = new SystemCalendarSettings(todoListEntryManager,
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
    
    public void notifyVisibleEntriesUpdated() {
        currentTodoListEntries = todoListEntryManager.getVisibleTodoListEntries(currentlySelectedDay);
        notifyDataSetChanged();
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
                view = LayoutInflater.from(parent.getContext()).inflate(R.layout.list_selection_todo, parent, false);
            }
        }
        
        TextView todoText = view.findViewById(R.id.todoText);
        ImageView settings = view.findViewById(R.id.settings);
        
        if (currentEntry.fromSystemCalendar) {
            ((CardView) view.findViewById(R.id.event_color)).setCardBackgroundColor(currentEntry.event.color);
            TextView time = view.findViewById(R.id.time_text);
            time.setText(currentEntry.getTimeSpan(view.getContext()));
            time.setTextColor(currentEntry.fontColor);
            settings.setOnClickListener(v -> systemCalendarSettings.show(currentEntry));
        } else {
            view.findViewById(R.id.deletionButton).setOnClickListener(view1 ->
                    displayConfirmationDialogue(view1.getContext(),
                            R.string.delete, R.string.are_you_sure,
                            R.string.no, R.string.yes,
                            view2 -> {
                                todoListEntryManager.removeEntry(currentEntry);
                                todoListEntryManager.saveEntriesAsync();
                                // deleting a global entry does not change indicators
                                todoListEntryManager.updateTodoListAdapter(currentEntry.isVisibleOnLockscreen(), !currentEntry.isGlobal());
                            }));
            
            CheckBox isDone = view.findViewById(R.id.isDone);
            
            isDone.setCheckedSilent(currentEntry.isCompleted());
            
            isDone.setOnClickListener(view12 -> {
                if (!currentEntry.isGlobal()) {
                    currentEntry.changeParameter(IS_COMPLETED, String.valueOf(isDone.isChecked()));
                } else {
                    currentEntry.changeParameter(ASSOCIATED_DAY, String.valueOf(currentlySelectedDay));
                }
                todoListEntryManager.saveEntriesAsync();
                todoListEntryManager.updateTodoListAdapter(true, true);
            });
            
            view.setOnLongClickListener(view1 -> {
                
                final List<Group> groupList = todoListEntryManager.getGroups();
                int currentIndex = max(groupIndexInList(groupList, currentEntry.getGroupName()), 0);
                displayEditTextSpinnerDialogue(view1.getContext(), R.string.edit_event, -1, R.string.event_name_input_hint,
                        R.string.cancel, R.string.save, R.string.move_to_global_list, currentEntry.getRawTextValue(), groupList,
                        currentIndex, (view2, text, selectedIndex) -> {
                            if (selectedIndex != currentIndex) {
                                currentEntry.changeGroup(groupList.get(selectedIndex));
                            }
                            currentEntry.changeParameter(TEXT_VALUE, text);
                            todoListEntryManager.saveEntriesAsync();
                            todoListEntryManager.updateTodoListAdapter(currentEntry.isVisibleOnLockscreen(), false);
                            return true;
                        },
                        !currentEntry.isGlobal() ? (view2, text, selectedIndex) -> {
                            if (selectedIndex != currentIndex) {
                                currentEntry.changeGroup(groupList.get(selectedIndex));
                            }
                            currentEntry.changeParameter(ASSOCIATED_DAY, DAY_FLAG_GLOBAL_STR);
                            currentEntry.changeParameter(IS_COMPLETED, "false");
                            todoListEntryManager.saveEntriesAsync();
                            // completed -> global = indicators don't change, no need to update
                            todoListEntryManager.updateTodoListAdapter(currentEntry.isVisibleOnLockscreen(), !currentEntry.isCompleted());
                            return true;
                        } : null);
                return true;
            });
            settings.setOnClickListener(v -> entrySettings.show(currentEntry, v.getContext()));
        }
        
        MaterialCardView backgroundLayer = view.findViewById(R.id.backgroundLayer);
        backgroundLayer.setCardBackgroundColor(currentEntry.bgColor);
        backgroundLayer.setStrokeColor(currentEntry.borderColor);
        
        if (currentEntry.isCompleted() || currentEntry.hideByContent()) {
            todoText.setTextColor(currentEntry.fontColor_completed);
        } else {
            todoText.setTextColor(currentEntry.fontColor);
        }
        
        todoText.setText(currentEntry.getTextOnDay(currentlySelectedDay, view.getContext()));
        
        return view;
    }
}
