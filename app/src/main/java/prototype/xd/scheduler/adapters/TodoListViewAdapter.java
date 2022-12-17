package prototype.xd.scheduler.adapters;

import static java.lang.Math.max;
import static prototype.xd.scheduler.entities.Group.groupIndexInList;
import static prototype.xd.scheduler.utilities.BitmapUtilities.mixTwoColors;
import static prototype.xd.scheduler.utilities.DateManager.currentlySelectedDay;
import static prototype.xd.scheduler.utilities.DialogueUtilities.displayConfirmationDialogue;
import static prototype.xd.scheduler.utilities.DialogueUtilities.displayEntryAdditionEditDialog;
import static prototype.xd.scheduler.utilities.Keys.ASSOCIATED_DAY;
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
        
        private void displayDeletionDialog(@NonNull final TodoListEntry entry,
                                           @NonNull final TodoListEntryManager todoListEntryManager) {
            displayConfirmationDialogue(context, lifecycle,
                    R.string.delete, R.string.are_you_sure,
                    R.string.no, R.string.yes,
                    view2 -> todoListEntryManager.removeEntry(entry));
        }
        
        private void displayEditDialog(@NonNull final TodoListEntry entry,
                                       @NonNull final TodoListEntryManager todoListEntryManager) {
            final List<Group> groupList = todoListEntryManager.getGroups();
            int currentIndex = max(groupIndexInList(groupList, entry.getRawGroupName()), 0);
            displayEntryAdditionEditDialog(context, lifecycle,
                    R.string.edit_event, R.string.save, entry.getRawTextValue(), groupList,
                    currentIndex, (view2, text, dialogBinding, selectedIndex) -> {
                        entry.changeGroup(groupList.get(selectedIndex));
                        entry.changeParameter(TEXT_VALUE, text);
                        boolean isGlobal = dialogBinding.globalEntrySwitch.isChecked();
                        if (isGlobal) {
                            entry.changeParameter(IS_COMPLETED, "false");
                        }
                        // TODO: 17.12.2022 change start and end day
                        todoListEntryManager.performDeferredTasks();
                        return true;
                    });
        }
        
        private void bindToSystemCalendarEntry(@NonNull final TodoListEntry entry,
                                               @NonNull final SystemCalendarSettings systemCalendarSettings) {
            ListSelectionCalendarBinding bnd = (ListSelectionCalendarBinding) viewBinding;
            
            bnd.eventColor.setCardBackgroundColor(entry.event.color);
            bnd.timeText.setText(entry.getTimeSpan(context));
            bnd.timeText.setTextColor(entry.fontColor.get(currentlySelectedDay));
            bnd.settings.setOnClickListener(v -> systemCalendarSettings.show(entry));
        }
        
        private void bindToRegularEntry(@NonNull final TodoListEntry entry,
                                        @NonNull final TodoListEntryManager todoListEntryManager,
                                        @NonNull final EntrySettings entrySettings) {
            ListSelectionTodoBinding bnd = (ListSelectionTodoBinding) viewBinding;
            
            bnd.deletionButton.setOnClickListener(view1 -> displayDeletionDialog(entry, todoListEntryManager));
            
            bnd.isDone.setCheckedSilent(entry.isCompleted());
            
            bnd.isDone.setOnClickListener(view12 -> {
                if (!entry.isGlobal()) {
                    entry.changeParameter(IS_COMPLETED, String.valueOf(bnd.isDone.isChecked()));
                } else {
                    entry.changeParameter(ASSOCIATED_DAY, String.valueOf(currentlySelectedDay));
                }
                todoListEntryManager.performDeferredTasks();
            });
            
            bnd.getRoot().setOnLongClickListener(view1 -> {
                displayEditDialog(entry, todoListEntryManager);
                return true;
            });
            bnd.settings.setOnClickListener(v -> entrySettings.show(entry, v.getContext()));
        }
        
        private void bindToCommonPart(@NonNull final TodoListEntry entry) {
            // fallback to get view by id because this part is common and we can't cast to any binding
            View root = viewBinding.getRoot();
            TextView todoText = root.findViewById(R.id.todoText);
            MaterialCardView backgroundLayer = root.findViewById(R.id.backgroundLayer);
            
            backgroundLayer.setCardBackgroundColor(entry.bgColor.get(currentlySelectedDay));
            backgroundLayer.setStrokeColor(entry.borderColor.get(currentlySelectedDay));
            
            if (entry.isCompleted() || entry.hideByContent()) {
                todoText.setTextColor(mixTwoColors(entry.fontColor.get(currentlySelectedDay), Color.WHITE, 0.5));
            } else {
                todoText.setTextColor(entry.fontColor.get(currentlySelectedDay));
            }
            
            todoText.setText(entry.getTextOnDay(currentlySelectedDay, context));
        }
        
        void bindTo(@NonNull final TodoListEntry currentEntry,
                    @NonNull final TodoListEntryManager todoListEntryManager,
                    @NonNull final EntrySettings entrySettings,
                    @NonNull final SystemCalendarSettings systemCalendarSettings) {
            
            if (currentEntry.isFromSystemCalendar()) {
                bindToSystemCalendarEntry(currentEntry, systemCalendarSettings);
            } else {
                bindToRegularEntry(currentEntry, todoListEntryManager, entrySettings);
            }
            
            bindToCommonPart(currentEntry);
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
        holder.bindTo(currentTodoListEntries.get(position), todoListEntryManager, entrySettings, systemCalendarSettings);
    }
    
    @Override
    public int getItemViewType(int i) {
        return currentTodoListEntries.get(i).getType();
    }
}
