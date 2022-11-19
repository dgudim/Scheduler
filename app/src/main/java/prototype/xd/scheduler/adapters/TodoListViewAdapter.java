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
import android.content.Context;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;
import androidx.viewbinding.ViewBinding;

import com.google.android.material.card.MaterialCardView;

import java.util.ArrayList;
import java.util.List;

import prototype.xd.scheduler.R;
import prototype.xd.scheduler.databinding.ListSelectionCalendarBinding;
import prototype.xd.scheduler.databinding.ListSelectionTodoBinding;
import prototype.xd.scheduler.entities.Group;
import prototype.xd.scheduler.entities.TodoListEntry;
import prototype.xd.scheduler.utilities.TodoListEntryManager;
import prototype.xd.scheduler.views.settings.EntrySettings;
import prototype.xd.scheduler.views.settings.SystemCalendarSettings;

public class TodoListViewAdapter extends RecyclerView.Adapter<TodoListViewAdapter.EntryViewHolder<?>> {
    
    static class EntryViewHolder<V extends ViewBinding> extends RecyclerView.ViewHolder {
        
        protected V viewBinding;
        protected Context context;
        
        EntryViewHolder(V viewBinding) {
            super(viewBinding.getRoot());
            this.viewBinding = viewBinding;
            context = viewBinding.getRoot().getContext();
        }
        
        void bind(TodoListEntry currentEntry,
                  TodoListEntryManager todoListEntryManager,
                  EntrySettings entrySettings,
                  SystemCalendarSettings systemCalendarSettings) {
            
            if (currentEntry.isFromSystemCalendar()) {
                ListSelectionCalendarBinding bnd = (ListSelectionCalendarBinding)viewBinding;
    
                bnd.eventColor.setCardBackgroundColor(currentEntry.event.color);
                bnd.timeText.setText(currentEntry.getTimeSpan(context));
                bnd.timeText.setTextColor(currentEntry.fontColor.get());
                bnd.settings.setOnClickListener(v -> systemCalendarSettings.show(currentEntry));
            } else {
                ListSelectionTodoBinding bnd = (ListSelectionTodoBinding)viewBinding;
                
                bnd.deletionButton.setOnClickListener(view1 ->
                        displayConfirmationDialogue(view1.getContext(),
                                R.string.delete, R.string.are_you_sure,
                                R.string.no, R.string.yes,
                                view2 -> {
                                    todoListEntryManager.removeEntry(currentEntry);
                                    todoListEntryManager.saveEntriesAsync();
                                    // deleting a global entry does not change indicators
                                    todoListEntryManager.updateTodoListAdapter(currentEntry.isVisibleOnLockscreenToday(), !currentEntry.isGlobal());
                                }));
        
                bnd.isDone.setCheckedSilent(currentEntry.isCompleted());
    
                bnd.isDone.setOnClickListener(view12 -> {
                    if (!currentEntry.isGlobal()) {
                        currentEntry.changeParameter(IS_COMPLETED, String.valueOf(bnd.isDone.isChecked()));
                    } else {
                        currentEntry.changeParameter(ASSOCIATED_DAY, String.valueOf(currentlySelectedDay));
                    }
                    todoListEntryManager.saveEntriesAsync();
                    todoListEntryManager.updateTodoListAdapter(true, true);
                });
        
                bnd.getRoot().setOnLongClickListener(view1 -> {
            
                    final List<Group> groupList = todoListEntryManager.getGroups();
                    int currentIndex = max(groupIndexInList(groupList, currentEntry.getRawGroupName()), 0);
                    displayEditTextSpinnerDialogue(context, R.string.edit_event, -1, R.string.event_name_input_hint,
                            R.string.cancel, R.string.save, R.string.move_to_global_list, currentEntry.getRawTextValue(), groupList,
                            currentIndex, (view2, text, selectedIndex) -> {
                                if (selectedIndex != currentIndex) {
                                    currentEntry.changeGroup(groupList.get(selectedIndex));
                                }
                                currentEntry.changeParameter(TEXT_VALUE, text);
                                todoListEntryManager.saveEntriesAsync();
                                todoListEntryManager.updateTodoListAdapter(currentEntry.isVisibleOnLockscreenToday(), false);
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
                                todoListEntryManager.updateTodoListAdapter(currentEntry.isVisibleOnLockscreenToday(), !currentEntry.isCompleted());
                                return true;
                            } : null);
                    return true;
                });
                bnd.settings.setOnClickListener(v -> entrySettings.show(currentEntry, v.getContext()));
            }
    
            // fallback to get view by id because this part is common and we can't cast to any binding
            View root = viewBinding.getRoot();
            
            TextView todoText = root.findViewById(R.id.todoText);
            
            MaterialCardView backgroundLayer = root.findViewById(R.id.backgroundLayer);
            backgroundLayer.setCardBackgroundColor(currentEntry.bgColor.get());
            backgroundLayer.setStrokeColor(currentEntry.borderColor.get());
    
            if (currentEntry.isCompleted() || currentEntry.hideByContent()) {
                todoText.setTextColor(currentEntry.fontColor.get());
            } else {
                todoText.setTextColor(currentEntry.fontColor.get());
            }
    
            todoText.setText(currentEntry.getTextOnDay(currentlySelectedDay, context));
    
        }
    }
    
    private final TodoListEntryManager todoListEntryManager;
    
    private List<TodoListEntry> currentTodoListEntries;
    
    private final EntrySettings entrySettings;
    private final SystemCalendarSettings systemCalendarSettings;
    
    // no need to pass parent to vies used by dialogs
    @SuppressLint("InflateParams")
    public TodoListViewAdapter(final TodoListEntryManager todoListEntryManager, final Context context) {
        this.todoListEntryManager = todoListEntryManager;
        currentTodoListEntries = new ArrayList<>();
        entrySettings = new EntrySettings(todoListEntryManager, context);
        systemCalendarSettings = new SystemCalendarSettings(todoListEntryManager, context);
        setHasStableIds(true);
    }
    
    @Override
    public long getItemId(int i) {
        return currentTodoListEntries.get(i).getId();
    }
    
    @Override
    public int getItemCount() {
        return currentTodoListEntries.size();
    }
    
    public void notifyVisibleEntriesUpdated() {
        currentTodoListEntries = todoListEntryManager.getVisibleTodoListEntries(currentlySelectedDay);
        notifyDataSetChanged();
    }
    
    @NonNull
    @Override
    public EntryViewHolder<?> onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        if(viewType == 1) {
            // calendar entry
            return new EntryViewHolder<>(ListSelectionCalendarBinding.inflate(LayoutInflater.from(parent.getContext()), parent, false));
        } else {
            // regular entry
            return new EntryViewHolder<>(ListSelectionTodoBinding.inflate(LayoutInflater.from(parent.getContext()), parent, false));
        }
    }
    
    @Override
    public void onBindViewHolder(@NonNull EntryViewHolder<?> holder, int position) {
        holder.bind(currentTodoListEntries.get(position), todoListEntryManager, entrySettings, systemCalendarSettings);
    }
    
    @Override
    public int getItemViewType(int i) {
        return currentTodoListEntries.get(i).getType();
    }
}
