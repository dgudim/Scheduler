package prototype.xd.scheduler.adapters;

import static java.lang.Math.max;
import static prototype.xd.scheduler.entities.Group.groupIndexInList;
import static prototype.xd.scheduler.utilities.BitmapUtilities.mixTwoColors;
import static prototype.xd.scheduler.utilities.DateManager.currentlySelectedDay;
import static prototype.xd.scheduler.utilities.DialogueUtilities.displayConfirmationDialogue;
import static prototype.xd.scheduler.utilities.DialogueUtilities.displayEditTextSpinnerDialogue;
import static prototype.xd.scheduler.utilities.Keys.ASSOCIATED_DAY;
import static prototype.xd.scheduler.utilities.Keys.DAY_FLAG_GLOBAL_STR;
import static prototype.xd.scheduler.utilities.Keys.IS_COMPLETED;
import static prototype.xd.scheduler.utilities.Keys.TEXT_VALUE;

import android.content.Context;
import android.graphics.Color;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.lifecycle.Lifecycle;
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
        
        @NonNull
        private final V viewBinding;
        @NonNull
        private final Context context;
        @NonNull
        private final Lifecycle lifecycle;
        
        EntryViewHolder(@NonNull final V viewBinding,
                        @NonNull final Lifecycle lifecycle) {
            super(viewBinding.getRoot());
            this.viewBinding = viewBinding;
            this.lifecycle = lifecycle;
            context = viewBinding.getRoot().getContext();
        }
        
        void bind(@NonNull final TodoListEntry currentEntry,
                  @NonNull final TodoListEntryManager todoListEntryManager,
                  @NonNull final EntrySettings entrySettings,
                  @NonNull final SystemCalendarSettings systemCalendarSettings) {
            
            if (currentEntry.isFromSystemCalendar()) {
                ListSelectionCalendarBinding bnd = (ListSelectionCalendarBinding) viewBinding;
                
                bnd.eventColor.setCardBackgroundColor(currentEntry.event.color);
                bnd.timeText.setText(currentEntry.getTimeSpan(context));
                bnd.timeText.setTextColor(currentEntry.fontColor.get(currentlySelectedDay));
                bnd.settings.setOnClickListener(v -> systemCalendarSettings.show(currentEntry));
            } else {
                ListSelectionTodoBinding bnd = (ListSelectionTodoBinding) viewBinding;
                
                bnd.deletionButton.setOnClickListener(view1 ->
                        displayConfirmationDialogue(view1.getContext(), lifecycle,
                                R.string.delete, R.string.are_you_sure,
                                R.string.no, R.string.yes,
                                view2 -> todoListEntryManager.removeEntry(currentEntry)));
                
                bnd.isDone.setCheckedSilent(currentEntry.isCompleted());
                
                bnd.isDone.setOnClickListener(view12 -> {
                    if (!currentEntry.isGlobal()) {
                        currentEntry.changeParameter(IS_COMPLETED, String.valueOf(bnd.isDone.isChecked()));
                    } else {
                        currentEntry.changeParameter(ASSOCIATED_DAY, String.valueOf(currentlySelectedDay));
                    }
                    todoListEntryManager.performDeferredTasks();
                });
                
                bnd.getRoot().setOnLongClickListener(view1 -> {
                    
                    final List<Group> groupList = todoListEntryManager.getGroups();
                    int currentIndex = max(groupIndexInList(groupList, currentEntry.getRawGroupName()), 0);
                    displayEditTextSpinnerDialogue(context, lifecycle,
                            R.string.edit_event, -1, R.string.event_name_input_hint,
                            R.string.cancel, R.string.save, R.string.move_to_global_list, currentEntry.rawTextValue.get(), groupList,
                            currentIndex, (view2, text, selectedIndex) -> {
                                if (selectedIndex != currentIndex) {
                                    currentEntry.changeGroup(groupList.get(selectedIndex));
                                }
                                currentEntry.changeParameter(TEXT_VALUE, text);
                                todoListEntryManager.performDeferredTasks();
                                return true;
                            },
                            !currentEntry.isGlobal() ? (view2, text, selectedIndex) -> {
                                if (selectedIndex != currentIndex) {
                                    currentEntry.changeGroup(groupList.get(selectedIndex));
                                }
                                currentEntry.changeParameter(ASSOCIATED_DAY, DAY_FLAG_GLOBAL_STR);
                                currentEntry.changeParameter(IS_COMPLETED, "false");
                                todoListEntryManager.performDeferredTasks();
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
            backgroundLayer.setCardBackgroundColor(currentEntry.bgColor.get(currentlySelectedDay));
            backgroundLayer.setStrokeColor(currentEntry.borderColor.get(currentlySelectedDay));
            
            if (currentEntry.isCompleted() || currentEntry.hideByContent()) {
                todoText.setTextColor(mixTwoColors(currentEntry.fontColor.get(currentlySelectedDay), Color.WHITE, 0.5));
            } else {
                todoText.setTextColor(currentEntry.fontColor.get(currentlySelectedDay));
            }
            
            todoText.setText(currentEntry.getTextOnDay(currentlySelectedDay, context));
            
        }
    }
    
    @NonNull
    private final TodoListEntryManager todoListEntryManager;
    @NonNull
    private List<TodoListEntry> currentTodoListEntries;
    
    @NonNull
    private final EntrySettings entrySettings;
    @NonNull
    private final SystemCalendarSettings systemCalendarSettings;
    @NonNull
    private final Lifecycle lifecycle;
    
    public TodoListViewAdapter(@NonNull final TodoListEntryManager todoListEntryManager,
                               @NonNull final Context context,
                               @NonNull final Lifecycle lifecycle) {
        
        this.todoListEntryManager = todoListEntryManager;
        this.lifecycle = lifecycle;
        currentTodoListEntries = new ArrayList<>();
        entrySettings = new EntrySettings(todoListEntryManager, context, lifecycle);
        systemCalendarSettings = new SystemCalendarSettings(todoListEntryManager, context, lifecycle);
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
        int itemsCount = currentTodoListEntries.size();
        currentTodoListEntries = todoListEntryManager.getVisibleTodoListEntries(currentlySelectedDay);
        notifyItemRangeChanged(0, max(itemsCount, currentTodoListEntries.size()));
    }
    
    @NonNull
    @Override
    public EntryViewHolder<?> onCreateViewHolder(@NonNull final ViewGroup parent, int viewType) {
        if (viewType == 1) {
            // calendar entry
            return new EntryViewHolder<>(ListSelectionCalendarBinding.inflate(LayoutInflater.from(parent.getContext()), parent, false), lifecycle);
        } else {
            // regular entry
            return new EntryViewHolder<>(ListSelectionTodoBinding.inflate(LayoutInflater.from(parent.getContext()), parent, false), lifecycle);
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
